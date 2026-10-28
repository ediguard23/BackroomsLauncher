'use strict';
/**
 * Lo comun para mezclar grabaciones de verdad (las bandas de las cinematicas del final): sonidos CC0 de
 * BigSoundBank (https://bigsoundbank.com, Joseph Sardin), que no se suben al repo y se bajan solos a
 * tools/sonidos/reales/ desde https://bigsoundbank.com/UPLOAD/mp3/<numero>.mp3, y la musica del ascensor
 * («Local Forecast - Elevator», Kevin MacLeod, incompetech.com, CC BY 4.0: ver CREDITOS.md).
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const D = require('./dsp');

const { SR } = D;
const REALES = path.join(__dirname, 'reales');
const VOCES = path.join(__dirname, 'voces');
const SONIDOS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
/** Lo que no es de BigSoundBank: nombre del archivo -> URL. */
const OTROS = {
  incompetech_local_forecast_elevator: 'https://incompetech.com/music/royalty-free/mp3-royaltyfree/Local%20Forecast%20-%20Elevator.mp3'
};

function ffmpeg () {
  if (process.env.FFMPEG) return process.env.FFMPEG;
  const winget = path.join(process.env.LOCALAPPDATA || '', 'Microsoft', 'WinGet', 'Packages');
  if (fs.existsSync(winget)) {
    for (const d of fs.readdirSync(winget).filter((n) => n.startsWith('Gyan.FFmpeg'))) {
      const base = path.join(winget, d);
      for (const v of fs.readdirSync(base)) {
        const f = path.join(base, v, 'bin', 'ffmpeg.exe');
        if (fs.existsSync(f)) return f;
      }
    }
  }
  return 'ffmpeg';
}

/** Baja los que falten. */
async function descargar (ids) {
  fs.mkdirSync(REALES, { recursive: true });
  for (const id of ids) {
    const f = path.join(REALES, `${id}.mp3`);
    if (fs.existsSync(f)) continue;
    const url = OTROS[id] || `https://bigsoundbank.com/UPLOAD/mp3/${id}.mp3`;
    process.stdout.write(`  descargando ${id}... `);
    const r = await fetch(url);
    if (!r.ok) throw new Error(`no se pudo bajar ${id}: ${r.status}`);
    fs.writeFileSync(f, Buffer.from(await r.arrayBuffer()));
    console.log('ok');
  }
}

const cache = new Map();
/** Un archivo (mp3, wav u ogg) a mono 44.1 kHz. */
function leer (archivo) {
  if (!cache.has(archivo)) {
    const buf = execFileSync(ffmpeg(), ['-v', 'error', '-i', archivo, '-af', 'aresample=44100', '-ac', '1', '-f', 'f32le', '-'], { maxBuffer: 1 << 29 });
    cache.set(archivo, new Float32Array(buf.buffer, buf.byteOffset, buf.length / 4).slice());
  }
  return cache.get(archivo);
}

/** Cambia el tono (y la duracion): 0.8 = mas grave y lento. */
function tonoDe (s, tono) {
  if (tono === 1) return s;
  const n = Math.floor(s.length / tono);
  const r = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    const x = i * tono;
    const j = Math.floor(x);
    const f = x - j;
    r[i] = (s[j] || 0) * (1 - f) + (s[j + 1] || 0) * f;
  }
  return r;
}

/** Normaliza (pico 1) y pone fundidos de entrada y salida (segundos). */
function normal (s, entra = 0.005, sale = 0.05) {
  let pico = 1e-9;
  for (const v of s) pico = Math.max(pico, Math.abs(v));
  const fe = Math.floor(entra * SR);
  const fs_ = Math.floor(sale * SR);
  const out = new Float32Array(s.length);
  for (let i = 0; i < s.length; i++) {
    let g = 1 / pico;
    if (i < fe) g *= i / fe;
    if (i > s.length - fs_) g *= (s.length - i) / fs_;
    out[i] = s[i] * g;
  }
  return out;
}

/** Un trozo (desde..hasta segundos del original) de una grabacion de reales/, normalizado. */
function real (id, desde = 0, hasta = Infinity, { tono = 1, entra = 0.005, sale = 0.05 } = {}) {
  const s = leer(path.join(REALES, `${id}.mp3`));
  const i0 = Math.max(0, Math.round(desde * SR));
  const i1 = Math.min(s.length, Math.round(hasta * SR));
  return normal(tonoDe(s.slice(i0, i1), tono), entra, sale);
}

