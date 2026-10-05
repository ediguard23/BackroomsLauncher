'use strict';
/**
 * Lector y escritor de PNG minimo (8 bits, RGB o RGBA, sin entrelazar), con
 * solo zlib de Node. Basta para las imagenes de assets/fuentes-ia y evita
 * meter una dependencia nativa (sharp) solo para recortar logos.
 */

const zlib = require('zlib');

const FIRMA = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

const TABLA_CRC = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32 (buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = TABLA_CRC[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function paeth (a, b, c) {
  const p = a + b - c;
  const pa = Math.abs(p - a);
  const pb = Math.abs(p - b);
  const pc = Math.abs(p - c);
  return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
}

/** Devuelve { width, height, data } con data en RGBA (Uint8Array). */
function leer (buf) {
  if (!buf.subarray(0, 8).equals(FIRMA)) throw new Error('No es un PNG');
  let pos = 8;
  let ancho = 0;
  let alto = 0;
  let tipo = 0;
  const idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos);
    const nombre = buf.toString('ascii', pos + 4, pos + 8);
    const datos = buf.subarray(pos + 8, pos + 8 + len);
    if (nombre === 'IHDR') {
      ancho = datos.readUInt32BE(0);
      alto = datos.readUInt32BE(4);
      const bits = datos[8];
      tipo = datos[9];
      if (bits !== 8 || (tipo !== 2 && tipo !== 6) || datos[12] !== 0) {
        throw new Error(`PNG no soportado (bits ${bits}, tipo ${tipo}, entrelazado ${datos[12]})`);
      }
    } else if (nombre === 'IDAT') {
      idat.push(datos);
    } else if (nombre === 'IEND') {
      break;
    }
    pos += 12 + len;
  }
  const bpp = tipo === 6 ? 4 : 3;
  const crudo = zlib.inflateSync(Buffer.concat(idat));
  const fila = ancho * bpp;
  const px = new Uint8Array(fila * alto);
  for (let y = 0; y < alto; y++) {
    const f = crudo[y * (fila + 1)];
    const src = y * (fila + 1) + 1;
    const dst = y * fila;
    for (let x = 0; x < fila; x++) {
      const a = x >= bpp ? px[dst + x - bpp] : 0;
      const b = y > 0 ? px[dst - fila + x] : 0;
      const c = x >= bpp && y > 0 ? px[dst - fila + x - bpp] : 0;
      let v = crudo[src + x];
      if (f === 1) v += a;
      else if (f === 2) v += b;
      else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) v += paeth(a, b, c);
      px[dst + x] = v & 0xff;
    }
  }
  if (bpp === 4) return { width: ancho, height: alto, data: px };
  const rgba = new Uint8Array(ancho * alto * 4);
  for (let i = 0, j = 0; i < px.length; i += 3, j += 4) {
    rgba[j] = px[i]; rgba[j + 1] = px[i + 1]; rgba[j + 2] = px[i + 2]; rgba[j + 3] = 255;
  }
  return { width: ancho, height: alto, data: rgba };
}

function trozo (nombre, datos) {
  const cab = Buffer.alloc(8);
  cab.writeUInt32BE(datos.length, 0);
  cab.write(nombre, 4, 'ascii');
  const cola = Buffer.alloc(4);
  cola.writeUInt32BE(crc32(Buffer.concat([cab.subarray(4), datos])), 0);
  return Buffer.concat([cab, datos, cola]);
}

/** Codifica RGBA a PNG eligiendo por fila el filtro que menos ocupa. */
function escribir ({ width, height, data }) {
  const fila = width * 4;
  const salida = Buffer.alloc((fila + 1) * height);
  const prueba = Buffer.alloc(fila);
  for (let y = 0; y < height; y++) {
    let mejor = 0;
    let mejorSuma = Infinity;
    for (let f = 0; f <= 4; f++) {
      let suma = 0;
      for (let x = 0; x < fila; x++) {
        const i = y * fila + x;
        const a = x >= 4 ? data[i - 4] : 0;
        const b = y > 0 ? data[i - fila] : 0;
        const c = x >= 4 && y > 0 ? data[i - fila - 4] : 0;
        const pred = f === 0 ? 0 : f === 1 ? a : f === 2 ? b : f === 3 ? (a + b) >> 1 : paeth(a, b, c);
        const v = (data[i] - pred) & 0xff;
        prueba[x] = v;
        suma += v < 128 ? v : 256 - v;
      }
      if (suma < mejorSuma) {
        mejorSuma = suma;
        mejor = f;
        prueba.copy(salida, y * (fila + 1) + 1);
      }
    }
    salida[y * (fila + 1)] = mejor;
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([
    FIRMA,
    trozo('IHDR', ihdr),
    trozo('IDAT', zlib.deflateSync(salida, { level: 9 })),
    trozo('IEND', Buffer.alloc(0))
  ]);
}

module.exports = { leer, escribir };
