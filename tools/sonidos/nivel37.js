#!/usr/bin/env node
'use strict';
/**
 * Los sonidos del juego en las Piscinas (Nivel 37) y los de la Serpiente. Todo
 * sintetizado aqui, en mono (Minecraft solo situa en 3D los sonidos mono), con
 * la reverberacion de una piscina cubierta metida en el propio sonido: azulejo,
 * agua y techo duro, casi sin amortiguar los agudos, y ecos que rebotan.
 *
 *   node tools/sonidos/nivel37.js [prefijo]   (sin prefijo, todos)
 *
 * (Los del menu de las Piscinas van en piscinas.js; aquellos son otra cosa.)
 *
 *  Ambiente (los dispara de vez en cuando AmbientePiscinas, en el cliente)
 *   piscinas_gota1-3      gotas sueltas al agua (la burbuja que resuena y sube)
 *   piscinas_agua1-3      agua que se mueve, muy baja, como una piscina lejana que se asienta
 *   piscinas_olas1-3      una ola que rompe contra el borde de la piscina (evento piscinas.orilla)
 *   piscinas_zambullida1-2  alguien que se tira al agua
 *   piscinas_chapoteo1-2  un chapoteo suave en algun sitio
 *   piscinas_tuberia1-5   tuberias: golpes de dilatacion, un crujido, un siseo, un gorgoteo, una vibracion
 *   piscinas_grave1-3     cosas graves que no se sabe que son
 *   piscinas_voz1-3       una voz humana lejanisima, irreconocible (rarisimo)
 *  Jugador
 *   piscinas_paso1-6      pasos secos sobre azulejo, con mucha reverberacion
 *   piscinas_paso_agua1-5 pasos con el agua por los tobillos
 *   piscinas_nado1-3      brazadas
 *  Luces
 *   piscinas_chispa1-3    el chasquido electrico de un panel que parpadea
 *  La Serpiente
 *   serpiente_grunido1-3  el grunido que se oye lejos (cada 1-2 minutos)
 *   serpiente_respira1-3  respiracion humeda y rasposa
 *   serpiente_paso1-4     el cuerpo que se arrastra por el azulejo y las garras de hueso
 *   serpiente_agua1-3     el cuerpo moviendose dentro del agua
 *   serpiente_grito       te ha visto
 *   serpiente_caza1-2     persiguiendote: resoplidos y siseo
 *   serpiente_ataque      se lanza: siseo, rugido y la mandibula que se abre
 *   serpiente_mordisco    la mandibula que cierra: hueso y agua
 *  Las misiones
 *   valvula_giro1-3       un cuarto de vuelta del volante: hierro que se resiste y el tubo que vibra
 *   valvula_cierre        el tope, el tubo que retumba y la presion que se escapa silbando
 *   valvula_maquinaria    lejos, una maquinaria que arranca (al cerrar las tres; solo la oye el)
 *   tarjeta_coger         plastico que roza y un clic
 *   lector_acepta         la ranura, dos pitidos y el rele del cierre
 *   puerta_salida         los cierres que saltan, el aire y la puerta pesada que se arrastra
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { suave, mono, poner, pico, guardarMono, garganta, rugido, ecos, fuerte, SALIDA } = E;

/* ============================================================== piezas */

/** Reverberacion de piscina cubierta: brillante, larga y con rebotes del azulejo. */
function piscina (buf, mezcla = 0.7, tam = 0.9, rebotes = true) {
  const rv = new D.Freeverb({ sala: tam, amortiguacion: 0.18, predelayMs: 28 });
  const out = new Float32Array(buf.length);
  for (let i = 0; i < buf.length; i++) out[i] = buf[i] + rv.paso(buf[i])[0] * mezcla;
  return rebotes ? ecos(out, [0.083, 0.171, 0.29], 0.32, 5200) : out;
}

/** Copia `src` dentro de `buf` desde t0 (con ganancia). */
function meter (buf, t0, src, g = 1) {
  const i0 = Math.round(t0 * SR);
  for (let i = 0; i < src.length && i0 + i < buf.length; i++) buf[i0 + i] += src[i] * g;
}

/** La burbuja de una gota en el agua: un seno que sube de tono (resonancia de Minnaert). */
function burbuja (f0, subida = 1.9, caida = 0.045) {
  let fase = 0;
  return (t) => {
    const f = f0 * (1 + (subida - 1) * Math.min(1, t / 0.035));
    fase += TAU * f / SR;
    return Math.sin(fase) * Math.exp(-t / caida) * Math.min(1, t / 0.0015);
  };
}

/** Ruido filtrado en banda con una envolvente (ataque y caida en segundos). */
function soplo (rnd, f, q, ataque, caida) {
  const bp = new D.Biquad('bp', f, q);
  return (t) => bp.paso(rnd() * 2 - 1) * Math.min(1, t / ataque) * Math.exp(-Math.max(0, t - ataque) / caida);
}

/** Bajar el volumen de todo el sonido a `db` decibelios (despues de pico). */
function bajar (buf, dB) {
  const g = Math.pow(10, dB / 20);
  for (let i = 0; i < buf.length; i++) buf[i] *= g;
  return buf;
}

/** Como se oye algo lejos: sin agudos y con mas sala que sonido directo. */
function lejos (buf, corte = 1400) {
  const lp = new D.Biquad('lp', corte, 0.7);
  const lp2 = new D.Biquad('lp', corte * 1.3, 0.7);
  for (let i = 0; i < buf.length; i++) buf[i] = lp2.paso(lp.paso(buf[i]));
  return buf;
}

/* ============================================================ ambiente */

