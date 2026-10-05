'use strict';
/**
 * Decoracion del Nivel 0 para el mod evento/: texturas dibujadas por codigo y
 * los JSON de cada bloque (estados, modelos, objetos y nombres).
 *
 *   npm run texturas      (genera tambien las de generar.js)
 *
 * Bloques: enchufes, dibujos y cintas en la pared, senal de salida, ventiladores
 * de techo, senales de suelo (y "al reves"), notas, sillas de oficina hundidas
 * en la moqueta, Hay Bacillus y pared fina.
 */

const fs = require('fs');
const path = require('path');
const { hash, ruido, fbm, escala, mezcla, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const RES = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources');
const ASSETS = path.join(RES, 'assets', NS);
const TEX = path.join(ASSETS, 'textures', 'block');

const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};
const t = (n) => `${NS}:block/${n}`;
const FACINGS = { north: 0, east: 90, south: 180, west: 270 };

/* =============================================================== texturas */

console.log('Decoracion del Nivel 0');

// ---- enchufes: placa blanca con dos tomas
function enchufe (manchado) {
  const l = new Lienzo(16);
  const placa = manchado ? [205, 190, 140] : [232, 226, 207];
  l.pintar((x, y) => {
    let c = escala(placa, 0.95 + hash(x, y, 3) * 0.08);
    if (x === 0 || y === 0) c = escala(placa, 1.05);
    if (x === 15 || y === 15) c = escala(placa, 0.82);
    if (manchado) c = mezcla(c, [120, 98, 52], Math.max(0, fbm(x * 2, y * 2, 5) - 0.45) * 1.4);
    return c;
  });
  for (const oy of [2, 9]) {
    l.rect(5, oy, 10, oy + 4, escala(placa, 0.88));
    l.rect(6, oy + 1, 6, oy + 2, [40, 36, 30]);
    l.rect(9, oy + 1, 9, oy + 2, [40, 36, 30]);
    l.rect(7, oy + 3, 8, oy + 3, [55, 50, 42]);
  }
  return l;
}
guardar(TEX, 'enchufe', enchufe(false));
guardar(TEX, 'enchufe_manchado', enchufe(true));

