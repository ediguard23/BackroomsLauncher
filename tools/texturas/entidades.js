'use strict';
/**
 * Modelos y texturas de las entidades del evento (mod evento/), dibujados
 * por codigo: la Bacteria y el Smiler (Nivel 0) y la Serpiente (las Piscinas).
 *
 *   node tools/texturas/entidades.js [carpeta-de-vistas]
 *
 * Cada modelo es una lista de piezas en el espacio de modelos de Minecraft
 * (y hacia abajo, 16 = un bloque, y=24 los pies, la cara mira a -z). De la
 * lista salen tres cosas, para que nunca se desincronicen (y el Anfitrion, el de la Fiesta):
 *  - la clase Java con la malla (cliente/entidad/Malla<Nombre>.java),
 *  - la textura (textures/entity/<nombre>.png), con cada cubo en su sitio
 *    del atlas (UV de caja de Minecraft) y pintado segun su material,
 *  - si se pide, una vista previa en PNG (frente, lado y tres cuartos).
 *
 * La Bacteria sigue el estilo de la de Escape the Backrooms (y de la de
 * ModelEngine que el usuario tomo de referencia), pero es un modelo propio:
 * espina fina y encorvada, brazos larguisimos que llegan al suelo con garras
 * de tres dedos, piernas quebradas y una cabeza de caja con la boca abierta
 * llena de dientes. Mide unos 3,3 bloques: cabe en los pasillos de 4.
 */

const fs = require('fs');
const path = require('path');
const png = require('../imagenes/png');
const { hash, ruido, escala, Lienzo, guardar } = require('./lienzo');

const RAIZ = path.join(__dirname, '..', '..');
const ASSETS = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento');
const JAVA = path.join(RAIZ, 'evento', 'src', 'main', 'java', 'net', 'backrooms', 'evento', 'cliente', 'entidad');
const VISTAS = process.argv[2];

const g = (d) => d * Math.PI / 180;

/* ============================================================ Bacteria */

// pieza: [nombre, padre, pivote [x,y,z], rotacion en grados [x,y,z], cubos]
// cubo: [x, y, z, ancho, alto, fondo, material, inflado]
const BACTERIA = {
  nombre: 'bacteria',
  tex: [128, 128],
  piezas: [
    ['cadera', null, [0, -2, 2], [0, 0, 0], [
      [-3, -1, -1.5, 6, 2, 3, 'fibra'],
      [-1, -4, -1, 2, 4, 2, 'fibra']
    ]],
    // espina: tres tramos que se encorvan hacia delante
    ['espina', 'cadera', [0, -3, 0], [18, 0, 0], [
      [-1, -10, -1, 2, 10, 2, 'fibra'],
      [-4, -7, -0.5, 8, 1, 1, 'hebra'],
      [-3.5, -3, -0.5, 7, 1, 1, 'hebra'],
      [1, -9, -2, 1, 6, 1, 'hebra']
    ]],
    ['pecho', 'espina', [0, -10, 0], [14, 0, 0], [
      [-1, -9, -1, 2, 9, 2, 'fibra'],
      [-5, -8, -0.5, 10, 1, 1, 'hebra'],
      [-6, -5, -0.5, 12, 1, 1, 'hebra'],
      [-4.5, -2, -0.5, 9, 1, 1, 'hebra'],
      [-2, -7, -1.5, 4, 5, 3, 'fibra']
    ]],
    ['hombros', 'pecho', [0, -9, 0], [-10, 0, 0], [
      [-11, -1.5, -1.5, 22, 3, 3, 'fibra'],
      [-13, -4, -1, 3, 4, 2, 'fibra'],
      [10, -4, -1, 3, 4, 2, 'fibra'],
      [-3, -3, -1, 6, 2, 2, 'fibra'],
      [-8, 1.5, -0.5, 1, 9, 1, 'hebra'],
      [6, 1.5, -0.5, 1, 7, 1, 'hebra'],
      [-5, 1.5, 0.5, 1, 5, 1, 'hebra']
    ]],
    ['cuello', 'hombros', [0, -1.5, 0], [-28, 0, 0], [
      [-1, -6, -1, 2, 6, 2, 'fibra'],
      [-2, -4, 0, 4, 1, 1, 'hebra']
    ]],
    // cabeza de caja, larga hacia delante, y la mandibula aparte para abrirla
    ['cabeza', 'cuello', [0, -6, 0], [30, 0, 0], [
      [-5.5, -9, -12, 11, 7, 13, 'cabeza'],
      [-5, -2, -11.5, 10, 2, 1, 'dientes'],
      [-5, -2, -10.5, 1, 2, 8, 'dientes'],
      [4, -2, -10.5, 1, 2, 8, 'dientes'],
      [-4.5, -2.01, -10.5, 9, 0.01, 9, 'boca'],
      [-5, -10, 0, 10, 4, 2, 'cabeza'],
      [-1, -13, -6, 2, 4, 2, 'hebra'],
      [2.5, -12, -2, 1, 3, 1, 'hebra'],
      [-4, -11, -9, 1, 2, 1, 'hebra'],
      [-6.5, -7, -8, 1, 6, 1, 'hebra'],
      [5.5, -6, -4, 1, 7, 1, 'hebra']
    ]],
    ['mandibula', 'cabeza', [0, -2, 0], [26, 0, 0], [
      [-5, 0, -11, 10, 2, 11, 'cabeza'],
      [-4.5, -2, -10.5, 9, 2, 1, 'dientes'],
      [-4.5, -2, -9.5, 1, 2, 8, 'dientes'],
      [3.5, -2, -9.5, 1, 2, 8, 'dientes'],
      [-4, 0, -10, 8, 0.01, 9, 'boca'],
      [-1, 2, -8, 1, 5, 1, 'hebra']
    ]],
    // brazos: hombro, brazo, antebrazo y una mano de tres garras que toca el suelo
    ...brazo('izq', 1),
    ...brazo('der', -1),
    ...pierna('izq', 1),
    ...pierna('der', -1)
  ]
};

