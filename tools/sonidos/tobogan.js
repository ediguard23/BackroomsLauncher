#!/usr/bin/env node
'use strict';
/**
 * El sonido de la cinematica del tobogan, de las Piscinas a la Fiesta (ToboganCliente), con los
 * tiempos de su guion. Dos archivos (Minecraft corta todo al cambiar de mundo):
 *
 *   cinematica_tobogan.ogg (11.4 s, evento "cinematica.tobogan"):
 *     0 - 1.1   pasos por la rejilla de metal del pasillo
 *     0.8, 1.0  las luces parpadean (chasquido y zumbido electrico)
 *     1.2       la sirena de contencion (sube una vez por segundo, como el rojo de las luces)
 *     1.6       la megafonia (voz de Higgsfield en tools/sonidos/voces/megafonia_contencion.wav;
 *               si no esta, solo la sirena)
 *     2.5       retumbar grave; 3.0 entra el agua por la puerta (rugido que crece, golpes y
 *               crujidos del metal)
 *     7.2 - 8.2 corre (pasos rapidos y la respiracion)
 *     8.3 - 8.9 se agacha, se mete por la boca y se sienta en el tobogan (golpe en el plastico)
 *     8.9       cae por el tobogan: el roce y el aire, cada vez mas deprisa; la sirena se queda atras
 *   cinematica_tobogan_despues.ogg (3 s, "cinematica.tobogan.despues"): el final del tobogan y la
 *     caida en la Fiesta (un golpe blando), al llegar.
 *
 *   node tools/sonidos/tobogan.js
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SONIDOS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'sounds');
const VOZ = path.join(__dirname, 'voces', 'megafonia_contencion.wav');
const DUR = 11.4;
const DUR_FIN = 3.0;

function ffmpeg () {
  if (process.env.FFMPEG) return process.env.FFMPEG;
  const medal = path.join(process.env.LOCALAPPDATA || '', 'Medal');
  if (fs.existsSync(medal)) {
    for (const d of fs.readdirSync(medal).filter((n) => n.startsWith('recorder')).sort().reverse()) {
      const f = path.join(medal, d, 'ffmpeg7.exe');
      if (fs.existsSync(f)) return f;
    }
  }
  return 'ffmpeg';
}

function leerVoz () {
  if (!fs.existsSync(VOZ)) return null;
  const buf = execFileSync(ffmpeg(), ['-v', 'error', '-i', VOZ, '-af', 'aresample=44100', '-ac', '1', '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
  return new Float32Array(buf.buffer, buf.byteOffset, buf.length / 4).slice();
}

/** Un buffer estereo con utilidades para mezclar en un momento y con paneo. */
function pista (s) {
  const n = Math.ceil(s * SR);
  const L = new Float32Array(n);
  const R = new Float32Array(n);
  const poner = (t0, mono, gan = 1, pan = 0) => {
    const i0 = Math.round(t0 * SR);
    for (let j = 0; j < mono.length && i0 + j < n; j++) {
      if (i0 + j < 0) continue;
      L[i0 + j] += mono[j] * gan * (1 - pan) * 0.5 * 1.4;
      R[i0 + j] += mono[j] * gan * (1 + pan) * 0.5 * 1.4;
    }
  };
  return { L, R, n, poner };
}

/** Un paso en la rejilla de metal: golpe sordo, el chasquido de la chapa y su vibracion. */
function paso (rnd, fuerte = 1) {
  const n = Math.round(0.35 * SR);
  const out = new Float32Array(n);
  const bp = new D.Biquad('bp', 2600 + rnd() * 600, 1.2);
  const anillo = new D.Biquad('bp', 3300 + rnd() * 400, 14);
  const bajo = new D.Biquad('lp', 160, 0.7);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const r = rnd() * 2 - 1;
    const golpe = bajo.paso(r) * Math.exp(-t / 0.03) * 2.2;
    const chapa = bp.paso(r) * Math.exp(-t / 0.012) * 0.9;
    const vib = anillo.paso(r * Math.exp(-t / 0.004)) * Math.exp(-t / 0.08) * 6;
    out[i] = (golpe + chapa + vib) * 0.5 * fuerte;
  }
  return out;
}

