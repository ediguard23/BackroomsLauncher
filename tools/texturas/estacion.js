'use strict';
/**
 * La Estacion Electrica (Nivel 3): texturas, modelos, estados, objetos y nombres de sus bloques
 * (evento/.../estacion/BloquesEstacion.java). Todo dibujado por codigo.
 *
 *   node tools/texturas/estacion.js   (va en npm run texturas)
 *
 * Una central vieja que nadie mantiene: hormigon con humedad, paredes pintadas de verde palido y
 * desconchadas, franjas con el color de cada sector, chapa estriada, generador naranja, bidones
 * amarillos, cuadros electricos con relojes y carteles de mantenimiento con numeros en vez de
 * nombres. Las instrucciones del acceso estan rotas: se leen a medias.
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

console.log('Estacion');

const VERDE_PARED = [150, 172, 152];
const HORMIGON = [126, 126, 120];
const NARANJA = [214, 108, 34];
const AMARILLO = [232, 186, 40];
const NEGRO = [26, 26, 26];
const SECTOR = [[196, 52, 44], [226, 182, 40], [52, 102, 196], [58, 150, 78]];
const SECTOR_NOMBRE = ['rojo', 'amarillo', 'azul', 'verde'];
const LETRA = ['A', 'B', 'C', 'D'];

/* ============================================================ pintura */

/** Hormigon: grano, juntas del encofrado cada 16 y regueros de humedad hacia abajo. */
function hormigon (x, y, canal, lado = 32) {
  let c = escala(HORMIGON, 0.86 + fbm(x, y, canal, lado) * 0.2 + hash(x, y, canal + 1) * 0.05);
  if (y % 16 === 15) c = escala(c, 0.82);
  if (hash(x, y, canal + 2) > 0.985) c = escala(c, 0.7); // poros
  const reguero = ruido(x * 3, y * 0.15, 4, canal + 3, lado);
  if (reguero > 0.7) c = mezcla(c, [74, 78, 70], Math.min(0.5, (reguero - 0.7) * 2.2));
  return c;
}

/** La pintura verde palido de las paredes, desconchada (se ve el hormigon) y sucia abajo. */
function pintada (x, y, canal, lado = 32) {
  let c = escala(VERDE_PARED, 0.93 + fbm(x, y, canal, lado) * 0.08);
  const chip = fbm(x * 1.5, y * 1.5, canal + 5, lado);
  if (chip > 0.68) c = hormigon(x, y, canal + 9, lado);
  else if (chip > 0.64) c = escala(c, 0.8);
  const sucio = y / lado;
  c = mezcla(c, [88, 92, 80], Math.max(0, sucio - 0.6) * 0.8);
  return c;
}

/** Chapa estriada: rombos en relieve en diagonal y algo de oxido. */
function chapa (x, y, canal, base = [124, 128, 130]) {
  let c = escala(base, 0.88 + fbm(x, y, canal, 32) * 0.12);
  const px = (x + (Math.floor(y / 4) % 2) * 4) % 8;
  const py = y % 4;
  if (py === 1 && (px === 2 || px === 3)) c = escala(c, 1.25);
  if (py === 2 && (px === 3 || px === 4)) c = escala(c, 0.72);
  const ox = fbm(x, y, canal + 7, 32);
  if (ox > 0.66) c = mezcla(c, [132, 76, 40], Math.min(0.7, (ox - 0.66) * 3));
  return c;
}

/** Metal pintado (generador, armarios): color, desgaste en los bordes, manchas de aceite. */
function metal (x, y, canal, base, lado = 16) {
  let c = escala(base, 0.9 + fbm(x, y, canal, lado) * 0.1 + hash(x, y, canal + 1) * 0.04);
  const borde = Math.min(x, y, lado - 1 - x, lado - 1 - y);
  if (borde === 0) c = escala(c, 0.7);
  if (borde === 1 && hash(x, y, canal + 2) > 0.6) c = mezcla(c, [150, 150, 146], 0.6);
  const aceite = fbm(x, y * 0.6, canal + 3, lado);
  if (aceite > 0.72) c = mezcla(c, [30, 26, 22], Math.min(0.6, (aceite - 0.72) * 3));
  return c;
}

/** Franjas de aviso negras y amarillas en diagonal. */
const aviso = (x, y, ancho = 4) => (Math.floor((x + y) / ancho) % 2 === 0 ? escala(AMARILLO, 0.95 + hash(x, y, 5) * 0.08) : NEGRO);

/* ============================================================ estructura */

guardar(TEX, 'hormigon_estacion', new Lienzo(32).pintar((x, y) => hormigon(x, y, 1000)));
guardar(TEX, 'pared_pintada', new Lienzo(32).pintar((x, y) => pintada(x, y, 1010)));
SECTOR.forEach((col, i) => {
  guardar(TEX, `franja_sector_${SECTOR_NOMBRE[i]}`, new Lienzo(32).pintar((x, y) => {
    if (y < 6 || y > 25) return pintada(x, y, 1020 + i);
    if (y === 6 || y === 25) return [40, 40, 38];
    let c = escala(col, 0.88 + fbm(x, y, 1030 + i, 32) * 0.12);
    if (fbm(x * 1.6, y * 1.6, 1040 + i, 32) > 0.7) c = escala(VERDE_PARED, 0.85); // desconchado
    return c;
  }));
});
guardar(TEX, 'suelo_estacion', new Lienzo(32).pintar((x, y) => {
  let c = escala([102, 104, 98], 0.86 + fbm(x, y, 1050, 32) * 0.18 + hash(x, y, 1051) * 0.04);
  if (x % 16 === 0 || y % 16 === 0) c = escala(c, 0.78);
  if (hash(x, y, 1052) > 0.99) c = [70, 70, 66];
  return c;
}));
guardar(TEX, 'suelo_estacion_manchado', new Lienzo(32).pintar((x, y) => {
  let c = escala([102, 104, 98], 0.86 + fbm(x, y, 1050, 32) * 0.18 + hash(x, y, 1051) * 0.04);
  if (x % 16 === 0 || y % 16 === 0) c = escala(c, 0.78);
  const m = fbm(x, y, 1053, 32);
  if (m > 0.56) c = mezcla(c, [28, 26, 24], Math.min(0.85, (m - 0.56) * 3.5)); // aceite
  if (m > 0.6 && hash(x, y, 1054) > 0.94) c = [70, 64, 90];                     // el brillo del aceite
  return c;
}));
guardar(TEX, 'chapa_estacion', new Lienzo(32).pintar((x, y) => chapa(x, y, 1060)));
guardar(TEX, 'rejilla_estacion', new Lienzo(16).pintar((x, y) => {
  // rejilla de pletinas: huecos que dejan ver el pozo (recortado)
  const barra = x % 4 === 0 || y % 8 === 0;
  if (!barra) return null;
  let c = escala([112, 116, 116], 0.9 + hash(x, y, 1070) * 0.15);
  if (hash(x, y, 1071) > 0.86) c = mezcla(c, [130, 80, 44], 0.6);
  return c;
}));
guardar(TEX, 'techo_estacion', new Lienzo(32).pintar((x, y) => {
  let c = escala([84, 86, 82], 0.86 + fbm(x, y, 1080, 32) * 0.2);
  if (x % 16 === 0) c = escala(c, 0.75);
  const hollin = fbm(x * 0.7, y * 0.7, 1081, 32);
  if (hollin > 0.6) c = mezcla(c, [30, 30, 28], (hollin - 0.6) * 1.6);
  return c;
}));
guardar(TEX, 'techo_tubos', new Lienzo(32).pintar((x, y) => {
  let c = escala([74, 76, 72], 0.86 + fbm(x, y, 1090, 32) * 0.18);
  // la bandeja de cables (de lado a lado) y sus cables de colores
  if (y >= 9 && y <= 20) {
    if (y === 9 || y === 20) return escala([150, 152, 150], 0.9 + hash(x, y, 1091) * 0.1);
    const cable = Math.floor((y - 10) / 2.5);
    const cols = [[30, 30, 30], [170, 60, 30], [40, 40, 40], [60, 90, 160], [200, 170, 40]];
    c = escala(cols[cable % cols.length], 0.8 + ((y - 10) % 2.5 < 1.2 ? 0.25 : 0) + hash(x, y, 1092) * 0.06);
  }
  // una tuberia
  if (y >= 25 && y <= 29) c = escala([120, 132, 120], 1.1 - Math.abs(y - 27) * 0.12 + hash(x, y, 1093) * 0.04);
  if (y >= 25 && y <= 29 && x % 16 === 4) c = [80, 80, 80];
  return c;
}));
const lampara = (encendida) => new Lienzo(32).pintar((x, y) => {
  if (x < 2 || y < 2 || x > 29 || y > 29) return escala([150, 150, 146], 0.85 + hash(x, y, 1100) * 0.1);
  const reja = (x - 2) % 6 === 0;
  if (reja) return [60, 60, 58];
  if (y >= 11 && y <= 20) {
    const d = Math.abs(y - 15.5) / 5;
    if (!encendida) return mezcla([120, 118, 110], [60, 58, 54], d + (hash(x, y, 1101) > 0.8 ? 0.3 : 0));
    return mezcla([255, 252, 236], [226, 230, 210], d);
  }
  return encendida ? [196, 198, 190] : [90, 90, 86];
});
guardar(TEX, 'lampara_estacion', lampara(true));
guardar(TEX, 'lampara_fundida', lampara(false));

