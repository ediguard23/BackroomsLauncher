#!/usr/bin/env node
'use strict';
/**
 * La voz de la Bacteria, mas dura y mas oscura, y lo que suena cuando te agarra.
 * Todo sintetizado (sin muestras de nadie), mono para que suene en 3D.
 *
 *   node tools/sonidos/bacteria.js            todos
 *   node tools/sonidos/bacteria.js mordisco   solo los que empiezan asi
 *
 *   bacteria_grito       te ha visto: inspira hacia atras, rugido de tres capas (un
 *                        subgrave que se nota en el pecho, la garganta rota y un alarido
 *                        metalico encima), distorsion y eco de pasillo
 *   bacteria_caza        persiguiendote: resoplidos con grunido, dientes y babas
 *   bacteria_acecho      rondando: crujidos de hueso, estertor y un gemido que se tuerce
 *   bacteria_renuncia    te ha perdido o te has escondido: grunido de rabia que se hunde
 *   bacteria_agarre      te agarra: chillido de golpe, latigazo de carne y un chasquido
 *   bacteria_levanta     te levanta: esfuerzo que sube, tendones que crujen, carne que se estira
 *   bacteria_mordisco1-3 mordisco: mandibula que cierra, hueso que cruje y desgarro humedo
 *   bacteria_devora      comiendo: gorgoteo, masticar humedo y tragar
 *   victima_grito1-2     el grito ahogado de quien acaba de agarrar
 *   hueco                meterse por un hueco: pladur que cruje, cascotes y papel
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { suave, mono, poner, reverb, guardarMono, ruidoBp, golpe, clic, garganta, rugido, ecos, fuerte, SALIDA } = E;

/* ============================================================ piezas */

/** Mezcla `src` (un buffer) en `buf` desde t0, con ganancia o envolvente. */
function meter (buf, t0, src, g = 1) {
  const i0 = Math.round(t0 * SR);
  for (let i = 0; i < src.length && i0 + i < buf.length; i++) {
    buf[i0 + i] += src[i] * (typeof g === 'function' ? g(i / SR) : g);
  }
}

/** Saturacion asimetrica (pares + impares): mas sucia que un tanh. */
function rasgar (buf, k = 3, asim = 0.35) {
  const n = Math.tanh(k);
  for (let i = 0; i < buf.length; i++) {
    const x = buf[i] * k;
    buf[i] = Math.tanh(x + asim * x * x * 0.3) / n;
  }
  return buf;
}

/** Modulacion en anillo parcial: lo vuelve metalico, nada humano. */
function anillo (buf, f, mezcla) {
  for (let i = 0; i < buf.length; i++) buf[i] = buf[i] * (1 - mezcla + mezcla * Math.sin(TAU * f * i / SR));
  return buf;
}

function alReves (buf) {
  return Float32Array.from(buf).reverse();
}

/** Aliento: ruido por una garganta (dos resonancias) con su envolvente. */
function aliento (dur, f1, f2, semilla, env) {
  const rnd = D.azar(semilla);
  const a = new D.Biquad('bp', f1, 1.4);
  const b = new D.Biquad('bp', f2, 2.5);
  const out = mono(dur);
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const n = rnd() * 2 - 1;
    out[i] = (a.paso(n) + b.paso(n) * 0.6) * env(t);
  }
  return out;
}

/** Hueso que cruje: una racha de chasquidos que se van apagando, con un golpe seco. */
function crujido (dur, semilla, { n = 14, fmin = 900, fmax = 3200, seco = 0.6 } = {}) {
  const rnd = D.azar(semilla);
  const out = mono(dur + 0.1);
  let t = 0;
  for (let k = 0; k < n && t < dur; k++) {
    t += 0.004 + rnd() * 0.025 * (1 + k / n);
    const a = Math.exp(-k / (n * 0.6));
    poner(out, t, 0.04, (u) => clic(fmin + rnd() * (fmax - fmin), 0.003 + rnd() * 0.004, semilla * 31 + k)(u) * a);
  }
  poner(out, 0, 0.25, (u) => golpe(140, 60, 0.04, 0.8, semilla + 7)(u) * seco);
  return out;
}