function brazo (lado, s) {
  const x = 11 * s;
  return [
    [`brazo_${lado}`, 'hombros', [x, -1, 0], [-8, 0, s * -8], [
      [-1, 0, -1, 2, 17, 2, 'fibra'],
      [s > 0 ? 1 : -2, 4, -0.5, 1, 7, 1, 'hebra']
    ]],
    [`antebrazo_${lado}`, `brazo_${lado}`, [0, 16.5, 0], [-14, 0, s * 4], [
      [-1, 0, -1, 2, 19, 2, 'fibra'],
      [-1.5, 6, -1.5, 3, 3, 3, 'fibra']
    ]],
    [`mano_${lado}`, `antebrazo_${lado}`, [0, 18.5, 0], [24, 0, 0], [
      [-2, 0, -1.5, 4, 2, 3, 'fibra']
    ]],
    // tres garras largas y abiertas, como un tripode
    [`garra_${lado}_a`, `mano_${lado}`, [0, 1.5, -1], [-18, 0, 0], [[-0.5, 0, -0.5, 1, 9, 1, 'garra']]],
    [`garra_${lado}_b`, `mano_${lado}`, [-1.5, 1.5, 0.5], [10, 0, 22], [[-0.5, 0, -0.5, 1, 8, 1, 'garra']]],
    [`garra_${lado}_c`, `mano_${lado}`, [1.5, 1.5, 0.5], [10, 0, -22], [[-0.5, 0, -0.5, 1, 8, 1, 'garra']]]
  ];
}

function pierna (lado, s) {
  return [
    [`muslo_${lado}`, 'cadera', [3 * s, 0, 0], [-26, 0, s * -6], [
      [-1, 0, -1, 2, 11, 2, 'fibra'],
      [-1.5, 3, -1.5, 3, 4, 3, 'fibra']
    ]],
    [`pierna_${lado}`, `muslo_${lado}`, [0, 10.5, 0], [48, 0, 0], [
      [-1, 0, -1, 2, 12, 2, 'fibra'],
      [s > 0 ? 0.5 : -1.5, 3, 0.5, 1, 6, 1, 'hebra']
    ]],
    [`pie_${lado}`, `pierna_${lado}`, [0, 11.5, 0], [-22, 0, 0], [
      [-1, 0, -1, 2, 3, 2, 'fibra'],
      [-1.5, 2, -5, 1, 1, 5, 'garra'],
      [0.5, 2, -5, 1, 1, 5, 'garra']
    ]]
  ];
}

/* ============================================================== Smiler */

// una sombra flotando con la cara: ojos blancos y una sonrisa enorme. Los
// ojos y los dientes brillan aparte (smiler_brillo.png, capa de "ojos").
const SMILER = {
  nombre: 'smiler',
  tex: [64, 64],
  piezas: [
    ['cuerpo', null, [0, 13, 0], [0, 0, 0], [
      [-4, -14, -2, 8, 16, 4, 'sombra'],
      [-3, 2, -1.5, 6, 6, 3, 'sombra'],
      [-2, 8, -1, 4, 5, 2, 'sombra']
    ]],
    ['cabeza', 'cuerpo', [0, -14, 0], [0, 0, 0], [
      [-7, -12, -4, 14, 12, 7, 'cara']
    ]],
    ['brazo_izq', 'cuerpo', [4.5, -13, 0], [0, 0, -12], [
      [0, 0, -1, 2, 20, 2, 'sombra']
    ]],
    ['brazo_der', 'cuerpo', [-4.5, -13, 0], [0, 0, 12], [
      [-2, 0, -1, 2, 20, 2, 'sombra']
    ]]
  ]
};

/* =========================================================== Serpiente */

// La entidad de las Piscinas (referencia de Bruno): una serpiente larguisima hecha
// de azulejos blancos con la junta azul y anillos de azulejo azul, que se alza del
// agua con un craneo enorme de ojos cian que brillan, la mandibula abierta, un
// cuello de costillas al aire y dos pares de brazos de hueso con garras. Tumbada
// mide unos 11 bloques; alzada, la cabeza queda a unos 3 del suelo. Los ojos van
// aparte (serpiente_brillo.png, capa de ojos, como el Smiler).

/**
 * Tramo del cuerpo que avanza hacia -z desde su pivote, con una junta que tapa el hueco de
 * la curva y una vertebra que asoma por el lomo (la cresta se ve de lejos).
 */
function tramo (nombre, padre, piv, rot, w, h, l, banda = false) {
  const cubos = [
    [-w / 2, -h / 2, -l, w, h, l, 'teja'],
    [-w / 2, -h / 2, -h / 2, w, h, h, 'teja', -0.3],
    [-1, -h / 2 - 3, -l / 2 - 1, 2, 3, 2, 'hueso']
  ];
  if (banda) cubos.push([-w / 2, -h / 2, -l / 2 - 1.5, w, h, 3, 'teja_azul', 0.35]);
  return [nombre, padre, piv, rot, cubos];
}