/* ============================================================ generador */

guardar(TEX, 'generador_carcasa', new Lienzo(32).pintar((x, y) => {
  let c = metal(x, y, 1110, NARANJA, 32);
  // rejillas de ventilacion
  if (y >= 6 && y <= 17 && x >= 5 && x <= 26 && (y - 6) % 3 === 0) c = [36, 30, 26];
  if (y >= 6 && y <= 17 && x >= 5 && x <= 26 && (y - 6) % 3 === 1) c = escala(NARANJA, 1.12);
  // franja de aviso abajo
  if (y >= 26) c = aviso(x, y);
  // tornillos
  if ((x === 2 || x === 29) && (y === 2 || y === 23)) c = [190, 190, 186];
  return c;
}));
guardar(TEX, 'generador_radiador', new Lienzo(32).pintar((x, y) => {
  if (x < 3 || y < 3 || x > 28 || y > 28) return metal(x, y, 1120, NARANJA, 32);
  // aletas del radiador
  const aleta = (x - 3) % 2 === 0;
  return escala(aleta ? [70, 72, 72] : [28, 30, 30], 0.9 + hash(x, y, 1121) * 0.15);
}));
guardar(TEX, 'generador_tapa', new Lienzo(32).pintar((x, y) => {
  let c = metal(x, y, 1130, escala(NARANJA, 0.92), 32);
  if ((x === 3 || x === 28) && y >= 3 && y <= 28) c = [120, 64, 20];
  if ((y === 3 || y === 28) && x >= 3 && x <= 28) c = [120, 64, 20];
  if (x % 8 === 4 && y % 8 === 4) c = [190, 190, 186];
  return c;
}));
guardar(TEX, 'boca_combustible', (() => {
  const l = new Lienzo(64).pintar((x, y) => metal(x, y, 1140, NARANJA, 64));
  l.disco(31.5, 26, 13, [60, 60, 58]);
  l.disco(31.5, 26, 10.5, [168, 168, 160]);
  l.disco(31.5, 26, 6, [120, 120, 114]);
  l.rect(21, 25, 42, 27, [90, 90, 86]);
  // la etiqueta
  l.rect(10, 44, 53, 56, NEGRO);
  l.rect(11, 45, 52, 55, [236, 200, 50]);
  l.texto('GASOIL', 'centro', 47, [20, 20, 20]);
  return l;
})());
guardar(TEX, 'tubo_escape', new Lienzo(16).pintar((x, y) => {
  let c = escala([88, 84, 80], 0.85 + fbm(x, y, 1150, 16) * 0.25);
  const calor = fbm(x * 0.5, y * 2, 1151, 16);
  if (calor > 0.6) c = mezcla(c, [110, 70, 50], (calor - 0.6) * 1.5);
  if (y % 8 === 0) c = [56, 54, 52];
  return c;
}));

/* ============================================================ modelos: ayudas */

const mod = (textures, elements, extra = {}) => ({ parent: 'minecraft:block/block', textures, elements, ...extra });
/** Un cubo con la misma textura en sus caras (UV sacadas de su posicion, como hace Minecraft). */
const caja = (from, to, tex, extra = {}) => {
  const faces = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) faces[d] = { texture: tex };
  return { from, to, faces, ...extra };
};
/** Igual, con la cara de delante (north) de otra textura y la UV entera. */
const cajaFrente = (from, to, frente, resto, extra = {}) => {
  const e = caja(from, to, resto, extra);
  e.faces.north = { texture: frente, uv: [0, 0, 16, 16] };
  return e;
};
const cubo = (n) => json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(n) } });
const simple = (id, modelo = id) => {
  json(`blockstates/${id}.json`, { variants: { '': { model: t(modelo) } } });
  json(`items/${id}.json`, { model: { type: 'minecraft:model', model: t(modelo) } });
};
/**
 * El blockstate de todas las combinaciones de `props` ({nombre: [valores]}). `fn(estado)` da el
 * modelo; si el estado tiene facing, se gira solo. `item`: el modelo del objeto.
 */
function estados (id, props, fn, item) {
  const v = {};
  const nombres = Object.keys(props);
  const rec = (i, est) => {
    if (i === nombres.length) {
      const clave = nombres.map((n) => `${n}=${est[n]}`).join(',');
      const giro = est.facing ? FACINGS[est.facing] : 0;
      v[clave] = { model: t(fn(est)), ...(giro ? { y: giro } : {}) };
      return;
    }
    for (const val of props[nombres[i]]) rec(i + 1, { ...est, [nombres[i]]: val });
  };
  rec(0, {});
  json(`blockstates/${id}.json`, { variants: v });
  json(`items/${id}.json`, { model: { type: 'minecraft:model', model: t(item) } });
}
const rango = (n) => [...Array(n).keys()];
const FAC = Object.keys(FACINGS);

/* ============================================================ bloques simples */

for (const n of ['hormigon_estacion', 'pared_pintada', 'suelo_estacion', 'suelo_estacion_manchado', 'chapa_estacion', 'rejilla_estacion',
  'techo_estacion', 'techo_tubos', 'lampara_estacion', 'lampara_fundida', 'generador_carcasa', 'generador_tapa']) {
  cubo(n);
  simple(n);
}
json('models/block/macizo_estacion.json', { parent: 'minecraft:block/block', textures: { particle: t('generador_carcasa') }, elements: [] });
simple('macizo_estacion');
SECTOR_NOMBRE.forEach((n) => cubo(`franja_sector_${n}`));
estados('franja_sector', { color: rango(4) }, (e) => `franja_sector_${SECTOR_NOMBRE[e.color]}`, 'franja_sector_rojo');
for (const n of ['generador_radiador', 'boca_combustible']) {
  json(`models/block/${n}.json`, { parent: 'minecraft:block/orientable', textures: { front: t(n), side: t('generador_carcasa'), top: t('generador_tapa') } });
  estados(n, { facing: FAC }, () => n, n);
}
json('models/block/tubo_escape.json', mod({ t: t('tubo_escape'), particle: t('tubo_escape') }, [
  caja([4, 0, 4], [12, 16, 12], '#t'),
  caja([3, 14, 3], [13, 16, 13], '#t')
]));
simple('tubo_escape');
console.log('  estructura y generador');