/** Desgarro humedo: ruido por una resonancia que barre, a tirones (fibras que saltan). */
function desgarro (dur, semilla, { f0 = 450, f1 = 2600, q = 3 } = {}) {
  const rnd = D.azar(semilla);
  const bp = new D.Biquad('bp', f0, q);
  const lp = new D.Biquad('lp', 500, 0.8);
  const out = mono(dur);
  let g = 1; let siguiente = 0;
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const u = t / dur;
    if (i % 32 === 0) bp.ajustar(f0 + (f1 - f0) * Math.pow(u, 0.7), q);
    if (t >= siguiente) { siguiente = t + 0.008 + rnd() * 0.03; g = 0.3 + rnd() * 0.9; }
    const n = rnd() * 2 - 1;
    const env = suave(0, 0.03, t) * (1 - suave(dur * 0.6, dur, t));
    out[i] = (bp.paso(n) * 1.4 + lp.paso(n) * 0.8) * g * env;
  }
  return out;
}

/** Gorgoteo: burbujas que suben de tono y revientan, mas o menos seguidas. */
function gorgoteo (dur, semilla, densidad = 30, grave = 1) {
  const rnd = D.azar(semilla);
  const out = mono(dur + 0.1);
  const n = Math.round(dur * densidad);
  for (let k = 0; k < n; k++) {
    const t0 = rnd() * dur;
    const f = (140 + rnd() * 380) * grave;
    const d = 0.02 + rnd() * 0.05;
    const a = 0.3 + rnd() * 0.7;
    poner(out, t0, d, (t) => Math.sin(TAU * f * (1 + t * 9) * t) * Math.exp(-t / (d * 0.4)) * a);
  }
  return out;
}

/** Saca el pico y lo deja todo bien alto sin recortar. */
function final (buf, empuje = 2.2, picoDb = -0.5) {
  return fuerte(buf, empuje, picoDb);
}

/* ======================================================= la voz de siempre */

function grito () {
  const dur = 3.6;
  const buf = mono(dur + 2.8);
  const ini = 0.42; // el rugido arranca aqui (antes, la inspiracion)
  // inspira hacia atras: un aliento al reves que crece hasta el golpe
  const insp = alReves(aliento(ini, 700, 1900, 61, (t) => Math.pow(t / ini, 2.5) * (1 - suave(ini - 0.02, ini, t) * 0.4)));
  meter(buf, 0, insp, 1.2);
  // capa 1: subgrave (45-70 Hz) con el grunido de pecho, se nota mas que se oye
  const curvaSub = (t) => 44 + 24 * suave(0.0, 0.25, t) - 18 * suave(2.4, 3.4, t) + 4 * Math.sin(TAU * 3.1 * t);
  const sub = rugido(dur, curvaSub, { semilla: 62, voces: 6, formantes: [[[240, 300], 2], [[520, 640], 3]], rotura: 0.9, ronquera: 0.25, sub: 0.9, cuerpo: 0.9 });
  // capa 2: la garganta rota (80-140 Hz) que se abre y tiembla
  const curva = (t) => 82 + 58 * suave(0.04, 0.3, t) + 10 * Math.sin(TAU * 6.2 * t) * suave(0.35, 0.8, t) - 70 * suave(2.2, 3.4, t);
  const voz = rugido(dur, curva, { semilla: 63, voces: 8, formantes: [[[330, 460], 2.5], [[700, 980], 3], [[1500, 1900], 4], [[2600, 2900], 5]], rotura: 0.92, ronquera: 0.4, sub: 0.4 });
  // capa 3: el alarido de encima, metalico (anillo a 53 Hz), lo que pone los pelos de punta
  const alarido = anillo(garganta(dur, (t) => curva(t) * 3.4, { semilla: 64, voces: 5, formantes: [[[900, 1400], 4], [[2300, 2800], 6], [[3500, 3900], 8]], carraspeo: 0.95, aspereza: 0.4 }), 53, 0.55);
  const env = (t) => suave(0.0, 0.05, t) * (1 - suave(2.5, dur, t));
  for (let i = 0; i < voz.length; i++) {
    const t = i / SR;
    const j = i + Math.round(ini * SR);
    if (j >= buf.length) break;
    buf[j] += (sub[i] * 0.9 + voz[i] + alarido[i] * 0.55 * suave(0.06, 0.3, t) * (1 - suave(1.6, 2.6, t))) * env(t);
  }
  // el golpe del principio y la mandibula que cruje al abrirse
  poner(buf, ini, 1.8, golpe(48, 26, 0.45, 0.6, 65));
  meter(buf, ini - 0.01, crujido(0.18, 66, { n: 9, fmin: 600, fmax: 2200 }), 0.9);
  // se apaga en un gorgoteo
  meter(buf, ini + 2.75, gorgoteo(0.7, 67, 26, 0.8), 0.5);
  rasgar(buf, 1.8, 0.4);
  const conEcos = ecos(buf, [0.17, 0.36, 0.58, 0.85], 0.55, 1700);
  return final(reverb(conEcos, 0.94, 0.6, { predelayMs: 45, amortiguacion: 0.22 }), 2.6, -0.3);
}

