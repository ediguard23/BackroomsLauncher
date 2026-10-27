#!/usr/bin/env node
'use strict';
/**
 * El sonido de la cinematica de la salida de la Fiesta (SalidaFiestaCliente), con los tiempos de su
 * guion. Dos archivos (Minecraft corta todo al cambiar de mundo):
 *
 *   cinematica_salida_fiesta.ogg (13.3 s, evento "cinematica.salida_fiesta"):
 *     0.5 - 3.5  golpes contra la puerta morada, lejos (al fondo del salon), cada vez mas fuertes
 *     4.0        la puerta revienta (crujido y astillas) y a 4.45 cae al suelo (golpe seco)
 *     4.6 - 6.0  los pasos del Anfitrion, que entra despacio
 *     5.4        su frase: tools/sonidos/voces/anfitrion_salida.wav («¿Ya te vas? La fiesta todavia no ha
 *                terminado.», Higgsfield, voz grave como las del Anfitrion); si no esta, anfitrion_voz_2
 *                («No te pierdas la fiesta...»)
 *     7.2        un golpe de aire (la camara vuelve al ascensor)
 *     7.8 - 8.8  el motor de las puertas del ascensor y el golpe al cerrarse
 *     9.0        el grito del Anfitrion (anfitrion_detecta), ya pegado a la puerta
 *     9.3 - 10.4 golpes contra las puertas de metal, muy cerca
 *     10.8, 11.1 los cables que se parten (latigazo metalico) y el tiron de la cabina
 *     11.1-12.9  la caida: rugido y viento que crecen, la cabina vibra
 *     12.9       el golpe contra el fondo
 *   cinematica_salida_fiesta_despues.ogg (5 s, "cinematica.salida_fiesta.despues"): el pitido en los
 *     oidos tras el golpe, el corazon y la respiracion, que se van apagando (ya en la Estacion).
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
const VOZ = path.join(__dirname, 'voces', 'anfitrion_salida.wav');
const DUR = 13.3;
const DUR_FIN = 5.0;

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

/** Un archivo de sonido (wav u ogg) a mono 44.1 kHz. */
function leer (archivo) {
  const buf = execFileSync(ffmpeg(), ['-v', 'error', '-i', archivo, '-af', 'aresample=44100', '-ac', '1', '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
  return new Float32Array(buf.buffer, buf.byteOffset, buf.length / 4).slice();
}

/** Un buffer estereo para mezclar en un momento, con ganancia y paneo. */
function pista (s) {
  const n = Math.ceil(s * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  const poner = (t0, mono, gan = 1, pan = 0) => {
    const i0 = Math.round(t0 * SR);
    for (let j = 0; j < mono.length && i0 + j < n; j++) {
      if (i0 + j < 0) continue;
      L[i0 + j] += mono[j] * gan * (1 - pan) * 0.7;
      R[i0 + j] += mono[j] * gan * (1 + pan) * 0.7;
    }
  };
  return { L, R, n, poner };
}

/** Golpe contra una puerta: el porrazo grave, el crujido de la madera y la puerta que vibra. */
function golpePuerta (rnd, fuerte) {
  const n = Math.round(0.6 * SR);
  const out = new Float32Array(n);
  const bajo = new D.Biquad('lp', 140 + fuerte * 60, 0.9);
  const madera = new D.Biquad('bp', 620 + rnd() * 120, 2.5);
  const vibra = new D.Biquad('bp', 180, 8);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const r = rnd() * 2 - 1;
    out[i] = bajo.paso(r) * 5.0 * Math.exp(-t / 0.07) * fuerte + madera.paso(r) * 1.6 * Math.exp(-t / 0.03) * fuerte
      + vibra.paso(r) * 2.0 * Math.exp(-t / 0.22) * fuerte;
  }
  return out;
}

/** La puerta que revienta: crujido largo, astillas y un chasquido. */
function revienta (rnd) {
  const n = Math.round(1.0 * SR);
  const out = new Float32Array(n);
  const cruje = new D.Biquad('bp', 900, 1.2);
  const bajo = new D.Biquad('lp', 200, 0.8);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const r = rnd() * 2 - 1;
    const granos = rnd() < 0.02 * Math.exp(-t / 0.25) ? (rnd() * 2 - 1) * 2 : 0;
    out[i] = cruje.paso(r) * 2.5 * Math.exp(-t / 0.18) + bajo.paso(r) * 6 * Math.exp(-t / 0.1) + granos;
  }
  return out;
}

/** Algo pesado que cae plano al suelo: golpe sordo y el traqueteo. */
function caeAlSuelo (rnd) {
  const n = Math.round(0.9 * SR);
  const out = new Float32Array(n);
  const lp = new D.Biquad('lp', 120, 0.8);
  const tabla = new D.Biquad('bp', 320, 3);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const r = rnd() * 2 - 1;
    out[i] = lp.paso(r) * 9 * Math.exp(-t / 0.09) + tabla.paso(r) * 2 * Math.exp(-t / 0.15) * (1 + Math.sin(t * 90) * 0.5);
  }
  return out;
}