/** Tramo de la cola, tumbado: avanza hacia +z con la panza en el suelo (y = 7 del pivote). */
function cola (nombre, padre, piv, giro, w, h, l, banda = false) {
  const cubos = [
    [-w / 2, 7 - h, 0, w, h, l, 'teja'],
    [-w / 2, 7 - h, -w / 2, w, h, w, 'teja', -0.3]
  ];
  if (w >= 8) cubos.push([-1, 7 - h - 2.5, l / 2 - 1, 2, 2.5, 2, 'hueso']);
  if (banda) cubos.push([-w / 2, 7 - h, l / 2 - 1.5, w, h, 3, 'teja_azul', 0.35]);
  return [nombre, padre, piv, [0, giro, 0], cubos];
}

/** Brazo de hueso: humero, cubito y radio, la mano y cuatro falanges largas en garra. */
function brazoHueso (lado, padre, piv, rot, largo, codo, abre) {
  const s = lado === 'izq' ? 1 : -1;
  const p = `${lado}_${padre}`;
  const piezas = [
    [`humero_${p}`, padre, piv, rot, [
      [-1, 0, -1, 2, largo, 2, 'hueso'],
      [-1.5, -1, -1.5, 3, 3, 3, 'hueso']
    ]],
    [`cubito_${p}`, `humero_${p}`, [0, largo, 0], [codo, 0, 0], [
      [-1.5, 0, -0.5, 1, largo, 1, 'hueso'],
      [0.5, 0, -0.5, 1, largo, 1, 'hueso'],
      [-1.5, -1, -1, 3, 2, 2, 'hueso']
    ]],
    [`mano_${p}`, `cubito_${p}`, [0, largo, 0], [abre, 0, 0], [
      [-2, 0, -1, 4, 2, 2, 'hueso']
    ]]
  ];
  for (let k = 0; k < 4; k++) {
    const x = -1.5 + k;
    piezas.push([`falange_${p}_${k}`, `mano_${p}`, [x, 1.5, 0], [28, 0, s * (k - 1.5) * 14], [
      [-0.5, 0, -0.5, 1, 5, 1, 'hueso'],
      [-0.5, 4.5, -1.5, 1, 6, 1, 'falange']
    ]]);
  }
  return piezas;
}

/** Costillas al aire en la panza (+y) de un tramo del cuello. */
function costillas (nombre, padre, piv, rot, w, n) {
  const cubos = [];
  for (let k = 0; k < n; k++) {
    const z = -2 - k * 3;
    cubos.push([-w / 2 - 1, 0, z, 1, 4, 1, 'hueso']);
    cubos.push([w / 2, 0, z, 1, 4, 1, 'hueso']);
    cubos.push([-w / 2 - 1, 3.5, z, w + 2, 1, 1, 'hueso']);
  }
  return [nombre, padre, piv, rot, cubos];
}

const SERPIENTE = {
  nombre: 'serpiente',
  tex: [256, 256],
  compartir: true,
  vista: { esc: 2.5, ancho: 700, alto: 420, centro: 0.75 },
  piezas: [
    // donde el cuerpo empieza a alzarse, tumbado en el suelo: 3 bloques por detras del
    // origen de la entidad, para que la cabeza quede sobre su caja de choque
    ['base', null, [0, 17, 54], [0, 0, 0], [
      [-8, -7, -10, 16, 14, 12, 'teja']
    ]],
    // el tronco se alza (cada tramo, mas inclinado) y el cuello se dobla hacia delante
    tramo('tronco1', 'base', [0, 0, -10], [-25, 0, 0], 16, 14, 12),
    tramo('tronco2', 'tronco1', [0, 0, -12], [-30, 0, 0], 15, 14, 12, true),
    tramo('tronco3', 'tronco2', [0, 0, -12], [-20, 0, 0], 14, 13, 12),
    tramo('cuello1', 'tronco3', [0, 0, -12], [15, 0, 0], 14, 13, 11, true),
    tramo('cuello2', 'cuello1', [0, 0, -11], [30, 0, 0], 13, 12, 10),
    tramo('cuello3', 'cuello2', [0, 0, -10], [30, 0, 0], 12, 11, 8),
    costillas('costillas2', 'cuello2', [0, 4.5, 0], [0, 0, 0], 13, 3),
    costillas('costillas3', 'cuello3', [0, 4, 0], [0, 0, 0], 12, 2),
    // el craneo, el doble de ancho que el cuello, mirando hacia abajo: hocico,
    // pomulos, la fila de dientes y la mandibula abierta
    ['craneo', 'cuello3', [0, -1, -6], [20, 0, 0], [
      [-12, -14, -20, 24, 16, 20, 'craneo'],
      [-9, -15.5, -17, 18, 2, 14, 'craneo'],
      [-9, 2, -19, 18, 4, 14, 'craneo'],
      [-13, -6, -17, 2, 6, 10, 'hueso'],
      [11, -6, -17, 2, 6, 10, 'hueso'],
      [-8, 6, -18.5, 16, 2, 1, 'colmillo'],
      [-8.5, 6, -18, 1, 2, 11, 'colmillo'],
      [7.5, 6, -18, 1, 2, 11, 'colmillo'],
      // relieve de la cara: la ceja, los pomulos, el tabique y los bordes hunden las cuencas
      [-12, -14, -21.5, 24, 3.5, 1.5, 'craneo'],
      [-12, -4, -21, 10, 2, 1, 'craneo'],
      [2, -4, -21, 10, 2, 1, 'craneo'],
      [-2, -10.5, -21, 4, 6.5, 1, 'craneo'],
      [-12, -10.5, -21, 3, 6.5, 1, 'craneo'],
      [9, -10.5, -21, 3, 6.5, 1, 'craneo'],
      // colmillos largos que asoman de la fila de dientes
      [-6.5, 6, -18.9, 1, 4, 1, 'diente'],
      [-3, 6, -18.9, 1, 3, 1, 'diente'],
      [2, 6, -18.9, 1, 3, 1, 'diente'],
      [5.5, 6, -18.9, 1, 4, 1, 'diente']
    ]],
    ['mandibula', 'craneo', [0, 5, -4], [22, 0, 0], [
      [-8, 0, -15, 16, 3, 15, 'craneo'],
      [-7.5, -2, -14.5, 15, 2, 1, 'colmillo'],
      [-7.5, -2, -13.5, 1, 2, 10, 'colmillo'],
      [6.5, -2, -13.5, 1, 2, 10, 'colmillo'],
      [-5.5, -4, -14.9, 1, 3, 1, 'diente'],
      [-2, -4, -14.9, 1, 2, 1, 'diente'],
      [1, -4, -14.9, 1, 2, 1, 'diente'],
      [4.5, -4, -14.9, 1, 3, 1, 'diente']
    ]],
    // brazos: el par de delante sale del cuello y se estira hacia delante; el de
    // atras, mas corto, apoya las garras en el suelo
    ...brazoHueso('izq', 'cuello1', [7, 3, -5], [-10, 0, -30], 19, -42, 34),
    ...brazoHueso('der', 'cuello1', [-7, 3, -5], [6, 0, 32], 19, -30, 40),
    ...brazoHueso('izq', 'tronco1', [8, 4, -6], [40, 0, -34], 13, -28, 30),
    ...brazoHueso('der', 'tronco1', [-8, 4, -6], [40, 0, 34], 13, -28, 30),
    // la cola, tumbada en S y cada vez mas fina
    cola('cola1', 'base', [0, 0, 2], 25, 15, 13, 12),
    cola('cola2', 'cola1', [0, 0, 12], 25, 14, 12, 12, true),
    cola('cola3', 'cola2', [0, 0, 12], -40, 13, 11, 12),
    cola('cola4', 'cola3', [0, 0, 12], -45, 12, 10, 12),
    cola('cola5', 'cola4', [0, 0, 12], -40, 11, 9, 11, true),
    cola('cola6', 'cola5', [0, 0, 11], 35, 10, 8, 11),
    cola('cola7', 'cola6', [0, 0, 11], 45, 9, 7, 10),
    cola('cola8', 'cola7', [0, 0, 10], 45, 8, 6, 10, true),
    cola('cola9', 'cola8', [0, 0, 10], 30, 6, 5, 9),
    cola('cola10', 'cola9', [0, 0, 9], 20, 5, 4, 9)
  ]
};

