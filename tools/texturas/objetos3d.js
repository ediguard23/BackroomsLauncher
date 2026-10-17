'use strict';
/**
 * Modelos 3D (de cubos, como los bloques) de la linterna, la camara y el agua de almendras,
 * con su textura de 32x32 dividida en cuatro materiales (cuadrantes de 16x16):
 *
 *   linterna        cuerpo de aluminio negro con moleteado, cabeza lisa, lente con reflector
 *                   y LED (encendida: blanca y calida) y goma del pulsador y la tapa
 *   camara          videocamara de mano: cuerpo de plastico gris oscuro, objetivo de cristal,
 *                   pantalla abatible, visor, correa de mano y el piloto rojo de REC
 *   agua_almendras  botella redondeada con el agua lechosa, etiqueta con la almendra,
 *                   cuello transparente y tapon azul
 *
 * Se ven en 3D en la mano, en el suelo y en el inventario. Sustituyen a los dibujos planos
 * de expedicion.js (la linterna parecia una antorcha).
 *
 *   node tools/texturas/objetos3d.js   (va en npm run texturas)
 */

const fs = require('fs');
const path = require('path');
const { hash, ruido, escala, mezcla, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', NS);
const TEX_ITEM = path.join(ASSETS, 'textures', 'item');
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};

console.log('Objetos 3D');

/* los cuatro materiales de cada textura: cuadrantes, en coordenadas de UV (0-16) */
const Q = { A: [0, 0, 8, 8], B: [8, 0, 16, 8], C: [0, 8, 8, 16], D: [8, 8, 16, 16] };

/** Pinta el cuadrante `q` (0 A, 1 B, 2 C, 3 D) con fn(x, y) en coordenadas 0-15 del cuadrante. */
function cuadrante (l, q, fn) {
  const ox = (q % 2) * 16;
  const oy = Math.floor(q / 2) * 16;
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const c = fn(x, y);
      if (c) l.punto(ox + x, oy + y, c, c.length > 3 ? c[3] : 1);
    }
  }
}

/**
 * Un cubo con todas sus caras del material `mat` (cuadrante); `caras` cambia el de alguna
 * (p. ej. { north: 'C' }). Con `sinCaras` se quitan caras que no se ven.
 */
function cubo (from, to, mat, caras = {}, sinCaras = []) {
  const faces = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) {
    if (sinCaras.includes(d)) continue;
    faces[d] = { uv: Q[caras[d] || mat], texture: '#t' };
  }
  return { from, to, faces };
}

/** Las posturas en la mano, en el suelo, en el inventario y en un marco. */
function display ({ mano = [0, 0, 0], manoT = [0, 0, 0], manoE = 0.68, tercera = [0, 0, 0], terceraT = [0, 2, 1], terceraE = 0.55, gui = [30, 225, 0], guiE = 0.9, guiT = [0, 0, 0] }) {
  const izq = (r) => [r[0], -r[1], -r[2]];
  return {
    firstperson_righthand: { rotation: mano, translation: manoT, scale: [manoE, manoE, manoE] },
    firstperson_lefthand: { rotation: izq(mano), translation: manoT, scale: [manoE, manoE, manoE] },
    thirdperson_righthand: { rotation: tercera, translation: terceraT, scale: [terceraE, terceraE, terceraE] },
    thirdperson_lefthand: { rotation: izq(tercera), translation: terceraT, scale: [terceraE, terceraE, terceraE] },
    ground: { rotation: [0, 0, 0], translation: [0, 2, 0], scale: [0.5, 0.5, 0.5] },
    gui: { rotation: gui, translation: guiT, scale: [guiE, guiE, guiE] },
    fixed: { rotation: [0, 90, 0], translation: [0, 0, 0], scale: [0.85, 0.85, 0.85] },
    head: { rotation: [0, 0, 0], translation: [0, 10, 0], scale: [0.8, 0.8, 0.8] }
  };
}

function modelo (tex, elementos, disp) {
  return { texture_size: [32, 32], textures: { t: `${NS}:item/${tex}`, particle: `${NS}:item/${tex}` }, elements: elementos, display: disp, gui_light: 'side' };
}

/* ============================================================ linterna */

