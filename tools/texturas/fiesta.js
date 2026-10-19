'use strict';
/**
 * La Fiesta (Nivel Fun, las Feeding Rooms): texturas, modelos, estados, objetos y nombres de
 * sus bloques (evento/.../fiesta/BloquesFiesta.java). Todo dibujado por codigo.
 *
 *   node tools/texturas/fiesta.js   (va en npm run texturas)
 *
 * Colores de fiesta infantil (menta, rosa, amarillo, rojo, azul) con algo que no cuadra si se
 * mira bien: el papel que se despega con manchas debajo, dibujos de ninos que no son tan
 * inocentes, carteles que insisten demasiado en que nadie se vaya, bandejas con nombres en la
 * cocina. La mayor parte parece normal; lo raro, poco y escondido.
 */

const fs = require('fs');
const path = require('path');
const { hash, fbm, ruido, mezcla, escala, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', NS);
const TEX = path.join(ASSETS, 'textures', 'block');
const ITEM = path.join(ASSETS, 'textures', 'item');
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};
const t = (n) => `${NS}:block/${n}`;
const FACINGS = { north: 0, east: 90, south: 180, west: 270 };

console.log('Fiesta');

const ROJO = [226, 56, 74];
const AMARILLO = [246, 206, 64];
const AZUL = [58, 150, 230];
const VERDE = [74, 196, 110];
const ROSA = [246, 160, 196];
const MENTA = [186, 236, 214];
const CREMA = [252, 244, 222];
const COLORES = [ROJO, AMARILLO, AZUL, VERDE];

/* ============================================================ papel */

// papel pintado menta con rayas crema finas y globitos pequenos repartidos
function papel (x, y, sucio = 0) {
  let c = (Math.floor(x / 4) % 2 === 0) ? MENTA : mezcla(MENTA, CREMA, 0.55);
  c = escala(c, 0.97 + fbm(x, y, 101, 32) * 0.05);
  // globitos: un circulo de color con su cuerda, en una rejilla girada
  const gx = ((x + Math.floor(y / 16) * 8) % 16) - 8;
  const gy = (y % 16) - 5;
  const col = COLORES[(Math.floor((x + Math.floor(y / 16) * 8) / 16) + Math.floor(y / 16)) % 4];
  if (gx * gx + gy * gy * 0.8 <= 9) c = escala(col, gx < 0 && gy < -1 ? 1.12 : 0.95);
  if (gx === 0 && gy > 3 && gy < 9) c = [120, 120, 110];
  if (sucio) {
    const m = fbm(x + 3, y + 9, 103, 32);
    if (m > 0.55) c = mezcla(c, [150, 136, 98], Math.min(0.6, (m - 0.55) * 2.4));
    // una tira despegada, irregular, que deja ver la pared de debajo (y alguna mancha oscura)
    const borde = 24 + Math.round(ruido(x, 0, 6, 105, 32) * 6);
    if (x > 18 && y > borde) c = escala([120, 112, 96], 0.75 + hash(x, y, 104) * 0.3);
    else if (x > 18 && y === borde) c = escala([236, 230, 206], 0.85);
  }
  return c;
}
guardar(TEX, 'papel_fiesta', new Lienzo(32).pintar((x, y) => papel(x, y)));
guardar(TEX, 'papel_fiesta_sucio', new Lienzo(32).pintar((x, y) => papel(x, y, 1)));

// rosa con estrellitas amarillas
guardar(TEX, 'papel_estrellas', new Lienzo(32).pintar((x, y) => {
  let c = escala(ROSA, 0.95 + fbm(x, y, 111, 32) * 0.07);
  const sx = ((x + (Math.floor(y / 11) % 2) * 5) % 11) - 5;
  const sy = (y % 11) - 5;
  const estrella = (Math.abs(sx) + Math.abs(sy) <= 2) || (sx === 0 && Math.abs(sy) <= 3) || (sy === 0 && Math.abs(sx) <= 3);
  if (estrella) c = escala(AMARILLO, Math.abs(sx) + Math.abs(sy) <= 1 ? 1.1 : 0.95);
  return c;
}));

// zocalo de rayas de colores (como un rodapie de guarderia) con un borde de madera arriba
guardar(TEX, 'zocalo_fiesta', new Lienzo(32).pintar((x, y) => {
  if (y < 4) return escala([196, 150, 98], 0.9 + hash(x, y, 121) * 0.12 - (y === 3 ? 0.2 : 0));
  const c = COLORES[Math.floor(x / 8) % 4];
  let k = 0.92 + fbm(x, y, 122, 32) * 0.08;
  if (y > 26) k *= 0.85 + hash(x, y, 123) * 0.1; // rozado por abajo
  return escala(c, k);
}));

/* ============================================================ suelos */

// suelo de goma de puzle: piezas de 16 de colores con los dientes del puzle
function goma (x, y, sucio) {
  const px = Math.floor(x / 16);
  const py = Math.floor(y / 16);
  const col = COLORES[(px + py * 2) % 4];
  const lx = x % 16;
  const ly = y % 16;
  let c = escala(col, 0.9 + fbm(x, y, 131, 32) * 0.06 + (hash(x, y, 132) - 0.5) * 0.04);
  // los dientes: un circulito en el centro de cada borde que es de la pieza de al lado
  const diente = (Math.abs(lx - 8) <= 1 && (ly <= 1 || ly >= 14)) || (Math.abs(ly - 8) <= 1 && (lx <= 1 || lx >= 14));
  if (diente) c = escala(COLORES[(px + py * 2 + 1) % 4], 0.88);
  if (lx === 0 || ly === 0) c = escala(c, 0.7);
  if (sucio) {
    const m = fbm(x, y, 133, 32);
    if (m > 0.52) c = mezcla(c, [90, 70, 52], Math.min(0.55, (m - 0.52) * 2.2));
    if (hash(x, y, 134) > 0.985) c = [70, 40, 30];
  }
  return c;
}
guardar(TEX, 'suelo_goma', new Lienzo(32).pintar((x, y) => goma(x, y, false)));
guardar(TEX, 'suelo_goma_sucio', new Lienzo(32).pintar((x, y) => goma(x, y, true)));

