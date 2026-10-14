'use strict';
/**
 * Bloques de las Piscinas (Nivel 37): texturas, modelos, estados, objetos y nombres.
 * Todo dibujado por codigo a 32x32 y sin costuras.
 *
 *   node tools/texturas/piscinas.js
 *
 * El azulejo es de 4x4 piezas por bloque (como en las referencias: piezas pequenas
 * y la junta muy marcada), esmaltado: cada pieza tiene su brillo y su bisel. El fondo
 * de la piscina y las franjas llevan causticas animadas (la luz que el agua mueve
 * sobre los azulejos); los focos de debajo del agua, una lente que brilla sola.
 */

const fs = require('fs');
const path = require('path');
const { hash, fbm, ruido, clamp, mezcla, escala, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const RAIZ = path.join(__dirname, '..', '..');
const ASSETS = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', NS);
const TEX = path.join(ASSETS, 'textures', 'block');
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};
const t = (n) => `${NS}:block/${n}`;

const L = 32;
const PASO = 8; // una pieza cada 8 px: 4x4 por bloque

console.log('Piscinas');

/* ============================================================ azulejo */

/**
 * Pieza de azulejo esmaltado en (x, y): devuelve {c, junta, px, py, tx, ty} con el
 * color de la pieza (o de la junta), la posicion dentro de la pieza y la pieza.
 */
function pieza (x, y, base, junta, canal) {
  const px = x % PASO;
  const py = y % PASO;
  const tx = Math.floor(x / PASO);
  const ty = Math.floor(y / PASO);
  // la junta va a los dos lados de cada pieza (1 px y 1 px: 2 entre piezas y entre bloques),
  // asi la textura es simetrica: las caras de los lados, que Minecraft pinta como en un
  // espejo respecto a la de arriba, casan en las esquinas y todas las lineas miden igual
  if (px === 0 || py === 0 || px === PASO - 1 || py === PASO - 1) {
    const c = escala(junta, 0.94 + hash(x, y, canal + 1) * 0.1);
    return { c, junta: true, px, py, tx, ty };
  }
  // cada pieza con su tono, un degradado de esmalte y un bisel suave (que no engorde la junta)
  let c = escala(base, 0.975 + hash(tx, ty, canal + 2) * 0.05);
  const d = (px + py - 2) / (2 * (PASO - 3));
  c = escala(c, 1.02 - d * 0.04);
  if (px === 1 || py === 1) c = escala(c, 1.02);
  if (px === PASO - 2 || py === PASO - 2) c = escala(c, 0.975);
  // reflejo del esmalte: un punto claro en algunas piezas
  if (px === 2 && py === 2 && hash(tx, ty, canal + 3) > 0.35) c = mezcla(c, [255, 255, 255], 0.5);
  c = escala(c, 0.99 + fbm(x, y, canal + 4) * 0.02);
  return { c, junta: false, px, py, tx, ty };
}

const BLANCO = [234, 242, 240];
const JUNTA = [180, 197, 200];

function azulejo (x, y) {
  return pieza(x, y, BLANCO, JUNTA, 700).c;
}
guardar(TEX, 'azulejo', new Lienzo(L).pintar(azulejo));

// sucio: juntas negras de mugre, cal amarillenta, chorretones que bajan y alguna mancha de moho
guardar(TEX, 'azulejo_sucio', new Lienzo(L).pintar((x, y) => {
  const p = pieza(x, y, BLANCO, [112, 118, 98], 710);
  let c = p.c;
  const cal = fbm(x, y, 711);
  if (cal > 0.5) c = mezcla(c, [206, 196, 150], Math.min(0.6, (cal - 0.5) * 2.2));
  const chorro = ruido(x, y * 0.2, 8, 713, L);
  if (chorro > 0.6) c = mezcla(c, [150, 140, 104], Math.min(0.5, (chorro - 0.6) * 2.2) * (0.4 + y / L));
  const moho = fbm(x + 7, y + 3, 715);
  if (moho > 0.66) c = mezcla(c, [70, 82, 64], Math.min(0.7, (moho - 0.66) * 4));
  if (p.junta) c = escala(c, 0.85);
  return c;
}));

// roto: tres piezas caidas (se ve el cemento con las estrias de la cola) y grietas
{
  const caidas = new Set(['1,1', '2,1', '2,2', '0,3']);
  guardar(TEX, 'azulejo_roto', new Lienzo(L).pintar((x, y) => {
    const tx = Math.floor(x / PASO);
    const ty = Math.floor(y / PASO);
    if (caidas.has(`${tx},${ty}`)) {
      // cemento: gris, rugoso, con las estrias en diagonal de la llana
      let c = escala([150, 150, 142], 0.85 + hash(x, y, 721) * 0.2 + fbm(x, y, 722) * 0.1);
      if ((x + y) % 4 === 0) c = escala(c, 0.86);
      // sombra del borde de las piezas de alrededor (arriba e izquierda)
      const px = x % PASO;
      const py = y % PASO;
      const arriba = !caidas.has(`${tx},${ty - 1}`) && py <= 1;
      const izq = !caidas.has(`${tx - 1},${ty}`) && px <= 1;
      if (arriba || izq) c = escala(c, 0.62);
      return c;
    }
    let c = azulejo(x, y);
    // grietas que salen del hueco
    const g = Math.abs(ruido(x * 1.6, y * 0.9, 4, 723, L) - 0.5);
    if (g < 0.018 && x > 4 && y > 4) c = escala(c, 0.55);
    return c;
  }));
}

// azul: piezas turquesa oscuro con la junta clara
guardar(TEX, 'azulejo_azul', new Lienzo(L).pintar((x, y) => pieza(x, y, [58, 156, 168], [168, 204, 206], 730).c));

// borde de la piscina: blanco con la junta azul (como la sala de la referencia de la entidad)
guardar(TEX, 'borde_piscina', new Lienzo(L).pintar((x, y) => pieza(x, y, BLANCO, [52, 92, 186], 740).c));

/* ============================================================ causticas */

/**
 * Causticas en un bloque de 32 px que se repite: bordes de celdas de Voronoi (F2 - F1
 * pequeno = linea de luz) con los puntos dando vueltas, cerradas en `n` fotogramas.
 */
function causticas (x, y, f, n, canal, celdas) {
  const fase = (f / n) * Math.PI * 2;
  const tam = L / celdas;
  const gx = x / tam;
  const gy = y / tam;
  const ix = Math.floor(gx);
  const iy = Math.floor(gy);
  let f1 = 9;
  let f2 = 9;
  for (let dy = -1; dy <= 1; dy++) {
    for (let dx = -1; dx <= 1; dx++) {
      const cx = ix + dx;
      const cy = iy + dy;
      const mx = ((cx % celdas) + celdas) % celdas;
      const my = ((cy % celdas) + celdas) % celdas;
      const a = hash(mx, my, canal) * Math.PI * 2;
      const r = 0.18 + hash(mx, my, canal + 1) * 0.16;
      const vx = cx + 0.5 + Math.cos(fase + a) * r;
      const vy = cy + 0.5 + Math.sin(fase * (hash(mx, my, canal + 2) > 0.5 ? 1 : -1) + a) * r;
      const d = Math.hypot(gx - vx, gy - vy);
      if (d < f1) { f2 = f1; f1 = d; } else if (d < f2) f2 = d;
    }
  }
  const e = f2 - f1;
  return Math.max(0, 1 - e / 0.16);
}

