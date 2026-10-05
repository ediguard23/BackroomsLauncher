'use strict';
/**
 * Texturas y modelos de la expedicion (mod evento/): el ascensor de salida,
 * el vestibulo (butacas, sofas, maquinas de agua de almendras, taquillas,
 * puertas de los ascensores), la linterna, la camara, la comida y los logos
 * de la pantalla del auditorio.
 *
 *   npm run texturas
 *
 * Tambien rehace el modelo de los tubos fluorescentes con tintindex: el
 * cliente los tine de gris en el apagon y de rojo con la alarma.
 */

const fs = require('fs');
const path = require('path');
const png = require('../imagenes/png');
const { hash, ruido, escala, mezcla, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const RAIZ = path.join(__dirname, '..', '..');
const ASSETS = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', NS);
const TEX = path.join(ASSETS, 'textures', 'block');
const TEX_ITEM = path.join(ASSETS, 'textures', 'item');
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};
const t = (n) => `${NS}:block/${n}`;
const ti = (n) => `${NS}:item/${n}`;
const m = (n) => `${NS}:block/${n}`;
const FACINGS = { north: 0, east: 90, south: 180, west: 270 };
const caras = (tex, extra = {}) => {
  const o = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) o[d] = { texture: tex, ...extra };
  return o;
};
const conFacing = (modelo) => {
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) v[`facing=${f}`] = y ? { model: m(modelo), y } : { model: m(modelo) };
  return { variants: v };
};

console.log('Expedicion');

/* ============================================================== acero */

function acero (semilla = 0) {
  const l = new Lienzo(16);
  l.pintar((x, y) => {
    // cepillado horizontal: cada fila un poco distinta
    const fila = hash(0, y, 300 + semilla) * 0.08 + ruido(x, y, 8, 301 + semilla, 16) * 0.05;
    return escala([150, 154, 160], 0.88 + fila + hash(x, y, 302 + semilla) * 0.04);
  });
  // juntas del panel y remaches
  l.rect(0, 0, 15, 0, [190, 194, 198]);
  l.rect(0, 15, 15, 15, [92, 95, 100]);
  l.rect(0, 0, 0, 15, [176, 180, 184]);
  l.rect(15, 0, 15, 15, [100, 103, 108]);
  for (const [x, y] of [[2, 2], [13, 2], [2, 13], [13, 13]]) {
    l.punto(x, y, [205, 208, 212]);
    l.punto(x + 1, y + 1, [88, 90, 96]);
  }
  return l;
}
guardar(TEX, 'acero', acero());

// chapa lagrimada del suelo del ascensor: lagrimas cortas en espiga
guardar(TEX, 'ascensor_suelo', new Lienzo(16).pintar((x, y) => {
  let c = escala([112, 115, 120], 0.93 + hash(x, y, 310) * 0.08);
  const cx = x % 4;
  const cy = y % 4;
  const par = ((Math.floor(x / 4) + Math.floor(y / 4)) & 1) === 0;
  const en = par ? (cx === cy || cx === cy + 1) : (cx + cy === 3 || cx + cy === 2);
  if (en && cx > 0 && cy > 0 && cx < 4 && cy < 4) c = escala([176, 180, 186], 1);
  else if (en) c = escala([150, 153, 158], 1);
  return c;
}));

guardar(TEX, 'luz_ascensor', new Lienzo(16).pintar((x, y) => {
  if (x === 0 || y === 0 || x === 15 || y === 15) return [120, 124, 128];
  if (x % 5 === 0 || y % 5 === 0) return [228, 228, 220];
  return escala([252, 250, 238], 0.98 + hash(x, y, 320) * 0.02);
}));

// panel de botones: placa de acero con dos columnas de botones y la pantallita
{
  const l = acero(5);
  l.rect(4, 1, 11, 4, [20, 14, 12]);
  l.rect(6, 2, 9, 3, [255, 70, 50]);                 // "N0" en rojo (dos rayitas)
  for (let i = 0; i < 4; i++) {
    for (const x of [5, 9]) {
      const y = 6 + i * 2;
      l.rect(x, y, x + 1, y + 1, i === 3 ? [255, 190, 70] : [210, 200, 170]);
      l.punto(x + 1, y + 1, [90, 80, 60]);
    }
  }
  guardar(TEX, 'panel_ascensor', l);
}

