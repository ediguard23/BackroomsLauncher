#!/usr/bin/env node
'use strict';
/**
 * Banda sonora de la cinematica del /start (55 s), sincronizada con
 * evento/.../shaders/core/cinematica.fsh y CinematicaCliente.java:
 *
 *   0.0  la videocamara se enciende (pitidos y motor de cinta)
 *   0.9  vestibulo: murmullo de 200 personas; ding del ascensor
 *   2.0  se cierran las puertas (rodillos y golpe metalico)
 *   3.6  arranca el motor; musica de ascensor (bossa nova por un altavoz barato)
 *  18.0  la luz falla: chasquidos electricos; la cinta de la musica se arrastra
 *  20.0  FRENAZO: golpe, la musica se corta, cae polvo; alarma; crujidos del cable
 *  23.2  se escurre (y otra vez a los 25.0); latidos y respiracion
 *  26.5  SE PARTE EL CABLE: latigazo + braaam; caida libre: viento que crece,
 *        traqueteo, rellanos pasando como un aleteo, riser que no deja de subir
 *  29.0  el freno de emergencia chirria y prende: fuego
 *  29.5  las puertas crujen (10 grietas) y a los 33.0 REVIENTAN
 *  39.0  IMPACTO; corte a sordera: pitido en los oidos y latidos sordos
 *  45.0  despiertas: respiracion, el pitido se va
 *  50.5  golpe grave y acorde para el titulo NIVEL 0
 *
 * Todo es sintesis propia (sin muestras de nadie). Sale a
 * evento/src/main/resources/assets/backrooms_evento/sounds/cinematica.ogg
 *
 *   node tools/sonidos/cinematica.js
 */

const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const DUR = 55.5;
const N = Math.ceil(DUR * SR);
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds', 'cinematica.ogg');

const T_CIERRA0 = 2.0, T_CIERRA1 = 3.4, T_BAJA = 3.6, T_PARPADEO = 18.0, T_ATASCO = 20.0;
const T_ROTURA = 26.5, T_FUEGO = 29.0, T_GRIETAS = 29.5, T_PUERTAS = 33.0, T_IMPACTO = 39.0, T_DESPERTAR = 45.0, T_TITULO = 50.5;

/* ----------------------------------------------------------- buses */

// A: todo hasta el impacto (luego se ensordece). B: lo de despues (ya sordo).
const A = { L: new Float32Array(N), R: new Float32Array(N), sala: new Float32Array(N), grande: new Float32Array(N) };
const B = { L: new Float32Array(N), R: new Float32Array(N), sala: new Float32Array(N), grande: new Float32Array(N) };

const paneo = (p) => [Math.cos((p + 1) * Math.PI / 4), Math.sin((p + 1) * Math.PI / 4)];
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };
const db = (d) => Math.pow(10, d / 20);
const midi = (n) => 440 * Math.pow(2, (n - 69) / 12);

/**
 * Pone un sonido en `bus` desde t0 durante dur. fn(t, tAbs) da una muestra
 * mono (se panea) o [l, r]. o: { g (dB), pan, sala, grande } (envios 0..1).
 */
function poner (bus, t0, dur, fn, o = {}) {
  const g = db(o.g ?? 0);
  const [pl, pr] = paneo(o.pan ?? 0);
  const i0 = Math.round(t0 * SR);
  const n = Math.round(dur * SR);
  for (let k = 0; k < n; k++) {
    const i = i0 + k;
    if (i < 0) continue;
    if (i >= N) break;
    const v = fn(k / SR, i / SR);
    let l, r;
    if (typeof v === 'number') { l = v * pl * g; r = v * pr * g; } else { l = v[0] * g; r = v[1] * g; }
    bus.L[i] += l;
    bus.R[i] += r;
    const m = (l + r) * 0.5;
    if (o.sala) bus.sala[i] += m * o.sala;
    if (o.grande) bus.grande[i] += m * o.grande;
  }
}

/* ------------------------------------------------------ instrumentos */

/** Ruido rosa estereo descorrelado. */
function ruidoEstereo (semilla) {
  const a = D.rosa(D.azar(semilla));
  const b = D.rosa(D.azar(semilla + 99));
  return () => [a(), b()];
}

/** Campana: parciales inarmonicos con caida (mas rapida arriba). */
function campana (f, { parciales = [[1, 1], [2.0, 0.45], [2.76, 0.35], [5.4, 0.18], [8.93, 0.07]], caida = 1.2 } = {}) {
  return (t) => {
    let s = 0;
    for (const [m, a] of parciales) s += a * Math.sin(TAU * f * m * t) * Math.exp(-t * (0.8 + m * 0.5) / caida);
    return s * Math.min(1, t / 0.003) * 0.35;
  };
}

/** Golpe metalico grande: parciales inarmonicos, batidos y un golpe de ruido. */
function golpeMetal (f, { caida = 1.0, ruido = 0.6, semilla = 1 } = {}) {
  const rnd = D.azar(semilla);
  const parc = [1, 1.47, 2.09, 2.56, 3.17, 4.03, 5.21].map((m, i) => [m * f * (1 + (rnd() - 0.5) * 0.02), 1 / (1 + i * 0.7), 0.5 + rnd() * 0.8]);
  const lp = new D.Biquad('lp', 2500, 0.7);
  return (t) => {
    let s = 0;
    for (const [fr, a, d] of parc) s += a * Math.sin(TAU * fr * t + Math.sin(TAU * 3.1 * t) * 0.5) * Math.exp(-t / (caida * d));
    const n = lp.paso((rnd() * 2 - 1)) * Math.exp(-t / 0.06) * ruido * 3;
    return (s * 0.35 + n) * Math.min(1, t / 0.001);
  };
}