/* ============================================================ en la pared */

// tuberias: 0 una gruesa, 1 dos finas, 2 una con su valvula roja, 3 un codo que baja
guardar(TEX, 'tuberia_estacion', new Lienzo(16).pintar((x, y) => {
  const k = 1.15 - Math.abs(y - 7.5) / 7.5 * 0.4;
  return escala([112, 136, 118], k * (0.9 + hash(x, y, 1200) * 0.1));
}));
guardar(TEX, 'tuberia_soporte', new Lienzo(16).pintar((x, y) => escala([70, 70, 70], 0.85 + hash(x, y, 1201) * 0.2)));
guardar(TEX, 'valvula_roja', new Lienzo(16).pintar((x, y) => escala([186, 40, 34], 0.85 + hash(x, y, 1202) * 0.2)));
const TUB = { p: t('tuberia_estacion'), s: t('tuberia_soporte'), v: t('valvula_roja'), particle: t('tuberia_estacion') };
const tuberias = [
  [caja([0, 8, 11], [16, 13, 16], '#p'), caja([6, 7, 10], [10, 14, 16], '#s')],
  [caja([0, 11, 13], [16, 14, 16], '#p'), caja([0, 5, 13], [16, 8, 16], '#p'), caja([3, 4, 12], [5, 15, 16], '#s'), caja([11, 4, 12], [13, 15, 16], '#s')],
  [caja([0, 6, 12], [16, 10, 16], '#p'), caja([7, 5, 10], [9, 11, 12], '#s'), caja([4, 4, 9], [12, 12, 10], '#v'), caja([6, 6, 9], [10, 10, 10], '#s')],
  [caja([0, 10, 12], [9, 14, 16], '#p'), caja([5, 0, 12], [9, 10, 16], '#p'), caja([4, 9, 11], [10, 15, 12], '#s')]
];
tuberias.forEach((els, i) => json(`models/block/tuberias_pared_${i}.json`, mod(TUB, els)));
estados('tuberias_pared', { facing: FAC, variante: rango(8) }, (e) => `tuberias_pared_${e.variante % 4}`, 'tuberias_pared_0');

// cuadros electricos con relojes, diferenciales, avisos... (frente 16x16)
const frenteCuadro = (v) => {
  const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1210 + v, v % 2 ? [142, 150, 140] : [126, 134, 150]));
  const reloj = (cx, cy, r) => {
    l.disco(cx, cy, r, [30, 30, 30]); l.disco(cx, cy, r - 1, [232, 230, 214]);
    const a = hash(cx, cy, 1220 + v) * Math.PI * 1.4 + Math.PI * 0.8;
    l.linea(cx, cy, cx + Math.cos(a) * (r - 1.5), cy + Math.sin(a) * (r - 1.5), [180, 30, 30], 1);
  };
  switch (v % 8) {
    case 0: reloj(5, 5, 3.5); reloj(11, 5, 3.5); l.rect(3, 10, 12, 13, [30, 30, 30]); for (let x = 4; x < 12; x += 2) l.rect(x, 11, x, 12, [200, 200, 196]); break;
    case 1: for (let y = 2; y < 14; y += 4) for (let x = 2; x < 14; x += 3) { l.rect(x, y, x + 1, y + 2, [236, 236, 230]); l.punto(x, y + (hash(x, y, v) > 0.5 ? 0 : 2), [30, 30, 30]); } break;
    case 2: reloj(8, 6, 5); l.rect(4, 12, 11, 13, [30, 30, 30]); l.rect(5, 12, 5, 13, [60, 220, 80]); break;
    case 3: l.poligono([[8, 2], [14, 13], [2, 13]], [236, 196, 40]); l.poligono([[8, 4], [12, 12], [4, 12]], [236, 196, 40]); l.linea(9, 5, 7, 9, NEGRO, 1); l.linea(7, 9, 9, 9, NEGRO, 1); l.linea(9, 9, 7, 12, NEGRO, 1); break;
    case 4: reloj(5, 5, 3.5); reloj(11, 5, 3.5); reloj(5, 11, 3.5); reloj(11, 11, 3.5); break;
    case 5: l.rect(2, 2, 13, 6, [20, 20, 20]); l.texto('47', 3, 0, [230, 50, 40]); l.rect(3, 9, 5, 11, [60, 200, 70]); l.rect(7, 9, 9, 11, [220, 60, 40]); l.rect(11, 9, 13, 11, [230, 200, 40]); break;
    case 6: l.rect(12, 6, 13, 10, [40, 40, 40]); l.rect(2, 2, 10, 4, [236, 236, 228]); l.rect(2, 3, 9, 3, [40, 40, 40]); break;
    default: reloj(8, 8, 6); l.punto(8, 8, [30, 30, 30]); break;
  }
  return l;
};
for (let v = 0; v < 8; v++) {
  guardar(TEX, `cuadro_electrico_${v}`, frenteCuadro(v));
  json(`models/block/cuadro_electrico_${v}.json`, mod({ f: t(`cuadro_electrico_${v}`), m: t('tuberia_soporte'), particle: t(`cuadro_electrico_${v}`) }, [
    cajaFrente([1, 1, 12], [15, 15, 16], '#f', '#m')
  ]));
}
estados('cuadro_electrico', { facing: FAC, variante: rango(8) }, (e) => `cuadro_electrico_${e.variante}`, 'cuadro_electrico_0');