/** Un paso pesado y blando del Anfitrion (zapatones de goma). */
function pasoAnfitrion (rnd) {
  const n = Math.round(0.3 * SR);
  const out = new Float32Array(n);
  const lp = new D.Biquad('lp', 220, 0.8);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    out[i] = lp.paso(rnd() * 2 - 1) * 4 * Math.exp(-t / 0.05) * Math.min(1, t / 0.004);
  }
  return out;
}

/** Golpe de aire (whoosh) que barre de grave a agudo. */
function aire (rnd, dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  const bp = new D.Biquad('bp', 400, 1.0);
  for (let i = 0; i < n; i++) {
    const k = i / n;
    bp.ajustar && bp.ajustar(300 + k * 2600, 1.0);
    out[i] = bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * k) * 1.4;
  }
  return out;
}

/** El motor de las puertas del ascensor (zumbido y traqueteo) y su golpe al cerrar. */
function motorPuertas (rnd, dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  const tr = new D.Biquad('bp', 1400, 4);
  let fase = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.min(1, t / 0.08) * Math.min(1, (dur - t) / 0.05);
    fase = (fase + 118 / SR) % 1;
    const zumbido = D.sierra(fase, 118 / SR) * 0.18;
    const traqueteo = tr.paso(rnd() * 2 - 1) * (0.5 + 0.5 * Math.sin(t * TAU * 21)) * 0.6;
    out[i] = (zumbido + traqueteo) * env;
  }
  return out;
}

/** Porrazo contra la chapa de las puertas del ascensor: muy cerca, metalico, con su eco de cabina. */
function golpeMetal (rnd, fuerte) {
  const n = Math.round(0.9 * SR);
  const out = new Float32Array(n);
  const bajo = new D.Biquad('lp', 160, 0.9);
  const modos = [173, 291, 433, 611, 879].map((f) => ({ f: f * (0.97 + rnd() * 0.06), d: 0.12 + rnd() * 0.25, a: 0.3 + rnd() * 0.4 }));
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    let chapa = 0;
    for (const m of modos) chapa += Math.sin(TAU * m.f * t) * m.a * Math.exp(-t / m.d);
    out[i] = (bajo.paso(rnd() * 2 - 1) * 7 * Math.exp(-t / 0.06) + chapa * 0.9 + (rnd() * 2 - 1) * Math.exp(-t / 0.008) * 1.2) * fuerte;
  }
  return out;
}

/** Un cable de acero que se parte: latigazo agudo que cae de tono y vibra. */
function cable (rnd) {
  const n = Math.round(1.2 * SR);
  const out = new Float32Array(n);
  let fase = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const f = 1900 * Math.exp(-t * 3.2) + 140;
    fase = (fase + f / SR) % 1;
    const vibra = Math.sin(TAU * fase) * Math.exp(-t / 0.35) * (1 + 0.4 * Math.sin(t * TAU * 31));
    out[i] = vibra * 0.9 + (rnd() * 2 - 1) * Math.exp(-t / 0.01) * 1.5;
  }
  return out;
}

