#!/usr/bin/env node
'use strict';
/**
 * La musica de la recepcion del Proyecto Umbral (donde se espera el /start): muy tenue, de
 * sala de espera, para que no haya silencio absoluto. Un colchon de cuerdas sinteticas muy
 * suave, un piano electrico que toca acordes abiertos sin prisa y alguna nota de vibrafono;
 * a 72 por minuto, en bucle sin costura (cerrarBucle). Estereo (no se situa en el mundo).
 * Sale a evento/.../sounds/recepcion_musica.ogg (evento "recepcion.musica").
 *
 *   node tools/sonidos/recepcion.js
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const midi = (n) => 440 * Math.pow(2, (n - 69) / 12);
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');

const BEAT = 60 / 72;
// 16 compases de 4: Fmaj9, Em7, Dm9, G6/9 (dos vueltas, la segunda con otro color)
const ACORDES = [
  [41, [57, 60, 64, 67]], [40, [55, 59, 62, 67]], [38, [53, 57, 60, 64]], [43, [55, 59, 62, 64]],
  [41, [57, 60, 64, 69]], [45, [55, 60, 64, 67]], [38, [53, 57, 62, 65]], [43, [53, 59, 62, 64]]
];
const COMPASES = 16;
const DUR = COMPASES * 4 * BEAT; // 53,3 s
const COLA = 4.0;

function musica () {
  const n = Math.ceil((DUR + COLA) * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  const sumar = (t0, dur, fn, pan = 0) => {
    const i0 = Math.round(t0 * SR);
    const m = Math.round(dur * SR);
    const pl = Math.cos((pan + 1) * Math.PI / 4);
    const pr = Math.sin((pan + 1) * Math.PI / 4);
    for (let k = 0; k < m && i0 + k < n; k++) {
      const v = fn(k / SR);
      L[i0 + k] += v * pl;
      R[i0 + k] += v * pr;
    }
  };
  const rnd = D.azar(7212);
  // el colchon: sierras suaves desafinadas, muy filtradas, que entran y salen con cada acorde
  for (let c = 0; c < COMPASES + 1; c++) {
    const [, voces] = ACORDES[c % ACORDES.length];
    const t0 = c * 4 * BEAT;
    const d = 4 * BEAT + 1.6;
    for (const [i, nota] of voces.entries()) {
      const f = midi(nota - 12);
      const lp = new D.Biquad('lp', 900, 0.6);
      let fase1 = rnd();
      let fase2 = rnd();
      sumar(t0, d, (t) => {
        fase1 += f * 1.003 / SR;
        fase2 += f * 0.997 / SR;
        const s = D.sierra(fase1 % 1, f / SR) + D.sierra(fase2 % 1, f / SR);
        const env = Math.min(1, t / 1.4) * Math.min(1, (d - t) / 1.4);
        return lp.paso(s) * env * 0.035;
      }, (i % 2 ? 0.4 : -0.4));
    }
  }
  // piano electrico (FM tipo Rhodes): el acorde arpegiado al principio del compas y un eco a mitad
  const rhodes = (f, d) => (t) => {
    const idx = 1.2 * Math.exp(-t * 6) + 0.2;
    const s = Math.sin(TAU * f * t + idx * Math.sin(TAU * f * t));
    return s * Math.exp(-t * 0.9) * Math.min(1, t / 0.005) * Math.min(1, (d - t) / 0.3) * 0.075 * (0.85 + 0.15 * Math.sin(TAU * 4.5 * t));
  };
  for (let c = 0; c < COMPASES; c++) {
    const [raiz, voces] = ACORDES[c % ACORDES.length];
    const t0 = c * 4 * BEAT;
    voces.forEach((nota, i) => sumar(t0 + i * 0.09, 3.6 * BEAT, rhodes(midi(nota), 3.6 * BEAT), -0.3 + i * 0.2));
    if (c % 2 === 1) {
      voces.slice(1).forEach((nota, i) => sumar(t0 + 2.5 * BEAT + i * 0.12, 1.5 * BEAT, rhodes(midi(nota + 12), 1.5 * BEAT), 0.3 - i * 0.2));
    }
    // el bajo, redondo y bajito
    const fb = midi(raiz);
    sumar(t0, 3.8 * BEAT, (t) => Math.sin(TAU * fb * t) * Math.exp(-t * 0.6) * Math.min(1, t / 0.02) * Math.min(1, (3.8 * BEAT - t) / 0.2) * 0.11);
  }
  // alguna nota suelta de vibrafono por encima
  const notas = [[2, 76], [6.5, 79], [11, 74], [18, 72], [22.5, 76], [27, 81], [34, 79], [38.5, 76], [43, 74], [50, 72], [54.5, 76], [59, 79]];
  for (const [b, nota] of notas) {
    const f = midi(nota);
    sumar(b * BEAT, 3.0, (t) => (Math.sin(TAU * f * t) + 0.15 * Math.sin(TAU * f * 4 * t) * Math.exp(-t * 6))
      * (0.8 + 0.2 * Math.sin(TAU * 4.8 * t)) * Math.exp(-t * 1.4) * Math.min(1, t / 0.004) * 0.05, (rnd() - 0.5) * 0.8);
  }
  // un poco de sala
  const rev = new D.Freeverb({ sala: 0.82, amortiguacion: 0.45, predelayMs: 20 });
  for (let i = 0; i < n; i++) {
    const [a, b] = rev.paso((L[i] + R[i]) * 0.5);
    L[i] += a * 0.45;
    R[i] += b * 0.45;
  }
  return [L, R];
}

if (require.main === module) (async () => {
  console.log('Musica de la recepcion');
  const bucle = D.cerrarBucle(musica(), DUR);
  const final = D.masterizar(bucle, { rmsDb: -26, picoDb: -6 });
  const f = path.join(SALIDA, 'recepcion_musica.ogg');
  const bytes = await D.guardarOgg(f, final, 4);
  console.log(`  recepcion_musica.ogg ${DUR.toFixed(1)} s, ${(bytes / 1024).toFixed(0)} KB`);
  const fj = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(fj, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  j['recepcion.musica'] = { sounds: [{ name: 'backrooms_evento:recepcion_musica' }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(fj, out);
  console.log('  sounds.json');
})();
