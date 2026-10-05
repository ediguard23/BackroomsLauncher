#!/usr/bin/env node
'use strict';
/**
 * Sonidos de las acciones del jugador, para que ninguno sea el generico de
 * Minecraft:
 *
 *   node tools/sonidos/acciones.js [prefijo]
 *
 *  jugador_herido1-3   golpe, roce del traje y una boqueada a traves del
 *                      respirador (la valvula hace clic): sustituye al "auch"
 *  jugador_muerte      boqueada ahogada en la mascara, valvula que traquetea,
 *                      el cuerpo contra la moqueta y el ultimo aire que se escapa
 *  casete_coger        la carcasa de plastico, las bobinas sueltas y el clic de la tapa
 *  agua_coger          cristal y el agua de almendras moviendose dentro
 *  comida_coger        el envoltorio arrugandose
 *  agua_trago1-3       tragos (el juego los repite mientras bebes)
 *  agua_suspiro        el tapon y el suspiro de alivio por la mascara, al acabar
 *  mision_completa     clic de la cinta, dos pitidos de videocamara y un acorde
 *                      en Si (el mismo tono que el zumbido de los tubos)
 *  ascensor_denegado   zumbador de acceso denegado por un altavoz pequeno
 *  ascensor_panel      boton metalico, rele y la campanilla de dos tonos
 *  escapado            para todos: radio que se abre y un acorde que sube y se queda
 *  nota_leer           papel arrugado que se abre
 *  butaca              cojin que se hunde y muelles
 *  bacteria_golpe      latigazo, desgarro humedo y un crujido
 *
 * Todo es sintesis propia. Mono los que suenan en un sitio del mundo; estereo
 * los avisos (mision, escapado).
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };

const mono = (s) => new Float32Array(Math.round(s * SR));
function poner (buf, t0, dur, fn) {
  const i0 = Math.round(t0 * SR);
  const n = Math.min(buf.length - i0, Math.round(dur * SR));
  for (let i = 0; i < n; i++) buf[i0 + i] += fn(i / SR);
}

function reverb (buf, sala, mezcla, opciones = {}) {
  const rv = new D.Freeverb({ sala, amortiguacion: 0.45, ...opciones });
  const out = new Float32Array(buf.length);
  for (let i = 0; i < buf.length; i++) out[i] = buf[i] + rv.paso(buf[i])[0] * mezcla;
  return out;
}

function pico (buf, picoDb = -1) {
  let m = 0;
  for (const v of buf) m = Math.max(m, Math.abs(v));
  const g = Math.pow(10, picoDb / 20) / (m || 1);
  for (let i = 0; i < buf.length; i++) buf[i] = Math.tanh(buf[i] * g * 1.05) / Math.tanh(1.05);
  const fe = Math.floor(0.0015 * SR); const fs = Math.floor(0.02 * SR);
  for (let i = 0; i < fe; i++) buf[i] *= i / fe;
  for (let i = 0; i < fs; i++) buf[buf.length - 1 - i] *= i / fs;
  return buf;
}

/** Ruido filtrado en banda, con su propio azar. */
function ruido (f, q, semilla, tipo = 'bp') {
  const rnd = D.azar(semilla);
  const bq = new D.Biquad(tipo, f, q);
  return () => bq.paso(rnd() * 2 - 1);
}

/** Golpe sordo con caida de tono. */
function golpe (f0, f1, caida, semilla, ruidoGolpe = 0.4) {
  const rnd = D.azar(semilla);
  const lp = new D.Biquad('lp', 1500, 0.7);
  let fase = 0;
  return (t) => {
    fase += (f1 + (f0 - f1) * Math.exp(-t * 35)) / SR;
    return (Math.sin(TAU * fase) + lp.paso(rnd() * 2 - 1) * ruidoGolpe * Math.exp(-t * 70)) * Math.exp(-t / caida);
  };
}

/** Clic con una resonancia (plastico, metal). */
function clic (f, caida, semilla, q = 8) {
  const n = ruido(f, q, semilla);
  return (t) => n() * Math.exp(-t / caida) * 3;
}

function campana (f, caida) {
  return (t) => [[1, 1], [2, 0.4], [3.01, 0.14], [4.2, 0.07]]
    .reduce((s, [k, a]) => s + Math.sin(TAU * f * k * t) * a * Math.exp(-t * k / caida), 0) * 0.5;
}