function luzAgua (x, y, f, n) {
  const a = causticas(x, y, f, n, 750, 3);
  const b = causticas(x, y, (f + n / 3) % n, n, 760, 4);
  return Math.min(1, a * 0.7 + b * 0.5 + a * b * 0.6);
}

const FOTOGRAMAS = 16;
function animada (nombre, fn) {
  const cuadros = [];
  for (let f = 0; f < FOTOGRAMAS; f++) cuadros.push(new Lienzo(L).pintar((x, y) => fn(x, y, f)));
  guardar(TEX, nombre, Lienzo.tira(cuadros));
  fs.writeFileSync(path.join(TEX, `${nombre}.png.mcmeta`), JSON.stringify({ animation: { frametime: 3, interpolate: true } }, null, 2) + '\n');
}

// fondo: piezas claras (el agua turquesa les da el color) con la luz que baila
animada('fondo_piscina', (x, y, f) => {
  const p = pieza(x, y, [214, 238, 236], [150, 196, 196], 770);
  return mezcla(p.c, [255, 255, 250], luzAgua(x, y, f, FOTOGRAMAS) * 0.55);
});

// franja de las calles: azul oscuro, con la misma luz encima
animada('franja_piscina', (x, y, f) => {
  const p = pieza(x, y, [44, 70, 150], [96, 124, 188], 780);
  return mezcla(p.c, [190, 220, 255], luzAgua(x, y, f, FOTOGRAMAS) * 0.45);
});

/* ============================================================ luces */

// panel del techo: marco gris claro y difusor casi blanco, frio, con la reticula del prisma
guardar(TEX, 'luz_techo', new Lienzo(L).pintar((x, y) => {
  if (x < 3 || y < 3 || x > 28 || y > 28) {
    if (x === 0 || y === 0 || x === 31 || y === 31) return [150, 160, 162];
    return [206, 214, 216];
  }
  let c = [240, 250, 255];
  if (x % 4 === 1 || y % 4 === 1) c = escala(c, 0.965);
  const centro = 1 - Math.hypot(x - 15.5, y - 15.5) / 22;
  return escala(c, 0.95 + centro * 0.05);
}));

// claraboya: cristal esmerilado muy claro con el marco en cruz
guardar(TEX, 'claraboya', new Lienzo(L).pintar((x, y) => {
  if (x <= 1 || y <= 1 || x >= 30 || y >= 30 || x === 15 || x === 16 || y === 15 || y === 16) {
    return x === 0 || y === 0 || x === 31 || y === 31 ? [176, 188, 192] : [214, 224, 228];
  }
  const grano = 0.985 + hash(x, y, 790) * 0.03;
  return escala([250, 253, 255], grano);
}));

// foco de debajo del agua: azulejo con el aro cromado (base) y la lente que brilla (aparte)
guardar(TEX, 'foco_piscina', new Lienzo(L).pintar((x, y) => {
  const d = Math.hypot(x - 15.5, y - 15.5);
  if (d < 6.2) return [200, 248, 244]; // la tapa la pinta la lente
  if (d < 9.2) {
    const brillo = 0.75 + 0.25 * Math.cos(Math.atan2(y - 15.5, x - 15.5) + 2.2);
    let c = escala([196, 206, 210], brillo);
    if (d > 8.4) c = escala(c, 0.7);
    return c;
  }
  return azulejo(x, y);
}));
guardar(TEX, 'foco_piscina_lente', new Lienzo(16).pintar((x, y) => {
  const d = Math.hypot(x - 7.5, y - 7.5);
  const c = mezcla([255, 255, 250], [150, 236, 240], Math.min(1, d / 8));
  if ((x + y) % 5 === 0 && d > 3) return escala(c, 0.94); // las aristas del cristal
  return c;
}));

// rejilla del desague: marco cromado y ranuras oscuras sobre el azulejo
guardar(TEX, 'rejilla', new Lienzo(L).pintar((x, y) => {
  if (x >= 6 && x <= 25 && y >= 6 && y <= 25) {
    if (x === 6 || y === 6 || x === 25 || y === 25) return [186, 194, 198];
    if (x % 3 === 0 && y > 7 && y < 24) return [28, 36, 38];
    return escala([168, 178, 182], 0.9 + hash(x, y, 795) * 0.1);
  }
  return azulejo(x, y);
}));

/* ============================================================ modelos */

const cubo = (n) => ({ parent: 'minecraft:block/cube_all', textures: { all: t(n) } });

function emisivo (n) {
  const cara = (d) => ({ texture: '#all', cullface: d });
  return {
    parent: 'minecraft:block/block',
    textures: { all: t(n), particle: t(n) },
    elements: [{
      from: [0, 0, 0],
      to: [16, 16, 16],
      shade: false,
      light_emission: 15,
      faces: { down: cara('down'), up: cara('up'), north: cara('north'), south: cara('south'), east: cara('east'), west: cara('west') }
    }]
  };
}

/** El foco: cubo de azulejo con el aro y, en cada cara de los lados, la lente que brilla un pelo por delante. */
function foco () {
  const cara = (d) => ({ texture: '#base', cullface: d });
  const lente = { texture: '#lente', uv: [0, 0, 16, 16] };
  const els = [{
    from: [0, 0, 0],
    to: [16, 16, 16],
    faces: { down: cara('down'), up: cara('up'), north: cara('north'), south: cara('south'), east: cara('east'), west: cara('west') }
  }];
  const r0 = 4.9;
  const r1 = 11.1;
  els.push({ from: [r0, r0, -0.05], to: [r1, r1, 0], shade: false, light_emission: 15, faces: { north: { ...lente, cullface: 'north' } } });
  els.push({ from: [r0, r0, 16], to: [r1, r1, 16.05], shade: false, light_emission: 15, faces: { south: { ...lente, cullface: 'south' } } });
  els.push({ from: [-0.05, r0, r0], to: [0, r1, r1], shade: false, light_emission: 15, faces: { west: { ...lente, cullface: 'west' } } });
  els.push({ from: [16, r0, r0], to: [16.05, r1, r1], shade: false, light_emission: 15, faces: { east: { ...lente, cullface: 'east' } } });
  return { parent: 'minecraft:block/block', textures: { base: t('foco_piscina'), lente: t('foco_piscina_lente'), particle: t('foco_piscina') }, elements: els };
}