function caza () {
  const buf = mono(2.5);
  // dos resoplidos con grunido: fuera, dentro
  for (const [t0, d, f, s, fuera] of [[0.0, 0.62, 70, 71, true], [0.75, 0.7, 62, 72, false]]) {
    const gr = rugido(d, (t) => f + 18 * Math.sin(Math.PI * t / d), { semilla: s, voces: 5, formantes: [[[400, 300], 2.5], [[900, 720], 3], [[2000, 1700], 4]], rotura: 0.97, ronquera: 1.0, sub: 0.6, cuerpo: 0.7 });
    const aire = aliento(d, fuera ? 600 : 900, fuera ? 1700 : 2400, s + 10, (t) => Math.pow(Math.sin(Math.PI * Math.min(1, t / d)), 0.6));
    meter(buf, t0, gr, (t) => Math.pow(Math.sin(Math.PI * Math.min(1, t / d)), 0.5) * 0.9);
    meter(buf, t0, aire, 1.3);
  }
  // dientes que castanetean y babas
  const rnd = D.azar(73);
  for (let k = 0; k < 6; k++) poner(buf, 0.55 + k * 0.05 + rnd() * 0.02, 0.04, clic(1700 + rnd() * 900, 0.004, 74 + k));
  meter(buf, 1.4, gorgoteo(0.5, 75, 20, 1.3), 0.35);
  rasgar(buf, 1.6, 0.3);
  return final(reverb(ecos(buf, [0.15, 0.31], 0.35, 1500), 0.86, 0.4, { predelayMs: 25 }), 2.0, -1);
}

function acecho () {
  const dur = 3.6;
  const buf = mono(dur + 1.2);
  const rnd = D.azar(81);
  // crujidos de hueso, como si se recolocara las articulaciones
  for (const r0 of [0.05, 1.05, 2.3]) meter(buf, r0, crujido(0.3 + rnd() * 0.2, 82 + Math.round(r0 * 10), { n: 8 + Math.floor(rnd() * 8), fmin: 700, fmax: 2600 }), 0.8);
  // estertor grave: la respiracion rota que entra y sale
  const ronco = ruidoBp(dur, 95, 3, 83);
  const resp = ruidoBp(dur, 480, 1.3, 84);
  for (let i = 0; i < ronco.length; i++) {
    const t = i / SR;
    const ciclo = Math.max(0, Math.sin(TAU * t / 1.8)) ** 2;
    buf[i] += (ronco[i] * 1.6 * (0.55 + 0.45 * Math.sin(TAU * 31 * t)) + resp[i] * 0.45) * ciclo * 0.6;
  }
  // un gemido que se tuerce hacia abajo, casi humano y luego no
  const gem = garganta(1.6, (t) => 150 - 70 * suave(0, 1.6, t) + 8 * Math.sin(TAU * 4 * t), { semilla: 85, voces: 4, formantes: [[[600, 420], 5], [[1100, 800], 6], [[2400, 2100], 8]], carraspeo: 0.85, aspereza: 0.3 });
  meter(buf, 1.5, anillo(gem, 37, 0.4), (t) => 0.5 * suave(0, 0.3, t) * (1 - suave(1.1, 1.6, t)));
  rasgar(buf, 1.4, 0.3);
  return final(reverb(ecos(buf, [0.21, 0.44], 0.35, 1300), 0.85, 0.42), 1.2, -4);
}

