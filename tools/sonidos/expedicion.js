#!/usr/bin/env node
'use strict';
/**
 * Sonidos de la expedicion (mod evento/), todos sintetizados aqui:
 *
 *   node tools/sonidos/expedicion.js
 *
 *  Entidades (mono: Minecraft solo situa en 3D los sonidos mono)
 *   bacteria_grito    te ha visto: chillido rasgado de dos voces + chirrido de insecto
 *   bacteria_acecho   chasquidos de hueso, respiracion ronca y un gorgoteo
 *   bacteria_pasos    golpe seco de pata fina sobre la moqueta
 *   smiler_flash      el fogonazo al mirarle: estallido y un chirrido agudisimo
 *   smiler_grito      cuando te alcanza
 *   linterna, camara  clic del interruptor; servo y pitido de la camara
 *  Ambiente (estereo)
 *   apagon            rele, chispazos al ritmo del parpadeo y el zumbido que se muere
 *   luz_vuelve        rele a los 0,5 s, los cebadores y el zumbido que vuelve
 *   alarma            sirena en bucle, lejana, rebotando por los pasillos
 *   ascensor          puertas, bajada, frenazo, campanilla y puertas otra vez
 *   vestibulo         bucle del vestibulo: aire acondicionado y musica de ascensor lejana
 *   pitido, susurros, latido   para el flashbang y las alucinaciones
 */

const path = require('path');
const fs = require('fs');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };
const db = (d) => Math.pow(10, d / 20);

/* ------------------------------------------------------------- utiles */

function mono (segundos) {
  return new Float32Array(Math.round(segundos * SR));
}

/** Mezcla `fn(t)` en `buf` desde t0 durante `dur` segundos. */
function poner (buf, t0, dur, fn) {
  const i0 = Math.round(t0 * SR);
  const n = Math.min(buf.length - i0, Math.round(dur * SR));
  for (let i = 0; i < n; i++) buf[i0 + i] += fn(i / SR);
}

function reverb (buf, sala, mezcla, opciones = {}) {
  const rv = new D.Freeverb({ sala, amortiguacion: 0.4, ...opciones });
  const out = new Float32Array(buf.length);
  for (let i = 0; i < buf.length; i++) out[i] = buf[i] + rv.paso(buf[i])[0] * mezcla;
  return out;
}

function reverbEst ([l, r], sala, mezcla, opciones = {}) {
  const rv = new D.Freeverb({ sala, amortiguacion: 0.35, ...opciones });
  for (let i = 0; i < l.length; i++) {
    const [a, b] = rv.paso((l[i] + r[i]) * 0.5);
    l[i] += a * mezcla;
    r[i] += b * mezcla;
  }
  return [l, r];
}

/** Normaliza un mono a un pico y suaviza los bordes. */
function pico (buf, picoDb = -1) {
  let m = 0;
  for (const v of buf) m = Math.max(m, Math.abs(v));
  const g = db(picoDb) / (m || 1);
  for (let i = 0; i < buf.length; i++) buf[i] = Math.tanh(buf[i] * g * 1.1) / Math.tanh(1.1);
  const fe = Math.floor(0.002 * SR); const fs = Math.floor(0.02 * SR);
  for (let i = 0; i < fe; i++) buf[i] *= i / fe;
  for (let i = 0; i < fs; i++) buf[buf.length - 1 - i] *= i / fs;
  return buf;
}

async function guardarMono (nombre, buf, calidad = 5) {
  const { createOggEncoder } = require('wasm-media-encoders');
  const enc = await createOggEncoder();
  enc.configure({ sampleRate: SR, channels: 1, vbrQuality: calidad });
  const trozos = [];
  for (let i = 0; i < buf.length; i += 4096) trozos.push(Buffer.from(enc.encode([buf.subarray(i, i + 4096)])));
  trozos.push(Buffer.from(enc.finalize()));
  fs.mkdirSync(SALIDA, { recursive: true });
  const f = path.join(SALIDA, `${nombre}.ogg`);
  fs.writeFileSync(f, Buffer.concat(trozos));
  console.log(`  ${nombre}.ogg  ${(buf.length / SR).toFixed(2)} s mono  ${(fs.statSync(f).size / 1024).toFixed(0)} KB`);
}