const SIMPLES = ['azulejo', 'azulejo_sucio', 'azulejo_roto', 'azulejo_azul', 'fondo_piscina', 'franja_piscina', 'borde_piscina', 'rejilla'];
for (const n of SIMPLES) json(`models/block/${n}.json`, cubo(n));
for (const n of ['luz_techo', 'claraboya']) json(`models/block/${n}.json`, emisivo(n));
json('models/block/foco_piscina.json', foco());
for (const n of [...SIMPLES, 'luz_techo', 'claraboya', 'foco_piscina']) {
  json(`blockstates/${n}.json`, { variants: { '': { model: t(n) } } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
}

// losa de azulejo
const lados = { bottom: t('azulejo'), top: t('azulejo'), side: t('azulejo') };
json('models/block/losa_azulejo.json', { parent: 'minecraft:block/slab', textures: lados });
json('models/block/losa_azulejo_arriba.json', { parent: 'minecraft:block/slab_top', textures: lados });
json('blockstates/losa_azulejo.json', {
  variants: {
    'type=bottom': { model: t('losa_azulejo') },
    'type=top': { model: t('losa_azulejo_arriba') },
    'type=double': { model: t('azulejo') }
  }
});
json('items/losa_azulejo.json', { model: { type: 'minecraft:model', model: t('losa_azulejo') } });

// escalon de azulejo: los 40 estados como los de las escaleras de piedra de Minecraft
json('models/block/escalon_azulejo.json', { parent: 'minecraft:block/stairs', textures: lados });
json('models/block/escalon_azulejo_dentro.json', { parent: 'minecraft:block/inner_stairs', textures: lados });
json('models/block/escalon_azulejo_fuera.json', { parent: 'minecraft:block/outer_stairs', textures: lados });
{
  const giro = { east: 0, south: 90, west: 180, north: 270 };
  const variantes = {};
  for (const [facing, g] of Object.entries(giro)) {
    for (const half of ['bottom', 'top']) {
      for (const shape of ['straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right']) {
        const modelo = shape === 'straight' ? 'escalon_azulejo' : shape.startsWith('inner') ? 'escalon_azulejo_dentro' : 'escalon_azulejo_fuera';
        let y = g;
        if (half === 'bottom' && shape.endsWith('left')) y = g - 90;
        if (half === 'top' && shape.endsWith('right')) y = g + 90;
        y = ((y % 360) + 360) % 360;
        const v = { model: t(modelo) };
        if (half === 'top') v.x = 180;
        if (y) v.y = y;
        if (v.x || v.y) v.uvlock = true;
        variantes[`facing=${facing},half=${half},shape=${shape}`] = v;
      }
    }
  }
  json('blockstates/escalon_azulejo.json', { variants: variantes });
}
json('items/escalon_azulejo.json', { model: { type: 'minecraft:model', model: t('escalon_azulejo') } });
console.log('  modelos, estados y objetos');

/* ================================================ detalles (pocos, sueltos) */
// Lo que dice que alguien estuvo aqui antes. El generador los pone muy de vez en cuando
// (PlanoPiscinas#detalle): casi todo el nivel sigue limpio.

const FACINGS = { north: 0, east: 90, south: 180, west: 270 };
const SANGRE = [104, 20, 14];
const SECA = [66, 18, 12];

// azulejo con una mancha oscura (humedad, moho) y azulejo con una manchita de sangre
guardar(TEX, 'azulejo_manchado', new Lienzo(L).pintar((x, y) => {
  let c = azulejo(x, y);
  const m = fbm(x * 0.8, y * 0.8, 800);
  const caida = ruido(x, y * 0.25, 8, 801, L) * (y / L);
  const t = Math.max(0, (m - 0.48) * 3) + Math.max(0, (caida - 0.55) * 2.5);
  if (t > 0) c = mezcla(c, [52, 56, 44], Math.min(0.8, t));
  return c;
}));
guardar(TEX, 'azulejo_sangre', new Lienzo(L).pintar((x, y) => {
  let c = azulejo(x, y);
  // en una sola pieza: una mancha seca y un hilo que cae hasta la junta
  const d = Math.hypot(x - 12.5, y - 11.5);
  if (d < 2.6 + ruido(x, y, 8, 802, L) * 1.6) c = mezcla(c, SECA, 0.85);
  else if (x === 12 && y > 12 && y < 16) c = mezcla(c, SECA, 0.7);
  return c;
}));

// el panel del techo apagado (el que parpadea, cuando se va)
guardar(TEX, 'luz_techo_apagada', new Lienzo(L).pintar((x, y) => {
  if (x < 3 || y < 3 || x > 28 || y > 28) {
    if (x === 0 || y === 0 || x === 31 || y === 31) return [150, 160, 162];
    return [206, 214, 216];
  }
  let c = escala([150, 158, 162], 0.94 + fbm(x, y, 805) * 0.1);
  if (x % 4 === 1 || y % 4 === 1) c = escala(c, 0.96);
  return c;
}));

/* ---- marcas en la pared (64x64, transparentes) */

const MARCAS = [];
function marca (nombre, l) {
  MARCAS.push(nombre);
  guardar(TEX, `marca_${nombre}`, l);
}

/** Trazo de dedo: grueso, irregular, y que gotea hacia abajo de vez en cuando. */
function dedo (l, pts, color, g, canal) {
  for (let i = 0; i + 1 < pts.length; i++) {
    const [x0, y0] = pts[i];
    const [x1, y1] = pts[i + 1];
    l.linea(x0, y0, x1, y1, color, g * (0.8 + hash(i, canal, 811) * 0.5), 0.9, 1.4);
  }
  for (let i = 0; i < pts.length; i += 3) {
    if (hash(i, canal, 812) < 0.45) {
      const [x, y] = pts[i];
      const largo = 5 + hash(i, canal, 813) * 18;
      for (let k = 0; k < largo; k++) l.disco(x + Math.sin(k * 0.3) * 0.3, y + k, Math.max(0.5, 1.3 - k / largo), color, 0.85);
      l.disco(x, y + largo, 1.4, color, 0.85);
    }
  }
}

/** Una letra a mano (C, O, R, E) en la caja (x, y, ancho, alto), algo torcida. */
function letraMano (l, ch, x, y, w, h, giro, color, canal) {
  const rot = ([u, v]) => {
    const px = (u - 0.5) * w;
    const py = (v - 0.5) * h;
    return [x + w / 2 + px * Math.cos(giro) - py * Math.sin(giro), y + h / 2 + px * Math.sin(giro) + py * Math.cos(giro)];
  };
  const arco = (a0, a1, n = 14) => Array.from({ length: n + 1 }, (_, i) => {
    const a = a0 + (a1 - a0) * i / n;
    return [0.5 + 0.48 * Math.cos(a), 0.5 + 0.5 * Math.sin(a)];
  });
  const trazos = {
    C: [arco(0.7, Math.PI * 2 - 0.7, 16)],   // un arco abierto a la derecha
    O: [arco(0, Math.PI * 2, 18)],
    R: [[[0.1, 1.0], [0.1, 0.0]], [[0.1, 0.02], [0.6, 0.02], [0.88, 0.22], [0.6, 0.48], [0.12, 0.5]], [[0.4, 0.5], [0.95, 1.0]]],
    E: [[[0.9, 0.02], [0.08, 0.0], [0.1, 1.0], [0.92, 1.0]], [[0.1, 0.5], [0.7, 0.5]]]
  };
  trazos[ch].forEach((tr, k) => dedo(l, tr.map(rot), color, 4.2, canal * 10 + k));
}

{
  // CORRE en tres bloques: se pinta en un lienzo de 192x64 y se corta en tres
  const ancho = new Lienzo(192, 64);
  const letras = ['C', 'O', 'R', 'R', 'E'];
  const anchos = letras.map((_, i) => 26 + hash(i, 1, 820) * 8);
  const huecos = letras.map((_, i) => 6 + hash(i, 5, 820) * 5);
  // que quepa entera y centrada (si no, la E se salia por el borde)
  const largo = anchos.reduce((a, b) => a + b) + huecos.slice(0, -1).reduce((a, b) => a + b);
  const esc = Math.min(1, 172 / largo);
  let x = (192 - largo * esc) / 2;
  letras.forEach((ch, i) => {
    const w = anchos[i] * esc;
    const h = 30 + hash(i, 2, 820) * 10;
    const y = 10 + hash(i, 3, 820) * 8 + i * 1.5;
    letraMano(ancho, ch, x, y, w, h, (hash(i, 4, 820) - 0.5) * 0.3, i < 2 ? SANGRE : mezcla(SANGRE, SECA, 0.4), i);
    x += w + (i < letras.length - 1 ? huecos[i] * esc : 0);
  });
  for (let p = 0; p < 3; p++) {
    const parte = new Lienzo(64);
    for (let y = 0; y < 64; y++) {
      for (let xx = 0; xx < 64; xx++) {
        const i = (y * 192 + p * 64 + xx) * 4;
        parte.data.set(ancho.data.subarray(i, i + 4), (y * 64 + xx) * 4);
      }
    }
    marca(`corre_${p}`, parte);
  }
}

function manoSangre (l, cx, cy, escalaMano, color, arrastre, canal) {
  l.disco(cx, cy, 6.5 * escalaMano, color, 0.82);
  l.disco(cx, cy + 3 * escalaMano, 5.5 * escalaMano, color, 0.82);
  const dedos = [[-7, -4, -0.7], [-3.5, -10, -0.15], [0.5, -11.5, 0], [4.5, -10, 0.15], [7.5, 1, 0.9]];
  dedos.forEach(([dx, dy, inc], k) => {
    l.linea(cx + dx * 0.45 * escalaMano, cy - 2 * escalaMano, cx + (dx + inc * 2) * escalaMano, cy + dy * escalaMano, color, 3 * escalaMano, 0.82, 0.5);
    l.disco(cx + (dx + inc * 2) * escalaMano, cy + dy * escalaMano, 1.6 * escalaMano, color, 0.82);
    if (arrastre > 0) l.linea(cx + dx * 0.6 * escalaMano, cy, cx + dx * 0.55 * escalaMano + hash(k, canal, 830), cy + arrastre * (0.7 + hash(k, canal, 831) * 0.3), color, 2.2, 0.6, 0.8);
  });
}

marca('mano', (() => {
  const l = new Lienzo(64);
  manoSangre(l, 30, 26, 1.6, SANGRE, 10, 1);
  for (let k = 0; k < 3; k++) l.linea(26 + k * 4, 38, 25 + k * 4, 46 + hash(k, 2, 832) * 14, SECA, 1.2, 0.7);
  return l;
})());
marca('manos', (() => {
  // dos manos que resbalan pared abajo
  const l = new Lienzo(64);
  manoSangre(l, 19, 16, 1.2, SECA, 34, 3);
  manoSangre(l, 44, 22, 1.2, SECA, 30, 4);
  return l;
})());
marca('aranazos', (() => {
  // cuatro arañazos que han levantado el esmalte: claros por dentro, oscuros en el borde
  const l = new Lienzo(64);
  for (let k = 0; k < 4; k++) {
    const x0 = 14 + k * 9;
    const pts = [];
    for (let i = 0; i <= 12; i++) pts.push([x0 + i * 1.4 + Math.sin(i * 0.5 + k) * 1.2, 8 + i * 3.6]);
    for (let i = 0; i + 1 < pts.length; i++) {
      l.linea(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], [70, 70, 66], 2.4, 0.75);
      l.linea(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], [214, 214, 206], 1.0, 0.9);
    }
  }
  return l;
})());
marca('conteo', (() => {
  // palitos rayados en el azulejo: alguien contaba algo
  const l = new Lienzo(64);
  const c = [58, 60, 62];
  for (let g = 0; g < 3; g++) {
    for (let fila = 0; fila < 2; fila++) {
      if (g === 2 && fila === 1) continue;
      const x0 = 8 + g * 18;
      const y0 = 12 + fila * 22;
      for (let i = 0; i < 4; i++) l.linea(x0 + i * 3, y0, x0 + i * 3 + 0.8, y0 + 14, c, 1.3, 0.85, 0.6);
      l.linea(x0 - 2, y0 + 12, x0 + 12, y0 + 2, c, 1.3, 0.85, 0.5);
    }
  }
  l.linea(46, 34, 46.5, 48, c, 1.3, 0.85, 0.6);
  return l;
})());
marca('espiral', (() => {
  // una espiral a rotulador negro, apretada, que no acaba
  const l = new Lienzo(64);
  let prev = null;
  for (let i = 0; i <= 260; i++) {
    const a = i * 0.12;
    const r = 1 + i * 0.095;
    const p = [32 + Math.cos(a) * r, 32 + Math.sin(a) * r * 0.92];
    if (prev) l.linea(prev[0], prev[1], p[0], p[1], [22, 20, 18], 1.6, 0.88, 0.4);
    prev = p;
  }
  return l;
})());
marca('mancha', (() => {
  // humedad negra que baja por la pared
  const l = new Lienzo(64);
  l.pintar((x, y) => {
    const m = fbm(x, y, 840, 64);
    const caida = ruido(x * 1.4, y * 0.2, 8, 841, 64);
    const centro = 1 - Math.min(1, Math.hypot((x - 32) / 30, (y - 20) / 26));
    const t = Math.max(0, (m - 0.42) * 2.6) * centro + Math.max(0, caida - 0.55) * 2.2 * (y / 64) * (1 - Math.abs(x - 32) / 34);
    return t > 0.05 ? [40, 44, 34, Math.min(0.85, t)] : null;
  });
  return l;
})());
marca('sangre', (() => {
  // un brochazo de sangre a la altura de la mano y unas gotas
  const l = new Lienzo(64);
  for (let k = 0; k < 6; k++) l.linea(12, 28 + k, 46 + hash(k, 1, 850) * 8, 24 + k * 1.5, SANGRE, 1.6, 0.7, 1.5);
  for (let k = 0; k < 5; k++) l.disco(20 + hash(k, 2, 851) * 30, 38 + hash(k, 3, 851) * 18, 0.8 + hash(k, 4, 851) * 1.2, SECA, 0.85);
  return l;
})());

