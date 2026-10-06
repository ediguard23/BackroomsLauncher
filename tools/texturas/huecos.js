'use strict';
/**
 * Los huecos de las paredes del Nivel 0 (bloque backrooms_evento:hueco): un
 * boquete roto en el zocalo por donde cabe alguien arrastrandose. Como en Escape
 * the Backrooms: el pladur reventado con el borde dentado, el yeso a la vista,
 * el hueco de dentro con los montantes y el aislante, cascotes en el suelo y una
 * tira de papel pintado despegada que se mece (textura animada). Por dentro no
 * hay luz: se ve negro.
 *
 *   node tools/texturas/huecos.js
 *
 * Saca tres variantes (el estado del bloque elige una al azar por posicion). El
 * hueco de paso es siempre el mismo (x 1..15, y 0..13 en pixeles), que es lo que
 * usa la colision del bloque (escondite/Hueco.java): aqui solo cambia el dibujo.
 */

const fs = require('fs');
const path = require('path');
const { hash, ruido, fbm, mezcla, escala, Lienzo, guardar } = require('./lienzo');

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

console.log('Huecos de las paredes');

/* ============================================================ texturas */

// yeso del pladur roto: blanco sucio, granulado, con la capa de papel amarilla a un lado
guardar(TEX, 'hueco_yeso', new Lienzo(16).pintar((x, y) => {
  let c = escala([224, 219, 204], 0.9 + fbm(x, y, 501, 16) * 0.14 + hash(x, y, 502) * 0.05);
  if (hash(x, y, 503) > 0.9) c = escala(c, 0.8);              // motas
  if (y === 0) c = mezcla(c, [204, 180, 96], 0.7);             // el papel pegado al borde
  if (fbm(x, y, 504, 16) > 0.62) c = mezcla(c, [150, 128, 84], 0.35); // manchas de humedad
  return c;
}));

// por dentro: casi negro, un montante de madera y aislante deshilachado
guardar(TEX, 'hueco_interior', new Lienzo(16).pintar((x, y) => {
  let c = escala([34, 29, 22], 0.8 + hash(x, y, 511) * 0.35);
  const lana = fbm(x, y, 512, 16);
  if (lana > 0.55) c = mezcla(c, [96, 74, 52], (lana - 0.55) * 1.6);   // aislante
  if (x >= 6 && x <= 8) c = escala([70, 54, 32], 0.85 + ruido(x, y * 0.3, 4, 513, 16) * 0.3); // montante
  if (x === 9) c = escala(c, 0.6);
  return c;
}));

// cascotes: polvo de yeso y trocitos sobre la moqueta
guardar(TEX, 'hueco_escombros', new Lienzo(16).pintar((x, y) => {
  let c = escala([150, 132, 74], 0.75 + hash(x, y, 521) * 0.2);          // moqueta sucia
  const polvo = fbm(x, y, 522, 16);
  if (polvo > 0.45) c = mezcla(c, [210, 204, 188], Math.min(0.85, (polvo - 0.45) * 2.5));
  if (hash(x, y, 523) > 0.86) c = escala([228, 224, 210], 0.85 + hash(x, y, 524) * 0.15); // trocitos
  if (hash(x, y, 525) > 0.95) c = [70, 58, 40];
  return c;
}));

// tira de papel pintado despegada que se mece: 12 fotogramas de 16x16
const PAPEL = [214, 194, 104];
function motivo (x, y) {
  const mx = ((x % 8) + 8) % 8;
  const my = (((y + (Math.floor(x / 8) % 2) * 4) % 8) + 8) % 8;
  const d = Math.abs(mx - 3.5);
  return (my === 2 && d < 1) || (my === 3 && d > 0.9 && d < 2) || (my === 4 && d > 1.9 && d < 3);
}
{
  const N = 12;
  const l = new Lienzo(16, 16 * N);
  // borde de abajo rasgado (fijo) y ancho de la tira
  const largo = [13, 14, 15, 15, 14, 12, 13, 11];
  for (let f = 0; f < N; f++) {
    const vaiven = Math.sin((f / N) * Math.PI * 2);
    for (let y = 0; y < 16; y++) {
      const k = (y / 15) ** 2;
      const dx = Math.round(vaiven * k * 2.2);
      for (let x = 0; x < 8; x++) {
        if (y > largo[x]) continue;
        const xx = x + dx;
        if (xx < 0 || xx > 7) continue;
        // se enrolla por la derecha: ahi se ve el reves blanco del papel
        const reves = x >= 6 && y > 4;
        let c = reves ? escala([206, 200, 184], 0.88 + hash(x, y, 531) * 0.1) : PAPEL.slice();
        if (!reves) {
          if (x === 0) c = escala(c, 1.04);
          if (motivo(x, y)) c = escala(c, 0.9);
          c = escala(c, 0.93 + hash(x, y, 532) * 0.08);
          c = mezcla(c, [128, 104, 52], Math.max(0, (y - 9) / 14));      // mas sucio abajo
        }
        if (y === largo[x]) c = escala(c, 0.8);                           // filo rasgado
        l.punto(xx, f * 16 + y, c, 1);
      }
    }
  }
  guardar(TEX, 'hueco_papel', l);
  fs.writeFileSync(path.join(TEX, 'hueco_papel.png.mcmeta'), JSON.stringify({ animation: { frametime: 3, interpolate: true } }, null, 2) + '\n');
}