// senales y carteles (64x64): avisos, el tablon de instrucciones roto (4, 5 y 6), notas numeradas
function cartel (fondo, borde, lineas, tinta, extra) {
  const l = new Lienzo(64);
  l.pintar((x, y) => {
    if (x < 4 || y < 6 || x > 59 || y > 57) return null;
    if (x < 6 || y < 8 || x > 57 || y > 55) return borde;
    return escala(fondo, 0.93 + fbm(x, y, 1300 + lineas.length, 64) * 0.09);
  });
  if (extra) extra(l);
  let y = Math.round(32 - lineas.length * 4.5) + (extra && extra.arriba ? 10 : 0);
  for (const s of lineas) { l.texto(s, 'centro', y, tinta); y += 9; }
  return l;
}
/** Quemado y roto: un trozo que falta (transparente) y el borde chamuscado. */
function romper (l, canal, cuanto) {
  const out = new Lienzo(64);
  out.pintar((x, y) => {
    const i = (y * 64 + x) * 4;
    if (l.data[i + 3] === 0) return null;
    const r = fbm(x, y, canal, 64) + (x + y) / 128 * cuanto;
    if (r > 0.95) return null;
    const c = [l.data[i], l.data[i + 1], l.data[i + 2]];
    if (r > 0.85) return mezcla(c, [30, 22, 16], (r - 0.85) * 9);
    return c;
  });
  return out;
}
const rayo = (l) => {
  l.poligono([[32, 10], [44, 30], [20, 30]], [236, 196, 40]);
  l.poligono([[32, 14], [41, 28], [23, 28]], [20, 20, 20]);
  l.poligono([[32, 16], [39, 27], [25, 27]], [236, 196, 40]);
  l.linea(33, 17, 30, 22, NEGRO, 1.4); l.linea(30, 22, 34, 22, NEGRO, 1.4); l.linea(34, 22, 31, 27, NEGRO, 1.4);
};
rayo.arriba = true;
const CARTELES = [
  cartel([236, 200, 48], NEGRO, ['PELIGRO', 'ALTA', 'TENSION'], NEGRO, rayo),
  cartel([236, 236, 228], [40, 120, 70], ['ACCESO', 'PERSONAL', 'NIVEL 3'], [40, 120, 70]),
  cartel([40, 44, 48], [200, 200, 196], ['SECTORES', 'A B -', 'C D -'], [236, 236, 228]),
  cartel([236, 200, 48], NEGRO, ['GASOIL', 'BOCA DEL', 'DEPOSITO'], NEGRO),
  romper(cartel([228, 226, 214], [50, 50, 50], ['INSTRUC.', '1 ARRANCAR', 'GENERADOR', '2 REPARAR'], [30, 30, 30]), 1310, 0.15),
  romper(cartel([228, 226, 214], [50, 50, 50], ['PANELES', '3 FUSIBLES', 'A CONTROL', '4 EN ORDEN'], [30, 30, 30]), 1311, 0.35),
  romper(cartel([228, 226, 214], [50, 50, 50], ['5 PASAR', 'NO ACERC', 'SE AL', 'SECTOR'], [30, 30, 30]), 1312, 0.55),
  cartel([236, 236, 228], [170, 40, 34], ['REV. 47-B', 'NO TOCAR', 'DOC 0371'], [170, 40, 34])
];
CARTELES.forEach((l, v) => {
  guardar(TEX, `senal_estacion_${v}`, l);
  json(`models/block/senal_estacion_${v}.json`, mod({ f: t(`senal_estacion_${v}`), particle: t(`senal_estacion_${v}`) }, [{
    from: [0, 0, 15.9], to: [16, 16, 15.9], faces: { north: { texture: '#f', uv: [0, 0, 16, 16] } }
  }]));
});
estados('senal_estacion', { facing: FAC, variante: rango(8) }, (e) => `senal_estacion_${e.variante}`, 'senal_estacion_0');

// la luz roja de emergencia, enjaulada
guardar(TEX, 'luz_roja', new Lienzo(16).pintar((x, y) => {
  const d = Math.hypot(x - 7.5, y - 7.5) / 8;
  return mezcla([255, 120, 100], [200, 20, 16], d);
}));
json('models/block/luz_roja.json', mod({ l: t('luz_roja'), m: t('tuberia_soporte'), particle: t('luz_roja') }, [
  caja([5, 4, 14], [11, 10, 16], '#m'),
  { ...caja([5.5, 4.5, 11], [10.5, 9.5, 14], '#l'), shade: false, light_emission: 15 },
  caja([5, 4, 10.5], [5.5, 10, 14], '#m'), caja([10.5, 4, 10.5], [11, 10, 14], '#m'),
  caja([5, 9.5, 10.5], [11, 10, 14], '#m'), caja([7.75, 4, 10.5], [8.25, 10, 11], '#m')
]));
estados('luz_roja', { facing: FAC, variante: rango(8) }, () => 'luz_roja', 'luz_roja');

// rotulos de sector (letra y numero) con el color del sector
for (let s = 0; s < 4; s++) {
  for (let n = 0; n < 16; n++) {
    const l = new Lienzo(32);
    l.rect(2, 8, 29, 23, NEGRO);
    l.rect(3, 9, 28, 22, SECTOR[s]);
    const txt = `${LETRA[s]}-${String(n).padStart(2, '0')}`;
    l.texto(txt, 'centro', 12, s === 1 ? NEGRO : [245, 245, 238]);
    guardar(TEX, `rotulo_${LETRA[s].toLowerCase()}${n}`, l);
    json(`models/block/rotulo_${LETRA[s].toLowerCase()}${n}.json`, mod({ f: t(`rotulo_${LETRA[s].toLowerCase()}${n}`), particle: t(`rotulo_${LETRA[s].toLowerCase()}${n}`) }, [{
      from: [0, 0, 15.9], to: [16, 16, 15.9], faces: { north: { texture: '#f', uv: [0, 0, 16, 16] } }
    }]));
  }
}
estados('rotulo_sector', { facing: FAC, color: rango(4), num: rango(16) }, (e) => `rotulo_${LETRA[e.color].toLowerCase()}${e.num}`, 'rotulo_a1');
console.log('  tuberias, cuadros, carteles, luces y rotulos');

/* ============================================================ en el suelo */

const pinta = (n, lado, fn) => { guardar(TEX, n, new Lienzo(lado).pintar(fn)); return t(n); };
pinta('armario_lado', 16, (x, y) => metal(x, y, 1400, [120, 128, 120]));
const ARMARIOS = [[120, 128, 120], [96, 110, 128], [140, 140, 132], [104, 120, 100]];
ARMARIOS.forEach((col, v) => {
  const l = new Lienzo(16).pintar((x, y) => {
    let c = metal(x, y, 1410 + v, col);
    if (x === 7 || x === 8) c = escala(c, 0.72);                                    // las dos puertas
    if ((x === 6 || x === 9) && y >= 7 && y <= 9) c = [200, 200, 196];             // tiradores
    if (y >= 2 && y <= 4 && ((x >= 2 && x <= 5) || (x >= 10 && x <= 13)) && x % 2 === 0) c = [40, 40, 40]; // rejillas
    return c;
  });
  if (v === 1) { l.poligono([[12, 9], [15, 14], [9, 14]], AMARILLO); l.punto(12, 12, NEGRO); }
  if (v === 3) l.rect(2, 11, 5, 13, [236, 236, 228]);
  guardar(TEX, `armario_electrico_${v}`, l);
  json(`models/block/armario_electrico_${v}.json`, mod({ f: t(`armario_electrico_${v}`), m: t('armario_lado'), particle: t('armario_lado') }, [
    cajaFrente([0, 0, 2], [16, 16, 16], '#f', '#m')
  ]));
});
estados('armario_electrico', { facing: FAC, variante: rango(8) }, (e) => `armario_electrico_${e.variante % 4}`, 'armario_electrico_0');

pinta('transformador_lado', 16, (x, y) => {
  let c = escala([84, 98, 84], 0.86 + fbm(x, y, 1420, 16) * 0.15);
  if (x % 3 === 0) c = escala(c, 0.65); // aletas
  if (x % 3 === 1) c = escala(c, 1.15);
  return c;
});
pinta('transformador_tapa', 16, (x, y) => metal(x, y, 1421, [84, 98, 84]));
pinta('aislador', 16, (x, y) => (y % 3 === 0 ? [120, 70, 40] : escala([150, 90, 50], 0.9 + hash(x, y, 1422) * 0.15)));
const placaAT = (v) => {
  const l = new Lienzo(16).pintar((x, y) => escala([84, 98, 84], 0.86 + fbm(x, y, 1423 + v, 16) * 0.15));
  l.rect(3, 4, 12, 11, [236, 236, 228]); l.rect(4, 5, 11, 5, NEGRO); l.rect(4, 7, 9, 7, NEGRO); l.rect(4, 9, 10, 9, NEGRO);
  if (v > 0) { l.poligono([[8, 12], [11, 15], [5, 15]], AMARILLO); }
  return l;
};
for (let v = 0; v < 3; v++) {
  guardar(TEX, `transformador_${v}`, placaAT(v));
  json(`models/block/transformador_${v}.json`, mod({ f: t(`transformador_${v}`), s: t('transformador_lado'), a: t('transformador_tapa'), i: t('aislador'), particle: t('transformador_lado') }, [
    { from: [1, 0, 1], to: [15, 13, 15], faces: { north: { texture: '#f', uv: [0, 0, 16, 16] }, south: { texture: '#s' }, east: { texture: '#s' }, west: { texture: '#s' }, up: { texture: '#a' }, down: { texture: '#a' } } },
    caja([3, 13, 6], [5, 16, 8], '#i'), caja([7, 13, 6], [9, 16, 8], '#i'), caja([11, 13, 6], [13, 16, 8], '#i')
  ]));
}
estados('transformador', { facing: FAC, variante: rango(8) }, (e) => `transformador_${e.variante % 3}`, 'transformador_0');