/* ====================================================== atlas y pintura */

/**
 * Coloca cada cubo en el atlas (UV de caja: 2*(fondo+ancho) x (fondo+alto)). Con
 * `compartir`, los cubos de las mismas medidas y material usan el mismo sitio.
 */
/* ============================================================ Anfitrion */

// El Anfitrion de la Fiesta: un disfraz de mascota de fiesta infantil demasiado alto y
// demasiado flaco. Cabezota redonda de fieltro (tres cajas cruzadas) con la sonrisa cosida, ojos
// de boton y nariz roja; gorrito de fiesta ladeado; mono de lunares con pajarita; brazos largos
// con guantes blancos; piernas de rayas y zapatones. Mide unos 2,8 bloques con el gorro.
const brazoAnf = (lado) => [`brazo_${lado}`, 'cadera', [lado === 'izq' ? 5.8 : -5.8, -10, 0], [0, 0, lado === 'izq' ? -4 : 4], [
  [-1.25, 0, -1.25, 2.5, 14, 2.5, 'disfraz'],
  [-1.75, 14, -1.75, 3.5, 3.5, 3.5, 'guante'],
  [-2, 13, -2, 4, 1, 4, 'guante']
]];
const piernaAnf = (lado) => [`pierna_${lado}`, 'cadera', [lado === 'izq' ? 2.2 : -2.2, 0, 0], [0, 0, 0], [
  [-1.25, 0, -1.25, 2.5, 10, 2.5, 'pantalon'],
  [-2, 10, -5, 4, 3, 7, 'zapato']
]];
const ANFITRION = {
  nombre: 'anfitrion',
  tex: [128, 128],
  vista: { esc: 7.5 },
  piezas: [
    ['cadera', null, [0, 11, 0], [0, 0, 0], [
      [-4.5, -12, -2.5, 9, 12, 5, 'disfraz'],
      [-4, -9, -3.2, 8, 7, 1, 'disfraz'],
      [-2.5, -12, -3.4, 5, 2.5, 1, 'pajarita'],
      [-1, -12.25, -3.6, 2, 3, 1, 'nudo'],
      [-1.5, -13, -1.5, 3, 1, 3, 'cuello']
    ]],
    ['cabeza', 'cadera', [0, -12, 0], [0, 0, 0], [
      [-6, -12, -6, 12, 12, 12, 'fieltro'],
      [-5, -13, -5, 10, 14, 10, 'fieltro'],
      [-6.5, -10, -5, 13, 8, 10, 'fieltro'],
      [-1.5, -6, -7.5, 3, 3, 1.5, 'nariz']
    ]],
    ['gorro', 'cabeza', [1.5, -12, -0.5], [0, 0, 14], [
      [-2.5, -3, -2.5, 5, 3, 5, 'gorro'],
      [-1.5, -6, -1.5, 3, 3, 3, 'gorro'],
      [-0.5, -8.5, -0.5, 1, 2.5, 1, 'gorro'],
      [-1, -10, -1, 2, 1.5, 2, 'pompon']
    ]],
    brazoAnf('izq'), brazoAnf('der'),
    piernaAnf('izq'), piernaAnf('der')
  ]
};

