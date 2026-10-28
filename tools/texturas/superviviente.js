'use strict';
/**
 * Las pieles de las figuras con esqueleto articulado (esqueleto/Figura, cliente/esqueleto/FiguraRenderer):
 * 64x64 en formato de piel de jugador. Todo dibujado aqui:
 *
 *   superviviente         otro explorador con el traje amarillo del evento (capucha, visor con la cara
 *                         asustada detras, banda reflectante, cinturon, guantes y botas negros)
 *   superviviente_sangre  el mismo traje con los guantes y las mangas manchados de sangre (las manos del
 *                         jugador al final de la Estacion)
 *
 *   node tools/texturas/superviviente.js
 */

const path = require('path');
const { hash, fbm, mezcla, escala, Lienzo, guardar } = require('./lienzo');

const TEX = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento', 'textures', 'entity');

/** Pinta una caja del formato de piel: (u, v) la esquina, w ancho, h alto, d fondo. */
function caja (l, u, v, w, h, d, fn) {
  const caras = [
    ['arriba', u + d, v, w, d], ['abajo', u + d + w, v, w, d],
    ['der', u, v + d, d, h], ['frente', u + d, v + d, w, h],
    ['izq', u + d + w, v + d, d, h], ['atras', u + 2 * d + w, v + d, w, h]
  ];
  for (const [cara, x0, y0, W, H] of caras) {
    for (let y = 0; y < H; y++) {
      for (let x = 0; x < W; x++) {
        const c = fn(cara, x, y, W, H);
        if (c) l.punto(x0 + x, y0 + y, c, 1);
      }
    }
  }
}

const AMARILLO = [214, 172, 40];
const NEGRO = [34, 34, 37];
const REFLECTANTE = [196, 200, 204];
const SANGRE = [118, 14, 12];
const SECA = [66, 12, 10];

const traje = (x, y, k) => {
  let c = escala(AMARILLO, 0.84 + fbm(x, y, 2100 + k, 16) * 0.26);
  if (fbm(x * 2, y * 2, 2150 + k, 32) > 0.7) c = mezcla(c, [110, 96, 60], 0.35); // polvo
  return c;
};
const goma = (x, y, k) => escala(NEGRO, 0.85 + hash(x, y, 2200 + k) * 0.25);

function piel (nombre, ensangrentado) {
  const l = new Lienzo(64);
  const mancha = (c, x, y, k, cuanta) => {
    if (!ensangrentado) return c;
    const n = fbm(x * 1.5, y * 1.5, 2300 + k, 64);
    if (n > 1 - cuanta) return mezcla(c, hash(x, y, k) > 0.45 ? SANGRE : SECA, 0.88);
    if (n > 1 - cuanta * 1.3 && hash(x, y, k + 1) > 0.55) return mezcla(c, SECA, 0.6);
    return c;
  };
  // la cabeza: capucha con visor; detras del cristal, la cara (ojos muy abiertos)
  caja(l, 0, 0, 8, 8, 8, (cara, x, y) => {
    if (cara !== 'frente') return traje(x, y, 1);
    if (y >= 2 && y <= 5 && x >= 1 && x <= 6) {
      let c = mezcla([206, 178, 160], [150, 176, 190], 0.35); // piel tras el cristal azulado
      if (y === 3 && (x === 2 || x === 5)) c = [236, 236, 236];
      if (y === 3 && (x === 2 || x === 5) && hash(x, y, 9) > 2) c = [20, 20, 24];
      if (y === 4 && (x === 2 || x === 5)) c = [40, 34, 40]; // las pupilas, abajo: asustado
      if (y === 2 && (x === 1 || x === 6)) c = escala(c, 0.8);
      return x === 6 && y === 2 ? [230, 240, 246] : c; // un brillo en el cristal
    }
    if (y === 6 && x >= 3 && x <= 4) return [56, 56, 60]; // el filtro
    return traje(x, y, 2);
  });
  // el torso: traje, banda reflectante, cinturon
  caja(l, 16, 16, 8, 12, 4, (cara, x, y) => {
    if (y === 8) return goma(x, y, 3);
    if (y === 4) return REFLECTANTE;
    if (cara === 'frente' && x === 3 && y < 8) return escala(AMARILLO, 0.7); // la cremallera
    return mancha(traje(x, y, 4), x, y, 5, 0.12);
  });
  // brazos: banda reflectante y guantes negros (las dos ultimas filas son la mano)
  for (const [u, v, k] of [[40, 16, 10], [32, 48, 20]]) {
    caja(l, u, v, 4, 12, 4, (cara, x, y) => {
      if (y >= 8) return mancha(goma(x, y, k), x, y, k + 1, y >= 10 ? 0.62 : 0.45);
      if (y === 3) return REFLECTANTE;
      return mancha(traje(x, y, k + 2), x, y, k + 3, y >= 5 ? 0.4 : 0.15);
    });
  }
  // piernas: banda reflectante y botas negras
  for (const [u, v, k] of [[0, 16, 30], [16, 48, 40]]) {
    caja(l, u, v, 4, 12, 4, (cara, x, y) => {
      if (y >= 9) return goma(x, y, k);
      if (y === 6) return REFLECTANTE;
      return mancha(traje(x, y, k + 2), x, y, k + 3, 0.06);
    });
  }
  guardar(TEX, nombre, l);
}

piel('superviviente', false);
piel('superviviente_sangre', true);