function gota (k) {
  const rnd = D.azar(3700 + k);
  const buf = mono(2.4);
  if (k < 4) {
    // al agua: el chasquido del impacto y la burbuja que resuena
    const f0 = 820 + rnd() * 900;
    poner(buf, 0.05, 0.004, (t) => (rnd() * 2 - 1) * Math.exp(-t * 900) * 0.5);
    poner(buf, 0.052, 0.4, burbuja(f0, 1.6 + rnd() * 0.5, 0.035 + rnd() * 0.02));
    if (rnd() < 0.6) poner(buf, 0.1 + rnd() * 0.06, 0.3, burbuja(f0 * (1.4 + rnd() * 0.5), 1.7, 0.02));
  } else {
    // en el azulejo: un "tic" seco y agudo
    const f = 3200 + rnd() * 1800;
    poner(buf, 0.05, 0.08, soplo(rnd, f, 6, 0.0006, 0.007));
    poner(buf, 0.05, 0.05, (t) => Math.sin(TAU * (1900 + rnd() * 30) * t) * Math.exp(-t / 0.006) * 0.4);
  }
  return bajar(pico(piscina(buf, 0.9, 0.93), -1), -2);
}

function agua (k) {
  const rnd = D.azar(3710 + k);
  const dur = 3.4 + k * 0.5;
  const buf = mono(dur + 1.5);
  const lp = new D.Biquad('lp', 520 + k * 60, 0.8);
  const bp = new D.Biquad('bp', 260, 1.4);
  poner(buf, 0, dur, (t) => {
    const ola = suave(0, dur * 0.35, t) * (1 - suave(dur * 0.55, dur, t)) * (0.7 + 0.3 * Math.sin(t * 1.7 + k));
    return (lp.paso(rnd() * 2 - 1) * 1.4 + bp.paso(rnd() * 2 - 1) * 0.4) * ola;
  });
  // burbujas graves sueltas: algo se asienta en el fondo
  for (let n = 0; n < 4 + k; n++) poner(buf, 0.6 + rnd() * (dur - 1.2), 0.3, burbuja(260 + rnd() * 260, 1.3, 0.05));
  return bajar(pico(piscina(lejos(buf, 1100), 0.8, 0.92), -1), -4);
}


/** Una ola que rompe contra el borde: crece grave, golpea el azulejo, se deshace en espuma y escurre. */
function olas (k) {
  const rnd = D.azar(3720 + k);
  const buf = mono(4.6);
  const t0 = 0.25 + rnd() * 0.2;
  // la ola que llega: ruido grave que crece
  const lp = new D.Biquad('lp', 380, 0.7);
  poner(buf, 0.0, t0 + 0.1, (t) => lp.paso(rnd() * 2 - 1) * Math.pow(Math.min(1, t / (t0 + 0.1)), 2) * 1.6);
  // el golpe contra la pared: un bum hueco y el chasquido del agua
  poner(buf, t0, 0.5, (t) => Math.sin(TAU * (70 + 30 * Math.exp(-t * 18)) * t) * Math.exp(-t / 0.11) * 1.1);
  const bp = new D.Biquad('bp', 900 + rnd() * 500, 0.9);
  poner(buf, t0, 0.5, (t) => bp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.006) * Math.exp(-t / 0.09) * 2.2);
  // la espuma: siseo que se apaga, con crepitar de burbujas
  const hp = new D.Biquad('hp', 1800, 0.7);
  poner(buf, t0 + 0.05, 2.6, (t) => hp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.7) * (0.7 + 0.3 * rnd()) * 0.9);
  for (let m = 0; m < 26; m++) poner(buf, t0 + 0.1 + rnd() * 1.6, 0.12, burbuja(1400 + rnd() * 2600, 1.6, 0.008 + rnd() * 0.01));
  // el agua que escurre de vuelta
  const lp2 = new D.Biquad('lp', 1100, 0.7);
  poner(buf, t0 + 0.8, 2.4, (t) => lp2.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 2.4) * 0.45);
  return bajar(pico(piscina(buf, 0.6, 0.9), -1), -4);
}

/** Alguien se tira al agua: el golpe del cuerpo, la zambullida con burbujas y la lluvia de gotas, con toda la sala. */
function zambullida (k) {
  const rnd = D.azar(3740 + k);
  const buf = mono(3.4);
  // el impacto: palmada seca y un golpe grave
  const bp = new D.Biquad('bp', 1500 + rnd() * 600, 1.1);
  poner(buf, 0.03, 0.2, (t) => bp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.002) * Math.exp(-t / 0.035) * 3.2);
  poner(buf, 0.03, 0.4, (t) => Math.sin(TAU * (85 + 70 * Math.exp(-t * 25)) * t) * Math.exp(-t / 0.08) * 1.3);
  // la cavidad que se cierra: un "plop" grave que sube y burbujas
  poner(buf, 0.09, 0.5, burbuja(230 + rnd() * 80, 2.2, 0.09));
  for (let m = 0; m < 18; m++) poner(buf, 0.1 + rnd() * 0.5, 0.15, burbuja(500 + rnd() * 1500, 1.8, 0.02));
  // el agua que salta y vuelve a caer, mas aguda
  const hp = new D.Biquad('hp', 2200, 0.7);
  poner(buf, 0.05, 1.2, (t) => hp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.25) * 1.2);
  for (let m = 0; m < 40; m++) poner(buf, 0.35 + rnd() * 0.9, 0.05, soplo(rnd, 3000 + rnd() * 3000, 4, 0.0005, 0.006));
  for (let m = 0; m < 12; m++) poner(buf, 0.4 + rnd() * 0.9, 0.12, burbuja(1600 + rnd() * 2000, 1.7, 0.01));
  return bajar(fuerte(piscina(buf, 0.85, 0.94), 1.6, -0.5), -5);
}

