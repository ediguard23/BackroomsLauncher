#!/usr/bin/env node
'use strict';
/**
 * El agua de la inundacion de las Piscinas (InundacionCliente): un rugido de agua que entra a
 * chorro por las rejillas y llena las salas, con borboteos y golpes de agua; 16 s en bucle
 * sin costura. Estereo (no se situa: suena alrededor).
 * Sale a evento/.../sounds/piscinas_inundacion.ogg (evento "piscinas.inundacion").
 *
 *   node tools/sonidos/inundacion.js
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
const DUR = 16.0;
const COLA = 2.0;

function agua () {
  const n = Math.ceil((DUR + COLA) * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  const rnd = D.azar(3737);
  // el rugido: ruido marron y rosa por dos filtros que respiran, distinto a cada lado
  for (const [buf, semilla, fase] of [[L, 11, 0], [R, 12, 1.7]]) {
    const r2 = D.azar(semilla);
    const marron = D.marron(r2);
    const rosa = D.rosa(r2);
    const lp = new D.Biquad('lp', 700, 0.7);
    const bp = new D.Biquad('bp', 1800, 0.8);
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      if (i % 64 === 0) bp.ajustar(1400 + 600 * Math.sin(TAU * 0.13 * t + fase) + 300 * Math.sin(TAU * 0.41 * t), 0.8);
      const ola = 0.75 + 0.25 * Math.sin(TAU * 0.09 * t + fase) * Math.sin(TAU * 0.23 * t);
      buf[i] += (lp.paso(marron()) * 1.6 + bp.paso(rosa()) * 0.5) * ola * 0.5;
    }
  }
  // borboteos: burbujas que suben (senos que suben de tono y se apagan)
  for (let k = 0; k < 140; k++) {
    const t0 = rnd() * DUR;
    const f0 = 300 + rnd() * 900;
    const d = 0.03 + rnd() * 0.07;
    const pan = rnd() * 2 - 1;
    const i0 = Math.round(t0 * SR);
    let fase = 0;
    for (let j = 0; j < d * SR && i0 + j < n; j++) {
      const t = j / SR;
      fase += (f0 * (1 + t * 9)) / SR;
      const v = Math.sin(TAU * fase) * Math.exp(-t / (d * 0.4)) * 0.06;
      L[i0 + j] += v * (1 - pan) * 0.5;
      R[i0 + j] += v * (1 + pan) * 0.5;
    }
  }
  // golpes de agua contra las paredes, sordos
  for (let k = 0; k < 9; k++) {
    const t0 = rnd() * DUR;
    const lp = new D.Biquad('lp', 400, 0.7);
    const i0 = Math.round(t0 * SR);
    const pan = rnd() * 2 - 1;
    for (let j = 0; j < 0.9 * SR && i0 + j < n; j++) {
      const t = j / SR;
      const v = lp.paso(rnd() * 2 - 1) * Math.min(1, t / 0.02) * Math.exp(-t / 0.25) * 0.5;
      L[i0 + j] += v * (1 - pan) * 0.5;
      R[i0 + j] += v * (1 + pan) * 0.5;
    }
  }
  const rev = new D.Freeverb({ sala: 0.9, amortiguacion: 0.3, predelayMs: 25 });
  for (let i = 0; i < n; i++) {
    const [a, b] = rev.paso((L[i] + R[i]) * 0.5);
    L[i] += a * 0.6;
    R[i] += b * 0.6;
  }
  return [L, R];
}

if (require.main === module) (async () => {
  console.log('Agua de la inundacion');
  const final = D.masterizar(D.cerrarBucle(agua(), DUR), { rmsDb: -18, picoDb: -2 });
  const bytes = await D.guardarOgg(path.join(SALIDA, 'piscinas_inundacion.ogg'), final, 4);
  console.log(`  piscinas_inundacion.ogg ${DUR} s, ${(bytes / 1024).toFixed(0)} KB`);
  const fj = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(fj, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  j['piscinas.inundacion'] = { sounds: [{ name: 'backrooms_evento:piscinas_inundacion' }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(fj, out);
  console.log('  sounds.json');
})();