async function guardarEst (nombre, est, calidad = 5) {
  const f = path.join(SALIDA, `${nombre}.ogg`);
  const t = await D.guardarOgg(f, est, calidad);
  console.log(`  ${nombre}.ogg  ${(est[0].length / SR).toFixed(2)} s estereo  ${(t / 1024).toFixed(0)} KB`);
}

/** Voz chillona: sierra con vibrato y jitter por tres formantes, saturada. */
function chillido (dur, curva, { formantes = [800, 1150, 2900], semilla = 1, jitter = 0.05, saturacion = 3 } = {}) {
  const rnd = D.azar(semilla);
  const bps = formantes.map((f, i) => new D.Biquad('bp', f, 6 + i * 2));
  let fase = 0; let j = 0;
  const out = new Float32Array(Math.round(dur * SR));
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    j += (rnd() - 0.5) * 0.08; j *= 0.995;
    const f = curva(t) * (1 + Math.sin(TAU * 7.5 * t) * 0.03 + j * jitter);
    fase = (fase + f / SR) % 1;
    const s = D.sierra(fase, f / SR);
    let v = 0;
    bps.forEach((bp, k) => { v += bp.paso(s) * [1, 0.7, 0.45][k]; });
    out[i] = Math.tanh(v * saturacion);
  }
  return out;
}

function ruidoBp (dur, f, q, semilla) {
  const rnd = D.azar(semilla);
  const bp = new D.Biquad('bp', f, q);
  const out = new Float32Array(Math.round(dur * SR));
  for (let i = 0; i < out.length; i++) out[i] = bp.paso(rnd() * 2 - 1);
  return out;
}

/** Golpe grave con caida de tono (bombo, puerta, rele). */
function golpe (f0, f1, caida, ruido = 0.3, semilla = 3) {
  const rnd = D.azar(semilla);
  const lp = new D.Biquad('lp', 1800, 0.7);
  let fase = 0;
  return (t) => {
    const f = f1 + (f0 - f1) * Math.exp(-t * 30);
    fase += f / SR;
    return (Math.sin(TAU * fase) + lp.paso(rnd() * 2 - 1) * ruido * Math.exp(-t * 60)) * Math.exp(-t / caida);
  };
}

/** Clic metalico corto: ruido filtrado con una resonancia. */
function clic (f, caida, semilla) {
  const rnd = D.azar(semilla);
  const bp = new D.Biquad('bp', f, 8);
  return (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t / caida) * 3;
}

function campana (f, caida) {
  const parciales = [[1, 1], [2.0, 0.45], [2.76, 0.32], [5.4, 0.15]];
  return (t) => parciales.reduce((s, [k, a]) => s + Math.sin(TAU * f * k * t) * a * Math.exp(-t * k / caida), 0) * 0.5;
}

/** Zumbido de balasto: 120 Hz con armonicos. */
function zumbido (t, f) {
  return Math.sin(TAU * f * t) + Math.sin(TAU * f * 2 * t) * 0.4 + Math.sin(TAU * f * 3 * t) * 0.55 + Math.sin(TAU * f * 5 * t) * 0.15;
}

/* ============================================================ entidades */

/**
 * Garganta de criatura: varias voces de sierra desafinadas (cada una con su
 * vibrato y su temblor), filtradas por formantes que se mueven (de "a" a "i"),
 * con carraspeo (golpes de amplitud a 40-80 Hz, como la voz rota) y muy poca
 * saturacion, para que se oiga la voz y no un ruido plano.
 */