/* ============================================================ vestibulo */

// terciopelo rojo de las butacas y tela de los sofas
const tela = (base, canal) => new Lienzo(16).pintar((x, y) => escala(base, 0.82 + ruido(x, y, 4, canal, 16) * 0.18 + hash(x, y, canal + 1) * 0.1));
guardar(TEX, 'terciopelo', tela([150, 22, 32], 330));
guardar(TEX, 'sofa_tela', tela([84, 98, 120], 340));
guardar(TEX, 'madera_oscura', new Lienzo(16).pintar((x, y) => escala([70, 46, 28], 0.85 + ruido(x, y * 0.2, 8, 350, 16) * 0.25 + hash(x, y, 351) * 0.05)));
guardar(TEX, 'metal_negro', new Lienzo(16).pintar((x, y) => escala([38, 38, 42], 0.9 + hash(x, y, 360) * 0.15)));

// maquina de agua de almendras: cuerpo rojo, cristal con botellas arriba, botonera y cajetin abajo
const ROJO = [176, 34, 32];
guardar(TEX, 'maquina_lado', new Lienzo(16).pintar((x, y) => escala(ROJO, 0.85 + (x < 2 ? 0.15 : 0) + hash(x, y, 370) * 0.08)));
{
  const l = new Lienzo(16).pintar((x, y) => escala(ROJO, 0.9 + hash(x, y, 371) * 0.08));
  l.rect(1, 1, 10, 15, [26, 30, 36]);               // cristal
  for (let fila = 0; fila < 3; fila++) {
    const y0 = 2 + fila * 5;
    l.rect(1, y0 + 4, 10, y0 + 4, [120, 124, 130]); // bandeja
    for (let b = 0; b < 3; b++) {
      const x0 = 2 + b * 3;
      l.rect(x0, y0 + 1, x0 + 1, y0 + 3, [236, 226, 196]); // botella de agua de almendras
      l.punto(x0, y0, [70, 120, 200]);                      // tapon azul
      l.punto(x0 + 1, y0 + 2, [190, 160, 110]);             // etiqueta
    }
  }
  l.rect(12, 2, 14, 3, [255, 236, 170]);              // luz del rotulo
  l.rect(12, 5, 14, 13, [60, 60, 64]);                // botonera
  for (let y = 6; y <= 12; y += 2) { l.punto(12, y, [220, 210, 180]); l.punto(14, y, [220, 210, 180]); }
  guardar(TEX, 'maquina_arriba', l);
}
{
  const l = new Lienzo(16).pintar((x, y) => escala(ROJO, 0.9 + hash(x, y, 372) * 0.08));
  l.rect(1, 0, 10, 4, [26, 30, 36]);
  l.rect(1, 4, 10, 4, [120, 124, 130]);
  l.rect(2, 1, 3, 3, [236, 226, 196]); l.rect(5, 1, 6, 3, [236, 226, 196]); l.rect(8, 1, 9, 3, [236, 226, 196]);
  l.rect(2, 7, 13, 9, [245, 236, 210]);               // franja con el rotulo
  l.rect(6, 7, 9, 9, [176, 120, 60]); l.rect(7, 7, 8, 7, [210, 160, 100]); // la almendra del rotulo
  l.rect(12, 1, 14, 3, [50, 50, 54]); l.punto(13, 2, [10, 10, 10]); // ranura de monedas
  l.rect(3, 11, 12, 14, [14, 14, 16]);                // cajetin de salida
  l.rect(3, 11, 12, 11, [70, 70, 74]);
  guardar(TEX, 'maquina_abajo', l);
}

// taquilla: metal gris azulado, rejillas, tirador y numero
guardar(TEX, 'taquilla_lado', new Lienzo(16).pintar((x, y) => escala([96, 108, 120], 0.9 + hash(x, y, 380) * 0.08)));
{
  const l = new Lienzo(16).pintar((x, y) => escala([104, 118, 132], 0.9 + hash(x, y, 381) * 0.07));
  l.rect(0, 0, 15, 0, [140, 152, 166]); l.rect(0, 0, 0, 15, [140, 152, 166]);
  l.rect(15, 0, 15, 15, [60, 68, 78]); l.rect(0, 15, 15, 15, [60, 68, 78]);
  for (let y = 2; y <= 6; y += 2) l.rect(4, y, 11, y, [40, 44, 50]);  // rejillas
  l.rect(12, 8, 13, 11, [190, 196, 204]);             // tirador
  l.rect(5, 9, 9, 11, [230, 226, 210]);               // placa del numero
  l.punto(6, 10, [40, 40, 40]); l.punto(8, 10, [40, 40, 40]);
  guardar(TEX, 'taquilla', l);
}

