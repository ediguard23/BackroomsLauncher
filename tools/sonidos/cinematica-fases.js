#!/usr/bin/env node
'use strict';
/**
 * Bandas sonoras de las bajadas a la fase 2 y a la fase 3, sincronizadas con
 * los guiones 2 y 3 de evento/.../shaders/core/cinematica.fsh (y los tiempos de
 * CinematicaCliente.java y Fases#bajada). Los instrumentos son los de
 * cinematica.js (la del /start).
 *
 * Fase 2 (33 s), "el ascensor que hay que arreglar":
 *   0.9 pitido del panel; 2.0 se cierran las puertas sobre el Nivel 0 (zumbido de tubos)
 *   3.6 arranca, musica de ascensor por el altavoz
 *   7.6 FRENAZO: se va la luz, alarma, pitidos de error
 *  10.6 / 11.6 / 12.5 golpes al panel con chispazos; 13.2 vuelve la luz y el motor
 *  15.0 sigue bajando (la musica, mas torcida); 20.4 se escurre y cae: viento, freno que chirria
 *  21.6 el freno de emergencia lo para en seco; 24.0 las puertas se abren a la fuerza
 *  24.5 sector B: goteras, tubos que fallan; 25.5 pasos sobre moqueta mojada; 27.8 se corta
 *  28.9 golpe grave y acorde del titulo FASE 2
 *
 * Fase 3 (30.6 s), "algo en el hueco":
 *   0.6 sector B; 1.4 pasos pesados de algo que cruza el pasillo; 2.0 se cierran las puertas
 *   3.6 baja despacio, el cable cruje; 8.6 apagon (se apaga todo); 9.8 vision nocturna
 *  11.5 garras en las puertas (chirrido de metal) y respiracion; 12.9 portazo
 *  14.6-18.1 golpes en el techo; 18.6 se escurre; 19.6 vuelve la luz roja y la alarma
 *  21.2 las puertas se abren solas; 23.6 silencio... 24.6 SUSTO; 25.4 se corta
 *  25.9 golpe y acorde del titulo FASE 3
 *
 * Todo es sintesis propia. Sale a evento/src/main/resources/assets/backrooms_evento/
 * sounds/cinematica_fase2.ogg y cinematica_fase3.ogg, y lo de despues del viaje
 * (Fases#bajada: 28.2 y 25.8 s) a cinematica_fase2_despues.ogg y _fase3_despues.ogg:
 * Minecraft corta todo al cambiar de mundo y el cliente lo pone al llegar.
 *
 *   node tools/sonidos/cinematica-fases.js [2|3]
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');
const C = require('./cinematica');

const { SR } = D;
const TAU = Math.PI * 2;
const { suave, db, midi, paneo, campana, golpeMetal, bum, crujido, alarma, braaam, latido, respiracion, chirrido, viento, musicaAscensor, altavoz } = C;
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento');

/* ---------------------------------------------------- mesa de mezclas */

function mesa (dur) {
  const N = Math.ceil(dur * SR);
  const bus = { N, L: new Float32Array(N), R: new Float32Array(N), sala: new Float32Array(N), grande: new Float32Array(N) };
  /** Pone un sonido desde t0 durante dur: fn(t, tAbs) da mono (se panea) o [l, r]. */
  bus.poner = (t0, d, fn, o = {}) => {
    const g = db(o.g ?? 0);
    const [pl, pr] = paneo(o.pan ?? 0);
    const i0 = Math.round(t0 * SR);
    const n = Math.round(d * SR);
    for (let k = 0; k < n; k++) {
      const i = i0 + k;
      if (i < 0) continue;
      if (i >= N) break;
      const v = fn(k / SR, i / SR);
      let l;
      let r;
      if (typeof v === 'number') { l = v * pl * g; r = v * pr * g; } else { l = v[0] * g; r = v[1] * g; }
      bus.L[i] += l;
      bus.R[i] += r;
      const m = (l + r) * 0.5;
      if (o.sala) bus.sala[i] += m * o.sala;
      if (o.grande) bus.grande[i] += m * o.grande;
    }
  };
  return bus;
}