/**
 * Aire a traves del respirador del traje: ruido con dos formantes (la boca y
 * el filtro de la mascara) y un silbido estrecho del filtro. `env` da la forma.
 */
function respirador (dur, env, { semilla = 1, boca = 900, silbido = 2700, aspereza = 0.5 } = {}) {
  const rnd = D.azar(semilla);
  const f1 = new D.Biquad('bp', boca, 3);
  const f2 = new D.Biquad('bp', boca * 2.2, 4);
  const sil = new D.Biquad('bp', silbido, 18);
  const hp = new D.Biquad('hp', 250, 0.7);
  const out = mono(dur);
  let ronco = 0;
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const n = hp.paso(rnd() * 2 - 1);
    ronco += ((rnd() < 0.02 ? 1 : 0) - ronco) * 0.05; // garganta que raspa
    const v = f1.paso(n) * 1.2 + f2.paso(n) * 0.7 + sil.paso(n) * 0.5 + n * ronco * aspereza * 0.3;
    out[i] = v * env(t);
  }
  return out;
}

/* ================================================================ jugador */

function herido (k) {
  const dur = 0.62;
  const buf = mono(dur);
  // el golpe en el cuerpo y el roce de la tela del traje
  poner(buf, 0, 0.3, golpe(120 + k * 15, 55, 0.07, 10 + k, 0.8));
  const tela = ruido(1800, 0.8, 20 + k);
  poner(buf, 0, 0.18, (t) => tela() * Math.exp(-t * 18) * 0.6);
  // la boqueada: aire que entra de golpe por el filtro
  const aire = respirador(0.45, (t) => suave(0.0, 0.04, t) * Math.exp(-Math.max(0, t - 0.06) * 9), { semilla: 30 + k, boca: 760 + k * 90 });
  poner(buf, 0.04, 0.45, (t) => aire[Math.min(aire.length - 1, Math.floor(t * SR))] * 1.1);
  // la valvula de exhalacion que hace clic al cerrarse
  poner(buf, 0.33 + k * 0.03, 0.04, clic(3400, 0.004, 40 + k));
  return pico(reverb(buf, 0.3, 0.12), -1.5);
}

function muerte () {
  const dur = 2.9;
  const buf = mono(dur);
  // boqueada que se ahoga: entra aire, la garganta se cierra a tirones
  const ahogo = respirador(1.0, (t) => suave(0, 0.06, t) * (0.5 + 0.5 * Math.abs(Math.sin(TAU * 7 * t))) * (1 - suave(0.7, 1.0, t)),
    { semilla: 51, boca: 680, aspereza: 1.2 });
  poner(buf, 0, 1.0, (t) => ahogo[Math.min(ahogo.length - 1, Math.floor(t * SR))] * 1.2);
  // la valvula traqueteando
  for (let i = 0; i < 9; i++) poner(buf, 0.15 + i * 0.075, 0.03, clic(3000 + (i % 3) * 400, 0.003, 60 + i));
  // el cuerpo contra la moqueta (dos golpes: rodillas y espalda)
  poner(buf, 1.05, 0.5, golpe(95, 42, 0.12, 70, 0.9));
  poner(buf, 1.32, 0.6, golpe(80, 36, 0.18, 71, 1.0));
  const tela = ruido(1500, 0.7, 72);
  poner(buf, 1.05, 0.5, (t) => tela() * Math.exp(-t * 8) * 0.4);
  // el ultimo aire que se escapa por la mascara, muy largo y cada vez mas debil
  const ultimo = respirador(1.4, (t) => suave(0, 0.2, t) * Math.exp(-t * 2.2) * 0.8, { semilla: 73, boca: 520, silbido: 2200, aspereza: 0.2 });
  poner(buf, 1.45, 1.4, (t) => ultimo[Math.min(ultimo.length - 1, Math.floor(t * SR))]);
  return pico(reverb(buf, 0.45, 0.18), -1);
}

/* ================================================================ objetos */

function caseteCoger () {
  const buf = mono(0.5);
  poner(buf, 0, 0.06, clic(2200, 0.008, 80, 5));       // carcasa contra la mano
  poner(buf, 0.01, 0.1, golpe(260, 160, 0.03, 81, 0.2));
  // bobinas sueltas que traquetean dentro
  for (let i = 0; i < 6; i++) poner(buf, 0.05 + i * 0.022 + (i % 2) * 0.006, 0.03, clic(4200 - i * 150, 0.004, 82 + i, 10));
  poner(buf, 0.24, 0.05, clic(1700, 0.006, 90, 6));     // la tapa de la funda
  poner(buf, 0.25, 0.08, golpe(320, 200, 0.02, 91, 0.1));
  return pico(reverb(buf, 0.25, 0.1), -3);
}