/** Un paso en el azulejo de una nave enorme: el tacon, el roce de la suela y el eco que tarda en irse. */
function paso (k) {
  const rnd = D.azar(3800 + k);
  const buf = mono(1.8);
  const g = 0.85 + rnd() * 0.3;
  // tacon: golpe sordo con algo de cuerpo
  const lp = new D.Biquad('lp', 900 + rnd() * 400, 0.7);
  poner(buf, 0.01, 0.12, (t) => (lp.paso(rnd() * 2 - 1) * 1.4 + Math.sin(TAU * (110 + rnd() * 40) * t) * 0.8) * Math.min(1, t / 0.0015) * Math.exp(-t / 0.028) * g);
  // la suela que roza (a veces), aguda y corta
  if (rnd() < 0.7) poner(buf, 0.05 + rnd() * 0.03, 0.09, soplo(rnd, 2600 + rnd() * 1800, 2.2, 0.01, 0.025));
  // el resto del pie
  poner(buf, 0.07, 0.08, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.02) * 0.5 * g);
  return bajar(pico(ecos(piscina(buf, 0.8, 0.93), [0.19, 0.37, 0.61], 0.35, 3000), -1), -5);
}

/** Una brazada: la mano que entra, el tiron bajo el agua y las salpicaduras que caen. */
function nado (k) {
  const rnd = D.azar(3820 + k);
  const buf = mono(1.6);
  const bp = new D.Biquad('bp', 1100 + rnd() * 500, 0.9);
  poner(buf, 0.02, 0.25, (t) => bp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.004) * Math.exp(-t / 0.05) * 2.2);
  poner(buf, 0.03, 0.2, burbuja(320 + rnd() * 120, 1.9, 0.05));
  const lp = new D.Biquad('lp', 700, 0.7);
  poner(buf, 0.08, 0.45, (t) => lp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.45) * 1.3);
  const hp = new D.Biquad('hp', 2500, 0.7);
  poner(buf, 0.3, 0.5, (t) => hp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.01) * Math.exp(-t / 0.12) * 0.9);
  for (let m = 0; m < 14; m++) poner(buf, 0.3 + rnd() * 0.5, 0.06, soplo(rnd, 2500 + rnd() * 3000, 4, 0.0005, 0.007));
  for (let m = 0; m < 6; m++) poner(buf, 0.15 + rnd() * 0.4, 0.12, burbuja(900 + rnd() * 1300, 1.7, 0.012));
  return bajar(pico(piscina(buf, 0.55, 0.9), -1), -5);
}

function chapoteo (k) {
  const rnd = D.azar(3730 + k);
  const buf = mono(3.6);
  const hp = new D.Biquad('hp', 300, 0.7);
  const lp = new D.Biquad('lp', 5500, 0.7);
  poner(buf, 0.05, 0.6, (t) => lp.paso(hp.paso(rnd() * 2 - 1)) * Math.min(1, t / 0.012) * Math.exp(-t / 0.16));
  poner(buf, 0.05, 0.4, (t) => Math.sin(TAU * (95 + 20 * Math.exp(-t * 20)) * t) * Math.exp(-t / 0.09) * 0.9);
  for (let m = 0; m < 9; m++) poner(buf, 0.1 + rnd() * 0.5, 0.2, burbuja(700 + rnd() * 1600, 1.8, 0.018));
  return bajar(pico(piscina(lejos(buf, 2600), 0.95, 0.93), -1), -3);
}

/** Tuberias: cinco cosas distintas, siempre lejos, dentro de las paredes. */
function tuberia (k) {
  const rnd = D.azar(3740 + k);
  const buf = mono(4.0);
  if (k === 0 || k === 1) {
    // golpes de dilatacion: el metal se mueve (uno o una tanda)
    const f = 170 + rnd() * 90;
    const golpes = k === 0 ? 1 : 3;
    for (let n = 0; n < golpes; n++) {
      const t0 = 0.05 + n * (0.32 + rnd() * 0.25);
      const g = n === 0 ? 1 : 0.6;
      poner(buf, t0, 1.4, (t) => [[1, 1], [2.76, 0.5], [5.4, 0.25], [8.93, 0.1]]
        .reduce((s, [m, a]) => s + Math.sin(TAU * f * m * t) * a * Math.exp(-t * m * 2.2), 0) * g * Math.exp(-t * 1.6));
      poner(buf, t0, 0.05, (t) => (rnd() * 2 - 1) * Math.exp(-t * 160) * g);
    }
  } else if (k === 2) {
    // crujido: el tubo cede a tirones
    const res = [new D.Biquad('bp', 520, 12), new D.Biquad('bp', 910, 10), new D.Biquad('bp', 1460, 8)];
    let fase = 0;
    poner(buf, 0.1, 1.6, (t) => {
      const ritmo = 34 + 26 * Math.sin(t * 3.1) + rnd() * 10;
      fase += ritmo / SR;
      let x = 0;
      if (fase >= 1) { fase -= 1; x = 1; }
      const env = suave(0, 0.2, t) * (1 - suave(1.2, 1.6, t));
      return res.reduce((s, r, i) => s + r.paso(x) * [1, 0.7, 0.4][i], 0) * env * 6;
    });
  } else if (k === 3) {
    // siseo: una fuga que se abre y se cierra
    const bp = new D.Biquad('bp', 3200, 1.6);
    poner(buf, 0.1, 2.2, (t) => bp.paso(rnd() * 2 - 1) * suave(0, 0.5, t) * (1 - suave(1.4, 2.2, t)) * (0.8 + 0.2 * Math.sin(t * 23)));
  } else {
    // gorgoteo: el agua pasa por dentro de un tubo
    const tubo = new D.Biquad('bp', 360, 4);
    const pre = mono(2.4);
    for (let n = 0; n < 16; n++) poner(pre, 0.1 + rnd() * 1.8, 0.2, burbuja(180 + rnd() * 260, 1.4, 0.04));
    for (let i = 0; i < pre.length; i++) buf[i] += tubo.paso(pre[i]) * 3 + pre[i] * 0.3;
  }
  return bajar(pico(piscina(lejos(buf, 2200), 0.85, 0.9), -1), -5);
}

