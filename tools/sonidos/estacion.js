#!/usr/bin/env node
'use strict';
/**
 * Los sonidos de la Estacion Electrica (Nivel 3) y del Amasijo. Sintetizados aqui, en mono
 * (para que suenen en 3D) salvo la maquinaria de fondo. La voz de la radio sale de
 * tools/sonidos/voces/estacion_radio.wav (Higgsfield) si esta; si no, la radio es solo ruido.
 *
 *   node tools/sonidos/estacion.js [prefijo]   (sin prefijo, todos)
 *
 *  Ambiente (AmbienteEstacion, en el cliente)
 *   estacion_ambiente      la maquinaria en bucle: transformadores a 50 Hz, ventilacion y un
 *                          tictac metalico lejano (estereo, sin posicion)
 *   estacion_estruendo1-3  algo metalico que cae lejos, en una nave enorme
 *   estacion_chispas1-3    el chisporroteo de un panel roto
 *  La corriente
 *   estacion_arranca       una central que vuelve: contactores que caen y el zumbido que sube
 *   estacion_fallo         cortocircuito: estallido, chispas y todo que se apaga con un quejido
 *   estacion_pasarela      los focos de la pasarela, uno tras otro: contactores que caen
 *  Lo que se hace
 *   estacion_coger         coger un material: un roce y un tintineo
 *   estacion_reparar       chispazos, la carraca del destornillador y el rele que entra
 *   estacion_persiana      la persiana metalica que sube traqueteando
 *   estacion_fusible       el fusible que encaja y el zumbido que vuelve a su circuito
 *   estacion_palanca       la palanca grande del interruptor
 *   estacion_radio         la radio del acceso: sintonia, ruido, la voz por el altavoz y el corte
 *  Los pasos (el SoundType de los suelos de la estacion)
 *   estacion_paso1-6       pasos sobre hormigon, con un poco de eco de nave (menos que las Piscinas)
 *   estacion_paso_chapa1-6 pasos sobre chapa y rejilla: el golpe y la chapa que vibra un poco
 *  El Amasijo
 *   amasijo_pasos1-4       carne que se arrastra y se apoya: blando, humedo, con algo de cola
 *   amasijo_grito          un coro de gargantas que gritan a la vez, con algo que burbujea
 *   amasijo_respira1-2     resuello humedo de varias bocas
 *   amasijo_ataque         el mordisco: crujido humedo y un rugido ahogado
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { mono, poner, reverb, pico, guardarMono, guardarEst, SALIDA, ruidoBp, golpe, clic, garganta, rugido, ecos, fuerte } = E;
const VOZ_RADIO = path.join(__dirname, 'voces', 'estacion_radio.wav');

/** Una nave industrial: reverberacion larga y metalica. */
const nave = (buf, mezcla = 0.4, tam = 0.85) => reverb(buf, tam, mezcla, { amortiguacion: 0.3, predelayMs: 25 });

/** Filtra un buffer con un biquad. */
function filtrar (buf, tipo, f, q = 0.7, g = 0) {
  const b = new D.Biquad(tipo, f, q, g);
  const out = new Float32Array(buf.length);
  for (let i = 0; i < buf.length; i++) out[i] = b.paso(buf[i]);
  return out;
}

/** Mezcla `src` en `buf` desde t0 con ganancia. */
function pegar (buf, src, t0, g = 1) {
  const i0 = Math.round(t0 * SR);
  for (let i = 0; i < src.length && i0 + i < buf.length; i++) if (i0 + i >= 0) buf[i0 + i] += src[i] * g;
}

/** Normaliza sin fundidos (para los bucles). */
function nivel (buf, picoDb = -3) {
  let m = 0;
  for (const v of buf) m = Math.max(m, Math.abs(v));
  const g = Math.pow(10, picoDb / 20) / (m || 1);
  for (let i = 0; i < buf.length; i++) buf[i] *= g;
  return buf;
}

