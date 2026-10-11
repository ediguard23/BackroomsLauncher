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
  if (px === 0 || py === 0) {
    const c = escala(junta, 0.94 + hash(x, y, canal + 1) * 0.1);
    return { c, junta: true, px, py, tx, ty };
  }
  // cada pieza con su tono, un degradado de esmalte y el bisel (claro arriba-izquierda)
  let c = escala(base, 0.975 + hash(tx, ty, canal + 2) * 0.05);
  const d = (px + py) / (2 * (PASO - 1));
  c = escala(c, 1.025 - d * 0.05);
  if (px === 1 || py === 1) c = escala(c, 1.03);
  if (px === PASO - 1 || py === PASO - 1) c = escala(c, 0.93);
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
  escalon_azulejo: ['Escalón de azulejo', 'Tile stairs']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: bloques de las piscinas');
