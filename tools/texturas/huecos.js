'use strict';
/**
 * Las paredes huecas del Nivel 0 (bloque backrooms_evento:hueco), como en Escape the
 * Backrooms: algunos tramos de pared son DOBLES (dos bloques de grosor) y estan
 * huecos por dentro de punta a punta, entre los dos tabiques de pladur. En uno o en
 * los dos lados el pladur esta reventado (dos columnas de ancho y 1,6 de alto): se
 * entra AGACHADO y dentro ya se puede estar de pie, a oscuras, escondido. Por dentro:
 * el reves del pladur, escombros en el suelo, el travesano de arriba y el aislante
 * colgando.
 *
 *   node tools/texturas/huecos.js
 *
 * Cada bloque de la pared hueca dice que tiene en cada cara (nada, tabique entero o
 * tabique roto) y a que altura esta; el estado se arma con "multipart" a partir de
 * modelos hechos para la cara norte (el estado los gira). La colision va en
 * escondite/Hueco.java.
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

console.log('Paredes huecas');

/* ============================================================ texturas */

// borde roto del pladur: yeso blanco sucio, granulado, con la capa de papel a un lado
guardar(TEX, 'hueco_yeso', new Lienzo(16).pintar((x, y) => {
  let c = escala([224, 219, 204], 0.9 + fbm(x, y, 501, 16) * 0.14 + hash(x, y, 502) * 0.05);
  if (hash(x, y, 503) > 0.9) c = escala(c, 0.8);
  if (y === 0) c = mezcla(c, [204, 180, 96], 0.7);
  if (fbm(x, y, 504, 16) > 0.62) c = mezcla(c, [150, 128, 84], 0.35);
  return c;
}));

// el reves del pladur, por dentro de la pared: carton gris, tornillos, humedad y sombra
guardar(TEX, 'hueco_dentro', new Lienzo(16).pintar((x, y) => {
  let c = escala([118, 112, 98], 0.82 + fbm(x, y, 511, 16) * 0.2 + hash(x, y, 512) * 0.05);
  if ((x === 3 || x === 12) && y % 5 === 2) c = [48, 46, 44];          // tornillos
  const moho = fbm(x, y, 513, 16);
  if (moho > 0.6) c = mezcla(c, [58, 54, 34], Math.min(0.8, (moho - 0.6) * 3));
  return escala(c, 0.62 + (y / 15) * 0.12);                             // mas oscuro abajo
}));

// montante y travesano de madera, en sombra
guardar(TEX, 'hueco_montante', new Lienzo(16).pintar((x, y) => {
  const veta = 0.85 + ruido(x * 0.4, y * 2.5, 6, 521, 16) * 0.22 + hash(x, y, 522) * 0.05;
  let c = escala([116, 88, 54], veta * 0.72);
  if (hash(Math.floor(x / 4), y, 523) > 0.94) c = escala(c, 0.7);       // nudos
  return c;
}));

// aislante de lana: rosado sucio, deshilachado
guardar(TEX, 'hueco_aislante', new Lienzo(16).pintar((x, y) => {
  const f = fbm(x, y, 531, 16);
  let c = escala([178, 140, 118], 0.55 + f * 0.45 + hash(x, y, 532) * 0.1);
  if (hash(x, y, 533) > 0.88) c = escala(c, 1.15);
  return escala(c, 0.8);
}));

// cascotes: polvo de yeso y trocitos sobre la moqueta
guardar(TEX, 'hueco_escombros', new Lienzo(16).pintar((x, y) => {
  let c = escala([150, 132, 74], 0.75 + hash(x, y, 541) * 0.2);
  const polvo = fbm(x, y, 542, 16);
  if (polvo > 0.45) c = mezcla(c, [210, 204, 188], Math.min(0.85, (polvo - 0.45) * 2.5));
  if (hash(x, y, 543) > 0.86) c = escala([228, 224, 210], 0.85 + hash(x, y, 544) * 0.15);
  if (hash(x, y, 545) > 0.95) c = [70, 58, 40];
  return c;
}));