// moqueta azul oscuro con confeti (puntos y rayitas de colores)
guardar(TEX, 'moqueta_confeti', new Lienzo(32).pintar((x, y) => {
  let c = escala([46, 52, 120], 0.86 + fbm(x, y, 141, 32) * 0.2 + hash(x, y, 142) * 0.06);
  const h = hash(Math.floor(x / 3), Math.floor(y / 3), 143);
  if (h > 0.86 && x % 3 !== 2 && y % 3 === 1) c = escala(COLORES[Math.floor(h * 97) % 4], 0.95);
  return c;
}));

// baldosa de cocina blanca y negra, con las juntas grises y algo de grasa
guardar(TEX, 'baldosa_cocina', new Lienzo(32).pintar((x, y) => {
  const blanca = (Math.floor(x / 8) + Math.floor(y / 8)) % 2 === 0;
  let c = blanca ? [232, 230, 222] : [38, 38, 42];
  c = escala(c, 0.94 + hash(x, y, 151) * 0.08);
  if (x % 8 === 0 || y % 8 === 0) c = [120, 116, 108];
  const g = fbm(x, y, 152, 32);
  if (g > 0.6) c = mezcla(c, [140, 120, 70], (g - 0.6) * 0.9);
  return c;
}));

/** Una baldosa brillante del damero: el color, la junta oscura alrededor y un reflejo arriba a la izquierda. */
function damero (col, x, y) {
  if (x === 0 || y === 0) return escala(col, 0.62);
  let c = escala(col, 0.95 + fbm(x, y, 181, 32) * 0.06 - (x + y) * 0.0018);
  const d = Math.hypot(x - 9, y - 8);
  if (d < 6) c = mezcla(c, [255, 255, 255], (6 - d) / 6 * 0.28);
  if (hash(x, y, 182) > 0.985) c = escala(c, 0.85);
  return c;
}

/* ============================================================ techo y luces */

guardar(TEX, 'techo_fiesta', new Lienzo(32).pintar((x, y) => {
  let c = escala([238, 236, 228], 0.95 + fbm(x, y, 161, 32) * 0.05);
  if (x % 16 === 0 || y % 16 === 0) c = [188, 186, 178];
  const h = hash(Math.floor(x / 4), Math.floor(y / 4), 162);
  if (h > 0.9 && x % 4 === 2 && y % 4 === 2) c = COLORES[Math.floor(h * 31) % 4];
  return c;
}));
const luz = (encendida) => new Lienzo(32).pintar((x, y) => {
  if (x < 2 || y < 2 || x > 29 || y > 29) return [200, 196, 186];
  const d = Math.hypot(x - 15.5, y - 15.5) / 16;
  return encendida ? mezcla([255, 250, 226], [255, 226, 180], d) : mezcla([170, 166, 156], [130, 126, 118], d);
});
guardar(TEX, 'luz_fiesta', luz(true));
guardar(TEX, 'luz_fiesta_apagada', luz(false));

/* ============================================================ acolchados */

// vinilo acolchado: cuadros con costuras en cruz y el brillo de cada almohadilla
for (const [n, col] of [['rojo', ROJO], ['amarillo', AMARILLO], ['azul', AZUL], ['verde', VERDE]]) {
  guardar(TEX, `acolchado_${n}`, new Lienzo(32).pintar((x, y) => {
    const lx = x % 16;
    const ly = y % 16;
    const d = Math.hypot(lx - 7.5, ly - 7.5) / 10;
    let c = escala(col, 1.08 - d * 0.28 + fbm(x, y, 171, 32) * 0.05);
    if (lx === 0 || ly === 0) c = escala(col, 0.55);
    if ((lx === 0 || ly === 0) && (x + y) % 3 === 0) c = escala(col, 0.8);
    if (lx === 4 && ly === 4) c = mezcla(c, [255, 255, 255], 0.5);
    return c;
  }));
}
console.log('  paredes, suelos, techo, luces y acolchados');

/* ============================================================ decoracion */

const mod = (textures, elements) => ({ parent: 'minecraft:block/block', textures, elements });
const caja = (from, to, tex, extra = {}) => {
  const faces = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) faces[d] = { texture: tex };
  return { from, to, faces, ...extra };
};

// globos: cuerpo, nudo y cuerda (4 colores)
const ldGlobo = (col) => new Lienzo(16).pintar((x, y) => {
  if (x < 8) {
    const d = Math.hypot(x - 2.5, y - 3);
    return d < 2.5 ? mezcla(col, [255, 255, 255], 0.45) : escala(col, 0.95 + hash(x, y, 181) * 0.08 - y * 0.012);
  }
  return [236, 236, 230];
});
const NOMBRES_COL = ['rojo', 'amarillo', 'azul', 'verde'];
COLORES.forEach((col, i) => {
  guardar(TEX, `globo_${NOMBRES_COL[i]}`, ldGlobo(col));
  json(`models/block/globo_${i}.json`, mod({ g: t(`globo_${NOMBRES_COL[i]}`), particle: t(`globo_${NOMBRES_COL[i]}`) }, [
    caja([5, 8, 5], [11, 15, 11], '#g', { faces: undefined }),
    caja([4.4, 9, 5.6], [11.6, 14, 10.4], '#g', { faces: undefined }),
    caja([5.6, 9, 4.4], [10.4, 14, 11.6], '#g', { faces: undefined }),
    caja([6, 15, 6], [10, 15.8, 10], '#g'),
    caja([6, 7, 6], [10, 8, 10], '#g'),
    caja([7.5, 5, 7.5], [8.5, 6, 8.5], '#g'),
    { from: [8, 0, 8], to: [8, 5, 8.01], faces: { north: { texture: '#g', uv: [12, 0, 12.5, 16] }, south: { texture: '#g', uv: [12, 0, 12.5, 16] } } }
  ].map((e) => {
    if (!e.faces) {
      e.faces = {};
      for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) e.faces[d] = { texture: '#g', uv: [0, 0, 7, 8] };
    }
    for (const f of Object.values(e.faces)) if (!f.uv) f.uv = [0, 0, 7, 8];
    return e;
  })));
});