const MAQUINAS = [
  // bomba: carcasa gris con un motor y una rueda
  (l) => { l.disco(8, 8, 6, [50, 54, 56]); l.disco(8, 8, 4, [100, 106, 108]); l.disco(8, 8, 1.5, [30, 30, 30]); },
  // compresor: panel con dos relojes y una luz
  (l) => { l.disco(5, 6, 3, NEGRO); l.disco(5, 6, 2, [232, 230, 214]); l.disco(11, 6, 3, NEGRO); l.disco(11, 6, 2, [232, 230, 214]); l.rect(7, 11, 9, 13, [220, 50, 40]); },
  // unidad de control: pantalla verde apagada y botones
  (l) => { l.rect(2, 2, 13, 8, [20, 28, 22]); l.rect(3, 3, 6, 3, [60, 160, 80]); for (let x = 3; x < 13; x += 3) l.rect(x, 11, x + 1, 12, [200, 200, 196]); },
  // ventilador industrial
  (l) => { l.disco(8, 8, 6.5, [30, 30, 30]); for (let a = 0; a < 6; a++) { const r = a * Math.PI / 3; l.linea(8, 8, 8 + Math.cos(r) * 5.5, 8 + Math.sin(r) * 5.5, [120, 120, 116], 1.5); } l.disco(8, 8, 1.5, [160, 160, 156]); }
];
pinta('maquina_lado', 16, (x, y) => metal(x, y, 1430, [110, 116, 118]));
MAQUINAS.forEach((dib, v) => {
  const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1431 + v, v === 0 ? [70, 100, 130] : [110, 116, 118]));
  dib(l);
  guardar(TEX, `maquina_estacion_${v}`, l);
  json(`models/block/maquina_estacion_${v}.json`, mod({ f: t(`maquina_estacion_${v}`), m: t('maquina_lado'), particle: t('maquina_lado') }, [
    cajaFrente([0, 0, 0], [16, 16, 16], '#f', '#m')
  ]));
});
estados('maquina_estacion', { facing: FAC, variante: rango(8) }, (e) => `maquina_estacion_${e.variante % 4}`, 'maquina_estacion_0');

// bidones: amarillos, uno oxidado y uno rojo
const bidonTex = (n, col, oxido) => pinta(n, 16, (x, y) => {
  let c = escala(col, 0.88 + fbm(x, y, 1440 + oxido * 7, 16) * 0.14);
  if (y === 3 || y === 12) c = escala(c, 0.7); // aros
  if (oxido && fbm(x, y, 1441, 16) > 0.55) c = mezcla(c, [110, 60, 30], 0.7);
  return c;
});
const tapaBidon = pinta('bidon_tapa', 16, (x, y) => (Math.hypot(x - 11, y - 4) < 1.6 ? [40, 40, 40] : escala([140, 140, 136], 0.9 + hash(x, y, 1442) * 0.1)));
const BID = [bidonTex('bidon_amarillo', AMARILLO, 0), bidonTex('bidon_oxidado', [150, 130, 70], 1), bidonTex('bidon_rojo', [180, 40, 34], 0)];
const bidon = (x, z, tex, alto = 13) => ({ from: [x, 0, z], to: [x + 6.5, alto, z + 6.5], faces: { north: { texture: tex }, south: { texture: tex }, east: { texture: tex }, west: { texture: tex }, up: { texture: '#t' }, down: { texture: '#t' } } });
const BIDONES = [
  [bidon(1, 1, '#a'), bidon(8.5, 1.5, '#a'), bidon(4, 8.5, '#a', 12)],
  [bidon(1, 1, '#o'), bidon(8.5, 7.5, '#a', 12)],
  [bidon(2, 2, '#r'), bidon(8.5, 8, '#o', 11)]
];
BIDONES.forEach((els, v) => json(`models/block/bidones_${v}.json`, mod({ a: BID[0], o: BID[1], r: BID[2], t: tapaBidon, particle: BID[0] }, els)));
estados('bidones', { facing: FAC, variante: rango(8) }, (e) => `bidones_${e.variante % 3}`, 'bidones_0');

// estanterias con cajas, rollos de cable y bidoncitos
const est = pinta('estanteria_metal', 16, (x, y) => escala([150, 110, 50], 0.85 + hash(x, y, 1450) * 0.15));
const cajaCarton = pinta('caja_carton', 16, (x, y) => {
  let c = escala([168, 130, 84], 0.88 + fbm(x, y, 1451, 16) * 0.14);
  if (x === 7 || x === 8) c = [196, 176, 120];
  return c;
});
const rollo = pinta('rollo_cable', 16, (x, y) => ((x + y) % 3 === 0 ? [110, 40, 20] : escala([190, 90, 40], 0.9 + hash(x, y, 1452) * 0.1)));
for (let v = 0; v < 4; v++) {
  const els = [];
  for (const x of [0, 15]) els.push(caja([x, 0, 4], [x + 1, 16, 5], '#e'), caja([x, 0, 15], [x + 1, 16, 16], '#e'));
  for (const y of [0, 7, 14]) els.push(caja([0, y, 4], [16, y + 1, 16], '#e'));
  // cosas en las baldas, segun la variante
  const cosas = [[1, 1, '#c', 6, 5], [8, 1, '#r', 5, 5], [2, 8, '#c', 5, 6], [9, 8, '#c', 5, 4]];
  cosas.forEach(([x, y, tx, w, h], i) => { if ((i + v) % 3 !== 2) els.push(caja([x, y, 6 + (i % 2)], [x + w, y + h, 14], tx)); });
  json(`models/block/estanteria_estacion_${v}.json`, mod({ e: est, c: cajaCarton, r: rollo, particle: est }, els));
}
estados('estanteria_estacion', { facing: FAC, variante: rango(8) }, (e) => `estanteria_estacion_${e.variante % 4}`, 'estanteria_estacion_0');