/** Graves que no se sabe que son: un golpe lejano, el edificio que gime, algo enorme que se mueve. */
function grave (k) {
  const rnd = D.azar(3750 + k);
  const buf = mono(6.0);
  if (k === 0) {
    poner(buf, 0.05, 4.0, (t) => (Math.sin(TAU * (42 + 18 * Math.exp(-t * 3)) * t) + Math.sin(TAU * 88 * t) * 0.35) * Math.exp(-t / 1.1));
    const lp = new D.Biquad('lp', 180, 0.7);
    poner(buf, 0.05, 1.5, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.35) * 2);
  } else if (k === 1) {
    let fase = 0;
    const bp = new D.Biquad('bp', 140, 3);
    poner(buf, 0.1, 4.6, (t) => {
      const f = 52 + 9 * Math.sin(t * 0.9) + 4 * Math.sin(t * 2.3);
      fase += f / SR;
      const s = D.sierra(fase % 1, f / SR);
      return bp.paso(s) * suave(0, 1.2, t) * (1 - suave(3.4, 4.6, t)) * 2.5;
    });
  } else {
    for (let n = 0; n < 3; n++) {
      poner(buf, 0.2 + n * 1.1, 1.6, (t) => (Math.sin(TAU * 58 * t) + Math.sin(TAU * 116 * t) * 0.4 + Math.sin(TAU * 174 * t) * 0.15)
        * Math.min(1, t / 0.08) * Math.exp(-t / 0.45) * (n === 2 ? 0.7 : 1));
    }
  }
  return bajar(pico(piscina(lejos(buf, 700), 1.1, 0.95, false), -1), -2);
}

/**
 * Una voz humana lejanisima: alguien que llama o se queja, pero no se entiende que
 * dice (silabas sin palabra, por formantes que cambian), tras muchas paredes.
 */
function voz (k) {
  const rnd = D.azar(3760 + k);
  const dur = 2.6 + k * 0.5;
  const buf = mono(dur + 3);
  const mujer = k === 1;
  const vocales = [[730, 1090, 2440], [530, 1840, 2480], [300, 2200, 2900], [570, 840, 2410], [440, 1020, 2240]];
  const esc = mujer ? 1.17 : 1.0;
  const bps = [0, 1, 2].map(() => new D.Biquad('bp', 500, 7));
  let fase = 0;
  let silaba = -1;
  let v0 = vocales[0];
  let v1 = vocales[1];
  let tSil = 0;
  let muda = false;
  const durSil = 0.17 + rnd() * 0.08;
  poner(buf, 0.2, dur, (t) => {
    const s = Math.floor(t / durSil);
    if (s !== silaba) {
      silaba = s;
      v0 = v1;
      v1 = vocales[Math.floor(rnd() * vocales.length)];
      tSil = t;
      muda = s > 0 && rnd() < 0.18;
    }
    const u = Math.min(1, (t - tSil) / (durSil * 0.5));
    const fo = (mujer ? 215 : 128) * (1 + 0.18 * Math.sin(t * 1.4 + k) - 0.12 * (t / dur)) * (1 + 0.02 * Math.sin(TAU * 5.5 * t));
    fase = (fase + fo / SR) % 1;
    const src = D.sierra(fase, fo / SR);
    let out = 0;
    for (let f = 0; f < 3; f++) {
      if (Math.round(t * SR) % 32 === 0) bps[f].ajustar((v0[f] + (v1[f] - v0[f]) * u) * esc, 7 + f * 3);
      out += bps[f].paso(src) * [1, 0.6, 0.3][f];
    }
    // silabas: sube y baja con cada una, y alguna se pierde entera
    const env = Math.sin(Math.PI * Math.min(1, (t - tSil) / durSil)) * (muda ? 0.1 : 1);
    return out * env * suave(0, 0.15, t) * (1 - suave(dur - 0.4, dur, t));
  });
  return bajar(pico(piscina(lejos(buf, 900), 1.4, 0.96), -1), -1);
}

/* ============================================================== jugador */

/** Paso en el agua por los tobillos: el pie que entra y el agua que salpica. */
function pasoAgua (k) {
  const rnd = D.azar(3810 + k);
  const buf = mono(1.4);
  const lp = new D.Biquad('lp', 1900, 0.8);
  poner(buf, 0.01, 0.5, (t) => lp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.02) * Math.exp(-t / 0.12) * 1.3);
  poner(buf, 0.03, 0.3, soplo(rnd, 1200 + rnd() * 500, 1.5, 0.01, 0.06));
  for (let m = 0; m < 4; m++) poner(buf, 0.08 + rnd() * 0.2, 0.15, burbuja(900 + rnd() * 1400, 1.7, 0.014));
  return bajar(pico(piscina(buf, 0.45, 0.88), -1), -4);
}


/* ================================================================ luces */

/** El chasquido de un panel que falla: el contacto, un zumbido corto y roto y otro clic. */
function chispa (k) {
  const rnd = D.azar(3830 + k);
  const buf = mono(1.4);
  poner(buf, 0.02, 0.02, (t) => (rnd() * 2 - 1) * Math.exp(-t * 700) * 1.2);
  let fase = 0;
  const dur = 0.07 + rnd() * 0.09;
  poner(buf, 0.03, dur, (t) => {
    fase = (fase + 120 / SR) % 1;
    const z = Math.sign(Math.sin(TAU * fase)) * 0.4 + D.sierra(fase, 120 / SR) * 0.3;
    const roto = rnd() < 0.08 ? 0 : 1;
    return z * roto * (0.6 + 0.4 * rnd()) * (1 - t / dur);
  });
  poner(buf, 0.03 + dur, 0.02, (t) => (rnd() * 2 - 1) * Math.exp(-t * 900) * 0.8);
  return bajar(pico(piscina(buf, 0.5, 0.85), -1), -3);
}