/** El chasquido de un rele y el zumbido de un tubo que falla. */
function parpadeo (rnd, dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const zumbido = Math.sign(Math.sin(TAU * 100 * t)) * 0.06 + Math.sin(TAU * 200 * t) * 0.04;
    const chispa = rnd() < 0.02 ? (rnd() * 2 - 1) * 0.5 : 0;
    const clic = t < 0.006 ? (rnd() * 2 - 1) * 0.8 : 0;
    out[i] = zumbido + chispa + clic;
  }
  return out;
}

/** La sirena de contencion: un barrido de 450 a 950 Hz cada segundo, aspero, en una sala grande. */
function sirena (dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  const bp = new D.Biquad('bp', 900, 0.7);
  let fase = 0;
  let fase2 = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const c = t % 1.0;
    const f = c < 0.78 ? 450 + 500 * Math.pow(c / 0.78, 0.8) : 950 - 500 * ((c - 0.78) / 0.22);
    fase = (fase + f / SR) % 1;
    fase2 = (fase2 + (f * 1.5) / SR) % 1;
    const env = c < 0.05 ? c / 0.05 : c > 0.9 ? Math.max(0, (1 - c) / 0.1) : 1;
    const v = D.sierra(fase, f / SR) * 0.6 + D.sierra(fase2, (f * 1.5) / SR) * 0.25;
    out[i] = Math.tanh(bp.paso(v) * 2.5) * env * Math.min(1, t / 0.3);
  }
  return out;
}

/** Altavoz de techo viejo y roto (como la megafonia de la alarma de voces.js). */
function altavoz (x) {
  const hp = new D.Biquad('hp', 450, 0.8);
  const lp = new D.Biquad('lp', 2900, 0.9);
  const caja = new D.Biquad('pico', 1700, 2, 5);
  const out = new Float32Array(x.length);
  for (let i = 0; i < x.length; i++) out[i] = Math.tanh(caja.paso(lp.paso(hp.paso(x[i]))) * 3.2);
  return out;
}

/** Respiracion de quien corre: inspira y expira, cada 0,35 s. */
function respiracion (rnd, dur) {
  const n = Math.round(dur * SR);
  const out = new Float32Array(n);
  const bp = new D.Biquad('bp', 1300, 0.9);
  const bp2 = new D.Biquad('bp', 600, 1.2);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const c = (t % 0.35) / 0.35;
    const env = Math.sin(Math.PI * Math.min(1, c / 0.8)) * (Math.floor(t / 0.35) % 2 === 0 ? 0.7 : 1);
    const r = rnd() * 2 - 1;
    out[i] = (bp.paso(r) * 0.9 + bp2.paso(r) * 0.5) * env * 0.35;
  }
  return out;
}

