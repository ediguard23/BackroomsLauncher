#!/usr/bin/env node
'use strict';
/**
 * El sonido del prologo del /start (PantallaPrologo, 26 s), que va justo antes de la
 * cinematica del ascensor: la sala de reuniones (el zumbido del aire, el reloj de pared),
 * el papel que te acercan por la mesa, el boligrafo, la firma, el papel que se llevan, la
 * silla, los pasos por el pasillo y la campanilla y las puertas del ascensor. Sin voces
 * (lo que se dice va en subtitulos).
 *
 *   node tools/sonidos/prologo.js
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { mono, poner, pico, guardarMono, reverb, SALIDA } = E;

const DUR = 26.0;

function prologo () {
  const rnd = D.azar(5100);
  const buf = mono(DUR);
  // el aire acondicionado de la sala (hasta que se sale al pasillo) y un zumbido electrico
  const lp = new D.Biquad('lp', 380, 0.7);
  poner(buf, 0, 20.5, (t) => lp.paso(rnd() * 2 - 1) * 0.5 * Math.min(1, t / 1.5) * Math.min(1, (20.5 - t) / 0.8)
    + Math.sin(TAU * 120 * t) * 0.012);
  // el reloj de pared: tic cada segundo
  for (let s = 1; s < 20; s++) {
    const bp = new D.Biquad('bp', s % 2 ? 2600 : 2300, 6);
    poner(buf, s, 0.04, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 180) * 0.5);
  }
  // el papel que se arrastra por la mesa hacia ti
  const papel = (t0, dur, g) => {
    const bp = new D.Biquad('bp', 3200, 0.8);
    poner(buf, t0, dur, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / dur) * g * (0.7 + 0.3 * Math.sin(TAU * 7 * t)));
  };
  papel(1.6, 1.4, 0.8);
  // el boligrafo que se deja en la mesa
  poner(buf, 3.4, 0.06, (t) => Math.sin(TAU * 1800 * t) * Math.exp(-t * 90) * 0.5);
  // pasar la hoja / ajustarla
  papel(6.8, 0.5, 0.5);
  // la firma: el roce de la punta en el papel, a tirones
  for (let t0 = 12.7; t0 < 15.0; t0 += 0.09 + rnd() * 0.12) {
    const bp = new D.Biquad('bp', 4200 + rnd() * 2000, 2);
    const d = 0.05 + rnd() * 0.1;
    poner(buf, t0, d, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / d) * 0.55);
  }
  // se lleva el papel; la silla que se arrastra al levantarte
  papel(16.4, 0.9, 0.8);
  const lpS = new D.Biquad('lp', 900, 1.2);
  poner(buf, 19.2, 0.7, (t) => (lpS.paso(rnd() * 2 - 1) + Math.sin(TAU * (190 + 40 * Math.sin(TAU * 9 * t)) * t) * 0.3) * Math.sin(Math.PI * t / 0.7) * 0.7);
  // los pasos por el pasillo (suelo duro)
  for (let k = 0; k < 8; k++) {
    const t0 = 20.3 + k * 0.52;
    poner(buf, t0, 0.15, (t) => (Math.sin(TAU * (120 + 30 * (k % 2)) * t) * 0.7 + (rnd() * 2 - 1) * 0.3) * Math.exp(-t / 0.03) * 0.8);
  }
  // la campanilla del ascensor y las puertas que se abren
  poner(buf, 24.0, 1.6, (t) => (Math.sin(TAU * 1318 * t) * 0.5 + Math.sin(TAU * 2636 * t) * 0.15) * Math.exp(-t / 0.45) * 0.6);
  const bpP = new D.Biquad('bp', 500, 1.5);
  poner(buf, 24.3, 1.4, (t) => bpP.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.4) * 0.6);
  return pico(reverb(buf, 0.5, 0.18), -2);
}

if (require.main === module) (async () => {
  console.log('Prologo del /start');
  await guardarMono('cinematica_prologo', prologo());
  const f = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(f, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  j['cinematica.prologo'] = { sounds: [{ name: 'backrooms_evento:cinematica_prologo', stream: true }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(f, out);
  console.log('  sounds.json');
})();