// consolas de la sala de control: mesa con el tablero inclinado (botones, relojes, pantallas)
pinta('consola_lado', 16, (x, y) => metal(x, y, 1460, [150, 146, 128]));
for (let v = 0; v < 4; v++) {
  const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1461 + v, [120, 118, 104]));
  for (let i = 0; i < 6; i++) {
    const bx = 2 + (i % 3) * 4 + (v % 2);
    const by = 3 + Math.floor(i / 3) * 6;
    const cols = [[200, 40, 34], [60, 170, 70], [230, 196, 40], [200, 200, 196]];
    if ((i + v) % 4 === 0) { l.disco(bx + 1, by + 1, 2, NEGRO); l.disco(bx + 1, by + 1, 1.2, [232, 230, 214]); } else l.rect(bx, by, bx + 1, by + 1, cols[(i + v) % 4]);
  }
  guardar(TEX, `consola_control_${v}`, l);
  json(`models/block/consola_control_${v}.json`, mod({ f: t(`consola_control_${v}`), m: t('consola_lado'), particle: t('consola_lado') }, [
    caja([0, 0, 6], [16, 10, 16], '#m'),
    { from: [0, 9, 2], to: [16, 11, 14], rotation: { angle: 22.5, axis: 'x', origin: [8, 10, 8] },
      faces: { up: { texture: '#f', uv: [0, 0, 16, 16] }, north: { texture: '#m' }, south: { texture: '#m' }, east: { texture: '#m' }, west: { texture: '#m' }, down: { texture: '#m' } } }
  ]));
}
estados('consola_control', { facing: FAC, variante: rango(8) }, (e) => `consola_control_${e.variante % 4}`, 'consola_control_0');

// la barandilla de la pasarela: amarilla y gastada
const bar = pinta('barandilla_estacion', 16, (x, y) => (hash(x, y, 1470) > 0.85 ? [110, 100, 70] : escala(AMARILLO, 0.82 + hash(x, y, 1471) * 0.15)));
json('models/block/barandilla.json', mod({ b: bar, particle: bar }, [
  caja([0, 14, 14], [16, 16, 16], '#b'), caja([0, 7, 14.5], [16, 8.5, 15.5], '#b'),
  caja([1, 0, 14], [3, 14, 16], '#b'), caja([13, 0, 14], [15, 14, 16], '#b')
]));
estados('barandilla', { facing: FAC, variante: rango(8) }, () => 'barandilla', 'barandilla');

// cables que cuelgan del techo (en cruz, recortados)
for (let v = 0; v < 2; v++) {
  guardar(TEX, `cables_colgando_${v}`, new Lienzo(16).pintar((x, y) => {
    const cables = v === 0 ? [[3, 16, [30, 30, 30]], [7, 11, [150, 50, 30]], [12, 14, [30, 30, 30]]] : [[5, 9, [40, 40, 40]], [10, 16, [60, 80, 150]]];
    for (const [cx, largo, col] of cables) {
      const dx = Math.round(Math.sin(y / 5 + cx) * 0.8);
      if (x === cx + dx && y < largo) return escala(col, 0.85 + hash(x, y, 1480) * 0.25);
      if (y === largo - 1 && Math.abs(x - cx - dx) === 1) return [180, 120, 60]; // el cobre pelado
    }
    return null;
  }));
  json(`models/block/cables_colgando_${v}.json`, { parent: 'minecraft:block/cross', textures: { cross: t(`cables_colgando_${v}`) } });
}
estados('cables_colgando', { facing: FAC, variante: rango(8) }, (e) => `cables_colgando_${e.variante % 2}`, 'cables_colgando_0');
console.log('  armarios, transformadores, maquinas, bidones, estanterias, consolas, barandilla y cables');

/* ============================================================ lo que se usa */

// el punto de recogida: un palet bajo con cinta de aviso y lo que da encima (o vacio)
const palet = pinta('palet_estacion', 16, (x, y) => (y < 3 ? aviso(x, y, 3) : escala([150, 118, 70], 0.85 + hash(x, y, 1500) * 0.15)));
const naranjaCable = pinta('cable_naranja', 16, (x, y) => ((x + y * 2) % 4 === 0 ? [140, 60, 20] : escala([220, 110, 40], 0.9 + hash(x, y, 1501) * 0.1)));
const rojoCaja = pinta('caja_herramientas_tex', 16, (x, y) => (y === 5 ? [40, 40, 40] : escala([190, 40, 34], 0.88 + hash(x, y, 1502) * 0.12)));
const cinta = pinta('cinta_tex', 16, (x, y) => escala(y % 4 === 0 ? [60, 60, 64] : [30, 30, 34], 0.9 + hash(x, y, 1503) * 0.2));
const gris = pinta('gris_estacion', 16, (x, y) => escala([150, 150, 146], 0.88 + hash(x, y, 1504) * 0.12));
const ceramica = pinta('fusible_ceramica', 16, (x, y) => escala([236, 232, 220], 0.92 + hash(x, y, 1505) * 0.08));
const FUS_COL = SECTOR.map((c, i) => pinta(`fusible_${SECTOR_NOMBRE[i]}_tex`, 16, () => c));
const base = [caja([1, 0, 1], [15, 2, 15], '#p')];
const objetoPunto = [
  // 0 cable: un rollo
  [caja([4, 2, 4], [12, 4, 12], '#o'), caja([3, 4, 3], [13, 6, 13], '#o'), caja([6, 4, 6], [10, 6, 10], '#g')],
  // 1 herramientas: la caja roja con su asa
  [caja([3, 2, 5], [13, 7, 11], '#o'), caja([5, 7, 7.5], [6, 9, 8.5], '#g'), caja([10, 7, 7.5], [11, 9, 8.5], '#g'), caja([5, 9, 7.5], [11, 10, 8.5], '#g')],
  // 2 cinta: dos rollos
  [caja([3, 2, 3], [8, 4, 8], '#o'), caja([8, 2, 8], [13, 4, 13], '#o'), caja([5, 4, 5], [11, 6, 11], '#o')],
  // 3 bidon: la garrafa amarilla con su asa y su tapon
  [caja([4, 2, 5], [12, 13, 11], '#o'), caja([5, 13, 7], [11, 15, 8.5], '#o'), caja([9, 13, 9], [11, 15, 11], '#g')]
];
const texPunto = [naranjaCable, rojoCaja, cinta, BID[0]];
for (let tipo = 0; tipo < 8; tipo++) {
  let els;
  let o;
  if (tipo < 4) { els = objetoPunto[tipo]; o = texPunto[tipo]; } else {
    // el fusible: en su soporte, de pie, con la banda del color
    o = FUS_COL[tipo - 4];
    els = [caja([5, 2, 5], [11, 4, 11], '#g'), caja([6.5, 4, 6.5], [9.5, 6, 9.5], '#g'), caja([6, 6, 6], [10, 12, 10], '#c'), caja([5.9, 8, 5.9], [10.1, 10, 10.1], '#o'), caja([6.5, 12, 6.5], [9.5, 14, 9.5], '#g')];
  }
  json(`models/block/punto_recogida_${tipo}.json`, mod({ p: palet, o, g: gris, c: ceramica, particle: palet }, [...base, ...els]));
}
json('models/block/punto_recogida_vacio.json', mod({ p: palet, particle: palet }, base));
estados('punto_recogida', { facing: FAC, tipo: rango(8), vacio: [false, true] }, (e) => (e.vacio ? 'punto_recogida_vacio' : `punto_recogida_${e.tipo}`), 'punto_recogida_3');