/* ============================================================== modelo */

/** Azar repetible para cada variante. */
function azar (semilla) {
  let s = semilla >>> 0;
  return () => { s = (Math.imul(s ^ (s >>> 15), 2246822507) + 0x9e3779b9) >>> 0; return (s >>> 8) / 16777216; };
}

/**
 * Lo que queda abierto en una cara del pladur: el paso entero (x 1..15, y < 13)
 * menos dientes que cuelgan del borde de arriba y algun mordisco en los lados.
 */
function abertura (semilla) {
  const rnd = azar(semilla);
  const arriba = new Array(16).fill(0);
  for (let x = 1; x < 15;) {
    const ancho = 1 + Math.floor(rnd() * 3);
    const r = rnd();
    const alto = r < 0.45 ? 13 : r < 0.7 ? 12 : r < 0.88 ? 11 : 10;
    for (let k = 0; k < ancho && x < 15; k++, x++) arriba[x] = alto;
  }
  const abierto = [];
  for (let y = 0; y < 16; y++) {
    abierto.push([]);
    const izq = rnd() < 0.22 && y > 1 && y < 11 ? 2 : 1;
    const der = rnd() < 0.22 && y > 1 && y < 11 ? 14 : 15;
    for (let x = 0; x < 16; x++) abierto[y].push(x >= izq && x < der && y < arriba[x]);
  }
  return abierto;
}

/** Junta los pixeles macizos en rectangulos (por filas y luego hacia arriba). */
function rectangulos (macizo) {
  const usado = macizo.map((f) => f.map(() => false));
  const out = [];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      if (!macizo[y][x] || usado[y][x]) continue;
      let x1 = x;
      while (x1 + 1 < 16 && macizo[y][x1 + 1] && !usado[y][x1 + 1]) x1++;
      let y1 = y;
      const filaLibre = (yy) => { for (let k = x; k <= x1; k++) if (!macizo[yy][k] || usado[yy][k]) return false; return true; };
      while (y1 + 1 < 16 && filaLibre(y1 + 1)) y1++;
      for (let yy = y; yy <= y1; yy++) for (let k = x; k <= x1; k++) usado[yy][k] = true;
      out.push([x, y, x1 + 1, y1 + 1]);
    }
  }
  return out;
}

/**
 * Una cara del pladur (z0..z1) con su boquete. `fuera` es la cara que da al
 * pasillo (north o south): papel/zocalo; la de dentro, negra.
 */
function panel (abierto, z0, z1, fuera) {
  const macizo = abierto.map((f) => f.map((v) => !v));
  const dentro = fuera === 'north' ? 'south' : 'north';
  return rectangulos(macizo).map(([x0, y0, x1, y1]) => {
    const caras = {};
    caras[fuera] = { texture: '#zocalo' };
    caras[dentro] = { texture: '#interior' };
    // los lados que tocan el borde del bloque se tapan con la pared de al lado
    caras.west = x0 === 0 ? { texture: '#zocalo', cullface: 'west' } : { texture: '#yeso' };
    caras.east = x1 === 16 ? { texture: '#zocalo', cullface: 'east' } : { texture: '#yeso' };
    caras.up = y1 === 16 ? { texture: '#zocalo', cullface: 'up' } : { texture: '#yeso' };
    caras.down = y0 === 0 ? { texture: '#zocalo', cullface: 'down' } : { texture: '#yeso' };
    return { from: [x0, y0, z0], to: [x1, y1, z1], faces: caras };
  });
}

