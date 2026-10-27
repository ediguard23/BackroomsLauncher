'use strict';
/**
 * La Estacion Electrica (Nivel 3): texturas, modelos, estados, objetos y nombres de sus bloques
 * (evento/.../estacion/BloquesEstacion.java). Todo dibujado por codigo.
 *
 *   node tools/texturas/estacion.js   (va en npm run texturas)
 *
 * Una central vieja que nadie mantiene: hormigon con humedad, paredes pintadas de verde palido y
 * desconchadas, franjas con el color de cada sector, chapa estriada, marcos de acero de las rejas, bidones
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

/* ============================================================ el marco de las rejas */

/** Acero pintado de gris: un perfil con sus cantos, remaches y algo de desgaste. */
const marco = (x, y, canal = 1110) => {
  let c = escala([92, 96, 100], 0.86 + fbm(x, y, canal, 16) * 0.2 + hash(x, y, canal + 1) * 0.05);
  if (x <= 1 || x >= 14) c = escala(c, 0.72);                    // los cantos del perfil
  if (x === 2 || x === 13) c = escala(c, 1.18);                  // el brillo del borde
  if ((x === 3 || x === 12) && (y === 2 || y === 13)) c = [176, 178, 180]; // remaches
  if (fbm(x, y * 0.5, canal + 2, 16) > 0.7) c = mezcla(c, [120, 82, 52], 0.35); // oxido
  return c;
};
guardar(TEX, 'marco_reja', new Lienzo(16).pintar((x, y) => marco(x, y)));

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
  'techo_estacion', 'techo_tubos', 'lampara_estacion', 'lampara_fundida', 'marco_reja']) {
  cubo(n);
  simple(n);
}
SECTOR_NOMBRE.forEach((n) => cubo(`franja_sector_${n}`));
estados('franja_sector', { color: rango(4) }, (e) => `franja_sector_${SECTOR_NOMBRE[e.color]}`, 'franja_sector_rojo');
console.log('  estructura');

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
  cartel([40, 44, 48], [200, 200, 196], ['CENTRAL', 'SALIDA', 'ARRIBA'], [236, 236, 228]),
  cartel([236, 200, 48], NEGRO, ['GASOIL', 'BOCA DEL', 'DEPOSITO'], NEGRO),
  romper(cartel([228, 226, 214], [50, 50, 50], ['INSTRUC.', '1 GASOIL', 'GENERAD.', '2 REJAS'], [30, 30, 30]), 1310, 0.15),
  romper(cartel([228, 226, 214], [50, 50, 50], ['FUSIBLE', 'EN CAJA', '3 CENTRAL', 'CADA PISO'], [30, 30, 30]), 1311, 0.35),
  romper(cartel([228, 226, 214], [50, 50, 50], ['4 SUBIR', '5 PASAR', 'NO ACERC', 'SE AL'], [30, 30, 30]), 1312, 0.55),
  cartel([236, 236, 228], [170, 40, 34], ['REV. 47-B', 'NO TOCAR', 'DOC 0371'], [170, 40, 34])
];
CARTELES.forEach((l, v) => {
  guardar(TEX, `senal_estacion_${v}`, l);
  json(`models/block/senal_estacion_${v}.json`, mod({ f: t(`senal_estacion_${v}`), particle: t(`senal_estacion_${v}`) }, [{
    from: [0, 0, 15.9], to: [16, 16, 15.9], faces: { north: { texture: '#f', uv: [0, 0, 16, 16] } }
  }]));
});
estados('senal_estacion', { facing: FAC, variante: rango(8) }, (e) => `senal_estacion_${e.variante}`, 'senal_estacion_0');