/** Un golpe de chapa: varios modos inarmonicos que suenan a metal hueco. */
function chapa (f, caida, semilla, brillo = 1) {
  const rnd = D.azar(semilla);
  const modos = [1, 1.47, 2.09, 2.56, 3.42, 4.7].map((k) => [f * k * (1 + (rnd() - 0.5) * 0.04), 1 / k ** 0.7, rnd() * TAU]);
  const lp = new D.Biquad('lp', 3000 * brillo, 0.7);
  return (t) => {
    let v = 0;
    for (const [fk, a, ph] of modos) v += Math.sin(TAU * fk * t + ph) * a * Math.exp(-t * (fk / f) / caida);
    return v * 0.4 + lp.paso(rnd() * 2 - 1) * Math.exp(-t * 80) * 0.6;
  };
}

/** Chisporroteo: chasquidos de ruido brillante a rafagas irregulares. */
function chispas (dur, densidad, semilla) {
  const rnd = D.azar(semilla);
  const out = mono(dur);
  let t = 0;
  while (t < dur) {
    const fuerza = 0.3 + rnd() * 0.7;
    const f = 2500 + rnd() * 4000;
    poner(out, t, 0.03 + rnd() * 0.05, (() => { const c = clic(f, 0.004 + rnd() * 0.01, Math.floor(rnd() * 1e6)); return (u) => c(u) * fuerza; })());
    // a veces, una rafaga
    t += rnd() < 0.25 ? 0.008 + rnd() * 0.02 : (0.02 + rnd() * 0.12) / densidad;
  }
  // el zumbido del arco por debajo
  const rz = D.azar(semilla + 9);
  poner(out, 0, dur, (u) => (Math.sin(TAU * 100 * u) * 0.3 + (rz() - 0.5) * 0.3) * 0.15 * (rz() < 0.3 ? 1 : 0.2));
  return out;
}

/* ============================================================== ambiente */

function ambiente () {
  const dur = 17;
  const [l, r] = D.estereo(dur);
  const rl = D.azar(301); const rr = D.azar(302);
  const bl = D.marron(rl); const br = D.marron(rr);
  const lpl = new D.Biquad('lp', 380, 0.7); const lpr = new D.Biquad('lp', 380, 0.7);
  const ticks = D.azar(303);
  const tic = [];
  for (let t = 0.5; t < dur; t += 1.7 + ticks() * 2.5) tic.push([t, ticks() < 0.5 ? -0.6 : 0.6, 500 + ticks() * 700]);
  for (let i = 0; i < l.length; i++) {
    const t = i / SR;
    // el zumbido de los transformadores (50 Hz y armonicos, que respira despacio)
    const zum = (Math.sin(TAU * 50 * t) * 0.6 + Math.sin(TAU * 100 * t) + Math.sin(TAU * 150 * t) * 0.35 + Math.sin(TAU * 300 * t) * 0.12) * 0.05 * (0.85 + 0.15 * Math.sin(TAU * t / 5.3));
    const vent = 0.85 + 0.15 * Math.sin(TAU * t / 3.4);
    l[i] = lpl.paso(bl()) * 0.75 * vent + zum;
    r[i] = lpr.paso(br()) * 0.75 * vent + zum * 0.9;
  }
  // un tictac metalico lejano (la estructura que se dilata)
  for (const [t0, pan, f] of tic) {
    const c = chapa(f, 0.12, Math.floor(t0 * 1000));
    const n = Math.round(0.4 * SR);
    const i0 = Math.round(t0 * SR);
    for (let k = 0; k < n && i0 + k < l.length; k++) {
      const v = c(k / SR) * 0.05;
      l[i0 + k] += v * (1 - pan);
      r[i0 + k] += v * (1 + pan);
    }
  }
  E.reverbEst([l, r], 0.8, 0.35);
  return D.masterizar(D.cerrarBucle([l, r], dur - 1), { rmsDb: -28, picoDb: -6 });
}