function cinematica () {
  const p = pista(DUR);
  const rnd = D.azar(4242);
  // pasos andando y luego corriendo
  for (const t of [0.12, 0.6, 1.08]) p.poner(t, paso(rnd, 0.8), 1, rnd() * 0.3 - 0.15);
  for (let k = 0; k < 4; k++) p.poner(7.25 + k * 0.25, paso(rnd, 1.25), 1, k % 2 ? 0.2 : -0.2);
  p.poner(7.2, respiracion(rnd, 1.3), 0.9);
  // el parpadeo de las luces
  p.poner(0.8, parpadeo(rnd, 0.1), 0.7, 0.3);
  p.poner(1.0, parpadeo(rnd, 0.12), 0.8, -0.2);
  // la sirena, lejos y por todo (con reverb larga); se va quedando atras al caer
  const sir = sirena(DUR - 1.2);
  const rvS = new D.Freeverb({ sala: 0.94, amortiguacion: 0.3, predelayMs: 40 });
  const lpS = new D.Biquad('lp', 6000, 0.7);
  const i0 = Math.round(1.2 * SR);
  for (let j = 0; j < sir.length && i0 + j < p.n; j++) {
    const t = 1.2 + j / SR;
    if (j % 256 === 0) lpS.ajustar(t < 8.9 ? 6000 : Math.max(500, 6000 - (t - 8.9) * 3000), 0.7);
    const caer = t < 8.9 ? 1 : Math.max(0.15, 1 - (t - 8.9) / 2);
    const v = lpS.paso(sir[j]) * 0.22 * caer;
    const [a, b] = rvS.paso(v);
    p.L[i0 + j] += v * 0.6 + a * 0.7;
    p.R[i0 + j] += v * 0.6 + b * 0.7;
  }
  // la megafonia
  const voz = leerVoz();
  if (voz) {
    const v = altavoz(voz);
    const rvV = new D.Freeverb({ sala: 0.9, amortiguacion: 0.4, predelayMs: 60 });
    const iv = Math.round(1.6 * SR);
    for (let j = 0; j < v.length + SR && iv + j < p.n; j++) {
      const x = j < v.length ? v[j] * 0.55 : 0;
      const [a, b] = rvV.paso(x);
      p.L[iv + j] += x * 0.7 + a * 0.6;
      p.R[iv + j] += x * 0.7 + b * 0.6;
    }
  } else {
    console.log('  (sin megafonia_contencion.wav: solo la sirena)');
  }
  // el retumbar y el agua que entra
  const marron = D.marron(D.azar(91));
  const rosa = D.rosa(D.azar(92));
  const lpA = new D.Biquad('lp', 200, 0.7);
  const bpA = new D.Biquad('bp', 900, 0.7);
  const sub = new D.Biquad('lp', 55, 0.8);
  for (let i = Math.round(2.5 * SR); i < p.n; i++) {
    const t = i / SR;
    const ret = Math.min(1, (t - 2.5) / 1.5) * (t < 8.9 ? 1 : Math.max(0.3, 1 - (t - 8.9) / 1.5));
    const r = sub.paso(marron()) * 2.5 * ret;
    let agua = 0;
    if (t > 3.0) {
      const k = Math.min(1, (t - 3.0) / 3.0);
      if (i % 128 === 0) lpA.ajustar(200 + 900 * k, 0.7);
      const lejos = t < 8.9 ? 1 : Math.max(0.1, 1 - (t - 8.9) / 1.2);
      agua = (lpA.paso(marron()) * 1.4 + bpA.paso(rosa()) * 0.35) * k * 0.6 * lejos;
    }
    p.L[i] += r * 0.5 + agua;
    p.R[i] += r * 0.5 + agua * 0.9;
  }
  // crujidos del metal de la boveda (gemidos graves que bajan de tono)
  for (const [t0, f0] of [[3.1, 70], [5.2, 55], [6.4, 82]]) {
    const n = Math.round(1.3 * SR);
    const out = new Float32Array(n);
    let fase = 0;
    const bp = new D.Biquad('bp', f0 * 3, 4);
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      fase = (fase + (f0 * (1 - 0.3 * t / 1.3)) / SR) % 1;
      out[i] = bp.paso(D.sierra(fase, f0 / SR) + (rnd() - 0.5) * 0.4) * Math.sin(Math.PI * t / 1.3) * (0.7 + 0.3 * Math.sin(TAU * 11 * t)) * 0.8;
    }
    p.poner(t0, out, 0.9, rnd() - 0.5);
  }
  // se agacha y se mete: roce de ropa, y el golpe al sentarse en el tobogan (plastico hueco)
  {
    const n = Math.round(0.5 * SR);
    const roce = new Float32Array(n);
    const bp = new D.Biquad('bp', 2200, 0.8);
    for (let i = 0; i < n; i++) roce[i] = bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * i / n) * 0.3;
    p.poner(8.3, roce, 1);
    const g = Math.round(0.6 * SR);
    const golpe = new Float32Array(g);
    const lp = new D.Biquad('lp', 260, 0.9);
    const hueco = new D.Biquad('bp', 420, 6);
    for (let i = 0; i < g; i++) {
      const t = i / SR;
      const r = rnd() * 2 - 1;
      golpe[i] = (lp.paso(r) * 2.5 * Math.exp(-t / 0.05) + hueco.paso(r * Math.exp(-t / 0.01)) * 5 * Math.exp(-t / 0.12)) * 0.6;
    }
    p.poner(8.75, golpe, 1);
  }
  // el tobogan: roce que sube de tono y el aire, cada vez mas fuerte
  {
    const bp = new D.Biquad('bp', 500, 1.1);
    const aire = new D.Biquad('hp', 1500, 0.7);
    const zumb = new D.Biquad('lp', 120, 0.7);
    const r2 = D.azar(77);
    for (let i = Math.round(8.85 * SR); i < p.n; i++) {
      const t = i / SR;
      const k = Math.min(1, (t - 8.85) / 1.6);
      if (i % 128 === 0) bp.ajustar(500 + 1300 * k, 1.1);
      const r = r2() * 2 - 1;
      const v = (bp.paso(r) * 0.6 + aire.paso(r) * 0.25 * k + zumb.paso(r) * 2 * (0.5 + 0.5 * Math.sin(TAU * 7 * t))) * (0.3 + 0.7 * k) * 0.55;
      p.L[i] += v * (1 + 0.2 * Math.sin(TAU * 1.7 * t));
      p.R[i] += v * (1 - 0.2 * Math.sin(TAU * 1.7 * t));
    }
  }
  return D.fundidos(D.masterizar([p.L, p.R], { rmsDb: -18, picoDb: -1 }), 0.01, 0.25);
}

