'use strict';
/**
 * Los colores de los mapas de Minecraft y un GIF hecho solo con ellos.
 *
 * Yamipa pasa cada pixel al color de mapa mas cercano (MapPalette.matchColor de Bukkit)
 * sin tramado. Si el GIF ya va en esa paleta, lo que se ve aqui es exactamente lo que
 * se ve en el juego: el tramado y los colores los decidimos nosotros.
 */

// MapColor de Minecraft 1.21.11, ids 1..61 (el 0 es transparente)
const BASES = [
  8368696, 16247203, 13092807, 16711680, 10526975, 10987431, 31744, 16777215, 10791096, 9923917,
  7368816, 4210943, 9402184, 16776437, 14188339, 11685080, 6724056, 15066419, 8375321, 15892389,
  5000268, 10066329, 5013401, 8339378, 3361970, 6704179, 6717235, 10040115, 1644825, 16445005,
  6085589, 4882687, 55610, 8476209, 7340544, 13742497, 10441252, 9787244, 7367818, 12223780,
  6780213, 10505550, 3746083, 8874850, 5725276, 8014168, 4996700, 4993571, 5001770, 9321518,
  2430480, 12398641, 9715553, 6035741, 1474182, 3837580, 5647422, 1356933, 6579300, 14200723,
  8365974
];
const BRILLOS = [180, 220, 255, 135]; // MapColor.Brightness LOW, NORMAL, HIGH, LOWEST

/** Los 244 colores opacos, como los calcula MapColor.calculateARGBColor. */
const PALETA = [];
for (const base of BASES) {
  for (const k of BRILLOS) {
    PALETA.push([Math.floor(((base >> 16) & 255) * k / 255), Math.floor(((base >> 8) & 255) * k / 255), Math.floor((base & 255) * k / 255)]);
  }
}

/** Color de la paleta por nombre (para los textos: asi no se traman). */
function color (id, brillo) {
  const i = (id - 1) * 4 + BRILLOS.indexOf(brillo);
  const [r, g, b] = PALETA[i];
  return `rgb(${r},${g},${b})`;
}

/* --------------------------------------------- color mas cercano (Oklab) */

function lineal (c) { c /= 255; return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); }
function oklab (r, g, b) {
  r = lineal(r); g = lineal(g); b = lineal(b);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return [0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
    1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
    0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s];
}

const LAB = PALETA.map(([r, g, b]) => oklab(r, g, b));
const PASO = 4; // tabla de 64x64x64
let tabla = null;

function cercano (r, g, b) {
  const [L, A, B] = oklab(r, g, b);
  let mejor = 0;
  let dm = Infinity;
  for (let i = 0; i < LAB.length; i++) {
    const dl = L - LAB[i][0];
    const da = A - LAB[i][1];
    const db = B - LAB[i][2];
    // mas peso al eje verde-rojo, y mas aun si el candidato es mas verde que el original:
    // en tonos oscuros la paleta tiene mas oliva que amarillo y el pasillo se volvia verde
    const d = dl * dl * 1.2 + da * da * (da > 0 ? 5.0 : 1.8) + db * db * 1.3;
    if (d < dm) { dm = d; mejor = i; }
  }
  return mejor;
}

function preparar () {
  if (tabla) return;
  const n = 256 / PASO;
  tabla = new Uint8Array(n * n * n);
  for (let r = 0; r < n; r++) {
    for (let g = 0; g < n; g++) {
      for (let b = 0; b < n; b++) tabla[(r * n + g) * n + b] = cercano(r * PASO + PASO / 2, g * PASO + PASO / 2, b * PASO + PASO / 2);
    }
  }
  // los colores exactos de la paleta no pueden caer en el vecino por la tabla
  exactos = new Map(PALETA.map(([r, g, b], i) => [(r << 16) | (g << 8) | b, i]));
}
let exactos = null;

function indice (r, g, b) {
  const e = exactos.get((r << 16) | (g << 8) | b);
  if (e !== undefined) return e;
  const n = 256 / PASO;
  return tabla[((r >> 2) * n + (g >> 2)) * n + (b >> 2)];
}

