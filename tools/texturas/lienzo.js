'use strict';
/**
 * Utilidades de dibujo para las texturas del evento: azar y ruido sin
 * costuras, un lienzo RGBA con primitivas (rectangulos, lineas gruesas,
 * circulos, poligonos, letras de 5x7) y el guardado a PNG.
 */

const fs = require('fs');
const path = require('path');
const png = require('../imagenes/png');

function hash (x, y, canal) {
  let h = (x * 374761393 + y * 668265263 + canal * 2147483647) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}

/** Ruido de valor periodico en un lado de `lado` pixeles con `celdas` celdas. */
function ruido (x, y, celdas, canal, lado = 32) {
  const fx = (x / lado) * celdas;
  const fy = (y / lado) * celdas;
  const x0 = Math.floor(fx);
  const y0 = Math.floor(fy);
  const sx = fx - x0;
  const sy = fy - y0;
  const u = sx * sx * (3 - 2 * sx);
  const v = sy * sy * (3 - 2 * sy);
  const m = (n) => ((n % celdas) + celdas) % celdas;
  const a = hash(m(x0), m(y0), canal);
  const b = hash(m(x0 + 1), m(y0), canal);
  const c = hash(m(x0), m(y0 + 1), canal);
  const d = hash(m(x0 + 1), m(y0 + 1), canal);
  return (a + (b - a) * u) * (1 - v) + (c + (d - c) * u) * v;
}

function fbm (x, y, canal, lado = 32) {
  return ruido(x, y, 2, canal, lado) * 0.5 + ruido(x, y, 4, canal + 1, lado) * 0.3 + ruido(x, y, 8, canal + 2, lado) * 0.2;
}

const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)));
const mezcla = (a, b, t) => a.map((v, k) => v + (b[k] - v) * t);
const escala = (c, f) => c.map((v) => v * f);