// puertas de los ascensores del vestibulo: acero cepillado y la junta del medio
for (const [n, junta] of [['puerta_ascensor_izq', 15], ['puerta_ascensor_der', 0]]) {
  const l = acero(n.length);
  l.rect(junta, 0, junta, 15, [30, 32, 36]);
  l.rect(0, 15, 15, 15, [120, 124, 128]);
  guardar(TEX, n, l);
}

/* ========================================================== modelos */

const modelos = {};
modelos.acero = { parent: 'minecraft:block/cube_all', textures: { all: t('acero') } };
modelos.ascensor_suelo = { parent: 'minecraft:block/cube_all', textures: { all: t('ascensor_suelo') } };
modelos.luz_ascensor = {
  parent: 'minecraft:block/block',
  textures: { all: t('luz_ascensor'), particle: t('luz_ascensor') },
  elements: [{ from: [0, 0, 0], to: [16, 16, 16], shade: false, light_emission: 15, faces: caras('#all', { tintindex: 0 }) }]
};
modelos.panel_ascensor = {
  parent: 'minecraft:block/block',
  textures: { all: t('panel_ascensor'), lado: t('acero'), particle: t('acero') },
  elements: [{
    from: [4, 1, 15], to: [12, 15, 16], light_emission: 6,
    faces: { north: { texture: '#all', uv: [0, 0, 16, 16] }, south: { texture: '#lado' }, east: { texture: '#lado' }, west: { texture: '#lado' }, up: { texture: '#lado' }, down: { texture: '#lado' } }
  }]
};
// butaca del auditorio: mira al norte (el respaldo al sur). Todo recto y dentro
// de su bloque: en fila, los reposabrazos de una tocan los de la siguiente
modelos.butaca = {
  parent: 'minecraft:block/block',
  textures: { tela: t('terciopelo'), metal: t('metal_negro'), madera: t('madera_oscura'), particle: t('terciopelo') },
  elements: [
    { from: [0, 0, 3], to: [1.5, 9, 15], faces: caras('#metal') },
    { from: [14.5, 0, 3], to: [16, 9, 15], faces: caras('#metal') },
    { from: [0, 9, 3], to: [1.5, 10, 15], faces: caras('#madera') },
    { from: [14.5, 9, 3], to: [16, 10, 15], faces: caras('#madera') },
    { from: [1.5, 2, 3], to: [14.5, 4, 13], faces: caras('#metal') },
    { from: [1.5, 4, 2], to: [14.5, 7, 13], faces: caras('#tela') },
    { from: [1.5, 4, 13], to: [14.5, 19, 15], faces: caras('#tela') },
    { from: [1.5, 5, 15], to: [14.5, 18, 15.5], faces: caras('#metal') }
  ]
};
// sofa del vestibulo: mira al norte
modelos.sofa = {
  parent: 'minecraft:block/block',
  textures: { tela: t('sofa_tela'), madera: t('madera_oscura'), particle: t('sofa_tela') },
  elements: [
    { from: [0, 0, 1], to: [16, 2, 15], faces: caras('#madera') },
    { from: [0, 2, 1], to: [16, 7, 15], faces: caras('#tela') },
    { from: [0.5, 7, 1.5], to: [15.5, 9, 12], faces: caras('#tela') },
    { from: [0, 7, 12], to: [16, 18, 16], faces: caras('#tela') }
  ]
};
const frontal = (frente, lado, arriba, emision = 0) => ({
  parent: 'minecraft:block/block',
  textures: { frente: t(frente), lado: t(lado), arriba: t(arriba), particle: t(lado) },
  elements: [{
    from: [0, 0, 0], to: [16, 16, 16], ...(emision ? { light_emission: emision } : {}),
    faces: {
      north: { texture: '#frente', cullface: 'north' }, south: { texture: '#lado', cullface: 'south' },
      east: { texture: '#lado', cullface: 'east' }, west: { texture: '#lado', cullface: 'west' },
      up: { texture: '#arriba', cullface: 'up' }, down: { texture: '#arriba', cullface: 'down' }
    }
  }]
});
modelos.maquina_abajo = frontal('maquina_abajo', 'maquina_lado', 'metal_negro');
modelos.maquina_arriba = frontal('maquina_arriba', 'maquina_lado', 'metal_negro', 6);
modelos.taquilla = frontal('taquilla', 'taquilla_lado', 'taquilla_lado');
modelos.puerta_ascensor_izq = frontal('puerta_ascensor_izq', 'acero', 'acero');
modelos.puerta_ascensor_der = frontal('puerta_ascensor_der', 'acero', 'acero');