/** Golpe grave con caida de tono (bombo, impacto). */
function bum (f0, f1, caida, { click = 0.4, semilla = 2 } = {}) {
  const rnd = D.azar(semilla);
  const lp = new D.Biquad('lp', 1800, 0.7);
  let fase = 0;
  return (t) => {
    const f = f1 + (f0 - f1) * Math.exp(-t / (caida * 0.25));
    fase += f / SR;
    const s = Math.tanh(Math.sin(TAU * fase) * 1.8) * Math.exp(-t / caida);
    const c = lp.paso(rnd() * 2 - 1) * Math.exp(-t / 0.012) * click;
    return s + c;
  };
}

/** Crujido de metal bajo tension: impulsos irregulares por resonadores muy agudos. */
function crujido (dur, f0, f1, semilla, { q = 28, densidad = 1 } = {}) {
  const rnd = D.azar(semilla);
  const res = [[1, 1], [2.31, 0.55], [3.87, 0.3]].map(([m, a]) => [new D.Biquad('bp', f0 * m, q), m, a]);
  let siguiente = 0;
  return (t) => {
    const f = f0 + (f1 - f0) * (t / dur);
    if ((Math.round(t * SR)) % 64 === 0) for (const [bp, m] of res) bp.ajustar(f * m, q);
    let x = 0;
    if (t >= siguiente) {
      x = (rnd() * 2 - 1) * 3;
      siguiente = t + (0.003 + rnd() * 0.018) / densidad * (1 + 0.6 * Math.sin(t * 2.7));
    }
    let s = 0;
    for (const [bp, , a] of res) s += bp.paso(x) * a;
    return s * Math.pow(Math.sin(Math.PI * Math.min(1, t / dur)), 0.6);
  };
}

/** Timbre de alarma electrico: martillo a 22 golpes/s sobre una campana. */
function alarma (f = 1180) {
  return (t) => {
    const golpe = ((t * 22) % 1) / 22;
    const e = Math.exp(-golpe / 0.035) * 0.8 + 0.25;
    const s = Math.sin(TAU * f * t) + 0.5 * Math.sin(TAU * f * 2.32 * t) + 0.25 * Math.sin(TAU * f * 3.91 * t);
    return Math.tanh(s * e * 1.4) * 0.3;
  };
}

/** Braaam: metales graves desafinados que se abren y se cierran. */
function braaam (notas, dur, semilla) {
  const rnd = D.azar(semilla);
  const osc = [];
  for (const n of notas) for (let k = 0; k < 3; k++) osc.push({ f: midi(n) * (1 + (k - 1) * 0.004 + (rnd() - 0.5) * 0.002), fase: rnd() });
  const lpL = new D.Biquad('lp', 300, 1.1);
  const lpR = new D.Biquad('lp', 300, 1.1);
  return (t) => {
    let l = 0;
    let r = 0;
    osc.forEach((o, i) => {
      const dt = o.f / SR;
      o.fase = (o.fase + dt) % 1;
      const s = D.sierra(o.fase, dt);
      if (i % 2) l += s; else r += s;
    });
    const corte = 250 + 1500 * Math.min(1, t / 0.12) * Math.exp(-t / (dur * 0.35));
    if ((Math.round(t * SR)) % 32 === 0) { lpL.ajustar(corte, 1.1); lpR.ajustar(corte * 1.05, 1.1); }
    const env = Math.min(1, t / 0.06) * Math.exp(-t / (dur * 0.5));
    const k = 1 / Math.sqrt(osc.length);
    return [Math.tanh(lpL.paso(l * k) * 2.2) * env, Math.tanh(lpR.paso(r * k) * 2.2) * env];
  };
}

/** Riser de Shepard: sube sin parar (y cada vez mas fuerte). */
function shepard (ritmoOct, base = 55) {
  const fases = new Float64Array(8);
  return (t) => {
    const p = (t * ritmoOct) % 1;
    let s = 0;
    for (let i = 0; i < 8; i++) {
      const f = base * Math.pow(2, i + p);
      const x = Math.log2(f / 440);
      const a = Math.exp(-x * x * 0.9);
      fases[i] += f / SR;
      s += a * Math.sin(TAU * fases[i]);
    }
    return s * 0.3;
  };
}

/** Latido: lub-dub grave. */
function latido (t) {
  const lub = Math.sin(TAU * 52 * t) * Math.exp(-t / 0.07) * Math.min(1, t / 0.004);
  const u = t - 0.24;
  const dub = u > 0 ? Math.sin(TAU * 44 * u) * Math.exp(-u / 0.09) * 0.75 * Math.min(1, u / 0.004) : 0;
  return Math.tanh((lub + dub) * 1.6);
}

/** Respiracion: inspirar (agudo) y espirar (grave) con ruido filtrado. */
function respiracion (periodo, semilla, intensidad = 1) {
  const rnd = D.azar(semilla);
  const bpIn = new D.Biquad('bp', 1800, 0.9);
  const bpOut = new D.Biquad('bp', 700, 0.8);
  return (t) => {
    const ph = (t % periodo) / periodo;
    const x = rnd() * 2 - 1;
    const ins = ph < 0.4 ? Math.sin(Math.PI * ph / 0.4) : 0;
    const esp = ph > 0.45 && ph < 0.95 ? Math.sin(Math.PI * (ph - 0.45) / 0.5) : 0;
    return (bpIn.paso(x) * ins * 0.8 + bpOut.paso(x) * esp * 1.2) * intensidad;
  };
}

/** Rafaga de fuego (fwoosh) y rugido con chasquidos. */
function fuego (semilla, { pop = 40 } = {}) {
  const rnd = D.azar(semilla);
  const br = D.marron(D.azar(semilla + 1));
  const lp = new D.Biquad('lp', 450, 0.7);
  const bpPop = new D.Biquad('bp', 1600, 2.5);
  return (t) => {
    const rugido = lp.paso(br()) * 1.4 * (0.8 + 0.2 * Math.sin(t * 13 + Math.sin(t * 3.7) * 2));
    const p = rnd() < pop / SR ? (rnd() * 2 - 1) * 6 : 0;
    return rugido + bpPop.paso(p);
  };
}