function empaquetar (modelo) {
  const [W, H] = modelo.tex;
  let x = 0; let y = 0; let fila = 0;
  const hechos = new Map();
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      const w = Math.ceil(c[3]); const h = Math.ceil(c[4]); const d = Math.ceil(c[5]);
      const clave = `${w}x${h}x${d}:${c[6]}`;
      if (modelo.compartir && hechos.has(clave)) {
        c.uv = hechos.get(clave);
        c.repetido = true;
        continue;
      }
      const ancho = 2 * (d + w);
      const alto = d + h;
      if (x + ancho > W) { x = 0; y += fila; fila = 0; }
      if (y + alto > H) throw new Error(`${modelo.nombre}: no cabe en ${W}x${H}`);
      c.uv = [x, y];
      hechos.set(clave, c.uv);
      x += ancho;
      fila = Math.max(fila, alto);
    }
  }
}

/** Rectangulos de cada cara de un cubo en el atlas (como ModelPart.Cube). */
function carasUV (c) {
  const [u, v] = c.uv;
  const w = Math.ceil(c[3]); const h = Math.ceil(c[4]); const d = Math.ceil(c[5]);
  return {
    up: [u + d, v, u + d + w, v + d],
    down: [u + d + w, v, u + d + w + w, v + d],
    east: [u, v + d, u + d, v + d + h],
    north: [u + d, v + d, u + d + w, v + d + h],
    west: [u + d + w, v + d, u + d + w + d, v + d + h],
    south: [u + d + w + d, v + d, u + d + w + d + w, v + d + h]
  };
}

const MATERIAL = {
  // hilos negros de Hay Bacillus con vetas grises a lo largo
  fibra: (x, y, k) => {
    const veta = ruido(x * 0.6, y * 3, 6, 400 + k, 32);
    const b = 14 + veta * 34 + hash(x, y, 401 + k) * 10;
    return [b, b * 0.98, b * 0.92];
  },
  hebra: (x, y, k) => { const b = 30 + hash(x, y, 410 + k) * 26; return [b, b, b * 0.94]; },
  garra: (x, y, k) => { const b = 46 + hash(x, y, 420 + k) * 30; return [b * 1.05, b, b * 0.9]; },
  cabeza: (x, y, k) => {
    const veta = ruido(x * 2, y * 0.8, 6, 430 + k, 32);
    const b = 18 + veta * 30 + hash(x, y, 431 + k) * 8;
    return [b, b * 0.97, b * 0.9];
  },
  dientes: (x, y, k) => ((x + k) % 2 === 0 ? [236, 228, 200] : (y % 2 ? [120, 110, 90] : [196, 186, 158])),
  boca: () => [70, 10, 12],
  sombra: (x, y, k) => { const b = 6 + hash(x, y, 440 + k) * 10; return [b, b, b + 2]; },
  cara: (x, y, k) => { const b = 8 + hash(x, y, 450 + k) * 8; return [b, b, b + 2]; },
  // la Serpiente: azulejo blanco con la junta azul, en piezas de 4 contadas desde el
  // borde de cada cara (como el bloque borde_piscina)
  teja: (x, y, k, lado, u0, v0) => {
    const px = x - u0;
    const py = y - v0;
    if (px % 4 === 0 || py % 4 === 0) return escala([52, 96, 190], 0.92 + hash(x, y, 460 + k) * 0.12);
    let c = escala([236, 241, 243], 0.96 + hash(Math.floor(px / 4), Math.floor(py / 4), 461 + k) * 0.05);
    if (px % 4 === 1 && py % 4 === 1) c = escala(c, 1.04);
    if (px % 4 === 3 || py % 4 === 3) c = escala(c, 0.93);
    return c;
  },
  teja_azul: (x, y, k, lado, u0, v0) => {
    const px = x - u0;
    const py = y - v0;
    if (px % 3 === 0 || py % 3 === 0) return [196, 214, 232];
    return escala([60, 112, 200], 0.92 + hash(Math.floor(px / 3), Math.floor(py / 3), 462 + k) * 0.12);
  },
  hueso: (x, y, k) => {
    const v = ruido(x * 0.7, y * 1.6, 6, 470 + k, 64);
    let c = escala([214, 206, 184], 0.82 + v * 0.22 + hash(x, y, 471 + k) * 0.06);
    if (hash(x, y, 472 + k) > 0.93) c = escala(c, 0.72);
    return c;
  },
  craneo: (x, y, k) => {
    const v = ruido(x * 1.1, y * 1.1, 8, 480 + k, 64);
    let c = escala([224, 218, 200], 0.86 + v * 0.18 + hash(x, y, 481 + k) * 0.05);
    if (Math.abs(ruido(x * 0.9, y * 0.5, 8, 482 + k, 64) - 0.5) < 0.015) c = escala(c, 0.72); // grietas
    return c;
  },
  colmillo: (x, y, k) => ((x + y + k) % 2 === 0 ? [238, 232, 212] : [58, 50, 42]),
  diente: (x, y, k, lado, u0, v0) => escala([236, 228, 204], 1 - Math.max(0, (y - v0) - 1) * 0.12),
  // el Anfitrion: lunares amarillos sobre morado gastado, fieltro beis, rayas, guantes y zapatos
  disfraz: (x, y, k) => {
    const lunar = ((Math.floor(x / 3) + Math.floor(y / 3)) % 2 === 0) && (x % 3 === 1) && (y % 3 === 1);
    const base = escala([104, 52, 132], 0.82 + ruido(x, y, 6, 500 + k, 128) * 0.2 + hash(x, y, 501 + k) * 0.06);
    return lunar ? escala([236, 202, 70], 0.9 + hash(x, y, 502) * 0.1) : base;
  },
  fieltro: (x, y, k) => {
    let c = escala([222, 196, 160], 0.86 + ruido(x, y, 5, 510 + k, 128) * 0.16 + hash(x, y, 511 + k) * 0.05);
    if (hash(x, y, 512 + k) > 0.97) c = escala(c, 0.8); // pelusas y manchas del fieltro viejo
    return c;
  },
  nariz: (x, y, k) => escala([200, 30, 36], 0.86 + hash(x, y, 520 + k) * 0.18),
  gorro: (x, y, k, lado, u0, v0) => ((Math.floor((y - v0 + x - u0) / 2)) % 2 === 0 ? [58, 132, 214] : [240, 206, 64]),
  pompon: (x, y, k) => escala([246, 242, 236], 0.9 + hash(x, y, 530 + k) * 0.1),
  pajarita: (x, y, k) => ((x + y) % 4 === 0 ? [240, 228, 210] : escala([196, 26, 40], 0.9 + hash(x, y, 540) * 0.1)),
  nudo: () => [150, 18, 30],
  cuello: () => [240, 230, 214],
  pantalon: (x, y, k, lado, u0, v0) => ((y - v0) % 4 < 2 ? escala([236, 232, 220], 0.86 + hash(x, y, 550) * 0.1) : escala([58, 132, 214], 0.85 + hash(x, y, 551) * 0.12)),
  guante: (x, y, k) => {
    let c = escala([244, 242, 236], 0.88 + hash(x, y, 560 + k) * 0.1);
    if (hash(x, y, 561 + k) > 0.9) c = escala(c, 0.78); // sucios de tanto agarrar
    return c;
  },
  zapato: (x, y, k) => escala([190, 34, 40], 0.82 + ruido(x, y, 4, 570 + k, 128) * 0.2),
  falange: (x, y, k, lado, u0, v0) => {
    const b = 150 - (y - v0) * 14 + hash(x, y, 490 + k) * 12;
    return [b, b * 0.94, b * 0.82];
  }
};