function mezclar (bus) {
  const rev = (envio, op) => {
    const r = new D.Freeverb(op);
    const l = new Float32Array(bus.N);
    const d = new Float32Array(bus.N);
    for (let i = 0; i < bus.N; i++) { const [a, b] = r.paso(envio[i]); l[i] = a; d[i] = b; }
    return [l, d];
  };
  const [sl, sr] = rev(bus.sala, { sala: 0.45, amortiguacion: 0.55, predelayMs: 6 });
  const [gl, gr] = rev(bus.grande, { sala: 0.95, amortiguacion: 0.3, predelayMs: 30 });
  const L = new Float32Array(bus.N);
  const R = new Float32Array(bus.N);
  let pico = 0;
  for (let i = 0; i < bus.N; i++) {
    L[i] = bus.L[i] + sl[i] * 0.9 + gl[i] * 1.3;
    R[i] = bus.R[i] + sr[i] * 0.9 + gr[i] * 1.3;
    pico = Math.max(pico, Math.abs(L[i]), Math.abs(R[i]));
  }
  // limitador suave y normalizacion a -1 dBFS
  const pre = 1.6 / (pico || 1);
  let pico2 = 0;
  for (let i = 0; i < bus.N; i++) {
    L[i] = Math.tanh(L[i] * pre);
    R[i] = Math.tanh(R[i] * pre);
    pico2 = Math.max(pico2, Math.abs(L[i]), Math.abs(R[i]));
  }
  const g = db(-1) / pico2;
  for (let i = 0; i < bus.N; i++) { L[i] *= g; R[i] *= g; }
  return D.fundidos([L, R], 0.01, 0.8);
}

/* ------------------------------------------------------ instrumentos */

/** Zumbido de los tubos del Nivel 0 (120 Hz y armonicos); `vivo(ta)` 0..1 lo corta. */
function tubos (vivo, semilla = 1) {
  const rnd = D.azar(semilla);
  const lp = new D.Biquad('lp', 2400, 0.7);
  let fase = 0;
  return (t, ta) => {
    fase += 120 / SR;
    const s = Math.sin(TAU * fase) + 0.5 * Math.sin(TAU * fase * 3) + 0.25 * Math.sign(Math.sin(TAU * fase * 2)) * 0.4;
    const chisporroteo = rnd() < 30 / SR ? (rnd() * 2 - 1) : 0;
    return lp.paso(s * 0.3 + chisporroteo) * vivo(ta);
  };
}

/** Gota que cae en un charco: "plic" con el tono hacia arriba. */
function gota (semilla) {
  const rnd = D.azar(semilla);
  const f0 = 700 + rnd() * 500;
  let fase = 0;
  return (t) => {
    fase += (f0 * (1 + 1.4 * Math.min(1, t / 0.035))) / SR;
    return Math.sin(TAU * fase) * Math.exp(-t / 0.05) * Math.min(1, t / 0.002) * 0.5;
  };
}

/** Paso sobre moqueta mojada: golpe sordo y un chapoteo. */
function pasoMojado (semilla) {
  const rnd = D.azar(semilla);
  const lp = new D.Biquad('lp', 260, 0.8);
  const bp = new D.Biquad('bp', 900 + rnd() * 300, 1.6);
  return (t) => {
    const x = rnd() * 2 - 1;
    return lp.paso(x) * Math.exp(-t / 0.05) * 2.2 + bp.paso(x) * Math.exp(-Math.max(0, t - 0.03) / 0.09) * (t > 0.03 ? 1.2 : 0);
  };
}

/** Pisada de algo enorme lejos: golpe grave con un arrastre. */
function pisadaGrande (semilla) {
  const b = bum(70, 38, 0.35, { click: 0.15, semilla });
  const rnd = D.azar(semilla + 7);
  const lp = new D.Biquad('lp', 500, 0.7);
  return (t) => b(t) * 0.9 + lp.paso(rnd() * 2 - 1) * Math.exp(-Math.max(0, t - 0.1) / 0.25) * (t > 0.1 ? 0.8 : 0);
}

/** Respiracion humeda y grave de algo que no es una persona. */
function resuello (dur, semilla) {
  const rnd = D.azar(semilla);
  const bp1 = new D.Biquad('bp', 180, 5);
  const bp2 = new D.Biquad('bp', 520, 6);
  const lp = new D.Biquad('lp', 1200, 0.7);
  return (t) => {
    const ph = (t % 1.7) / 1.7;
    const env = ph < 0.55 ? Math.sin(Math.PI * ph / 0.55) : Math.sin(Math.PI * (ph - 0.55) / 0.45) * 0.6;
    const x = rnd() * 2 - 1;
    const gruñido = 1 + 0.6 * Math.sin(TAU * 31 * t) * (ph < 0.55 ? 0.3 : 1);
    return lp.paso(bp1.paso(x) * 3 * gruñido + bp2.paso(x) * 1.5) * env * Math.sin(Math.PI * Math.min(1, t / dur));
  };
}

/** Chispazo electrico: arco que chisporrotea. */
function chispazoSonido (semilla, dur = 0.35) {
  const rnd = D.azar(semilla);
  const bp = new D.Biquad('bp', 3400, 1.2);
  return (t) => {
    const zumbido = Math.sign(Math.sin(TAU * 120 * t)) * 0.25;
    const x = (rnd() * 2 - 1) * (rnd() < 0.35 ? 1 : 0.25);
    return (bp.paso(x) * 2.5 + zumbido * 0.5) * Math.exp(-t / (dur * 0.5));
  };
}