// jiron de papel pintado que cuelga del borde y se mece: 12 fotogramas, forma rasgada
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
  // ancho de la tira en cada fila: ancha arriba (pegada) y en punta rasgada abajo
  const ancho = [9, 9, 8, 8, 8, 7, 7, 6, 6, 5, 4, 4, 3, 2, 1, 0];
  const desv = [0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4];
  for (let f = 0; f < N; f++) {
    const vaiven = Math.sin((f / N) * Math.PI * 2);
    for (let y = 0; y < 16; y++) {
      const dx = Math.round(vaiven * ((y / 15) ** 2) * 2.0);
      for (let k = 0; k < ancho[y]; k++) {
        const x = 2 + desv[y] + k + dx;
        if (x < 0 || x > 15) continue;
        if (k === ancho[y] - 1 && hash(k, y, 551) > 0.5) continue; // filo deshilachado
        const reves = k >= ancho[y] - 2 && y > 3;                   // se enrolla: el reves blanco
        let c = reves ? escala([206, 200, 184], 0.85 + hash(x, y, 552) * 0.1) : PAPEL.slice();
        if (!reves) {
          if (motivo(x, y)) c = escala(c, 0.9);
          c = escala(c, 0.92 + hash(x, y, 553) * 0.08);
          c = mezcla(c, [118, 96, 48], Math.max(0, (y - 6) / 14));
        }
        l.punto(x, f * 16 + y, c, 1);
      }
    }
  }
  guardar(TEX, 'hueco_papel', l);
  fs.writeFileSync(path.join(TEX, 'hueco_papel.png.mcmeta'), JSON.stringify({ animation: { frametime: 3, interpolate: true } }, null, 2) + '\n');
}

/* ======================================================= el boquete */

function azar (semilla) {
  let s = semilla >>> 0;
  return () => { s = (Math.imul(s ^ (s >>> 15), 2246822507) + 0x9e3779b9) >>> 0; return (s >>> 8) / 16777216; };
}

/**
 * Lo que queda roto (abierto) en el tabique, en una rejilla de 32x32 pixeles que
 * abarca las cuatro partes del boquete (x 0..31: izquierda y derecha; y 0..31: abajo
 * y arriba). El paso de la colision es x 2..30, y < 25,6; el dibujo llega mas arriba.
 */
function rotura (semilla) {
  const rnd = azar(semilla);
  const arriba = new Array(32).fill(0);
  for (let x = 2; x < 30;) {
    const ancho = 1 + Math.floor(rnd() * 3);
    const centro = 1 - Math.abs(x - 15.5) / 14;
    const alto = 25 + Math.round(centro * 4 + rnd() * 2.4);
    for (let k = 0; k < ancho && x < 30; k++, x++) arriba[x] = Math.min(31, alto);
  }
  const abierto = [];
  for (let y = 0; y < 32; y++) {
    abierto.push([]);
    const izq = y > 2 && y < 24 && rnd() < 0.3 ? 3 : 2;
    const der = y > 2 && y < 24 && rnd() < 0.3 ? 29 : 30;
    for (let x = 0; x < 32; x++) abierto[y].push(x >= izq && x < der && y < arriba[x]);
  }
  return abierto;
}

function rectangulos (macizo) {
  const usado = macizo.map((f) => f.map(() => false));
  const out = [];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      if (!macizo[y][x] || usado[y][x]) continue;
      let x1 = x;
      while (x1 + 1 < 16 && macizo[y][x1 + 1] && !usado[y][x1 + 1]) x1++;
      let y1 = y;
      const fila = (yy) => { for (let k = x; k <= x1; k++) if (!macizo[yy][k] || usado[yy][k]) return false; return true; };
      while (y1 + 1 < 16 && fila(y1 + 1)) y1++;
      for (let yy = y; yy <= y1; yy++) for (let k = x; k <= x1; k++) usado[yy][k] = true;
      out.push([x, y, x1 + 1, y1 + 1]);
    }
  }
  return out;
}

function caja (desde, hasta, tex, caras) {
  const f = {};
  for (const c of caras) f[c] = { texture: tex };
  return { from: desde, to: hasta, faces: f };
}

const TEXTURAS = {
  zocalo: t('zocalo'), papel_pared: t('papel_pintado'), yeso: t('hueco_yeso'), dentro: t('hueco_dentro'),
  montante: t('hueco_montante'), aislante: t('hueco_aislante'), escombros: t('hueco_escombros'),
  papel: t('hueco_papel'), particle: t('papel_pintado')
};