function estruendo (semilla) {
  const out = mono(4);
  const rnd = D.azar(semilla);
  // una pieza que cae: golpe grande, rebotes y algo que rueda
  poner(out, 0.0, 2.5, chapa(70 + rnd() * 40, 0.5, semilla));
  poner(out, 0.0, 1.0, golpe(90, 40, 0.25, 0.6, semilla + 1));
  let t = 0.35 + rnd() * 0.2;
  for (let k = 0; k < 4; k++) {
    const g = 0.5 * Math.pow(0.6, k);
    poner(out, t, 1.2, (() => { const c = chapa(160 + rnd() * 120, 0.2, semilla + 10 + k); return (u) => c(u) * g; })());
    t += 0.18 + rnd() * 0.15;
  }
  const lejos = filtrar(out, 'lp', 1400);
  return pico(nave(lejos, 0.7, 0.92), -2);
}

/* ============================================================== la corriente */

/** Una central que vuelve: tres contactores grandes y el zumbido de 50 Hz que sube. */
function arranca () {
  const out = mono(3.5);
  for (const [t0, f] of [[0.0, 150], [0.22, 130], [0.5, 120]]) poner(out, t0, 0.4, golpe(f, 60, 0.06, 0.9, Math.floor(t0 * 100) + 321));
  poner(out, 0.0, 0.5, chapa(260, 0.08, 322));
  // el zumbido: 100 Hz y sus armonicos, que sube y se queda
  poner(out, 0.5, 3.0, (t) => {
    const env = Math.min(1, t * 1.5) * Math.min(1, (3.0 - t) * 2);
    return (Math.sin(TAU * 100 * t) * 0.3 + Math.sin(TAU * 200 * t) * 0.12 + Math.sin(TAU * 300 * t) * 0.05) * env;
  });
  pegar(out, chispas(0.4, 2, 323), 0.05, 0.4);
  return pico(nave(out, 0.3), -1);
}

function fallo () {
  const out = mono(3.2);
  // el estallido
  poner(out, 0, 1.2, golpe(160, 40, 0.18, 1.2, 351));
  pegar(out, chispas(1.6, 3, 352), 0, 1.2);
  // la sirena electrica que baja y los reles que caen
  let f = 0;
  poner(out, 0.3, 2.2, (t) => { f += (900 - t * 380) / SR; return Math.tanh(Math.sin(TAU * f) * 3) * 0.2 * Math.min(1, t * 8) * (1 - t / 2.2); });
  for (const t0 of [0.25, 0.4, 0.6, 0.75]) poner(out, t0, 0.3, golpe(120, 50, 0.04, 0.8, Math.floor(t0 * 100)));
  return pico(nave(out, 0.35), -1);
}

function pasarela () {
  const out = mono(5);
  // cada foco: el golpe del contactor y su zumbido que entra (lejos los ultimos)
  for (let k = 0; k < 8; k++) {
    const t0 = k * 0.45;
    const g = 1 - k * 0.07;
    poner(out, t0, 0.5, (() => { const gg = golpe(110, 55, 0.06, 0.9, 360 + k); return (t) => gg(t) * g; })());
    poner(out, t0 + 0.05, 5 - t0 - 0.05, (t) => Math.sin(TAU * 100 * t + k) * 0.03 * g * Math.min(1, t * 4));
  }
  return pico(nave(out, 0.45), -1);
}

/* ============================================================== lo que se hace */

function coger () {
  const out = mono(0.5);
  pegar(out, ruidoBp(0.2, 2500, 0.8), 0, 0.3);
  poner(out, 0.08, 0.4, chapa(1400, 0.05, 381, 1.5));
  return pico(out, -3);
}

function reparar () {
  const out = mono(2.6);
  pegar(out, chispas(0.6, 2, 391), 0, 0.8);
  // la carraca del destornillador
  for (let k = 0; k < 9; k++) poner(out, 0.55 + k * 0.07, 0.04, clic(3200, 0.004, 392 + k));
  // el panel que se cierra y el rele que entra; el zumbido vuelve
  poner(out, 1.3, 0.5, chapa(260, 0.1, 393));
  poner(out, 1.7, 0.4, golpe(140, 60, 0.05, 0.8, 394));
  poner(out, 1.75, 0.85, (t) => Math.sin(TAU * 100 * t) * 0.2 * Math.min(1, t * 6) * Math.min(1, (0.85 - t) * 5));
  return pico(reverb(out, 0.5, 0.2), -1);
}

