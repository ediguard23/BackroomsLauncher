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
const caras = (tex) => {
  const f = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) f[d] = { texture: tex };
  return f;
};
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

// tartas (fresa, chocolate, vainilla y una a medio comer): plato, dos pisos de bizcocho con capas de nata,
// glaseado que chorrea por los lados, fresas en el piso de abajo y velas de rayas con su llama (que alumbra).
// Van siempre encima de una mesa (su tablero llega a 13/16): el modelo empieza 3 mas abajo para apoyarse
const SABORES = [
  { glaseado: [250, 160, 196], bizcocho: [250, 226, 190] },
  { glaseado: [92, 52, 32], bizcocho: [136, 82, 50] },
  { glaseado: [252, 246, 228], bizcocho: [244, 212, 150] },
  { glaseado: [250, 160, 196], bizcocho: [250, 226, 190] }
];
guardar(TEX, 'plato_tarta', new Lienzo(16).pintar((x, y) => (x < 1 || y < 1 || x > 14 || y > 14 ? [190, 190, 196] : escala([246, 246, 248], 0.96 + hash(x, y, 223) * 0.04))));
guardar(TEX, 'vela_fiesta', new Lienzo(16).pintar((x, y) => ((x + y) % 6 < 3 ? [250, 250, 250] : [70, 140, 230])));
guardar(TEX, 'llama_vela', new Lienzo(16).pintar((x, y) => mezcla([255, 250, 200], [255, 140, 30], y / 15)));
guardar(TEX, 'fresa_tarta', new Lienzo(16).pintar((x, y) => {
  if (y < 4) return [70, 160, 70];
  return hash(x, y, 224) > 0.85 ? [250, 230, 120] : escala([214, 30, 46], 0.9 + hash(x, y, 225) * 0.1);
}));
SABORES.forEach((sab, i) => {
  // el lado: glaseado arriba que chorrea en gotas de largo distinto, bizcocho con dos capas de nata
  guardar(TEX, `tarta_lado_${i}`, new Lienzo(16).pintar((x, y) => {
    const gota = 3 + Math.floor(hash(x, 0, 226 + i) * 4) * (x % 3 === 1 ? 1 : 0.4);
    if (y < gota) return escala(sab.glaseado, 0.95 + (y === 0 ? 0.1 : 0) + hash(x, y, 227) * 0.05);
    if (y === 7 || y === 8 || y === 12) return escala(CREMA, 0.95 + hash(x, y, 228) * 0.05);
    return escala(sab.bizcocho, 0.88 + hash(x, y, 229 + i) * 0.14);
  }));
  // la cima: glaseado con virutas de colores y un borde de nata en bolitas
  guardar(TEX, `tarta_cima_${i}`, new Lienzo(16).pintar((x, y) => {
    const borde = x < 2 || y < 2 || x > 13 || y > 13;
    if (borde && (x + y) % 3 !== 0) return escala(CREMA, 0.92 + hash(x, y, 230) * 0.08);
    const h = hash(x, y, 231 + i);
    if (h > 0.9) return COLORES[Math.floor(h * 97) % 4];
    return escala(sab.glaseado, 0.92 + hash(x, y, 232) * 0.08);
  }));
  const T = (n) => ({ texture: n });
  const piso = (from, to) => ({ from, to, faces: { north: T('#l'), south: T('#l'), east: T('#l'), west: T('#l'), up: T('#c'), down: T('#p') } });
  const media = i === 3;
  const els = [
    { from: [1.5, -3, 1.5], to: [14.5, -2.6, 14.5], faces: caras('#p') },
    piso([2.5, -2.6, 2.5], [13.5, 1.4, media ? 8 : 13.5]),
    piso([4.5, 1.4, 4.5], [11.5, 4.4, media ? 8 : 11.5])
  ];
  const fresas = media ? [[3, 3], [11.6, 3]] : [[3, 3], [11.6, 3], [3, 11.6], [11.6, 11.6], [7.3, 2.9], [7.3, 12]];
  for (const [fx, fz] of fresas) els.push({ from: [fx, 1.4, fz], to: [fx + 1.4, 2.6, fz + 1.4], faces: caras('#f') });
  const velas = media ? [[7.6, 5.4]] : [[6, 7.6], [9.2, 7.6], [7.6, 5.4], [7.6, 9.6]];
  for (const [vx, vz] of velas) {
    els.push({ from: [vx, 4.4, vz], to: [vx + 0.8, 7.4, vz + 0.8], faces: caras('#v') });
    els.push({ from: [vx + 0.15, 7.4, vz + 0.15], to: [vx + 0.65, 8.6, vz + 0.65], shade: false, light_emission: 15, faces: caras('#ll') });
  }
  json(`models/block/tarta_${i}.json`, mod({ l: t(`tarta_lado_${i}`), c: t(`tarta_cima_${i}`), p: t('plato_tarta'), f: t('fresa_tarta'),
    v: t('vela_fiesta'), ll: t('llama_vela'), particle: t(`tarta_cima_${i}`) }, els));
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

// lo de miedo del salon final: peluches destrozados, sangre en el suelo y en las paredes
const SANGRE = [112, 10, 14];
const SANGRE_SECA = [70, 10, 12];
/** Una mancha de sangre: unos cuantos discos y gotas alrededor (null fuera). */
const salpicadura = (x, y, semilla, cx, cy, r) => {
  const d = Math.hypot(x - cx, y - cy);
  const borde = r * (0.75 + fbm(Math.atan2(y - cy, x - cx) * 3, semilla, semilla, 8) * 0.5);
  if (d < borde) return escala(d < borde * 0.55 ? SANGRE_SECA : SANGRE, 0.85 + hash(x, y, semilla) * 0.2);
  if (d < r * 2.2 && hash(x, y, semilla + 1) > 0.9) return escala(SANGRE, 0.9);
  return null;
};
guardar(TEX, 'relleno', new Lienzo(16).pintar((x, y) => escala([236, 232, 222], 0.84 + fbm(x, y, 301, 8) * 0.2)));
[[150, 100, 60], ROSA, [150, 100, 60], AZUL].forEach((col, i) => {
  guardar(TEX, `peluche_roto_${i}`, new Lienzo(16).pintar((x, y) => {
    let c = escala(col, 0.86 + hash(x, y, 311 + i) * 0.14);
    // la costura rota y la sangre por encima
    if (Math.abs(x - 8) < 1 && y > 5) c = [236, 226, 210];
    const m = salpicadura(x, y, 320 + i, 6 + i * 2, 9, 3.2);
    if (m) c = m;
    if (y === 4 && (x === 5 || x === 10)) c = i === 2 && x === 10 ? [236, 226, 210] : [20, 18, 18];
    return c;
  }));
}
);
const pel = (i) => ({ p: t(`peluche_roto_${i}`), r: t('relleno'), particle: t(`peluche_roto_${i}`) });
// 0: sin cabeza, con el relleno saliendo del cuello
json('models/block/peluche_roto_0.json', mod(pel(0), [
  caja([5, 0, 6], [11, 6, 10], '#p'), caja([6, 6, 7], [10, 7.5, 9], '#r'), caja([6.5, 7.5, 7.5], [8.5, 8.5, 8.5], '#r'),
  caja([3.5, 2, 7], [5, 5.5, 9], '#p'), caja([11, 2, 7], [12.5, 5.5, 9], '#p')
]));
// 1: tumbado boca arriba, ensangrentado
json('models/block/peluche_roto_1.json', mod(pel(1), [
  caja([3, 0, 6], [9, 3.5, 10], '#p'), caja([9, 0, 5.5], [13.5, 4, 10.5], '#p'),
  caja([12, 3.5, 5.5], [13.5, 5, 7], '#p'), caja([12, 3.5, 9], [13.5, 5, 10.5], '#p'),
  caja([4, 0, 3.5], [7, 2, 5.5], '#p'), caja([4, 0, 10.5], [7, 2, 12.5], '#p')
]));
// 2: solo la cabeza, con el relleno por donde la arrancaron
json('models/block/peluche_roto_2.json', mod(pel(2), [
  caja([5, 0, 6], [10, 4.5, 10.5], '#p'), caja([5, 4.5, 7.5], [6.5, 6, 9], '#p'), caja([8.5, 4.5, 7.5], [10, 6, 9], '#p'),
  caja([6, 0, 10.5], [9, 2, 12], '#r'), caja([6.5, 0, 12], [8, 1, 13], '#r')
]));
// 3: sin un brazo (tirado al lado) y el relleno asomando del hombro
json('models/block/peluche_roto_3.json', mod(pel(3), [
  caja([5, 0, 6], [11, 6, 10], '#p'), caja([5.5, 6, 6.5], [10.5, 10.5, 10], '#p'),
  caja([5, 10, 7.5], [6.5, 11.5, 9], '#p'), caja([9.5, 10, 7.5], [11, 11.5, 9], '#p'),
  caja([3.5, 2, 7], [5, 5.5, 9], '#p'), caja([11, 4, 7], [12, 5.5, 9], '#r'),
  caja([11, 0, 11], [14.5, 1.5, 12.5], '#p'), caja([10.5, 0, 11.2], [11, 1.2, 12.3], '#r')
]));
estados('peluche_roto', 4, (i) => `peluche_roto_${i}`);
// sangre en el suelo: un charco, salpicaduras, un rastro arrastrado y gotas
for (let v = 0; v < 4; v++) {
  guardar(TEX, `sangre_fiesta_${v}`, new Lienzo(16).pintar((x, y) => {
    if (v === 0) return salpicadura(x, y, 331, 8, 8, 5);
    if (v === 1) return salpicadura(x, y, 332, 5, 6, 2.6) || salpicadura(x, y, 333, 11, 11, 2.2);
    if (v === 2) return Math.abs(y - 8 - Math.sin(x * 0.5) * 1.2) < 2.2 - x * 0.08 && hash(x, y, 334) > 0.12 ? escala(SANGRE, 0.75 + hash(x, y, 335) * 0.3) : null;
    return hash(x, y, 336) > 0.93 ? escala(SANGRE, 0.9) : salpicadura(x, y, 337, 9, 7, 1.4);
  }));
  json(`models/block/sangre_fiesta_${v}.json`, mod({ c: t(`sangre_fiesta_${v}`), particle: t(`sangre_fiesta_${v}`) }, [{
    from: [0, 0.1, 0], to: [16, 0.1, 16], faces: { up: { texture: '#c', uv: [0, 0, 16, 16] } }
  }]));
}
estados('sangre_fiesta', 4, (i) => `sangre_fiesta_${i}`);
// sangre en la pared: salpicadura, chorretones, una mano y dos manos que resbalan
for (let v = 0; v < 4; v++) {
  guardar(TEX, `sangre_pared_${v}`, new Lienzo(16).pintar((x, y) => {
    const chorro = (x0) => Math.abs(x - x0) < 0.8 && y > 6 && y < 6 + 6 * hash(x0, 0, 341 + v) + 3;
    if (v === 0) return salpicadura(x, y, 342, 8, 6, 4) || (chorro(6) || chorro(10) ? escala(SANGRE, 0.85) : null);
    if (v === 1) return (chorro(3) || chorro(7) || chorro(12) || y < 4 && hash(x, y, 343) > 0.3) ? escala(SANGRE, 0.8 + hash(x, y, 344) * 0.25) : null;
    // una mano: la palma y cinco dedos
    const mano = (cx, cy) => {
      const palma = Math.hypot((x - cx) / 2.2, (y - cy) / 2.6) < 1;
      let dedo = false;
      [[-2, -4], [-0.8, -4.8], [0.6, -4.8], [1.9, -4], [3, -1]].forEach(([dx, dy]) => { dedo = dedo || Math.hypot(x - cx - dx, (y - cy - dy) / 1.6) < 0.7; });
      return palma || dedo;
    };
    if (v === 2) return mano(8, 9) ? escala(SANGRE, 0.8 + hash(x, y, 345) * 0.25) : null;
    return (mano(5, 6) || mano(11, 10) || (Math.abs(x - 5) < 1.5 && y > 8 && y < 14)) ? escala(SANGRE, 0.75 + hash(x, y, 346) * 0.3) : null;
  }));
  json(`models/block/sangre_pared_${v}.json`, enPared(`sangre_pared_${v}`));
}
estados('sangre_pared', 4, (i) => `sangre_pared_${i}`);

// el ascensor de la salida (salon final): cabina de acero cepillado, suelo de chapa estriada, plafon,
// marco y viga de las puertas, las hojas correderas (las mueve la cinematica, SalidaFiestaCliente),
// la botonera de dentro, el boton de llamada de fuera, el indicador de piso (7 segmentos) y el pasamanos
const cepillado = (x, y, base, semilla) => escala(base, 0.84 + fbm(x * 0.12, y * 2.4, semilla, 16) * 0.18 + hash(x, y, semilla + 1) * 0.04);
guardar(TEX, 'ascensor_fiesta_pared', new Lienzo(16).pintar((x, y) => {
  let c = cepillado(x, y, [172, 176, 180], 701);
  if (x === 0) c = [112, 116, 120];
  if (x === 15) c = escala(c, 1.12);
  if (y === 15) c = escala(c, 0.86);
  return c;
}));
guardar(TEX, 'ascensor_fiesta_suelo', new Lienzo(16).pintar((x, y) => {
  let c = escala([62, 64, 66], 0.9 + hash(x, y, 711) * 0.1);
  // la chapa estriada: rayitas en diagonal alternadas
  const bx = Math.floor(x / 4);
  const by = Math.floor(y / 4);
  const u = x % 4;
  const v = y % 4;
  const raya = (bx + by) % 2 === 0 ? u === v || u === v + 1 : u === 3 - v || u === 4 - v;
  if (raya && u > 0 && v > 0) c = escala([96, 98, 100], 0.95 + hash(x, y, 712) * 0.1);
  return c;
}));
guardar(TEX, 'ascensor_fiesta_techo', new Lienzo(16).pintar((x, y) => {
  if (x < 2 || y < 2 || x > 13 || y > 13) return cepillado(x, y, [150, 154, 158], 721);
  return (x === 7 || y === 7) ? [214, 216, 210] : escala([244, 244, 236], 0.97 + hash(x, y, 722) * 0.03);
}));
guardar(TEX, 'ascensor_fiesta_marco', new Lienzo(16).pintar((x, y) => {
  let c = cepillado(x, y, [186, 190, 194], 731);
  if (x === 0 || y === 0) c = escala(c, 1.12);
  if (x === 15 || y === 15) c = escala(c, 0.78);
  return c;
}));
guardar(TEX, 'ascensor_fiesta_viga', new Lienzo(16).pintar((x, y) => {
  let c = cepillado(x, y, [104, 108, 114], 741);
  if (y >= 12 && y <= 13) c = [38, 40, 44];                 // el carril de las hojas
  if (y === 11 || y === 14) c = escala(c, 1.2);
  if ((x === 3 || x === 12) && (y === 5 || y === 6)) c = [196, 200, 204];  // tornillos
  return c;
}));
// la hoja: 16 x 32 (un bloque de ancho, dos de alto; la cinematica la escala a 1,5 x 3)
guardar(TEX, 'ascensor_fiesta_hoja', new Lienzo(16, 32).pintar((x, y) => {
  let c = cepillado(x, y, [176, 182, 188], 751);
  c = escala(c, 1.0 + Math.max(0, 0.08 - Math.abs(x - 5) * 0.02));  // un reflejo vertical
  if (x === 15) c = [64, 66, 70];                           // el canto donde se juntan
  if (y >= 28) c = escala(cepillado(x, y, [120, 124, 128], 752), 0.95);  // el zocalo
  if (y === 27) c = [90, 94, 98];
  return c;
}));
guardar(TEX, 'ascensor_fiesta_hoja_canto', new Lienzo(16).pintar((x, y) => escala([80, 84, 88], 0.9 + hash(x, y, 753) * 0.1)));
json('models/block/ascensor_fiesta_hoja.json', mod({ h: t('ascensor_fiesta_hoja'), c: t('ascensor_fiesta_hoja_canto'), particle: t('ascensor_fiesta_hoja') }, [{
  from: [0, 0, 0], to: [16, 32, 2], faces: {
    north: { texture: '#h', uv: [0, 0, 16, 16] }, south: { texture: '#h', uv: [16, 0, 0, 16] },
    east: { texture: '#c' }, west: { texture: '#c' }, up: { texture: '#c' }, down: { texture: '#c' } } }]));
json('blockstates/ascensor_fiesta_hoja.json', { variants: { '': { model: t('ascensor_fiesta_hoja') } } });
for (const n of ['ascensor_fiesta_pared', 'ascensor_fiesta_suelo', 'ascensor_fiesta_marco', 'ascensor_fiesta_viga']) {
  json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(n) } });
  json(`blockstates/${n}.json`, { variants: { '': { model: t(n) } } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
}
json('models/block/ascensor_fiesta_techo.json', mod({ a: t('ascensor_fiesta_techo'), particle: t('ascensor_fiesta_techo') }, [{
  from: [0, 0, 0], to: [16, 16, 16], shade: false, light_emission: 13, faces: caras('#a') }]));
json('blockstates/ascensor_fiesta_techo.json', { variants: { '': { model: t('ascensor_fiesta_techo') } } });
json('items/ascensor_fiesta_techo.json', { model: { type: 'minecraft:model', model: t('ascensor_fiesta_techo') } });
// la botonera de dentro (0) y el boton de llamada de fuera (1)
{
  const l = new Lienzo(16).pintar((x, y) => cepillado(x, y, [150, 154, 160], 761));
  // dentro: 2 columnas x 4 filas de botones redondos (pisos 5..1 y 0), el de abajo amarillo (alarma)
  for (let f = 0; f < 4; f++) {
    for (let k = 0; k < 2; k++) {
      const cx = 6 + k * 4;
      const cy = 3 + f * 3;
      l.disco(cx, cy, 1.2, [60, 62, 66]);
      l.punto(cx, cy, f === 0 && k === 0 ? [255, 168, 60] : [200, 204, 208]);
    }
  }
  l.disco(8, 14, 1.2, [220, 180, 40]);
  guardar(TEX, 'ascensor_fiesta_panel_0', l);
  const ll = new Lienzo(16).pintar((x, y) => cepillado(x, y, [150, 154, 160], 762));
  ll.disco(8, 5.5, 1.4, [60, 62, 66]);
  ll.disco(8, 10.5, 1.4, [60, 62, 66]);
  ll.punto(8, 5, [220, 224, 228]); ll.punto(7, 6, [220, 224, 228]); ll.punto(9, 6, [220, 224, 228]);   // subir
  ll.punto(8, 11, [255, 168, 60]); ll.punto(7, 10, [255, 168, 60]); ll.punto(9, 10, [255, 168, 60]); // bajar (encendido)
  guardar(TEX, 'ascensor_fiesta_panel_1', ll);
  json('models/block/ascensor_fiesta_panel_0.json', mod({ f: t('ascensor_fiesta_panel_0'), m: t('ascensor_fiesta_marco'), particle: t('ascensor_fiesta_marco') }, [{
    from: [4, 1, 15], to: [12, 15, 16], faces: { north: { texture: '#f', uv: [4, 1, 12, 15] }, east: { texture: '#m' }, west: { texture: '#m' },
      up: { texture: '#m' }, down: { texture: '#m' } } }]));
  json('models/block/ascensor_fiesta_panel_1.json', mod({ f: t('ascensor_fiesta_panel_1'), m: t('ascensor_fiesta_marco'), particle: t('ascensor_fiesta_marco') }, [{
    from: [6, 3, 15], to: [10, 13, 16], faces: { north: { texture: '#f', uv: [6, 3, 10, 13] }, east: { texture: '#m' }, west: { texture: '#m' },
      up: { texture: '#m' }, down: { texture: '#m' } } }]));
  estados('ascensor_fiesta_panel', 2, (i) => `ascensor_fiesta_panel_${i}`);
}
// el indicador de piso: 0..5, "-" (6) y apagado (7), en rojo de 7 segmentos con la flecha de bajada
{
  const SEG = { a: [[1, 0], [2, 0]], b: [[3, 1], [3, 2]], c: [[3, 4], [3, 5]], d: [[1, 6], [2, 6]], e: [[0, 4], [0, 5]], f: [[0, 1], [0, 2]], g: [[1, 3], [2, 3]] };
  const DIG = ['abcdef', 'bc', 'abdeg', 'abcdg', 'bcfg', 'acdfg', 'g', ''];
  const ROJO_LED = [255, 52, 36];
  for (let v = 0; v < 8; v++) {
    const l = new Lienzo(16).pintar((x, y) => (y >= 4 && y <= 12 && x >= 2 && x <= 13 ? [14, 10, 10] : cepillado(x, y, [150, 154, 160], 771)));
    for (const s of DIG[v]) for (const [px, py] of SEG[s]) l.punto(8 + px, 5 + py, ROJO_LED);
    if (v < 7) { l.punto(5, 7, ROJO_LED); l.punto(4, 6, ROJO_LED); l.punto(5, 6, ROJO_LED); l.punto(6, 6, ROJO_LED); }   // la flecha
    guardar(TEX, `ascensor_fiesta_indicador_${v}`, l);
    json(`models/block/ascensor_fiesta_indicador_${v}.json`, mod({ f: t(`ascensor_fiesta_indicador_${v}`), m: t('ascensor_fiesta_marco'), particle: t('ascensor_fiesta_marco') }, [{
      from: [2, 3, 15], to: [14, 13, 16], shade: false, light_emission: 8, faces: { north: { texture: '#f', uv: [2, 3, 14, 13] },
        east: { texture: '#m' }, west: { texture: '#m' }, up: { texture: '#m' }, down: { texture: '#m' } } }]));
  }
  estados('ascensor_fiesta_indicador', 8, (i) => `ascensor_fiesta_indicador_${i}`);
}
// el pasamanos (en la pared, a la altura de la cintura si el bloque esta a ras del suelo)
json('models/block/ascensor_fiesta_barandilla.json', mod({ m: t('ascensor_fiesta_marco'), particle: t('ascensor_fiesta_marco') }, [
  caja([0, 13.5, 13], [16, 15, 14.5], '#m'), caja([2, 13.7, 14.5], [3, 14.7, 16], '#m'), caja([13, 13.7, 14.5], [14, 14.7, 16], '#m')
]));
estados('ascensor_fiesta_barandilla', 1, () => 'ascensor_fiesta_barandilla');
console.log('  ascensor de la salida: cabina, puertas, botonera, indicador y pasamanos');
console.log('  decoracion: globos, guirnaldas, carteles, dibujos, mesas, tartas, regalos, sillitas, confeti, peluches, cocina, camitas, peluches rotos y sangre');

/* ============================================================ para jugar */


/* ============================================================ suelos defectuosos y juegos */

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
    if (d > 7.2) return null;
    if (d > 6.2) return [168, 118, 24];
    if (d > 5.3) return x + y < 14 ? [255, 228, 120] : [214, 160, 40];
    let c = escala([242, 197, 58], 1.0 + (7.5 - (x + y) / 2) * 0.025);
    // la estrella de cinco puntas, en relieve
    const a = Math.atan2(y - 7.5, x - 7.5) + Math.PI / 2;
    const r = 2.0 + 1.9 * Math.pow(Math.abs(Math.cos(a * 2.5)), 3);
    if (d < r) c = d > r - 0.9 ? [190, 138, 30] : [255, 236, 150];
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
  peluche_roto: ['Peluche destrozado', 'Torn plush toy'],
  sangre_fiesta: ['Sangre', 'Blood'],
  sangre_pared: ['Sangre en la pared', 'Blood on the wall'],
  ascensor_fiesta_pared: ['Pared de ascensor', 'Elevator wall'],
  ascensor_fiesta_suelo: ['Suelo de ascensor', 'Elevator floor'],
  ascensor_fiesta_techo: ['Plafón de ascensor', 'Elevator ceiling light'],
  ascensor_fiesta_marco: ['Marco de ascensor', 'Elevator frame'],
  ascensor_fiesta_viga: ['Viga de las puertas', 'Elevator door header'],
  ascensor_fiesta_panel: ['Botonera del ascensor', 'Elevator buttons'],
  ascensor_fiesta_indicador: ['Indicador de piso', 'Floor indicator'],
  ascensor_fiesta_barandilla: ['Pasamanos', 'Handrail'],
  olla: ['Olla', 'Pot'],
  bandeja: ['Bandeja', 'Tray'],
  camita: ['Camita', 'Nap bed'],
  piscina_bolas: ['Piscina de bolas', 'Ball pit'],
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