/** Cartel de piscina viejo: placa blanca con borde de color, letras gastadas, grieta y esquina rota. */
function cartel (lineas, color, canal) {
  const l = new Lienzo(64);
  const x0 = 4; const y0 = 12; const x1 = 59; const y1 = 51;
  const rota = (x, y) => x > 47 && y > 41 && (x - 47) + (y - 41) > 9;
  l.pintar((x, y) => {
    if (x < x0 || x > x1 || y < y0 || y > y1 || rota(x, y)) return null;
    const borde = x < x0 + 2 || x > x1 - 2 || y < y0 + 2 || y > y1 - 2;
    let c = borde ? color : escala([226, 224, 214], 0.93 + fbm(x, y, 860 + canal, 64) * 0.09);
    if (fbm(x, y, 862 + canal, 64) > 0.62) c = mezcla(c, [150, 140, 104], 0.45);
    return c;
  });
  lineas.forEach(([txt, p, y], k) => l.texto(txt, 'centro', y, mezcla(color, [226, 224, 214], 0.25 + hash(k, canal, 863) * 0.2), p, 0.88));
  // la grieta
  let gx = 18; let gy = y0;
  for (let i = 0; i < 18; i++) {
    const nx = gx + (hash(i, canal, 864) - 0.4) * 4;
    const ny = gy + 2.2;
    l.linea(gx, gy, nx, ny, [70, 66, 58], 0.8, 0.8);
    gx = nx; gy = ny;
  }
  // tornillos (uno se ha caido: el de la esquina rota)
  for (const [x, y] of [[x0 + 4, y0 + 4], [x1 - 4, y0 + 4], [x0 + 4, y1 - 4]]) l.disco(x, y, 1.2, [120, 118, 110], 1);
  return l;
}
marca('cartel_no_correr', cartel([['NO', 2, 18], ['CORRER', 1, 36]], [176, 30, 28], 1));
marca('cartel_profundidad', cartel([['PROF.', 1, 19], ['2 M', 2, 30]], [36, 78, 168], 2));
marca('cartel_saltar', cartel([['PROHIBIDO', 1, 22], ['SALTAR', 1, 33]], [176, 30, 28], 3));