function persiana () {
  const out = mono(3.4);
  const rnd = D.azar(401);
  // las lamas que traquetean al enrollarse, cada vez mas deprisa al principio
  let t = 0.05;
  while (t < 2.9) {
    poner(out, t, 0.15, (() => { const c = chapa(500 + rnd() * 300, 0.04, Math.floor(rnd() * 1e6)); return (u) => c(u) * 0.5; })());
    t += 0.045 + rnd() * 0.02;
  }
  // el motor
  poner(out, 0, 3.0, (u) => (Math.sin(TAU * 90 * u) * 0.2 + (rnd() - 0.5) * 0.08) * Math.min(1, u * 5) * Math.min(1, (3.0 - u) * 5));
  // el tope de arriba
  poner(out, 2.95, 0.4, golpe(120, 60, 0.07, 0.9, 402));
  return pico(nave(filtrar(out, 'lp', 4000), 0.35), -1);
}

function fusible () {
  const out = mono(1.3);
  poner(out, 0, 0.2, clic(1800, 0.01, 411));
  poner(out, 0.06, 0.4, golpe(160, 70, 0.05, 0.6, 412));
  poner(out, 0.12, 1.1, (t) => Math.sin(TAU * 100 * t) * 0.25 * Math.min(1, t * 5) * Math.min(1, (1.1 - t) * 4));
  pegar(out, chispas(0.15, 4, 413), 0.08, 0.4);
  return pico(reverb(out, 0.4, 0.2), -2);
}

function palanca () {
  const out = mono(0.8);
  poner(out, 0, 0.2, chapa(600, 0.04, 421));
  poner(out, 0.12, 0.6, golpe(120, 55, 0.06, 0.9, 422));
  poner(out, 0.12, 0.6, chapa(240, 0.08, 423));
  return pico(reverb(out, 0.5, 0.25), -1);
}

/** La radio: la sintonia, el ruido, la voz por un altavoz pequeno que va y viene, y el corte. */
function radio () {
  let voz = null;
  if (fs.existsSync(VOZ_RADIO)) {
    const ff = process.env.FFMPEG || 'ffmpeg';
    const b = execFileSync(ff, ['-v', 'error', '-i', VOZ_RADIO, '-af', 'aresample=44100', '-ac', '1', '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
    voz = new Float32Array(b.buffer, b.byteOffset, b.length / 4).slice();
  }
  const durVoz = voz ? voz.length / SR : 0;
  const dur = 1.6 + durVoz + 1.2;
  const out = mono(dur);
  const rnd = D.azar(431);
  const hp = new D.Biquad('hp', 900, 0.7);
  // el ruido de fondo de la radio, que sube y baja (mas fuerte al principio y al final)
  poner(out, 0, dur, (t) => {
    const fuerte = t < 1.2 ? 1 : t > dur - 1.1 ? 1.2 : 0.25 + 0.15 * Math.sin(TAU * 0.7 * t) + (rnd() < 0.002 ? 0.8 : 0);
    return hp.paso(rnd() * 2 - 1) * 0.25 * fuerte;
  });
  // la sintonia: un silbido que barre
  let f = 0;
  poner(out, 0.1, 1.0, (t) => { f += (2400 - t * 1800) / SR; return Math.sin(TAU * f) * 0.12 * (1 - t); });
  if (voz) {
    // por un altavoz pequeno: sin graves ni agudos, saturada, y a ratos se va
    const bp1 = new D.Biquad('hp', 450, 0.8);
    const bp2 = new D.Biquad('lp', 2800, 0.8);
    const va = D.azar(432);
    let g = 1;
    const v = new Float32Array(voz.length);
    for (let i = 0; i < voz.length; i++) {
      if (i % 2205 === 0) g = va() < 0.12 ? 0.25 : 1;
      v[i] = Math.tanh(bp2.paso(bp1.paso(voz[i])) * 3) * 0.5 * g;
    }
    pegar(out, v, 1.3, 1);
  }
  // el corte: chasquido, un pitido y nada
  const tc = dur - 1.1;
  poner(out, tc, 0.15, clic(2000, 0.02, 433));
  poner(out, tc + 0.05, 0.5, (t) => Math.sin(TAU * 1000 * t) * 0.2 * Math.min(1, t * 200) * (1 - t / 0.5));
  for (let i = Math.round((tc + 0.6) * SR); i < out.length; i++) out[i] *= 0.15;
  return pico(out, -1);
}

/* ============================================================== los pasos */

/** Un eco corto de nave: menos que en las Piscinas (que es todo azulejo y agua). */
const ecoPaso = (buf) => ecos(reverb(buf, 0.62, 0.22, { amortiguacion: 0.45, predelayMs: 18 }), [0.16, 0.33], 0.16, 2200);

/** Un paso sobre hormigon: el tacon sordo, la suela que roza y la nave que responde un poco. */
function pasoEstacion (k) {
  const rnd = D.azar(5100 + k);
  const buf = mono(1.2);
  const g = 0.85 + rnd() * 0.3;
  const lp = new D.Biquad('lp', 700 + rnd() * 350, 0.7);
  poner(buf, 0.01, 0.12, (t) => (lp.paso(rnd() * 2 - 1) * 1.3 + Math.sin(TAU * (95 + rnd() * 30) * t) * 0.7) * Math.min(1, t / 0.002) * Math.exp(-t / 0.026) * g);
  if (rnd() < 0.7) {
    const bp = new D.Biquad('bp', 2400 + rnd() * 1500, 1.8);
    poner(buf, 0.045 + rnd() * 0.03, 0.08, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.08) * 0.35);
  }
  poner(buf, 0.07, 0.08, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.02) * 0.45 * g);
  return pico(ecoPaso(buf), -1);
}