/* =========================================================== la Serpiente */

/** Agua en la garganta: burbujeo grave que modula la voz. */
function gargarismo (dur, semilla, g = 1) {
  const rnd = D.azar(semilla);
  const out = mono(dur);
  for (let n = 0; n < dur * 18; n++) poner(out, rnd() * (dur - 0.1), 0.12, burbuja(140 + rnd() * 220, 1.3, 0.03));
  for (let i = 0; i < out.length; i++) out[i] *= g;
  return out;
}

const GARGANTA_ENORME = [[[300, 240], 3.5], [[640, 520], 4], [[1500, 1150], 5]];

function grunido (k) {
  const dur = 2.4 + k * 0.4;
  const buf = mono(dur + 3);
  const r = rugido(dur, (t) => 58 + 14 * Math.sin(Math.PI * t / dur) - 6 * (t / dur) + (k === 2 ? 8 * Math.sin(t * 5) : 0), {
    semilla: 3900 + k, voces: 7, formantes: GARGANTA_ENORME, rotura: 0.85, ronquera: 0.4, sub: 0.7, cuerpo: 0.35
  });
  const env = (t) => suave(0, 0.35, t) * (1 - suave(dur - 0.8, dur, t));
  for (let i = 0; i < r.length; i++) r[i] *= env(i / SR);
  meter(buf, 0.1, r);
  meter(buf, 0.1, gargarismo(dur, 3910 + k, 0.35));
  return bajar(fuerte(piscina(lejos(buf, 1600), 0.9, 0.94), 1.8, -0.5), -1);
}

function respira (k) {
  const rnd = D.azar(3920 + k);
  const buf = mono(4.4);
  // inspira: aire que entra por una garganta rota, cada vez mas agudo
  const bpI = new D.Biquad('bp', 450, 2);
  poner(buf, 0.05, 1.3, (t) => {
    if (Math.round(t * SR) % 64 === 0) bpI.ajustar(420 + 520 * (t / 1.3), 2);
    return bpI.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.3) * (0.8 + 0.2 * Math.sin(TAU * 31 * t)) * 1.6;
  });
  const g1 = garganta(1.3, (t) => 74 + 10 * t, { semilla: 3925 + k, voces: 3, formantes: [[[420, 520], 5], [[1100, 1300], 6]], carraspeo: 0.8, aspereza: 0.6 });
  for (let i = 0; i < g1.length; i++) g1[i] *= Math.sin(Math.PI * i / g1.length) * 0.25;
  meter(buf, 0.05, g1);
  // espira: humeda, con estertor
  const lp = new D.Biquad('lp', 900, 0.8);
  poner(buf, 1.65, 1.8, (t) => lp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * Math.min(1, t / 1.8)) * (0.6 + 0.4 * Math.abs(Math.sin(TAU * 23 * t))) * 1.8);
  meter(buf, 1.7, gargarismo(1.5, 3928 + k, 0.5));
  return bajar(pico(piscina(buf, 0.6, 0.9), -1), -2);
}

/** El cuerpo arrastrandose por el azulejo y las garras de hueso tocando el suelo. */
function serpientePaso (k) {
  const rnd = D.azar(3940 + k);
  const buf = mono(1.8);
  const bp = new D.Biquad('bp', 1600, 0.9);
  poner(buf, 0.02, 0.7, (t) => {
    const grano = rnd() < 0.12 ? (rnd() * 2 - 1) * 3 : 0;
    return bp.paso((rnd() * 2 - 1) * 0.6 + grano) * Math.sin(Math.PI * t / 0.7) * 1.2;
  });
  for (let n = 0; n < 2 + (k % 2); n++) poner(buf, 0.08 + n * (0.12 + rnd() * 0.08), 0.05, soplo(rnd, 3000 + rnd() * 900, 5, 0.0005, 0.008));
  poner(buf, 0.02, 0.4, (t) => Math.sin(TAU * (62 + 10 * Math.exp(-t * 12)) * t) * Math.exp(-t / 0.12) * 1.2);
  return bajar(pico(piscina(buf, 0.6, 0.9), -1), -2);
}

function serpienteAgua (k) {
  const rnd = D.azar(3950 + k);
  const buf = mono(3.0);
  const lp = new D.Biquad('lp', 1400, 0.8);
  poner(buf, 0.05, 1.5, (t) => lp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.5) * (0.7 + 0.3 * Math.sin(t * 9)) * 1.6);
  poner(buf, 0.1, 1.2, (t) => Math.sin(TAU * (70 + 15 * Math.sin(t * 3)) * t) * Math.sin(Math.PI * t / 1.2) * 0.8);
  for (let m = 0; m < 8; m++) poner(buf, 0.2 + rnd() * 1.1, 0.2, burbuja(500 + rnd() * 1200, 1.7, 0.02));
  return bajar(pico(piscina(buf, 0.7, 0.9), -1), -2);
}

function serpienteGrito () {
  const buf = mono(4.2);
  const r = rugido(1.6, (t) => 88 + 70 * suave(0, 0.25, t) - 30 * suave(0.6, 1.6, t), {
    semilla: 3960, voces: 8, formantes: [[[520, 680], 4], [[1150, 1500], 5], [[2600, 3100], 6]], rotura: 0.7, ronquera: 0.5, sub: 0.5, cuerpo: 0.3
  });
  for (let i = 0; i < r.length; i++) r[i] *= suave(0, 0.06, i / SR) * (1 - suave(1.2, 1.6, i / SR));
  meter(buf, 0.05, r);
  meter(buf, 0.05, gargarismo(1.4, 3961, 0.3));
  return fuerte(piscina(buf, 0.75, 0.92), 2.4, -0.5);
}