function garganta (dur, curva, { voces = 5, semilla = 1, formantes, carraspeo = 0.5, aspereza = 0.25 }) {
  const rnd = D.azar(semilla);
  const out = new Float32Array(Math.round(dur * SR));
  const vs = [];
  for (let k = 0; k < voces; k++) {
    vs.push({ fase: rnd(), det: 1 + (rnd() - 0.5) * 0.06 * (k > 0 ? 1 : 0) + (k === voces - 1 ? 0.5 : 0), vib: 5 + rnd() * 3, j: 0, a: k === 0 ? 1 : 0.55 });
  }
  const bps = formantes.map(([f, q]) => new D.Biquad('bp', f[0], q));
  let fry = 1;
  let siguienteFry = 0;
  const ruido = new D.Biquad('bp', 3000, 1.2);
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const u = t / dur;
    let src = 0;
    for (const v of vs) {
      v.j += (rnd() - 0.5) * 0.12; v.j *= 0.996;
      const f = curva(t) * v.det * (1 + Math.sin(TAU * v.vib * t + v.det * 9) * 0.035 + v.j * 0.04);
      v.fase = (v.fase + f / SR) % 1;
      src += D.sierra(v.fase, f / SR) * v.a;
    }
    // carraspeo: la voz se rompe a golpes
    if (t >= siguienteFry) {
      siguienteFry = t + 1 / (40 + rnd() * 40);
      fry = 1 - carraspeo * rnd();
    }
    src = src * fry + ruido.paso(rnd() * 2 - 1) * aspereza * 3;
    let v = 0;
    if (i % 64 === 0) formantes.forEach(([f, q], k) => bps[k].ajustar(f[0] + (f[1] - f[0]) * u, q));
    formantes.forEach((fq, k) => { v += bps[k].paso(src) * (k === 0 ? 1 : 0.8 / k); });
    out[i] = Math.tanh(v * 1.3);
  }
  return out;
}

function bacteriaGrito () {
  const dur = 2.6;
  const buf = mono(dur);
  // sube de golpe, se queda temblando arriba y se quiebra hacia abajo
  const curva = (t) => 310 + 420 * suave(0, 0.18, t) + 60 * Math.sin(t * 9) * suave(0.2, 0.6, t) - 260 * suave(1.3, 2.5, t);
  const voz = garganta(dur, curva, { semilla: 11, formantes: [[[750, 600], 5], [[1150, 2300], 7], [[2700, 3100], 9], [[3600, 4200], 11]], carraspeo: 0.7, aspereza: 0.2 });
  // una segunda garganta mas grave y rota debajo
  const grave = garganta(dur, (t) => curva(t) * 0.5, { semilla: 12, voces: 3, formantes: [[[420, 380], 4], [[900, 1300], 6]], carraspeo: 0.9, aspereza: 0.1 });
  // chirrido de insecto: tono agudo con trino rapido
  const env = (t) => suave(0, 0.04, t) * (1 - suave(1.7, dur, t));
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const trino = Math.sin(TAU * (3300 + 500 * Math.sin(TAU * 0.7 * t)) * t + 3 * Math.sin(TAU * 61 * t)) * (0.5 + 0.5 * Math.sin(TAU * 23 * t));
    buf[i] = (voz[i] * 0.8 + grave[i] * 0.5 + trino * 0.12 * suave(0.05, 0.4, t)) * env(t);
  }
  // un chasquido seco al empezar, como si se le desencajara la mandibula
  poner(buf, 0, 0.06, clic(1800, 0.01, 15));
  return pico(reverb(buf, 0.7, 0.3, { predelayMs: 20 }), -0.5);
}

function bacteriaAcecho () {
  const dur = 3.2;
  const buf = mono(dur);
  const rnd = D.azar(21);
  // rafagas de chasquidos (como huesos o patas de insecto)
  for (const r0 of [0.1, 0.9, 1.7, 2.4]) {
    const n = 4 + Math.floor(rnd() * 6);
    for (let k = 0; k < n; k++) poner(buf, r0 + k * (0.035 + rnd() * 0.03), 0.05, clic(1500 + rnd() * 2500, 0.006, 22 + k));
  }
  // respiracion ronca: entra y sale
  const resp = ruidoBp(dur, 700, 1.4, 23);
  const ronco = ruidoBp(dur, 160, 4, 24);
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const ciclo = Math.max(0, Math.sin(TAU * t / 1.6)) ** 2;
    buf[i] += (resp[i] * 0.5 + ronco[i] * 1.2 * (0.6 + 0.4 * Math.sin(TAU * 31 * t))) * ciclo * 0.6;
  }
  // y un gemido grave, muy bajito, entre respiracion y respiracion
  const gemido = garganta(dur, (t) => 140 + 30 * Math.sin(t * 2), { semilla: 25, voces: 2, formantes: [[[500, 420], 5], [[1100, 900], 6]], carraspeo: 0.8, aspereza: 0.05 });
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    buf[i] += gemido[i] * 0.12 * suave(0.6, 1.0, t) * (1 - suave(1.4, 1.8, t));
  }
  return pico(reverb(buf, 0.5, 0.2), -3);
}