/** El blockstate de un bloque con FACING y VARIANTE: `modelo(v)` da el modelo de cada variante. */
function estados (id, nVariantes, modelo, sinGiro = false) {
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    for (let i = 0; i < nVariantes; i++) {
      v[`facing=${f},variante=${i}`] = { model: t(modelo(i)), ...(y && !sinGiro ? { y } : {}) };
    }
  }
  json(`blockstates/${id}.json`, { variants: v });
  json(`items/${id}.json`, { model: { type: 'minecraft:model', model: t(modelo(0)) } });
}
estados('globo', 4, (i) => `globo_${i}`, true);

/** Plano pegado a la pared (como los rotulos): la textura en la cara de delante. */
const enPared = (tex, emision = 0) => mod({ f: t(tex), particle: t(tex) }, [{
  from: [0, 0, 15.9], to: [16, 16, 15.9], shade: emision === 0, ...(emision ? { light_emission: emision } : {}),
  faces: { north: { texture: '#f', uv: [0, 0, 16, 16] } }
}]);

// guirnaldas de banderines (dos: colores calidos y frios), colgando de una cuerda
for (let v = 0; v < 2; v++) {
  guardar(TEX, `guirnalda_${v}`, new Lienzo(32).pintar((x, y) => {
    const cuerda = 3 + Math.round(Math.sin((x / 32) * Math.PI) * 2);
    if (y === cuerda) return [80, 70, 60];
    const k = Math.floor(x / 8);
    const lx = x % 8;
    if (y > cuerda && y < cuerda + 10 && Math.abs(lx - 3.5) < (cuerda + 10 - y) * 0.42) {
      return escala(COLORES[(k + v * 2) % 4], 0.95 + hash(x, y, 191) * 0.08);
    }
    return null;
  }));
  json(`models/block/guirnalda_${v}.json`, enPared(`guirnalda_${v}`));
}
estados('guirnalda', 8, (i) => `guirnalda_${i % 2}`);

// carteles de la fiesta (64x64): fondo de color, borde, el texto y algun dibujo
const CARTELES = [
  { fondo: AMARILLO, texto: ['VEN A LA', 'FIESTA!'], tinta: ROJO, dibujo: 'globos' },
  { fondo: [250, 214, 90], texto: ['PREMIO', '1500', 'TICKETS'], tinta: [150, 30, 40], dibujo: 'ticket' },
  { fondo: CREMA, texto: ['NADIE SE', 'VA SIN', 'SU PREMIO'], tinta: [70, 40, 120], dibujo: 'estrellas' },
  { fondo: AZUL, texto: ['SONRIE!'], tinta: CREMA, dibujo: 'sonrisa' },
  { fondo: ROSA, texto: ['TU AMIGO', 'EL', 'ANFITRION'], tinta: [90, 30, 90], dibujo: 'anfitrion' },
  { fondo: VERDE, texto: ['NO CORRER', 'EN LA', 'FIESTA'], tinta: CREMA, dibujo: 'nada' },
  { fondo: [236, 236, 228], texto: ['LA COMIDA', 'SE SIRVE', 'PRONTO'], tinta: [60, 60, 60], dibujo: 'plato' },
  { fondo: [40, 40, 46], texto: ['SEMAFORO', 'LUZ VERDE', 'LUZ ROJA'], tinta: AMARILLO, dibujo: 'semaforo' }
];
CARTELES.forEach((c, i) => {
  const l = new Lienzo(64);
  l.pintar((x, y) => {
    if (x < 3 || y < 3 || x > 60 || y > 60) return null;
    if (x < 5 || y < 5 || x > 58 || y > 58) return escala(c.tinta, 0.9);
    return escala(c.fondo, 0.95 + fbm(x, y, 200 + i, 64) * 0.08);
  });
  const dibujosArriba = ['globos', 'ticket', 'sonrisa', 'anfitrion', 'semaforo', 'plato'].includes(c.dibujo);
  let y = dibujosArriba ? (c.texto.length > 2 ? 30 : 36) : Math.round(32 - c.texto.length * 5);
  for (const linea of c.texto) {
    l.texto(linea, 'centro', y, c.tinta, 1);
    y += 9;
  }
  switch (c.dibujo) {
    case 'globos':
      [[16, 16, ROJO], [30, 12, AZUL], [46, 17, VERDE]].forEach(([gx, gy, col]) => { l.disco(gx, gy, 6, col); l.linea(gx, gy + 6, gx + 2, gy + 15, [80, 70, 60], 1); });
      break;
    case 'ticket':
      l.rect(18, 10, 45, 26, ROJO);
      l.disco(18, 18, 3, c.fondo); l.disco(45, 18, 3, c.fondo);
      l.texto('ADMIT', 'centro', 15, CREMA, 1);
      break;
    case 'sonrisa': {
      l.disco(32, 18, 12, AMARILLO);
      l.disco(27, 14, 1.6, [30, 30, 30]); l.disco(37, 14, 1.6, [30, 30, 30]);
      for (let x = 24; x <= 40; x++) l.punto(x, 21 + Math.round(Math.pow((x - 32) / 8, 2) * -3 + 3), [30, 30, 30]);
      break;
    }
    case 'anfitrion': {
      l.disco(32, 17, 11, [222, 196, 160]);
      l.poligono([[27, 7], [37, 7], [32, -4]], AZUL);
      l.rect(27, 13, 29, 15, [20, 20, 20]); l.rect(35, 13, 37, 15, [20, 20, 20]);
      l.disco(32, 18, 2, ROJO);
      for (let x = 24; x <= 40; x++) l.punto(x, 23 - Math.round(Math.pow((x - 32) / 8, 2) * 2), [60, 10, 14]);
      break;
    }
    case 'semaforo':
      l.rect(26, 6, 38, 30, [20, 20, 22]);
      l.disco(32, 12, 4, ROJO); l.disco(32, 24, 4, VERDE);
      break;
    case 'plato':
      l.disco(32, 18, 11, [250, 250, 250]); l.disco(32, 18, 8, [230, 228, 220]);
      l.linea(14, 8, 14, 28, [150, 150, 150], 2); l.linea(50, 8, 50, 28, [150, 150, 150], 2);
      break;
    case 'estrellas':
      for (const [sx, sy] of [[12, 12], [52, 12], [12, 52], [52, 52]]) l.poligono([[sx, sy - 5], [sx + 2, sy - 1], [sx + 5, sy], [sx + 2, sy + 1], [sx, sy + 5], [sx - 2, sy + 1], [sx - 5, sy], [sx - 2, sy - 1]], AMARILLO);
      break;
    default:
  }
  guardar(TEX, `cartel_fiesta_${i}`, l);
  json(`models/block/cartel_fiesta_${i}.json`, enPared(`cartel_fiesta_${i}`));
});
estados('cartel_fiesta', 8, (i) => `cartel_fiesta_${i}`);