/** Pitido corto (electronica de la videocamara, del panel o del display). */
const pitido = (f, d) => (t) => Math.sign(Math.sin(TAU * f * t)) * 0.12 * Math.min(1, t / 0.003) * Math.min(1, (d - t) / 0.01);

/** Motor del ascensor: zumbido grave y el cable silbando; arranca suave y para con quejido. */
function motor (t0, t1, base = 48, semilla = 41) {
  let fase = 0;
  let fase2 = 0;
  const lp = new D.Biquad('lp', 240, 0.7);
  const bpCable = new D.Biquad('bp', 420, 3);
  const r = D.azar(semilla);
  return (t, ta) => {
    const arr = suave(0, 0.9, t);
    const parada = ta < t1 ? 1 : Math.exp(-(ta - t1) / 0.12);
    const f = base * (0.6 + 0.4 * arr) * (0.4 + 0.6 * parada);
    fase += f / SR;
    fase2 += f * 1.5 / SR;
    const zumbido = lp.paso(D.sierra(fase % 1, f / SR) + 0.6 * D.sierra(fase2 % 1, f * 1.5 / SR));
    const cable = bpCable.paso(r() * 2 - 1) * (0.6 + 0.4 * Math.sin(t * 2.3));
    return (zumbido * 0.9 + cable * 0.5) * arr * parada;
  };
}

/** Musica del ascensor por el altavoz, con la cinta torcida (vel(ta)) y cortes. */
function musica (bus, t0, t1, desde, vel, o) {
  const dur = t1 - t0;
  const cruda = altavoz(musicaAscensor(desde + dur * 1.3 + 2));
  const salida = new Float32Array(Math.ceil(dur * SR));
  let pos = desde * SR;
  for (let i = 0; i < salida.length; i++) {
    pos += vel(t0 + i / SR);
    const k = Math.floor(pos);
    const fr = pos - k;
    salida[i] = k + 1 < cruda.length ? cruda[k] * (1 - fr) + cruda[k + 1] * fr : 0;
  }
  bus.poner(t0, dur, (t) => (salida[Math.round(t * SR)] || 0) * suave(0, 0.8, t) * (1 - suave(dur - 0.1, dur, t)), o);
}

/** La videocamara se enciende: dos pitidos y el motor de la cinta. */
function camaraEnciende (bus, semilla) {
  const rnd = D.azar(semilla);
  bus.poner(0.1, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.25, { g: -8 });
  bus.poner(0.26, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.25, { g: -8 });
  const bp = new D.Biquad('bp', 900, 1.5);
  bus.poner(0.0, 1.0, (t) => (Math.sin(TAU * (700 + 500 * t) * t) * 0.15 + bp.paso(rnd() * 2 - 1) * 0.6) * Math.sin(Math.PI * t), { g: -18 });
  bus.poner(0.86, 0.03, (t) => (rnd() * 2 - 1) * Math.exp(-t * 200), { g: -14 });
}

/** Las puertas de acero del rellano se cierran (rodillos y golpe). */
function puertasCierran (bus, t0, t1, semilla) {
  const br = D.marron(D.azar(semilla));
  const lp = new D.Biquad('lp', 260, 0.8);
  const r = D.azar(semilla + 1);
  const bp = new D.Biquad('bp', 2200, 6);
  bus.poner(t0, t1 - t0, (t) => {
    const tic = r() < 30 / SR ? (r() * 2 - 1) * 2 : 0;
    return (lp.paso(br()) * 2.2 + bp.paso(tic)) * Math.sin(Math.PI * t / (t1 - t0));
  }, { g: -10, sala: 0.3 });
  bus.poner(t1, 1.2, golpeMetal(170, { caida: 0.3, ruido: 0.5, semilla: semilla + 2 }), { g: -9, sala: 0.6 });
  bus.poner(t1, 0.6, bum(110, 60, 0.18, { click: 0.2, semilla: semilla + 3 }), { g: -10 });
}

/** Puertas forzadas: metal que rechina y se arrastra. */
function puertasAbren (bus, t0, dur, semilla, g = -8) {
  bus.poner(t0, dur, crujido(dur, 150, 95, semilla, { q: 14, densidad: 1.6 }), { g, sala: 0.5 });
  const r = D.azar(semilla + 5);
  const bp = new D.Biquad('bp', 700, 2);
  bus.poner(t0, dur, (t) => bp.paso(r() * 2 - 1) * Math.sin(Math.PI * t / dur) * (0.6 + 0.4 * Math.sin(t * 13)) * 1.6, { g: g - 4, sala: 0.4 });
  bus.poner(t0 + dur - 0.05, 1.0, golpeMetal(140, { caida: 0.4, ruido: 0.4, semilla: semilla + 6 }), { g: g - 2, sala: 0.6 });
}