/** La caida: rugido grave y viento que crecen, y la cabina traqueteando. */
function caida (rnd, dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  const ruge = new D.Biquad('lp', 90, 0.9);
  const viento = new D.Biquad('bp', 900, 0.8);
  const traq = new D.Biquad('bp', 2200, 3);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const k = t / dur;
    const r = rnd() * 2 - 1;
    out[i] = ruge.paso(r) * (3 + 6 * k) + viento.paso(r) * (0.4 + 1.6 * k * k) + traq.paso(r) * (0.3 + 0.5 * Math.abs(Math.sin(t * 47))) * k;
  }
  return out;
}

/** El golpe contra el fondo del hueco: enorme, con chapa retorcida. */
function choque (rnd) {
  const n = Math.round(1.6 * SR);
  const out = new Float32Array(n);
  const lp = new D.Biquad('lp', 70, 0.9);
  const medio = new D.Biquad('bp', 400, 1.0);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const r = rnd() * 2 - 1;
    out[i] = lp.paso(r) * 16 * Math.exp(-t / 0.25) + medio.paso(r) * 4 * Math.exp(-t / 0.12) + Math.sin(TAU * 38 * t) * 1.5 * Math.exp(-t / 0.4);
  }
  return out;
}

/** La voz del Anfitrion: como dentro de la cabezota de espuma (igual que voces.js#anfitrionVoz). */
function vozAnfitrion () {
  const v = fs.existsSync(VOZ) ? leer(VOZ) : leer(path.join(SONIDOS, 'anfitrion_voz_2.ogg'));
  if (!fs.existsSync(VOZ)) return v;
  const lp = new D.Biquad('lp', 1100, 0.8);
  const caja = new D.Biquad('pico', 420, 1.5, 6);
  const out = new Float32Array(v.length);
  let pico = 0;
  for (let i = 0; i < v.length; i++) {
    out[i] = caja.paso(lp.paso(v[i]));
    pico = Math.max(pico, Math.abs(out[i]));
  }
  for (let i = 0; i < out.length; i++) out[i] *= 0.8 / (pico || 1);
  return out;
}

function cinematica () {
  const rnd = D.azar(4401);
  const lejos = pista(DUR);
  const cerca = pista(DUR);
  // los golpes en la puerta morada, lejos (al fondo del salon: con mucha sala)
  [[0.5, 0.45], [1.4, 0.55], [2.4, 0.75], [3.0, 0.85], [3.5, 1.0]].forEach(([t, f]) => lejos.poner(t, golpePuerta(rnd, f), 0.9));
  lejos.poner(4.0, golpePuerta(rnd, 1.3), 1.0);
  lejos.poner(4.0, revienta(rnd), 0.9);
  lejos.poner(4.45, caeAlSuelo(rnd), 1.0);
  for (let t = 4.65, k = 0; t < 6.0; t += 0.46, k++) lejos.poner(t, pasoAnfitrion(rnd), 0.8, k % 2 ? 0.1 : -0.1);
  // la voz: un poco mas cerca (la camara se le acerca)
  cerca.poner(5.4, vozAnfitrion(), 0.75);
  // en el ascensor
  cerca.poner(7.15, aire(rnd, 0.6), 0.6);
  cerca.poner(7.8, motorPuertas(rnd, 1.0), 0.8);
  cerca.poner(8.8, golpeMetal(rnd, 0.6), 0.8);
  // los pasos del Anfitrion corriendo hacia el ascensor (por la rendija)
  for (let t = 7.65, k = 0; t < 8.9; t += 0.24, k++) lejos.poner(t, pasoAnfitrion(rnd), 0.5 + k * 0.08, k % 2 ? 0.1 : -0.1);
  try {
    cerca.poner(9.0, leer(path.join(SONIDOS, 'anfitrion_detecta.ogg')), 1.1);
  } catch (e) {
    console.log('  (sin anfitrion_detecta.ogg)');
  }
  [9.3, 9.7, 10.1, 10.4].forEach((t, k) => cerca.poner(t, golpeMetal(rnd, 0.8 + k * 0.1), 1.0, k % 2 ? 0.25 : -0.25));
  cerca.poner(10.8, cable(rnd), 0.8, -0.3);
  cerca.poner(10.8, golpeMetal(rnd, 0.9), 0.8);
  cerca.poner(11.1, cable(rnd), 0.9, 0.3);
  cerca.poner(11.1, caida(rnd, 1.85), 0.9);
  cerca.poner(12.9, choque(rnd), 1.2);
  // la sala grande del salon para lo de lejos; la cabina, poca
  const salon = new D.Freeverb({ sala: 0.9, amortiguacion: 0.4, predelayMs: 40 });
  const cabina = new D.Freeverb({ sala: 0.35, amortiguacion: 0.6, predelayMs: 6 });
  const L = new Float32Array(lejos.n);
  const R = new Float32Array(lejos.n);
  const lpLejos = new D.Biquad('lp', 2600, 0.7);
  for (let i = 0; i < lejos.n; i++) {
    const m = lpLejos.paso((lejos.L[i] + lejos.R[i]) * 0.5);
    const [a, b] = salon.paso(m);
    const [c, d] = cabina.paso((cerca.L[i] + cerca.R[i]) * 0.5);
    L[i] = lejos.L[i] * 0.45 + a * 0.55 + cerca.L[i] + c * 0.25;
    R[i] = lejos.R[i] * 0.45 + b * 0.55 + cerca.R[i] + d * 0.25;
  }
  // tras el choque, nada (corta en seco)
  for (let i = Math.round((12.9 + 1.4) * SR); i < L.length; i++) {
    L[i] = 0;
    R[i] = 0;
  }
  return D.fundidos(D.masterizar([L, R], { rmsDb: -19, picoDb: -1 }), 0.004, 0.05);
}