function pintar (modelo) {
  const [W, H] = modelo.tex;
  const l = new Lienzo(W, H);
  let k = 0;
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      if (c.repetido) continue;
      const mat = MATERIAL[c[6]];
      for (const [lado, [u0, v0, u1, v1]] of Object.entries(carasUV(c))) {
        for (let y = v0; y < v1; y++) {
          for (let x = u0; x < u1; x++) l.punto(x, y, mat(x, y, k, lado, u0, v0));
        }
      }
      k++;
    }
  }
  return l;
}

/**
 * La cara del craneo de la Serpiente (la cara north del primer cubo de "craneo" y la
 * del hocico): cuencas negras con los ojos cian (que brillan: tambien en `brillo`),
 * el agujero de la nariz y la sombra de las cejas.
 */
function caraSerpiente (modelo, l, brillo) {
  const craneo = modelo.piezas.find((p) => p[0] === 'craneo')[4];
  const [u0, v0] = carasUV(craneo[0]).north;
  const pon = (x, y, c) => l.punto(u0 + x, v0 + y, c);
  const luz = (x, y, c) => { l.punto(u0 + x, v0 + y, c); brillo.punto(u0 + x, v0 + y, c); };
  // la frente de 24x16: ceja en sombra, dos cuencas de 7x7 y la nariz entre ellas
  for (let x = 1; x < 23; x++) pon(x, 3, escala([224, 218, 200], 0.66));
  for (const x0 of [3, 14]) {
    for (let y = 4; y < 11; y++) {
      for (let x = x0; x < x0 + 7; x++) {
        const borde = y === 4 || y === 10 || x === x0 || x === x0 + 6;
        pon(x, y, borde ? [46, 50, 56] : [8, 12, 18]);
      }
    }
    // el ojo: 3x3 cian, blanco en el centro, con un pixel de halo arriba y abajo
    for (let y = 6; y < 9; y++) {
      for (let x = x0 + 2; x < x0 + 5; x++) luz(x, y, x === x0 + 3 && y === 7 ? [226, 255, 255] : [70, 226, 238]);
    }
    luz(x0 + 3, 5, [30, 120, 136]);
    luz(x0 + 3, 9, [30, 120, 136]);
  }
  // la nariz: un agujero en punta hacia arriba que baja hasta el hocico
  for (let y = 10; y < 16; y++) {
    const medio = y === 10 ? 1 : 2;
    for (let x = 12 - medio; x < 12 + medio; x++) pon(x, y, [16, 14, 14]);
  }
  const [s0, t0] = carasUV(craneo[2]).north;
  for (let x = 7; x < 11; x++) l.punto(s0 + x, t0, [16, 14, 14]);
  for (let x = 8; x < 10; x++) l.punto(s0 + x, t0 + 1, [16, 14, 14]);
}

