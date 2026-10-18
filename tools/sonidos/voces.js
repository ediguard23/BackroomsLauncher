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
 *  megafonia_inundacion / megafonia_drenaje  las de la inundacion de las Piscinas
 *                (fallo del drenaje / averia contenida), con los tonos de aviso
 *  megafonia_recepcion_*  los avisos de la recepcion (cada 2 min, a todos a la vez)
 *
 *  fiesta_*      la megafonia de la Fiesta (el animador, demasiado alegre): un jingle de
 *                feria y la voz por un altavoz viejo, en mono (suena desde los altavoces);
 *                el anuncio "cortado" se corta de golpe con un chasquido
 *  semaforo_*, fiesta_nino_*  la nina (el semaforo y las voces de salas vacias)
 *  anfitrion_*   el Anfitrion: grave y ahogado, como dentro de la cabezota de espuma
 *  Lo que aun no tenga su wav sale con lo sintetizado de fiesta.js (la senal, el silbato...)
 *  o, el Anfitrion, con una risa y un murmullo sinteticos.
 *
 * Si falta el wav de una voz (aun sin generar), esa megafonia sale solo con su
 * campanilla o sus tonos, y la recepcion se queda con las que haya.
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

function hay (nombre) {
  return fs.existsSync(path.join(ORIGEN, `${nombre}.wav`));
}