function serpienteCaza (k) {
  const rnd = D.azar(3970 + k);
  const buf = mono(2.2);
  for (let n = 0; n < 3; n++) {
    const t0 = 0.05 + n * 0.36;
    poner(buf, t0, 0.3, soplo(rnd, 1800 + rnd() * 800, 1.2, 0.02, 0.09));
  }
  const g = garganta(1.1, (t) => 66 + 8 * Math.sin(t * 7), { semilla: 3975 + k, voces: 4, formantes: GARGANTA_ENORME, carraspeo: 0.85, aspereza: 0.45 });
  for (let i = 0; i < g.length; i++) g[i] *= Math.sin(Math.PI * i / g.length) * 0.5;
  meter(buf, 0.05, g);
  return bajar(pico(piscina(buf, 0.6, 0.9), -1), -1);
}

function serpienteAtaque () {
  const rnd = D.azar(3980);
  const buf = mono(3.2);
  // siseo que toma aire
  const bp = new D.Biquad('bp', 2400, 1.1);
  poner(buf, 0.0, 0.32, (t) => bp.paso(rnd() * 2 - 1) * suave(0, 0.28, t) * 1.4);
  // el rugido mientras se lanza
  const r = rugido(1.0, (t) => 96 + 64 * suave(0, 0.3, t), {
    semilla: 3981, voces: 8, formantes: [[[600, 760], 4], [[1300, 1600], 5], [[2800, 3300], 6]], rotura: 0.75, ronquera: 0.55, sub: 0.45, cuerpo: 0.3
  });
  for (let i = 0; i < r.length; i++) r[i] *= suave(0, 0.04, i / SR) * (1 - suave(0.8, 1.0, i / SR));
  meter(buf, 0.3, r);
  // la mandibula abriendose: crujido de hueso
  for (let n = 0; n < 5; n++) poner(buf, 0.28 + n * 0.035, 0.03, soplo(rnd, 1400 + rnd() * 800, 4, 0.0005, 0.006));
  return fuerte(piscina(buf, 0.55, 0.9), 2.2, -0.5);
}

function serpienteMordisco () {
  const rnd = D.azar(3990);
  const buf = mono(2.0);
  // el golpe de la mandibula, hueso contra hueso
  poner(buf, 0.02, 0.2, (t) => (Math.sin(TAU * 210 * t) * 0.8 + Math.sin(TAU * 530 * t) * 0.4) * Math.exp(-t / 0.035));
  poner(buf, 0.02, 0.05, (t) => (rnd() * 2 - 1) * Math.exp(-t * 220) * 1.4);
  // crujido y desgarro humedo
  for (let n = 0; n < 7; n++) poner(buf, 0.05 + rnd() * 0.25, 0.04, soplo(rnd, 900 + rnd() * 1800, 3, 0.0008, 0.012));
  const lp = new D.Biquad('lp', 1200, 0.8);
  poner(buf, 0.08, 0.5, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.15) * (0.6 + 0.4 * Math.sin(TAU * 27 * t)) * 1.5);
  meter(buf, 0.1, gargarismo(0.6, 3991, 0.6));
  return fuerte(piscina(buf, 0.45, 0.88), 2.4, -0.5);
}

/* ============================================================ misiones */

/** Un cuarto de vuelta del volante: el hierro oxidado que se resiste (roce a tirones) y el tubo que vibra. */
function valvulaGiro (k) {
  const rnd = D.azar(4100 + k);
  const buf = mono(0.9);
  // stick-slip: clics metalicos irregulares que se juntan y se separan
  let t = 0.02;
  while (t < 0.34) {
    poner(buf, t, 0.03, clicMetal(2200 + rnd() * 1600, 0.004 + rnd() * 0.004, rnd));
    t += 0.012 + rnd() * 0.03;
  }
  // el chirrido del eje
  const f0 = 620 + rnd() * 420;
  poner(buf, 0.03, 0.3, (u) => Math.sin(TAU * f0 * u + Math.sin(TAU * 23 * u) * 2.5) * Math.sin(Math.PI * u / 0.3) * (0.5 + 0.5 * rnd()) * 0.22);
  // y el tubo que suena
  poner(buf, 0.02, 0.8, (u) => (Math.sin(TAU * 312 * u) * 0.5 + Math.sin(TAU * 861 * u) * 0.25) * Math.exp(-u / 0.18) * 0.25);
  return bajar(pico(piscina(buf, 0.45, 0.8), -1), -3);
}

/** Clic metalico con ruido del generador dado. */
function clicMetal (f, caida, rnd) {
  const bp = new D.Biquad('bp', f, 7);
  return (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t / caida) * 2.5;
}

/**
 * La valvula cierra: el tope metalico y la presion que sale de golpe, como un freno de aire
 * (PSSSH seco: sube en un instante, silba y se corta), con la sala detras.
 */
function valvulaCierre () {
  const rnd = D.azar(4120);
  const buf = mono(2.6);
  // el tope: chasquido metalico y un golpe corto
  poner(buf, 0.02, 0.05, clicMetal(2400, 0.008, rnd));
  poner(buf, 0.02, 0.3, (t) => Math.sin(TAU * (140 + 60 * Math.exp(-t * 30)) * t) * Math.exp(-t / 0.05) * 0.9);
  // la descarga: ruido brillante con cuerpo, ataque instantaneo, meseta corta y caida rapida
  const hp = new D.Biquad('hp', 1500, 0.7);
  const bp = new D.Biquad('bp', 4200, 0.8);
  const bp2 = new D.Biquad('bp', 7000, 1.2);
  const dur = 0.95;
  poner(buf, 0.06, dur + 0.4, (t) => {
    const r = rnd() * 2 - 1;
    const env = Math.min(1, t / 0.008) * (t < 0.12 ? 1.3 - t * 2.5 : Math.exp(-(t - 0.12) / 0.28));
    const silbo = Math.sin(TAU * (2950 - 400 * t) * t) * 0.12; // la valvula que silba al vaciarse
    return (bp.paso(hp.paso(r)) * 2.6 + bp2.paso(r) * 1.4 + silbo) * env;
  });
  // el ultimo resoplido
  poner(buf, 0.06 + dur, 0.35, (t) => hp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.08) * 0.6);
  return fuerte(piscina(buf, 0.45, 0.88), 2.0, -0.5);
}