// la luz roja de emergencia: placa de chapa, tulipa roja encendida y la jaula delante
guardar(TEX, 'luz_roja', new Lienzo(16).pintar((x, y) => {
  const d = Math.hypot(x - 8, y - 7) / 4;
  let c = mezcla([255, 150, 120], [196, 24, 18], Math.min(1, d));
  if (x === 6 && y === 5) c = [255, 236, 226];
  return c;
}));
json('models/block/luz_roja.json', mod({ l: t('luz_roja'), m: t('tuberia_soporte'), particle: t('luz_roja') }, [
  caja([4, 3, 15], [12, 11, 16], '#m'),
  caja([4.5, 3.5, 14], [11.5, 10.5, 15], '#m'),
  { ...caja([5, 4, 10.5], [11, 10, 14], '#l'), shade: false, light_emission: 15 },
  caja([6.5, 3.5, 10], [7, 10.5, 10.5], '#m'), caja([9, 3.5, 10], [9.5, 10.5, 10.5], '#m'),
  caja([4.5, 6.75, 10], [11.5, 7.25, 10.5], '#m')
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

// racks de servidores abandonados (para llenar las centrales): bahias de discos, ventiladores y pilotos
// apagados; uno con un par de pilotos rojos, uno abierto sin discos y con cables, uno con la rejilla
pinta('rack_lado', 16, (x, y) => {
  let c = metal(x, y, 1430, [44, 46, 50]);
  if (y % 4 === 0) c = escala(c, 0.75);
  return c;
});
for (let v = 0; v < 4; v++) {
  const l = new Lienzo(16).pintar((x, y) => {
    let c = metal(x, y, 1431 + v, [38, 40, 44]);
    if (x === 0 || x === 15) c = [70, 72, 76];                              // los rieles
    return c;
  });
  for (let fila = 0; fila < 7; fila++) {
    const y0 = 1 + fila * 2;
    if (v === 2 && fila % 2 === 1) continue;                                // le faltan discos
    l.rect(2, y0, 13, y0, fila % 3 === 2 ? [70, 74, 80] : [24, 24, 26]);  // un servidor (o un hueco)
    l.punto(3, y0, v === 1 && fila % 3 === 0 ? [230, 40, 30] : [40, 46, 40]); // su piloto (casi todos apagados)
    if (v !== 3) l.rect(9, y0, 12, y0, [56, 58, 62]);                      // las bahias
  }
  if (v === 2) {
    // cables que cuelgan por delante
    for (let y = 2; y < 16; y++) { l.punto(6 + Math.round(Math.sin(y * 0.6)), y, [20, 20, 20]); l.punto(10 + Math.round(Math.cos(y * 0.5)), y, [150, 30, 30]); }
  }
  if (v === 3) {
    // la rejilla del ventilador
    l.disco(8, 8, 5.5, [26, 26, 28]);
    for (let y = 3; y <= 13; y += 2) l.rect(3, y, 13, y, [66, 68, 72]);
  }
  guardar(TEX, `rack_servidor_${v}`, l);
  json(`models/block/rack_servidor_${v}.json`, mod({ f: t(`rack_servidor_${v}`), s: t('rack_lado'), particle: t('rack_lado') }, [
    cajaFrente([1, 0, 1], [15, 16, 15], '#f', '#s')
  ]));
}
estados('rack_servidor', { facing: FAC, variante: rango(8) }, (e) => `rack_servidor_${e.variante % 4}`, 'rack_servidor_0');

// cables gruesos tirados por el suelo (de las maquinas al panel): rectos, en curva, un haz y uno con empalme
pinta('cable_grueso', 16, (x, y) => escala(y % 4 === 0 ? [36, 36, 38] : [22, 22, 24], 0.9 + hash(x, y, 1440) * 0.15));
pinta('cable_grueso_rojo', 16, (x, y) => escala(y % 4 === 0 ? [120, 34, 30] : [92, 24, 22], 0.9 + hash(x, y, 1441) * 0.15));
const CABLES_SUELO = [
  [caja([3, 0, 0], [5, 1.5, 16], '#n'), caja([9, 0, 0], [11, 1.5, 16], '#r')],
  [caja([3, 0, 0], [5, 1.5, 9], '#n'), caja([3, 0, 7], [16, 1.5, 9], '#n'), caja([10, 0, 0], [12, 1.5, 4], '#r')],
  [caja([2, 0, 0], [4, 1.5, 16], '#n'), caja([4, 0, 0], [6, 1.5, 16], '#r'), caja([6, 0, 0], [8, 1.5, 16], '#n'), caja([4.5, 1.5, 0], [6.5, 3, 16], '#n')],
  [caja([6, 0, 0], [8, 1.5, 16], '#n'), caja([5, 0, 6], [9, 3, 10], '#r')]
];
CABLES_SUELO.forEach((els, v) => json(`models/block/cables_suelo_${v}.json`, mod({ n: t('cable_grueso'), r: t('cable_grueso_rojo'), particle: t('cable_grueso') }, els)));
estados('cables_suelo', { facing: FAC, variante: rango(8) }, (e) => `cables_suelo_${e.variante % 4}`, 'cables_suelo_0');

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
const mangos = pinta('cizalla_mangos', 16, (x, y) => escala([190, 36, 30], 0.85 + hash(x, y, 1502) * 0.15));
const acero = pinta('acero_estacion', 16, (x, y) => escala([176, 180, 184], 0.8 + hash(x, y, 1506) * 0.25));
const cinta = pinta('cinta_tex', 16, (x, y) => escala(y % 4 === 0 ? [60, 60, 64] : [30, 30, 34], 0.9 + hash(x, y, 1503) * 0.2));
const gris = pinta('gris_estacion', 16, (x, y) => escala([150, 150, 146], 0.88 + hash(x, y, 1504) * 0.12));
const ceramica = pinta('fusible_ceramica', 16, (x, y) => escala([236, 232, 220], 0.92 + hash(x, y, 1505) * 0.08));
const bandaFusible = pinta('fusible_banda', 16, (x, y) => (x % 4 === 0 ? [40, 40, 40] : escala(AMARILLO, 0.95 + hash(x, y, 1507) * 0.08)));
const base = [caja([1, 0, 1], [15, 2, 15], '#p')];
const objetoPunto = [
  // 0 cable: un rollo
  [caja([4, 2, 4], [12, 4, 12], '#o'), caja([3, 4, 3], [13, 6, 13], '#o'), caja([6, 4, 6], [10, 6, 10], '#g')],
  // 1 cizalla: tumbada en diagonal, mangos rojos y las mandibulas de acero
  [{ ...caja([2, 2, 7], [10, 3.5, 8], '#o'), rotation: { angle: 22.5, axis: 'y', origin: [8, 2, 8] } },
    { ...caja([2, 2, 9], [10, 3.5, 10], '#o'), rotation: { angle: 22.5, axis: 'y', origin: [8, 2, 8] } },
    { ...caja([10, 2, 7.5], [14, 3, 9.5], '#g'), rotation: { angle: 22.5, axis: 'y', origin: [8, 2, 8] } }],
  // 2 cinta: dos rollos
  [caja([3, 2, 3], [8, 4, 8], '#o'), caja([8, 2, 8], [13, 4, 13], '#o'), caja([5, 4, 5], [11, 6, 11], '#o')],
  // 3 bidon: la garrafa amarilla con su asa y su tapon
  [caja([4, 2, 5], [12, 13, 11], '#o'), caja([5, 13, 7], [11, 15, 8.5], '#o'), caja([9, 13, 9], [11, 15, 11], '#g')],
  // 4 fusibles: tres cartuchos tumbados en su bandeja (cuerpo de ceramica, banda amarilla, puntas de metal)
  [caja([2, 2, 4], [14, 3, 12], '#g'),
    caja([4, 3, 4.5], [12, 5, 6.5], '#c'), caja([7, 3, 4.4], [9, 5.1, 6.6], '#o'), caja([3, 3, 4.6], [4, 5, 6.4], '#g'), caja([12, 3, 4.6], [13, 5, 6.4], '#g'),
    caja([4, 3, 7], [12, 5, 9], '#c'), caja([7, 3, 6.9], [9, 5.1, 9.1], '#o'), caja([3, 3, 7.1], [4, 5, 8.9], '#g'), caja([12, 3, 7.1], [13, 5, 8.9], '#g'),
    caja([4, 3, 9.5], [12, 5, 11.5], '#c'), caja([7, 3, 9.4], [9, 5.1, 11.6], '#o'), caja([3, 3, 9.6], [4, 5, 11.4], '#g'), caja([12, 3, 9.6], [13, 5, 11.4], '#g')]
];
const texPunto = [naranjaCable, mangos, cinta, BID[0], bandaFusible];
for (let tipo = 0; tipo < 5; tipo++) {
  json(`models/block/punto_recogida_${tipo}.json`, mod({ p: palet, o: texPunto[tipo], g: tipo === 1 ? acero : gris, c: ceramica, particle: palet }, [...base, ...objetoPunto[tipo]]));
}
json('models/block/punto_recogida_vacio.json', mod({ p: palet, particle: palet }, base));
estados('punto_recogida', { facing: FAC, tipo: rango(8), vacio: [false, true] }, (e) => (e.vacio ? 'punto_recogida_vacio' : `punto_recogida_${Math.min(e.tipo, 4)}`), 'punto_recogida_4');

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

// la caja de fusible de una reja: en el marco de la derecha, se ve por todos los lados; con el fusible, la luz verde
for (const hecho of [false, true]) {
  const n = hecho ? 'caja_fusible_puesto' : 'caja_fusible';
  const l = new Lienzo(16).pintar((x, y) => marco(x, y, 1550));
  l.rect(3, 1, 12, 14, [40, 40, 40]);
  l.rect(4, 2, 11, 13, [96, 100, 104]);
  l.rect(4, 2, 11, 3, AMARILLO);
  l.rect(6, 5, 9, 12, [20, 20, 20]);
  if (hecho) { l.rect(7, 5, 8, 12, [236, 232, 220]); l.rect(7, 7, 8, 9, AMARILLO); }
  l.rect(10, 5, 10, 6, hecho ? [60, 230, 80] : [200, 40, 30]);
  guardar(TEX, n, l);
  json(`models/block/${n}.json`, { parent: 'minecraft:block/cube', textures: { north: t(n), south: t(n), east: t(n), west: t(n), up: t('marco_reja'), down: t('marco_reja'), particle: t(n) } });
}
json('blockstates/caja_fusible.json', { variants: { 'hecho=false': { model: t('caja_fusible') }, 'hecho=true': { model: t('caja_fusible_puesto') } } });
json('items/caja_fusible.json', { model: { type: 'minecraft:model', model: t('caja_fusible') } });
for (const viejo of ['blockstates/interruptor.json', 'items/interruptor.json']) {
  const ruta = path.join(ASSETS, viejo);
  if (fs.existsSync(ruta)) fs.unlinkSync(ruta);
}

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
estados('persiana_estacion', { facing: FAC, parte: rango(9), abierta: [false, true], oculta: [false, true] },
  (e) => (e.abierta ? 'persiana_abierta' : ['persiana_baja', 'persiana_lamas', 'persiana_alta'][e.parte % 3]), 'persiana_lamas');

// los buzones: cerrados, con la ranura del fusible; abiertos, vacios con la puerta abierta
{
  const cerrado = new Lienzo(16).pintar((x, y) => metal(x, y, 1600, [128, 132, 138]));
  cerrado.rect(2, 2, 10, 4, [232, 228, 214]);
  cerrado.rect(3, 3, 9, 3, [40, 40, 40]);
  cerrado.rect(12, 5, 13, 10, [36, 36, 36]);
  cerrado.rect(3, 11, 8, 14, AMARILLO);
  cerrado.rect(4, 12, 7, 13, [22, 22, 22]);
  cerrado.punto(10, 13, [200, 40, 30]);
  guardar(TEX, 'buzon_cerrado', cerrado);
  const vacio = new Lienzo(16).pintar((x, y) => (x < 1 || y < 1 || x > 14 || y > 14 ? [90, 94, 98] : escala([28, 28, 30], 0.8 + hash(x, y, 1601) * 0.3)));
  guardar(TEX, 'buzon_vacio', vacio);
  json('models/block/buzon_cerrado.json', mod({ f: t('buzon_cerrado'), m: t('armario_lado'), particle: t('buzon_cerrado') }, [
    cajaFrente([3, 2, 10], [13, 14, 16], '#f', '#m')
  ]));
  json('models/block/buzon_abierto.json', mod({ f: t('buzon_vacio'), d: t('buzon_cerrado'), m: t('armario_lado'), particle: t('buzon_cerrado') }, [
    cajaFrente([3, 2, 10], [13, 14, 16], '#f', '#m'),
    { from: [2.5, 2, 1], to: [3, 14, 10], faces: { west: { texture: '#d', uv: [0, 0, 16, 16] }, east: { texture: '#d', uv: [0, 0, 16, 16] }, north: { texture: '#m' }, south: { texture: '#m' }, up: { texture: '#m' }, down: { texture: '#m' } } }
  ]));
  estados('buzon_estacion', { facing: FAC, abierta: [false, true] }, (e) => (e.abierta ? 'buzon_abierto' : 'buzon_cerrado'), 'buzon_cerrado');
}

// las rejas de los laberintos (y la puerta encadenada del final): barrotes de acero con travesanos
const barrotes = (x, y) => {
  if (x % 4 === 1 || x % 4 === 2) return escala([96, 100, 102], 0.85 + hash(x, y, 1590) * 0.2 + (x % 4 === 1 ? 0.15 : 0));
  if (y === 7 || y === 8) return escala([80, 84, 86], 0.9 + hash(x, y, 1591) * 0.15);
  return null;
};
pinta('reja_barrotes', 16, barrotes);
pinta('reja_baja', 16, (x, y) => (y >= 13 ? aviso(x, y, 3) : barrotes(x, y)));
pinta('reja_alta', 16, (x, y) => (y < 3 ? escala([70, 72, 74], 0.85 + hash(x, y, 1592) * 0.15) : barrotes(x, y)));
['reja_baja', 'reja_barrotes', 'reja_alta'].forEach((n) => {
  json(`models/block/${n}.json`, mod({ l: t(n), particle: t(n) }, [
    { from: [0, 0, 7], to: [16, 16, 9], faces: { north: { texture: '#l', uv: [0, 0, 16, 16] }, south: { texture: '#l', uv: [0, 0, 16, 16] }, east: { texture: '#l', uv: [7, 0, 9, 16] }, west: { texture: '#l', uv: [7, 0, 9, 16] }, up: { texture: '#l', uv: [0, 7, 16, 9] }, down: { texture: '#l', uv: [0, 7, 16, 9] } } }
  ]));
});
json('models/block/reja_abierta.json', { parent: 'minecraft:block/block', textures: { particle: t('reja_barrotes') }, elements: [] });
estados('reja_estacion', { facing: FAC, parte: rango(9), abierta: [false, true], oculta: [false, true] },
  (e) => (e.abierta ? 'reja_abierta' : ['reja_baja', 'reja_barrotes', 'reja_alta'][e.parte % 3]), 'reja_barrotes');

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
objeto('cizalla', (x, y) => {
  // en diagonal: las mandibulas arriba a la derecha, los mangos rojos abajo a la izquierda
  const u = x - y;
  const v = x + y;
  if (v >= 18 && v <= 27 && Math.abs(u) <= 3) return escala([186, 190, 194], 0.8 + (u > 0 ? 0.2 : 0));
  if (v >= 15 && v < 18 && Math.abs(u) <= 2) return [70, 70, 72];
  if (v >= 2 && v < 15 && (Math.abs(u - 2) <= 0 || Math.abs(u + 2) <= 0 || Math.abs(u - 1.5) < 1 && v < 12 || Math.abs(u + 1.5) < 1 && v < 12)) return escala([196, 36, 30], 0.85 + (v % 3 === 0 ? 0.15 : 0));
  return null;
});
objeto('cinta_aislante', (x, y) => {
  // un rollo visto un poco de lado: la cara (negra y brillante), el canto debajo, el carton del centro
  // y la punta de la cinta colgando
  const cx = 7.5;
  const cy = 6.5;
  const d = Math.hypot(x - cx, (y - cy) * 1.15);
  const dc = Math.hypot(x - cx, (y - cy - 2) * 1.15);
  if (d <= 6.2) {
    if (d < 1.6) return [40, 34, 28];                              // el agujero
    if (d < 2.9) return escala([206, 178, 128], d < 2.3 ? 0.8 : 1); // el carton
    let c = escala([44, 46, 54], 0.95 + (x < cx && y < cy ? 0.25 : 0));
    if (Math.abs(d - 4.6) < 0.5 && x < cx + 2 && y < cy) c = [120, 124, 140]; // el brillo
    if (d > 5.6) c = [20, 20, 24];
    return c;
  }
  if (dc <= 6.2 && y > cy) return escala([28, 28, 34], 0.8 + (dc < 5.4 ? 0.2 : 0)); // el canto
  if (x >= 11 && x <= 13 && y >= 9 && y <= 15 && x - 11 <= (y - 9) / 2.5 + 1) return y === 15 ? [20, 20, 24] : [56, 58, 66]; // la punta
  return null;
});
objeto('fusible', (x, y) => {
  // un fusible de cartucho en diagonal: puntas de metal, cuerpo de ceramica blanco y la banda amarilla
  const t = ((x - 7.5) - (y - 7.5)) / Math.SQRT2;
  const w = ((x - 7.5) + (y - 7.5)) / Math.SQRT2;
  const L = 7.4;
  const R = 2.3;
  if (Math.abs(t) > L || Math.abs(w) > R) return null;
  const borde = Math.abs(w) > R - 0.8 || Math.abs(t) > L - 0.7;
  const luz = 1.1 - (w + R) / (2 * R) * 0.45;
  let c;
  if (Math.abs(t) > L - 2.6) c = escala([196, 200, 206], luz);          // las puntas de metal
  else if (Math.abs(t) < 1.4) c = escala(AMARILLO, luz);                 // la banda
  else c = escala([240, 236, 226], luz);                                 // la ceramica
  if (Math.abs(Math.abs(t) - (L - 2.6)) < 0.5) c = escala(c, 0.6);        // el canto de la punta
  if (borde) c = escala(c, 0.55);
  return c;
});
for (const viejo of ['fusible_rojo', 'fusible_amarillo', 'fusible_azul', 'fusible_verde', 'caja_herramientas']) {
  for (const ruta of [path.join(ITEM, viejo + '.png'), path.join(ASSETS, 'models', 'item', viejo + '.json'), path.join(ASSETS, 'items', viejo + '.json')]) {
    if (fs.existsSync(ruta)) fs.unlinkSync(ruta);
  }
}
{
  const fuera = [
    ...['generador_carcasa', 'generador_radiador', 'generador_tapa', 'boca_combustible', 'tubo_escape', 'generador_frente', 'generador_lado',
      'generador_atras', 'generador_tapa_grande'].map((n) => path.join(TEX, n + '.png')),
    path.join(ITEM, 'bidon_combustible.png'), path.join(ASSETS, 'models', 'item', 'bidon_combustible.json'), path.join(ASSETS, 'items', 'bidon_combustible.json')
  ];
  for (const n of ['generador_carcasa', 'generador_radiador', 'generador_tapa', 'boca_combustible', 'tubo_escape', 'macizo_estacion', 'generador_estacion']) {
    fuera.push(path.join(ASSETS, 'models', 'block', n + '.json'), path.join(ASSETS, 'blockstates', n + '.json'), path.join(ASSETS, 'items', n + '.json'));
  }
  for (let n = 0; n < 36; n++) fuera.push(path.join(ASSETS, 'models', 'block', `generador_estacion_${n}.json`));
  for (const ruta of fuera) if (fs.existsSync(ruta)) fs.unlinkSync(ruta);
}
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
  marco_reja: ['Marco de la reja', 'Gate frame'],
  tuberias_pared: ['Tuberías', 'Pipes'],
  cuadro_electrico: ['Cuadro eléctrico', 'Electrical box'],
  senal_estacion: ['Cartel', 'Sign'],
  luz_roja: ['Luz de emergencia', 'Emergency light'],
  rotulo_sector: ['Rótulo de sector', 'Sector label'],
  cables_colgando: ['Cables colgando', 'Hanging cables'],
  armario_electrico: ['Armario eléctrico', 'Electrical cabinet'],
  transformador: ['Transformador', 'Transformer'],
  rack_servidor: ['Rack de servidores', 'Server rack'],
  cables_suelo: ['Cables por el suelo', 'Floor cables'],
  maquina_estacion: ['Máquina', 'Machine'],
  bidones: ['Bidones', 'Drums'],
  estanteria_estacion: ['Estantería', 'Shelf'],
  consola_control: ['Consola de control', 'Control console'],
  barandilla: ['Barandilla', 'Railing'],
  punto_recogida: ['Punto de recogida', 'Pickup point'],
  panel_averiado: ['Central eléctrica', 'Power terminal'],
  caja_fusible: ['Caja de fusible', 'Fuse box'],
  reja_estacion: ['Reja', 'Gate'],
  buzon_estacion: ['Buzón', 'Mailbox'],
  persiana_estacion: ['Persiana metálica', 'Roller shutter'],
  foco_pasarela: ['Foco de la pasarela', 'Catwalk light']
};
const OTROS = {
  'item.backrooms_evento.cable_electrico': ['Cable eléctrico', 'Electrical cable'],
  'item.backrooms_evento.cizalla': ['Cizalla', 'Bolt cutters'],
  'item.backrooms_evento.cinta_aislante': ['Cinta aislante', 'Insulating tape'],
  'item.backrooms_evento.fusible': ['Fusible', 'Fuse'],
  'entity.backrooms_evento.amasijo': ['El Amasijo', 'The Tangle'],
  'death.attack.backrooms_evento.estacion': ['%1$s no salió de la Estación', '%1$s never left the Station'],
  'death.attack.backrooms_evento.estacion.player': ['%2$s arrastró a %1$s entre los transformadores', '%2$s dragged %1$s between the transformers']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  for (const [id, v] of Object.entries(OTROS)) lang[id] = v[k];
  for (const viejo of ['block.backrooms_evento.interruptor', 'item.backrooms_evento.caja_herramientas', 'item.backrooms_evento.fusible_rojo',
    'item.backrooms_evento.fusible_amarillo', 'item.backrooms_evento.fusible_azul', 'item.backrooms_evento.fusible_verde',
    'item.backrooms_evento.bidon_combustible', ...['macizo_estacion', 'generador_carcasa', 'generador_radiador', 'generador_tapa', 'boca_combustible',
      'tubo_escape', 'generador_estacion'].map((n) => `block.backrooms_evento.${n}`)]) delete lang[viejo];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: la estacion');
