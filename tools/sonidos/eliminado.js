#!/usr/bin/env node
'use strict';
/**
 * El sonido de una eliminacion (lo oyen todos a la vez cuando alguien cae):
 *
 *   node tools/sonidos/eliminado.js
 *
 * Primero el monitor de constantes del traje: dos pitidos y la linea plana.
 * Luego se corta la senal: estatica que tartamudea, un tono que se hunde como
 * una cinta que se para, un golpe grave y un acorde disonante que se apaga
 * con reverb. Va en estereo y sin posicion (es un aviso, no un ruido del mundo).
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento');
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };

function eliminado () {
  const dur = 3.6;
  const rnd = D.azar(4041);
  const buf = D.estereo(dur);
  const rev = new D.Freeverb({ sala: 0.9, amortiguacion: 0.4, predelayMs: 20 });
  const bpEst = [new D.Biquad('bp', 2600, 0.7), new D.Biquad('bp', 3100, 0.7)];
  const lpCinta = new D.Biquad('lp', 2400, 0.8);
  const lpGolpe = new D.Biquad('lp', 160, 0.7);
  let faseCinta = 0;
  let faseSub = 0;
  // acorde disonante: La2, Sib2 y Mib3 (tritono), un poco desafinados
  const acorde = [110, 116.54 * 1.003, 155.56 * 0.997];
  const fases = acorde.map(() => rnd());

  for (let k = 0; k < buf[0].length; k++) {
    const t = k / SR;
    let L = 0;
    let R = 0;

    // estatica cortada a trozos durante el primer tercio de segundo
    if (t < 0.42) {
      const trozo = Math.floor(t * 38);
      const abierto = (trozo * 7919) % 5 !== 0;
      const env = abierto ? 1 - t / 0.42 : 0;
      L += bpEst[0].paso((rnd() * 2 - 1) * env) * 0.9;
      R += bpEst[1].paso((rnd() * 2 - 1) * env) * 0.9;
    } else {
      bpEst[0].paso(0);
      bpEst[1].paso(0);
    }

    // la cinta se para: un tono que cae de 880 a 70 Hz con temblor
    if (t < 0.75) {
      const f = 880 * Math.pow(70 / 880, Math.pow(t / 0.75, 0.7)) * (1 + 0.012 * Math.sin(TAU * 9 * t));
      faseCinta += f / SR;
      const s = (faseCinta % 1) * 2 - 1; // sierra
      const env = suave(0, 0.02, t) * (1 - suave(0.5, 0.75, t));
      const c = lpCinta.paso(s) * env * 0.35;
      L += c;
      R += c;
    }

    // golpe grave al cortarse
    if (t >= 0.42) {
      const d = t - 0.42;
      const f = 34 + 26 * Math.exp(-d / 0.25);
      faseSub += f / SR;
      const sub = Math.tanh(Math.sin(TAU * faseSub) * 1.8) * Math.exp(-d / 0.9) * 0.85;
      const ruido = lpGolpe.paso(d < 0.03 ? rnd() * 2 - 1 : 0) * 2.2;
      L += sub + ruido;
      R += sub + ruido;
    }

    // el acorde disonante que queda flotando
    if (t >= 0.5) {
      const d = t - 0.5;
      const env = suave(0, 0.35, d) * (1 - suave(1.6, 3.0, d));
      let a = 0;
      acorde.forEach((f, i) => {
        fases[i] = (fases[i] + f / SR) % 1;
        const ph = TAU * fases[i];
        a += (Math.sin(ph) + 0.35 * Math.sin(2 * ph) + 0.12 * Math.sin(3 * ph)) * (i === 2 ? 0.7 : 1);
      });
      L += a * env * 0.09;
      R += a * env * 0.09 * (1 + 0.1 * Math.sin(TAU * 0.7 * d));
    }

    const [wl, wr] = rev.paso((L + R) * 0.5);
    buf[0][k] = L + wl * 0.5;
    buf[1][k] = R + wr * 0.5;
  }
  let pico = 0;
  for (let k = 0; k < buf[0].length; k++) pico = Math.max(pico, Math.abs(buf[0][k]), Math.abs(buf[1][k]));
  const g = Math.pow(10, -3 / 20) / (pico || 1);
  for (let k = 0; k < buf[0].length; k++) { buf[0][k] *= g; buf[1][k] *= g; }
  return D.fundidos(buf, 0.001, 0.4);
}

/** El monitor: pitido, pitido... y la linea plana hasta que se corta la senal. */
function monitor (corte) {
  const n = Math.round(corte * SR);
  const out = new Float32Array(n);
  const tono = (t) => Math.sin(TAU * 1000 * t) * 0.7 + Math.sin(TAU * 2000 * t) * 0.12;
  for (let k = 0; k < n; k++) {
    const t = k / SR;
    let v = 0;
    for (const p of [0.0, 0.5]) {
      if (t >= p && t < p + 0.11) v = tono(t) * suave(p, p + 0.004, t) * (1 - suave(p + 0.1, p + 0.11, t));
    }
    if (t >= 1.0) v = tono(t) * suave(1.0, 1.006, t); // la linea plana
    out[k] = v * 0.32;
  }
  return out;
}

/** El aviso entero: el monitor y, a los 2 s, el corte de la senal de siempre. */
function completo () {
  const CORTE = 2.0;
  const m = monitor(CORTE);
  const resto = eliminado();
  const n = m.length + resto[0].length;
  const out = [new Float32Array(n), new Float32Array(n)];
  // el pitido suena en un altavoz pequeno: algo de sala y un poco a la izquierda
  const rv = new D.Freeverb({ sala: 0.5, amortiguacion: 0.5 });
  for (let k = 0; k < m.length; k++) {
    const [a, b] = rv.paso(m[k]);
    out[0][k] = m[k] * 0.9 + a * 0.3;
    out[1][k] = m[k] * 0.7 + b * 0.3;
  }
  for (let k = 0; k < resto[0].length; k++) {
    out[0][m.length + k] += resto[0][k];
    out[1][m.length + k] += resto[1][k];
  }
  return out;
}

(async () => {
  const audio = completo();
  const bytes = await D.guardarOgg(path.join(ASSETS, 'sounds', 'eliminado.ogg'), audio, 5);
  console.log(`  eliminado.ogg  ${(audio[0].length / SR).toFixed(2)} s  ${(bytes / 1024).toFixed(0)} KB`);
  // sounds.json: solo se anade la entrada, lo demas se respeta
  const f = path.join(ASSETS, 'sounds.json');
  const j = JSON.parse(fs.readFileSync(f, 'utf8'));
  j.eliminado = { sounds: [{ name: 'backrooms_evento:eliminado' }] };
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
})();