/** Una voz de voces/ (o, si no esta, un sonido del juego de repuesto), normalizada. */
function voz (nombre, repuesto, { tono = 1, desde = 0, hasta = Infinity } = {}) {
  const f = path.join(VOCES, nombre);
  const propia = fs.existsSync(f);
  const s = propia ? leer(f) : leer(path.join(SONIDOS, repuesto));
  return { s: normal(tonoDe(s.slice(Math.round(desde * SR), Math.min(s.length, Math.round(hasta * SR))), tono), 0.01, 0.1), propia };
}

/** Un sonido ya generado del juego (sounds/<nombre>.ogg), normalizado. */
function delJuego (nombre, tono = 1) {
  return normal(tonoDe(leer(path.join(SONIDOS, `${nombre}.ogg`)), tono));
}

function filtro (mono, tipo, f, q = 0.7, gan = 0) {
  const b = new D.Biquad(tipo, f, q, gan);
  return mono.map((v) => b.paso(v));
}

/** Multiplica por una envolvente de puntos [[segundo, ganancia], ...] (relativa al principio del sonido). */
function envolvente (mono, puntos) {
  const out = new Float32Array(mono.length);
  let k = 1;
  for (let i = 0; i < mono.length; i++) {
    const t = i / SR;
    while (k < puntos.length - 1 && t >= puntos[k][0]) k++;
    const [t0, a] = puntos[k - 1];
    const [t1, b] = puntos[k];
    const g = t <= t0 ? a : t >= t1 ? b : a + (b - a) * (t - t0) / (t1 - t0);
    out[i] = mono[i] * g;
  }
  return out;
}

/** Un buffer estereo; `poner(t, mono, gan, pan, en)`: en = donde esta el golpe dentro del trozo. */
function pista (segundos) {
  const n = Math.ceil(segundos * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  const poner = (t0, mono, gan = 1, pan = 0, en = 0) => {
    const i0 = Math.round((t0 - en) * SR);
    for (let j = 0; j < mono.length && i0 + j < n; j++) {
      if (i0 + j < 0) continue;
      L[i0 + j] += mono[j] * gan * (1 - pan) * 0.7;
      R[i0 + j] += mono[j] * gan * (1 + pan) * 0.7;
    }
  };
  return { L, R, n, poner };
}

/** Pasa una pista por una sala (Freeverb) y un filtro de paso bajo: lo seco por `seco` y lo humedo por `humedo`. */
function sala (p, { lp = 20000, sala: tam = 0.6, amortiguacion = 0.5, predelayMs = 10, seco = 1, humedo = 0.3 } = {}) {
  const v = new D.Freeverb({ sala: tam, amortiguacion, predelayMs });
  const fl = new D.Biquad('lp', lp, 0.7);
  const fr = new D.Biquad('lp', lp, 0.7);
  const L = new Float32Array(p.n);
  const R = new Float32Array(p.n);
  for (let i = 0; i < p.n; i++) {
    const l = fl.paso(p.L[i]);
    const r = fr.paso(p.R[i]);
    const [a, b] = v.paso((l + r) * 0.5);
    L[i] = l * seco + a * humedo;
    R[i] = r * seco + b * humedo;
  }
  return { L, R, n: p.n };
}

/** Suma pistas estereo. */
function sumar (n, ...pistas) {
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  for (const p of pistas) {
    for (let i = 0; i < n && i < p.n; i++) {
      L[i] += p.L[i];
      R[i] += p.R[i];
    }
  }
  return [L, R];
}

/** Guarda un sonido mono (los del mundo: Minecraft solo situa en 3D los mono). */
async function guardarMono (archivo, mono, calidad = 4) {
  const { createOggEncoder } = require('wasm-media-encoders');
  const enc = await createOggEncoder();
  enc.configure({ sampleRate: SR, channels: 1, vbrQuality: calidad });
  const trozos = [];
  for (let i = 0; i < mono.length; i += 4096) trozos.push(Buffer.from(enc.encode([mono.subarray(i, i + 4096)])));
  trozos.push(Buffer.from(enc.finalize()));
  fs.writeFileSync(archivo, Buffer.concat(trozos));
  return fs.statSync(archivo).size;
}

/** Pone o cambia entradas de sounds.json (respetando sus saltos de linea). */
function soundsJson (entradas) {
  const fj = path.join(SONIDOS, '..', 'sounds.json');
  const texto = fs.readFileSync(fj, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  Object.assign(j, entradas);
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(fj, out);
}

module.exports = { SR, SONIDOS, descargar, leer, real, voz, delJuego, filtro, envolvente, pista, sala, sumar, guardarMono, soundsJson, tonoDe, normal };
