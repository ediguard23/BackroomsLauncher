#!/usr/bin/env node
'use strict';
/**
 * Voces del evento: las frases se generaron con Higgsfield (Seed Audio, voces
 * Marisol, Juan e Ines) y estan en tools/sonidos/voces/*.wav; aqui se
 * procesan para el juego:
 *
 *   node tools/sonidos/voces.js
 *
 *  megafonia_*   campanilla "ding-dong" y la voz por un altavoz de techo viejo
 *                (sin graves ni agudos, algo saturada) rebotando en el vestibulo
 *  megafonia_alerta  la de la alarma: tonos de aviso, mas rota y muy lejana
 *  susurro_*     mas grave y lenta, con aliento por encima (ruido que sigue a
 *                la voz), muy cerca y con eco: para las alucinaciones (mono)
 *
 * Necesita ffmpeg (variable FFMPEG o el de Medal) para leer los wav.
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const ORIGEN = path.join(__dirname, 'voces');
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');

function ffmpeg () {
  if (process.env.FFMPEG) return process.env.FFMPEG;
  const medal = path.join(process.env.LOCALAPPDATA || '', 'Medal');
  for (const d of fs.readdirSync(medal).filter((n) => n.startsWith('recorder')).sort().reverse()) {
    const f = path.join(medal, d, 'ffmpeg7.exe');
    if (fs.existsSync(f)) return f;
  }
  return 'ffmpeg';
}

/** Lee un wav como mono float a 44,1 kHz; `velocidad` < 1 lo hace mas lento y grave. */
function leer (nombre, velocidad = 1) {
  const filtros = velocidad === 1 ? 'aresample=44100' : `asetrate=24000*${velocidad},aresample=44100`;
  const buf = execFileSync(ffmpeg(), ['-v', 'error', '-i', path.join(ORIGEN, `${nombre}.wav`), '-af', filtros, '-ac', '1', '-f', 'f32le', '-'],
    { maxBuffer: 1 << 28 });
  return new Float32Array(buf.buffer, buf.byteOffset, buf.length / 4).slice();
}

function campana (f, t, caida = 1.6) {
  return [[1, 1], [2, 0.35], [3.01, 0.12], [4.2, 0.06]].reduce((s, [k, a]) => s + Math.sin(TAU * f * k * t) * a * Math.exp(-t * k / caida), 0);
}

/** Altavoz de techo: banda estrecha, un poco de saturacion y resonancia de caja. */
function altavoz (x, { bajo = 320, alto = 3600, saturacion = 1.6 } = {}) {
  const hp = new D.Biquad('hp', bajo, 0.8);
  const lp = new D.Biquad('lp', alto, 0.9);
  const caja = new D.Biquad('pico', 1700, 2, 5);
  const out = new Float32Array(x.length);
  for (let i = 0; i < x.length; i++) out[i] = Math.tanh(caja.paso(lp.paso(hp.paso(x[i]))) * saturacion);
  return out;
}

function aEstereo (mono, sala, mezcla, opciones = {}) {
  const l = new Float32Array(mono.length);
  const r = new Float32Array(mono.length);
  const rv = new D.Freeverb({ sala, amortiguacion: 0.45, ...opciones });
  for (let i = 0; i < mono.length; i++) {
    const [a, b] = rv.paso(mono[i]);
    l[i] = mono[i] * 0.8 + a * mezcla;
    r[i] = mono[i] * 0.8 + b * mezcla;
  }
  return [l, r];
}

function concatenar (...trozos) {
  const n = trozos.reduce((s, t) => s + t.length, 0);
  const out = new Float32Array(n);
  let o = 0;
  for (const t of trozos) { out.set(t, o); o += t.length; }
  return out;
}

function silencio (s) {
  return new Float32Array(Math.round(s * SR));
}

/** "Ding-dong" de megafonia: Mi y Do de campana. */
function dingDong () {
  const out = silencio(1.9);
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    out[i] = campana(659.25, t) * 0.32 + (t > 0.55 ? campana(523.25, t - 0.55) * 0.32 : 0);
  }
  return out;
}

/** Dos pitidos de aviso, como los de una central de alarmas. */
function tonosAviso () {
  const out = silencio(1.3);
  for (let i = 0; i < out.length; i++) {
    const t = i / SR;
    const on = (t < 0.28) || (t > 0.42 && t < 0.7);
    out[i] = on ? Math.sign(Math.sin(TAU * 880 * t)) * 0.18 : 0;
  }
  return out;
}