/** Chirrido de freno contra los railes: ruido muy resonante que tiembla. */
function chirrido (semilla) {
  const rnd = D.azar(semilla);
  const bp1 = new D.Biquad('bp', 2600, 30);
  const bp2 = new D.Biquad('bp', 3900, 25);
  const bp3 = new D.Biquad('bp', 1100, 6);
  let tirones = 0;
  return (t) => {
    const f = 2500 + 500 * Math.sin(t * 6.3) + 250 * Math.sin(t * 17.1) + (rnd() - 0.5) * 120;
    if ((Math.round(t * SR)) % 64 === 0) { bp1.ajustar(f, 30); bp2.ajustar(f * 1.52, 25); }
    if (rnd() < 45 / SR) tirones = 1;
    tirones *= 0.9995;
    const x = (rnd() * 2 - 1) * (0.5 + tirones);
    return Math.tanh((bp1.paso(x) * 4 + bp2.paso(x) * 2.5 + bp3.paso(x) * 0.8) * 1.5) * 0.6;
  };
}

/** Viento de caida: ruido rosa en banda que sube con la velocidad. */
function viento (semilla, centro) {
  const n = ruidoEstereo(semilla);
  const bpL = new D.Biquad('bp', 300, 0.7);
  const bpR = new D.Biquad('bp', 320, 0.7);
  const lpL = new D.Biquad('lp', 160, 0.7);
  const lpR = new D.Biquad('lp', 170, 0.7);
  return (t, ta) => {
    const c = centro(ta);
    if ((Math.round(t * SR)) % 64 === 0) { bpL.ajustar(c, 0.7); bpR.ajustar(c * 1.07, 0.7); }
    const [a, b] = n();
    return [bpL.paso(a) * 2 + lpL.paso(a) * 1.5, bpR.paso(b) * 2 + lpR.paso(b) * 1.5];
  };
}

/* ---------------------------------------------- musica de ascensor */

/** Bossa nova liminal: piano electrico, bajo, vibrafono y escobillas. */
function musicaAscensor (segundos) {
  const M = new Float32Array(Math.ceil(segundos * SR));
  const beat = 60 / 100;
  const acordes = [
    [[52, 55, 59, 62], 36], [[55, 59, 60, 64], 33], [[53, 57, 60, 64], 38], [[53, 57, 59, 64], 31],
    [[50, 55, 59, 64], 40], [[49, 55, 57, 61], 33], [[53, 57, 60, 64], 38], [[53, 57, 59, 64], 31]];
  const melodia = [
    [0, 67, 1], [1, 69, 0.5], [1.5, 71, 1.5], [3, 74, 1], [4, 72, 1.5], [5.5, 71, 0.5], [6, 69, 1], [7, 64, 1],
    [8, 65, 1], [9, 69, 0.5], [9.5, 72, 1.5], [11, 76, 1], [12, 74, 2], [14, 71, 1], [15, 67, 1],
    [16, 76, 1], [17, 74, 0.5], [17.5, 71, 1.5], [19, 67, 1], [20, 69, 1.5], [21.5, 72, 0.5], [22, 71, 2], [24, 67, 4]];
  const sumar = (t0, dur, fn) => {
    const i0 = Math.round(t0 * SR);
    const n = Math.round(dur * SR);
    for (let k = 0; k < n && i0 + k < M.length; k++) M[i0 + k] += fn(k / SR);
  };
  // piano electrico (FM tipo Rhodes)
  const rhodes = (f, d) => (t) => {
    const idx = 1.6 * Math.exp(-t * 7) + 0.25;
    const s = Math.sin(TAU * f * t + idx * Math.sin(TAU * f * t)) + 0.15 * Math.sin(TAU * f * 14 * t) * Math.exp(-t * 30);
    return s * Math.exp(-t * 1.1) * Math.min(1, t / 0.004) * Math.min(1, (d - t) / 0.08) * 0.12;
  };
  const golpesComp = [0, 1.5, 2.5, 3.5];
  for (let b = 0; b * 4 * beat < segundos; b++) {
    const [voces, raiz] = acordes[b % acordes.length];
    const t0 = b * 4 * beat;
    for (const g of golpesComp) {
      const d = g === 0 ? 1.4 * beat : 0.9 * beat;
      for (const n of voces) sumar(t0 + g * beat + (n % 3) * 0.006, d, rhodes(midi(n), d));
    }
    // bajo: raiz y quinta
    const bajo = (f, d) => (t) => (Math.sin(TAU * f * t) + 0.25 * Math.sin(TAU * f * 2 * t)) * Math.exp(-t * 2.2) * Math.min(1, t / 0.01) * Math.min(1, (d - t) / 0.05) * 0.32;
    sumar(t0, 1.4 * beat, bajo(midi(raiz), 1.4 * beat));
    sumar(t0 + 1.5 * beat, 0.45 * beat, bajo(midi(raiz + 7), 0.45 * beat));
    sumar(t0 + 2 * beat, 1.4 * beat, bajo(midi(raiz), 1.4 * beat));
    sumar(t0 + 3.5 * beat, 0.45 * beat, bajo(midi(raiz + 7), 0.45 * beat));
    // escobillas en 2 y 4 y golpecitos en corcheas
    const rnd = D.azar(100 + b);
    const hp = new D.Biquad('hp', 4000, 0.7);
    for (let c = 0; c < 8; c++) {
      const fuerte = c === 2 || c === 6;
      sumar(t0 + c * beat / 2, 0.25, (t) => hp.paso(rnd() * 2 - 1) * (fuerte ? Math.min(1, t / 0.05) * Math.exp(-t * 9) * 0.22 : Math.exp(-t * 40) * 0.08));
    }
  }
  // vibrafono
  for (const [b0, n, d] of melodia) {
    const f = midi(n);
    sumar(b0 * beat, d * beat + 1.2, (t) => (Math.sin(TAU * f * t) + 0.18 * Math.sin(TAU * f * 4 * t) * Math.exp(-t * 6)) *
      (0.75 + 0.25 * Math.sin(TAU * 5.2 * t)) * Math.exp(-t * 1.3) * Math.min(1, t / 0.003) * 0.17);
  }
  return M;
}