// los paneles averiados: la puerta abierta y dentro lo roto (segun lo que haga falta); reparados, cerrados
const caja_panel = pinta('panel_caja', 16, (x, y) => metal(x, y, 1510, [116, 124, 118]));
const interiorPanel = (tipo) => {
  const l = new Lienzo(16).pintar((x, y) => {
    let c = escala([40, 42, 40], 0.85 + hash(x, y, 1520 + tipo) * 0.2);
    if (fbm(x, y, 1521 + tipo, 16) > 0.62) c = [18, 16, 14]; // quemado
    return c;
  });
  // carriles con diferenciales
  for (const y of [3, 9]) { l.rect(1, y, 14, y + 2, [196, 196, 190]); for (let x = 2; x < 14; x += 3) l.rect(x, y, x, y + 2, [90, 90, 86]); }
  if (tipo === 0) { l.linea(4, 13, 6, 8, [200, 80, 30], 1); l.linea(11, 15, 9, 9, [200, 80, 30], 1); l.punto(6, 8, [230, 170, 90]); l.punto(9, 9, [230, 170, 90]); }
  if (tipo === 1) { l.rect(5, 9, 10, 11, [60, 60, 58]); l.linea(10, 8, 13, 14, [140, 140, 136], 1); l.punto(3, 14, [190, 190, 186]); l.punto(12, 6, [190, 190, 186]); }
  if (tipo === 2) { for (let x = 3; x < 13; x += 3) { l.linea(x, 12, x + 1, 15, [190, 120, 60], 1); l.punto(x, 12, [240, 210, 140]); } }
  return l;
};
const puertaAbierta = pinta('panel_puerta', 16, (x, y) => {
  let c = metal(x, y, 1530, [116, 124, 118]);
  if (y >= 6 && y <= 9 && x >= 3 && x <= 12) c = [236, 196, 40]; // el aviso de la puerta
  return c;
});
for (let tipo = 0; tipo < 3; tipo++) {
  guardar(TEX, `panel_interior_${tipo}`, interiorPanel(tipo));
  json(`models/block/panel_averiado_${tipo}.json`, mod({ i: t(`panel_interior_${tipo}`), m: caja_panel, d: puertaAbierta, particle: caja_panel }, [
    // la caja, abierta por delante: el fondo con lo roto y los lados
    { from: [1, 0, 15], to: [15, 16, 16], faces: { north: { texture: '#i', uv: [0, 0, 16, 16] }, south: { texture: '#m' } } },
    caja([1, 0, 11], [2, 16, 15], '#m'), caja([14, 0, 11], [15, 16, 15], '#m'),
    caja([2, 15, 11], [14, 16, 15], '#m'), caja([2, 0, 11], [14, 1, 15], '#m'),
    // la puerta, abierta del todo hacia la izquierda
    { from: [14, 0, 0], to: [15, 16, 11], faces: { east: { texture: '#d', uv: [0, 0, 16, 16] }, west: { texture: '#d', uv: [0, 0, 16, 16] }, north: { texture: '#m' }, south: { texture: '#m' }, up: { texture: '#m' }, down: { texture: '#m' } } }
  ]));
}
guardar(TEX, 'panel_cerrado', (() => {
  const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1540, [116, 124, 118]));
  l.rect(3, 6, 12, 9, [236, 196, 40]);
  l.rect(12, 2, 13, 3, [60, 220, 80]);
  l.rect(2, 12, 2, 14, [200, 200, 196]);
  return l;
})());
json('models/block/panel_reparado.json', mod({ f: t('panel_cerrado'), m: caja_panel, particle: caja_panel }, [
  cajaFrente([1, 0, 11], [15, 16, 16], '#f', '#m')
]));
estados('panel_averiado', { facing: FAC, tipo: rango(8), hecho: [false, true] }, (e) => (e.hecho ? 'panel_reparado' : `panel_averiado_${e.tipo % 3}`), 'panel_averiado_0');

// la caja de fusibles de la sala de control: el hueco con el color; con el fusible, encendido
for (let c = 0; c < 4; c++) {
  for (const hecho of [false, true]) {
    const n = `caja_fusible_${SECTOR_NOMBRE[c]}${hecho ? '_puesto' : ''}`;
    const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1550 + c, [96, 100, 104]));
    l.rect(1, 1, 14, 3, SECTOR[c]);
    l.rect(5, 4, 10, 14, [20, 20, 20]);
    if (hecho) { l.rect(6, 5, 9, 13, [236, 232, 220]); l.rect(6, 8, 9, 10, SECTOR[c]); l.rect(12, 6, 13, 7, [60, 230, 80]); } else l.rect(12, 6, 13, 7, [200, 40, 30]);
    guardar(TEX, n, l);
    json(`models/block/${n}.json`, mod({ f: t(n), m: caja_panel, particle: caja_panel }, [
      { ...cajaFrente([3, 1, 13], [13, 15, 16], '#f', '#m'), ...(hecho ? { shade: false } : {}) }
    ]));
  }
}
estados('caja_fusible', { facing: FAC, color: rango(4), hecho: [false, true] }, (e) => `caja_fusible_${SECTOR_NOMBRE[e.color]}${e.hecho ? '_puesto' : ''}`, 'caja_fusible_rojo');

// los interruptores: la palanca abajo (apagado) o arriba (encendido), con su color
for (let c = 0; c < 4; c++) {
  const placa = `interruptor_${SECTOR_NOMBRE[c]}`;
  const l = new Lienzo(16).pintar((x, y) => metal(x, y, 1560 + c, [96, 100, 104]));
  l.rect(2, 1, 13, 3, SECTOR[c]);
  l.rect(6, 4, 9, 13, [20, 20, 20]);
  guardar(TEX, placa, l);
  for (const hecho of [false, true]) {
    json(`models/block/${placa}${hecho ? '_arriba' : ''}.json`, mod({ f: t(placa), m: caja_panel, g: gris, r: FUS_COL[c], particle: caja_panel }, [
      cajaFrente([4, 2, 14], [12, 14, 16], '#f', '#m'),
      { from: [7, 8, 9], to: [9, 9.5, 14], rotation: { angle: hecho ? 45 : -45, axis: 'x', origin: [8, 8.5, 14] },
        faces: { north: { texture: '#r' }, south: { texture: '#g' }, east: { texture: '#g' }, west: { texture: '#g' }, up: { texture: '#g' }, down: { texture: '#g' } } }
    ]));
  }
}
estados('interruptor', { facing: FAC, color: rango(4), hecho: [false, true] }, (e) => `interruptor_${SECTOR_NOMBRE[e.color]}${e.hecho ? '_arriba' : ''}`, 'interruptor_rojo');

// la persiana metalica de 3x3: lamas; abajo la franja de aviso, arriba el cajon donde se enrolla
const lamas = (x, y) => {
  let c = escala([140, 146, 148], 0.85 + fbm(x, y, 1570, 16) * 0.15);
  if (y % 4 === 3) c = escala(c, 0.62);
  if (y % 4 === 0) c = escala(c, 1.12);
  return c;
};
pinta('persiana_lamas', 16, lamas);
pinta('persiana_baja', 16, (x, y) => (y >= 12 ? aviso(x, y, 3) : lamas(x, y)));
pinta('persiana_alta', 16, (x, y) => (y < 4 ? escala([90, 94, 96], 0.85 + hash(x, y, 1571) * 0.15) : lamas(x, y)));
['persiana_baja', 'persiana_lamas', 'persiana_alta'].forEach((n) => {
  json(`models/block/${n}.json`, mod({ l: t(n), particle: t(n) }, [
    { from: [0, 0, 6], to: [16, 16, 10], faces: { north: { texture: '#l', uv: [0, 0, 16, 16] }, south: { texture: '#l', uv: [0, 0, 16, 16] }, east: { texture: '#l' }, west: { texture: '#l' }, up: { texture: '#l' }, down: { texture: '#l' } } }
  ]));
});
json('models/block/persiana_abierta.json', { parent: 'minecraft:block/block', textures: { particle: t('persiana_lamas') }, elements: [] });
estados('persiana_estacion', { facing: FAC, parte: rango(9), abierta: [false, true] },
  (e) => (e.abierta ? 'persiana_abierta' : ['persiana_baja', 'persiana_lamas', 'persiana_alta'][e.parte % 3]), 'persiana_lamas');