// dibujos de ninos con cera: los primeros, normales; los ultimos, no tanto
const CERA = (l, x0, y0, x1, y1, col, g = 1.4) => l.linea(x0, y0, x1, y1, col, g, 0.9, 0.9);
const papelito = () => new Lienzo(32).pintar((x, y) => (x < 2 || y < 2 || x > 29 || y > 29 ? null : escala([250, 248, 240], 0.96 + hash(x, y, 211) * 0.05)));
const monigote = (l, x, y, col, ojosX = false) => {
  l.disco(x, y, 2.5, col, 0.9);
  CERA(l, x, y + 2, x, y + 8, col); CERA(l, x, y + 4, x - 3, y + 6, col); CERA(l, x, y + 4, x + 3, y + 6, col);
  CERA(l, x, y + 8, x - 2, y + 12, col); CERA(l, x, y + 8, x + 2, y + 12, col);
  if (ojosX) { l.punto(x - 1, y - 1, [20, 20, 20]); l.punto(x + 1, y - 1, [20, 20, 20]); }
};
const DIBUJOS = [
  (l) => { l.disco(24, 8, 4, AMARILLO); CERA(l, 6, 20, 14, 13, ROJO); CERA(l, 14, 13, 22, 20, ROJO); l.rect(7, 20, 21, 27, [230, 150, 90]); l.rect(12, 23, 15, 27, [120, 70, 40]); },
  (l) => { monigote(l, 9, 9, AZUL); monigote(l, 16, 11, ROJO); monigote(l, 23, 9, VERDE); l.disco(9, 3, 2, ROJO); CERA(l, 9, 5, 9, 9, [60, 60, 60], 0.8); },
  (l) => { l.disco(16, 8, 4.5, [222, 196, 160]); l.poligono([[13, 4], [19, 4], [16, -1]], AZUL); CERA(l, 13, 10, 19, 10, ROJO, 1); monigote(l, 7, 14, AZUL); monigote(l, 25, 14, VERDE); CERA(l, 16, 13, 16, 26, [104, 52, 132], 2.4); },
  (l) => { for (let k = 0; k < 4; k++) for (let a = 0; a < 30; a++) { const ang = Math.PI * a / 29; l.punto(16 + Math.cos(ang) * (12 - k * 2.2), 22 - Math.sin(ang) * (12 - k * 2.2), COLORES[k], 0.9); } },
  // los ninos durmiendo en fila y, detras, la figura alta que sonrie
  (l) => { for (let k = 0; k < 4; k++) { l.rect(3 + k * 7, 22, 8 + k * 7, 25, AZUL); l.disco(4 + k * 7, 21, 1.5, [230, 190, 160]); } CERA(l, 27, 4, 27, 19, [40, 40, 40], 1.8); l.disco(27, 5, 2.5, [40, 40, 40]); CERA(l, 25, 6, 29, 6, ROJO, 0.8); },
  // un monton de ninos entrando en una boca enorme con dientes
  (l) => { l.disco(22, 14, 8, [40, 10, 14]); for (let k = 0; k < 5; k++) l.rect(15 + k * 3, 7, 16 + k * 3, 9, CREMA); for (let k = 0; k < 4; k++) monigote(l, 4 + k * 3, 14 + (k % 2), COLORES[k]); },
  // NO ME DEJAN SALIR, apretando mucho la cera
  (l) => { l.texto('NO ME', 'centro', 7, [30, 30, 30], 1); l.texto('DEJAN', 'centro', 15, [30, 30, 30], 1); l.texto('SALIR', 'centro', 23, ROJO, 1); },
  // un nino con los ojos en cruz y un monton de tickets alrededor
  (l) => { monigote(l, 16, 9, VERDE, true); for (let k = 0; k < 6; k++) l.rect(3 + k * 5, 26 - (k % 2) * 2, 6 + k * 5, 28 - (k % 2) * 2, ROJO); }
];
DIBUJOS.forEach((d, i) => {
  const l = papelito();
  d(l);
  guardar(TEX, `dibujo_fiesta_${i}`, l);
  json(`models/block/dibujo_fiesta_${i}.json`, enPared(`dibujo_fiesta_${i}`));
});
estados('dibujo_fiesta', 8, (i) => `dibujo_fiesta_${i}`);