function aguaCoger () {
  const buf = mono(0.9);
  // cristal: un "tin" corto con dos parciales inarmonicos
  poner(buf, 0, 0.5, (t) => (Math.sin(TAU * 2350 * t) * 0.6 + Math.sin(TAU * 3910 * t) * 0.3) * Math.exp(-t * 14) * 0.5);
  poner(buf, 0, 0.04, clic(5000, 0.003, 100));
  // el agua dentro: burbujas que suben (senos con barrido rapido)
  const rnd = D.azar(101);
  for (let i = 0; i < 7; i++) {
    const t0 = 0.08 + rnd() * 0.5;
    const f0 = 500 + rnd() * 700;
    poner(buf, t0, 0.08, (t) => Math.sin(TAU * (f0 * t + 2500 * t * t)) * Math.exp(-t * 45) * 0.35);
  }
  const ola = ruido(700, 1.5, 102);
  poner(buf, 0.05, 0.6, (t) => ola() * Math.sin(Math.PI * t / 0.6) * 0.25);
  return pico(reverb(buf, 0.25, 0.1), -3);
}

function comidaCoger () {
  const buf = mono(0.55);
  const rnd = D.azar(110);
  const hp = new D.Biquad('hp', 2500, 0.7);
  const bp = new D.Biquad('bp', 4500, 1.2);
  // crujidos de envoltorio: rafagas cortisimas al azar, mas densas al principio
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const densidad = 0.06 * Math.exp(-t * 4);
    const chispa = rnd() < densidad ? (rnd() * 2 - 1) * 2.5 : 0;
    buf[i] = bp.paso(hp.paso(chispa)) * suave(0, 0.01, t);
  }
  poner(buf, 0, 0.15, golpe(200, 120, 0.03, 111, 0.3));
  return pico(reverb(buf, 0.2, 0.08), -4);
}

/** Un trago (el juego lo repite cada 0,2 s mientras se bebe): garganta y un poco de agua. */
function aguaTrago (k) {
  const buf = mono(0.26);
  poner(buf, 0.03, 0.2, golpe(165 - k * 12, 88, 0.045, 121 + k, 0.12));
  const agua = ruido(1000 + k * 150, 2, 125 + k);
  poner(buf, 0, 0.14, (t) => agua() * Math.sin(Math.PI * t / 0.14) * 0.3);
  return pico(buf, -6);
}

/** Al terminar: el tapon y un suspiro de alivio a traves de la mascara. */
function aguaSuspiro () {
  const buf = mono(1.0);
  poner(buf, 0, 0.06, clic(1300, 0.01, 120, 4));
  const suspiro = respirador(0.8, (t) => Math.sin(Math.PI * Math.min(1, t / 0.8)) * 0.5, { semilla: 130, boca: 600, aspereza: 0 });
  poner(buf, 0.12, 0.8, (t) => suspiro[Math.min(suspiro.length - 1, Math.floor(t * SR))]);
  return pico(reverb(buf, 0.3, 0.1), -4);
}

function notaLeer () {
  const buf = mono(0.75);
  const rnd = D.azar(140);
  const bp = new D.Biquad('bp', 3200, 0.9);
  const lp = new D.Biquad('lp', 6000, 0.7);
  // papel arrugado que se despliega: crujidos en dos golpes
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const golpe1 = Math.exp(-Math.pow((t - 0.12) / 0.08, 2));
    const golpe2 = Math.exp(-Math.pow((t - 0.42) / 0.12, 2));
    const densidad = 0.12 * (golpe1 + golpe2 * 0.8);
    const chispa = rnd() < densidad ? (rnd() * 2 - 1) * 2 : 0;
    buf[i] = lp.paso(bp.paso(chispa) + (rnd() * 2 - 1) * 0.03 * (golpe1 + golpe2));
  }
  return pico(reverb(buf, 0.2, 0.08), -5);
}

