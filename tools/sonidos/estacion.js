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
 *   estacion_generador     el diesel al ralenti en bucle, desde el generador (mono)
 *   estacion_estruendo1-3  algo metalico que cae lejos, en una nave enorme
 *   estacion_chispas1-3    el chisporroteo de un panel roto
 *  El generador y la corriente
 *   estacion_arranca       motor de arranque, el diesel que prende y sube; las luces vuelven (rele)
 *   estacion_parada        tose, se ahoga y se para; los reles caen y el zumbido se muere
 *   estacion_reserva       la alarma del deposito en reserva: dos tonos de zumbador
 *   estacion_repostar      el gasoil que cae por la boca, a borbotones
 *   estacion_fallo         cortocircuito: estallido, chispas y todo que se apaga con un quejido
 *   estacion_pasarela      los focos de la pasarela, uno tras otro: contactores que caen
 *  Lo que se hace
 *   estacion_bidon         coger una garrafa: el golpe de la chapa y el liquido que se mueve
 *   estacion_coger         coger un material: un roce y un tintineo
 *   estacion_reparar       chispazos, la carraca del destornillador y el rele que entra
 *   estacion_persiana      la persiana metalica que sube traqueteando
 *   estacion_fusible       el fusible que encaja y el zumbido que vuelve a su circuito
 *   estacion_palanca       la palanca grande del interruptor
 *   estacion_radio         la radio del acceso: sintonia, ruido, la voz por el altavoz y el corte
 *  El Amasijo
 *   amasijo_pasos1-3       muchas manos que golpean el suelo, deprisa y humedas
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

/** El diesel: explosiones a `rpm`, con el traqueteo de las valvulas y el escape. */
function diesel (dur, rpm, semilla, { escape = 0.6, valvulas = 0.25 } = {}) {
  const rnd = D.azar(semilla);
  const out = mono(dur);
  const lp = new D.Biquad('lp', 260, 0.8);
  const bp = new D.Biquad('bp', 1800, 3);
  let fase = 0;
  let pulso = 0;
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const f = (typeof rpm === 'function' ? rpm(t) : rpm) / 60 * 2; // 4 cilindros, 4 tiempos
    fase += f / SR;
    if (fase >= 1) { fase -= 1; pulso = 0.7 + rnd() * 0.3; }
    pulso *= 0.9985;
    const exp = Math.exp(-fase * 9) * pulso;
    const ruido = rnd() * 2 - 1;
    out[i] = lp.paso(exp * 2 + ruido * 0.15) * escape + bp.paso(ruido) * valvulas * (0.4 + Math.exp(-fase * 20)) + Math.sin(TAU * f * t) * 0.08;
  }
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