/** El final del tobogan y la caida en la Fiesta. */
function despues () {
  const p = pista(DUR_FIN);
  const r2 = D.azar(88);
  const bp = new D.Biquad('bp', 1800, 1.1);
  const aire = new D.Biquad('hp', 1500, 0.7);
  for (let i = 0; i < p.n; i++) {
    const t = i / SR;
    const k = t < 0.55 ? 1 : Math.max(0, 1 - (t - 0.55) / 0.08);
    const r = r2() * 2 - 1;
    const v = (bp.paso(r) * 0.6 + aire.paso(r) * 0.3) * k * 0.5 * Math.min(1, t / 0.05);
    p.L[i] += v;
    p.R[i] += v;
  }
  // el golpe blando al caer, y algo de plastico que rueda
  const g = Math.round(0.8 * SR);
  const golpe = new Float32Array(g);
  const lp = new D.Biquad('lp', 200, 0.9);
  for (let i = 0; i < g; i++) {
    const t = i / SR;
    golpe[i] = lp.paso(r2() * 2 - 1) * 3 * Math.exp(-t / 0.08);
  }
  p.poner(0.6, golpe, 0.9);
  for (let k = 0; k < 9; k++) {
    const n = Math.round(0.05 * SR);
    const tic = new Float32Array(n);
    const f = 1800 + r2() * 1600;
    for (let i = 0; i < n; i++) tic[i] = Math.sin(TAU * f * i / SR) * Math.exp(-i / SR / 0.008) * 0.25;
    p.poner(0.65 + k * 0.09 + r2() * 0.05, tic, 1 - k * 0.08, r2() - 0.5);
  }
  const rv = new D.Freeverb({ sala: 0.6, amortiguacion: 0.5, predelayMs: 12 });
  for (let i = 0; i < p.n; i++) {
    const [a, b] = rv.paso((p.L[i] + p.R[i]) * 0.5);
    p.L[i] += a * 0.3;
    p.R[i] += b * 0.3;
  }
  return D.fundidos(D.masterizar([p.L, p.R], { rmsDb: -20, picoDb: -1 }), 0.004, 0.5);
}

if (require.main === module) (async () => {
  console.log('Cinematica del tobogan');
  for (const [nombre, buf, dur] of [['cinematica_tobogan', cinematica(), DUR], ['cinematica_tobogan_despues', despues(), DUR_FIN]]) {
    const bytes = await D.guardarOgg(path.join(SONIDOS, `${nombre}.ogg`), buf, 4);
    console.log(`  ${nombre}.ogg ${dur} s, ${(bytes / 1024).toFixed(0)} KB`);
  }
  const fj = path.join(SONIDOS, '..', 'sounds.json');
  const texto = fs.readFileSync(fj, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  j['cinematica.tobogan'] = { sounds: [{ name: 'backrooms_evento:cinematica_tobogan' }] };
  j['cinematica.tobogan.despues'] = { sounds: [{ name: 'backrooms_evento:cinematica_tobogan_despues' }] };
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(fj, out);
  console.log('  sounds.json');
})();