{
  const modelos = {};
  MARCAS.forEach((n) => {
    modelos[`marca_${n}`] = { parent: 'minecraft:block/block', textures: { all: t(`marca_${n}`), particle: t(`marca_${n}`) }, elements: [{ from: [0, 0, 15.92], to: [16, 16, 15.92], faces: { north: { texture: '#all', uv: [0, 0, 16, 16] } } }] };
  });
  for (const [n, m] of Object.entries(modelos)) json(`models/block/${n}.json`, m);
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) MARCAS.forEach((n, i) => { v[`facing=${f},marca=${i}`] = { model: t(`marca_${n}`), ...(y ? { y } : {}) }; });
  json('blockstates/marca.json', { variants: v });
  json('items/marca.json', { model: { type: 'minecraft:model', model: t('marca_mano') } });
  console.log(`  ${MARCAS.length} marcas: ${MARCAS.join(', ')}`);
}

/* ---- manchas en el suelo (32x32, transparentes, pegadas al azulejo) */

const MANCHAS = [];
function mancha (nombre, fn) {
  MANCHAS.push(nombre);
  guardar(TEX, `mancha_${nombre}`, new Lienzo(L).pintar(fn));
}
mancha('suciedad', (x, y) => {
  const m = fbm(x, y, 870);
  return m > 0.5 ? [...escala([92, 86, 70], 0.8 + hash(x, y, 871) * 0.3), Math.min(0.85, (m - 0.5) * 3.2)] : null;
});
mancha('charco', (x, y) => {
  const d = Math.hypot((x - 15.5) / 13, (y - 16) / 10) + (fbm(x, y, 872) - 0.5) * 0.5;
  return d < 1 ? [30, 34, 28, Math.min(0.75, (1 - d) * 2.2)] : null;
});
mancha('gotas_sangre', (x, y) => {
  // un rastro de gotas que cruza
  for (let k = 0; k < 7; k++) {
    const gx = 3 + k * 4.2 + hash(k, 1, 873) * 2;
    const gy = 9 + k * 2.2 + hash(k, 2, 873) * 3;
    if (Math.hypot(x - gx, y - gy) < 0.8 + hash(k, 3, 873) * 1.1) return [...SECA, 0.9];
  }
  return null;
});
mancha('escombros', (x, y) => {
  // trozos de azulejo y polvo de cemento
  if (hash(Math.floor(x / 3), Math.floor(y / 3), 874) > 0.82 && fbm(x, y, 875) > 0.45) return [...escala([228, 232, 230], 0.9 + hash(x, y, 876) * 0.1), 1];
  const m = fbm(x, y, 877);
  return m > 0.56 ? [150, 150, 142, Math.min(0.8, (m - 0.56) * 3.5)] : null;
});
mancha('huellas', (x, y) => {
  // pisadas descalzas, mojadas y sucias
  for (let k = 0; k < 3; k++) {
    const cx = 9 + k * 7 + (k % 2) * 3;
    const cy = 26 - k * 9;
    const dx = x - cx; const dy = y - cy;
    if (Math.hypot(dx / 2.2, dy / 4) < 1) return [70, 66, 54, 0.6];
    for (let d = 0; d < 4; d++) if (Math.hypot(x - (cx - 2 + d * 1.4), y - (cy - 5.2)) < 0.8) return [70, 66, 54, 0.6];
  }
  return null;
});
{
  for (const n of MANCHAS) {
    json(`models/block/mancha_${n}.json`, {
      parent: 'minecraft:block/block',
      textures: { all: t(`mancha_${n}`), particle: t(`mancha_${n}`) },
      elements: [{ from: [0, 0, 0], to: [16, 0.06, 16], faces: { up: { texture: '#all', uv: [0, 0, 16, 16] } } }]
    });
  }
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) MANCHAS.forEach((n, i) => { v[`facing=${f},mancha=${i}`] = { model: t(`mancha_${n}`), ...(y ? { y } : {}) }; });
  json('blockstates/mancha.json', { variants: v });
  json('items/mancha.json', { model: { type: 'minecraft:model', model: t('mancha_suciedad') } });
  console.log(`  ${MANCHAS.length} manchas: ${MANCHAS.join(', ')}`);
}

/* ---- objetos abandonados (modelos de bloque, mirando al norte) */

guardar(TEX, 'flotador', new Lienzo(16).pintar((x, y) => {
  // plastico amarillo con brillo (arriba) y el pico naranja / ojo negro (abajo)
  if (y >= 12) return x < 8 ? [232, 118, 24] : [18, 18, 20];
  let c = escala([240, 204, 40], 0.9 + fbm(x, y, 880, 16) * 0.12);
  if ((x + y) % 7 === 0) c = mezcla(c, [255, 250, 220], 0.4);
  return c;
}));
guardar(TEX, 'toalla', new Lienzo(16).pintar((x, y) => {
  let c = Math.floor(y / 3) % 2 === 0 ? [70, 120, 176] : [214, 212, 200];
  c = escala(c, 0.82 + hash(x, y, 881) * 0.16);
  if (fbm(x, y, 882, 16) > 0.62) c = mezcla(c, [120, 112, 86], 0.35);
  return c;
}));
guardar(TEX, 'chanclas', new Lienzo(16).pintar((x, y) => {
  if (y < 8) return escala([34, 74, 150], 0.85 + hash(x, y, 883) * 0.15);
  return escala([214, 64, 52], 0.85 + hash(x, y, 884) * 0.15);
}));
guardar(TEX, 'mochila', new Lienzo(16).pintar((x, y) => {
  let c = escala([58, 74, 52], 0.82 + fbm(x, y, 885, 16) * 0.2);
  if (y === 5 || y === 6) c = [40, 40, 38];
  if (y === 6 && x % 2 === 0) c = [150, 150, 140];
  if (fbm(x, y, 886, 16) > 0.66) c = mezcla(c, [32, 36, 30], 0.5);
  return c;
}));

/** Elemento de modelo con la misma textura en todas las caras (uv proporcional). */
function caja (from, to, tex, extra = {}) {
  const f = {};
  const uv = { north: [from[0], 16 - to[1], to[0], 16 - from[1]], south: [from[0], 16 - to[1], to[0], 16 - from[1]],
    east: [from[2], 16 - to[1], to[2], 16 - from[1]], west: [from[2], 16 - to[1], to[2], 16 - from[1]],
    up: [from[0], from[2], to[0], to[2]], down: [from[0], from[2], to[0], to[2]] };
  for (const cara of Object.keys(uv)) f[cara] = { texture: tex, uv: uv[cara].map((v) => Math.max(0, Math.min(16, v))) };
  return { from, to, faces: f, ...extra };
}