function generador () {
  // un bucle de 4 s: el ralenti a 750 rpm (25 explosiones/s: 100 exactas en 4 s)
  const dur = 5;
  const m = diesel(dur, 750, 311);
  // el silbido del turbo y un traqueteo de chapa suelta
  const rnd = D.azar(312);
  const bp = new D.Biquad('bp', 3200, 8);
  for (let i = 0; i < m.length; i++) {
    const t = i / SR;
    m[i] += bp.paso(rnd() * 2 - 1) * 0.05 + Math.sin(TAU * 25 * t) * 0.05 * (rnd() < 0.02 ? 3 : 0);
  }
  const [a] = D.cerrarBucle([m, Float32Array.from(m)], 4);
  return nivel(a, -2);
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

/* ============================================================== el generador y la corriente */

function arranca () {
  const out = mono(4.5);
  // el motor de arranque: un quejido electrico que gira a tirones
  const rnd = D.azar(321);
  let fase = 0;
  poner(out, 0, 1.4, (t) => {
    const f = 180 + Math.sin(TAU * 7 * t) * 30;
    fase += f / SR;
    return (D.sierra(fase % 1, f / SR) * 0.25 + (rnd() - 0.5) * 0.1) * (0.6 + 0.4 * Math.abs(Math.sin(TAU * 3.5 * t)));
  });
  // el diesel que prende: de 200 a 1100 rpm y baja a 800
  const rpm = (t) => (t < 1.2 ? 0 : t < 2.2 ? 200 + (t - 1.2) * 900 : 1100 - Math.min(1, (t - 2.2) / 1.2) * 300);
  const d = diesel(4.5, (t) => Math.max(60, rpm(t)), 322, { escape: 0.8 });
  for (let i = 0; i < out.length; i++) { const t = i / SR; out[i] += d[i] * Math.min(1, Math.max(0, (t - 1.1) * 2)); }
  // los reles: la corriente vuelve
  for (const t0 of [3.0, 3.15, 3.35]) poner(out, t0, 0.3, golpe(140, 60, 0.05, 0.8, Math.floor(t0 * 100)));
  poner(out, 3.0, 1.5, (t) => Math.sin(TAU * 100 * t) * 0.15 * Math.min(1, t * 3));
  return pico(nave(out, 0.3), -1);
}

function parada () {
  const out = mono(4);
  const rpm = (t) => Math.max(40, 800 - t * 350 + Math.sin(TAU * 2.5 * t) * 120 * Math.min(1, t));
  const d = diesel(4, rpm, 331, { escape: 0.8 });
  for (let i = 0; i < out.length; i++) { const t = i / SR; out[i] += d[i] * Math.max(0, 1 - t / 2.4); }
  // tose dos veces
  poner(out, 0.6, 0.3, golpe(80, 40, 0.08, 1, 332));
  poner(out, 1.2, 0.3, golpe(70, 35, 0.1, 1, 333));
  // reles que caen y el zumbido que se muere
  for (const t0 of [2.2, 2.35]) poner(out, t0, 0.3, golpe(130, 50, 0.05, 0.8, Math.floor(t0 * 100)));
  let f = 0;
  poner(out, 2.2, 1.8, (t) => { f += (100 * (1 - t / 1.8) + 10) / SR; return Math.sin(TAU * f) * 0.25 * (1 - t / 1.8); });
  return pico(nave(out, 0.4), -1);
}

function reserva () {
  const out = mono(1.6);
  for (let k = 0; k < 4; k++) {
    const f = k % 2 ? 620 : 780;
    poner(out, k * 0.36, 0.3, (t) => Math.tanh(Math.sin(TAU * f * t) * 4) * 0.4 * Math.min(1, t * 200) * Math.min(1, (0.3 - t) * 200));
  }
  return pico(nave(filtrar(out, 'bp', 900, 0.8), 0.25), -2);
}

/**
 * Echar gasolina con la garrafa: el tapon, el chorro que cae en el deposito (muchas burbujas
 * pequenas que suben de tono, como el agua de verdad) y los glu-glu de la garrafa cuando le entra
 * aire, cada vez mas agudos porque el deposito se va llenando. Al final, el chorro se corta.
 */
function repostar () {
  const dur = 3.2;
  const out = mono(dur);
  const rnd = D.azar(341);
  const ini = 0.35;
  const fin = 2.75;
  // el tapon que se desenrosca
  poner(out, 0.0, 0.12, clic(2600, 0.012, 342));
  poner(out, 0.12, 0.25, chapa(900, 0.04, 343));
  // el chorro: un siseo suave y muchas burbujas (senos que suben de tono y se apagan rapido)
  const siseo = new D.Biquad('bp', 1800, 0.6);
  const graves = new D.Biquad('lp', 500, 0.7);
  poner(out, ini, fin - ini, (t) => {
    const env = Math.min(1, t * 8) * Math.min(1, (fin - ini - t) * 6);
    const n = rnd() * 2 - 1;
    return (siseo.paso(n) * 0.12 + graves.paso(n) * 0.35) * env;
  });
  let t = ini;
  while (t < fin - 0.05) {
    const lleno = (t - ini) / (fin - ini);
    const f0 = (500 + rnd() * 1300) * (1 + lleno * 0.35);
    const tau = 0.006 + rnd() * 0.02;
    const a = 0.15 + rnd() * 0.35;
    poner(out, t, tau * 6, (u) => Math.sin(TAU * f0 * (1 + u * 9) * u) * Math.exp(-u / tau) * a);
    t += 0.006 + rnd() * 0.03;
  }
  // los glu-glu: la garrafa traga aire (burbujas grandes y graves) y el chorro tiembla un instante
  let g = ini + 0.3;
  while (g < fin - 0.2) {
    const lleno = (g - ini) / (fin - ini);
    for (let k = 0; k < 3; k++) {
      const f0 = (140 + rnd() * 90) * (1 + lleno * 0.5);
      poner(out, g + k * 0.045, 0.16, (u) => Math.sin(TAU * f0 * (1 + u * 3) * u) * Math.exp(-u * 22) * (0.9 - k * 0.2));
    }
    g += 0.42 + rnd() * 0.18;
  }
  // el chorro se corta: las ultimas gotas
  for (let k = 0; k < 4; k++) {
    const f0 = 900 + rnd() * 700;
    poner(out, fin + 0.05 + k * (0.08 + rnd() * 0.08), 0.06, (u) => Math.sin(TAU * f0 * (1 + u * 12) * u) * Math.exp(-u * 70) * 0.35);
  }
  return pico(reverb(filtrar(out, 'hp', 80), 0.35, 0.18), -2);
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

function bidon () {
  const out = mono(0.9);
  poner(out, 0, 0.6, chapa(220, 0.12, 371));
  // el liquido que se mueve dentro
  const rnd = D.azar(372);
  const bp = new D.Biquad('bp', 700, 2);
  poner(out, 0.05, 0.7, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 4) * (0.5 + 0.5 * Math.sin(TAU * 7 * t)) * 0.8);
  return pico(out, -2);
}

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

/* ============================================================== el Amasijo */

/** Muchas manos que golpean el suelo de chapa: palmadas humedas casi a la vez. */
function amasijoPasos (semilla) {
  const out = mono(0.5);
  const rnd = D.azar(semilla);
  for (let k = 0; k < 6; k++) {
    const t0 = rnd() * 0.18;
    const g = 0.5 + rnd() * 0.5;
    // la palmada: ruido corto, con cuerpo grave y la chapa que suena un poco
    const bp = new D.Biquad('bp', 900 + rnd() * 900, 1.2);
    const r2 = D.azar(semilla * 7 + k);
    poner(out, t0, 0.12, (t) => (bp.paso(r2() * 2 - 1) * 2 + Math.sin(TAU * 120 * t) * 0.6) * Math.exp(-t * 55) * g);
    if (k % 2 === 0) poner(out, t0, 0.2, (() => { const c = chapa(320 + rnd() * 200, 0.05, semilla + k); return (t) => c(t) * 0.15 * g; })());
  }
  // el chof humedo
  const wet = new D.Biquad('bp', 1500, 3);
  poner(out, 0.05, 0.2, (t) => wet.paso(rnd() * 2 - 1) * Math.exp(-t * 25) * 0.8);
  return pico(out, -1);
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
  ['estacion_generador', generador],
  ['estacion_estruendo1', () => estruendo(461)], ['estacion_estruendo2', () => estruendo(462)], ['estacion_estruendo3', () => estruendo(463)],
  ['estacion_chispas1', () => pico(chispas(0.5, 2, 471), -3)], ['estacion_chispas2', () => pico(chispas(0.4, 1.4, 472), -3)], ['estacion_chispas3', () => pico(chispas(0.6, 3, 473), -3)],
  ['estacion_arranca', arranca], ['estacion_parada', parada], ['estacion_reserva', reserva], ['estacion_repostar', repostar],
  ['estacion_fallo', fallo], ['estacion_pasarela', pasarela], ['estacion_bidon', bidon], ['estacion_coger', coger],
  ['estacion_reparar', reparar], ['estacion_persiana', persiana], ['estacion_fusible', fusible], ['estacion_palanca', palanca],
  ['estacion_radio', radio],
  ['amasijo_pasos1', () => amasijoPasos(481)], ['amasijo_pasos2', () => amasijoPasos(482)], ['amasijo_pasos3', () => amasijoPasos(483)],
  ['amasijo_grito', amasijoGrito],
  ['amasijo_respira1', () => amasijoRespira(491)], ['amasijo_respira2', () => amasijoRespira(492)],
  ['amasijo_ataque', amasijoAtaque]
];

/** Entradas de sounds.json: evento -> [archivos, distancia de atenuacion]. */
const EVENTOS = {
  'estacion.ambiente': [['estacion_ambiente'], 16],
  'estacion.generador': [['estacion_generador'], 40],
  'estacion.estruendo': [['estacion_estruendo1', 'estacion_estruendo2', 'estacion_estruendo3'], 80],
  'estacion.chispas': [['estacion_chispas1', 'estacion_chispas2', 'estacion_chispas3'], 12],
  'estacion.arranca': [['estacion_arranca'], 64],
  'estacion.parada': [['estacion_parada'], 64],
  'estacion.reserva': [['estacion_reserva'], 48],
  'estacion.repostar': [['estacion_repostar'], 16],
  'estacion.fallo': [['estacion_fallo'], 48],
  'estacion.pasarela': [['estacion_pasarela'], 64],
  'estacion.bidon': [['estacion_bidon'], 12],
  'estacion.coger': [['estacion_coger'], 12],
  'estacion.reparar': [['estacion_reparar'], 20],
  'estacion.persiana': [['estacion_persiana'], 32],
  'estacion.fusible': [['estacion_fusible'], 16],
  'estacion.palanca': [['estacion_palanca'], 20],
  'estacion.radio': [['estacion_radio'], 16],
  'amasijo.pasos': [['amasijo_pasos1', 'amasijo_pasos2', 'amasijo_pasos3'], 28],
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
  for (const [evento, [archivos, distancia]] of Object.entries(EVENTOS)) {
    j[evento] = { sounds: archivos.map((a) => ({ name: `backrooms_evento:${a}`, attenuation_distance: distancia })) };
  }
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(f, out);
  console.log('  sounds.json');
})();