function bacteriaPasos () {
  const buf = mono(0.35);
  poner(buf, 0, 0.3, golpe(140, 60, 0.05, 0.6, 31));
  poner(buf, 0.002, 0.05, clic(2600, 0.004, 32));
  poner(buf, 0.03, 0.08, clic(900, 0.012, 33));
  return pico(buf, -4);
}

function smilerFlash () {
  const dur = 2.0;
  const buf = mono(dur);
  const rnd = D.azar(41);
  const hs = new D.Biquad('hp', 1500, 0.7);
  // estallido
  poner(buf, 0, 0.5, (t) => hs.paso(rnd() * 2 - 1) * Math.exp(-t * 7) * 1.2);
  poner(buf, 0, 0.4, golpe(90, 40, 0.12, 0.8, 42));
  // la risa: tres carcajadas agudas que se deforman
  for (const [t0, f0] of [[0.08, 520], [0.32, 600], [0.58, 470]]) {
    const r = garganta(0.22, (t) => f0 + 160 * Math.sin(Math.PI * t / 0.22), { semilla: 43 + f0, voces: 3, formantes: [[[900, 1200], 6], [[2400, 2800], 8]], carraspeo: 0.3, aspereza: 0.1 });
    poner(buf, t0, 0.22, (t) => r[Math.min(r.length - 1, Math.floor(t * SR))] * 0.7 * Math.sin(Math.PI * t / 0.22));
  }
  // y el pitido agudisimo que se queda
  poner(buf, 0.02, dur - 0.02, (t) => {
    const f = 3200 + 400 * Math.sin(TAU * 13 * t) + 900 * Math.exp(-t * 3);
    return (Math.sin(TAU * f * t + 2 * Math.sin(TAU * 1700 * t)) * 0.4 + Math.sin(TAU * 4100 * t) * 0.2) * Math.exp(-t * 1.3) * suave(0, 0.03, t);
  });
  return pico(reverb(buf, 0.4, 0.2));
}

function smilerGrito () {
  const dur = 1.7;
  const buf = mono(dur);
  // chillido que cae como una sirena rota, con la boca abriendose ("i" -> "a")
  const v = garganta(dur, (t) => 950 - 520 * suave(0, 1.5, t) + 40 * Math.sin(t * 40), { semilla: 51, formantes: [[[400, 900], 6], [[2600, 1400], 8], [[3300, 2700], 10]], carraspeo: 0.6, aspereza: 0.3 });
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    buf[i] = v[i] * suave(0, 0.015, t) * (1 - suave(1.0, dur, t));
  }
  poner(buf, 0, 0.5, golpe(110, 45, 0.15, 0.7, 53));
  return pico(reverb(buf, 0.5, 0.2), -0.5);
}

function linterna () {
  const buf = mono(0.16);
  poner(buf, 0, 0.05, clic(2400, 0.004, 61));
  poner(buf, 0, 0.05, clic(5200, 0.002, 62));
  poner(buf, 0.07, 0.05, clic(2000, 0.003, 63));
  return pico(buf, -3);
}

function camara () {
  const buf = mono(0.7);
  const rnd = D.azar(71);
  const bp = new D.Biquad('bp', 900, 4);
  // servo del zoom
  poner(buf, 0, 0.45, (t) => {
    bp.ajustar(800 + t * 900, 4);
    return bp.paso(rnd() * 2 - 1) * (0.5 + 0.5 * Math.sin(TAU * 140 * t)) * suave(0, 0.04, t) * (1 - suave(0.35, 0.45, t)) * 0.8;
  });
  // pitido de grabar
  poner(buf, 0.48, 0.12, (t) => Math.sin(TAU * 2100 * t) * 0.35 * (1 - suave(0.09, 0.12, t)));
  return pico(buf, -4);
}