function megafonia (nombre) {
  const voz = altavoz(leer(nombre));
  const todo = concatenar(silencio(0.1), altavoz(dingDong(), { saturacion: 1.1 }), voz, silencio(1.8));
  return D.fundidos(D.masterizar(aEstereo(todo, 0.92, 0.55, { predelayMs: 35 }), { rmsDb: -19, picoDb: -1 }));
}

function alerta () {
  const voz = altavoz(leer('megafonia_alerta'), { bajo: 450, alto: 2900, saturacion: 3.2 });
  const todo = concatenar(silencio(0.1), altavoz(tonosAviso(), { saturacion: 2 }), voz, silencio(2.5));
  return D.fundidos(D.masterizar(aEstereo(todo, 0.97, 1.1, { predelayMs: 70 }), { rmsDb: -20, picoDb: -1 }));
}

/** Susurro: voz lenta y grave casi sin cuerpo, con aliento que la sigue y eco cercano. */
function susurro (nombre) {
  const voz = leer(nombre, 0.86);
  const hp = new D.Biquad('hp', 260, 0.7);
  const rnd = D.azar(nombre.length * 97);
  const aliento = new D.Biquad('bp', 3200, 0.8);
  const aliento2 = new D.Biquad('hp', 1800, 0.7);
  const out = new Float32Array(voz.length + Math.round(1.5 * SR));
  let env = 0;
  for (let i = 0; i < voz.length; i++) {
    const v = hp.paso(voz[i]);
    env += (Math.abs(v) - env) * 0.004;
    const n = aliento2.paso(aliento.paso(rnd() * 2 - 1));
    out[i] = v * 0.35 + n * env * 7;
  }
  const rv = new D.Freeverb({ sala: 0.6, amortiguacion: 0.6, predelayMs: 8 });
  for (let i = 0; i < out.length; i++) out[i] = out[i] + rv.paso(out[i])[0] * 0.5;
  // normalizar y bordes suaves
  let m = 0;
  for (const x of out) m = Math.max(m, Math.abs(x));
  const g = Math.pow(10, -3 / 20) / (m || 1);
  for (let i = 0; i < out.length; i++) out[i] *= g;
  for (let i = 0; i < 2000; i++) { out[i] *= i / 2000; out[out.length - 1 - i] *= i / 2000; }
  return out;
}

async function guardarMono (nombre, buf) {
  const { createOggEncoder } = require('wasm-media-encoders');
  const enc = await createOggEncoder();
  enc.configure({ sampleRate: SR, channels: 1, vbrQuality: 5 });
  const trozos = [];
  for (let i = 0; i < buf.length; i += 4096) trozos.push(Buffer.from(enc.encode([buf.subarray(i, i + 4096)])));
  trozos.push(Buffer.from(enc.finalize()));
  fs.writeFileSync(path.join(SALIDA, `${nombre}.ogg`), Buffer.concat(trozos));
  console.log(`  ${nombre}.ogg  ${(buf.length / SR).toFixed(1)} s mono`);
}

async function guardarEst (nombre, est) {
  await D.guardarOgg(path.join(SALIDA, `${nombre}.ogg`), est, 5);
  console.log(`  ${nombre}.ogg  ${(est[0].length / SR).toFixed(1)} s estereo`);
}

(async () => {
  console.log('Voces');
  await guardarEst('megafonia_auditorio', megafonia('megafonia_auditorio'));
  await guardarEst('megafonia_linterna', megafonia('megafonia_linterna'));
  await guardarEst('megafonia_alerta', alerta());
  await guardarMono('susurro_detras', susurro('susurro_detras'));
  await guardarMono('susurro_atras', susurro('susurro_atras'));

  const f = path.join(SALIDA, '..', 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  j['megafonia.vestibulo'] = { sounds: [{ name: 'backrooms_evento:megafonia_auditorio' }, { name: 'backrooms_evento:megafonia_linterna' }] };
  j['megafonia.alerta'] = { sounds: [{ name: 'backrooms_evento:megafonia_alerta' }] };
  j.susurros = {
    sounds: [
      { name: 'backrooms_evento:susurros', weight: 1 },
      { name: 'backrooms_evento:susurro_detras', weight: 2 },
      { name: 'backrooms_evento:susurro_atras', weight: 2 }
    ]
  };
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
})();