const OBJETOS = {
  // el flotador con cabeza de pato (como el de la foto de la sala inundada), tirado en el suelo
  flotador: { tex: 'flotador', elementos: [
    caja([2, 0, 2], [14, 3, 5], '#t'), caja([2, 0, 11], [14, 3, 14], '#t'),
    caja([2, 0, 5], [5, 3, 11], '#t'), caja([11, 0, 5], [14, 3, 11], '#t'),
    caja([6, 3, 1], [10, 8, 5], '#t'),
    { from: [7, 4, -1], to: [9, 5.5, 1], faces: { north: { texture: '#t', uv: [0, 12, 4, 15] }, up: { texture: '#t', uv: [0, 12, 4, 15] }, east: { texture: '#t', uv: [0, 12, 4, 15] }, west: { texture: '#t', uv: [0, 12, 4, 15] }, down: { texture: '#t', uv: [0, 12, 4, 15] } } },
    { from: [5.95, 6, 2], to: [5.95, 7, 3], faces: { west: { texture: '#t', uv: [10, 12, 12, 14] } } },
    { from: [10.05, 6, 2], to: [10.05, 7, 3], faces: { east: { texture: '#t', uv: [10, 12, 12, 14] } } }
  ] },
  // una toalla arrugada
  toalla: { tex: 'toalla', elementos: [caja([2, 0, 3], [13, 1, 12], '#t'), caja([4, 1, 5], [11, 2, 10], '#t'), caja([9, 1, 2], [14, 1.6, 6], '#t')] },
  // un par de chanclas, una algo girada
  chanclas: { tex: 'chanclas', elementos: [
    caja([3, 0, 3], [7, 0.8, 12], '#t'), caja([4.5, 0.8, 5], [5.5, 1.8, 6], '#t'),
    { ...caja([9, 0, 4], [13, 0.8, 13], '#t'), rotation: { origin: [11, 0, 8.5], axis: 'y', angle: 22.5 } },
    { ...caja([10.5, 0.8, 6], [11.5, 1.8, 7], '#t'), rotation: { origin: [11, 0, 8.5], axis: 'y', angle: 22.5 } }
  ] },
  // una mochila tirada de lado
  mochila: { tex: 'mochila', elementos: [caja([3, 0, 4], [13, 6, 12], '#t'), caja([5, 0, 2], [11, 4, 4], '#t'), caja([4, 6, 7], [12, 7, 9], '#t')] }
};
for (const [n, o] of Object.entries(OBJETOS)) {
  json(`models/block/${n}.json`, { parent: 'minecraft:block/block', textures: { t: t(o.tex), particle: t(o.tex) }, elements: o.elementos });
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) v[`facing=${f}`] = { model: t(n), ...(y ? { y } : {}) };
  json(`blockstates/${n}.json`, { variants: v });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
}

// el panel que parpadea: encendido como el panel, apagado gris
json('models/block/luz_techo_apagada.json', cubo('luz_techo_apagada'));
json('blockstates/luz_techo_parpadeo.json', { variants: { 'lit=true': { model: t('luz_techo') }, 'lit=false': { model: t('luz_techo_apagada') } } });
json('items/luz_techo_parpadeo.json', { model: { type: 'minecraft:model', model: t('luz_techo') } });
for (const n of ['azulejo_manchado', 'azulejo_sangre']) {
  json(`models/block/${n}.json`, cubo(n));
  json(`blockstates/${n}.json`, { variants: { '': { model: t(n) } } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
}
console.log('  detalles: marcas, manchas, objetos, panel que parpadea');

/* ============================================================ misiones */

/** Un cubo con una cara (`frente`, la del norte) distinta y las demas de azulejo (va metido en la pared). */
function enPared (frente, extra = {}) {
  const az = t('azulejo');
  const cara = (tex, d) => ({ texture: tex, cullface: d });
  return {
    parent: 'minecraft:block/block',
    textures: { f: t(frente), a: az, particle: t(frente) },
    elements: [{ from: [0, 0, 0], to: [16, 16, 16], ...extra,
      faces: { north: cara('#f', 'north'), south: cara('#a', 'south'), east: cara('#a', 'east'), west: cara('#a', 'west'), up: cara('#a', 'up'), down: cara('#a', 'down') } }]
  };
}

const ACERO = [104, 112, 108];
const ROJO_VALVULA = [168, 30, 24];
const VERDE_SALIDA = [22, 150, 74];
const COLORES_TARJETA = { roja: [196, 34, 30], amarilla: [232, 190, 30], verde: [34, 160, 70] };

// la valvula: volante de hierro pintado de rojo (desconchado) en un tubo con su brida
guardar(TEX, 'valvula_rueda', new Lienzo(16).pintar((x, y) => {
  let c = escala(ROJO_VALVULA, 0.82 + fbm(x, y, 900, 16) * 0.25);
  if (fbm(x, y, 901, 16) > 0.64) c = mezcla(c, [70, 52, 44], 0.7); // pintura saltada: hierro oxidado
  if ((x * 7 + y * 3) % 11 === 0) c = escala(c, 1.15);
  return c;
}));
guardar(TEX, 'valvula_tubo', new Lienzo(16).pintar((x, y) => {
  let c = escala(ACERO, 0.8 + fbm(x, y, 902, 16) * 0.25);
  if (y % 8 === 0) c = escala(c, 0.8);
  if (fbm(x, y, 903, 16) > 0.62) c = mezcla(c, [118, 82, 48], 0.5); // oxido
  return c;
}));
{
  // el bloque es el tubo con su brida y el eje; el volante es un objeto aparte que gira suave
  // (ValvulaRenderer): un octogono de dos cuadrados (el segundo a 45 grados) y cuatro radios
  const z0 = 9.2;
  const z1 = 10.6;
  const a45 = { rotation: { origin: [8, 8, 10], axis: 'z', angle: 45, rescale: false } };
  const aro = (extra) => [
    { ...caja([5.5, 12.2, z0], [10.5, 13.6, z1], '#r'), ...extra },
    { ...caja([5.5, 2.4, z0], [10.5, 3.8, z1], '#r'), ...extra },
    { ...caja([2.4, 5.5, z0], [3.8, 10.5, z1], '#r'), ...extra },
    { ...caja([12.2, 5.5, z0], [13.6, 10.5, z1], '#r'), ...extra }
  ];
  json('models/block/valvula.json', { parent: 'minecraft:block/block', textures: { t: t('valvula_tubo'), particle: t('valvula_rueda') },
    elements: [caja([4, 4, 15], [12, 12, 16], '#t'), caja([6, 6, 11], [10, 10, 15], '#t'), caja([6.8, 6.8, 9], [9.2, 9.2, 11], '#t')] });
  json('models/item/volante_valvula.json', { parent: 'minecraft:block/block', textures: { r: t('valvula_rueda'), particle: t('valvula_rueda') },
    elements: [...aro({}), ...aro(a45), caja([3.5, 7.4, 9.5], [12.5, 8.6, 10.3], '#r'), caja([7.4, 3.5, 9.5], [8.6, 12.5, 10.3], '#r')] });
  json('items/volante_valvula.json', { model: { type: 'minecraft:model', model: `${NS}:item/volante_valvula` } });
  for (let a = 0; a < 4; a++) fs.rmSync(path.join(ASSETS, 'models', 'block', `valvula_${a}.json`), { force: true });
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) v[`facing=${f}`] = { model: t('valvula'), ...(y ? { y } : {}) };
  json('blockstates/valvula.json', { variants: v });
  json('items/valvula.json', { model: { type: 'minecraft:model', model: t('valvula') } });
}

// los lectores: caja negra con la franja de su color, la ranura y un piloto (encendido al meterla)
{
  const nombres = ['roja', 'amarilla', 'verde'];
  nombres.forEach((n, k) => {
    for (const on of [false, true]) {
      const col = COLORES_TARJETA[n];
      guardar(TEX, `lector_${n}${on ? '_on' : ''}`, new Lienzo(16).pintar((x, y) => {
        let c = escala([38, 40, 42], 0.85 + fbm(x, y, 910 + k, 16) * 0.2);
        if (x === 0 || y === 0 || x === 15 || y === 15) c = [70, 72, 74];
        if (y >= 2 && y <= 4 && x >= 2 && x <= 13) c = escala(col, on ? 1.1 : 0.75);
        if (y === 9 && x >= 3 && x <= 12) c = [8, 8, 10]; // la ranura
        if (y === 8 && x >= 3 && x <= 12) c = [90, 92, 94];
        if (x >= 6 && x <= 9 && y >= 12 && y <= 13) c = on ? [120, 255, 140] : [40, 70, 44]; // el piloto
        return c;
      }));
      const m = `lector_${n}${on ? '_on' : ''}`;
      const caraF = { texture: '#f' };
      json(`models/block/${m}.json`, { parent: 'minecraft:block/block', textures: { f: t(m), l: t('valvula_tubo'), particle: t(m) },
        elements: [{ from: [4, 2, 14], to: [12, 14, 16], ...(on ? { shade: false, light_emission: 8 } : {}),
          faces: { north: { ...caraF, uv: [0, 0, 16, 16] }, east: { texture: '#l', uv: [0, 0, 2, 12] }, west: { texture: '#l', uv: [0, 0, 2, 12] },
            up: { texture: '#l', uv: [0, 0, 8, 2] }, down: { texture: '#l', uv: [0, 0, 8, 2] } } }] });
    }
  });
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    nombres.forEach((n, k) => {
      for (const on of [false, true]) v[`color=${k},facing=${f},lit=${on}`] = { model: t(`lector_${n}${on ? '_on' : ''}`), ...(y ? { y } : {}) };
    });
  }
  json('blockstates/lector_tarjeta.json', { variants: v });
  json('items/lector_tarjeta.json', { model: { type: 'minecraft:model', model: t('lector_roja') } });
}