/* ============================================================ las nuevas */

function renuncia () {
  const dur = 2.0;
  const buf = mono(dur + 1.4);
  // grunido de rabia que se hunde
  const gr = rugido(dur, (t) => 96 - 50 * suave(0.2, 1.8, t) + 7 * Math.sin(TAU * 5 * t), { semilla: 91, voces: 7, formantes: [[[420, 300], 2.5], [[900, 650], 3], [[1900, 1500], 4]], rotura: 0.9, ronquera: 0.5, sub: 0.7, cuerpo: 0.6 });
  meter(buf, 0.08, gr, (t) => suave(0, 0.08, t) * (1 - suave(1.3, dur, t)));
  // un bufido por la nariz y dientes
  meter(buf, 0, aliento(0.3, 500, 1500, 92, (t) => Math.exp(-t * 9)), 1.4);
  meter(buf, 1.2, crujido(0.3, 93, { n: 10, fmin: 800, fmax: 2400 }), 0.6);
  rasgar(buf, 1.7, 0.4);
  return final(reverb(ecos(buf, [0.18, 0.4], 0.4, 1500), 0.9, 0.5, { predelayMs: 30 }), 2.0, -1);
}

function agarre () {
  const dur = 1.3;
  const buf = mono(dur + 1.6);
  // chillido de golpe, muy agudo y rasgado
  const ch = anillo(garganta(0.9, (t) => 420 + 240 * suave(0, 0.12, t) - 160 * suave(0.3, 0.9, t), { semilla: 101, voces: 6, formantes: [[[1100, 900], 5], [[2600, 2200], 7], [[3800, 3400], 9]], carraspeo: 1.0, aspereza: 0.5 }), 61, 0.6);
  meter(buf, 0.0, ch, (t) => suave(0, 0.015, t) * (1 - suave(0.5, 0.9, t)) * 0.9);
  // y por debajo el rugido que lo empuja
  const gr = rugido(1.0, (t) => 110 - 30 * suave(0.2, 1.0, t), { semilla: 102, voces: 7, formantes: [[[380, 300], 2.5], [[850, 700], 3], [[1800, 1500], 4]], rotura: 0.9, ronquera: 0.5, sub: 0.6 });
  meter(buf, 0.02, gr, (t) => suave(0, 0.03, t) * (1 - suave(0.6, 1.0, t)));
  // latigazo de carne: ruido muy corto y brillante con un golpe grave
  poner(buf, 0.0, 0.5, golpe(95, 40, 0.09, 1.2, 103));
  meter(buf, 0.0, desgarro(0.18, 104, { f0: 1800, f1: 600, q: 2 }), 1.3);
  meter(buf, 0.03, crujido(0.2, 105, { n: 12, fmin: 1000, fmax: 3600 }), 1.0);
  rasgar(buf, 2.2, 0.45);
  return final(reverb(ecos(buf, [0.14, 0.3], 0.4, 2000), 0.82, 0.38, { predelayMs: 15 }), 2.6, -0.3);
}

function levanta () {
  const dur = 1.5;
  const buf = mono(dur + 1.0);
  // esfuerzo: un grunido que sube, temblando
  const gr = rugido(dur, (t) => 70 + 45 * suave(0, 1.2, t) + 6 * Math.sin(TAU * 9 * t), { semilla: 111, voces: 7, formantes: [[[350, 520], 2.5], [[780, 1100], 3], [[1700, 2100], 4]], rotura: 0.85, ronquera: 0.55, sub: 0.5, cuerpo: 0.5 });
  meter(buf, 0, gr, (t) => suave(0, 0.15, t) * (1 - suave(1.2, dur, t)) * 0.9);
  // carne y ropa que se estiran: ruido con resonancias que suben despacio
  meter(buf, 0.1, desgarro(1.2, 112, { f0: 300, f1: 1400, q: 6 }), 0.7);
  // tendones y articulaciones que crujen
  const rnd = D.azar(113);
  for (let k = 0; k < 4; k++) meter(buf, 0.15 + k * 0.28 + rnd() * 0.08, crujido(0.15, 114 + k, { n: 6, fmin: 500, fmax: 1800 }), 0.7);
  rasgar(buf, 1.6, 0.35);
  return final(reverb(buf, 0.75, 0.3, { predelayMs: 12 }), 2.0, -1);
}