// los focos de la pasarela, en el suelo junto a ella
for (const lit of [false, true]) {
  const n = lit ? 'foco_pasarela_on' : 'foco_pasarela_off';
  guardar(TEX, n, new Lienzo(16).pintar((x, y) => {
    if (x < 2 || y < 2 || x > 13 || y > 13) return chapa(x, y, 1580);
    if ((x - 2) % 4 === 0) return [50, 50, 50];
    const d = Math.hypot(x - 7.5, y - 7.5) / 7;
    return lit ? mezcla([255, 250, 210], [250, 200, 90], d) : mezcla([110, 110, 100], [60, 60, 56], d);
  }));
  json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_bottom_top', textures: { top: t(n), side: t('chapa_estacion'), bottom: t('chapa_estacion') } });
}
estados('foco_pasarela', { lit: [false, true] }, (e) => (e.lit ? 'foco_pasarela_on' : 'foco_pasarela_off'), 'foco_pasarela_on');
console.log('  puntos, paneles, fusibles, interruptores, persianas y focos');

/* ============================================================ objetos */

const objeto = (n, fn) => {
  guardar(ITEM, n, new Lienzo(16).pintar(fn));
  json(`models/item/${n}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${n}` } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/${n}` } });
};
objeto('cable_electrico', (x, y) => {
  const d = Math.hypot(x - 7.5, y - 7.5);
  if (d > 7 || d < 2.5) return null;
  if (Math.abs(d - 4.8) < 0.6) return [120, 50, 16];
  return escala([226, 112, 40], 0.85 + ((x + y) % 3 === 0 ? 0.2 : 0));
});
objeto('caja_herramientas', (x, y) => {
  if (y >= 3 && y <= 4 && x >= 5 && x <= 10) return (x === 5 || x === 10 || y === 3) ? [50, 50, 50] : null;
  if (y < 5 || y > 13 || x < 1 || x > 14) return null;
  if (y === 8) return [40, 40, 40];
  if (x === 7 || x === 8) return y === 9 ? [200, 200, 196] : null;
  return escala([200, 40, 34], 0.85 + (y < 8 ? 0.15 : 0));
});
objeto('cinta_aislante', (x, y) => {
  const d = Math.hypot(x - 7.5, y - 7.5);
  if (d > 6.5 || d < 3) return null;
  if (d < 3.8) return [200, 190, 160];
  return escala([40, 40, 46], 0.9 + (x < 7 && y < 7 ? 0.4 : 0));
});
objeto('bidon_combustible', (x, y) => {
  if (y >= 1 && y <= 3 && x >= 5 && x <= 10) return (y === 1 || x === 5 || x === 10) ? [180, 140, 30] : null;
  if (y >= 1 && y <= 3 && x >= 11 && x <= 12) return [40, 40, 40];
  if (y < 4 || y > 15 || x < 2 || x > 13) return null;
  let c = escala(AMARILLO, 0.85 + (x < 6 ? 0.15 : 0));
  if ((x - 2) === (y - 4) || (13 - x) === (y - 4)) c = escala(c, 0.8); // la X de la chapa
  return c;
});
SECTOR.forEach((col, i) => objeto(`fusible_${SECTOR_NOMBRE[i]}`, (x, y) => {
  if (x < 5 || x > 10) return null;
  if (y < 1 || y > 14) return null;
  if (y <= 2 || y >= 13) return [180, 180, 176];
  if (y >= 6 && y <= 9) return col;
  return escala([236, 232, 220], x === 5 ? 0.85 : 1);
}));
console.log('  objetos');

/* ============================================================ nombres */

const NOMBRES = {
  hormigon_estacion: ['Hormigón de la estación', 'Station concrete'],
  pared_pintada: ['Pared pintada', 'Painted wall'],
  franja_sector: ['Franja de sector', 'Sector stripe'],
  suelo_estacion: ['Suelo de la estación', 'Station floor'],
  suelo_estacion_manchado: ['Suelo manchado de aceite', 'Oil-stained floor'],
  chapa_estacion: ['Chapa estriada', 'Checker plate'],
  rejilla_estacion: ['Rejilla', 'Grating'],
  techo_estacion: ['Techo de la estación', 'Station ceiling'],
  techo_tubos: ['Techo con bandeja de cables', 'Cable tray ceiling'],
  lampara_estacion: ['Lámpara enjaulada', 'Caged lamp'],
  lampara_fundida: ['Lámpara fundida', 'Burnt-out lamp'],
  macizo_estacion: ['Macizo', 'Solid'],
  generador_carcasa: ['Carcasa del generador', 'Generator casing'],
  generador_radiador: ['Radiador del generador', 'Generator radiator'],
  generador_tapa: ['Tapa del generador', 'Generator top'],
  boca_combustible: ['Boca del depósito', 'Fuel inlet'],
  tubo_escape: ['Tubo de escape', 'Exhaust pipe'],
  tuberias_pared: ['Tuberías', 'Pipes'],
  cuadro_electrico: ['Cuadro eléctrico', 'Electrical box'],
  senal_estacion: ['Cartel', 'Sign'],
  luz_roja: ['Luz de emergencia', 'Emergency light'],
  rotulo_sector: ['Rótulo de sector', 'Sector label'],
  cables_colgando: ['Cables colgando', 'Hanging cables'],
  armario_electrico: ['Armario eléctrico', 'Electrical cabinet'],
  transformador: ['Transformador', 'Transformer'],
  maquina_estacion: ['Máquina', 'Machine'],
  bidones: ['Bidones', 'Drums'],
  estanteria_estacion: ['Estantería', 'Shelf'],
  consola_control: ['Consola de control', 'Control console'],
  barandilla: ['Barandilla', 'Railing'],
  punto_recogida: ['Punto de recogida', 'Pickup point'],
  panel_averiado: ['Panel averiado', 'Broken panel'],
  caja_fusible: ['Caja de fusibles', 'Fuse box'],
  interruptor: ['Interruptor', 'Switch'],
  persiana_estacion: ['Persiana metálica', 'Roller shutter'],
  foco_pasarela: ['Foco de la pasarela', 'Catwalk light']
};
const OTROS = {
  'item.backrooms_evento.cable_electrico': ['Cable eléctrico', 'Electrical cable'],
  'item.backrooms_evento.caja_herramientas': ['Caja de herramientas', 'Toolbox'],
  'item.backrooms_evento.cinta_aislante': ['Cinta aislante', 'Insulating tape'],
  'item.backrooms_evento.bidon_combustible': ['Bidón de gasoil', 'Fuel can'],
  'item.backrooms_evento.fusible_rojo': ['Fusible rojo', 'Red fuse'],
  'item.backrooms_evento.fusible_amarillo': ['Fusible amarillo', 'Yellow fuse'],
  'item.backrooms_evento.fusible_azul': ['Fusible azul', 'Blue fuse'],
  'item.backrooms_evento.fusible_verde': ['Fusible verde', 'Green fuse'],
  'entity.backrooms_evento.amasijo': ['El Amasijo', 'The Tangle'],
  'death.attack.backrooms_evento.estacion': ['%1$s se quedó a oscuras en la Estación', '%1$s was left in the dark at the Station'],
  'death.attack.backrooms_evento.estacion.player': ['%2$s arrastró a %1$s a los pasillos sin luz', '%2$s dragged %1$s into the unlit corridors']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  for (const [id, v] of Object.entries(OTROS)) lang[id] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: la estacion');