/** Golpe de reloj de la cinta al cortarse. */
function cintaCorta (bus, t, semilla) {
  const r = D.azar(semilla);
  const lp = new D.Biquad('lp', 9000, 0.7);
  bus.poner(t - 0.6, 0.6, (u) => lp.paso(r() * 2 - 1) * Math.pow(u / 0.6, 2) * 0.9, { g: -12 });
  bus.poner(t, 0.04, (u) => (r() * 2 - 1) * Math.exp(-u * 120), { g: -8 });
}

/** El titulo de la fase: ola de ruido, golpe grave y acorde menor que se queda. */
function titulo (bus, t0, notas, semilla) {
  const r = D.azar(semilla);
  const lp = new D.Biquad('lp', 300, 0.7);
  bus.poner(t0 - 1.4, 1.4, (t) => {
    if ((Math.round(t * SR)) % 64 === 0) lp.ajustar(300 + 6000 * Math.pow(t / 1.4, 2), 0.7);
    return lp.paso(r() * 2 - 1) * Math.pow(t / 1.4, 3) * 1.4;
  }, { g: -13, grande: 0.6 });
  bus.poner(t0, 4.0, bum(60, 32, 1.6, { click: 0.6, semilla: semilla + 1 }), { g: -3, grande: 0.7 });
  bus.poner(t0, 4.2, braaam(notas, 4.2, semilla + 2), { g: -10, grande: 0.8 });
}

/* ------------------------------------------------- fase 2: el arreglo */