/** Lejos, una maquinaria grande que arranca: un motor que coge vueltas y pistones cada vez mas seguidos. */
function valvulaMaquinaria () {
  const rnd = D.azar(4140);
  const dur = 7.5;
  const buf = mono(dur);
  let fase = 0;
  poner(buf, 0, dur, (t) => {
    const f = 28 + 30 * suave(0.3, 4.5, t);
    fase += f / SR;
    const motor = Math.sin(TAU * fase) + Math.sin(TAU * fase * 2) * 0.5 + Math.sin(TAU * fase * 3.02) * 0.3 + Math.sin(TAU * fase * 5.1) * 0.12;
    return motor * suave(0, 1.2, t) * (1 - suave(dur - 1.2, dur, t)) * 0.45;
  });
  let t = 0.6;
  let paso = 0.9;
  while (t < dur - 0.6) {
    poner(buf, t, 0.4, (u) => Math.sin(TAU * (70 + 40 * Math.exp(-u * 20)) * u) * Math.exp(-u / 0.07) * 0.8);
    poner(buf, t, 0.05, clicMetal(900 + rnd() * 500, 0.012, rnd));
    t += paso;
    paso = Math.max(0.28, paso * 0.86);
  }
  // un quejido de metal a mitad
  poner(buf, 2.4, 1.4, (u) => Math.sin(TAU * (380 - 90 * u) * u + Math.sin(TAU * 5 * u) * 3) * Math.sin(Math.PI * u / 1.4) * 0.25);
  return bajar(pico(lejos(piscina(buf, 0.9, 0.95), 900), -1), -2);
}

/** Coger una tarjeta: el plastico que roza y un clic. */
function tarjetaCoger () {
  const rnd = D.azar(4160);
  const buf = mono(0.7);
  const hp = new D.Biquad('hp', 1800, 0.8);
  poner(buf, 0.02, 0.14, (t) => hp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.14) * 0.9);
  poner(buf, 0.16, 0.03, clicMetal(3400, 0.004, rnd));
  poner(buf, 0.2, 0.25, (t) => Math.sin(TAU * 1320 * t) * Math.exp(-t / 0.05) * 0.3);
  return bajar(pico(piscina(buf, 0.35, 0.7), -1), -2);
}

/** El lector se traga la tarjeta: la ranura, dos pitidos y el rele del cierre que salta. */
function lectorAcepta () {
  const rnd = D.azar(4180);
  const buf = mono(1.6);
  const hp = new D.Biquad('hp', 1500, 0.8);
  poner(buf, 0.02, 0.22, (t) => hp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.22) * 0.7);
  const pitido = (f) => (t) => Math.sign(Math.sin(TAU * f * t)) * 0.18 * Math.min(1, t / 0.004) * (t < 0.11 ? 1 : Math.exp(-(t - 0.11) / 0.01));
  poner(buf, 0.3, 0.14, pitido(1760));
  poner(buf, 0.46, 0.14, pitido(2350));
  poner(buf, 0.66, 0.4, (t) => Math.sin(TAU * (130 + 50 * Math.exp(-t * 30)) * t) * Math.exp(-t / 0.06));
  poner(buf, 0.66, 0.05, clicMetal(1400, 0.008, rnd));
  return bajar(pico(piscina(buf, 0.4, 0.8), -1), -1);
}

/** La salida se abre: los cierres que saltan uno tras otro, el aire que entra y la puerta pesada que se arrastra. */
function puertaSalida () {
  const rnd = D.azar(4200);
  const dur = 6.0;
  const buf = mono(dur);
  for (let k = 0; k < 3; k++) {
    poner(buf, 0.1 + k * 0.35, 0.5, (t) => Math.sin(TAU * (80 + 70 * Math.exp(-t * 25)) * t) * Math.exp(-t / 0.1) * 1.1);
    poner(buf, 0.1 + k * 0.35, 0.06, clicMetal(1100 + k * 300, 0.015, rnd));
  }
  // zumbido electrico del electroiman que se suelta
  poner(buf, 0.05, 1.1, (t) => (Math.sign(Math.sin(TAU * 100 * t)) * 0.15 + Math.sin(TAU * 200 * t) * 0.1) * Math.exp(-t / 0.4));
  // la puerta: chapa que se arrastra (ruido grave con resonancias que cambian) y el golpe al llegar al tope
  const bp = new D.Biquad('bp', 180, 3);
  const bp2 = new D.Biquad('bp', 640, 5);
  poner(buf, 1.3, 3.4, (t) => {
    const r = rnd() * 2 - 1;
    return (bp.paso(r) * 1.4 + bp2.paso(r) * (0.4 + 0.3 * Math.sin(TAU * 1.7 * t))) * Math.sin(Math.PI * Math.min(1, t / 3.4)) * (0.8 + 0.2 * Math.sin(TAU * 11 * t));
  });
  poner(buf, 4.7, 1.2, (t) => (Math.sin(TAU * (60 + 40 * Math.exp(-t * 20)) * t) + Math.sin(TAU * 233 * t) * 0.4 * Math.exp(-t / 0.3)) * Math.exp(-t / 0.25));
  // el aire de detras, frio
  const lp = new D.Biquad('lp', 700, 0.7);
  poner(buf, 1.4, 4.4, (t) => lp.paso(rnd() * 2 - 1) * suave(0, 1.5, t) * (1 - suave(3.2, 4.4, t)) * 0.6);
  return fuerte(piscina(buf, 0.7, 0.93), 1.8, -0.5);
}