/** Lee un wav como mono float a 44,1 kHz; `velocidad` < 1 lo hace mas lento y grave. */
function leer (nombre, velocidad = 1) {
  if (!hay(nombre)) return silencio(0.5);
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

function alerta (nombre = 'megafonia_alerta') {
  const voz = altavoz(leer(nombre), { bajo: 450, alto: 2900, saturacion: 3.2 });
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

/* ------------------------------------------------------------- la Fiesta */

const F = require('./fiesta');
const nota = (n) => 440 * Math.pow(2, (n - 69) / 12);

/** El jingle de feria antes de cada anuncio: arpegio de cajita y un toque de matasuegras. */
function jingleFiesta () {
  const out = silencio(1.3);
  [72, 76, 79, 84].forEach((n, i) => {
    const f = nota(n);
    const i0 = Math.round(i * 0.12 * SR);
    for (let k = 0; k < Math.round(0.9 * SR) && i0 + k < out.length; k++) {
      const t = k / SR;
      out[i0 + k] += (Math.sin(TAU * f * t) * Math.exp(-t * 4) + Math.sin(TAU * f * 3.01 * t) * 0.2 * Math.exp(-t * 10)) * 0.3;
    }
  });
  return out;
}

function monoNorm (x, picoDb = -2) {
  let m = 0;
  for (const v of x) m = Math.max(m, Math.abs(v));
  const g = Math.pow(10, picoDb / 20) / (m || 1);
  const out = new Float32Array(x.length);
  for (let i = 0; i < x.length; i++) out[i] = x[i] * g;
  for (let i = 0; i < 400 && i < out.length; i++) { out[i] *= i / 400; out[out.length - 1 - i] *= i / 400; }
  return out;
}

/** Anuncio de la Fiesta: jingle y voz por el altavoz viejo, con sala. `cortar`: se corta de golpe. */
function megafoniaFiesta (nombre, cortar = false) {
  let voz = altavoz(leer(nombre), { bajo: 350, alto: 3200, saturacion: 2.2 });
  let fin = silencio(1.2);
  if (cortar) {
    // se corta en seco (un poco antes del final) con un chasquido y un pitido de acople
    voz = voz.slice(0, Math.max(0, voz.length - Math.round(0.35 * SR)));
    fin = silencio(1.4);
    for (let i = 0; i < 60; i++) fin[i] = (Math.random() * 2 - 1) * (1 - i / 60);
    for (let i = 0; i < Math.round(0.5 * SR); i++) fin[200 + i] += Math.sin(TAU * 2700 * i / SR) * 0.08 * Math.exp(-i / SR * 6);
  }
  const todo = concatenar(silencio(0.05), jingleFiesta(), voz, fin);
  return monoNorm(F.sala(todo, 0.4, 0.7), -2);
}

/** Senal sintetizada y, si ya hay voz grabada, la voz de la nina encima. */
function nina (nombre, base, retraso = 0.15) {
  const s = base();
  if (!hay(nombre)) return s;
  const v = F.sala(leer(nombre), 0.3, 0.6);
  const out = new Float32Array(Math.max(s.length, Math.round(retraso * SR) + v.length));
  out.set(s);
  for (let i = 0; i < v.length; i++) out[Math.round(retraso * SR) + i] += v[i] * 1.4;
  return monoNorm(out, -1);
}

/** La voz del Anfitrion: mas grave, ahogada (sin agudos) y con la resonancia de la cabezota. */
function anfitrionVoz (nombre) {
  const v = leer(nombre, 0.9);
  const lp = new D.Biquad('lp', 1100, 0.8);
  const caja = new D.Biquad('pico', 420, 1.5, 6);
  const out = new Float32Array(v.length);
  for (let i = 0; i < v.length; i++) out[i] = caja.paso(lp.paso(v[i]));
  return monoNorm(F.sala(out, 0.35, 0.65), -2);
}

/** Risa sintetica ahogada (jo, jo, jo) para cuando no hay voz grabada. */
function risaSintetica (semilla) {
  const rnd = D.azar(900 + semilla);
  const out = silencio(2.0);
  const lp = new D.Biquad('lp', 800, 0.7);
  for (let k = 0; k < 5; k++) {
    const t0 = 0.05 + k * (0.22 + rnd() * 0.05);
    const f = 120 - k * 6 + rnd() * 8;
    for (let i = 0; i < Math.round(0.16 * SR); i++) {
      const t = i / SR;
      const fase = (f * t) % 1;
      out[Math.round(t0 * SR) + i] += lp.paso(D.sierra(fase, f / SR) + (rnd() - 0.5) * 0.3) * Math.sin(Math.PI * t / 0.16);
    }
  }
  return monoNorm(F.sala(out, 0.4, 0.7), -3);
}

async function guardarMonoV (nombre, buf) {
  await guardarMono(nombre, buf);
}

(async () => {
  console.log('Voces');
  await guardarEst('megafonia_linterna', megafonia('megafonia_linterna'));
  await guardarEst('megafonia_alerta', alerta());
  await guardarEst('megafonia_inundacion', alerta('megafonia_inundacion'));
  await guardarEst('megafonia_drenaje', megafonia('megafonia_drenaje'));
  const recepcion = ['megafonia_recepcion_1', 'megafonia_recepcion_2', 'megafonia_recepcion_3'].filter(hay);
  // la Fiesta: la megafonia del animador
  const FIESTA = ['fiesta_bienvenida', 'fiesta_apagon', 'fiesta_luz', 'fiesta_premio', 'fiesta_anuncio_tickets', 'fiesta_anuncio_semaforo',
    'fiesta_anuncio_comida', 'fiesta_anuncio_sonrian', 'fiesta_anuncio_cortado'];
  for (const n of FIESTA) await guardarMonoV(n, megafoniaFiesta(n, n === 'fiesta_anuncio_cortado'));
  // la nina del semaforo (con su senal debajo) y las voces de salas vacias
  await guardarMonoV('semaforo_verde', nina('semaforo_verde', () => F.senal(true)));
  await guardarMonoV('semaforo_roja', nina('semaforo_roja', () => F.senal(false)));
  await guardarMonoV('semaforo_pillado', nina('semaforo_pillado', F.silbato, 0.5));
  await guardarMonoV('semaforo_gana', nina('semaforo_gana', F.fanfarria, 0.8));
  const ninos = ['fiesta_nino_1', 'fiesta_nino_2', 'fiesta_nino_3', 'fiesta_nino_4'].filter(hay);
  for (const n of ninos) await guardarMonoV(n, monoNorm(F.sala(leer(n), 0.5, 0.8), -4));
  // el Anfitrion
  const vocesAnf = ['anfitrion_voz_1', 'anfitrion_voz_2', 'anfitrion_voz_3'].filter(hay);
  for (const n of vocesAnf) await guardarMonoV(n, anfitrionVoz(n));
  const risas = ['anfitrion_risa_1', 'anfitrion_risa_2'].filter(hay);
  for (const n of risas) await guardarMonoV(n, anfitrionVoz(n));
  if (!risas.length) { await guardarMonoV('anfitrion_risa_sint1', risaSintetica(1)); await guardarMonoV('anfitrion_risa_sint2', risaSintetica(2)); }
  for (const r of recepcion) await guardarEst(r, megafonia(r));
  await guardarMono('susurro_detras', susurro('susurro_detras'));
  await guardarMono('susurro_atras', susurro('susurro_atras'));

  const f = path.join(SALIDA, '..', 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  // la recepcion ya no manda a nadie al auditorio: la linterna y los avisos de espera, uno por
  // evento para que el servidor elija cual (y todos oigan el mismo a la vez: Vestibulo#megafonia);
  // un aviso sin grabar aun suena como el de la linterna
  j['megafonia.vestibulo'] = { sounds: [{ name: 'backrooms_evento:megafonia_linterna' }] };
  for (let i = 1; i <= 3; i++) {
    const n = `megafonia_recepcion_${i}`;
    j[`megafonia.recepcion_${i}`] = { sounds: [{ name: `backrooms_evento:${recepcion.includes(n) ? n : 'megafonia_linterna'}` }] };
  }
  j['megafonia.alerta'] = { sounds: [{ name: 'backrooms_evento:megafonia_alerta' }] };
  j['megafonia.inundacion'] = { sounds: [{ name: 'backrooms_evento:megafonia_inundacion' }] };
  j['megafonia.drenaje'] = { sounds: [{ name: 'backrooms_evento:megafonia_drenaje' }] };
  // la Fiesta: un evento por anuncio (cada cliente elige el suyo: AmbienteFiesta) y las voces
  const ev = (nombres, dist) => ({ sounds: nombres.map((n) => ({ name: `backrooms_evento:${n}`, ...(dist ? { attenuation_distance: dist } : {}) })) });
  j['fiesta.bienvenida'] = ev(['fiesta_bienvenida']);
  j['fiesta.apagon'] = ev(['fiesta_apagon']);
  j['fiesta.luz'] = ev(['fiesta_luz']);
  j['fiesta.premio'] = ev(['fiesta_premio']);
  for (const a of ['tickets', 'semaforo', 'comida', 'sonrian', 'cortado']) j[`fiesta.anuncio_${a}`] = ev([`fiesta_anuncio_${a}`], 48);
  j['semaforo.verde'] = ev(['semaforo_verde'], 64);
  j['semaforo.roja'] = ev(['semaforo_roja'], 64);
  j['semaforo.pillado'] = ev(['semaforo_pillado'], 24);
  j['semaforo.gana'] = ev(['semaforo_gana'], 24);
  j['fiesta.nino'] = ev(ninos.length ? ninos : ['fiesta_juguete2'], 40);
  j['anfitrion.voz'] = ev(vocesAnf.length ? vocesAnf : ['anfitrion_respira1', 'anfitrion_respira2'], 32);
  j['anfitrion.risa'] = ev(risas.length ? risas : ['anfitrion_risa_sint1', 'anfitrion_risa_sint2'], 64);
  j.susurros = {
    sounds: [
      { name: 'backrooms_evento:susurros', weight: 1 },
      { name: 'backrooms_evento:susurro_detras', weight: 2 },
      { name: 'backrooms_evento:susurro_atras', weight: 2 }
    ]
  };
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
})();