/** Tabique entero en la cara norte (z 0..2): papel o zocalo fuera, el reves dentro. */
function tabique (frente) {
  return {
    textures: TEXTURAS,
    elements: [{
      from: [0, 0, 0], to: [16, 16, 2],
      faces: {
        north: { texture: frente }, south: { texture: '#dentro' },
        east: { texture: frente, cullface: 'east' }, west: { texture: frente, cullface: 'west' },
        up: { texture: frente, cullface: 'up' }, down: { texture: frente, cullface: 'down' }
      }
    }]
  };
}

/** Un cuarto del boquete en el tabique de la cara norte, con su montante detras. */
function roto (parte) {
  const izq = parte.endsWith('izq');
  const arriba = parte.startsWith('arriba');
  const ox = izq ? 0 : 16;
  const oy = arriba ? 16 : 0;
  const frente = arriba ? '#papel_pared' : '#zocalo';
  const abierto = rotura(701);
  const macizo = [];
  for (let y = 0; y < 16; y++) {
    macizo.push([]);
    for (let x = 0; x < 16; x++) macizo[y].push(!abierto[oy + y][ox + x]);
  }
  const rnd = azar((izq ? 11 : 23) + (arriba ? 101 : 0));
  const els = rectangulos(macizo).map(([x0, y0, x1, y1]) => ({
    from: [x0, y0, 0], to: [x1, y1, 2],
    faces: {
      north: { texture: frente }, south: { texture: '#dentro' },
      west: x0 === 0 && izq ? { texture: frente, cullface: 'west' } : { texture: '#yeso' },
      east: x1 === 16 && !izq ? { texture: frente, cullface: 'east' } : { texture: '#yeso' },
      up: y1 === 16 && arriba ? { texture: frente, cullface: 'up' } : { texture: '#yeso' },
      down: y0 === 0 && !arriba ? { texture: frente, cullface: 'down' } : { texture: '#yeso' }
    }
  }));
  // el montante de madera del borde del boquete, justo detras del tabique
  const mx = izq ? [0, 2] : [14, 16];
  els.push(caja([mx[0], 0, 2], [mx[1], 16, 4], '#montante', [izq ? 'east' : 'west', 'north', 'south']));
  if (arriba) {
    // el dintel por dentro y un jiron de papel colgando por fuera
    els.push(caja([0, 9, 2], [16, 10, 4], '#montante', ['down', 'south']));
    if (rnd() < 0.85) {
      const xa = izq ? 5 + Math.floor(rnd() * 6) : 1 + Math.floor(rnd() * 6);
      const ya = 4 + Math.floor(rnd() * 4);
      els.push({
        from: [xa, ya, -0.2], to: [xa + 6, ya + 8, -0.2], shade: false,
        faces: { north: { texture: '#papel', uv: [0, 0, 16, 16] }, south: { texture: '#papel', uv: [16, 0, 0, 16] } }
      });
    }
  } else {
    // cascotes delante, en el pasillo
    const n = 2 + Math.floor(rnd() * 3);
    for (let k = 0; k < n; k++) {
      const ancho = 1 + Math.floor(rnd() * 2);
      const fondo = 1 + Math.floor(rnd() * 2);
      const x = 1 + Math.floor(rnd() * (14 - ancho));
      const z = -1 - Math.floor(rnd() * 3) - fondo;
      els.push(caja([x, 0, z], [x + ancho, 0.5 + Math.floor(rnd() * 2) * 0.5, z + fondo], '#yeso', ['up', 'north', 'south', 'east', 'west']));
    }
  }
  return { textures: TEXTURAS, elements: els };
}

/** El suelo por dentro: polvo de yeso y trozos de pladur. */
function sueloDentro () {
  const rnd = azar(901);
  const els = [{ from: [0, 0, 0], to: [16, 0.25, 16], faces: { up: { texture: '#escombros' } } }];
  for (let k = 0; k < 4; k++) {
    const x = Math.floor(rnd() * 13);
    const z = 2 + Math.floor(rnd() * 11);
    els.push(caja([x, 0, z], [x + 1 + Math.floor(rnd() * 3), 0.5 + Math.floor(rnd() * 2) * 0.5, z + 1 + Math.floor(rnd() * 2)], '#yeso', ['up', 'north', 'south', 'east', 'west']));
  }
  return { textures: TEXTURAS, elements: els };
}