/** La cara del Smiler: ojos y sonrisa sobre su frente (la cara north del cubo "cara"). */
function caraSmiler (modelo, l, brillo) {
  const c = modelo.piezas.find((p) => p[0] === 'cabeza')[4][0];
  const [u0, v0] = carasUV(c).north;
  const blanco = [250, 250, 244];
  const pon = (x, y, c) => { l.punto(u0 + x, v0 + y, c); brillo.punto(u0 + x, v0 + y, c); };
  // ojos: rendijas inclinadas hacia el centro, como si sonrieran tambien
  for (const [x0, dir] of [[2, 1], [9, -1]]) {
    for (let x = 0; x < 3; x++) {
      const y = 2 + (dir > 0 ? (x === 2 ? 1 : 0) : (x === 0 ? 1 : 0));
      pon(x0 + x, y, blanco);
      pon(x0 + x, y + 1, blanco);
    }
  }
  // la sonrisa: una media luna de lado a lado, dientes arriba y abajo
  for (let x = 0; x < 14; x++) {
    const c = Math.pow((x - 6.5) / 6.5, 2);
    const arriba = 6 + Math.round(2.6 * (1 - c));
    const grueso = x === 0 || x === 13 ? 1 : 3;
    for (let r = 0; r < grueso; r++) {
      const y = arriba + r - (x === 0 || x === 13 ? 0 : 0);
      let col = blanco;
      if (r === 1 && grueso === 3) col = [40, 6, 6];              // la raja entre los dientes
      else if (x % 2 === 1) col = [176, 170, 150];                 // separacion entre dientes
      pon(x, y, col);
    }
  }
}

/**
 * La cara del Anfitrion, en la frente de la cabeza (cara north del primer cubo, 12x12): dos
 * botones negros de ojos con su brillo (que en el apagon se ve: tambien en `brillo`), las
 * mejillas coloradas y la sonrisa enorme cosida con punto de cruz.
 */
function caraAnfitrion (modelo, l, brillo) {
  const c = modelo.piezas.find((p) => p[0] === 'cabeza')[4][0];
  const [u0, v0] = carasUV(c).north;
  const pon = (x, y, col) => l.punto(u0 + x, v0 + y, col);
  const luz = (x, y, col) => { pon(x, y, col); brillo.punto(u0 + x, v0 + y, col); };
  for (const x0 of [2, 7]) {
    for (let y = 3; y < 6; y++) for (let x = x0; x < x0 + 3; x++) pon(x, y, [14, 12, 14]);
    luz(x0 + 1, 4, [230, 230, 220]);
    pon(x0 + 1, 2, escala([222, 196, 160], 0.7)); // la ceja de hilo
  }
  pon(1, 7, [230, 120, 120]); pon(10, 7, [230, 120, 120]);
  // la sonrisa: de lado a lado, curvada hacia arriba, con la boca oscura y puntadas encima
  for (let x = 1; x < 11; x++) {
    const k = Math.pow((x - 5.5) / 4.5, 2);
    const y = 9 - Math.round(1.6 * k);
    pon(x, y, [52, 10, 14]);
    pon(x, y + 1, [52, 10, 14]);
    if (x % 2 === 0) { pon(x, y - 1, [30, 24, 20]); pon(x, y + 2, [30, 24, 20]); }
  }
}

/* ================================================================= Java */

function java (modelo) {
  const clase = 'Malla' + modelo.nombre[0].toUpperCase() + modelo.nombre.slice(1);
  const f = (n) => `${Math.round(n * 1000) / 1000}F`;
  const lineas = [];
  for (const [nombre, padre, piv, rot, cubos] of modelo.piezas) {
    const lista = cubos.map((c) => `.texOffs(${c.uv[0]}, ${c.uv[1]}).addBox(${f(c[0])}, ${f(c[1])}, ${f(c[2])}, ${f(c[3])}, ${f(c[4])}, ${f(c[5])}${c[7] ? `, new CubeDeformation(${f(c[7])})` : ''})`).join('\n\t\t\t\t');
    const pose = `PartPose.offsetAndRotation(${f(piv[0])}, ${f(piv[1])}, ${f(piv[2])}, ${f(g(rot[0]))}, ${f(g(rot[1]))}, ${f(g(rot[2]))})`;
    lineas.push(`\t\tPartDefinition ${nombre} = ${padre || 'raiz'}.addOrReplaceChild("${nombre}", CubeListBuilder.create()\n\t\t\t\t${lista},\n\t\t\t${pose});`);
  }
  const fuente = `package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de ${modelo.nombre}. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class ${clase} {
	private ${clase}() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
${lineas.join('\n')}
		return LayerDefinition.create(malla, ${modelo.tex[0]}, ${modelo.tex[1]});
	}
}
`;
  fs.mkdirSync(JAVA, { recursive: true });
  fs.writeFileSync(path.join(JAVA, `${clase}.java`), fuente);
  console.log(`  ${clase}.java`);
}

/* ========================================================= vista previa */

function rotZYX (v, r) {
  let [x, y, z] = v;
  let c = Math.cos(r[0]); let s = Math.sin(r[0]);
  [y, z] = [y * c - z * s, y * s + z * c];
  c = Math.cos(r[1]); s = Math.sin(r[1]);
  [x, z] = [x * c + z * s, -x * s + z * c];
  c = Math.cos(r[2]); s = Math.sin(r[2]);
  [x, y] = [x * c - y * s, x * s + y * c];
  return [x, y, z];
}