// los tubos de siempre, ahora con tintindex (gris en el apagon, rojo con la alarma)
modelos.fluorescente = {
  parent: 'minecraft:block/block',
  textures: { all: t('fluorescente'), particle: t('fluorescente') },
  elements: [{
    from: [0, 0, 0], to: [16, 16, 16], shade: false, light_emission: 15,
    faces: Object.fromEntries(['down', 'up', 'north', 'south', 'east', 'west'].map((d) => [d, { texture: '#all', cullface: d, tintindex: 0 }]))
  }]
};
for (const [n, mo] of Object.entries(modelos)) json(`models/block/${n}.json`, mo);

const estados = {
  acero: { variants: { '': { model: m('acero') } } },
  ascensor_suelo: { variants: { '': { model: m('ascensor_suelo') } } },
  luz_ascensor: { variants: { '': { model: m('luz_ascensor') } } },
  panel_ascensor: conFacing('panel_ascensor'),
  butaca: conFacing('butaca'),
  sofa: conFacing('sofa'),
  maquina_abajo: conFacing('maquina_abajo'),
  maquina_arriba: conFacing('maquina_arriba'),
  taquilla: conFacing('taquilla'),
  puerta_ascensor_izq: conFacing('puerta_ascensor_izq'),
  puerta_ascensor_der: conFacing('puerta_ascensor_der')
};
for (const [n, e] of Object.entries(estados)) {
  json(`blockstates/${n}.json`, e);
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: m(n) } });
}

/* ============================================================ objetos */

// linterna amarilla (como la de las grabaciones de la expedicion), apagada y encendida
function linterna (encendida) {
  const l = new Lienzo(16);
  const pon = (x, y, c) => l.punto(x, y, c);
  // cuerpo en diagonal, de 2 pixeles de grueso, con luz arriba y sombra abajo
  for (let i = 0; i < 7; i++) {
    const x = 3 + i;
    const y = 12 - i;
    pon(x, y, [255, 214, 70]);
    pon(x + 1, y, [236, 188, 36]);
    pon(x, y + 1, [190, 146, 20]);
    pon(x + 1, y + 1, [214, 166, 28]);
  }
  pon(5, 10, [40, 40, 44]); pon(6, 10, [40, 40, 44]);       // boton
  pon(2, 13, [44, 44, 48]); pon(3, 13, [60, 60, 64]); pon(2, 14, [30, 30, 34]); pon(3, 14, [44, 44, 48]); // tapa
  // cabeza: un cuadrado oscuro girado y la lente
  for (const [x, y] of [[10, 5], [11, 5], [10, 6], [11, 4], [12, 4], [12, 5], [11, 6], [12, 3], [13, 3], [13, 4], [10, 4], [11, 3]]) pon(x, y, [52, 52, 58]);
  for (const [x, y] of [[9, 6], [10, 7], [13, 2], [14, 3], [14, 2]]) pon(x, y, [34, 34, 38]);
  const lente = encendida ? [255, 250, 214] : [148, 160, 174];
  for (const [x, y] of [[12, 2], [13, 1], [14, 1], [15, 2], [14, 3], [13, 2], [14, 2]]) pon(x, y, lente);
  pon(13, 2, encendida ? [255, 255, 255] : [210, 222, 236]);
  if (encendida) {
    pon(15, 0, [255, 240, 170]); pon(12, 0, [255, 240, 170]); pon(15, 4, [255, 240, 170]);
  }
  return l;
}
guardar(TEX_ITEM, 'linterna', linterna(false));
guardar(TEX_ITEM, 'linterna_encendida', linterna(true));