function texLinterna (encendida) {
  const l = new Lienzo(32);
  // A: el cuerpo, aluminio anodizado negro con moleteado en rombos
  cuadrante(l, 0, (x, y) => {
    const rombo = ((x + y) % 4 === 0) || ((x - y + 16) % 4 === 0);
    const base = escala([40, 42, 46], 0.92 + hash(x, y, 1) * 0.12);
    return rombo ? escala(base, 1.45) : base;
  });
  // B: la cabeza, metal liso con un cepillado suave y un bisel claro
  cuadrante(l, 1, (x, y) => {
    let c = escala([52, 55, 60], 0.94 + ruido(x, y, 4, 2, 16) * 0.1 + (y % 3 === 0 ? 0.04 : 0));
    if (y === 0 || x === 0) c = escala(c, 1.35);
    if (y === 15 || x === 15) c = escala(c, 0.7);
    return c;
  });
  // C: la lente: bisel, reflector plateado en anillos y el LED en el centro
  cuadrante(l, 2, (x, y) => {
    const d = Math.hypot(x - 7.5, y - 7.5);
    if (d > 7.2) return [30, 31, 34];
    if (d > 6.2) return [64, 66, 72];
    if (encendida) {
      if (d < 1.8) return [255, 255, 250];
      return mezcla([255, 246, 214], [236, 214, 160], Math.min(1, d / 6));
    }
    if (d < 1.6) return [236, 232, 214];
    const anillo = Math.sin(d * 2.4) * 0.5 + 0.5;
    const brillo = (x < y ? 1.15 : 0.9);
    return escala([150, 156, 166], (0.75 + anillo * 0.35) * brillo);
  });
  // D: goma negra (pulsador y tapa) con un aro naranja de la marca
  cuadrante(l, 3, (x, y) => {
    if (y === 7) return [196, 98, 30];
    return escala([22, 22, 25], 0.9 + hash(x, y, 9) * 0.2);
  });
  return l;
}
guardar(TEX_ITEM, 'linterna_3d', texLinterna(false));
guardar(TEX_ITEM, 'linterna_3d_encendida', texLinterna(true));

// apuntando al norte (hacia delante en la mano): lente en z = 0, tapa en z = 16
const LINTERNA = [
  cubo([5, 6, 0.5], [11, 10, 4], 'B', { north: 'C' }),         // cabeza (octogono: dos cajas)
  cubo([6, 5, 0.45], [10, 11, 4], 'B', { north: 'C' }),
  cubo([6, 6, 4], [10, 10, 5.5], 'B', {}, ['north']),           // cuello que se estrecha
  cubo([6.5, 6.5, 5.5], [9.5, 9.5, 14], 'A', {}, ['north']),    // cuerpo moleteado
  cubo([6.25, 6.25, 14], [9.75, 9.75, 15.5], 'D'),              // tapa de goma
  cubo([7.25, 9.5, 7], [8.75, 10.1, 9], 'D', {}, ['down']),     // pulsador
  cubo([9.5, 7.2, 8], [9.85, 8.8, 13], 'B', {}, ['west'])       // clip
];
const dispLinterna = display({ mano: [0, -8, 0], manoT: [1, 4, -3], manoE: 0.42, tercera: [0, 0, 0], terceraT: [0, 3, 1], terceraE: 0.6, gui: [30, 135, 0], guiE: 0.95 });
json('models/item/linterna.json', modelo('linterna_3d', LINTERNA, dispLinterna));
json('models/item/linterna_encendida.json', modelo('linterna_3d_encendida', LINTERNA, dispLinterna));
json('items/linterna.json', {
  model: {
    type: 'minecraft:condition', property: 'minecraft:custom_model_data', index: 0,
    on_true: { type: 'minecraft:model', model: `${NS}:item/linterna_encendida` },
    on_false: { type: 'minecraft:model', model: `${NS}:item/linterna` }
  }
});