// Bayer 8x8: tramado ordenado, quieto de un fotograma a otro (el de difusion "hierve")
const BAYER = (() => {
  const m = [[0, 2], [3, 1]];
  let b = m;
  for (let s = 2; s < 8; s *= 2) {
    const nb = [];
    for (let y = 0; y < s * 2; y++) {
      nb.push([]);
      for (let x = 0; x < s * 2; x++) nb[y].push(4 * b[y % s][x % s] + m[Math.floor(y / s)][Math.floor(x / s)]);
    }
    b = nb;
  }
  return b.map((fila) => fila.map((v) => (v + 0.5) / 64 - 0.5));
})();

/**
 * fondo: RGBA (lo que va tramado), texto: RGBA (encima, sin tramar).
 * Devuelve un indice de la paleta por pixel.
 */
function cuantizar (fondo, texto, ancho, alto, fuerza = 18) {
  preparar();
  const out = new Uint8Array(ancho * alto);
  for (let y = 0; y < alto; y++) {
    for (let x = 0; x < ancho; x++) {
      const p = y * ancho + x;
      const o = p * 4;
      const a = texto ? texto[o + 3] / 255 : 0;
      let r = fondo[o] * (1 - a) + (a > 0 ? texto[o] * a : 0);
      let g = fondo[o + 1] * (1 - a) + (a > 0 ? texto[o + 1] * a : 0);
      let b = fondo[o + 2] * (1 - a) + (a > 0 ? texto[o + 2] * a : 0);
      if (a < 0.35) {
        const d = BAYER[y & 7][x & 7] * fuerza * (1 - a);
        r += d; g += d; b += d;
      }
      out[p] = indice(Math.max(0, Math.min(255, Math.round(r))), Math.max(0, Math.min(255, Math.round(g))), Math.max(0, Math.min(255, Math.round(b))));
    }
  }
  return out;
}

/* ------------------------------------------------------------------ GIF */

function lzw (indices, minimo) {
  const clear = 1 << minimo;
  const fin = clear + 1;
  const out = [];
  let acum = 0;
  let bits = 0;
  let tam = minimo + 1;
  let siguiente = fin + 1;
  let dic = new Map();
  const emitir = (c) => {
    acum |= c << bits;
    bits += tam;
    while (bits >= 8) { out.push(acum & 255); acum >>>= 8; bits -= 8; }
  };
  emitir(clear);
  let prefijo = indices[0];
  for (let i = 1; i < indices.length; i++) {
    const k = indices[i];
    const clave = prefijo * 256 + k;
    const v = dic.get(clave);
    if (v !== undefined) { prefijo = v; continue; }
    emitir(prefijo);
    if (siguiente === 4096) {
      emitir(clear);
      siguiente = fin + 1;
      tam = minimo + 1;
      dic = new Map();
    } else {
      if (siguiente >= (1 << tam)) tam++;
      dic.set(clave, siguiente++);
    }
    prefijo = k;
  }
  emitir(prefijo);
  emitir(fin);
  if (bits > 0) out.push(acum & 255);
  return out;
}

/** Fotogramas completos (sin transparencias ni recortes: Yamipa los lee con ImageIO). */
function gif (fotogramas, ancho, alto, centesimas) {
  const b = [];
  const u16 = (v) => { b.push(v & 255, (v >> 8) & 255); };
  for (const c of 'GIF89a') b.push(c.charCodeAt(0));
  u16(ancho); u16(alto);
  b.push(0xF7, 0, 0);
  for (let i = 0; i < 256; i++) {
    const c = PALETA[i] || [0, 0, 0];
    b.push(c[0], c[1], c[2]);
  }
  b.push(0x21, 0xFF, 0x0B);
  for (const c of 'NETSCAPE2.0') b.push(c.charCodeAt(0));
  b.push(3, 1, 0, 0, 0);
  for (const f of fotogramas) {
    b.push(0x21, 0xF9, 4, 0x04);
    u16(centesimas);
    b.push(0, 0);
    b.push(0x2C); u16(0); u16(0); u16(ancho); u16(alto); b.push(0);
    b.push(8);
    const datos = lzw(f, 8);
    for (let i = 0; i < datos.length; i += 255) {
      const trozo = datos.slice(i, i + 255);
      b.push(trozo.length, ...trozo);
    }
    b.push(0);
  }
  b.push(0x3B);
  return Buffer.from(b);
}

module.exports = { PALETA, color, cuantizar, gif };