function cascote (x, z, ancho, fondo, alto) {
  return {
    from: [x, 0, z], to: [x + ancho, alto, z + fondo],
    faces: { up: { texture: '#yeso' }, north: { texture: '#yeso' }, south: { texture: '#yeso' }, east: { texture: '#yeso' }, west: { texture: '#yeso' } }
  };
}

function modelo (v) {
  const rnd = azar(900 + v);
  const els = [];
  // las dos caras del pladur, cada una rota a su manera
  els.push(...panel(abertura(100 + v * 2), 0, 2, 'north'));
  els.push(...panel(abertura(101 + v * 2), 14, 16, 'south'));
  // el hueco de dentro: montantes a los lados y el travesano de arriba, a oscuras
  els.push({ from: [0, 0, 2], to: [1, 16, 14], faces: { east: { texture: '#interior' } } });
  els.push({ from: [15, 0, 2], to: [16, 16, 14], faces: { west: { texture: '#interior' } } });
  els.push({ from: [1, 13, 2], to: [15, 16, 14], faces: { down: { texture: '#interior' } } });
  // polvo y cascotes en el suelo de dentro y a los dos lados
  els.push({ from: [1, 0, 1], to: [15, 0.3, 15], faces: { up: { texture: '#escombros' } } });
  for (const lado of [-1, 1]) {
    const n = 2 + Math.floor(rnd() * 3);
    for (let k = 0; k < n; k++) {
      const ancho = 1 + Math.floor(rnd() * 2);
      const fondo = 1 + Math.floor(rnd() * 2);
      const x = 1 + Math.floor(rnd() * (14 - ancho));
      const dist = 0.5 + Math.floor(rnd() * 3);
      const z = lado < 0 ? -dist - fondo : 16 + dist;
      els.push(cascote(x, z, ancho, fondo, 0.5 + Math.floor(rnd() * 2) * 0.5));
    }
  }
  // la tira de papel despegada, por delante de la pared y colgando sobre el boquete
  const xa = 2 + Math.floor(rnd() * 7);
  els.push({
    from: [xa, 8, -0.3], to: [xa + 6, 20, -0.3], shade: false,
    faces: { north: { texture: '#papel', uv: [0, 0, 8, 16] }, south: { texture: '#papel', uv: [8, 0, 0, 16] } }
  });
  const xb = 2 + Math.floor(rnd() * 7);
  els.push({
    from: [xb, 9, 16.3], to: [xb + 6, 21, 16.3], shade: false,
    faces: { south: { texture: '#papel', uv: [0, 0, 8, 16] }, north: { texture: '#papel', uv: [8, 0, 0, 16] } }
  });
  return {
    textures: {
      zocalo: t('zocalo'), yeso: t('hueco_yeso'), interior: t('hueco_interior'),
      escombros: t('hueco_escombros'), papel: t('hueco_papel'), particle: t('zocalo')
    },
    elements: els
  };
}

const VARIANTES = 3;
for (let v = 1; v <= VARIANTES; v++) {
  const m = modelo(v);
  json(`models/block/hueco_${v}.json`, m);
  console.log(`  models/block/hueco_${v}.json  ${m.elements.length} piezas`);
}
// axis = hacia donde se pasa: z (pared a lo largo de x) o x
const lista = (giro) => Array.from({ length: VARIANTES }, (_, k) => (giro ? { model: t(`hueco_${k + 1}`), y: giro } : { model: t(`hueco_${k + 1}`) }));
json('blockstates/hueco.json', { variants: { 'axis=z': lista(0), 'axis=x': lista(90) } });
json('items/hueco.json', { model: { type: 'minecraft:model', model: t('hueco_1') } });

/* ============================================================ nombres */

const NOMBRES = {
  'block.backrooms_evento.hueco': ['Hueco en la pared', 'Hole in the wall'],
  'key.backrooms_evento.arrastrarse': ['Arrastrarse', 'Crawl'],
  'death.attack.backrooms_evento.devorado': ['%1$s fue devorado', '%1$s was devoured'],
  'death.attack.backrooms_evento.devorado.player': ['%1$s fue devorado por %2$s', '%1$s was devoured by %2$s'],
  'hud.backrooms_evento.hueco': ['MAYÚS · METERSE EN EL HUECO', 'SHIFT · CRAWL INTO THE HOLE'],
  'hud.backrooms_evento.escondido': ['ESCONDIDO', 'HIDDEN']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [clave, v] of Object.entries(NOMBRES)) lang[clave] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: hueco, arrastrarse, devorado');