// ---- dibujos de la pared (rotulador, tinta, cinta)
const ROTULADOR = [24, 20, 16];
const DIBUJOS = [];
function dibujo (nombre, fn) {
  const l = new Lienzo(32);
  fn(l);
  DIBUJOS.push(nombre);
  guardar(TEX, `dibujo_${DIBUJOS.length - 1}`, l);
}
dibujo('sonrisa', (l) => {
  l.linea(10, 9, 10, 13, ROTULADOR, 2, 0.9, 0.6);
  l.linea(21, 9, 21, 13, ROTULADOR, 2, 0.9, 0.6);
  for (let i = 0; i <= 20; i++) {
    const a = Math.PI * (0.08 + 0.84 * i / 20);
    const x = 16 - Math.cos(a) * 11;
    const y = 16 + Math.sin(a) * 7;
    l.disco(x, y, 1, ROTULADOR, 0.9);
    if (i % 3 === 0 && i > 0 && i < 20) l.linea(x, y, x, y - 2.5, ROTULADOR, 1, 0.8);
  }
});
const flecha = (dir) => (l) => {
  const p = { der: [[6, 16], [25, 16], [19, 10], [25, 16], [19, 22]], izq: [[26, 16], [7, 16], [13, 10], [7, 16], [13, 22]], arriba: [[16, 27], [16, 6], [10, 12], [16, 6], [22, 12]] }[dir];
  l.linea(p[0][0], p[0][1], p[1][0], p[1][1], ROTULADOR, 2.4, 0.9, 0.8);
  l.linea(p[2][0], p[2][1], p[3][0], p[3][1], ROTULADOR, 2.4, 0.9, 0.6);
  l.linea(p[3][0], p[3][1], p[4][0], p[4][1], ROTULADOR, 2.4, 0.9, 0.6);
};
dibujo('flecha_der', flecha('der'));
dibujo('flecha_izq', flecha('izq'));
dibujo('flecha_arriba', flecha('arriba'));
const palabra = (s, color = ROTULADOR) => (l) => {
  l.texto(s, 'centro', 12, color, 1, 0.92);
  // goterones bajo las letras
  for (let i = 0; i < 4; i++) {
    const x = 4 + Math.floor(hash(i, s.length, 3) * 24);
    l.linea(x, 19, x, 20 + hash(i, 1, 5) * 7, color, 1, 0.7);
  }
};
dibujo('corre', palabra('CORRE'));
dibujo('no', (l) => { l.texto('NO', 'centro', 9, ROTULADOR, 2, 0.92); });
dibujo('huye', palabra('HUYE', [92, 22, 18]));
dibujo('aqui', palabra('AQUI'));
dibujo('interrogacion', (l) => { l.texto('?', 'centro', 6, ROTULADOR, 3, 0.9); });
dibujo('mano', (l) => {
  const sangre = [92, 26, 18];
  l.disco(16, 20, 5.5, sangre, 0.85);
  const dedos = [[9, 13, -0.6], [12.5, 9, -0.2], [16.5, 8, 0], [20.5, 9.5, 0.2], [22.5, 16, 0.9]];
  for (const [x, y, inc] of dedos) l.linea(16 + (x - 16) * 0.6, 17, x + inc * 2, y, sangre, 2.6, 0.85, 0.4);
  l.linea(16, 24, 15, 30, sangre, 1.4, 0.6);
});
dibujo('conteo', (l) => {
  for (let g = 0; g < 2; g++) {
    const x0 = 5 + g * 12;
    for (let i = 0; i < 4; i++) l.linea(x0 + i * 2.5, 9, x0 + i * 2.5 + 0.5, 21, ROTULADOR, 1.2, 0.85, 0.5);
    l.linea(x0 - 1, 19, x0 + 9, 11, ROTULADOR, 1.2, 0.85, 0.4);
  }
  l.linea(27, 10, 27.5, 21, ROTULADOR, 1.2, 0.85, 0.5);
});
dibujo('ojo', (l) => {
  for (let i = 0; i <= 24; i++) {
    const a = Math.PI * (i / 24);
    l.disco(16 - Math.cos(a) * 11, 16 - Math.sin(a) * 6, 0.9, ROTULADOR, 0.9);
    l.disco(16 - Math.cos(a) * 11, 16 + Math.sin(a) * 6, 0.9, ROTULADOR, 0.9);
  }
  l.disco(16, 16, 3.6, ROTULADOR, 0.9);
  l.disco(15, 15, 0.8, [230, 225, 210], 0.9);
});
dibujo('cinta_x', (l) => {
  const cinta = (x0, y0, x1, y1) => {
    const n = 40;
    for (let i = 0; i <= n; i++) {
      const x = x0 + (x1 - x0) * i / n;
      const y = y0 + (y1 - y0) * i / n;
      l.disco(x, y, 2.6, escala([168, 166, 158], 0.9 + hash(i, x0, 4) * 0.15), 1);
    }
  };
  cinta(4, 4, 28, 28);
  cinta(28, 4, 4, 28);
});
dibujo('cinta_precaucion', (l) => {
  l.pintar((x, y) => {
    if (y < 12 || y > 19) return null;
    const franja = Math.floor((x + y) / 4) % 2 === 0;
    const c = franja ? [232, 196, 30] : [24, 22, 18];
    return [...escala(c, 0.92 + hash(x, y, 8) * 0.12), 1];
  });
});

// ---- salida (cartel verde "EXIT"), emisivo
{
  const l = new Lienzo(32);
  l.rect(0, 6, 31, 25, [52, 54, 50]);
  l.rect(1, 7, 30, 24, [26, 168, 84]);
  l.texto('EXIT', 'centro', 10, [236, 255, 240], 1);
  l.linea(10, 21, 22, 21, [236, 255, 240], 1);
  l.linea(22, 21, 19, 19, [236, 255, 240], 1);
  l.linea(22, 21, 19, 23, [236, 255, 240], 1);
  guardar(TEX, 'salida', l);
}