function butaca () {
  const buf = mono(0.8);
  poner(buf, 0, 0.35, golpe(110, 60, 0.09, 150, 0.6));  // el cuerpo en el cojin
  const tela = ruido(900, 0.8, 151);
  poner(buf, 0, 0.25, (t) => tela() * Math.exp(-t * 12) * 0.5);
  // muelles: resonancias metalicas largas y desafinadas
  for (const [f, a] of [[410, 0.3], [627, 0.2], [893, 0.12]]) {
    poner(buf, 0.03, 0.7, (t) => Math.sin(TAU * f * t + 0.6 * Math.sin(TAU * 7 * t)) * a * Math.exp(-t * 6));
  }
  poner(buf, 0.18, 0.2, (t) => Math.sin(TAU * (240 + 80 * t) * t) * 0.15 * Math.sin(Math.PI * t / 0.2)); // cruje la madera
  return pico(reverb(buf, 0.35, 0.12), -4);
}

function bacteriaGolpe () {
  const buf = mono(0.7);
  const rnd = D.azar(160);
  // latigazo: ruido que barre de grave a agudo muy rapido
  const bp = new D.Biquad('bp', 400, 2);
  for (let i = 0; i < Math.round(0.18 * SR); i++) {
    const t = i / SR;
    bp.ajustar(400 + 5000 * (t / 0.18), 2);
    buf[i] += bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.18) * 1.3;
  }
  // desgarro humedo y el golpe
  const hum = ruido(1400, 1.2, 161);
  poner(buf, 0.15, 0.3, (t) => hum() * Math.exp(-t * 14) * (0.6 + 0.4 * Math.sin(TAU * 90 * t)));
  poner(buf, 0.15, 0.45, golpe(130, 48, 0.1, 162, 1.1));
  poner(buf, 0.17, 0.05, clic(2600, 0.006, 163, 4));    // crujido (hueso, o el casco)
  poner(buf, 0.2, 0.05, clic(1900, 0.008, 164, 4));
  return pico(reverb(buf, 0.4, 0.15), -0.5);
}

/* ================================================================== avisos */

const SI = 123.47; // Si2: el acorde va en el tono del zumbido de los tubos

function misionCompleta () {
  const dur = 3.4;
  const l = mono(dur);
  const r = mono(dur);
  const ambos = (t0, d, fn, pan = 0) => {
    const i0 = Math.round(t0 * SR);
    for (let i = 0; i < Math.round(d * SR) && i0 + i < l.length; i++) {
      const v = fn(i / SR);
      l[i0 + i] += v * (1 - pan) ;
      r[i0 + i] += v * (1 + pan);
    }
  };
  // clic de la cinta y su motor
  ambos(0, 0.05, clic(1900, 0.006, 170, 5));
  const motor = ruido(300, 3, 171);
  ambos(0.02, 0.35, (t) => motor() * Math.sin(Math.PI * t / 0.35) * 0.25);
  // dos pitidos de videocamara
  ambos(0.32, 0.09, (t) => Math.sin(TAU * 2093 * t) * 0.22 * suave(0, 0.005, t) * (1 - suave(0.07, 0.09, t)));
  ambos(0.46, 0.13, (t) => Math.sin(TAU * 2637 * t) * 0.22 * suave(0, 0.005, t) * (1 - suave(0.1, 0.13, t)));
  // acorde Si-Re#-Fa#-La# (Si maj7) que sube y se queda flotando
  [1, 1.26, 1.498, 1.888, 2].forEach((k, i) => {
    const f = SI * 2 * k;
    ambos(0.55 + i * 0.05, 2.8, (t) => {
      const e = suave(0, 0.5, t) * Math.exp(-Math.max(0, t - 0.6) * 1.3);
      return (Math.sin(TAU * f * t) + 0.3 * Math.sin(TAU * f * 2 * t + 0.4)) * e * 0.07;
    }, (i - 2) * 0.25);
  });
  const est = [l, r];
  const rv = new D.Freeverb({ sala: 0.85, amortiguacion: 0.4 });
  for (let i = 0; i < l.length; i++) { const [a, b] = rv.paso((l[i] + r[i]) / 2); l[i] += a * 0.45; r[i] += b * 0.45; }
  return D.fundidos(D.masterizar(est, { rmsDb: -22, picoDb: -2 }));
}

