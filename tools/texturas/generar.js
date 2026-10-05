'use strict';
/**
 * Texturas de los bloques del Nivel 0 (mod evento/), dibujadas por codigo a
 * 32x32 y sin costuras: papel pintado amarillo con su motivo, zocalo, moqueta
 * (seca y mojada), placas del techo y los tubos fluorescentes.
 *
 *   npm run texturas
 *
 * Todo sale de una semilla fija: volver a ejecutarlo da los mismos PNG.
 */

const fs = require('fs');
const path = require('path');
const png = require('../imagenes/png');

const LADO = 32;
const DESTINO = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'textures', 'block');

/* --------------------------------------------------------------- azar */

function hash (x, y, canal) {
  let h = (x * 374761393 + y * 668265263 + canal * 2147483647) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}

/** Ruido de valor periodico (sin costuras) en una reticula de `celdas` x `celdas`. */
function ruido (x, y, celdas, canal) {
  const fx = (x / LADO) * celdas;
  const fy = (y / LADO) * celdas;
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

function fbm (x, y, canal) {
  return ruido(x, y, 2, canal) * 0.5 + ruido(x, y, 4, canal + 1) * 0.3 + ruido(x, y, 8, canal + 2) * 0.2;
}

/* ------------------------------------------------------------ dibujo */

function lienzo (fn) {
  const img = { width: LADO, height: LADO, data: new Uint8Array(LADO * LADO * 4) };
  for (let y = 0; y < LADO; y++) {
    for (let x = 0; x < LADO; x++) {
      const [r, g, b] = fn(x, y);
      const i = (y * LADO + x) * 4;
      img.data[i] = clamp(r);
      img.data[i + 1] = clamp(g);
      img.data[i + 2] = clamp(b);
      img.data[i + 3] = 255;
    }
  }
  return img;
}

const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)));
const mezcla = (a, b, t) => a.map((v, k) => v + (b[k] - v) * t);
const escala = (c, f) => c.map((v) => v * f);

function guardar (nombre, img) {
  fs.mkdirSync(DESTINO, { recursive: true });
  fs.writeFileSync(path.join(DESTINO, `${nombre}.png`), png.escribir(img));
  console.log(`  ${nombre}.png`);
}

/* ------------------------------------------------------------ texturas */

const PAPEL = [214, 194, 104];

/** Motivo del papel: flechas "^" apiladas en columnas de 8, como el papel del Nivel 0. */
function motivo (x, y) {
  const mx = x % 8;
  const my = (y + (Math.floor(x / 8) % 2) * 4) % 8; // columnas alternas desfasadas
  const d = Math.abs(mx - 3.5);
  return (my === 2 && d < 1) || (my === 3 && d > 0.9 && d < 2) || (my === 4 && d > 1.9 && d < 3);
}

function papel (x, y, sucio) {
  let c = PAPEL.slice();
  if (x % 8 === 0) c = escala(c, 1.04); // raya vertical clara
  if (motivo(x, y)) c = escala(c, 0.9);
  c = escala(c, 0.95 + fbm(x, y, 3) * 0.1);
  c = escala(c, 0.97 + hash(x, y, 9) * 0.06);
  if (sucio) {
    // lamparon de humedad con chorretones; se desvanece hacia los bordes del bloque para
    // que una pared manchada suelta no se vea como un cuadrado
    const mancha = fbm(x, y, 21);
    const chorro = ruido(x, y * 0.25, 8, 23);
    const dx = (x - 15.5) / 16;
    const dy = (y - 13) / 19;
    const borde = Math.max(0, 1 - Math.sqrt(dx * dx + dy * dy));
    const t = (Math.max(0, (mancha - 0.42) * 2.4) + Math.max(0, (chorro - 0.62) * 2) * (y / LADO)) * Math.min(1, borde * 1.8);
    c = mezcla(c, [128, 104, 52], Math.min(0.75, t));
  }
  return c;
}

console.log('Texturas del Nivel 0');
guardar('papel_pintado', lienzo((x, y) => papel(x, y, false)));
guardar('papel_pintado_sucio', lienzo((x, y) => papel(x, y, true)));

// zocalo: papel arriba y la tabla de madera abajo (7 px)
guardar('zocalo', lienzo((x, y) => {
  if (y < 25) return papel(x, y, false);
  if (y === 25) return [128, 104, 62];
  if (y === 31) return [58, 46, 26];
  const veta = 0.92 + ruido(x * 0.25, y, 8, 31) * 0.12 + hash(x, y, 33) * 0.04;
  return escala([96, 77, 44], veta);
}));

function moqueta (x, y, mojada) {
  const base = mojada ? [124, 106, 52] : [180, 158, 86];
  let c = escala(base, 0.84 + hash(x, y, 41) * 0.24); // fibras
  c = escala(c, 0.94 + fbm(x, y, 43) * 0.12);
  if (hash(x, y, 45) > 0.985) c = escala(c, 0.7); // motas
  if (mojada && hash(x, y, 47) > 0.96) c = escala(c, 1.25); // brillos del agua
  return c;
}
guardar('moqueta', lienzo((x, y) => moqueta(x, y, false)));
guardar('moqueta_mojada', lienzo((x, y) => moqueta(x, y, true)));

// placa de techo: borde mas oscuro (cada bloque es una placa) y agujeritos
guardar('techo', lienzo((x, y) => {
  let c = escala([222, 214, 184], 0.96 + fbm(x, y, 51) * 0.06);
  if (x === 0 || y === 0) c = [176, 168, 138];
  else if (x === 31 || y === 31) c = [196, 188, 158];
  else if ((x % 4 === 2 && y % 4 === 2 && hash(x, y, 53) > 0.35) || hash(x, y, 55) > 0.97) c = escala(c, 0.86);
  return c;
}));

// tubos: marco gris y difusor; encendido casi blanco, apagado gris sucio
function tubo (x, y, encendido) {
  const marco = x < 2 || y < 2 || x > 29 || y > 29;
  if (marco) return (x === 0 || y === 0 || x === 31 || y === 31) ? [150, 148, 138] : [196, 194, 184];
  const difusor = (x % 4 === 0 || y % 4 === 0) ? 0.97 : 1;
  if (encendido) {
    const banda = Math.abs(y - 15.5) < 6 ? 1 : 0.96; // los tubos se notan detras del difusor
    return escala([255, 252, 232], difusor * banda);
  }
  return escala([142, 140, 128], difusor * (0.94 + fbm(x, y, 61) * 0.1));
}
guardar('fluorescente', lienzo((x, y) => tubo(x, y, true)));
guardar('fluorescente_apagado', lienzo((x, y) => tubo(x, y, false)));