// ---- ventiladores: tiras de animacion de las aspas (vistas desde abajo)
function aspas (palas, largo, ancho, cuadros, giroTotal) {
  const marcos = [];
  for (let f = 0; f < cuadros; f++) {
    const l = new Lienzo(32);
    const base = (giroTotal * f) / cuadros;
    for (let p = 0; p < palas; p++) {
      const a = base + (2 * Math.PI * p) / palas;
      const ux = Math.cos(a);
      const uy = Math.sin(a);
      // pala: rectangulo redondeado desde el centro
      const pts = [];
      for (const [s, w] of [[3, ancho * 0.6], [largo, ancho], [largo + 1, ancho * 0.6]]) {
        pts.push([16 + ux * s - uy * w, 16 + uy * s + ux * w]);
      }
      for (const [s, w] of [[largo + 1, -ancho * 0.6], [largo, -ancho], [3, -ancho * 0.6]]) {
        pts.push([16 + ux * s - uy * w, 16 + uy * s + ux * w]);
      }
      l.poligono(pts, [196, 192, 182]);
    }
    l.disco(16, 16, 3.2, [92, 90, 86]);
    l.disco(16, 16, 1.6, [60, 58, 54]);
    marcos.push(l);
  }
  return Lienzo.tira(marcos);
}
const MCMETA = (frametime) => ({ animation: { frametime, interpolate: false } });
guardar(TEX, 'ventilador_aspas', aspas(4, 14, 2.8, 8, Math.PI / 2));
fs.writeFileSync(path.join(TEX, 'ventilador_aspas.png.mcmeta'), JSON.stringify(MCMETA(2), null, 2) + '\n');
guardar(TEX, 'ventilador_grande_aspas', aspas(3, 15, 1.3, 8, (2 * Math.PI) / 3));
fs.writeFileSync(path.join(TEX, 'ventilador_grande_aspas.png.mcmeta'), JSON.stringify(MCMETA(2), null, 2) + '\n');
guardar(TEX, 'metal', new Lienzo(16).pintar((x, y) => escala([128, 126, 120], 0.9 + hash(x, y, 11) * 0.15 + (y % 4 === 0 ? 0.05 : 0))));

// ---- senales de suelo (con su version "al reves")
function senal (tipo) {
  const l = new Lienzo(32);
  if (tipo === 'alto') {
    const pts = [];
    for (let i = 0; i < 8; i++) {
      const a = Math.PI / 8 + (i * Math.PI) / 4;
      pts.push([16 + Math.cos(a) * 15, 16 + Math.sin(a) * 15]);
    }
    l.poligono(pts, [236, 236, 230]);
    l.poligono(pts.map(([x, y]) => [16 + (x - 16) * 0.86, 16 + (y - 16) * 0.86]), [196, 34, 30]);
    l.texto('ALTO', 'centro', 13, [244, 242, 236], 1);
  } else if (tipo === 'peligro') {
    l.poligono([[16, 2], [31, 29], [1, 29]], [26, 24, 20]);
    l.poligono([[16, 6], [27.5, 27], [4.5, 27]], [244, 200, 30]);
    l.rect(15, 11, 16, 20, [26, 24, 20]);
    l.rect(15, 23, 16, 24, [26, 24, 20]);
  } else {
    l.disco(15.5, 15.5, 15, [236, 236, 230]);
    l.disco(15.5, 15.5, 13, [36, 156, 70]);
    l.texto('SIGA', 'centro', 13, [244, 242, 236], 1);
  }
  return l;
}
for (const s of ['alto', 'peligro', 'siga']) {
  const l = senal(s);
  guardar(TEX, `senal_${s}`, l);
  if (s !== 'peligro') guardar(TEX, `senal_${s}_reves`, l.espejo());
}
guardar(TEX, 'senal_trasera', new Lienzo(32).pintar((x, y) => [...escala([150, 152, 150], 0.9 + hash(x, y, 13) * 0.12), 1]));

// ---- nota en el suelo: papel rayado con letra a mano
{
  const l = new Lienzo(32);
  l.pintar((x, y) => {
    let c = escala([236, 230, 208], 0.95 + fbm(x, y, 17) * 0.08);
    if (y % 4 === 3) c = mezcla(c, [120, 150, 200], 0.35);
    if (x === 5) c = mezcla(c, [200, 90, 80], 0.4);
    return c;
  });
  for (let fila = 0; fila < 6; fila++) {
    const y = 6 + fila * 4;
    let x = 7;
    while (x < 28) {
      const largo = 2 + Math.floor(hash(fila, x, 19) * 5);
      l.linea(x, y, Math.min(28, x + largo), y - 0.5 + hash(x, fila, 21), [40, 44, 70], 0.8, 0.85, 0.6);
      x += largo + 2;
    }
  }
  guardar(TEX, 'nota', l);
}