// la puerta: dos hojas de chapa gris verdosa con barra antipanico, bisagras y una franja roja;
// se dibuja entera (64x96, 2x3 bloques) y se corta en seis. Abierta: negro.
{
  const P = new Lienzo(64, 96);
  P.pintar((x, y) => {
    const hoja = x < 32 ? 0 : 1;
    const lx = x - hoja * 32;
    let c = escala([96, 112, 104], 0.86 + fbm(x, y, 920, 64) * 0.16);
    if (fbm(x * 0.5, y * 2, 921, 64) > 0.6) c = escala(c, 0.9); // chorretones de suciedad
    // marco y canto de cada hoja
    if (lx <= 1 || lx >= 30 || y <= 1) c = escala([70, 80, 76], 0.9);
    if (hoja === 0 && lx === 31) c = [30, 32, 30]; // la junta entre las hojas
    // paneles hundidos
    const panel = (y > 8 && y < 40) || (y > 46 && y < 90);
    if (panel && lx > 5 && lx < 26 && (lx === 6 || lx === 25 || y === 9 || y === 39 || y === 47 || y === 89)) c = escala(c, 0.78);
    // franja roja de emergencia
    if (y >= 41 && y <= 44) c = escala([176, 36, 28], 0.85 + hash(x, y, 922) * 0.15);
    return c;
  });
  // barra antipanico (a la altura de la cintura) y bisagras
  P.rect(5, 58, 58, 61, [176, 178, 172]).rect(5, 62, 58, 62, [90, 92, 88]);
  for (const yb of [12, 50, 84]) { P.rect(0, yb, 1, yb + 5, [40, 42, 40]); P.rect(62, yb, 63, yb + 5, [40, 42, 40]); }
  // ventanuco de malla en cada hoja
  for (const hx of [10, 42]) {
    P.rect(hx, 14, hx + 11, 30, [28, 36, 38]);
    for (let y = 14; y <= 30; y += 3) P.rect(hx, y, hx + 11, y, [70, 78, 80]);
    for (let x = hx; x <= hx + 11; x += 3) P.rect(x, 14, x, 30, [70, 78, 80]);
  }
  P.texto('SALIDA', 'centro', 68, [235, 235, 225], 1);
  for (let col = 0; col < 2; col++) {
    for (let fila = 0; fila < 3; fila++) {
      const parte = new Lienzo(32);
      for (let y = 0; y < 32; y++) {
        for (let x = 0; x < 32; x++) {
          const i = ((96 - 32 * (fila + 1) + y) * 64 + col * 32 + x) * 4;
          parte.data.set(P.data.subarray(i, i + 4), (y * 32 + x) * 4);
        }
      }
      guardar(TEX, `puerta_salida_${col * 3 + fila}`, parte);
      json(`models/block/puerta_salida_${col * 3 + fila}.json`, enPared(`puerta_salida_${col * 3 + fila}`));
    }
  }
  guardar(TEX, 'puerta_salida_abierta', new Lienzo(32).pintar((x, y) => escala([10, 12, 14], 0.7 + fbm(x, y, 924, 32) * 0.4)));
  json('models/block/puerta_salida_abierta.json', enPared('puerta_salida_abierta'));
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    for (let p = 0; p < 6; p++) {
      v[`abierta=false,facing=${f},parte=${p}`] = { model: t(`puerta_salida_${p}`), ...(y ? { y } : {}) };
      v[`abierta=true,facing=${f},parte=${p}`] = { model: t('puerta_salida_abierta'), ...(y ? { y } : {}) };
    }
  }
  json('blockstates/puerta_salida.json', { variants: v });
  json('items/puerta_salida.json', { model: { type: 'minecraft:model', model: t('puerta_salida_1') } });
}