/* ============================================================== ambiente */

function apagon () {
  const est = D.estereo(4.5);
  const m = mono(4.5);
  const rnd = D.azar(81);
  // el zumbido muere al ritmo del parpadeo (0 / 0,16 / 0,31 / 0,36)
  poner(m, 0, 0.6, (t) => {
    const vivo = t < 0.07 ? 1 : t < 0.16 ? 0.2 : t < 0.31 ? 1 : t < 0.36 ? 0.5 : Math.exp(-(t - 0.36) * 20);
    const f = 120 * (t < 0.36 ? 1 : Math.max(0.3, 1 - (t - 0.36) * 2.5));
    return zumbido(t, f) * vivo * 0.18;
  });
  // chispazos y el golpe del rele general
  for (const t0 of [0, 0.16, 0.31]) {
    poner(m, t0, 0.12, (t) => (rnd() * 2 - 1) * Math.exp(-t * 40) * (rnd() < 0.3 ? 1 : 0.3) * 0.9);
  }
  poner(m, 0.36, 1.2, golpe(70, 35, 0.35, 0.9, 82));
  poner(m, 0.36, 0.1, clic(1800, 0.01, 83));
  // el metal de los tubos que se enfria
  for (const t0 of [1.4, 2.3, 3.6]) poner(m, t0, 0.05, clic(3000 + rnd() * 2000, 0.003, 84));
  for (let i = 0; i < m.length; i++) { est[0][i] = m[i]; est[1][i] = m[i]; }
  reverbEst(est, 0.9, 0.5, { predelayMs: 30 });
  return D.fundidos(D.masterizar(est, { rmsDb: -20, picoDb: -1 }));
}

function luzVuelve () {
  const est = D.estereo(4.0);
  const m = mono(4.0);
  poner(m, 0.5, 1.0, golpe(80, 40, 0.25, 0.8, 91));
  poner(m, 0.5, 0.08, clic(1600, 0.01, 92));
  // los cebadores: tic y un zumbido corto en cada arranque
  for (const t0 of [0.56, 0.84]) {
    poner(m, t0, 0.04, clic(4000, 0.003, 93));
    poner(m, t0, 0.14, (t) => zumbido(t, 120) * 0.12 * Math.exp(-t * 10));
  }
  // el zumbido vuelve a subir
  poner(m, 0.98, 3.0, (t) => zumbido(t, 120) * 0.16 * suave(0, 0.4, t) * (1 - suave(2.2, 3.0, t)));
  for (let i = 0; i < m.length; i++) { est[0][i] = m[i]; est[1][i] = m[i]; }
  reverbEst(est, 0.85, 0.4);
  return D.fundidos(D.masterizar(est, { rmsDb: -21, picoDb: -1 }));
}

function alarma () {
  const N = 4; const cola = 1.5;
  const est = D.estereo(N + cola);
  const m = mono(N + cola);
  const lp = new D.Biquad('lp', 2600, 0.7);
  let fase = 0;
  // "whoop" industrial: sube de 480 a 980 Hz en cada segundo
  for (let i = 0; i < m.length; i++) {
    const t = i / SR;
    const c = (t % 1);
    const f = 480 + 500 * Math.pow(c, 0.7);
    fase = (fase + f / SR) % 1;
    const onda = Math.tanh(Math.sin(TAU * fase) * 2.5) + D.sierra(fase, f / SR) * 0.3;
    m[i] = lp.paso(onda) * (c < 0.85 ? 1 : 1 - (c - 0.85) / 0.15) * 0.4;
  }
  for (let i = 0; i < m.length; i++) { est[0][i] = m[i]; est[1][i] = m[i] * 0.92; }
  // lejos y rebotando por los pasillos
  reverbEst(est, 0.97, 1.4, { predelayMs: 60 });
  const bucle = D.cerrarBucle(est, N);
  return D.masterizar(bucle, { rmsDb: -19, picoDb: -1.5 });
}