// ---- silla de oficina
guardar(TEX, 'silla_tela', new Lienzo(16).pintar((x, y) => escala([46, 52, 70], 0.85 + hash(x, y, 23) * 0.25)));
guardar(TEX, 'silla_plastico', new Lienzo(16).pintar((x, y) => escala([34, 34, 36], 0.9 + hash(x, y, 25) * 0.15)));

// ---- Hay Bacillus: masa negra organica con venas
const BACILO = [18, 20, 18];
guardar(TEX, 'bacilo', new Lienzo(32).pintar((x, y) => {
  const vena = Math.abs(ruido(x, y, 4, 31) - 0.5) < 0.04 ? 1 : 0;
  let c = escala(BACILO, 0.7 + fbm(x, y, 33) * 0.8);
  if (vena) c = mezcla(c, [38, 58, 46], 0.7);
  return c;
}));
guardar(TEX, 'raiz_bacilo', new Lienzo(32).pintar((x, y) => {
  const raiz = Math.abs(ruido(x * 0.5, y, 4, 35) - 0.5) < 0.06;
  let c = escala(BACILO, 0.6 + fbm(x, y, 37) * 0.7);
  if (raiz) c = escala([58, 50, 40], 0.9);
  return c;
}));
guardar(TEX, 'capa_bacilo', new Lienzo(32).pintar((x, y) => {
  const m = fbm(x, y, 39);
  if (m < 0.6) return null;
  return [...escala(BACILO, 0.7 + hash(x, y, 41) * 0.5), Math.min(1, (m - 0.6) * 6)];
}));

/* ============================================================ modelos JSON */

const modelos = {};
const caras = (tex, extra = {}) => {
  const o = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) o[d] = { texture: tex, ...extra };
  return o;
};

// en la pared: el modelo mira al norte y va pegado a la pared del sur (z = 16)
modelos.enchufe = { parent: 'minecraft:block/block', textures: { all: t('enchufe'), particle: t('enchufe') }, elements: [{ from: [5, 2, 15.5], to: [11, 11, 16], faces: caras('#all') }] };
modelos.enchufe_manchado = { ...modelos.enchufe, textures: { all: t('enchufe_manchado'), particle: t('enchufe_manchado') } };
DIBUJOS.forEach((n, i) => {
  modelos[`dibujo_${i}`] = { parent: 'minecraft:block/block', textures: { all: t(`dibujo_${i}`), particle: t(`dibujo_${i}`) }, elements: [{ from: [0, 0, 15.9], to: [16, 16, 15.9], faces: { north: { texture: '#all', uv: [0, 0, 16, 16] } } }] };
});
modelos.salida = {
  parent: 'minecraft:block/block',
  textures: { all: t('salida'), back: t('metal'), particle: t('salida') },
  elements: [{
    from: [1, 9, 14], to: [15, 16, 16], shade: false, light_emission: 15,
    faces: { north: { texture: '#all', uv: [0, 3, 16, 12.5] }, south: { texture: '#back' }, east: { texture: '#back' }, west: { texture: '#back' }, up: { texture: '#back' }, down: { texture: '#back' } }
  }]
};

// ventiladores colgados del techo (el bloque es el aire de debajo del techo)
const ventilador = (aspas, radio) => ({
  parent: 'minecraft:block/block',
  textures: { aspas: t(aspas), metal: t('metal'), particle: t('metal') },
  elements: [
    { from: [7.5, 11, 7.5], to: [8.5, 16, 8.5], faces: caras('#metal') },
    { from: [6, 9.5, 6], to: [10, 11, 10], faces: caras('#metal') },
    { from: [8 - radio, 10, 8 - radio], to: [8 + radio, 10, 8 + radio], faces: { up: { texture: '#aspas', uv: [0, 0, 16, 16] }, down: { texture: '#aspas', uv: [0, 0, 16, 16] } } }
  ]
});
modelos.ventilador = ventilador('ventilador_aspas', 12);
modelos.ventilador_grande = ventilador('ventilador_grande_aspas', 20);