/* ================================================================ todos */

const SONIDOS = [];
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_gota${k + 1}`, () => gota(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_agua${k + 1}`, () => agua(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_olas${k + 1}`, () => olas(k)]);
for (let k = 0; k < 2; k++) SONIDOS.push([`piscinas_zambullida${k + 1}`, () => zambullida(k)]);
for (let k = 0; k < 2; k++) SONIDOS.push([`piscinas_chapoteo${k + 1}`, () => chapoteo(k)]);
for (let k = 0; k < 5; k++) SONIDOS.push([`piscinas_tuberia${k + 1}`, () => tuberia(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_grave${k + 1}`, () => grave(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_voz${k + 1}`, () => voz(k)]);
for (let k = 0; k < 6; k++) SONIDOS.push([`piscinas_paso${k + 1}`, () => paso(k)]);
for (let k = 0; k < 5; k++) SONIDOS.push([`piscinas_paso_agua${k + 1}`, () => pasoAgua(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_nado${k + 1}`, () => nado(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`piscinas_chispa${k + 1}`, () => chispa(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`serpiente_grunido${k + 1}`, () => grunido(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`serpiente_respira${k + 1}`, () => respira(k)]);
for (let k = 0; k < 4; k++) SONIDOS.push([`serpiente_paso${k + 1}`, () => serpientePaso(k)]);
for (let k = 0; k < 3; k++) SONIDOS.push([`serpiente_agua${k + 1}`, () => serpienteAgua(k)]);
SONIDOS.push(['serpiente_grito', serpienteGrito]);
for (let k = 0; k < 2; k++) SONIDOS.push([`serpiente_caza${k + 1}`, () => serpienteCaza(k)]);
SONIDOS.push(['serpiente_ataque', serpienteAtaque]);
SONIDOS.push(['serpiente_mordisco', serpienteMordisco]);
for (let k = 0; k < 3; k++) SONIDOS.push([`valvula_giro${k + 1}`, () => valvulaGiro(k)]);
SONIDOS.push(['valvula_cierre', valvulaCierre]);
SONIDOS.push(['valvula_maquinaria', valvulaMaquinaria]);
SONIDOS.push(['tarjeta_coger', tarjetaCoger]);
SONIDOS.push(['lector_acepta', lectorAcepta]);
SONIDOS.push(['puerta_salida', puertaSalida]);

/**
 * Entradas de sounds.json: evento -> [prefijo, cuantos, distancia de atenuacion]. La
 * distancia es hasta donde se oye a volumen 1 (con mas volumen, mas lejos).
 */
const EVENTOS = {
  'piscinas.gota': ['piscinas_gota', 3, 24],
  'piscinas.agua': ['piscinas_agua', 3, 24],
  'piscinas.orilla': ['piscinas_olas', 3, 20],
  'piscinas.zambullida': ['piscinas_zambullida', 2, 32],
  'piscinas.chapoteo': ['piscinas_chapoteo', 2, 40],
  'piscinas.tuberia': ['piscinas_tuberia', 5, 40],
  'piscinas.grave': ['piscinas_grave', 3, 96],
  'piscinas.voz': ['piscinas_voz', 3, 72],
  'piscinas.paso': ['piscinas_paso', 6, 16],
  'piscinas.paso_agua': ['piscinas_paso_agua', 5, 16],
  'piscinas.nado': ['piscinas_nado', 3, 16],
  'piscinas.chispa': ['piscinas_chispa', 3, 24],
  'serpiente.grunido': ['serpiente_grunido', 3, 128],
  'serpiente.respira': ['serpiente_respira', 3, 40],
  'serpiente.paso': ['serpiente_paso', 4, 32],
  'serpiente.agua': ['serpiente_agua', 3, 40],
  'serpiente.grito': ['serpiente_grito', 1, 96],
  'serpiente.caza': ['serpiente_caza', 2, 48],
  'serpiente.ataque': ['serpiente_ataque', 1, 48],
  'serpiente.mordisco': ['serpiente_mordisco', 1, 32],
  'valvula.giro': ['valvula_giro', 3, 16],
  'valvula.cierre': ['valvula_cierre', 1, 32],
  'valvula.maquinaria': ['valvula_maquinaria', 1, 16],
  'tarjeta.coger': ['tarjeta_coger', 1, 12],
  'lector.acepta': ['lector_acepta', 1, 16],
  'puerta.salida': ['puerta_salida', 1, 96]
};

if (require.main === module) (async () => {
  const prefijo = process.argv[2] || '';
  console.log('Sonidos de las Piscinas y la Serpiente');
  for (const [nombre, fn] of SONIDOS) {
    if (!nombre.startsWith(prefijo)) continue;
    await guardarMono(nombre, fn());
  }
  // sounds.json: solo se anaden (o rehacen) estas entradas, lo demas se respeta
  const f = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(f, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  for (const [evento, [pre, n, distancia]] of Object.entries(EVENTOS)) {
    const sonidos = [];
    for (let k = 1; k <= n; k++) sonidos.push({ name: `backrooms_evento:${n === 1 ? pre : pre + k}`, attenuation_distance: distancia });
    j[evento] = { sounds: sonidos };
  }
  // las Piscinas ya no tienen ambiente en bucle (lo que suena va suelto, de vez en cuando)
  delete j['piscinas.ambiente'];
  // el susurro con el nombre del jugador se sintetiza en su cliente (SusurroNombre): aqui solo
  // el hueco, un sonido vacio en streaming que el mod rellena (FabricSoundInstance#getAudioStream)
  j['piscinas.susurro'] = { sounds: [{ name: 'fabric-sound-api-v1:empty', stream: true }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(f, out);
  console.log('  sounds.json');
})();