function ascensor () {
  const dur = 9.0;
  const est = D.estereo(dur);
  const m = mono(dur);
  const rnd = D.azar(101);
  const puertas = (t0) => {
    const bp = new D.Biquad('bp', 300, 1.2);
    poner(m, t0, 1.2, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.2) * 0.6);
  };
  puertas(0);
  poner(m, 1.2, 0.8, golpe(90, 45, 0.18, 0.7, 102));
  // motor y cables mientras baja
  const lp = new D.Biquad('lp', 220, 0.8);
  poner(m, 1.4, 4.4, (t) => {
    const e = suave(0, 0.6, t) * (1 - suave(3.6, 4.4, t));
    return (lp.paso(rnd() * 2 - 1) * 2.5 + Math.sin(TAU * 58 * t) * 0.25 + Math.sin(TAU * 116 * t) * 0.1) * e * 0.6;
  });
  for (const t0 of [2.3, 3.4, 4.6]) poner(m, t0, 0.3, clic(700 + rnd() * 400, 0.05, 103 + t0));
  // frenazo, campanilla y puertas
  poner(m, 5.8, 0.8, golpe(70, 40, 0.2, 0.6, 104));
  poner(m, 6.1, 2.5, campana(1318, 1.4));
  poner(m, 6.45, 2.0, campana(1046, 1.4));
  puertas(6.7);
  for (let i = 0; i < m.length; i++) { est[0][i] = m[i]; est[1][i] = m[i]; }
  reverbEst(est, 0.5, 0.2);
  return D.fundidos(D.masterizar(est, { rmsDb: -20, picoDb: -1 }));
}

function vestibulo () {
  const N = 48; const cola = 3;
  const est = D.estereo(N + cola);
  const rnd = D.azar(111);
  const mr = D.marron(rnd);
  const lpA = new D.Biquad('lp', 380, 0.7);
  const hp = new D.Biquad('hp', 2500, 0.7);
  const ro = D.rosa(rnd);
  // aire acondicionado: soplido grave y siseo de rejilla
  for (let i = 0; i < est[0].length; i++) {
    const v = lpA.paso(mr()) * 0.5 + hp.paso(ro()) * 0.05;
    est[0][i] += v;
    est[1][i] += v * 0.95;
  }
  // musica de ascensor lejana: piano electrico en acordes suaves (Fa maj7, Rem9, Sib maj7, Do9)
  const ACORDES = [[53, 57, 60, 64], [50, 57, 60, 64], [46, 53, 57, 62], [48, 55, 58, 62]];
  const midi = (n) => 440 * Math.pow(2, (n - 69) / 12);
  const musica = mono(N + cola);
  for (let c = 0; c < 16; c++) {
    const t0 = c * 3;
    for (const n of ACORDES[c % 4]) {
      const f = midi(n + 12);
      poner(musica, t0 + rnd() * 0.05, 3.2, (t) => (Math.sin(TAU * f * t) + Math.sin(TAU * f * 2 * t) * 0.2) * Math.exp(-t * 0.9) * 0.08);
    }
    // una notita de melodia
    const mel = ACORDES[c % 4][Math.floor(rnd() * 4)] + 24;
    poner(musica, t0 + 1.5, 1.4, (t) => Math.sin(TAU * midi(mel) * t) * Math.exp(-t * 2) * 0.06);
  }
  // por el altavoz del techo: sin graves ni agudos
  const bpM = new D.Biquad('bp', 1100, 0.6);
  for (let i = 0; i < musica.length; i++) {
    const v = bpM.paso(musica[i]) * 0.9;
    est[0][i] += v;
    est[1][i] += v;
  }
  // de vez en cuando, la campanilla de un ascensor lejos
  const lejos = mono(N + cola);
  for (const t0 of [9, 31]) poner(lejos, t0, 2.5, campana(1318, 1.2));
  for (let i = 0; i < lejos.length; i++) { est[0][i] += lejos[i] * 0.12; est[1][i] += lejos[i] * 0.08; }
  reverbEst(est, 0.88, 0.35);
  return D.masterizar(D.cerrarBucle(est, N), { rmsDb: -27, picoDb: -6 });
}