// senales: poste y placa (frente al norte)
const senalModelo = (frente) => ({
  parent: 'minecraft:block/block',
  textures: { frente: t(frente), trasera: t('senal_trasera'), poste: t('metal'), particle: t(frente) },
  elements: [
    { from: [7.5, 0, 8.5], to: [8.5, 16, 9.5], faces: caras('#poste') },
    { from: [2, 14, 8], to: [14, 26, 8.5], faces: { north: { texture: '#frente', uv: [0, 0, 16, 16] }, south: { texture: '#trasera', uv: [0, 0, 16, 16] } } }
  ]
});
for (const s of ['alto', 'peligro', 'siga', 'alto_reves', 'siga_reves']) modelos[`senal_${s}`] = senalModelo(`senal_${s}`);

modelos.nota = { parent: 'minecraft:block/block', textures: { all: t('nota'), particle: t('nota') }, elements: [{ from: [4, 0, 3], to: [12, 0.25, 13], rotation: { angle: 22.5, axis: 'y', origin: [8, 0, 8] }, faces: { up: { texture: '#all', uv: [0, 0, 16, 16] }, down: { texture: '#all', uv: [0, 0, 16, 16] } } }] };

// silla de oficina hundida en la moqueta: la mitad de abajo queda bajo el suelo
const sillaPiezas = (dy) => [
  { from: [3, 0 + dy, 7.25], to: [13, 1 + dy, 8.75], faces: caras('#plastico') },
  { from: [7.25, 0 + dy, 3], to: [8.75, 1 + dy, 13], faces: caras('#plastico') },
  { from: [7.5, 1 + dy, 7.5], to: [8.5, 6 + dy, 8.5], faces: caras('#plastico') },
  { from: [3, 6 + dy, 3], to: [13, 8 + dy, 13], faces: caras('#tela') },
  { from: [3.5, 8 + dy, 12], to: [12.5, 18 + dy, 13.5], faces: caras('#tela') },
  { from: [7.25, 6 + dy, 13], to: [8.75, 11 + dy, 14], faces: caras('#plastico') }
];
const sillaTex = { tela: t('silla_tela'), plastico: t('silla_plastico'), particle: t('silla_tela') };
modelos.silla = { parent: 'minecraft:block/block', textures: sillaTex, elements: sillaPiezas(-5) };
modelos.silla_volcada = {
  parent: 'minecraft:block/block', textures: sillaTex,
  elements: sillaPiezas(-4).map((e) => ({ ...e, rotation: { angle: 45, axis: 'x', origin: [8, 0, 8] } }))
};

modelos.bacilo = { parent: 'minecraft:block/cube_all', textures: { all: t('bacilo') } };
modelos.raiz_bacilo = { parent: 'minecraft:block/cube_all', textures: { all: t('raiz_bacilo') } };
modelos.capa_bacilo = { parent: 'minecraft:block/carpet', textures: { wool: t('capa_bacilo') } };

for (const s of ['', '_sucia']) {
  const pane = s ? t('papel_pintado_sucio') : t('papel_pintado');
  const edge = t('papel_pintado');
  modelos[`pared_fina${s}_post`] = { parent: 'minecraft:block/template_glass_pane_post', textures: { edge, pane, particle: pane } };
  modelos[`pared_fina${s}_side`] = { parent: 'minecraft:block/template_glass_pane_side', textures: { edge, pane, particle: pane } };
  modelos[`pared_fina${s}_side_alt`] = { parent: 'minecraft:block/template_glass_pane_side_alt', textures: { edge, pane, particle: pane } };
  modelos[`pared_fina${s}_noside`] = { parent: 'minecraft:block/template_glass_pane_noside', textures: { pane, particle: pane } };
  modelos[`pared_fina${s}_noside_alt`] = { parent: 'minecraft:block/template_glass_pane_noside_alt', textures: { pane, particle: pane } };
}

for (const [n, m] of Object.entries(modelos)) json(`models/block/${n}.json`, m);

/* ===================================================== estados y objetos */