/** Un paso sobre chapa: el golpe, la plancha que vibra grave y el eco. */
function pasoChapa (k) {
  const rnd = D.azar(5200 + k);
  const buf = mono(1.2);
  const g = 0.85 + rnd() * 0.3;
  const lp = new D.Biquad('lp', 1100 + rnd() * 400, 0.7);
  poner(buf, 0.01, 0.1, (t) => lp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.0015) * Math.exp(-t / 0.02) * 1.3 * g);
  poner(buf, 0.01, 0.5, (() => { const c = chapa(140 + rnd() * 90, 0.09, 5210 + k, 0.7); return (t) => c(t) * 0.45 * g; })());
  if (rnd() < 0.5) poner(buf, 0.06, 0.12, (() => { const c = chapa(600 + rnd() * 400, 0.03, 5220 + k, 1.2); return (t) => c(t) * 0.12; })());
  return pico(ecoPaso(buf), -1);
}

/* ============================================================== el Amasijo */

/**
 * El paso del Amasijo: una masa blanda que se apoya y se arrastra. Golpes graves y lentos de
 * ataque (nada de chasquidos), un chof humedo que se despega, un roce de carne y algo de cola de
 * nave, para que no suene seco.
 */
function amasijoPasos (semilla) {
  const out = mono(1.1);
  const rnd = D.azar(semilla);
  // el apoyo: dos o tres masas que caen casi juntas, graves y blandas
  for (let k = 0; k < 3; k++) {
    const t0 = k * (0.05 + rnd() * 0.06);
    const g = (0.6 + rnd() * 0.4) * (1 - k * 0.2);
    const lp = new D.Biquad('lp', 260 + rnd() * 160, 0.8);
    const r2 = D.azar(semilla * 7 + k);
    const f0 = 55 + rnd() * 25;
    poner(out, t0, 0.3, (t) => (lp.paso(r2() * 2 - 1) * 1.6 + Math.sin(TAU * f0 * t) * 0.9) * Math.min(1, t / 0.012) * Math.exp(-t / 0.07) * g);
  }
  // el chof que se despega: ruido medio que sube de tono y se corta suave
  const wet = new D.Biquad('bp', 700, 2.2);
  poner(out, 0.08 + rnd() * 0.05, 0.22, (t) => {
    wet.ajustar ? wet.ajustar(700 + t * 2500) : 0;
    return wet.paso(rnd() * 2 - 1) * Math.sin(Math.PI * Math.min(1, t / 0.22)) * 0.7;
  });
  // unas burbujas pequenas, humedas
  for (let m = 0; m < 4; m++) {
    const f0 = 250 + rnd() * 350;
    poner(out, 0.1 + rnd() * 0.25, 0.08, (u) => Math.sin(TAU * f0 * (1 + u * 6) * u) * Math.exp(-u * 35) * 0.3);
  }
  // el roce de la carne contra el suelo
  const roce = new D.Biquad('bp', 1200, 0.9);
  poner(out, 0.12, 0.35, (t) => roce.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.35) * 0.18);
  return pico(reverb(filtrar(out, 'lp', 3500), 0.55, 0.2, { amortiguacion: 0.5, predelayMs: 15 }), -1);
}