/** Letras de 5x7 (las mismas que el OSD del mod). */
const LETRAS = {
  A: ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
  C: ['.###.', '#...#', '#....', '#....', '#....', '#...#', '.###.'],
  D: ['####.', '#...#', '#...#', '#...#', '#...#', '#...#', '####.'],
  E: ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
  G: ['.###.', '#...#', '#....', '#.###', '#...#', '#...#', '.####'],
  H: ['#...#', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
  I: ['.###.', '..#..', '..#..', '..#..', '..#..', '..#..', '.###.'],
  L: ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
  N: ['#...#', '#...#', '##..#', '#.#.#', '#..##', '#...#', '#...#'],
  O: ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
  P: ['####.', '#...#', '#...#', '####.', '#....', '#....', '#....'],
  Q: ['.###.', '#...#', '#...#', '#...#', '#.#.#', '#..#.', '.##.#'],
  R: ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
  S: ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'],
  T: ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
  U: ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
  X: ['#...#', '#...#', '.#.#.', '..#..', '.#.#.', '#...#', '#...#'],
  Y: ['#...#', '#...#', '.#.#.', '..#..', '..#..', '..#..', '..#..'],
  '?': ['.###.', '#...#', '....#', '...#.', '..#..', '.....', '..#..'],
  '!': ['..#..', '..#..', '..#..', '..#..', '..#..', '.....', '..#..']
};

class Lienzo {
  constructor (ancho, alto = ancho) {
    this.width = ancho;
    this.height = alto;
    this.data = new Uint8Array(ancho * alto * 4);
  }

  /** Pinta (x, y) con [r, g, b] y alfa a (0-1), mezclando con lo que haya. */
  punto (x, y, c, a = 1) {
    x = Math.round(x);
    y = Math.round(y);
    if (x < 0 || y < 0 || x >= this.width || y >= this.height || a <= 0) return;
    const i = (y * this.width + x) * 4;
    const ad = this.data[i + 3] / 255;
    const ao = a + ad * (1 - a);
    for (let k = 0; k < 3; k++) {
      this.data[i + k] = clamp((c[k] * a + this.data[i + k] * ad * (1 - a)) / (ao || 1));
    }
    this.data[i + 3] = clamp(ao * 255);
  }

  /** Rellena todo con fn(x, y) -> [r, g, b] o [r, g, b, a] (a 0-1). */
  pintar (fn) {
    for (let y = 0; y < this.height; y++) {
      for (let x = 0; x < this.width; x++) {
        const c = fn(x, y);
        if (c) this.punto(x, y, c, c.length > 3 ? c[3] : 1);
      }
    }
    return this;
  }

  rect (x0, y0, x1, y1, c, a = 1) {
    for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) this.punto(x, y, c, a);
    return this;
  }

  /** Linea de grosor `g` con un pulso irregular (trazo a mano). */
  linea (x0, y0, x1, y1, c, g = 1, a = 1, temblor = 0) {
    const n = Math.max(1, Math.ceil(Math.hypot(x1 - x0, y1 - y0) * 2));
    for (let i = 0; i <= n; i++) {
      const t = i / n;
      const x = x0 + (x1 - x0) * t + (temblor ? (hash(i, 7, x0 + y0) - 0.5) * temblor : 0);
      const y = y0 + (y1 - y0) * t + (temblor ? (hash(i, 9, x1 + y1) - 0.5) * temblor : 0);
      this.disco(x, y, g / 2, c, a);
    }
    return this;
  }

  disco (cx, cy, r, c, a = 1) {
    for (let y = Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
      for (let x = Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
        if ((x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.25) this.punto(x, y, c, a);
      }
    }
    return this;
  }

  /** Relleno de un poligono convexo o no (regla par-impar). */
  poligono (pts, c, a = 1) {
    for (let y = 0; y < this.height; y++) {
      for (let x = 0; x < this.width; x++) {
        let dentro = false;
        for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
          const [xi, yi] = pts[i];
          const [xj, yj] = pts[j];
          if ((yi > y + 0.5) !== (yj > y + 0.5) && x + 0.5 < ((xj - xi) * (y + 0.5 - yi)) / (yj - yi) + xi) dentro = !dentro;
        }
        if (dentro) this.punto(x, y, c, a);
      }
    }
    return this;
  }

  /** Texto en letras de 5x7 con pixeles de `p`; `x` puede ser 'centro'. */
  texto (s, x, y, c, p = 1, a = 1) {
    const ancho = s.length * 6 * p - p;
    let cx = x === 'centro' ? Math.round((this.width - ancho) / 2) : x;
    for (const ch of s) {
      const g = LETRAS[ch];
      if (g) {
        g.forEach((fila, fy) => [...fila].forEach((v, fx) => {
          if (v === '#') this.rect(cx + fx * p, y + fy * p, cx + fx * p + p - 1, y + fy * p + p - 1, c, a);
        }));
      }
      cx += 6 * p;
    }
    return this;
  }

  /** Copia volteada en horizontal (las senales "al reves" del Nivel 0). */
  espejo () {
    const out = new Lienzo(this.width, this.height);
    for (let y = 0; y < this.height; y++) {
      for (let x = 0; x < this.width; x++) {
        const s = (y * this.width + (this.width - 1 - x)) * 4;
        out.data.set(this.data.subarray(s, s + 4), (y * this.width + x) * 4);
      }
    }
    return out;
  }

  /** Pega otro lienzo debajo de este (para tiras de animacion). */
  static tira (cuadros) {
    const w = cuadros[0].width;
    const h = cuadros[0].height;
    const out = new Lienzo(w, h * cuadros.length);
    cuadros.forEach((c, i) => out.data.set(c.data, i * w * h * 4));
    return out;
  }
}

function guardar (dir, nombre, lienzo) {
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${nombre}.png`), png.escribir(lienzo));
  console.log(`  ${nombre}.png`);
}

module.exports = { hash, ruido, fbm, clamp, mezcla, escala, Lienzo, guardar };