const m = (n) => `${NS}:block/${n}`;
const conFacing = (modelo) => {
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) v[`facing=${f}`] = y ? { model: m(modelo), y } : { model: m(modelo) };
  return { variants: v };
};
const estados = {
  enchufe: conFacing('enchufe'),
  enchufe_manchado: conFacing('enchufe_manchado'),
  salida: conFacing('salida'),
  ventilador: { variants: { '': { model: m('ventilador') } } },
  ventilador_grande: { variants: { '': { model: m('ventilador_grande') } } },
  nota: conFacing('nota'),
  bacilo: { variants: { '': { model: m('bacilo') } } },
  raiz_bacilo: { variants: { '': { model: m('raiz_bacilo') } } },
  capa_bacilo: { variants: { '': { model: m('capa_bacilo') } } }
};
for (const s of ['alto', 'peligro', 'siga', 'alto_reves', 'siga_reves']) estados[`senal_${s}`] = conFacing(`senal_${s}`);
{
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    for (const volcada of [false, true]) v[`facing=${f},volcada=${volcada}`] = { model: m(volcada ? 'silla_volcada' : 'silla'), ...(y ? { y } : {}) };
  }
  estados.silla = { variants: v };
}
{
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    DIBUJOS.forEach((n, i) => { v[`dibujo=${i},facing=${f}`] = { model: m(`dibujo_${i}`), ...(y ? { y } : {}) }; });
  }
  estados.dibujo = { variants: v };
}
for (const s of ['', '_sucia']) {
  const id = `pared_fina${s}`;
  const mm = (k, y) => ({ model: m(`${id}_${k}`), ...(y ? { y } : {}) });
  estados[id] = {
    multipart: [
      { apply: mm('post') },
      { apply: mm('side'), when: { north: 'true' } },
      { apply: mm('side', 90), when: { east: 'true' } },
      { apply: mm('side_alt'), when: { south: 'true' } },
      { apply: mm('side_alt', 90), when: { west: 'true' } },
      { apply: mm('noside'), when: { north: 'false' } },
      { apply: mm('noside_alt'), when: { east: 'false' } },
      { apply: mm('noside_alt', 90), when: { south: 'false' } },
      { apply: mm('noside', 270), when: { west: 'false' } }
    ]
  };
}
for (const [n, e] of Object.entries(estados)) json(`blockstates/${n}.json`, e);

// objetos: el icono del inventario usa el modelo del bloque (pared fina: el poste)
const objetoModelo = {
  dibujo: 'dibujo_0', silla: 'silla', pared_fina: 'pared_fina_side', pared_fina_sucia: 'pared_fina_sucia_side'
};
for (const n of Object.keys(estados)) json(`items/${n}.json`, { model: { type: 'minecraft:model', model: m(objetoModelo[n] || n) } });

// nombres
const NOMBRES = {
  enchufe: ['Enchufe', 'Outlet'],
  enchufe_manchado: ['Enchufe manchado', 'Stained outlet'],
  dibujo: ['Dibujo en la pared', 'Wall drawing'],
  salida: ['Cartel de salida', 'Exit sign'],
  ventilador: ['Ventilador de techo', 'Ceiling fan'],
  ventilador_grande: ['Ventilador de techo grande', 'Large ceiling fan'],
  nota: ['Nota', 'Note'],
  silla: ['Silla de oficina', 'Office chair'],
  bacilo: ['Hay Bacillus', 'Hay Bacillus'],
  raiz_bacilo: ['Raíz de Hay Bacillus', 'Hay Bacillus root'],
  capa_bacilo: ['Vena de Hay Bacillus', 'Hay Bacillus vein'],
  senal_alto: ['Señal de alto', 'Stop sign'],
  senal_peligro: ['Señal de peligro', 'Danger sign'],
  senal_siga: ['Señal de siga', 'Go sign'],
  senal_alto_reves: ['Señal de otla', 'Pots sign'],
  senal_siga_reves: ['Señal de agis', 'Og sign'],
  pared_fina: ['Pared fina', 'Thin wallpaper'],
  pared_fina_sucia: ['Pared fina manchada', 'Thin stained wallpaper']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, n] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = n[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}

// para el codigo Java: cuantos dibujos hay
fs.writeFileSync(path.join(__dirname, 'dibujos.json'), JSON.stringify(DIBUJOS) + '\n');
console.log(`  ${DIBUJOS.length} dibujos: ${DIBUJOS.join(', ')}`);