/* ============================================================== camara */
{
  const l = new Lienzo(32);
  // A: plastico gris oscuro con grano y una junta
  cuadrante(l, 0, (x, y) => {
    let c = escala([46, 47, 52], 0.9 + hash(x, y, 21) * 0.14 + ruido(x, y, 5, 22, 16) * 0.06);
    if (y === 5) c = escala(c, 0.65);
    if (y === 6) c = escala(c, 1.2);
    return c;
  });
  // B: el objetivo: aro negro, cristal azulado con reflejos
  cuadrante(l, 1, (x, y) => {
    const d = Math.hypot(x - 7.5, y - 7.5);
    if (d > 7.0) return [20, 20, 22];
    if (d > 5.6) return [36, 36, 40];
    if (d > 5.0) return [120, 124, 132];
    let c = mezcla([26, 40, 70], [8, 12, 24], d / 5);
    if (Math.hypot(x - 5.5, y - 5) < 1.4) c = [190, 214, 240];
    if (Math.hypot(x - 9.5, y - 10) < 0.8) c = [110, 150, 190];
    return c;
  });
  // C: la pantalla abatible: marco y LCD apagado con un reflejo
  cuadrante(l, 2, (x, y) => {
    if (x < 1 || y < 1 || x > 14 || y > 14) return [34, 34, 38];
    let c = escala([18, 26, 32], 0.95 + hash(x, y, 31) * 0.08);
    if (x + y > 9 && x + y < 13) c = escala(c, 1.8);
    return c;
  });
  // D: la correa (goma con costura) y, en la esquina, el piloto rojo de REC
  cuadrante(l, 3, (x, y) => {
    if (x >= 12 && y >= 12) return [235, 40, 30];
    let c = escala([30, 26, 24], 0.9 + hash(x, y, 41) * 0.16);
    if ((x === 2 || x === 9) && y % 3 !== 0) c = [92, 86, 78];
    return c;
  });
  guardar(TEX_ITEM, 'camara_3d', l);
}
const REC = [14, 14, 16, 16];
const CAMARA = [
  cubo([5, 4, 3], [11, 11, 13], 'A'),                                  // cuerpo
  cubo([6, 5.5, 0], [10, 9.5, 3], 'D', { north: 'B' }, ['south']),     // objetivo (octogono)
  cubo([6.5, 5, 0.05], [9.5, 10, 3], 'D', { north: 'B' }, ['south']),
  cubo([6.5, 11, 9], [9.5, 13, 14], 'A', {}, ['down']),                // visor
  cubo([6.75, 11.25, 14], [9.25, 12.75, 15], 'D'),                     // ocular
  cubo([4.2, 5, 4], [5, 10, 10], 'A', { west: 'C' }, ['east']),        // pantalla abatible
  cubo([11, 5, 5], [12, 10, 11], 'D', {}, ['west']),                   // correa
  { from: [9.4, 11, 3.6], to: [10.2, 11.4, 4.4], faces: { up: { uv: REC, texture: '#t' }, north: { uv: REC, texture: '#t' }, east: { uv: REC, texture: '#t' }, west: { uv: REC, texture: '#t' }, south: { uv: REC, texture: '#t' } } }
];
json('models/item/camara.json', modelo('camara_3d', CAMARA,
  display({ mano: [0, -12, 0], manoT: [1, 3.5, -3], manoE: 0.42, terceraT: [0, 2.5, 1], terceraE: 0.55, gui: [25, 135, 0], guiE: 0.95 })));
json('items/camara.json', { model: { type: 'minecraft:model', model: `${NS}:item/camara` } });

/* ====================================================== agua de almendras */
{
  const l = new Lienzo(32);
  // A: el agua lechosa a traves del plastico, con un brillo vertical
  cuadrante(l, 0, (x, y) => {
    let c = escala([236, 228, 204], 0.93 + ruido(x, y, 4, 51, 16) * 0.08 - y * 0.006);
    if (x === 3 || x === 4) c = mezcla(c, [255, 255, 250], 0.6);
    if (x === 13) c = escala(c, 0.86);
    return c;
  });
  // B: la etiqueta: marron claro, una almendra y dos lineas de texto
  cuadrante(l, 1, (x, y) => {
    let c = escala([206, 160, 102], 0.95 + hash(x, y, 61) * 0.08);
    const ax = (x - 7.5) / 3.2;
    const ay = (y - 6.5) / 4.4;
    if (ax * ax + ay * ay < 1) c = mezcla([128, 80, 42], [168, 110, 60], (x - 4) / 8);
    if (y === 12 || y === 14) c = (x > 2 && x < 13) ? [92, 60, 34] : c;
    if (y === 0 || y === 15) c = [150, 104, 60];
    return c;
  });
  // C: el tapon azul, con estrias
  cuadrante(l, 2, (x, y) => escala([58, 106, 196], (x % 2 ? 0.86 : 1.05) * (0.95 + hash(x, y, 71) * 0.08)));
  // D: el cuello de plastico transparente
  cuadrante(l, 3, (x, y) => escala([214, 222, 228], (x < 5 ? 1.08 : 0.94) + hash(x, y, 81) * 0.05));
  guardar(TEX_ITEM, 'agua_almendras_3d', l);
}
const AGUA = [
  cubo([6, 0, 6.5], [10, 9, 9.5], 'A'),                                        // botella (dos cajas)
  cubo([6.5, 0, 6], [9.5, 9, 10], 'A'),
  cubo([5.95, 3, 6.45], [10.05, 6.5, 9.55], 'B', {}, ['up', 'down']),          // etiqueta
  cubo([6.45, 3, 5.95], [9.55, 6.5, 10.05], 'B', {}, ['up', 'down']),
  cubo([6.75, 9, 6.75], [9.25, 10, 9.25], 'A', {}, ['down']),                  // hombros
  cubo([7.25, 10, 7.25], [8.75, 11.5, 8.75], 'D', {}, ['down']),               // cuello
  cubo([7, 11.5, 7], [9, 12.75, 9], 'C')                                       // tapon
];
json('models/item/agua_almendras.json', modelo('agua_almendras_3d', AGUA,
  display({ mano: [0, -35, 0], manoT: [0, 4.5, 1], manoE: 0.6, tercera: [0, 0, 0], terceraT: [0, 3, 1], terceraE: 0.7, gui: [20, 30, 0], guiE: 1.15, guiT: [0, 1, 0] })));
json('items/agua_almendras.json', { model: { type: 'minecraft:model', model: `${NS}:item/agua_almendras` } });