function ascensorDenegado () {
  const buf = mono(0.75);
  const lp = new D.Biquad('lp', 2200, 0.8);
  const hp = new D.Biquad('hp', 300, 0.7);
  // zumbador: dos pulsos de onda cuadrada desafinada, como de altavoz barato
  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const on = (t < 0.26) || (t > 0.36 && t < 0.62);
    const s = Math.sign(Math.sin(TAU * 196 * t)) * 0.6 + Math.sign(Math.sin(TAU * 293 * t)) * 0.4;
    buf[i] = on ? Math.tanh(hp.paso(lp.paso(s)) * 2) * 0.6 : hp.paso(lp.paso(0));
  }
  poner(buf, 0, 0.04, clic(2500, 0.005, 180, 5));       // el boton
  return pico(reverb(buf, 0.3, 0.12), -2);
}

function ascensorPanel () {
  const buf = mono(2.2);
  poner(buf, 0, 0.05, clic(2800, 0.004, 190, 6));       // boton metalico
  poner(buf, 0.06, 0.2, golpe(140, 80, 0.03, 191, 0.4)); // rele
  poner(buf, 0.06, 0.05, clic(1500, 0.006, 192, 4));
  poner(buf, 0.3, 1.8, campana(1318.5, 1.2));           // ding
  poner(buf, 0.62, 1.6, campana(1046.5, 1.2));          // dong
  return pico(reverb(buf, 0.6, 0.25), -2);
}

function escapado () {
  const dur = 5.5;
  const l = mono(dur);
  const r = mono(dur);
  const rnd = D.azar(200);
  // la radio que se abre: chasquido y estatica que se va limpiando
  const st = new D.Biquad('bp', 2400, 0.6);
  for (let i = 0; i < Math.round(0.9 * SR); i++) {
    const t = i / SR;
    const v = st.paso(rnd() * 2 - 1) * (1 - t / 0.9) * (t < 0.04 ? 2 : 0.6);
    l[i] += v; r[i] += v * 0.9;
  }
  // acorde que sube de Si menor a Si mayor: se sale, pero algo sigue ahi abajo
  const notas = [[SI, 0], [SI * 1.189, 0], [SI * 1.498, 0.1], [SI * 2, 0.2], [SI * 2.52, 1.4], [SI * 3, 0.6]];
  notas.forEach(([f, t0], i) => {
    const i0 = Math.round((0.5 + t0) * SR);
    for (let k = 0; i0 + k < l.length; k++) {
      const t = k / SR;
      // la tercera menor se convierte en mayor a mitad
      const ff = i === 1 ? f * (1 + 0.0595 * suave(1.6, 2.4, t)) : f;
      const e = suave(0, 0.8, t) * (1 - suave(3.6, 4.9 - t0, t));
      const v = (Math.sin(TAU * ff * t) + 0.4 * Math.sin(TAU * ff * 2 * t) + 0.15 * Math.sin(TAU * ff * 3 * t)) * e * 0.07;
      l[i0 + k] += v * (i % 2 ? 0.8 : 1.1);
      r[i0 + k] += v * (i % 2 ? 1.1 : 0.8);
    }
  });
  // una campana lejana al final
  const c = campana(SI * 8, 2.2);
  for (let k = 0; k < Math.round(3 * SR) && Math.round(2.4 * SR) + k < l.length; k++) {
    const v = c(k / SR) * 0.25;
    l[Math.round(2.4 * SR) + k] += v; r[Math.round(2.4 * SR) + k] += v;
  }
  const rv = new D.Freeverb({ sala: 0.92, amortiguacion: 0.35, predelayMs: 25 });
  for (let i = 0; i < l.length; i++) { const [a, b] = rv.paso((l[i] + r[i]) / 2); l[i] += a * 0.6; r[i] += b * 0.6; }
  return D.fundidos(D.masterizar([l, r], { rmsDb: -21, picoDb: -1.5 }), 0.002, 0.6);
}

/* ===================================================================== todo */

async function guardarMono (nombre, buf) {
  const { createOggEncoder } = require('wasm-media-encoders');
  const enc = await createOggEncoder();
  enc.configure({ sampleRate: SR, channels: 1, vbrQuality: 5 });
  const trozos = [];
  for (let i = 0; i < buf.length; i += 4096) trozos.push(Buffer.from(enc.encode([buf.subarray(i, i + 4096)])));
  trozos.push(Buffer.from(enc.finalize()));
  fs.writeFileSync(path.join(SALIDA, `${nombre}.ogg`), Buffer.concat(trozos));
  console.log(`  ${nombre}.ogg  ${(buf.length / SR).toFixed(2)} s mono`);
}