// altavoz de megafonia: caja con rejilla
guardar(TEX, 'altavoz_fiesta', new Lienzo(16).pintar((x, y) => {
  if (x < 3 || y < 3 || x > 12 || y > 12) return null;
  if (x === 3 || y === 3 || x === 12 || y === 12) return [70, 72, 76];
  return (x + y) % 2 === 0 ? [40, 42, 46] : [96, 98, 104];
}));
json('models/block/altavoz_fiesta.json', enPared('altavoz_fiesta'));
estados('altavoz_fiesta', 8, () => 'altavoz_fiesta');

// mesa con mantel de cuadros (4 colores): tablero y faldon por los cuatro lados
COLORES.forEach((col, i) => {
  guardar(TEX, `mantel_${i}`, new Lienzo(16).pintar((x, y) => ((Math.floor(x / 2) + Math.floor(y / 2)) % 2 === 0 ? escala(col, 0.95) : mezcla(col, CREMA, 0.7))));
  json(`models/block/mesa_fiesta_${i}.json`, mod({ m: t(`mantel_${i}`), particle: t(`mantel_${i}`) }, [
    caja([0, 11, 0], [16, 13, 16], '#m'),
    caja([0, 4, 0], [16, 11, 0.2], '#m'), caja([0, 4, 15.8], [16, 11, 16], '#m'),
    caja([0, 4, 0], [0.2, 11, 16], '#m'), caja([15.8, 4, 0], [16, 11, 16], '#m')
  ]));
});
estados('mesa_fiesta', 4, (i) => `mesa_fiesta_${i}`);

// tartas (fresa, chocolate, vainilla y una a medio comer) con velas
const SABORES = [[246, 170, 196], [120, 70, 44], [250, 236, 196], [246, 170, 196]];
SABORES.forEach((s, i) => {
  guardar(TEX, `tarta_${i}`, new Lienzo(16).pintar((x, y) => {
    if (y < 3) return escala(CREMA, 0.95 + hash(x, y, 221) * 0.08);
    if (y === 3 && x % 3 === 0) return CREMA;
    if (y > 8 && y < 11) return escala(ROJO, 0.8);
    return escala(s, 0.9 + hash(x, y, 222 + i) * 0.1);
  }));
  const els = [caja([3, 0, 3], [13, 6, 13], '#t'), caja([5, 6, 7.5], [5.8, 9, 8.3], '#v'), caja([10, 6, 7.5], [10.8, 9, 8.3], '#v'), caja([7.6, 6, 5], [8.4, 9, 5.8], '#v')];
  if (i === 3) els[0] = caja([3, 0, 3], [13, 6, 8], '#t');
  json(`models/block/tarta_${i}.json`, mod({ t: t(`tarta_${i}`), v: t('globo_azul'), particle: t(`tarta_${i}`) }, els));
});
estados('tarta', 4, (i) => `tarta_${i}`);

// regalos (4 colores) con su lazo en cruz
COLORES.forEach((col, i) => {
  const lazo = COLORES[(i + 1) % 4];
  guardar(TEX, `regalo_${i}`, new Lienzo(16).pintar((x, y) => (Math.abs(x - 7.5) < 1.5 || Math.abs(y - 7.5) < 1.5 ? escala(lazo, 1.05) : escala(col, 0.92 + hash(x, y, 231) * 0.08))));
  json(`models/block/regalo_${i}.json`, mod({ r: t(`regalo_${i}`), particle: t(`regalo_${i}`) }, [
    caja([2, 0, 2], [14, 12, 14], '#r'), caja([5, 12, 7], [11, 14, 9], '#r')
  ]));
});
estados('regalo', 4, (i) => `regalo_${i}`);

// sillitas de plastico
COLORES.forEach((col, i) => {
  guardar(TEX, `sillita_${i}`, new Lienzo(16).pintar((x, y) => escala(col, 0.9 + hash(x, y, 241) * 0.1 - (y > 12 ? 0.1 : 0))));
  json(`models/block/sillita_${i}.json`, mod({ s: t(`sillita_${i}`), particle: t(`sillita_${i}`) }, [
    caja([3, 6, 3], [13, 8, 13], '#s'), caja([3, 8, 11], [13, 15, 13], '#s'),
    caja([3, 0, 3], [5, 6, 5], '#s'), caja([11, 0, 3], [13, 6, 5], '#s'), caja([3, 0, 11], [5, 6, 13], '#s'), caja([11, 0, 11], [13, 6, 13], '#s')
  ]));
});
estados('sillita', 4, (i) => `sillita_${i}`);

// confeti tirado en el suelo
for (let v = 0; v < 4; v++) {
  guardar(TEX, `confeti_${v}`, new Lienzo(16).pintar((x, y) => {
    const h = hash(x, y, 251 + v);
    return h > 0.9 ? COLORES[Math.floor(h * 1000) % 4] : null;
  }));
  json(`models/block/confeti_${v}.json`, mod({ c: t(`confeti_${v}`), particle: t(`confeti_${v}`) }, [{
    from: [0, 0.1, 0], to: [16, 0.1, 16], faces: { up: { texture: '#c', uv: [0, 0, 16, 16] } }
  }]));
}
estados('confeti', 4, (i) => `confeti_${i}`);