// videocamara: cuerpo negro, objetivo, correa y el piloto rojo de REC
{
  const l = new Lienzo(16);
  l.rect(3, 5, 12, 11, [36, 36, 40]);
  l.rect(4, 4, 11, 4, [56, 56, 60]);
  l.rect(12, 6, 14, 10, [24, 24, 28]);                     // objetivo
  l.rect(13, 7, 14, 9, [70, 90, 120]);
  l.punto(14, 7, [170, 200, 230]);
  l.rect(1, 6, 3, 9, [50, 50, 54]);                        // visor
  l.rect(4, 12, 11, 12, [90, 70, 50]);                     // correa
  l.punto(10, 5, [230, 40, 30]);                            // REC
  l.rect(5, 7, 9, 9, [48, 48, 52]);
  l.rect(5, 8, 8, 8, [150, 150, 150]);                     // marca
  guardar(TEX_ITEM, 'camara', l);
}

// agua de almendras: botella con el liquido blanquecino, tapon azul y etiqueta
{
  const l = new Lienzo(16);
  l.rect(6, 1, 9, 2, [60, 110, 200]);                      // tapon
  l.rect(6, 3, 9, 4, [210, 220, 225], 0.8);                // cuello
  l.rect(4, 5, 11, 14, [238, 230, 205]);                   // liquido
  l.rect(4, 5, 4, 14, [255, 252, 240]);                    // brillo
  l.rect(11, 5, 11, 14, [200, 190, 165]);
  l.rect(4, 8, 11, 11, [196, 150, 96]);                    // etiqueta de almendra
  l.rect(6, 9, 9, 10, [120, 76, 40]);
  l.rect(5, 15, 10, 15, [180, 172, 150]);
  guardar(TEX_ITEM, 'agua_almendras', l);
}

// galletas: paquete azul abierto con galletas asomando
{
  const l = new Lienzo(16);
  l.rect(2, 6, 13, 13, [60, 90, 170]);
  l.rect(2, 6, 13, 6, [90, 120, 200]);
  l.rect(4, 9, 11, 10, [240, 220, 120]);                   // rotulo
  for (const [cx, cy] of [[5, 4], [9, 3], [12, 5]]) {
    l.disco(cx, cy, 2, [196, 140, 70]);
    l.punto(cx - 1, cy, [100, 60, 30]); l.punto(cx + 1, cy - 1, [100, 60, 30]);
  }
  guardar(TEX_ITEM, 'galletas', l);
}

// porcion de pizza
{
  const l = new Lienzo(16);
  l.poligono([[2, 3], [14, 3], [8, 15]], [240, 196, 80]);  // queso
  l.rect(2, 2, 14, 3, [200, 130, 60]);                     // borde
  l.rect(3, 1, 13, 1, [222, 156, 80]);
  for (const [cx, cy] of [[5.5, 5.5], [10.5, 5.5], [8, 9.5]]) { l.disco(cx, cy, 1.5, [178, 44, 36]); l.punto(Math.floor(cx), Math.floor(cy), [214, 80, 60]); }
  l.punto(5, 8, [90, 140, 60]); l.punto(11, 8, [90, 140, 60]);
  guardar(TEX_ITEM, 'pizza', l);
}

