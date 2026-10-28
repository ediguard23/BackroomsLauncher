#!/usr/bin/env node
'use strict';
/**
 * El sonido de la cinematica de la salida de la Fiesta (SalidaFiestaCliente), con los tiempos de su
 * guion. Esta vez con grabaciones de verdad (golpes, chapa, puertas, motor, cables...): sonidos CC0
 * (dominio publico) de BigSoundBank (https://bigsoundbank.com, de Joseph Sardin). No se suben al
 * repo: van en tools/sonidos/reales/ (en el .gitignore) y si falta alguno se descarga solo de
 * https://bigsoundbank.com/UPLOAD/mp3/<numero>.mp3. Los numeros de cada uno estan en REALES.
 * Las voces del Anfitrion son de Higgsfield (tools/sonidos/voces/).
 *
 * Dos archivos (Minecraft corta todo al cambiar de mundo):
 *
 *   cinematica_salida_fiesta.ogg (67 s, evento "cinematica.salida_fiesta"):
 *     0.6         el «ding» del ascensor; dentro, el zumbido del plafon
 *     4.8         un golpe lejano en el salon
 *     12.6 - 19.5 cinco golpes en la puerta morada, cada uno distinto y mas fuerte (lejos, con eco del salon)
 *     21.3        la madera cruje y cede; 21.9 la puerta cae al suelo
 *     22.3 - 41   el jugador jadea y el corazon se le dispara; 24.7 y 27.5 aporrea la botonera (clics)
 *     22.6 - 23.4 dos pasos lentos al salir; 23.8 su chillido y echa a correr: pasos cada vez mas cerca
 *     29.6 - 31.0 las puertas del ascensor se cierran (el golpe al encajar)
 *     31.3        se estampa contra ellas; 31.55 «¡NOOO!» (voces/anfitrion_no.wav)
 *     35.5 - 40.4 golpes contra las puertas (acero grueso: grave, sin agudos), cada vez mas flojos
 *     40.4 - 43.6 silencio (respira tras la puerta; el jugador contiene el aliento)
 *     43.6        la amenaza amortiguada por las puertas (voces/anfitrion_amenaza.wav, 9.5 s)
 *     53.5 - 57.4 arriba: la cadena, la radial cortando, chispas y un golpe
 *     57.7        arranca el motor; 60.5 chispazo y la luz que falla; 61.1 salta el diferencial
 *     61.5, 62.0  se parten los cables; 62.1 - 65.7 cae (rugido, viento, la cabina traquetea)
 *     65.7        el golpe contra el fondo; enseguida, nada
 *   cinematica_salida_fiesta_despues.ogg (16.5 s, "cinematica.salida_fiesta.despues"), ya en la
 *     Estacion: pitido y corazon amortiguados en el negro, que se van apagando al abrir los ojos;
 *     el zumbido de los transformadores que vuelve; a 11 s el chispazo del titulo «NIVEL 3».
 *
 *   node tools/sonidos/salida-fiesta.js
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SONIDOS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
const REALES = path.join(__dirname, 'reales');
const VOCES = path.join(__dirname, 'voces');
const DUR = 67.0;
const DUR_FIN = 16.5;

/** Los de BigSoundBank (CC0) que se usan, por numero. */
const USADOS = {
  '0103': 'portazo', '0105': 'punetazo', '0134': 'descarga electrica', '0146': 'viento fuerte', '0218': 'latido',
  '0223': 'portazos', '0336': 'puerta corredera', '0359': 'cadena pesada', '0374': 'llamar a la puerta',
  '0616': 'puerta metalica que cruje', '0806': 'punetazos', '0952': 'soldadura', '0064': 'transformador',
  '0086': 'transformador', '0808': 'botones de horno', '1025': 'tres golpes de teatro', '1103': 'hombre sin aliento', '1022': 'caida de piedra', '1092': 'diferencial', '1108': 'martillo que cae',
  '1118': 'radial cortando', '1299': 'ramas que se parten', '1300': 'rama que se parte', '1318': 'pasos rapidos',
  '1339': 'portazo', '1388': 'acufeno', '1501': 'portazo', '1711': 'ascensor', '1771': 'chapa',
  '1772': 'chapa', '1773': 'chapa', '1774': 'chapa', '1776': 'chapa', '1777': 'chapa', '2118': 'neon',
  '2436': 'vibraciones', '2452': 'caida de un cuerpo', '2453': 'caida de un cuerpo', '2949': 'latigo',
  '2951': 'latigo', '3061': 'interruptor industrial', '3113': 'trueno', '3341': 'tren de mercancias'
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

/** Baja los que falten (los mp3 publicos del reproductor de BigSoundBank). */
async function descargar () {
  fs.mkdirSync(REALES, { recursive: true });
  for (const id of Object.keys(USADOS)) {
    const f = path.join(REALES, `${id}.mp3`);
    if (fs.existsSync(f)) continue;
    process.stdout.write(`  descargando ${id} (${USADOS[id]})... `);
    const r = await fetch(`https://bigsoundbank.com/UPLOAD/mp3/${id}.mp3`);
    if (!r.ok) throw new Error(`no se pudo bajar ${id}: ${r.status}`);
    fs.writeFileSync(f, Buffer.from(await r.arrayBuffer()));
    console.log('ok');
  }
}

const cache = new Map();
/** Un archivo de sonido (mp3, wav u ogg) a mono 44.1 kHz. */
function leer (archivo) {
  if (!cache.has(archivo)) {
    const buf = execFileSync(ffmpeg(), ['-v', 'error', '-i', archivo, '-af', 'aresample=44100', '-ac', '1', '-f', 'f32le', '-'], { maxBuffer: 1 << 29 });
    cache.set(archivo, new Float32Array(buf.buffer, buf.byteOffset, buf.length / 4).slice());
  }
  return cache.get(archivo);
}

/**
 * Un trozo de un sonido real, normalizado (pico 1): desde..hasta segundos del original, con su tono
 * (0.8 = mas grave y lento) y fundidos de entrada y salida.
 */
function real (id, desde = 0, hasta = Infinity, { tono = 1, entra = 0.005, sale = 0.05 } = {}) {
  const s = leer(path.join(REALES, `${id}.mp3`));
  const i0 = Math.max(0, Math.round(desde * SR));
  const i1 = Math.min(s.length, Math.round(hasta * SR));
  let out = s.slice(i0, i1);
  if (tono !== 1) {
    const n = Math.floor(out.length / tono);
    const r = new Float32Array(n);
    for (let i = 0; i < n; i++) {
      const x = i * tono;
      const j = Math.floor(x);
      const f = x - j;
      r[i] = (out[j] || 0) * (1 - f) + (out[j + 1] || 0) * f;
    }
    out = r;
  }
  let pico = 1e-9;
  for (const v of out) pico = Math.max(pico, Math.abs(v));
  const fe = Math.floor(entra * SR);
  const fs_ = Math.floor(sale * SR);
  for (let i = 0; i < out.length; i++) {
    let g = 1 / pico;
    if (i < fe) g *= i / fe;
    if (i > out.length - fs_) g *= (out.length - i) / fs_;
    out[i] *= g;
  }
  return out;
}

/** Una voz de Higgsfield (normalizada) o, si no esta, la de repuesto. */
function voz (nombre, repuesto, tono = 0.9) {
  const f = path.join(VOCES, nombre);
  const propia = fs.existsSync(f);
  let s = propia ? leer(f) : leer(path.join(SONIDOS, repuesto));
  if (propia) {
    // como las demas del Anfitrion (voces.js): mas grave y lenta, con la resonancia de la cabezota
    const n = Math.floor(s.length / tono);
    const r = new Float32Array(n);
    for (let i = 0; i < n; i++) {
      const x = i * tono;
      const j = Math.floor(x);
      r[i] = (s[j] || 0) * (1 - (x - j)) + (s[j + 1] || 0) * (x - j);
    }
    s = filtro(filtro(r, 'lp', 2400, 0.8), 'pico', 420, 1.5, 6);
  }
  let pico = 1e-9;
  for (const v of s) pico = Math.max(pico, Math.abs(v));
  return { s: s.map((v) => v / pico), propia };
}

/** Filtra un mono con un Biquad nuevo. */
function filtro (mono, tipo, f, q = 0.7, gan = 0) {
  const b = new D.Biquad(tipo, f, q, gan);
  return mono.map((v) => b.paso(v));
}

/** Un buffer estereo para mezclar en un momento, con ganancia y paneo. */
function pista (s) {
  const n = Math.ceil(s * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  /** `t0` puede ser el momento del golpe si se da `en` (donde esta el golpe dentro del trozo). */
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

/** El «ding» del ascensor (dos campanadas suaves, como las de verdad). */
function ding () {
  const n = Math.round(2.2 * SR);
  const out = new Float32Array(n);
  for (const [t0, f] of [[0, 1318.5], [0.42, 1046.5]]) {
    for (let i = Math.round(t0 * SR); i < n; i++) {
      const t = i / SR - t0;
      const e = Math.exp(-t / 0.55) * Math.min(1, t / 0.004);
      out[i] += (Math.sin(TAU * f * t) + 0.35 * Math.sin(TAU * f * 2.76 * t) * Math.exp(-t / 0.15) + 0.2 * Math.sin(TAU * f * 5.4 * t) * Math.exp(-t / 0.06)) * e * 0.4;
    }
  }
  return out;
}

function cinematica () {
  const rnd = D.azar(5521);
  const lejos = pista(DUR);   // en el salon: con su eco grande y sin agudos
  const cabina = pista(DUR);  // dentro del ascensor
  const tras = pista(DUR);    // al otro lado de las puertas cerradas (amortiguado)
  const seco = pista(DUR);    // sin sala

  // --- dentro, tranquilo: el ding y el zumbido del plafon (hasta que falla la luz)
  seco.poner(0.6, ding(), 0.35);
  const zumbido = filtro(real('2118', 0, 9.9, { entra: 0.5, sale: 0.5 }), 'lp', 900);
  for (let t = 0; t < 52.6; t += 9.3) cabina.poner(t, zumbido.subarray(0, Math.round(Math.min(9.4, 52.6 - t) * SR)), 0.05);
  // 4.8: algo cae lejos, en el salon
  lejos.poner(4.8, real('0103', 1.6, 4.3, { tono: 0.8 }), 0.55, 0.05, 0.47);
  lejos.poner(4.95, real('0616', 20.0, 21.3, { tono: 0.85, entra: 0.05, sale: 0.4 }), 0.25, 0.05);

  // --- los cinco golpes en la puerta morada: cada uno distinto (portazos y punetazos sobre madera), mas fuerte
  const golpes = [
    [12.6, real('0374', 0.25, 0.62, { tono: 0.85 }), 0.11, 0.42],
    [14.4, real('0223', 0.0, 0.9, { tono: 0.9 }), 0.08, 0.5],
    [16.0, real('1339', 0.0, 0.8, { tono: 0.82 }), 0.04, 0.6],
    [17.8, real('0223', 1.9, 2.75, { tono: 0.85 }), 0.12, 0.72],
    [19.5, real('1501', 0.0, 0.9, { tono: 0.8 }), 0.25, 0.9]
  ];
  for (const [t, s, en, g] of golpes) {
    lejos.poner(t, s, g * 1.35, 0.04, en);
    // el porrazo grave del puno
    lejos.poner(t, filtro(real('0105', 0.6, 1.4, { tono: 0.75 }), 'lp', 500), g * 0.6, 0.04, 0.25);
  }
  // 21.3: la madera cruje y cede; 21.9: la puerta cae plana al suelo
  lejos.poner(21.3, real('1299', 3.6, 4.3, { tono: 0.75 }), 0.75, 0.04, 0.2);
  lejos.poner(21.36, real('1300', 0.1, 1.2, { tono: 0.7 }), 0.6, 0.04, 0.11);
  lejos.poner(21.9, real('0223', 1.95, 3.4, { tono: 0.7 }), 1.0, 0.04, 0.07);
  lejos.poner(21.9, real('2453', 0.4, 1.6, { tono: 0.8 }), 0.7, 0.04, 0.15);

  // --- el Anfitrion: dos pasos al salir, el chillido y la carrera (pasos cada 0.24 s, como su animacion)
  const paso = (k) => {
    const al = [[1.95, 0.3], [4.4, 0.3], [9.87, 0.3], [13.98, 0.3], [14.56, 0.3], [10.18, 0.3]][k % 6];
    return real('1318', al[0] - 0.05, al[0] + al[1], { tono: 0.78 });
  };
  lejos.poner(22.75, paso(0), 0.3, 0.04);
  lejos.poner(23.27, paso(1), 0.32, 0.04);
  try {
    lejos.poner(23.75, leer(path.join(SONIDOS, 'anfitrion_detecta.ogg')), 0.55, 0.04);
  } catch (e) {
    console.log('  (sin anfitrion_detecta.ogg)');
  }
  const z0 = -14;   // relativo: de la puerta (a 50 bloques) a las puertas del ascensor
  const zAnf = (t) => {
    const total = 49.5;
    const v = total / (31.2 - 23.8 - 0.25);
    const tau = t - 23.8;
    return Math.min(total, tau < 0.5 ? v * tau * tau : v * (tau - 0.25));
  };
  for (let t = 23.95, k = 2; t < 31.15; k++) {
    const d = Math.max(2.5, 51.5 - zAnf(t));
    const g = Math.min(0.7, 3.2 / d + 0.08);
    const s = paso(k);
    // de lejos, por el salon; los ultimos, ya por la boca del ascensor
    (d > 8 ? lejos : cabina).poner(t, d > 8 ? s : filtro(s, 'lp', 3000), g * (d > 8 ? 0.9 : 0.65), k % 2 ? 0.12 : -0.12);
    t += t < 24.3 ? 0.36 : 0.238;
  }
  void z0;

  // --- el jugador, desesperado (pegado al oido): respira a bocanadas y el corazon se le dispara desde que cae la
  // puerta; aporrea la botonera dos veces mientras el Anfitrion viene corriendo; al cerrarse, sigue jadeando y
  // contiene el aliento con la amenaza; vuelve a jadear cuando el ascensor falla
  const jugador = pista(DUR);
  const aliento = filtro(real('1103', 0, 19.0, { entra: 0.3, sale: 1.0 }), 'lp', 5000);
  const env = (t, puntos) => {
    for (let k = 1; k < puntos.length; k++) {
      if (t < puntos[k][0]) {
        const [t0, a] = puntos[k - 1];
        const [t1, b] = puntos[k];
        return a + (b - a) * (t - t0) / (t1 - t0);
      }
    }
    return puntos[puntos.length - 1][1];
  };
  const conEnvolvente = (mono, t0, puntos) => mono.map((v, i) => v * env(t0 + i / SR, puntos));
  jugador.poner(22.3, conEnvolvente(aliento, 22.3, [[22.3, 0], [24.0, 0.45], [31.0, 0.6], [40.4, 0.45], [43.0, 0.08], [44.0, 0.05]]), 1.0);
  jugador.poner(52.8, conEnvolvente(aliento.subarray(0, Math.round(13 * SR)), 52.8, [[52.8, 0], [54.5, 0.25], [61.5, 0.5], [65.7, 0.55]]), 1.0);
  const latido = filtro(real('0218', 0, 8.6, { tono: 1.35, entra: 0.2, sale: 0.3 }), 'lp', 190);
  for (const [desde, hasta, g] of [[22.0, 41.5, 0.7], [53.4, 65.7, 0.55]]) {
    for (let t = desde; t < hasta; t += latido.length / SR - 0.05) {
      jugador.poner(t, latido.subarray(0, Math.round(Math.min(latido.length / SR, hasta - t) * SR)), g);
    }
  }
  const clics = [5.44, 5.71, 7.08, 11.55, 11.95, 12.35, 16.72, 0.08];
  [24.75, 24.95, 25.12, 25.28, 27.5, 27.65, 27.8, 27.95].forEach((t, k) => {
    cabina.poner(t, real('0808', clics[k] - 0.02, clics[k] + 0.18, { sale: 0.04 }), 0.55, 0.35, 0.02);
  });

  // --- las puertas del ascensor se cierran (el golpe al encajar a 31.0)
  cabina.poner(31.0, real('0336', 2.6, 4.9, { sale: 0.3 }), 0.4, 0, 4.14 - 2.6);
  // los golpes contra las puertas: una puerta de acero gruesa, no una lata: la chapa muy grave y sin agudos, el
  // porrazo seco de madera de teatro (que da el cuerpo), el puno y un portazo grave por debajo
  const puerta = pista(DUR);
  const porrazos = [0.10, 3.67, 4.87, 6.03, 0.73, 1.38, 1.70, 2.02, 2.35];
  const golpePuerta = (t, k, fuerza) => {
    const [id, en] = [['1777', 0.07], ['1774', 0.11], ['1776', 0.06], ['1772', 0.47], ['1777', 0.07], ['1774', 0.11], ['1776', 0.06], ['1777', 0.07], ['1774', 0.11]][k % 9];
    const tono = 0.52 + rnd() * 0.06;
    puerta.poner(t, real(id, Math.max(0, en - 0.05), en + 1.6, { tono }), 0.5 * fuerza, k % 2 ? 0.15 : -0.15, Math.min(en, 0.05) / tono);
    const h = porrazos[k % porrazos.length];
    puerta.poner(t, real('1025', h - 0.05, h + 0.6, { tono: 0.8 }), 1.0 * fuerza, 0, 0.05 / 0.8);
    puerta.poner(t, real('0105', 0.6, 1.6, { tono: 0.7 }), 0.6 * fuerza, 0, 0.19 / 0.7);
    puerta.poner(t, real('0103', 1.9, 2.8, { tono: 0.65 }), 0.45 * fuerza, 0, 0.08 / 0.65);
  };
  // 31.3: se estampa contra ellas
  golpePuerta(31.3, 0, 1.25);
  golpePuerta(31.36, 1, 0.6);
  // «¡NOOO!» pegado a las puertas (la chapa le quita los agudos)
  const no = voz('anfitrion_no.wav', 'anfitrion_pierde.ogg', 0.92);
  puerta.poner(31.55, no.s, no.propia ? 0.9 : 0.7);
  // golpes contra las puertas, cada vez mas flojos
  const fuerza = [1.0, 1.0, 0.95, 0.9, 0.8, 0.7, 0.55, 0.42, 0.3];
  [35.5, 36.0, 36.4, 36.9, 37.4, 38.0, 38.7, 39.5, 40.4].forEach((t, k) => golpePuerta(t, k + 2, fuerza[k]));
  // silencio: solo el zumbido y, al otro lado, como respira
  try {
    tras.poner(41.7, leer(path.join(SONIDOS, 'anfitrion_respira1.ogg')), 0.25);
    tras.poner(42.8, leer(path.join(SONIDOS, 'anfitrion_respira2.ogg')), 0.25);
  } catch (e) {
    console.log('  (sin anfitrion_respira)');
  }
  // 43.6: la amenaza, amortiguada tras las puertas
  const amenaza = voz('anfitrion_amenaza.wav', 'anfitrion_voz_2.ogg', 0.95);
  tras.poner(43.6, amenaza.s, amenaza.propia ? 1.2 : 0.8);

  // --- arriba, en el techo de la cabina: la cadena, golpes, la radial y chispas
  const techo = pista(DUR);
  techo.poner(53.5, real('0359', 1.0, 4.2, { entra: 0.05, sale: 0.3 }), 0.55, -0.2, 0.21);
  [53.7, 54.2, 54.6, 57.3].forEach((t, k) => techo.poner(t, real(['1773', '1774', '1776', '1777'][k], 0, 1.0, { tono: 0.6 }), 0.5, k % 2 ? 0.3 : -0.3, 0.1));
  techo.poner(54.9, real('1118', 3.4, 5.6, { entra: 0.15, sale: 0.3 }), 0.55, 0.15);
  techo.poner(57.05, real('0952', 2.7, 3.6, { entra: 0.02, sale: 0.1 }), 0.4, 0.2);

  // --- el motor arranca y baja; la luz falla; el diferencial; los cables
  cabina.poner(57.7, real('1711', 4.6, 9.4, { entra: 0.05, sale: 0.4 }), 0.6, 0);
  cabina.poner(60.5, real('0134', 8.4, 9.8, { entra: 0.01, sale: 0.2 }), 0.75, 0.25);
  seco.poner(60.55, filtro(real('2118', 0, 1.0), 'bp', 2400, 1.2), 0.25, 0.25);
  cabina.poner(61.1, real('1092', 0.2, 1.6), 0.7, -0.25, 0.1);
  cabina.poner(60.8, real('0616', 20.0, 21.0, { tono: 0.7, entra: 0.05 }), 0.6, 0);
  for (const t of [61.5, 62.0]) {
    const a = t < 61.7;
    cabina.poner(t, real(a ? '2949' : '2951', 0, 2.0, { tono: 0.62 }), 0.95, a ? -0.3 : 0.3, (a ? 0.26 : 0.30) / 0.62);
    cabina.poner(t, real('1774', 0, 2.6, { tono: 0.6 }), 0.8, 0, 0.11 / 0.6);
  }
  // --- la caida: el rugido (tren), el viento y la cabina que traquetea, cada vez mas
  const caida = (mono, gan, sube = 3.6) => {
    const n = Math.round(3.6 * SR);
    const out = new Float32Array(n);
    for (let i = 0; i < n && i < mono.length; i++) out[i] = mono[i] * gan * Math.pow(Math.min(1, i / SR / sube), 1.4);
    return out;
  };
  cabina.poner(62.1, caida(real('3341', 10.0, 13.7), 0.85), 1, 0);
  cabina.poner(62.1, caida(real('0146', 41.0, 44.7), 0.6), 1, 0);
  cabina.poner(62.05, caida(real('2436', 0, 3.7, { tono: 0.8 }), 0.5, 1.0), 1, 0);
  for (let t = 62.2, k = 0; t < 65.6; k++) {
    const id = ['1771', '1773', '1776', '1772'][k % 4];
    cabina.poner(t, filtro(real(id, 0, 0.5, { tono: 0.7 + rnd() * 0.2, sale: 0.15 }), 'lp', 2500), 0.15 + 0.3 * (t - 62.1) / 3.6, rnd() * 0.8 - 0.4, 0.05);
    t += 0.16 + rnd() * 0.12;
  }
  // --- el golpe contra el fondo
  seco.poner(65.7, real('3113', 2.0, 5.1, { entra: 0.002 }), 1.5, 0, 0.27);
  cabina.poner(65.7, real('1774', 0, 2.5, { tono: 0.7 }), 1.3, 0, 0.11 / 0.7);
  cabina.poner(65.7, real('2453', 0.3, 1.6), 0.8, 0, 0.22);
  cabina.poner(65.7, real('1108', 0, 1.3, { tono: 0.6 }), 0.6, 0, 0.21 / 0.6);

  // las salas
  const salon = new D.Freeverb({ sala: 0.92, amortiguacion: 0.45, predelayMs: 55 });
  const cab = new D.Freeverb({ sala: 0.3, amortiguacion: 0.75, predelayMs: 4 });
  const L = new Float32Array(lejos.n);
  const R = new Float32Array(lejos.n);
  const lpLejos = new D.Biquad('lp', 2300, 0.7);
  const lpLejosR = new D.Biquad('lp', 2300, 0.7);
  const lpTras = new D.Biquad('lp', 950, 0.7);
  const lpTrasR = new D.Biquad('lp', 950, 0.7);
  const graveTras = new D.Biquad('pico', 220, 0.8, 4);
  const graveTrasR = new D.Biquad('pico', 220, 0.8, 4);
  const lpTecho = new D.Biquad('lp', 1700, 0.7);
  const lpTechoR = new D.Biquad('lp', 1700, 0.7);
  // las puertas: sin agudos (son gruesas) y con mucho cuerpo abajo
  const lpPuerta = new D.Biquad('lp', 1800, 0.7);
  const lpPuertaR = new D.Biquad('lp', 1800, 0.7);
  const gravePuerta = new D.Biquad('pico', 95, 0.9, 6);
  const gravePuertaR = new D.Biquad('pico', 95, 0.9, 6);
  for (let i = 0; i < lejos.n; i++) {
    const lL = lpLejos.paso(lejos.L[i]);
    const lR = lpLejosR.paso(lejos.R[i]);
    const [a, b] = salon.paso((lL + lR) * 0.5);
    const tL = graveTras.paso(lpTras.paso(tras.L[i]));
    const tR = graveTrasR.paso(lpTrasR.paso(tras.R[i]));
    const uL = lpTecho.paso(techo.L[i]);
    const uR = lpTechoR.paso(techo.R[i]);
    const pL = gravePuerta.paso(lpPuerta.paso(puerta.L[i]));
    const pR = gravePuertaR.paso(lpPuertaR.paso(puerta.R[i]));
    const dentroL = cabina.L[i] + tL + uL + pL;
    const dentroR = cabina.R[i] + tR + uR + pR;
    const [c, d] = cab.paso((dentroL + dentroR) * 0.5);
    L[i] = lL * 0.4 + a * 0.75 + dentroL + c * 0.18 + seco.L[i] + jugador.L[i];
    R[i] = lR * 0.4 + b * 0.75 + dentroR + d * 0.18 + seco.R[i] + jugador.R[i];
  }
  // tras el golpe se va todo (pierde el conocimiento): fundido rapido a nada
  const c0 = Math.round(66.05 * SR);
  const c1 = Math.round(66.6 * SR);
  for (let i = c0; i < L.length; i++) {
    const g = i >= c1 ? 0 : Math.pow(1 - (i - c0) / (c1 - c0), 2);
    L[i] *= g;
    R[i] *= g;
  }
  return D.fundidos(D.masterizar([L, R], { rmsDb: -21, picoDb: -0.8 }), 0.004, 0.05);
}

function despues () {
  const p = pista(DUR_FIN);
  // el pitido en los oidos (acufeno) y el corazon, amortiguados: fuertes en el negro, se van al abrir los ojos
  const pit = real('1388', 2.0, 2.0 + DUR_FIN, { entra: 0.8, sale: 1.0 });
  for (let i = 0; i < pit.length; i++) {
    const t = i / SR;
    pit[i] *= t < 3 ? 1 : Math.max(0, 1 - (t - 3) / 8);
  }
  p.poner(0, pit, 0.22);
  const lat = filtro(real('0218', 0, 8.6, { tono: 0.85, entra: 0.3, sale: 0.5 }), 'lp', 260);
  for (let t = 0.2; t < 11; t += lat.length / SR - 0.05) {
    const g = t < 4 ? 0.85 : Math.max(0.2, 0.85 - (t - 4) * 0.09);
    p.poner(t, lat, g);
  }
  // los transformadores de la Estacion, que vuelven poco a poco (sin agudos al principio)
  const hum = real('0086', 0, 2.5, { entra: 0.3, sale: 0.3 });
  const zum = new Float32Array(Math.round(DUR_FIN * SR));
  const lp = new D.Biquad('lp', 300, 0.7);
  for (let i = 0; i < zum.length; i++) {
    const t = i / SR;
    if (i % 512 === 0) lp.ajustar(300 + 2500 * Math.min(1, Math.max(0, (t - 3) / 9)));
    zum[i] = lp.paso(hum[i % hum.length]) * Math.min(1, Math.max(0, (t - 1.5) / 6)) * (t > 15 ? Math.max(0, 1 - (t - 15) / 1.4) : 1);
  }
  p.poner(0, zum, 0.25);
  // al incorporarse y levantarse: el roce y un golpe sordo de la mano en el suelo
  p.poner(7.6, filtro(real('2452', 0, 1.6), 'lp', 1500), 0.3, -0.2, 0.18);
  p.poner(9.6, filtro(real('1318', 9.82, 10.2, { tono: 0.9 }), 'lp', 2500), 0.35, 0.1);
  p.poner(10.3, filtro(real('1318', 13.93, 14.3, { tono: 0.9 }), 'lp', 2500), 0.35, -0.1);
  // el titulo «NIVEL 3»: el chispazo y el zumbido de un fluorescente que se enciende
  p.poner(11.0, real('3061', 0, 1.2), 0.55, 0, 0.07);
  p.poner(11.0, real('0134', 8.45, 9.6, { entra: 0.005, sale: 0.15 }), 0.5, 0.15);
  p.poner(11.05, filtro(real('2118', 0, 4.4, { sale: 1.2 }), 'bp', 1900, 0.9), 0.3, 0);
  const verb = new D.Freeverb({ sala: 0.7, amortiguacion: 0.5, predelayMs: 20 });
  for (let i = 0; i < p.n; i++) {
    const [a, b] = verb.paso((p.L[i] + p.R[i]) * 0.5);
    p.L[i] += a * 0.35;
    p.R[i] += b * 0.35;
  }
  return D.fundidos(D.masterizar([p.L, p.R], { rmsDb: -25, picoDb: -2 }), 0.01, 0.8);
}

if (require.main === module) (async () => {
  console.log('Cinematica de la salida de la Fiesta');
  await descargar();
  for (const [nombre, f, dur] of [['cinematica_salida_fiesta', cinematica, DUR], ['cinematica_salida_fiesta_despues', despues, DUR_FIN]]) {
    const bytes = await D.guardarOgg(path.join(SONIDOS, `${nombre}.ogg`), f(), 4);
    console.log(`  ${nombre}.ogg ${dur} s, ${(bytes / 1024).toFixed(0)} KB`);
  }
  for (const v of ['anfitrion_no.wav', 'anfitrion_amenaza.wav']) {
    console.log(fs.existsSync(path.join(VOCES, v)) ? `  voz: ${v}` : `  voz: falta ${v} (va la de repuesto)`);
  }
  const fj = path.join(SONIDOS, '..', 'sounds.json');
  const texto = fs.readFileSync(fj, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  j['cinematica.salida_fiesta'] = { sounds: [{ name: 'backrooms_evento:cinematica_salida_fiesta' }] };
  j['cinematica.salida_fiesta.despues'] = { sounds: [{ name: 'backrooms_evento:cinematica_salida_fiesta_despues' }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(fj, out);
  console.log('  sounds.json');
})();