async function guardarEst (nombre, est) {
  await D.guardarOgg(path.join(SALIDA, `${nombre}.ogg`), est, 5);
  console.log(`  ${nombre}.ogg  ${(est[0].length / SR).toFixed(2)} s estereo`);
}

(async () => {
  console.log('Sonidos de las acciones');
  const solo = process.argv[2];
  const toca = (n) => !solo || n.startsWith(solo);
  for (let k = 1; k <= 3; k++) if (toca('jugador_herido')) await guardarMono(`jugador_herido${k}`, herido(k));
  if (toca('jugador_muerte')) await guardarMono('jugador_muerte', muerte());
  if (toca('casete_coger')) await guardarMono('casete_coger', caseteCoger());
  if (toca('agua_coger')) await guardarMono('agua_coger', aguaCoger());
  if (toca('comida_coger')) await guardarMono('comida_coger', comidaCoger());
  for (let k = 1; k <= 3; k++) if (toca('agua_trago')) await guardarMono(`agua_trago${k}`, aguaTrago(k));
  if (toca('agua_suspiro')) await guardarMono('agua_suspiro', aguaSuspiro());
  if (toca('nota_leer')) await guardarMono('nota_leer', notaLeer());
  if (toca('butaca')) await guardarMono('butaca', butaca());
  if (toca('bacteria_golpe')) await guardarMono('bacteria_golpe', bacteriaGolpe());
  if (toca('ascensor_denegado')) await guardarMono('ascensor_denegado', ascensorDenegado());
  if (toca('ascensor_panel')) await guardarMono('ascensor_panel', ascensorPanel());
  if (toca('mision_completa')) await guardarEst('mision_completa', misionCompleta());
  if (toca('escapado')) await guardarEst('escapado', escapado());

  // los nuestros en sounds.json del mod
  const f = path.join(SALIDA, '..', 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  const s = (nombre, extra = {}) => ({ sounds: [{ name: `backrooms_evento:${nombre}`, ...extra }] });
  Object.assign(j, {
    'casete.coger': s('casete_coger', { attenuation_distance: 8 }),
    'agua.coger': s('agua_coger', { attenuation_distance: 8 }),
    'comida.coger': s('comida_coger', { attenuation_distance: 8 }),
    'agua.beber': { sounds: [1, 2, 3].map((k) => ({ name: `backrooms_evento:agua_trago${k}`, attenuation_distance: 8 })) },
    'agua.suspiro': s('agua_suspiro', { attenuation_distance: 8 }),
    'nota.leer': s('nota_leer', { attenuation_distance: 6 }),
    butaca: s('butaca', { attenuation_distance: 8 }),
    'bacteria.golpe': s('bacteria_golpe', { attenuation_distance: 24 }),
    'ascensor.denegado': s('ascensor_denegado', { attenuation_distance: 12 }),
    'ascensor.panel': s('ascensor_panel', { attenuation_distance: 16 }),
    'mision.completa': s('mision_completa'),
    escapado: s('escapado')
  });
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');

  // y los del jugador sustituyen a los de Minecraft (el "auch" y la muerte)
  const fm = path.join(SALIDA, '..', '..', 'minecraft', 'sounds.json');
  fs.mkdirSync(path.dirname(fm), { recursive: true });
  fs.writeFileSync(fm, JSON.stringify({
    'entity.player.hurt': { replace: true, sounds: [1, 2, 3].map((k) => ({ name: `backrooms_evento:jugador_herido${k}` })) },
    'entity.player.hurt_drown': { replace: true, sounds: [{ name: 'backrooms_evento:jugador_herido2' }] },
    'entity.player.hurt_on_fire': { replace: true, sounds: [{ name: 'backrooms_evento:jugador_herido3' }] },
    'entity.player.hurt_freeze': { replace: true, sounds: [{ name: 'backrooms_evento:jugador_herido1' }] },
    'entity.player.hurt_sweet_berry_bush': { replace: true, sounds: [{ name: 'backrooms_evento:jugador_herido1' }] },
    'entity.player.death': { replace: true, sounds: [{ name: 'backrooms_evento:jugador_muerte' }] }
  }, null, 2) + '\n');
  console.log('  minecraft/sounds.json (herido y muerte del jugador)');
})();