function pitido () {
  const est = D.estereo(4.5);
  for (let i = 0; i < est[0].length; i++) {
    const t = i / SR;
    const e = suave(0, 0.05, t) * (1 - suave(0.8, 4.5, t));
    est[0][i] = (Math.sin(TAU * 5900 * t) + Math.sin(TAU * 5913 * t) * 0.6) * e * 0.25;
    est[1][i] = (Math.sin(TAU * 5905 * t) + Math.sin(TAU * 5921 * t) * 0.6) * e * 0.25;
  }
  return D.fundidos(est);
}

function susurros () {
  const dur = 3.4;
  const buf = mono(dur);
  const rnd = D.azar(121);
  const f1 = new D.Biquad('bp', 700, 5);
  const f2 = new D.Biquad('bp', 1800, 6);
  const hp = new D.Biquad('hp', 400, 0.7);
  // silabas: ruido por dos formantes que cambian, a golpes
  let silaba = 0; let fin = 0; let vocal = [700, 1800];
  const VOCALES = [[700, 1200], [400, 2200], [300, 900], [600, 1700], [350, 2600]];
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    if (t >= fin) {
      silaba = t;
      fin = t + 0.09 + rnd() * 0.16;
      vocal = VOCALES[Math.floor(rnd() * VOCALES.length)];
      f1.ajustar(vocal[0], 5);
      f2.ajustar(vocal[1], 6);
    }
    const u = (t - silaba) / (fin - silaba);
    const e = Math.sin(Math.PI * u) * (0.5 + 0.5 * Math.sin(TAU * 0.6 * t + 1));
    const n = hp.paso(rnd() * 2 - 1);
    buf[i] = (f1.paso(n) + f2.paso(n) * 0.7) * e * suave(0, 0.3, t) * (1 - suave(dur - 0.5, dur, t));
  }
  return pico(reverb(buf, 0.7, 0.4), -6);
}

function latido () {
  const est = D.estereo(0.7);
  const m = mono(0.7);
  poner(m, 0, 0.3, golpe(70, 42, 0.07, 0.15, 131));
  poner(m, 0.22, 0.3, golpe(62, 38, 0.06, 0.1, 132));
  for (let i = 0; i < m.length; i++) { est[0][i] = m[i]; est[1][i] = m[i]; }
  return D.fundidos(D.masterizar(est, { rmsDb: -16, picoDb: -1 }));
}

/* ================================================================= todo */

(async () => {
  console.log('Sonidos de la expedicion');
  await guardarMono('bacteria_grito', bacteriaGrito());
  await guardarMono('bacteria_acecho', bacteriaAcecho());
  await guardarMono('bacteria_pasos', bacteriaPasos());
  await guardarMono('smiler_flash', smilerFlash());
  await guardarMono('smiler_grito', smilerGrito());
  await guardarMono('linterna', linterna());
  await guardarMono('camara', camara());
  await guardarMono('susurros', susurros());
  await guardarEst('apagon', apagon());
  await guardarEst('luz_vuelve', luzVuelve());
  await guardarEst('alarma', alarma());
  await guardarEst('ascensor', ascensor());
  await guardarEst('vestibulo', vestibulo(), 4);
  await guardarEst('pitido', pitido());
  await guardarEst('latido', latido());

  // sounds.json: lo que no es de aqui se respeta
  const f = path.join(SALIDA, '..', 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  const s = (nombre, extra = {}) => ({ sounds: [{ name: `backrooms_evento:${nombre}`, ...extra }] });
  Object.assign(j, {
    'bacteria.grito': s('bacteria_grito', { attenuation_distance: 48 }),
    'bacteria.acecho': s('bacteria_acecho', { attenuation_distance: 24 }),
    'bacteria.pasos': s('bacteria_pasos', { attenuation_distance: 20 }),
    'smiler.flash': s('smiler_flash', { attenuation_distance: 32 }),
    'smiler.grito': s('smiler_grito', { attenuation_distance: 32 }),
    linterna: s('linterna', { attenuation_distance: 12 }),
    camara: s('camara', { attenuation_distance: 10 }),
    susurros: s('susurros'),
    apagon: s('apagon'),
    luz_vuelve: s('luz_vuelve'),
    alarma: s('alarma', { stream: true }),
    ascensor: s('ascensor'),
    'vestibulo.ambiente': s('vestibulo', { stream: true }),
    pitido: s('pitido'),
    latido: s('latido')
  });
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
})();