function despues () {
  const rnd = D.azar(4402);
  const p = pista(DUR_FIN);
  // el pitido en los oidos, que se apaga
  const pit = new Float32Array(Math.round(DUR_FIN * SR));
  for (let i = 0; i < pit.length; i++) {
    const t = i / SR;
    pit[i] = Math.sin(TAU * 3870 * t) * 0.16 * Math.exp(-t / 1.8) * Math.min(1, t / 0.05);
  }
  p.poner(0, pit, 1.0);
  // el corazon, deprisa y bajando, y la respiracion
  const lat = (fuerte) => {
    const n = Math.round(0.18 * SR);
    const out = new Float32Array(n);
    const lp = new D.Biquad('lp', 70, 0.9);
    for (let i = 0; i < n; i++) out[i] = lp.paso(rnd() * 2 - 1) * 9 * Math.exp(-i / SR / 0.05) * fuerte;
    return out;
  };
  for (let t = 0.3, k = 0; t < DUR_FIN - 0.4; k++) {
    const f = Math.max(0.2, 1 - t / DUR_FIN);
    p.poner(t, lat(f), 0.8);
    p.poner(t + 0.17, lat(f * 0.7), 0.8);
    t += 0.55 + t * 0.06;
  }
  const resp = new D.Biquad('bp', 900, 0.7);
  const r = new Float32Array(Math.round(DUR_FIN * SR));
  for (let i = 0; i < r.length; i++) {
    const t = i / SR;
    const ciclo = (t % 1.4) / 1.4;
    r[i] = resp.paso(rnd() * 2 - 1) * Math.pow(Math.sin(Math.PI * ciclo), 2) * 0.35 * Math.exp(-t / 3.5);
  }
  p.poner(0.6, r, 0.8);
  return D.fundidos(D.masterizar([p.L, p.R], { rmsDb: -24, picoDb: -2 }), 0.01, 1.0);
}

if (require.main === module) (async () => {
  console.log('Cinematica de la salida de la Fiesta');
  for (const [nombre, buf, dur] of [['cinematica_salida_fiesta', cinematica(), DUR], ['cinematica_salida_fiesta_despues', despues(), DUR_FIN]]) {
    const bytes = await D.guardarOgg(path.join(SONIDOS, `${nombre}.ogg`), buf, 4);
    console.log(`  ${nombre}.ogg ${dur} s, ${(bytes / 1024).toFixed(0)} KB`);
  }
  console.log(fs.existsSync(VOZ) ? '  voz: anfitrion_salida.wav' : '  voz: anfitrion_voz_2 (falta anfitrion_salida.wav)');
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