// carteles verdes de salida de emergencia (con luz propia): el pequeno con el hombrecillo que
// corre hacia una puerta y la flecha; el grande de encima de la puerta, en dos mitades
{
  const BLANCO_S = [244, 248, 240];
  const hombrecillo = (l, x, y, s, espejo) => {
    const X = (v) => espejo ? x - v * s : x + v * s;
    const Y = (v) => y + v * s;
    l.disco(X(3), Y(0), 2.2 * s, BLANCO_S);
    l.linea(X(2.4), Y(2.6), X(0.8), Y(8), BLANCO_S, 2.4 * s);   // cuerpo
    l.linea(X(2.2), Y(3.6), X(5), Y(5.5), BLANCO_S, 1.8 * s);   // brazo delante
    l.linea(X(2), Y(3.8), X(-0.8), Y(4.6), BLANCO_S, 1.8 * s);  // brazo detras
    l.linea(X(0.8), Y(8), X(3.6), Y(10.5), BLANCO_S, 2 * s);    // pierna delante
    l.linea(X(3.6), Y(10.5), X(3.2), Y(13.5), BLANCO_S, 2 * s);
    l.linea(X(0.8), Y(8), X(-2.2), Y(11), BLANCO_S, 2 * s);     // pierna detras
    l.linea(X(-2.2), Y(11), X(-4.6), Y(11.5), BLANCO_S, 2 * s);
  };
  const flecha = (l, x, y, s, izquierda) => {
    const d = izquierda ? -1 : 1;
    l.linea(x - 5 * s * d, y, x + 4 * s * d, y, BLANCO_S, 2.6 * s);
    l.poligono([[x + 7 * s * d, y], [x + 2.5 * s * d, y - 4.5 * s], [x + 2.5 * s * d, y + 4.5 * s]], BLANCO_S);
  };
  {
    // hacia la derecha: el hombrecillo corre hacia la puerta y, al lado, la flecha; el de la
    // izquierda es el mismo en espejo
    const l = new Lienzo(64);
    l.pintar((x, y) => (x >= 4 && x <= 59 && y >= 15 && y <= 48 ? escala(VERDE_SALIDA, 0.94 + fbm(x, y, 930, 64) * 0.1) : null));
    l.rect(34, 19, 44, 44, BLANCO_S).rect(36, 21, 42, 44, VERDE_SALIDA);
    hombrecillo(l, 20, 20.5, 1.75, false);
    flecha(l, 51, 31, 0.95, false);
    guardar(TEX, 'cartel_salida_derecha', l);
    guardar(TEX, 'cartel_salida_izquierda', l.espejo());
  }
  const g = new Lienzo(128, 64);
  g.pintar((x, y) => (x >= 2 && x <= 125 && y >= 6 && y <= 57 ? escala(VERDE_SALIDA, 0.94 + fbm(x, y, 931, 64) * 0.1) : null));
  g.texto('SALIDA', 'centro', 12, BLANCO_S, 3).texto('EMERGENCIA', 'centro', 38, BLANCO_S, 2);
  for (const [k, n] of [[0, 'izq'], [1, 'der']]) {
    const parte = new Lienzo(64);
    for (let y = 0; y < 64; y++) {
      for (let x = 0; x < 64; x++) {
        const i = (y * 128 + k * 64 + x) * 4;
        parte.data.set(g.data.subarray(i, i + 4), (y * 64 + x) * 4);
      }
    }
    guardar(TEX, `cartel_salida_grande_${n}`, parte);
  }
  const panel = (tex) => ({ parent: 'minecraft:block/block', textures: { all: t(tex), particle: t(tex) },
    elements: [{ from: [0, 0, 15.9], to: [16, 16, 15.9], shade: false, light_emission: 12, faces: { north: { texture: '#all', uv: [0, 0, 16, 16] } } }] });
  const nombres = ['cartel_salida_izquierda', 'cartel_salida_derecha', 'cartel_salida_grande_izq', 'cartel_salida_grande_der'];
  nombres.forEach((n) => json(`models/block/${n}.json`, panel(n)));
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) nombres.forEach((n, i) => { v[`facing=${f},variante=${i}`] = { model: t(n), ...(y ? { y } : {}) }; });
  json('blockstates/cartel_salida.json', { variants: v });
  json('items/cartel_salida.json', { model: { type: 'minecraft:model', model: t('cartel_salida_derecha') } });
}

// las tres tarjetas (objetos): plastico de su color, banda blanca con la etiqueta, banda magnetica y chip
{
  const ITEM = path.join(ASSETS, 'textures', 'item');
  for (const [n, col] of Object.entries(COLORES_TARJETA)) {
    guardar(ITEM, `tarjeta_${n}`, new Lienzo(16).pintar((x, y) => {
      if (y < 3 || y > 12 || x < 1 || x > 14) return null;
      if ((x === 1 || x === 14) && (y === 3 || y === 12)) return null; // esquinas redondeadas
      let c = escala(col, 0.9 + fbm(x, y, 940, 16) * 0.15);
      if (x === 1 || x === 14 || y === 3 || y === 12) c = escala(col, 0.65);
      if (y === 5) c = [24, 24, 26];                      // banda magnetica
      if (y >= 8 && y <= 10 && x >= 7 && x <= 13) c = [236, 236, 228]; // etiqueta
      if (y === 9 && x >= 8 && x <= 12 && x % 2 === 0) c = [60, 60, 60];
      if (y >= 8 && y <= 10 && x >= 3 && x <= 5) c = (x + y) % 2 ? [214, 176, 64] : [168, 132, 40]; // chip
      return c;
    }));
    json(`models/item/tarjeta_${n}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/tarjeta_${n}` } });
    json(`items/tarjeta_${n}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/tarjeta_${n}` } });
  }
}
console.log('  misiones: valvula, lectores, puerta de salida, carteles y tarjetas');

/* ============================================================ nombres */

const NOMBRES = {
  azulejo: ['Azulejo', 'Tile'],
  azulejo_sucio: ['Azulejo sucio', 'Dirty tile'],
  azulejo_roto: ['Azulejo roto', 'Broken tile'],
  azulejo_azul: ['Azulejo azul', 'Blue tile'],
  fondo_piscina: ['Fondo de piscina', 'Pool floor'],
  franja_piscina: ['Franja de calle', 'Lane stripe'],
  borde_piscina: ['Borde de piscina', 'Pool edge'],
  rejilla: ['Rejilla', 'Drain grate'],
  luz_techo: ['Panel de luz', 'Light panel'],
  claraboya: ['Claraboya', 'Skylight'],
  foco_piscina: ['Foco de piscina', 'Pool light'],
  losa_azulejo: ['Losa de azulejo', 'Tile slab'],
  escalon_azulejo: ['Escalón de azulejo', 'Tile stairs'],
  luz_techo_parpadeo: ['Panel de luz que falla', 'Flickering light panel'],
  azulejo_manchado: ['Azulejo manchado', 'Stained tile'],
  azulejo_sangre: ['Azulejo con sangre', 'Bloodied tile'],
  marca: ['Marca en la pared', 'Wall mark'],
  mancha: ['Mancha en el suelo', 'Floor stain'],
  flotador: ['Flotador', 'Pool float'],
  toalla: ['Toalla', 'Towel'],
  chanclas: ['Chanclas', 'Flip-flops'],
  mochila: ['Mochila', 'Backpack'],
  valvula: ['Válvula', 'Valve'],
  lector_tarjeta: ['Lector de tarjeta', 'Card reader'],
  puerta_salida: ['Salida de emergencia', 'Emergency exit'],
  cartel_salida: ['Cartel de salida', 'Exit sign']
};
const OTROS = {
  'item.backrooms_evento.tarjeta_roja': ['Tarjeta roja', 'Red keycard'],
  'item.backrooms_evento.tarjeta_amarilla': ['Tarjeta amarilla', 'Yellow keycard'],
  'item.backrooms_evento.tarjeta_verde': ['Tarjeta verde', 'Green keycard'],
  'entity.backrooms_evento.tarjeta': ['Tarjeta', 'Keycard'],
  'entity.backrooms_evento.cadaver': ['Cadáver', 'Corpse'],
  'item.backrooms_evento.volante_valvula': ['Volante de válvula', 'Valve wheel']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  for (const [id, v] of Object.entries(OTROS)) lang[id] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: bloques de las piscinas');