// peluches: osito (marron, rosa, azul) y uno al que le falta un ojo y tiene una costura abierta
[[150, 100, 60], ROSA, AZUL, [150, 100, 60]].forEach((col, i) => {
  guardar(TEX, `peluche_${i}`, new Lienzo(16).pintar((x, y) => {
    let c = escala(col, 0.88 + hash(x, y, 261 + i) * 0.14);
    if (y === 4 && (x === 5 || x === 10)) c = (i === 3 && x === 10) ? [236, 226, 210] : [20, 18, 18];
    if (y === 7 && x >= 6 && x <= 9) c = [60, 30, 26];
    if (i === 3 && x === 3 && y > 8) c = [236, 226, 210];
    return c;
  }));
  json(`models/block/peluche_${i}.json`, mod({ p: t(`peluche_${i}`), particle: t(`peluche_${i}`) }, [
    caja([5, 0, 6], [11, 6, 10], '#p'), caja([5.5, 6, 6.5], [10.5, 10.5, 10], '#p'),
    caja([5, 10, 7.5], [6.5, 11.5, 9], '#p'), caja([9.5, 10, 7.5], [11, 11.5, 9], '#p'),
    caja([3.5, 2, 7], [5, 5.5, 9], '#p'), caja([11, 2, 7], [12.5, 5.5, 9], '#p')
  ]));
});
estados('peluche', 4, (i) => `peluche_${i}`);

// ollas de acero enormes y bandejas con la etiqueta de un nombre
guardar(TEX, 'olla', new Lienzo(16).pintar((x, y) => escala([176, 180, 184], 0.8 + ruido(x, y * 0.3, 4, 271, 16) * 0.25 - (y < 2 ? 0.2 : 0))));
json('models/block/olla.json', mod({ o: t('olla'), particle: t('olla') }, [
  caja([1, 0, 1], [15, 12, 15], '#o'), caja([0, 9, 7], [1, 11, 9], '#o'), caja([15, 9, 7], [16, 11, 9], '#o'), caja([2, 12, 2], [14, 13, 14], '#o')
]));
estados('olla', 4, () => 'olla');
for (let v = 0; v < 4; v++) {
  guardar(TEX, `bandeja_${v}`, new Lienzo(16).pintar((x, y) => {
    let c = escala([196, 200, 204], 0.9 + hash(x, y, 281) * 0.08);
    if (Math.hypot(x - 8, y - 7) < 4) c = escala([150 - v * 20, 120, 90], 0.85 + hash(x, y, 282 + v) * 0.2);
    if (y >= 12 && y <= 14 && x >= 3 && x <= 12) c = y === 13 && x % 2 === 0 && x > 3 && x < 12 ? [60, 60, 70] : [244, 240, 228];
    return c;
  }));
  json(`models/block/bandeja_${v}.json`, mod({ b: t(`bandeja_${v}`), particle: t(`bandeja_${v}`) }, [caja([1, 0, 3], [15, 1.5, 13], '#b')]));
}
estados('bandeja', 4, (i) => `bandeja_${i}`);

// camitas de la siesta: somier, colchon de color y almohada
COLORES.forEach((col, i) => {
  guardar(TEX, `camita_${i}`, new Lienzo(16).pintar((x, y) => (y < 5 ? escala(CREMA, 0.95 + hash(x, y, 291) * 0.05) : escala(col, 0.85 + ((x + y) % 4 === 0 ? 0.1 : 0) + hash(x, y, 292) * 0.05))));
  json(`models/block/camita_${i}.json`, mod({ c: t(`camita_${i}`), m: t('olla'), particle: t(`camita_${i}`) }, [
    caja([0, 0, 0], [16, 3, 16], '#m'), caja([0.5, 3, 0.5], [15.5, 6, 15.5], '#c'), caja([3, 6, 1], [13, 7.5, 5], '#c')
  ]));
});
estados('camita', 4, (i) => `camita_${i}`);
console.log('  decoracion: globos, guirnaldas, carteles, dibujos, mesas, tartas, regalos, sillitas, confeti, peluches, cocina, camitas');

/* ============================================================ para jugar */


/* ============================================================ suelos defectuosos y juegos */