/** Altavoz barato del ascensor: paso banda, un poco saturado. */
function altavoz (x) {
  const hp = new D.Biquad('hp', 320, 0.7);
  const lp = new D.Biquad('lp', 3400, 0.8);
  const pico = new D.Biquad('pico', 1200, 1.2, 4);
  const y = new Float32Array(x.length);
  for (let i = 0; i < x.length; i++) y[i] = Math.tanh(pico.paso(lp.paso(hp.paso(x[i]))) * 2.2) * 0.6;
  return y;
}

/* ------------------------------------------------------------ guion */

function componer () {
  const rnd = D.azar(7);

  // --- la videocamara se enciende
  poner(A, 0.1, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.25, { g: -8 });
  poner(A, 0.26, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.25, { g: -8 });
  {
    const hp = new D.Biquad('bp', 900, 1.5);
    poner(A, 0.0, 1.0, (t) => (Math.sin(TAU * (700 + 500 * t) * t) * 0.15 + hp.paso(rnd() * 2 - 1) * 0.6) * Math.sin(Math.PI * t), { g: -18 });
  }
  poner(A, 0.86, 0.03, (t) => (rnd() * 2 - 1) * Math.exp(-t * 200), { g: -14 });

  // --- vestibulo: murmullo de mucha gente que se apaga al cerrarse las puertas
  {
    const voces = [];
    for (let v = 0; v < 7; v++) {
      const r2 = D.azar(200 + v);
      voces.push({ f1: 450 + r2() * 250, f2: 1300 + r2() * 600, fase: r2() * 10, vel: 3.5 + r2() * 2.5, pan: r2() * 2 - 1,
        bp1: new D.Biquad('bp', 500, 4), bp2: new D.Biquad('bp', 1500, 5), n: D.rosa(r2) });
    }
    const lp = new D.Biquad('lp', 6000, 0.7);
    poner(A, 0.6, 3.4, (t, ta) => {
      let l = 0;
      let r = 0;
      for (const v of voces) {
        const x = v.n();
        const silaba = Math.max(0, Math.sin(TAU * v.vel * t + v.fase + Math.sin(t * 1.3 + v.fase) * 2));
        const s = (v.bp1.paso(x) + v.bp2.paso(x) * 0.6) * silaba;
        const [pl, pr] = paneo(v.pan);
        l += s * pl;
        r += s * pr;
      }
      const cierre = 1 - suave(T_CIERRA0, T_CIERRA1, ta);
      if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(400 + 5600 * cierre, 0.7);
      const amp = suave(0.6, 1.2, ta) * (0.15 + 0.85 * cierre);
      return [lp.paso(l) * amp * 1.4, lp.paso(r) * amp * 1.4];
    }, { g: -11, sala: 0.3 });
  }

  // --- ding-dong del ascensor
  poner(A, 1.45, 2.5, campana(midi(88)), { g: -9, sala: 0.5 });
  poner(A, 1.85, 2.8, campana(midi(84)), { g: -9, sala: 0.5 });

  // --- puertas que se cierran
  {
    const br = D.marron(D.azar(31));
    const lp = new D.Biquad('lp', 260, 0.8);
    const r3 = D.azar(32);
    const bp = new D.Biquad('bp', 2200, 6);
    poner(A, T_CIERRA0, T_CIERRA1 - T_CIERRA0, (t) => {
      const tic = r3() < 30 / SR ? (r3() * 2 - 1) * 2 : 0;
      return (lp.paso(br()) * 2.2 + bp.paso(tic)) * Math.sin(Math.PI * t / (T_CIERRA1 - T_CIERRA0));
    }, { g: -10, sala: 0.3 });
    poner(A, T_CIERRA1, 1.2, golpeMetal(190, { caida: 0.3, ruido: 0.5, semilla: 33 }), { g: -9, sala: 0.6 });
    poner(A, T_CIERRA1, 0.6, bum(110, 60, 0.18, { click: 0.2 }), { g: -10 });
  }

  // --- rele del motor y el motor
  poner(A, T_BAJA - 0.05, 0.05, (t) => (rnd() * 2 - 1) * Math.exp(-t * 150), { g: -12, pan: 0.4 });
  {
    let fase = 0;
    let fase2 = 0;
    const lp = new D.Biquad('lp', 240, 0.7);
    const bpCable = new D.Biquad('bp', 420, 3);
    const r4 = D.azar(41);
    poner(A, T_BAJA, T_ATASCO + 0.6 - T_BAJA, (t, ta) => {
      const arr = suave(0, 0.9, t);
      const parada = ta < T_ATASCO ? 1 : Math.exp(-(ta - T_ATASCO) / 0.12);
      const f = 48 * (0.6 + 0.4 * arr) * (0.4 + 0.6 * parada);
      fase += f / SR;
      fase2 += f * 1.5 / SR;
      const zumbido = lp.paso(D.sierra(fase % 1, f / SR) + 0.6 * D.sierra(fase2 % 1, f * 1.5 / SR));
      const cable = bpCable.paso(r4() * 2 - 1) * (0.6 + 0.4 * Math.sin(t * 2.3));
      return (zumbido * 0.9 + cable * 0.5) * arr * parada;
    }, { g: -10 });
    // las plantas pasando: un soplo cada 4 bloques (cada 1.33 s a 3 bloques/s)
    for (let tp = T_BAJA + 1.2; tp < T_ATASCO; tp += 4 / 3) {
      const bp = new D.Biquad('bp', 380, 1.4);
      const r5 = D.azar(Math.round(tp * 100));
      poner(A, tp - 0.25, 0.6, (t) => bp.paso(r5() * 2 - 1) * Math.sin(Math.PI * t / 0.6) * 1.2, { g: -17, pan: (r5() - 0.5) * 0.4 });
      poner(A, tp, 0.02, (t) => (r5() * 2 - 1) * Math.exp(-t * 300), { g: -24 });
    }
  }

  // --- la musica del ascensor, por el altavoz; a los 18 la cinta se arrastra
  {
    const dur = T_ATASCO - (T_BAJA + 0.3);
    const cruda = altavoz(musicaAscensor(dur + 2));
    const salida = new Float32Array(Math.ceil(dur * SR));
    let pos = 0;
    const r6 = D.azar(61);
    let caida = 1;
    for (let i = 0; i < salida.length; i++) {
      const ta = T_BAJA + 0.3 + i / SR;
      let vel = 1;
      if (ta > T_PARPADEO) {
        const u = (ta - T_PARPADEO) / (T_ATASCO - T_PARPADEO);
        vel = 1 - 0.45 * u * u + 0.04 * Math.sin(ta * 9);
        if (i % 2205 === 0) caida = r6() < 0.35 ? 0.1 : 1; // cortes con los parpadeos de la luz
      }
      pos += vel;
      const k = Math.floor(pos);
      const fr = pos - k;
      salida[i] = k + 1 < cruda.length ? (cruda[k] * (1 - fr) + cruda[k + 1] * fr) * caida : 0;
    }
    poner(A, T_BAJA + 0.3, dur, (t) => {
      const i = Math.round(t * SR);
      return (salida[i] || 0) * suave(0, 1.2, t);
    }, { g: -5, pan: 0.2, sala: 0.45 });
  }

  // --- la luz falla: chasquidos electricos (zzt), nada de yunques
  {
    const r7 = D.azar(71);
    const bp = new D.Biquad('bp', 3200, 1.2);
    let arco = 0;
    poner(A, T_PARPADEO, T_ATASCO + 0.6 - T_PARPADEO, (t) => {
      if (r7() < 9 / SR) arco = 0.04 + r7() * 0.08;
      let s = 0;
      if (arco > 0) {
        arco -= 1 / SR;
        const zumbido = Math.sign(Math.sin(TAU * 120 * t)) * 0.3;
        s = bp.paso((r7() * 2 - 1) * (r7() < 0.3 ? 1 : 0.2)) * 2 + zumbido * 0.4;
      }
      return s;
    }, { g: -14, pan: -0.1, sala: 0.3 });
  }

  // --- FRENAZO
  poner(A, T_ATASCO, 3.0, golpeMetal(95, { caida: 1.4, ruido: 1.0, semilla: 81 }), { g: -3, sala: 0.7, grande: 0.2 });
  poner(A, T_ATASCO, 1.5, bum(120, 38, 0.6, { click: 0.6, semilla: 82 }), { g: -2 });
  {
    // cadenas y chapas sueltas
    const r8 = D.azar(83);
    const bp = new D.Biquad('bp', 2600, 4);
    poner(A, T_ATASCO + 0.02, 1.4, (t) => bp.paso(r8() < 70 * Math.exp(-t * 2.5) / SR ? (r8() * 2 - 1) * 4 : 0), { g: -10, sala: 0.5 });
    // el motor se apaga con un quejido
    let f = 0;
    poner(A, T_ATASCO, 1.2, (t) => { f += (900 * Math.exp(-t * 3) + 40) / SR; return Math.sin(TAU * f) * Math.exp(-t * 2.5) * 0.3; }, { g: -14 });
    // polvo cayendo
    const hp = new D.Biquad('hp', 3000, 0.7);
    for (const t0 of [T_ATASCO + 0.1, 23.25, 25.05]) {
      const r9 = D.azar(Math.round(t0 * 10));
      poner(A, t0, 2.4, (t) => hp.paso(r9() < 400 * Math.exp(-t * 1.4) / SR ? r9() * 2 - 1 : 0) * 2, { g: -18, pan: (r9() - 0.5) });
    }
  }
  // rele de emergencia y alarma
  poner(A, T_ATASCO + 0.42, 0.04, (t) => (rnd() * 2 - 1) * Math.exp(-t * 180), { g: -10, pan: 0.5 });
  poner(A, T_ATASCO + 0.7, 1.5, alarma(), { g: -10, pan: 0.3, sala: 0.5 });
  poner(A, 23.7, 0.8, alarma(), { g: -10, pan: 0.3, sala: 0.5 });

  // crujidos del cable (cada vez peores) y quejidos agudos del metal
  [[21.0, 1.6, 110, 70, 1], [22.3, 2.2, 160, 95, 2], [23.9, 1.2, 130, 180, 3], [24.4, 1.8, 90, 60, 4], [25.6, 0.9, 200, 140, 5], [26.0, 0.55, 240, 300, 6]].forEach(([t0, d, f0, f1, s], i) => {
    poner(A, t0, d, crujido(d, f0, f1, 900 + s, { densidad: 1 + i * 0.25 }), { g: -6 + i * 0.6, pan: (i % 2 ? 0.3 : -0.3), sala: 0.6 });
  });
  poner(A, 22.6, 1.8, crujido(1.8, 1900, 2400, 951, { q: 45, densidad: 0.6 }), { g: -22, pan: -0.5, sala: 0.7 });
  // se escurre: rasponazo + golpe
  for (const [t0, g] of [[23.2, -6], [25.0, -3]]) {
    const r10 = D.azar(Math.round(t0 * 7));
    const bp = new D.Biquad('bp', 1500, 2);
    poner(A, t0, 0.3, (t) => bp.paso(r10() * 2 - 1) * Math.exp(-t * 9) * 3, { g, sala: 0.5 });
    poner(A, t0 + 0.1, 1.2, bum(100, 45, 0.35, { click: 0.4, semilla: 1 + t0 }), { g: g + 1 });
    poner(A, t0 + 0.1, 1.5, golpeMetal(130, { caida: 0.6, ruido: 0.5, semilla: Math.round(t0) }), { g: g - 3, sala: 0.6 });
  }
  // pitidos de error del display
  for (let tb = 23.2; tb < T_ROTURA; tb += 2 / 3) {
    poner(A, tb, 0.08, (t) => Math.sign(Math.sin(TAU * 1000 * t)) * 0.12, { g: -22 });
    poner(A, tb + 0.13, 0.08, (t) => Math.sign(Math.sin(TAU * 1000 * t)) * 0.12, { g: -22 });
  }
  // un gemido lejano desde el fondo del hueco (algo hay ahi abajo)
  {
    const r11 = D.azar(111);
    const bp1 = new D.Biquad('bp', 300, 8);
    const bp2 = new D.Biquad('bp', 800, 8);
    const lp = new D.Biquad('lp', 900, 0.7);
    poner(A, 24.3, 2.0, (t) => {
      const f = 260 - 60 * t + 20 * Math.sin(t * 5);
      if ((Math.round(t * SR)) % 64 === 0) { bp1.ajustar(f, 8); bp2.ajustar(f * 2.7, 8); }
      const x = r11() * 2 - 1;
      return lp.paso(bp1.paso(x) * 3 + bp2.paso(x) * 1.5) * Math.sin(Math.PI * t / 2.0);
    }, { g: -15, pan: -0.2, sala: 0.9, grande: 0.4 });
  }
  // tension: latidos, respiracion nerviosa y un pedal grave que sube
  for (let tl = 21.0, p = 0.8; tl < T_ROTURA; tl += p, p = Math.max(0.52, p * 0.95)) poner(A, tl, 0.5, latido, { g: -9 });
  poner(A, 20.8, T_ROTURA - 20.8, respiracion(1.25, 121, 1), { g: -20 });
  {
    let f1 = 0;
    let f2 = 0;
    poner(A, 21.0, T_ROTURA - 21.0, (t) => {
      const sube = Math.pow(2, t / 5.5 * 0.5);
      f1 += 41.2 * sube / SR;
      f2 += 41.2 * 1.06 * sube / SR;
      return (Math.sin(TAU * f1) + Math.sin(TAU * f2) * 0.7) * suave(0, 5.5, t) * 0.5;
    }, { g: -12, grande: 0.3 });
  }

  // --- SE PARTE EL CABLE
  poner(A, T_ROTURA, 0.02, (t) => (rnd() * 2 - 1) * 1.5, { g: -2, sala: 0.8 });
  {
    // latigazo: cuerda de acero que cae de tono
    let fase = 0;
    poner(A, T_ROTURA, 1.6, (t) => {
      const f = 120 + 900 * Math.exp(-t / 0.18);
      fase += f / SR;
      const s = D.sierra(fase % 1, f / SR) * 0.5 + Math.sin(TAU * fase * 2.01) * 0.3;
      return Math.tanh(s * 2) * Math.exp(-t / 0.5);
    }, { g: -6, sala: 0.6, grande: 0.5 });
    const bp = new D.Biquad('bp', 3000, 0.8);
    poner(A, T_ROTURA, 0.5, (t) => {
      if ((Math.round(t * SR)) % 64 === 0) bp.ajustar(4000 * Math.exp(-t * 6) + 300, 0.8);
      return bp.paso(rnd() * 2 - 1) * Math.exp(-t * 4) * 3;
    }, { g: -6, pan: 0.2 });
  }
  poner(A, T_ROTURA + 0.03, 6.0, braaam([24, 31, 36, 43], 6.0, 131), { g: -1, grande: 0.6 });
  poner(A, T_ROTURA + 0.03, 3.0, bum(70, 30, 1.6, { click: 0.8, semilla: 132 }), { g: 0, grande: 0.3 });

  // --- caida libre
  const vel = (ta) => ta < T_ROTURA + 0.3 ? 0 : 9.8 * (ta - T_ROTURA - 0.3);
  poner(A, T_ROTURA + 0.2, T_IMPACTO - T_ROTURA - 0.2, (() => {
    const v = viento(141, (ta) => 220 + vel(ta) * 9);
    return (t, ta) => {
      const [l, r] = v(t, ta);
      const k = Math.min(1, 0.15 + vel(ta) / 90) * (1 + 0.8 * suave(T_PUERTAS, T_PUERTAS + 0.4, ta));
      return [l * k, r * k];
    };
  })(), { g: -4 });
  {
    // traqueteo de la cabina y rellanos pasando como un aleteo grave
    const r12 = D.azar(151);
    const bp = new D.Biquad('bp', 2300, 5);
    const lp = new D.Biquad('lp', 130, 0.7);
    const br = D.marron(D.azar(152));
    let faseAleteo = 0;
    poner(A, T_ROTURA + 0.3, T_IMPACTO - T_ROTURA - 0.3, (t, ta) => {
      const v = vel(ta);
      const tic = r12() < (20 + v * 3) / SR ? (r12() * 2 - 1) * 3 : 0;
      faseAleteo += (v / 4) / SR;
      const aleteo = Math.pow(Math.max(0, Math.sin(TAU * faseAleteo)), 6);
      const rumor = lp.paso(br()) * (0.5 + aleteo * 1.5) * Math.min(1, v / 40);
      return bp.paso(tic) * 0.8 + rumor * 2;
    }, { g: -7, sala: 0.3 });
  }
  poner(A, 27.5, T_IMPACTO - 27.5, (() => { const c = chirrido(161); return (t) => c(t) * suave(0, 1.5, t) * (0.6 + 0.4 * Math.sin(t * 2.1)); })(), { g: -9, pan: -0.15, sala: 0.4 });
  {
    // chispas
    const r13 = D.azar(171);
    const hp = new D.Biquad('hp', 4500, 0.7);
    poner(A, 27.6, T_IMPACTO - 27.6, (t) => hp.paso(r13() < 160 / SR ? (r13() * 2 - 1) * 3 : 0) * suave(0, 1, t), { g: -14, pan: 0.2 });
  }
  // FUEGO: fwoosh y rugido
  {
    const r14 = D.azar(181);
    const lp = new D.Biquad('lp', 200, 0.8);
    poner(A, T_FUEGO, 1.2, (t) => {
      if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(150 + 3000 * Math.min(1, t / 0.35), 0.8);
      return lp.paso(r14() * 2 - 1) * Math.sin(Math.PI * Math.min(1, t / 1.2)) * 3;
    }, { g: -5, sala: 0.5 });
    const f = fuego(182, { pop: 55 });
    poner(A, T_FUEGO + 0.2, T_IMPACTO - T_FUEGO - 0.2, (t, ta) => f(t) * suave(0, 1.5, t) * (1 + 0.7 * suave(T_PUERTAS, T_PUERTAS + 0.4, ta)), { g: -8, sala: 0.3 });
  }
  // alarma desesperada durante la caida
  poner(A, 27.4, T_IMPACTO - 27.4, (() => { const a = alarma(1240); return (t) => a(t) * 0.8; })(), { g: -16, pan: 0.35, sala: 0.5 });
  // las puertas crujen: 10 grietas
  for (let k = 0; k < 10; k++) {
    const t0 = T_GRIETAS + k * (T_PUERTAS - T_GRIETAS) / 10;
    const r15 = D.azar(300 + k);
    const bp = new D.Biquad('bp', 700 + r15() * 600, 3);
    poner(A, t0, 0.35, (t) => bp.paso(r15() * 2 - 1) * Math.exp(-t * 14) * (2 + k * 0.25), { g: -9 + k * 0.4, pan: (r15() - 0.5) * 0.6, sala: 0.5 });
    poner(A, t0, 0.5, crujido(0.5, 300 + k * 30, 250, 400 + k, { q: 18, densidad: 2 }), { g: -10, sala: 0.5 });
  }
  // REVIENTAN LAS PUERTAS
  poner(A, T_PUERTAS, 3.0, golpeMetal(150, { caida: 1.1, ruido: 1.4, semilla: 331 }), { g: -2, sala: 0.6, grande: 0.3 });
  poner(A, T_PUERTAS, 2.0, bum(90, 35, 0.8, { click: 1.0, semilla: 332 }), { g: -2 });
  poner(A, T_PUERTAS + 0.02, 5.0, braaam([25, 32, 37, 44, 49], 5.0, 333), { g: -2, grande: 0.6 });
  {
    const r16 = D.azar(334);
    const bp = new D.Biquad('bp', 3200, 3);
    poner(A, T_PUERTAS + 0.05, 1.6, (t) => bp.paso(r16() < 300 * Math.exp(-t * 2) / SR ? (r16() * 2 - 1) * 4 : 0), { g: -8, sala: 0.6 });
  }
  // riser de Shepard y tambores cada vez mas seguidos
  poner(A, T_ROTURA + 0.5, T_IMPACTO - T_ROTURA - 0.5, (() => { const s = shepard(0.14, 40); return (t) => s(t) * (0.2 + 0.8 * Math.pow(t / 12, 1.5)); })(), { g: -9, grande: 0.3 });
  for (let tt = 30.0, p = 1.5; tt < T_IMPACTO - 0.05; tt += p, p = Math.max(0.17, p * 0.86)) {
    poner(A, tt, 0.9, bum(85, 42, 0.32, { click: 0.5, semilla: Math.round(tt * 100) }), { g: -6, grande: 0.25 });
  }
  // el ultimo segundo: un pitido que sube
  {
    let f = 0;
    poner(A, T_IMPACTO - 1.6, 1.6, (t) => { f += (1500 + 5000 * Math.pow(t / 1.6, 2)) / SR; return Math.sin(TAU * f) * Math.pow(t / 1.6, 2) * 0.3; }, { g: -12 });
  }

  // --- IMPACTO
  poner(A, T_IMPACTO, 3.5, bum(95, 28, 2.4, { click: 1.0, semilla: 401 }), { g: 2, grande: 0.5 });
  poner(A, T_IMPACTO, 3.0, golpeMetal(70, { caida: 1.6, ruido: 2.0, semilla: 402 }), { g: 0, grande: 0.6 });
  {
    const r17 = D.azar(403);
    const lp = new D.Biquad('lp', 9000, 0.7);
    poner(A, T_IMPACTO, 2.0, (t) => {
      if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(200 + 9000 * Math.exp(-t * 3), 0.7);
      return lp.paso(r17() * 2 - 1) * Math.exp(-t * 2.2) * 3;
    }, { g: -2, grande: 0.6 });
    // cristales
    for (let k = 0; k < 40; k++) {
      const f = 3000 + r17() * 6000;
      poner(A, T_IMPACTO + r17() * 0.6, 0.15, (t) => Math.sin(TAU * f * t) * Math.exp(-t * (20 + r17() * 30)) * 0.4, { g: -12, pan: r17() * 2 - 1, sala: 0.6 });
    }
  }

  /* ----------------------------------- despues del golpe (bus B) */

  // pitido en los oidos
  poner(B, T_IMPACTO + 0.1, 13.5, (t) => {
    const env = Math.min(1, t / 0.08) * (t < 6 ? 1 : Math.exp(-(t - 6) / 2.4));
    return (Math.sin(TAU * 4200 * t) + 0.35 * Math.sin(TAU * 4237 * t) + 0.1 * Math.sin(TAU * 8400 * t)) * env * (0.9 + 0.1 * Math.sin(t * 0.9));
  }, { g: -15 });
  // latidos sordos que se calman
  {
    const lp = new D.Biquad('lp', 140, 0.7);
    let tl = T_IMPACTO + 0.6;
    let p = 0.62;
    while (tl < 53) {
      const t0 = tl;
      poner(B, t0, 0.55, (t) => lp.paso(latido(t)) * 2.2, { g: -6 - Math.max(0, t0 - 48) * 1.2 });
      tl += p;
      p = Math.min(1.0, p * 1.04);
    }
  }
  // rumor sordo de escombros asentandose
  {
    const br = D.marron(D.azar(501));
    const lp = new D.Biquad('lp', 160, 0.7);
    poner(B, T_IMPACTO + 0.1, 6.0, (t) => lp.paso(br()) * 2 * Math.exp(-t / 2.5), { g: -10 });
  }
  // respiracion al despertar: entrecortada y luego mas tranquila
  poner(B, T_DESPERTAR + 0.3, 9.0, (() => { const r = respiracion(1.6, 601, 1); return (t) => r(t) * Math.min(1, t / 1.5) * (1 - suave(6, 9, t) * 0.5); })(), { g: -16 });
  poner(B, T_DESPERTAR + 0.2, 0.6, (() => { const r = D.azar(602); const bp = new D.Biquad('bp', 1400, 1); return (t) => bp.paso(r() * 2 - 1) * Math.sin(Math.PI * t / 0.6) * 1.5; })(), { g: -12 });
  // el oido vuelve: el zumbido de los tubos amortiguado que se abre
  {
    let fase = 0;
    const lp = new D.Biquad('lp', 200, 0.7);
    poner(B, T_DESPERTAR, 10.5, (t) => {
      fase += 120 / SR;
      if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(200 + 1800 * suave(1, 7, t), 0.7);
      const s = Math.sin(TAU * fase) + 0.55 * Math.sin(TAU * fase * 3) + 0.2 * Math.sin(TAU * fase * 5);
      return lp.paso(s) * suave(0.5, 3, t) * (1 - suave(8, 10.5, t)) * 0.4;
    }, { g: -16 });
  }
  // titulo: ola de ruido que sube, golpe grave y acorde menor que se queda
  {
    const r18 = D.azar(701);
    const lp = new D.Biquad('lp', 300, 0.7);
    poner(B, T_TITULO - 1.6, 1.6, (t) => {
      if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(300 + 6000 * Math.pow(t / 1.6, 2), 0.7);
      return lp.paso(r18() * 2 - 1) * Math.pow(t / 1.6, 3) * 1.5;
    }, { g: -12, grande: 0.6 });
    poner(B, T_TITULO, 4.5, bum(60, 32, 1.8, { click: 0.6, semilla: 702 }), { g: -2, grande: 0.7 });
    poner(B, T_TITULO, 5.0, braaam([33, 40, 45, 48], 5.0, 703), { g: -9, grande: 0.8 });
  }
}