function fase2 () {
  const DUR = 33.3;
  const A = mesa(DUR);
  const FRENO = 7.6;
  const GOLPES = [10.6, 11.6, 12.5];
  const ARREGLO = 13.2;
  const SIGUE = 15.0;
  const CAE = 20.4;
  const FRENA = 21.6;
  const ABRE = 24.0;
  const ANDA = 25.5;
  const FIN = 27.8;

  camaraEnciende(A, 2001);
  // el Nivel 0 al otro lado de las puertas: el zumbido de siempre, que se apaga al cerrarse
  A.poner(0.5, 3.3, tubos((ta) => suave(0.5, 1.0, ta) * (1 - 0.85 * suave(2.0, 3.4, ta)), 2002), { g: -13, sala: 0.4 });
  // el pitido del panel que acabas de pulsar y la campanilla
  A.poner(0.9, 0.09, pitido(1320, 0.09), { g: -16 });
  A.poner(1.05, 0.12, pitido(990, 0.12), { g: -16 });
  A.poner(1.3, 2.4, campana(midi(86)), { g: -12, sala: 0.5 });
  puertasCierran(A, 2.0, 3.4, 2010);

  // motor y musica de ascensor
  A.poner(3.55, 0.05, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 150), { g: -12, pan: 0.4 });
  A.poner(3.6, FRENO + 0.6 - 3.6, motor(3.6, FRENO), { g: -10 });
  musica(A, 3.9, FRENO, 6.0, (ta) => ta < 6.8 ? 1 : 1 - 0.4 * suave(6.8, FRENO, ta) + 0.05 * Math.sin(ta * 9), { g: -6, pan: 0.2, sala: 0.45 });
  // la luz falla: zzt
  for (const t0 of [6.9, 7.15, 7.4]) A.poner(t0, 0.18, chispazoSonido(Math.round(t0 * 100), 0.18), { g: -16, sala: 0.3 });

  // FRENAZO
  A.poner(FRENO, 3.0, golpeMetal(95, { caida: 1.4, ruido: 1.0, semilla: 2020 }), { g: -3, sala: 0.7, grande: 0.2 });
  A.poner(FRENO, 1.5, bum(120, 38, 0.6, { click: 0.6, semilla: 2021 }), { g: -2 });
  { let f = 0; A.poner(FRENO, 1.2, (t) => { f += (900 * Math.exp(-t * 3) + 40) / SR; return Math.sin(TAU * f) * Math.exp(-t * 2.5) * 0.3; }, { g: -14 }); }
  {
    const r = D.azar(2022);
    const hp = new D.Biquad('hp', 3000, 0.7);
    A.poner(FRENO + 0.1, 2.4, (t) => hp.paso(r() < 400 * Math.exp(-t * 1.4) / SR ? r() * 2 - 1 : 0) * 2, { g: -18 });
  }
  A.poner(FRENO + 0.45, 0.04, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 180), { g: -10, pan: 0.5 });
  A.poner(FRENO + 0.6, 1.3, alarma(), { g: -11, pan: 0.3, sala: 0.5 });
  // pitidos de error del panel mientras no se arregla
  for (let tb = FRENO + 0.8; tb < ARREGLO; tb += 2 / 3) {
    A.poner(tb, 0.08, pitido(1000, 0.08), { g: -20 });
    A.poner(tb + 0.13, 0.08, pitido(1000, 0.08), { g: -20 });
  }
  // tension: respiracion y latidos
  A.poner(FRENO + 0.5, SIGUE - FRENO - 0.5, respiracion(1.3, 2023, 1), { g: -20 });
  for (let tl = FRENO + 0.9, p = 0.75; tl < ARREGLO + 0.4; tl += p) A.poner(tl, 0.5, latido, { g: -12 });

  // los tres golpes al panel: chapa, chispazo y zumbido
  GOLPES.forEach((t0, k) => {
    A.poner(t0, 1.0, golpeMetal(380 + k * 40, { caida: 0.35, ruido: 0.9, semilla: 2030 + k }), { g: -7, pan: 0.05, sala: 0.6 });
    A.poner(t0, 0.5, bum(140, 70, 0.12, { click: 0.6, semilla: 2040 + k }), { g: -10 });
    A.poner(t0 + 0.02, 0.45, chispazoSonido(2050 + k, 0.45), { g: -10 + k, sala: 0.4 });
  });
  // vuelve todo: rele, tubos de la cabina, motor y un ding de alivio
  A.poner(ARREGLO, 0.05, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 150), { g: -9, pan: -0.3 });
  A.poner(ARREGLO, 0.6, chispazoSonido(2060, 0.6), { g: -14 });
  A.poner(ARREGLO + 0.4, 2.6, campana(midi(84)), { g: -11, sala: 0.5 });
  A.poner(ARREGLO + 1.2, CAE + 0.5 - ARREGLO - 1.2, motor(ARREGLO + 1.2, CAE, 46, 2061), { g: -10 });
  musica(A, SIGUE + 0.3, A_SIN(CAE), 18.0, (ta) => 0.93 + 0.06 * Math.sin(ta * 2.1) + (ta > 19.4 ? -0.35 * suave(19.4, CAE, ta) : 0), { g: -7, pan: 0.2, sala: 0.45 });
  // las plantas pasando
  for (let tp = SIGUE + 1.2; tp < CAE; tp += 4 / 3) {
    const bp = new D.Biquad('bp', 380, 1.4);
    const r5 = D.azar(Math.round(tp * 100));
    A.poner(tp - 0.25, 0.6, (t) => bp.paso(r5() * 2 - 1) * Math.sin(Math.PI * t / 0.6) * 1.2, { g: -18, pan: (r5() - 0.5) * 0.4 });
  }
  // vuelve a fallar: zzt y el cable crujiendo
  for (const t0 of [19.5, 19.8, 20.1]) A.poner(t0, 0.18, chispazoSonido(Math.round(t0 * 100), 0.18), { g: -15, sala: 0.3 });
  [[18.6, 1.4, 120, 80, 1], [19.6, 0.9, 170, 120, 2]].forEach(([t0, d, f0, f1, s]) => A.poner(t0, d, crujido(d, f0, f1, 2070 + s), { g: -7, pan: s % 2 ? 0.3 : -0.3, sala: 0.6 }));

  // SE ESCURRE Y CAE
  {
    let fase = 0;
    A.poner(CAE, 1.2, (t) => {
      const f = 140 + 700 * Math.exp(-t / 0.15);
      fase += f / SR;
      return Math.tanh((D.sierra(fase % 1, f / SR) * 0.5 + Math.sin(TAU * fase * 2.01) * 0.3) * 2) * Math.exp(-t / 0.4);
    }, { g: -8, sala: 0.6, grande: 0.4 });
    const vel = (ta) => Math.max(0, 9.8 * (ta - CAE));
    const v = viento(2080, (ta) => 220 + vel(ta) * 14);
    A.poner(CAE + 0.1, FRENA - CAE + 0.2, (t, ta) => { const [l, r] = v(t, ta); const k = Math.min(1, 0.2 + vel(ta) / 12) * (1 - suave(FRENA, FRENA + 0.2, ta)); return [l * k, r * k]; }, { g: -5 });
    const c = chirrido(2081);
    A.poner(CAE + 0.25, FRENA - CAE + 0.6, (t) => c(t) * suave(0, 0.4, t), { g: -8, pan: -0.15, sala: 0.4 });
    const r13 = D.azar(2082);
    const hp = new D.Biquad('hp', 4500, 0.7);
    A.poner(CAE + 0.2, FRENA - CAE + 0.6, (t) => hp.paso(r13() < 200 / SR ? (r13() * 2 - 1) * 3 : 0), { g: -13, pan: 0.2 });
    A.poner(CAE + 0.05, 2.0, braaam([26, 33, 38], 2.0, 2083), { g: -6, grande: 0.5 });
  }
  // FRENO DE EMERGENCIA
  A.poner(FRENA, 3.0, golpeMetal(85, { caida: 1.5, ruido: 1.6, semilla: 2090 }), { g: -1, sala: 0.7, grande: 0.3 });
  A.poner(FRENA, 2.0, bum(110, 32, 0.9, { click: 1.0, semilla: 2091 }), { g: 0, grande: 0.3 });
  A.poner(FRENA + 0.5, 2.2, alarma(1240), { g: -12, pan: 0.35, sala: 0.5 });
  for (let tl = FRENA + 0.4, p = 0.55; tl < ABRE + 1.0; tl += p, p = Math.min(0.75, p * 1.03)) A.poner(tl, 0.5, latido, { g: -10 });
  A.poner(FRENA + 0.3, ABRE + 1.5 - FRENA, respiracion(1.0, 2092, 1.2), { g: -18 });
  [[22.3, 1.4, 100, 70, 3], [23.3, 0.8, 190, 150, 4]].forEach(([t0, d, f0, f1, s]) => A.poner(t0, d, crujido(d, f0, f1, 2093 + s), { g: -7, sala: 0.6 }));
  for (let tb = FRENA + 0.8; tb < ABRE; tb += 2 / 3) A.poner(tb, 0.08, pitido(1000, 0.08), { g: -21 });

  // LAS PUERTAS SE ABREN A MEDIAS: el sector B
  puertasAbren(A, ABRE, 1.6, 2100, -7);
  A.poner(ABRE + 0.3, FIN - ABRE, tubos((ta) => (0.4 + 0.6 * (Math.sin(ta * 9.0) > -0.2 ? 1 : 0)) * suave(ABRE, ABRE + 1.2, ta), 2101), { g: -14, sala: 0.5 });
  {
    const r = D.azar(2102);
    for (let tg = ABRE + 0.6; tg < FIN; tg += 0.35 + r() * 0.6) A.poner(tg, 0.4, gota(Math.round(tg * 1000)), { g: -16 - r() * 6, pan: (r() - 0.5) * 1.4, sala: 0.8, grande: 0.3 });
  }
  // un gemido lejano en el sector B
  {
    const r = D.azar(2103);
    const bp1 = new D.Biquad('bp', 280, 8);
    const lp = new D.Biquad('lp', 900, 0.7);
    A.poner(ABRE + 1.6, 2.2, (t) => { const f = 250 - 50 * t; if ((Math.round(t * SR)) % 64 === 0) bp1.ajustar(f, 8); return lp.paso(bp1.paso(r() * 2 - 1) * 3) * Math.sin(Math.PI * t / 2.2); }, { g: -16, pan: -0.4, sala: 0.9, grande: 0.5 });
  }
  // pasos hacia la abertura (moqueta mojada)
  for (let tp = ANDA + 0.2, k = 0; tp < FIN - 0.1; tp += 0.55, k++) A.poner(tp, 0.35, pasoMojado(2110 + k), { g: -11, pan: k % 2 ? 0.15 : -0.15, sala: 0.3 });
  cintaCorta(A, FIN, 2120);
  titulo(A, 28.9, [33, 40, 45, 48], 2130);
  return mezclar(A);
}