/** Arriba por dentro: el travesano y el aislante que cuelga. */
function techoDentro () {
  const rnd = azar(911);
  const els = [caja([0, 13, 0], [16, 16, 16], '#montante', ['down'])];
  const n = 2 + Math.floor(rnd() * 2);
  for (let k = 0; k < n; k++) {
    const x = Math.floor(rnd() * 11);
    const ancho = 3 + Math.floor(rnd() * 3);
    const alto = 2 + Math.floor(rnd() * 4);
    const z = 2 + Math.floor(rnd() * 8);
    els.push(caja([x, 13 - alto, z], [Math.min(16, x + ancho), 13, Math.min(14, z + 3 + Math.floor(rnd() * 3))], '#aislante', ['down', 'north', 'south', 'east', 'west']));
  }
  // un cable que cruza
  els.push(caja([0, 11.5, 7], [16, 12, 7.5], '#dentro', ['down', 'north', 'south']));
  return { textures: TEXTURAS, elements: els };
}

// fuera los modelos de las versiones anteriores
for (const n of fs.readdirSync(path.join(ASSETS, 'models', 'block'))) {
  if (/^hueco_(\d|abajo|arriba)/.test(n)) fs.unlinkSync(path.join(ASSETS, 'models', 'block', n));
}

const PARTES = ['abajo_izq', 'abajo_der', 'arriba_izq', 'arriba_der'];
const MODELOS = {
  hueco_tabique_zocalo: tabique('#zocalo'),
  hueco_tabique_papel: tabique('#papel_pared'),
  hueco_suelo: sueloDentro(),
  hueco_techo: techoDentro()
};
for (const p of PARTES) MODELOS[`hueco_roto_${p}`] = roto(p);
for (const [n, m] of Object.entries(MODELOS)) {
  json(`models/block/${n}.json`, m);
  console.log(`  models/block/${n}.json  ${m.elements.length} piezas`);
}

// el estado: cada cara (norte, este, sur, oeste) con su tabique entero o roto, girado
const CARAS = { norte: 0, este: 90, sur: 180, oeste: 270 };
const partes = [];
const girado = (modelo, y) => (y ? { model: t(modelo), y } : { model: t(modelo) });
for (const [cara, y] of Object.entries(CARAS)) {
  partes.push({ when: { [cara]: 'entero', altura: 'suelo' }, apply: girado('hueco_tabique_zocalo', y) });
  partes.push({ when: { [cara]: 'entero', altura: 'medio|techo' }, apply: girado('hueco_tabique_papel', y) });
  for (const p of PARTES) partes.push({ when: { [cara]: 'roto', parte: p }, apply: girado(`hueco_roto_${p}`, y) });
}
partes.push({ when: { altura: 'suelo' }, apply: { model: t('hueco_suelo') } });
partes.push({ when: { altura: 'techo' }, apply: { model: t('hueco_techo') } });
json('blockstates/hueco.json', { multipart: partes });
json('items/hueco.json', { model: { type: 'minecraft:model', model: t('hueco_roto_abajo_izq') } });

/* ============================================================ nombres */

const NOMBRES = {
  'block.backrooms_evento.hueco': ['Pared hueca', 'Hollow wall'],
  // con la Bacteria como causa Minecraft usa la clave corta y le pasa su nombre como %2$s
  'death.attack.backrooms_evento.devorado': ['%1$s fue devorado por %2$s', '%1$s was devoured by %2$s'],
  'death.attack.backrooms_evento.devorado.player': ['%1$s fue devorado por %2$s', '%1$s was devoured by %2$s']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [clave, v] of Object.entries(NOMBRES)) lang[clave] = v[k];
  for (const viejo of ['key.backrooms_evento.arrastrarse', 'hud.backrooms_evento.hueco', 'hud.backrooms_evento.escondido']) delete lang[viejo];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: hueco, devorado');