/* ---------------------------------------------------------- mezcla */

function reverb (envio, opciones) {
  const rev = new D.Freeverb(opciones);
  const l = new Float32Array(N);
  const r = new Float32Array(N);
  for (let i = 0; i < N; i++) {
    const [a, b] = rev.paso(envio[i]);
    l[i] = a;
    r[i] = b;
  }
  return [l, r];
}

function mezclarBus (bus) {
  const [sl, sr] = reverb(bus.sala, { sala: 0.45, amortiguacion: 0.55, predelayMs: 6 });
  const [gl, gr] = reverb(bus.grande, { sala: 0.95, amortiguacion: 0.3, predelayMs: 30 });
  for (let i = 0; i < N; i++) {
    bus.L[i] += sl[i] * 0.9 + gl[i] * 1.3;
    bus.R[i] += sr[i] * 0.9 + gr[i] * 1.3;
  }
}

/** A partir del impacto el bus A se oye como con los oidos tapados. */
function sordera (bus) {
  const lpL = new D.Biquad('lp', 18000, 0.7);
  const lpR = new D.Biquad('lp', 18000, 0.7);
  const i0 = Math.round((T_IMPACTO + 0.08) * SR);
  for (let i = i0; i < N; i++) {
    const u = (i - i0) / SR;
    if ((i - i0) % 64 === 0) {
      const c = 250 + 17000 * Math.exp(-u / 0.06);
      lpL.ajustar(c, 0.7);
      lpR.ajustar(c, 0.7);
    }
    const g = 0.12 + 0.88 * Math.exp(-u / 0.05);
    bus.L[i] = lpL.paso(bus.L[i]) * g;
    bus.R[i] = lpR.paso(bus.R[i]) * g;
  }
}