function vista (modelo, tex, archivo, poses = {}) {
  const porNombre = Object.fromEntries(modelo.piezas.map((p) => [p[0], p]));
  const mundo = (nombre, v) => {
    // de la pieza a sus padres: rotar y luego mover al pivote
    let p = porNombre[nombre];
    let q = v;
    while (p) {
      const extra = poses[p[0]] || [0, 0, 0];
      q = rotZYX(q, p[3].map((a, i) => g(a + extra[i])));
      q = [q[0] + p[2][0], q[1] + p[2][1], q[2] + p[2][2]];
      p = p[1] ? porNombre[p[1]] : null;
    }
    return q;
  };
  const caras = [];
  const ESQ = {
    north: [[0, 0, 0], [1, 0, 0], [1, 1, 0], [0, 1, 0]],
    south: [[1, 0, 1], [0, 0, 1], [0, 1, 1], [1, 1, 1]],
    east: [[0, 0, 1], [0, 0, 0], [0, 1, 0], [0, 1, 1]],
    west: [[1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]],
    up: [[0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]],
    down: [[0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]]
  };
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      const uvs = carasUV(c);
      for (const [lado, esq] of Object.entries(ESQ)) {
        const pts = esq.map((e) => mundo(p[0], [c[0] + e[0] * c[3], c[1] + e[1] * c[4], c[2] + e[2] * c[5]]));
        const [u0, v0, u1, v1] = uvs[lado];
        caras.push({ pts, uv: [[u0, v0], [u1, v0], [u1, v1], [u0, v1]] });
      }
    }
  }
  const vis = modelo.vista || {};
  const W1 = vis.ancho || 360; const H = vis.alto || 520; const giros = [0, 90, -38];
  const cx = W1 * (vis.centro || 0.5);
  const out = { width: W1 * 3, height: H, data: new Uint8Array(W1 * 3 * H * 4) };
  for (let i = 0; i < W1 * 3 * H; i++) { const y = Math.floor(i / (W1 * 3)); out.data.set(y > H - 60 ? [150, 128, 70, 255] : [196, 180, 112, 255], i * 4); }
  const esc = vis.esc || 6.2;
  giros.forEach((giro, vi) => {
    const zb = new Float32Array(W1 * H).fill(Infinity);
    for (const c of caras) {
      // y del modelo hacia abajo y 24 = suelo; la camara mira la cara (desde -z)
      const P = c.pts.map((p) => { const q = rotZYX(p, [g(-8), g(giro), 0]); return [cx - q[0] * esc, H - 60 - (24 - q[1]) * esc, q[2]]; });
      const a = P[1].map((v, k) => v - P[0][k]); const b = P[3].map((v, k) => v - P[0][k]);
      const nz = a[0] * b[1] - a[1] * b[0];
      const luz = 0.6 + 0.4 * Math.min(1, Math.abs(nz) / (Math.hypot(a[0], a[1]) * Math.hypot(b[0], b[1]) + 1e-6));
      for (const tri of [[0, 1, 2], [0, 2, 3]]) {
        const [A, B, C] = tri.map((i) => P[i]);
        const [ua, ub, uc] = tri.map((i) => c.uv[i]);
        const den = (B[1] - C[1]) * (A[0] - C[0]) + (C[0] - B[0]) * (A[1] - C[1]);
        if (Math.abs(den) < 1e-6) continue;
        for (let y = Math.max(0, Math.floor(Math.min(A[1], B[1], C[1]))); y <= Math.min(H - 1, Math.ceil(Math.max(A[1], B[1], C[1]))); y++) {
          for (let x = Math.max(0, Math.floor(Math.min(A[0], B[0], C[0]))); x <= Math.min(W1 - 1, Math.ceil(Math.max(A[0], B[0], C[0]))); x++) {
            const l1 = ((B[1] - C[1]) * (x + 0.5 - C[0]) + (C[0] - B[0]) * (y + 0.5 - C[1])) / den;
            const l2 = ((C[1] - A[1]) * (x + 0.5 - C[0]) + (A[0] - C[0]) * (y + 0.5 - C[1])) / den;
            const l3 = 1 - l1 - l2;
            if (l1 < -1e-4 || l2 < -1e-4 || l3 < -1e-4) continue;
            const z = l1 * A[2] + l2 * B[2] + l3 * C[2];
            if (z >= zb[y * W1 + x]) continue;
            const u = Math.min(tex.width - 1, Math.floor(l1 * ua[0] + l2 * ub[0] + l3 * uc[0]));
            const v = Math.min(tex.height - 1, Math.floor(l1 * ua[1] + l2 * ub[1] + l3 * uc[1]));
            const ti = (v * tex.width + u) * 4;
            if (tex.data[ti + 3] < 10) continue;
            zb[y * W1 + x] = z;
            const o = (y * W1 * 3 + vi * W1 + x) * 4;
            for (let k = 0; k < 3; k++) out.data[o + k] = Math.min(255, tex.data[ti + k] * luz);
          }
        }
      }
    }
  });
  fs.writeFileSync(archivo, png.escribir(out));
  console.log(`  vista: ${archivo}`);
}

/* =============================================================== todo */

console.log('Entidades');
for (const modelo of [BACTERIA, SMILER, SERPIENTE, ANFITRION]) {
  empaquetar(modelo);
  const tex = pintar(modelo);
  if (modelo === SMILER || modelo === SERPIENTE || modelo === ANFITRION) {
    const brillo = new Lienzo(...modelo.tex);
    ({ smiler: caraSmiler, serpiente: caraSerpiente, anfitrion: caraAnfitrion })[modelo.nombre](modelo, tex, brillo);
    guardar(path.join(ASSETS, 'textures', 'entity'), `${modelo.nombre}_brillo`, brillo);
  }
  guardar(path.join(ASSETS, 'textures', 'entity'), modelo.nombre, tex);
  java(modelo);
  if (VISTAS) vista(modelo, tex, path.join(VISTAS, `${modelo.nombre}_vista.png`));
}