const generado = (tex, padre = 'minecraft:item/generated') => ({ parent: padre, textures: { layer0: ti(tex) } });
json('models/item/linterna.json', generado('linterna', 'minecraft:item/handheld'));
json('models/item/linterna_encendida.json', generado('linterna_encendida', 'minecraft:item/handheld'));
json('items/linterna.json', {
  model: {
    type: 'minecraft:condition', property: 'minecraft:custom_model_data', index: 0,
    on_true: { type: 'minecraft:model', model: `${NS}:item/linterna_encendida` },
    on_false: { type: 'minecraft:model', model: `${NS}:item/linterna` }
  }
});
for (const n of ['camara', 'agua_almendras', 'galletas', 'pizza']) {
  json(`models/item/${n}.json`, generado(n));
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/${n}` } });
}

/* =============================================== logos de la pantalla */

// se reescalan a 512x512 (potencia de dos: si no, el atlas pierde los mipmaps de todo)
function reescalar (origen, nombre) {
  const img = png.leer(fs.readFileSync(origen));
  const L = 512;
  const k = Math.min(L / img.width, L / img.height);
  const w = Math.round(img.width * k);
  const h = Math.round(img.height * k);
  const ox = Math.floor((L - w) / 2);
  const oy = Math.floor((L - h) / 2);
  const out = new Lienzo(L);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      // media de los pixeles de origen que caen en este (reduccion limpia)
      const sx0 = Math.floor(x / k);
      const sy0 = Math.floor(y / k);
      const sx1 = Math.max(sx0 + 1, Math.floor((x + 1) / k));
      const sy1 = Math.max(sy0 + 1, Math.floor((y + 1) / k));
      let r = 0; let g = 0; let b = 0; let a = 0; let n = 0;
      for (let yy = sy0; yy < sy1 && yy < img.height; yy++) {
        for (let xx = sx0; xx < sx1 && xx < img.width; xx++) {
          const i = (yy * img.width + xx) * 4;
          const al = img.data[i + 3];
          r += img.data[i] * al; g += img.data[i + 1] * al; b += img.data[i + 2] * al; a += al; n++;
        }
      }
      if (a > 0) {
        const i = ((oy + y) * L + ox + x) * 4;
        out.data[i] = Math.round(r / a); out.data[i + 1] = Math.round(g / a); out.data[i + 2] = Math.round(b / a);
        out.data[i + 3] = Math.round(a / n);
      }
    }
  }
  guardar(TEX, nombre, out);
}
const GUI = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', 'backrooms', 'textures', 'gui');
reescalar(path.join(GUI, 'logo_0.png'), 'logo_pantalla');
reescalar(path.join(GUI, 'peakmc_studio_0.png'), 'logo_peakmc');
for (const n of ['logo_pantalla', 'logo_peakmc']) {
  json(`models/item/${n}.json`, {
    textures: { logo: t(n), particle: t(n) },
    elements: [{ from: [0, 0, 8], to: [16, 16, 8], shade: false, light_emission: 15, faces: { north: { texture: '#logo', uv: [16, 0, 0, 16] }, south: { texture: '#logo', uv: [0, 0, 16, 16] } } }]
  });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/${n}` } });
}

/* ============================================================ nombres */

const NOMBRES = {
  'block.acero': ['Acero', 'Steel panel'],
  'block.ascensor_suelo': ['Suelo de ascensor', 'Elevator floor'],
  'block.luz_ascensor': ['Luz de ascensor', 'Elevator light'],
  'block.panel_ascensor': ['Panel del ascensor', 'Elevator panel'],
  'block.butaca': ['Butaca', 'Theater seat'],
  'block.sofa': ['Sofá', 'Sofa'],
  'block.maquina_abajo': ['Máquina de agua (abajo)', 'Vending machine (bottom)'],
  'block.maquina_arriba': ['Máquina de agua (arriba)', 'Vending machine (top)'],
  'block.taquilla': ['Taquilla', 'Locker'],
  'block.puerta_ascensor_izq': ['Puerta de ascensor (izquierda)', 'Elevator door (left)'],
  'block.puerta_ascensor_der': ['Puerta de ascensor (derecha)', 'Elevator door (right)'],
  'item.linterna': ['Linterna', 'Flashlight'],
  'item.camara': ['Cámara', 'Camcorder'],
  'item.agua_almendras': ['Agua de almendras', 'Almond water'],
  'item.galletas': ['Galletas', 'Cookies'],
  'item.pizza': ['Pizza', 'Pizza'],
  'entity.asiento': ['Asiento', 'Seat'],
  'entity.comida': ['Comida', 'Food'],
  'entity.bacteria': ['La Bacteria', 'The Bacteria'],
  'entity.smiler': ['Smiler', 'Smiler']
};
const EXTRA = {
  'key.backrooms_evento.linterna': ['Linterna', 'Flashlight'],
  'key.backrooms_evento.camara': ['Cámara', 'Camcorder'],
  'key.category.backrooms_evento.backrooms': ['Backrooms', 'Backrooms']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [clave, v] of Object.entries(NOMBRES)) {
    const [tipo, id] = clave.split('.');
    lang[`${tipo}.${NS}.${id}`] = v[k];
  }
  for (const [clave, v] of Object.entries(EXTRA)) lang[clave] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
