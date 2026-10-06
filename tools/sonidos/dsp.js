'use strict';
/**
 * Piezas de sintesis para tools/sonidos/generar.js: osciladores sin aliasing,
 * filtros biquad (RBJ), ruido rosa y marron, reverb Freeverb, cinta (wow y
 * flutter), bucle sin costura y normalizacion. Todo determinista: la misma
 * semilla da exactamente el mismo archivo.
 */

const SR = 44100;

/* ------------------------------------------------------------------ azar */

function azar (semilla) {
  let a = semilla >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/* ------------------------------------------------------------ osciladores */

function polyblep (t, dt) {
  if (t < dt) { t /= dt; return t + t - t * t - 1; }
  if (t > 1 - dt) { t = (t - 1) / dt; return t * t + t + t + 1; }
  return 0;
}

/** Sierra limitada en banda. Llamar con la fase (0..1) y el incremento por muestra. */
function sierra (fase, dt) { return 2 * fase - 1 - polyblep(fase, dt); }

/* ---------------------------------------------------------------- filtros */

class Biquad {
  constructor (tipo, f, q = 0.707, gananciaDb = 0) {
    this.tipo = tipo;
    this.x1 = this.x2 = this.y1 = this.y2 = 0;
    this.ajustar(f, q, gananciaDb);
  }

  ajustar (f, q = this.q, gananciaDb = this.g) {
    this.q = q;
    this.g = gananciaDb;
    const w = 2 * Math.PI * Math.min(f, SR * 0.45) / SR;
    const cw = Math.cos(w);
    const sw = Math.sin(w);
    const alfa = sw / (2 * q);
    const A = Math.pow(10, gananciaDb / 40);
    let b0, b1, b2, a0, a1, a2;
    switch (this.tipo) {
      case 'lp': b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = b0; a0 = 1 + alfa; a1 = -2 * cw; a2 = 1 - alfa; break;
      case 'hp': b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = b0; a0 = 1 + alfa; a1 = -2 * cw; a2 = 1 - alfa; break;
      case 'bp': b0 = alfa; b1 = 0; b2 = -alfa; a0 = 1 + alfa; a1 = -2 * cw; a2 = 1 - alfa; break;
      case 'pico': b0 = 1 + alfa * A; b1 = -2 * cw; b2 = 1 - alfa * A; a0 = 1 + alfa / A; a1 = -2 * cw; a2 = 1 - alfa / A; break;
      case 'graves': {
        const r = 2 * Math.sqrt(A) * alfa;
        b0 = A * ((A + 1) - (A - 1) * cw + r); b1 = 2 * A * ((A - 1) - (A + 1) * cw); b2 = A * ((A + 1) - (A - 1) * cw - r);
        a0 = (A + 1) + (A - 1) * cw + r; a1 = -2 * ((A - 1) + (A + 1) * cw); a2 = (A + 1) + (A - 1) * cw - r;
        break;
      }
      default: throw new Error(`filtro ${this.tipo}`);
    }
    this.b0 = b0 / a0; this.b1 = b1 / a0; this.b2 = b2 / a0; this.a1 = a1 / a0; this.a2 = a2 / a0;
  }

  paso (x) {
    const y = this.b0 * x + this.b1 * this.x1 + this.b2 * this.x2 - this.a1 * this.y1 - this.a2 * this.y2;
    this.x2 = this.x1; this.x1 = x; this.y2 = this.y1; this.y1 = y;
    return y;
  }
}

/* ------------------------------------------------------------------ ruido */

/** Ruido rosa (filtro de Paul Kellet). */
function rosa (rnd) {
  let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
  return () => {
    const w = rnd() * 2 - 1;
    b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759;
    b2 = 0.96900 * b2 + w * 0.1538520; b3 = 0.86650 * b3 + w * 0.3104856;
    b4 = 0.55000 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.0168980;
    const out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362;
    b6 = w * 0.115926;
    return out * 0.11;
  };
}

/** Ruido marron (integrado con fuga). */
function marron (rnd) {
  let v = 0;
  return () => { v = (v + 0.02 * (rnd() * 2 - 1)) * 0.998; return v * 3.5; };
}

/* ---------------------------------------------------------------- reverb */

const COMBS = [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617];
const ALLPASS = [556, 441, 341, 225];

class Freeverb {
  constructor ({ sala = 0.85, amortiguacion = 0.35, ancho = 1, predelayMs = 18 } = {}) {
    const fb = sala * 0.28 + 0.7;
    const mk = (n) => ({ buf: new Float32Array(n), i: 0, store: 0 });
    this.canales = [0, 23].map((spread) => ({
      combs: COMBS.map((n) => mk(n + spread)),
      aps: ALLPASS.map((n) => mk(n + spread))
    }));
    this.fb = fb;
    this.d1 = amortiguacion;
    this.d2 = 1 - amortiguacion;
    this.ancho = ancho;
    this.pre = new Float32Array(Math.max(1, Math.floor(predelayMs * SR / 1000)));
    this.pi = 0;
  }

  /** Recibe una muestra mono (o la suma L+R) y devuelve [L, R] humedos. */
  paso (x) {
    const entrada = this.pre[this.pi];
    this.pre[this.pi] = x * 0.015;
    this.pi = (this.pi + 1) % this.pre.length;
    const out = [0, 0];
    for (let c = 0; c < 2; c++) {
      const ch = this.canales[c];
      let s = 0;
      for (const cb of ch.combs) {
        const y = cb.buf[cb.i];
        cb.store = y * this.d2 + cb.store * this.d1;
        cb.buf[cb.i] = entrada + cb.store * this.fb;
        cb.i = (cb.i + 1) % cb.buf.length;
        s += y;
      }
      for (const ap of ch.aps) {
        const b = ap.buf[ap.i];
        ap.buf[ap.i] = s + b * 0.5;
        ap.i = (ap.i + 1) % ap.buf.length;
        s = b - s;
      }
      out[c] = s;
    }
    const w1 = (1 + this.ancho) / 2;
    const w2 = (1 - this.ancho) / 2;
    return [out[0] * w1 + out[1] * w2, out[1] * w1 + out[0] * w2];
  }
}

/* ------------------------------------------------------------------ cinta */

/** Wow y flutter: linea de retardo modulada con lectura interpolada. */
class Cinta {
  constructor ({ wowHz = 0.45, wowMs = 1.6, flutterHz = 6.3, flutterMs = 0.12, rnd = Math.random } = {}) {
    this.buf = [new Float32Array(SR), new Float32Array(SR)];
    this.i = 0;
    this.t = 0;
    this.wow = [wowHz, wowMs];
    this.flutter = [flutterHz, flutterMs];
    this.deriva = 0;
    this.rnd = rnd;
  }

  paso (l, r) {
    const n = this.buf[0].length;
    this.buf[0][this.i] = l;
    this.buf[1][this.i] = r;
    this.t += 1 / SR;
    this.deriva += (this.rnd() - 0.5) * 0.002;
    this.deriva *= 0.9995;
    const ms = 8 + this.wow[1] * Math.sin(2 * Math.PI * this.wow[0] * this.t) +
      this.flutter[1] * Math.sin(2 * Math.PI * this.flutter[0] * this.t) + this.deriva;
    const d = ms * SR / 1000;
    let pos = this.i - d;
    while (pos < 0) pos += n;
    const i0 = Math.floor(pos);
    const fr = pos - i0;
    const i1 = (i0 + 1) % n;
    this.i = (this.i + 1) % n;
    return [
      this.buf[0][i0] * (1 - fr) + this.buf[0][i1] * fr,
      this.buf[1][i0] * (1 - fr) + this.buf[1][i1] * fr
    ];
  }
}

/* ----------------------------------------------------------------- utiles */

function estereo (segundos) {
  const n = Math.round(segundos * SR);
  return [new Float32Array(n), new Float32Array(n)];
}

/**
 * Convierte un render de N+X muestras en un bucle de N sin costura: las
 * primeras X muestras se funden (a potencia constante) con la cola.
 */
function cerrarBucle ([l, r], segundosBucle) {
  const n = Math.round(segundosBucle * SR);
  const x = l.length - n;
  if (x <= 0) throw new Error('El render no tiene cola para el fundido');
  const outL = l.slice(0, n);
  const outR = r.slice(0, n);
  for (let i = 0; i < x; i++) {
    const a = Math.sin((Math.PI / 2) * (i / x));
    const b = Math.cos((Math.PI / 2) * (i / x));
    outL[i] = l[i] * a + l[n + i] * b;
    outR[i] = r[i] * a + r[n + i] * b;
  }
  return [outL, outR];
}

/** Ajusta a un nivel RMS objetivo sin pasar del pico (con saturacion suave al final). */
function masterizar ([l, r], { rmsDb = -20, picoDb = -1 } = {}) {
  let suma = 0;
  for (let i = 0; i < l.length; i++) suma += l[i] * l[i] + r[i] * r[i];
  const rms = Math.sqrt(suma / (2 * l.length)) || 1e-9;
  let g = Math.pow(10, rmsDb / 20) / rms;
  const techo = Math.pow(10, picoDb / 20);
  // limitador suave: tanh por encima de 3/4 del techo
  const lim = (v) => {
    const u = v * g;
    const k = techo * 0.75;
    if (Math.abs(u) <= k) return u;
    const s = Math.sign(u);
    return s * (k + (techo - k) * Math.tanh((Math.abs(u) - k) / (techo - k)));
  };
  for (let i = 0; i < l.length; i++) { l[i] = lim(l[i]); r[i] = lim(r[i]); }
  return [l, r];
}

function fundidos ([l, r], entradaS = 0.004, salidaS = 0.03) {
  const fe = Math.floor(entradaS * SR);
  const fs = Math.floor(salidaS * SR);
  for (let i = 0; i < fe; i++) { const g = i / fe; l[i] *= g; r[i] *= g; }
  for (let i = 0; i < fs; i++) { const g = i / fs; l[l.length - 1 - i] *= g; r[r.length - 1 - i] *= g; }
  return [l, r];
}

/**
 * Lo que suena desde `desde` segundos hasta el final, con un fundido de entrada corto.
 * Para las cinematicas: Minecraft corta todos los sonidos al cambiar de mundo, asi que lo
 * de despues del viaje va en otro archivo que el cliente pone al llegar.
 */
function desde ([l, r], segundos) {
  const i0 = Math.min(l.length, Math.round(segundos * SR));
  const a = l.slice(i0);
  const b = r.slice(i0);
  const fe = Math.floor(0.012 * SR);
  for (let i = 0; i < fe && i < a.length; i++) { const g = i / fe; a[i] *= g; b[i] *= g; }
  return [a, b];
}

/** Codifica a OGG Vorbis estereo y lo guarda. */
async function guardarOgg (file, [l, r], calidad = 5) {
  const { createOggEncoder } = require('wasm-media-encoders');
  const fs = require('fs');
  const path = require('path');
  const enc = await createOggEncoder();
  enc.configure({ sampleRate: SR, channels: 2, vbrQuality: calidad });
  const trozos = [];
  for (let i = 0; i < l.length; i += 4096) trozos.push(Buffer.from(enc.encode([l.subarray(i, i + 4096), r.subarray(i, i + 4096)])));
  trozos.push(Buffer.from(enc.finalize()));
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, Buffer.concat(trozos));
  return fs.statSync(file).size;
}

module.exports = { guardarOgg, SR, azar, sierra, Biquad, rosa, marron, Freeverb, Cinta, estereo, cerrarBucle, masterizar, fundidos, desde };