function mordisco (k) {
  const dur = 0.95;
  const buf = mono(dur + 0.9);
  const s = 120 + k * 10;
  // la mandibula cierra de golpe
  poner(buf, 0, 0.35, golpe(120 + k * 15, 50, 0.05, 1.4, s));
  poner(buf, 0, 0.05, clic(2600 - k * 300, 0.006, s + 1));
  // hueso que cruje y se parte
  meter(buf, 0.02, crujido(0.35, s + 2, { n: 18 + k * 3, fmin: 700, fmax: 3400 }), 1.3);
  // desgarro humedo y sangre
  meter(buf, 0.06, desgarro(0.55, s + 3, { f0: 700 + k * 150, f1: 2200, q: 2.5 }), 1.0);
  meter(buf, 0.15, gorgoteo(0.6, s + 4, 45, 0.9), 0.55);
  // grunido contra la carne
  const gr = rugido(0.7, (t) => 80 + 20 * Math.sin(t * 6), { semilla: s + 5, voces: 5, formantes: [[[340, 280], 2.5], [[760, 640], 3]], rotura: 0.95, ronquera: 0.8, sub: 0.6, cuerpo: 0.7 });
  meter(buf, 0.05, gr, (t) => (1 - suave(0.3, 0.7, t)) * 0.6);
  rasgar(buf, 2.0, 0.4);
  return final(reverb(buf, 0.6, 0.22, { predelayMs: 8 }), 2.4, -0.3);
}

function devora () {
  const dur = 2.3;
  const buf = mono(dur + 1.0);
  // gorgoteo grave continuo, garganta llena
  const gr = rugido(dur, (t) => 58 + 8 * Math.sin(t * 4.1), { semilla: 131, voces: 6, formantes: [[[300, 250], 3], [[650, 560], 4]], rotura: 0.98, ronquera: 0.9, sub: 0.7, cuerpo: 0.8 });
  meter(buf, 0, gr, (t) => suave(0, 0.2, t) * (1 - suave(1.8, dur, t)) * 0.7);
  meter(buf, 0, gorgoteo(dur, 132, 38, 0.8), 0.7);
  // masticar: chasquidos humedos con crujidos, cada vez mas lentos
  let t = 0.05;
  for (let k = 0; k < 7; k++) {
    meter(buf, t, desgarro(0.22, 133 + k, { f0: 500, f1: 1500, q: 2 }), 0.8);
    meter(buf, t, crujido(0.12, 140 + k, { n: 6, fmin: 900, fmax: 2600 }), 0.6);
    t += 0.22 + k * 0.03;
  }
  // y traga
  poner(buf, 1.95, 0.4, golpe(70, 40, 0.12, 0.3, 150));
  meter(buf, 1.9, aliento(0.35, 300, 900, 151, (u) => Math.sin(Math.PI * Math.min(1, u / 0.35))), 0.8);
  rasgar(buf, 1.6, 0.35);
  return final(reverb(buf, 0.7, 0.28), 2.0, -1);
}

/** Grito humano ahogado: sube, se rompe y lo cortan. */
function victima (k) {
  const dur = 1.25;
  const buf = mono(dur + 0.8);
  const f0 = k === 1 ? 330 : 270;
  const curva = (t) => f0 + 160 * suave(0, 0.18, t) + 25 * Math.sin(TAU * 6.5 * t) - 60 * suave(0.6, 1.2, t);
  // formantes de "a" abierta que se cierran a "u" (se le tapa la boca)
  const v = garganta(dur, curva, { semilla: 160 + k, voces: 3, formantes: [[[850, 450], 6], [[1250, 800], 7], [[2700, 2300], 9]], carraspeo: 0.75, aspereza: 0.35 });
  meter(buf, 0, v, (t) => suave(0, 0.04, t) * (1 - suave(0.95, dur, t)));
  meter(buf, 0, aliento(dur, 1200, 2800, 170 + k, (t) => 0.35 * suave(0, 0.05, t) * (1 - suave(0.8, dur, t))), 1);
  rasgar(buf, 2.0, 0.2);
  return final(reverb(ecos(buf, [0.16, 0.34], 0.3, 2200), 0.8, 0.35), 2.0, -1);
}