if (require.main === module) (async () => {
  const t0 = Date.now();
  componer();
  mezclarBus(A);
  sordera(A);
  mezclarBus(B);
  const L = new Float32Array(N);
  const R = new Float32Array(N);
  for (let i = 0; i < N; i++) {
    L[i] = A.L[i] + B.L[i];
    R[i] = A.R[i] + B.R[i];
  }
  // limitador suave y normalizacion a -1 dBFS
  let pico = 0;
  for (let i = 0; i < N; i++) pico = Math.max(pico, Math.abs(L[i]), Math.abs(R[i]));
  const pre = 1.6 / (pico || 1);
  let pico2 = 0;
  for (let i = 0; i < N; i++) {
    L[i] = Math.tanh(L[i] * pre);
    R[i] = Math.tanh(R[i] * pre);
    pico2 = Math.max(pico2, Math.abs(L[i]), Math.abs(R[i]));
  }
  const g = db(-1) / pico2;
  for (let i = 0; i < N; i++) { L[i] *= g; R[i] *= g; }
  const audio = D.fundidos([L, R], 0.01, 0.8);
  // volumen por tramos (para revisar la mezcla sin oirla)
  for (let s = 0; s < DUR; s += 2.5) {
    let e = 0;
    const a = Math.round(s * SR);
    const b = Math.min(N, Math.round((s + 2.5) * SR));
    for (let i = a; i < b; i++) e += audio[0][i] * audio[0][i];
    const rms = 20 * Math.log10(Math.sqrt(e / (b - a)) + 1e-9);
    console.log(`  ${s.toFixed(1).padStart(5)} s  ${rms.toFixed(1).padStart(6)} dB  ${'#'.repeat(Math.max(0, Math.round((rms + 50) / 1.5)))}`);
  }
  const bytes = await D.guardarOgg(SALIDA, audio, 5);
  console.log(`cinematica.ogg ${(N / SR).toFixed(1)} s, ${(bytes / 1024).toFixed(0)} KB (${((Date.now() - t0) / 1000).toFixed(1)} s)`);
})();