function amasijoGrito () {
  const dur = 2.4;
  const out = mono(dur);
  // un coro de gargantas: cada una a su tono, todas abriendo de "o" a "a"
  for (let k = 0; k < 4; k++) {
    const base = [210, 260, 330, 170][k];
    const g = garganta(dur - 0.1 * k, (t) => base * (1 + Math.min(1, t * 3) * 0.4) * (1 - Math.max(0, t - 1.6) * 0.3), {
      voces: 4, semilla: 440 + k, carraspeo: 0.6, aspereza: 0.35,
      formantes: [[[500, 850], 6], [[900, 1300], 7], [[2400, 2800], 9]]
    });
    pegar(out, g, 0.05 * k, 0.6);
  }
  // por debajo, un rugido grave
  pegar(out, rugido(dur, (t) => 85 + Math.sin(t * 3) * 10, { semilla: 445, formantes: [[[300, 500], 4], [[700, 900], 5]], rotura: 0.7 }), 0, 0.7);
  // algo que burbujea (humedo)
  const rnd = D.azar(446);
  for (let t = 0.1; t < dur - 0.3; t += 0.04 + rnd() * 0.08) {
    const f0 = 300 + rnd() * 500;
    poner(out, t, 0.05, (u) => Math.sin(TAU * (f0 + u * 4000) * u) * Math.exp(-u * 60) * 0.25);
  }
  return fuerte(ecos(out, [0.13, 0.29], 0.35, 1800), 2.2, -0.5);
}

function amasijoRespira (semilla) {
  const dur = 2.2;
  const out = mono(dur);
  const rnd = D.azar(semilla);
  // varias bocas: cada una aspira y suelta, desfasadas, con un gorgoteo
  for (let k = 0; k < 3; k++) {
    const t0 = k * 0.25 + rnd() * 0.1;
    const f = 600 + rnd() * 700;
    const bp = new D.Biquad('bp', f, 2.5);
    const r2 = D.azar(semilla * 3 + k);
    poner(out, t0, 1.8, (t) => {
      const env = Math.sin(Math.PI * Math.min(1, t / 0.8)) * (t < 0.8 ? 0.7 : 0) + (t >= 0.9 ? Math.sin(Math.PI * Math.min(1, (t - 0.9) / 0.9)) : 0);
      const gorgo = 0.6 + 0.4 * Math.sin(TAU * (28 + k * 9) * t) * (r2() < 0.5 ? 1 : 0.5);
      return bp.paso(r2() * 2 - 1) * env * gorgo * 1.2;
    });
  }
  return pico(reverb(out, 0.4, 0.15), -2);
}

function amasijoAtaque () {
  const out = mono(1.3);
  const rnd = D.azar(451);
  // el mordisco: crujido (muchos chasquidos secos) y el golpe humedo
  for (let k = 0; k < 14; k++) poner(out, 0.02 + rnd() * 0.15, 0.03, clic(1200 + rnd() * 2000, 0.005, 452 + k));
  poner(out, 0, 0.4, golpe(120, 50, 0.08, 1.4, 453));
  const wet = new D.Biquad('bp', 1100, 2);
  poner(out, 0.05, 0.4, (t) => wet.paso(rnd() * 2 - 1) * Math.exp(-t * 9) * 1.2);
  pegar(out, rugido(1.0, (t) => 95 - t * 25, { semilla: 454, formantes: [[[350, 450], 4], [[800, 700], 5]], rotura: 0.8 }), 0.15, 0.6);
  return fuerte(out, 2, -0.5);
}

/* ============================================================== todo */