// la musica tras el arreglo suena hasta que se escurre
function A_SIN (cae) { return cae - 0.1; }

/* ---------------------------------------------- fase 3: algo en el hueco */

function fase3 () {
  const DUR = 30.9;
  const A = mesa(DUR);
  const APAGON = 8.6;
  const NOCHE = 9.8;
  const GARRAS = 11.5;
  const PORTAZO = 12.9;
  const GOLPES = [14.6, 15.4, 16.0, 17.0, 17.5, 18.1];
  const LURCH = 18.6;
  const LUZ = 19.6;
  const ABRE = 21.2;
  const CARA = 23.6;
  const SALTA = 24.6;
  const FIN = 25.4;

  camaraEnciende(A, 3001);
  // el sector B: tubos que fallan y goteras
  A.poner(0.5, 3.2, tubos((ta) => suave(0.5, 1.0, ta) * (0.5 + 0.5 * (Math.sin(ta * 11.0) > 0 ? 1 : 0)) * (1 - 0.85 * suave(2.0, 3.4, ta)), 3002), { g: -14, sala: 0.5 });
  for (const tg of [0.7, 1.25, 2.1, 2.9]) A.poner(tg, 0.4, gota(Math.round(tg * 1000)), { g: -18, pan: 0.4, sala: 0.8 });
  // algo cruza el fondo del pasillo: pisadas enormes y un resuello, de izquierda a derecha
  [1.45, 1.95, 2.45].forEach((tp, k) => A.poner(tp, 0.9, pisadaGrande(3010 + k), { g: -10 + k, pan: -0.6 + k * 0.6, sala: 0.6, grande: 0.3 }));
  A.poner(1.4, 1.6, resuello(1.6, 3013), { g: -16, pan: 0, sala: 0.6 });
  A.poner(0.9, 0.09, pitido(1320, 0.09), { g: -16 });
  A.poner(1.05, 0.12, pitido(990, 0.12), { g: -16 });
  puertasCierran(A, 2.0, 3.4, 3020);

  // baja despacio; el cable cruje
  A.poner(3.6, APAGON + 0.4 - 3.6, motor(3.6, APAGON, 38, 3030), { g: -10 });
  [[4.8, 1.4, 90, 70, 1], [6.3, 1.8, 120, 85, 2], [7.6, 1.1, 160, 130, 3]].forEach(([t0, d, f0, f1, s]) => A.poner(t0, d, crujido(d, f0, f1, 3030 + s), { g: -9, pan: s % 2 ? 0.4 : -0.4, sala: 0.6 }));
  for (const t0 of [7.95, 8.2, 8.45]) A.poner(t0, 0.18, chispazoSonido(Math.round(t0 * 100), 0.18), { g: -15, sala: 0.3 });
  // APAGON: todo se apaga con un quejido electrico
  { let f = 0; A.poner(APAGON, 1.4, (t) => { f += (420 * Math.exp(-t * 2.2) + 30) / SR; return Math.sin(TAU * f) * Math.exp(-t * 1.6) * 0.5; }, { g: -9, grande: 0.3 }); }
  A.poner(APAGON, 0.05, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 120), { g: -7 });
  // a oscuras: latidos y respiracion que se acelera
  for (let tl = APAGON + 0.5, p = 0.85; tl < LUZ; tl += p, p = Math.max(0.5, p * 0.97)) A.poner(tl, 0.5, latido, { g: -11 });
  A.poner(APAGON + 0.3, LUZ - APAGON, respiracion(1.15, 3040, 1.1), { g: -18 });
  // la videocamara cambia a vision nocturna: pitido y el silbido del infrarrojo
  A.poner(NOCHE, 0.07, pitido(1800, 0.07), { g: -15 });
  A.poner(NOCHE + 0.12, 0.07, pitido(2400, 0.07), { g: -15 });
  A.poner(NOCHE, LUZ - NOCHE, (t) => Math.sin(TAU * 15700 * t) * 0.05 * suave(0, 0.3, t), { g: -24 });
  // algo se arrastra por el hueco
  A.poner(10.3, 1.4, crujido(1.4, 2200, 1700, 3050, { q: 40, densidad: 0.8 }), { g: -18, pan: -0.3, sala: 0.8 });
  // GARRAS en las puertas: chirrido de metal y un resuello pegado
  A.poner(GARRAS, PORTAZO - GARRAS, crujido(PORTAZO - GARRAS, 2600, 3300, 3060, { q: 50, densidad: 1.4 }), { g: -10, sala: 0.5 });
  A.poner(GARRAS + 0.3, PORTAZO - GARRAS, crujido(PORTAZO - GARRAS, 140, 110, 3061, { q: 12, densidad: 1.8 }), { g: -9, sala: 0.4 });
  A.poner(GARRAS + 0.2, PORTAZO - GARRAS, resuello(PORTAZO - GARRAS, 3062), { g: -9, sala: 0.3 });
  A.poner(PORTAZO, 2.0, golpeMetal(130, { caida: 0.8, ruido: 1.4, semilla: 3063 }), { g: -2, sala: 0.6, grande: 0.3 });
  A.poner(PORTAZO, 1.2, bum(120, 45, 0.4, { click: 0.8, semilla: 3064 }), { g: -3 });
  // golpes en el techo
  GOLPES.forEach((t0, k) => {
    A.poner(t0, 1.6, bum(95 - k * 3, 36, 0.5, { click: 0.7, semilla: 3070 + k }), { g: -3 + k * 0.4, grande: 0.25 });
    A.poner(t0, 1.4, golpeMetal(110 + k * 7, { caida: 0.6, ruido: 0.8, semilla: 3080 + k }), { g: -6 + k * 0.4, sala: 0.6 });
    const r = D.azar(3090 + k);
    const hp = new D.Biquad('hp', 3000, 0.7);
    A.poner(t0 + 0.05, 1.6, (t) => hp.paso(r() < 300 * Math.exp(-t * 1.8) / SR ? r() * 2 - 1 : 0) * 2, { g: -18 });
  });
  A.poner(14.4, LURCH - 14.4, resuello(LURCH - 14.4, 3100), { g: -14, sala: 0.7 });
  // SE ESCURRE
  A.poner(LURCH, 1.2, crujido(1.2, 260, 120, 3111, { q: 20, densidad: 2 }), { g: -5, sala: 0.6 });
  A.poner(LURCH + 0.22, 1.5, bum(100, 34, 0.6, { click: 0.8, semilla: 3112 }), { g: -2 });
  A.poner(LURCH + 0.22, 2.0, golpeMetal(90, { caida: 1.0, ruido: 1.2, semilla: 3113 }), { g: -4, sala: 0.6, grande: 0.2 });
  // VUELVE LA LUZ: rele, zumbido de emergencia y alarma lenta
  A.poner(LUZ, 0.05, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 150), { g: -9, pan: -0.3 });
  A.poner(LUZ, FIN - LUZ, tubos((ta) => 0.5 + 0.5 * (Math.sin(ta * 7.0) > -0.6 ? 1 : 0), 3120), { g: -16 });
  for (const t0 of [LUZ + 0.4, LUZ + 2.4]) A.poner(t0, 1.1, alarma(980), { g: -14, pan: 0.3, sala: 0.6 });
  // LAS PUERTAS SE ABREN SOLAS
  puertasAbren(A, ABRE, 2.0, 3130, -9);
  // silencio... un tono que no deberia estar ahi
  { let f = 0; A.poner(ABRE + 1.6, SALTA - ABRE - 1.6, (t) => { f += (2100 + 900 * t / 3) / SR; return Math.sin(TAU * f) * suave(0, 2.0, t) * 0.08; }, { g: -18, grande: 0.4 }); }
  for (let tl = ABRE + 0.4, p = 0.6; tl < SALTA; tl += p, p = Math.max(0.36, p * 0.93)) A.poner(tl, 0.5, latido, { g: -9 });
  // SUSTO: grito de metal y ruido, y la cinta se rompe
  A.poner(SALTA, 2.0, braaam([25, 32, 37, 44, 49], 2.0, 3140), { g: 0, grande: 0.5 });
  A.poner(SALTA, 1.2, bum(130, 40, 0.5, { click: 1.0, semilla: 3141 }), { g: 1 });
  {
    const r = D.azar(3142);
    const bp1 = new D.Biquad('bp', 1100, 6);
    const bp2 = new D.Biquad('bp', 2700, 7);
    A.poner(SALTA, 0.9, (t) => {
      const f = 1100 + 900 * Math.sin(t * 23);
      if ((Math.round(t * SR)) % 64 === 0) { bp1.ajustar(f, 6); bp2.ajustar(f * 2.4, 7); }
      const x = r() * 2 - 1;
      return Math.tanh((bp1.paso(x) * 5 + bp2.paso(x) * 3) * 1.6) * Math.exp(-t / 0.6);
    }, { g: -4, sala: 0.5 });
  }
  cintaCorta(A, FIN, 3150);
  titulo(A, 25.9, [32, 39, 44, 47], 3160);
  return mezclar(A);
}