function hueco () {
  const dur = 0.85;
  const buf = mono(dur + 0.4);
  const rnd = D.azar(181);
  // pladur que cruje y se desmorona
  meter(buf, 0, crujido(0.4, 182, { n: 20, fmin: 500, fmax: 2200 }), 0.9);
  // cascotes que caen: golpecitos sordos
  for (let k = 0; k < 9; k++) poner(buf, 0.08 + rnd() * 0.6, 0.08, golpe(180 + rnd() * 200, 90, 0.015, 0.9, 183 + k));
  // polvo de yeso
  const hp = new D.Biquad('hp', 2500, 0.7);
  const rp = D.azar(195);
  poner(buf, 0.02, 0.8, (t) => hp.paso(rp() * 2 - 1) * 0.25 * suave(0, 0.05, t) * (1 - suave(0.3, 0.8, t)));
  // el papel que se rasga un poco
  meter(buf, 0.05, desgarro(0.3, 196, { f0: 2400, f1: 3600, q: 4 }), 0.35);
  return final(reverb(buf, 0.45, 0.12), 1.4, -2);
}

/* ================================================================ todo */

(async () => {
  console.log('Sonidos de la Bacteria');
  const solo = process.argv[2];
  const toca = (n) => !solo || n.startsWith(solo);
  if (toca('bacteria_grito')) await guardarMono('bacteria_grito', grito());
  if (toca('bacteria_caza')) await guardarMono('bacteria_caza', caza());
  if (toca('bacteria_acecho')) await guardarMono('bacteria_acecho', acecho());
  if (toca('bacteria_renuncia')) await guardarMono('bacteria_renuncia', renuncia());
  if (toca('bacteria_agarre')) await guardarMono('bacteria_agarre', agarre());
  if (toca('bacteria_levanta')) await guardarMono('bacteria_levanta', levanta());
  for (let k = 1; k <= 3; k++) if (toca('bacteria_mordisco')) await guardarMono(`bacteria_mordisco${k}`, mordisco(k));
  if (toca('bacteria_devora')) await guardarMono('bacteria_devora', devora());
  for (let k = 1; k <= 2; k++) if (toca('victima_grito')) await guardarMono(`victima_grito${k}`, victima(k));
  if (toca('hueco')) await guardarMono('hueco', hueco());

  const f = path.join(SALIDA, '..', 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  const s = (nombre, extra = {}) => ({ sounds: [{ name: `backrooms_evento:${nombre}`, ...extra }] });
  const varias = (nombre, n, extra = {}) => ({ sounds: Array.from({ length: n }, (_, k) => ({ name: `backrooms_evento:${nombre}${k + 1}`, ...extra })) });
  Object.assign(j, {
    'bacteria.grito': s('bacteria_grito', { attenuation_distance: 64 }),
    'bacteria.caza': s('bacteria_caza', { attenuation_distance: 28 }),
    'bacteria.acecho': s('bacteria_acecho', { attenuation_distance: 24 }),
    'bacteria.renuncia': s('bacteria_renuncia', { attenuation_distance: 40 }),
    'bacteria.agarre': s('bacteria_agarre', { attenuation_distance: 48 }),
    'bacteria.levanta': s('bacteria_levanta', { attenuation_distance: 32 }),
    'bacteria.mordisco': varias('bacteria_mordisco', 3, { attenuation_distance: 32 }),
    'bacteria.devora': s('bacteria_devora', { attenuation_distance: 32 }),
    'victima.grito': varias('victima_grito', 2, { attenuation_distance: 48 }),
    hueco: s('hueco', { attenuation_distance: 12 })
  });
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
  console.log('  sounds.json');
})();