const SONIDOS = [
  ['estacion_estruendo1', () => estruendo(461)], ['estacion_estruendo2', () => estruendo(462)], ['estacion_estruendo3', () => estruendo(463)],
  ['estacion_chispas1', () => pico(chispas(0.5, 2, 471), -3)], ['estacion_chispas2', () => pico(chispas(0.4, 1.4, 472), -3)], ['estacion_chispas3', () => pico(chispas(0.6, 3, 473), -3)],
  ['estacion_arranca', arranca],
  ['estacion_fallo', fallo], ['estacion_pasarela', pasarela], ['estacion_coger', coger],
  ['estacion_reparar', reparar], ['estacion_persiana', persiana], ['estacion_fusible', fusible], ['estacion_palanca', palanca],
  ['estacion_radio', radio],
  ['amasijo_pasos1', () => amasijoPasos(481)], ['amasijo_pasos2', () => amasijoPasos(482)], ['amasijo_pasos3', () => amasijoPasos(483)],
  ['amasijo_pasos4', () => amasijoPasos(484)],
  ['amasijo_grito', amasijoGrito],
  ['amasijo_respira1', () => amasijoRespira(491)], ['amasijo_respira2', () => amasijoRespira(492)],
  ['amasijo_ataque', amasijoAtaque]
];
for (let k = 0; k < 6; k++) SONIDOS.push([`estacion_paso${k + 1}`, () => pasoEstacion(k)]);
for (let k = 0; k < 6; k++) SONIDOS.push([`estacion_paso_chapa${k + 1}`, () => pasoChapa(k)]);

/** Entradas de sounds.json: evento -> [archivos, distancia de atenuacion]. */
const EVENTOS = {
  'estacion.ambiente': [['estacion_ambiente'], 16],
  'estacion.estruendo': [['estacion_estruendo1', 'estacion_estruendo2', 'estacion_estruendo3'], 80],
  'estacion.chispas': [['estacion_chispas1', 'estacion_chispas2', 'estacion_chispas3'], 12],
  'estacion.arranca': [['estacion_arranca'], 64],
  'estacion.fallo': [['estacion_fallo'], 48],
  'estacion.pasarela': [['estacion_pasarela'], 64],
  'estacion.coger': [['estacion_coger'], 12],
  'estacion.reparar': [['estacion_reparar'], 20],
  'estacion.persiana': [['estacion_persiana'], 32],
  'estacion.fusible': [['estacion_fusible'], 16],
  'estacion.palanca': [['estacion_palanca'], 20],
  'estacion.radio': [['estacion_radio'], 16],
  'amasijo.pasos': [['amasijo_pasos1', 'amasijo_pasos2', 'amasijo_pasos3', 'amasijo_pasos4'], 28],
  'estacion.paso': [[1, 2, 3, 4, 5, 6].map((k) => 'estacion_paso' + k), 16],
  'estacion.paso_chapa': [[1, 2, 3, 4, 5, 6].map((k) => 'estacion_paso_chapa' + k), 16],
  'amasijo.grito': [['amasijo_grito'], 64],
  'amasijo.respira': [['amasijo_respira1', 'amasijo_respira2'], 20],
  'amasijo.ataque': [['amasijo_ataque'], 24]
};

if (require.main === module) (async () => {
  const prefijo = process.argv[2] || '';
  console.log('Sonidos de la Estacion y el Amasijo');
  if ('estacion_ambiente'.startsWith(prefijo)) await guardarEst('estacion_ambiente', ambiente());
  for (const [nombre, fn] of SONIDOS) {
    if (!nombre.startsWith(prefijo)) continue;
    await guardarMono(nombre, fn());
  }
  const f = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(f, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  for (const viejo of ['estacion.generador', 'estacion.parada', 'estacion.reserva', 'estacion.repostar', 'estacion.bidon']) delete j[viejo];
  for (const [evento, [archivos, distancia]] of Object.entries(EVENTOS)) {
    j[evento] = { sounds: archivos.map((a) => ({ name: `backrooms_evento:${a}`, attenuation_distance: distancia })) };
  }
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(f, out);
  console.log('  sounds.json');
})();