/* ---------------------------------------------------------- salida */

if (require.main === module) (async () => {
  const cual = process.argv[2];
  const hechos = [];
  for (const [n, fn, viaje] of [[2, fase2, 28.2], [3, fase3, 25.8]]) {
    if (cual && String(n) !== cual) continue;
    const t0 = Date.now();
    const audio = fn();
    const bytes = await D.guardarOgg(path.join(ASSETS, 'sounds', `cinematica_fase${n}.ogg`), audio, 5);
    console.log(`cinematica_fase${n}.ogg ${(audio[0].length / SR).toFixed(1)} s, ${(bytes / 1024).toFixed(0)} KB (${((Date.now() - t0) / 1000).toFixed(1)} s)`);
    await D.guardarOgg(path.join(ASSETS, 'sounds', `cinematica_fase${n}_despues.ogg`), D.desde(audio, viaje), 5);
    hechos.push(n);
  }
  // sounds.json: solo se anaden estas entradas, lo demas se respeta
  const f = path.join(ASSETS, 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const n of hechos) {
    j[`cinematica.fase${n}`] = { sounds: [{ name: `backrooms_evento:cinematica_fase${n}` }] };
    j[`cinematica.fase${n}.despues`] = { sounds: [{ name: `backrooms_evento:cinematica_fase${n}_despues` }] };
  }
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
})();