// el suelo defectuoso: la misma goma (o moqueta) que alrededor, animada. Casi siempre igual;
// a ratos un parpadeo, una franja desplazada o los colores que se corrompen un instante
{
  const base = [(x, y) => goma(x, y, false), null];
  const moqueta = (x, y) => {
    let c = escala([46, 52, 120], 0.86 + fbm(x, y, 141, 32) * 0.2 + hash(x, y, 142) * 0.06);
    const h = hash(Math.floor(x / 3), Math.floor(y / 3), 143);
    if (h > 0.86 && x % 3 !== 2 && y % 3 === 1) c = escala(COLORES[Math.floor(h * 97) % 4], 0.95);
    return c;
  };
  base[1] = moqueta;
  // el damero del laberinto: un bloque por color (blanco, azul, amarillo), como PlanoFiesta#damero
  for (const col of [[232, 232, 226], [52, 92, 196], [236, 196, 58]]) base.push((x, y) => damero(col, x, y));
  base.forEach((f, v) => {
    const cuadros = [];
    for (let k = 0; k < 6; k++) {
      cuadros.push(new Lienzo(32).pintar((x, y) => {
        if (k === 0) return f(x, y);
        if (k === 1) return escala(f(x, y), 0.82);                                   // parpadeo
        if (k === 2) { const d = (y >= 10 && y < 16) ? 4 : 0; return f((x + d) % 32, y); } // franja corrida
        if (k === 3) { const c = f(x, y); return [c[2], c[0], c[1]]; }              // colores cambiados
        if (k === 4) return (y % 3 === 0) ? escala(f(x, y), 0.6) : f(x, y);         // lineas
        return mezcla(f(x, y), [255, 255, 255], (x + y) % 7 === 0 ? 0.25 : 0);      // ruido
      }));
    }
    const nombre = `suelo_defectuoso_${v}`;
    guardar(TEX, nombre, Lienzo.tira(cuadros));
    // casi todo el rato el cuadro normal; los otros, muy de vez en cuando y muy cortos
    const frames = [{ index: 0, time: 70 }, { index: 1, time: 2 }, { index: 0, time: 40 }, { index: 2, time: 2 }, { index: 0, time: 3 },
      { index: 4, time: 2 }, { index: 0, time: 90 }, { index: 3, time: 1 }, { index: 5, time: 2 }, { index: 0, time: 55 }];
    fs.writeFileSync(path.join(TEX, `${nombre}.png.mcmeta`), JSON.stringify({ animation: { frametime: 1, frames } }, null, 2) + '\n');
    json(`models/block/${nombre}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(nombre) } });
  });
  const v = {};
  for (const roto of [false, true]) for (let i = 0; i < 5; i++) v[`roto=${roto},variante=${i}`] = { model: t(`suelo_defectuoso_${i}`) };
  json('blockstates/suelo_defectuoso.json', { variants: v });
  json('items/suelo_defectuoso.json', { model: { type: 'minecraft:model', model: t('suelo_defectuoso_0') } });
}

// la piscina de bolas: bolas de colores amontonadas
guardar(TEX, 'piscina_bolas', new Lienzo(32).pintar((x, y) => {
  const cx = Math.floor(x / 5);
  const cy = Math.floor(y / 5);
  const ox = (x % 5) - 2 + (hash(cx, cy, 341) - 0.5);
  const oy = (y % 5) - 2 + (hash(cx, cy, 342) - 0.5);
  const col = COLORES[Math.floor(hash(cx, cy, 343) * 4)];
  const d = Math.hypot(ox, oy);
  if (d > 2.6) return [40, 36, 44];
  return escala(col, 1.15 - d * 0.18 + (ox < 0 && oy < 0 ? 0.1 : 0));
}));
json('models/block/piscina_bolas.json', { parent: 'minecraft:block/block', textures: { b: t('piscina_bolas'), particle: t('piscina_bolas') },
  elements: [caja([0, 0, 0], [16, 6, 16], '#b')] });
json('blockstates/piscina_bolas.json', { variants: { '': { model: t('piscina_bolas') } } });
json('items/piscina_bolas.json', { model: { type: 'minecraft:model', model: t('piscina_bolas') } });

// la trampilla del final del parkour: placa dorada con una flecha hacia arriba que brilla
{
  const l = new Lienzo(32).pintar((x, y) => {
    let c = escala([222, 182, 60], 0.9 + hash(x, y, 351) * 0.1);
    if (x < 2 || y < 2 || x > 29 || y > 29) c = [150, 110, 30];
    return c;
  });
  l.poligono([[16, 5], [26, 16], [20, 16], [20, 27], [12, 27], [12, 16], [6, 16]], [255, 250, 220]);
  guardar(TEX, 'trampilla_fiesta', l);
  json('models/block/trampilla_fiesta.json', { parent: 'minecraft:block/cube_all', textures: { all: t('trampilla_fiesta') } });
  json('blockstates/trampilla_fiesta.json', { variants: { '': { model: t('trampilla_fiesta') } } });
  json('items/trampilla_fiesta.json', { model: { type: 'minecraft:model', model: t('trampilla_fiesta') } });
}

// la nota de la fiesta: papel de colores doblado con letra de cera
guardar(TEX, 'nota_fiesta', new Lienzo(16).pintar((x, y) => {
  if (x < 3 || y < 3 || x > 12 || y > 12) return null;
  let c = escala([250, 232, 196], 0.94 + hash(x, y, 361) * 0.06);
  if (y % 2 === 1 && x > 4 && x < 11 && hash(x, y, 362) > 0.3) c = [70, 60, 120];
  if (x === 8) c = escala(c, 0.88);
  return c;
}));
json('models/block/nota_fiesta.json', mod({ n: t('nota_fiesta'), particle: t('nota_fiesta') }, [{
  from: [0, 0.1, 0], to: [16, 0.1, 16], faces: { up: { texture: '#n', uv: [0, 0, 16, 16] } } }]));
{
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) for (let i = 0; i < 8; i++) v[`facing=${f},variante=${i}`] = { model: t('nota_fiesta'), ...(y ? { y } : {}) };
  json('blockstates/nota_fiesta.json', { variants: v });
  json('items/nota_fiesta.json', { model: { type: 'minecraft:model', model: t('nota_fiesta') } });
}

// las baldosas de los juegos: de color (apagada, mate; encendida, brillante con el centro casi
// blanco) y la estrella de empezar
{
  const v = {};
  const COLS = [...COLORES, [240, 240, 236]];
  COLS.forEach((col, c) => {
    for (const luz of [false, true]) {
      const n = `baldosa_juego_${c}${luz ? '_luz' : ''}`;
      guardar(TEX, n, new Lienzo(16).pintar((x, y) => {
        if (x === 0 || y === 0 || x === 15 || y === 15) return [40, 40, 44];
        const d = Math.hypot(x - 7.5, y - 7.5) / 8;
        let k = luz ? mezcla([255, 255, 250], col, Math.min(1, d * 1.4)) : escala(col, 0.45 + (1 - d) * 0.1);
        if (c === 4) {
          // la estrella: amarilla sobre blanco
          const a = Math.atan2(y - 7.5, x - 7.5);
          const r = Math.hypot(x - 7.5, y - 7.5);
          const pico = 3 + 3.5 * Math.pow(Math.abs(Math.cos(a * 2.5)), 2);
          k = r < pico ? (luz ? [255, 236, 120] : [230, 190, 60]) : escala([236, 232, 220], 0.9);
        }
        return k;
      }));
      json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(n) } });
      v[`color=${c},luz=${luz}`] = { model: t(n) };
    }
  });
  json('blockstates/baldosa_juego.json', { variants: v });
  json('items/baldosa_juego.json', { model: { type: 'minecraft:model', model: t('baldosa_juego_4_luz') } });
}
console.log('  suelos defectuosos, piscina de bolas, trampilla, notas y baldosas de juego');

/* ============================================================ bloques simples */

const SIMPLES = ['papel_fiesta', 'papel_estrellas', 'papel_fiesta_sucio', 'zocalo_fiesta', 'suelo_goma', 'suelo_goma_sucio', 'moqueta_confeti',
  'baldosa_cocina', 'techo_fiesta', 'luz_fiesta', 'acolchado_rojo', 'acolchado_amarillo', 'acolchado_azul', 'acolchado_verde'];
for (const n of SIMPLES) {
  json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(n) } });
  json(`blockstates/${n}.json`, { variants: { '': { model: t(n) } } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
}
json('models/block/luz_fiesta_apagada.json', { parent: 'minecraft:block/cube_all', textures: { all: t('luz_fiesta_apagada') } });
json('blockstates/luz_fiesta_parpadeo.json', { variants: { 'lit=true': { model: t('luz_fiesta') }, 'lit=false': { model: t('luz_fiesta_apagada') } } });
json('items/luz_fiesta_parpadeo.json', { model: { type: 'minecraft:model', model: t('luz_fiesta') } });

/* ============================================================ fichas */

// la moneda dorada (con su sonrisa grabada) y la tira de tickets rojos
{
  const m = new Lienzo(16).pintar((x, y) => {
    const d = Math.hypot(x - 7.5, y - 7.5);
    if (d > 7) return null;
    let c = d > 5.8 ? [184, 134, 28] : (x + y < 12 ? [255, 236, 140] : [242, 197, 58]);
    if (y === 9 && x >= 5 && x <= 10 && d < 5) c = [184, 134, 28];
    if (y === 10 && (x === 5 || x === 10)) c = [184, 134, 28];
    if (y === 6 && (x === 5 || x === 10)) c = [184, 134, 28];
    return c;
  });
  guardar(ITEM, 'moneda_fiesta', m);
  const tk = new Lienzo(16).pintar((x, y) => {
    if (y < 4 || y > 11) return null;
    if ((x === 0 || x === 15) && (y === 7 || y === 8)) return null;
    let c = x % 5 === 4 ? [170, 30, 44] : ROJO;
    if (y === 4 || y === 11) c = [170, 30, 44];
    if (y >= 6 && y <= 9 && x % 5 >= 1 && x % 5 <= 2) c = CREMA;
    return c;
  });
  guardar(ITEM, 'tickets_fiesta', tk);
  for (const n of ['moneda_fiesta', 'tickets_fiesta']) {
    json(`models/item/${n}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${n}` } });
    json(`items/${n}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/${n}` } });
  }
}

/* ============================================================ nombres */

const NOMBRES = {
  papel_fiesta: ['Papel de fiesta', 'Party wallpaper'],
  papel_estrellas: ['Papel de estrellas', 'Star wallpaper'],
  papel_fiesta_sucio: ['Papel de fiesta despegado', 'Peeling party wallpaper'],
  zocalo_fiesta: ['Zócalo de colores', 'Colourful skirting'],
  suelo_goma: ['Suelo de goma', 'Foam floor'],
  suelo_goma_sucio: ['Suelo de goma sucio', 'Dirty foam floor'],
  moqueta_confeti: ['Moqueta de confeti', 'Confetti carpet'],
  baldosa_cocina: ['Baldosa de cocina', 'Kitchen tile'],
  techo_fiesta: ['Techo de fiesta', 'Party ceiling'],
  luz_fiesta: ['Luz de fiesta', 'Party light'],
  luz_fiesta_parpadeo: ['Luz de fiesta que falla', 'Flickering party light'],
  acolchado_rojo: ['Acolchado rojo', 'Red padding'],
  acolchado_amarillo: ['Acolchado amarillo', 'Yellow padding'],
  acolchado_azul: ['Acolchado azul', 'Blue padding'],
  acolchado_verde: ['Acolchado verde', 'Green padding'],
  globo: ['Globo', 'Balloon'],
  guirnalda: ['Guirnalda', 'Bunting'],
  cartel_fiesta: ['Cartel de fiesta', 'Party poster'],
  dibujo_fiesta: ['Dibujo de niño', 'Child\'s drawing'],
  altavoz_fiesta: ['Altavoz', 'Speaker'],
  mesa_fiesta: ['Mesa con mantel', 'Party table'],
  tarta: ['Tarta', 'Cake'],
  regalo: ['Regalo', 'Present'],
  sillita: ['Sillita', 'Little chair'],
  confeti: ['Confeti', 'Confetti'],
  peluche: ['Peluche', 'Plush toy'],
  olla: ['Olla', 'Pot'],
  bandeja: ['Bandeja', 'Tray'],
  camita: ['Camita', 'Nap bed'],
  suelo_defectuoso: ['Suelo defectuoso', 'Faulty floor'],
  piscina_bolas: ['Piscina de bolas', 'Ball pit'],
  trampilla_fiesta: ['Trampilla', 'Hatch'],
  nota_fiesta: ['Nota', 'Note'],
  baldosa_juego: ['Baldosa de juego', 'Game tile']
};
const OTROS = {
  'item.backrooms_evento.moneda_fiesta': ['Moneda de la fiesta', 'Party coin'],
  'item.backrooms_evento.tickets_fiesta': ['Tickets', 'Tickets'],
  'entity.backrooms_evento.ficha': ['Ficha', 'Token'],
  'entity.backrooms_evento.anfitrion': ['El Anfitrión', 'The Host'],
  'death.attack.backrooms_evento.fiesta': ['%1$s se quedó en la fiesta para siempre', '%1$s stayed at the party forever'],
  'death.attack.backrooms_evento.fiesta.player': ['%2$s abrazó a %1$s y no lo soltó', '%2$s hugged %1$s and never let go']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  for (const [id, v] of Object.entries(OTROS)) lang[id] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: la fiesta');
