'use strict';
/**
 * La Fiesta, segunda parte: los bloques del laberinto, el castillo de bolas, la sala central y el
 * salon final (evento/.../fiesta/BloquesFiesta.java). Texturas, modelos, estados, objetos y nombres;
 * todo dibujado por codigo. Va despues de fiesta.js (usa sus texturas de zocalo y acolchados).
 *
 *   node tools/texturas/laberinto.js   (va en npm run texturas)
 *
 * El laberinto es de muros blancos con zocalo granate, columnas de colores en las esquinas, suelo
 * de baldosas brillantes a cuadros y techo oscuro; las puertas, verdes de seis cuarterones. El
 * castillo: postes amarillos acolchados, redes negras y lona azul. La sala central: verde agua con
 * franja azul y amarilla y moqueta de carreteras. Todo un poco gastado.
 */

const fs = require('fs');
const path = require('path');
const { hash, fbm, mezcla, escala, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', NS);
const TEX = path.join(ASSETS, 'textures', 'block');
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};
const t = (n) => `${NS}:block/${n}`;
const FACINGS = { north: 0, east: 90, south: 180, west: 270 };
const mod = (textures, elements) => ({ parent: 'minecraft:block/block', textures, elements });
const cubo = (n) => json(`models/block/${n}.json`, { parent: 'minecraft:block/cube_all', textures: { all: t(n) } });
const simple = (n) => {
  cubo(n);
  json(`blockstates/${n}.json`, { variants: { '': { model: t(n) } } });
  json(`items/${n}.json`, { model: { type: 'minecraft:model', model: t(n) } });
};
/** Blockstate de un bloque con FACING y VARIANTE (modelo(v) da el modelo de cada variante). */
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
/** Plano pegado a la pared (como los rotulos): la textura en la cara de delante. */
const enPared = (tex, emision = 0) => mod({ f: t(tex), particle: t(tex) }, [{
  from: [0, 0, 15.9], to: [16, 16, 15.9], shade: emision === 0, ...(emision ? { light_emission: emision } : {}),
  faces: { north: { texture: '#f', uv: [0, 0, 16, 16] } }
}]);
const caras = (tex, extra = {}) => {
  const f = {};
  for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) f[d] = { texture: tex, ...extra };
  return f;
};

console.log('Laberinto, castillo y sala central');

const ROJO = [226, 56, 74];
const AMARILLO = [246, 206, 64];
const AZUL = [58, 150, 230];
const VERDE = [74, 196, 110];
const MORADO = [150, 80, 200];

/* ============================================================ el laberinto */

// muro blanco de yeso, liso, con algo de sucio abajo y alguna raya
guardar(TEX, 'pared_laberinto', new Lienzo(32).pintar((x, y) => {
  let c = escala([236, 234, 226], 0.95 + fbm(x, y, 401, 32) * 0.05 + (hash(x, y, 402) - 0.5) * 0.02);
  if (y > 22) c = mezcla(c, [196, 186, 160], (y - 22) / 10 * 0.25 * fbm(x, y, 403, 32));
  if (x === 15 && hash(0, y >> 3, 404) > 0.7) c = escala(c, 0.93);
  return c;
}));
simple('pared_laberinto');

// la parte de abajo del muro: el mismo yeso blanco y un rodapie granate brillante (un cuarto del bloque)
guardar(TEX, 'zocalo_laberinto', new Lienzo(32).pintar((x, y) => {
  if (y < 23) {
    let c = escala([236, 234, 226], 0.95 + fbm(x, y, 401, 32) * 0.05 + (hash(x, y, 402) - 0.5) * 0.02);
    return mezcla(c, [196, 186, 160], y / 23 * 0.12 * fbm(x, y, 403, 32));
  }
  if (y < 25) return [176, 70, 70];
  let c = escala([128, 28, 36], 0.92 + fbm(x, y, 411, 32) * 0.08);
  if (y === 25) c = [94, 18, 24];
  if (y > 29) c = escala(c, 0.8 + hash(x, y, 412) * 0.1);
  return c;
}));
simple('zocalo_laberinto');

// techo oscuro: placas grises con su rejilla negra
guardar(TEX, 'techo_laberinto', new Lienzo(32).pintar((x, y) => {
  let c = escala([62, 60, 64], 0.9 + fbm(x, y, 421, 32) * 0.15);
  if (x % 16 === 0 || y % 16 === 0) c = [26, 26, 28];
  return c;
}));
simple('techo_laberinto');

// columnas pintadas de colores: brillo vertical en el centro y el canto mas oscuro
for (const [n, col] of [['roja', ROJO], ['amarilla', AMARILLO], ['verde', VERDE], ['azul', [46, 62, 150]], ['morada', MORADO]]) {
  guardar(TEX, `columna_${n}`, new Lienzo(16).pintar((x, y) => {
    let c = escala(col, 0.86 + Math.sin((x + 0.5) / 16 * Math.PI) * 0.2 + (hash(x, y, 431) - 0.5) * 0.03);
    if (x === 0 || x === 15) c = escala(col, 0.7);
    return c;
  }));
  simple(`columna_${n}`);
}

/** Una baldosa brillante del damero (igual que en fiesta.js, el suelo defectuoso). */
function damero (col, x, y) {
  if (x === 0 || y === 0) return escala(col, 0.62);
  let c = escala(col, 0.95 + fbm(x, y, 181, 32) * 0.06 - (x + y) * 0.0018);
  const d = Math.hypot(x - 9, y - 8);
  if (d < 6) c = mezcla(c, [255, 255, 255], (6 - d) / 6 * 0.28);
  if (hash(x, y, 182) > 0.985) c = escala(c, 0.85);
  return c;
}
for (const [n, col] of [['blanco', [232, 232, 226]], ['azul', [52, 92, 196]], ['amarillo', [236, 196, 58]], ['rojo', [200, 40, 52]], ['morado', [104, 64, 176]]]) {
  guardar(TEX, `damero_${n}`, new Lienzo(32).pintar((x, y) => damero(col, x, y)));
  simple(`damero_${n}`);
}

// banderines: una cuerda de lado a lado con tres banderines de colores (4 juegos de colores)
const COLS = [ROJO, AMARILLO, AZUL, VERDE];
for (let v = 0; v < 4; v++) {
  guardar(TEX, `banderines_${v}`, new Lienzo(16).pintar((x, y) => {
    if (y === 1 + (x % 8 === 0 ? 1 : 0)) return [70, 60, 52];
    const k = Math.floor((x + 2) / 6);
    const lx = (x + 2) % 6;
    if (y > 1 && y < 9 && Math.abs(lx - 2.5) < (9 - y) * 0.36) return escala(COLS[(k + v) % 4], 0.95 + hash(x, y, 441) * 0.08);
    return null;
  }));
  json(`models/block/banderines_${v}.json`, mod({ b: t(`banderines_${v}`), particle: t(`banderines_${v}`) }, [{
    from: [0, 6, 8], to: [16, 16, 8], shade: false,
    faces: { north: { texture: '#b', uv: [0, 0, 16, 10] }, south: { texture: '#b', uv: [16, 0, 0, 10] } }
  }]));
}
estados('banderines', 4, (i) => `banderines_${i}`);

// la flecha de neon colgada del techo: placa oscura con la flecha morada que brilla, y dos cadenitas.
// La flecha apunta al norte con FACING norte (en la cara de abajo, la parte de arriba de la textura es el sur)
{
  const l = new Lienzo(16).pintar(() => [30, 24, 40]);
  l.poligono([[8, 14], [13, 8], [10, 8], [10, 2], [6, 2], [6, 8], [3, 8]], [236, 120, 255]);
  l.poligono([[8, 12], [11, 9], [9, 9], [9, 3], [7, 3], [7, 9], [5, 9]], [255, 220, 255]);
  guardar(TEX, 'flecha_techo', l);
  guardar(TEX, 'flecha_marco', new Lienzo(16).pintar((x, y) => escala([40, 34, 52], 0.9 + hash(x, y, 451) * 0.15)));
  json('models/block/flecha_techo.json', mod({ f: t('flecha_techo'), m: t('flecha_marco'), particle: t('flecha_techo') }, [
    { from: [1, 13, 1], to: [15, 14, 15], light_emission: 15, faces: { down: { texture: '#f' }, up: { texture: '#m' }, north: { texture: '#m' },
      south: { texture: '#m' }, east: { texture: '#m' }, west: { texture: '#m' } }, shade: false },
    { from: [3, 14, 7.5], to: [4, 16, 8.5], faces: caras('#m') },
    { from: [12, 14, 7.5], to: [13, 16, 8.5], faces: caras('#m') }
  ]));
  estados('flecha_techo', 4, () => 'flecha_techo');
}

// los rotulos que brillan: SALIDA (verde) y CERRAMOS (rojo), en una placa a media altura
for (const [i, texto, fondo, tinta] of [[0, 'SALIDA', [30, 150, 70], [236, 255, 236]], [1, 'CERRAMOS', [150, 26, 30], [255, 226, 200]]]) {
  const l = new Lienzo(64).pintar((x, y) => {
    if (y < 18 || y > 45) return null;
    if (y === 18 || y === 45 || x === 0 || x === 63) return escala(fondo, 0.6);
    return escala(fondo, 0.9 + hash(x, y, 461) * 0.08);
  });
  l.texto(texto, 'centro', 25, tinta, 1);
  guardar(TEX, `letrero_fiesta_${i}`, l);
  json(`models/block/letrero_fiesta_${i}.json`, enPared(`letrero_fiesta_${i}`, 12));
}
estados('letrero_fiesta', 8, (i) => `letrero_fiesta_${i % 2}`);

// la nube de la sala central (tres trozos de pared: izquierda, centro, derecha)
{
  const ancho = 48;
  const nube = (x, y) => {
    const d = Math.min(Math.hypot((x - 10) / 9, (y - 10) / 6), Math.hypot((x - 24) / 13, (y - 8) / 8), Math.hypot((x - 38) / 9, (y - 10) / 6));
    if (d > 1 || y > 14) return null;
    return escala([248, 248, 252], 0.86 + (1 - d) * 0.14 - (y > 11 ? 0.06 : 0));
  };
  for (let k = 0; k < 3; k++) {
    guardar(TEX, `nube_fiesta_${k}`, new Lienzo(16).pintar((x, y) => nube(x + k * 16, y)));
    json(`models/block/nube_fiesta_${k}.json`, enPared(`nube_fiesta_${k}`));
  }
  void ancho;
}
estados('nube_fiesta', 8, (i) => `nube_fiesta_${i % 3}`);

/* ============================================================ las puertas */

// la hoja de la puerta (16 x 32 en dos texturas de 16): marco, seis cuarterones, el pomo de laton
// y, en la de abajo de las verdes, nada; en la de arriba, la placa con lo que cuesta (monedas)
const PUERTAS = [['verde', [44, 120, 62]], ['roja', [168, 36, 40]], ['final', [92, 46, 130]]];
const hoja = (col, x, y, mitad) => {
  const yy = y + (mitad === 1 ? 0 : 16);
  if (x < 1 || x > 14) return escala([226, 224, 214], 0.9 + hash(x, yy, 471) * 0.08);
  let c = escala(col, 0.92 + fbm(x, yy, 472, 32) * 0.08);
  // cuarterones: dos columnas, tres filas (de arriba a abajo en 32)
  const filas = [[2, 9], [12, 19], [22, 30]];
  for (const [y0, y1] of filas) {
    for (const [x0, x1] of [[3, 7], [9, 13]]) {
      if (x >= x0 && x <= x1 && yy >= y0 && yy <= y1) {
        c = escala(col, (x === x0 || yy === y0) ? 0.7 : (x === x1 || yy === y1) ? 1.15 : 0.98);
      }
    }
  }
  return c;
};
PUERTAS.forEach(([n, col], tipo) => {
  for (const mitad of [0, 1]) {
    for (let coste = 0; coste <= 5; coste++) {
      if (mitad === 0 && coste > 0) continue;
      if (tipo > 0 && coste > 0) continue;
      const l = new Lienzo(16).pintar((x, y) => hoja(col, x, y, mitad));
      if (mitad === 0) {
        l.rect(12, 1, 13, 2, [224, 186, 80]);
        l.punto(12, 1, [255, 236, 150]);
      }
      if (mitad === 1 && tipo === 0 && coste > 0) {
        l.rect(3, 4, 12, 11, [30, 26, 34]);
        for (let k = 0; k < coste; k++) {
          const cx = 7.5 + (k - (coste - 1) / 2) * 2;
          l.punto(Math.round(cx), 6, [242, 197, 58]);
          l.punto(Math.round(cx), 7, [242, 197, 58]);
        }
        l.rect(5, 9, 10, 9, [242, 197, 58]);
      }
      if (mitad === 1 && tipo === 1) l.rect(4, 6, 11, 7, [240, 230, 210]);
      if (mitad === 1 && tipo === 2) {
        l.rect(3, 4, 12, 11, [30, 26, 34]);
        l.rect(5, 6, 10, 9, [226, 56, 74]);
        l.punto(4, 7, [30, 26, 34]);
        l.punto(11, 7, [30, 26, 34]);
      }
      const nombre = `puerta_${n}_${mitad === 0 ? 'abajo' : 'arriba'}${coste > 0 ? '_' + coste : ''}`;
      guardar(TEX, nombre, l);
      json(`models/block/${nombre}.json`, { parent: 'minecraft:block/block', textures: { f: t(nombre), l: t('pared_laberinto'), particle: t(nombre) },
        elements: [{ from: [0, 0, 0], to: [16, 16, 16], faces: {
          north: { texture: '#f', cullface: 'north' }, south: { texture: '#f', cullface: 'south' },
          east: { texture: '#l', cullface: 'east' }, west: { texture: '#l', cullface: 'west' }, up: { texture: '#l', cullface: 'up' }, down: { texture: '#l', cullface: 'down' } } }] });
    }
  }
});
{
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    for (let tipo = 0; tipo <= 2; tipo++) {
      for (let coste = 0; coste <= 5; coste++) {
        const n = PUERTAS[tipo][0];
        v[`coste=${coste},facing=${f},parte=0,tipo=${tipo}`] = { model: t(`puerta_${n}_abajo`), ...(y ? { y } : {}) };
        v[`coste=${coste},facing=${f},parte=1,tipo=${tipo}`] = { model: t(`puerta_${n}_arriba${tipo === 0 && coste > 0 ? '_' + coste : ''}`), ...(y ? { y } : {}) };
      }
    }
  }
  json('blockstates/puerta_fiesta.json', { variants: v });
  json('items/puerta_fiesta.json', { model: { type: 'minecraft:model', model: t('puerta_verde_arriba_1') } });
}

// el localizador de salida: placa blanca con un boton azul redondo, una flecha y "2" monedas
{
  const l = new Lienzo(32).pintar((x, y) => {
    if (x < 6 || x > 25 || y < 4 || y > 27) return null;
    if (x === 6 || x === 25 || y === 4 || y === 27) return [150, 150, 156];
    return escala([236, 236, 240], 0.95 + hash(x, y, 481) * 0.05);
  });
  l.disco(15.5, 13, 6, [20, 70, 160]);
  l.disco(15.5, 13, 4.6, [70, 150, 250]);
  l.disco(14, 11.5, 1.4, [190, 230, 255]);
  l.disco(13, 23, 1.6, [242, 197, 58]);
  l.disco(18, 23, 1.6, [242, 197, 58]);
  guardar(TEX, 'localizador_salida', l);
  json('models/block/localizador_salida.json', enPared('localizador_salida', 6));
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) v[`facing=${f}`] = { model: t('localizador_salida'), ...(y ? { y } : {}) };
  json('blockstates/localizador_salida.json', { variants: v });
  json('items/localizador_salida.json', { model: { type: 'minecraft:model', model: t('localizador_salida') } });
}

// el marcador de LEDs: panel negro con la rejilla de LEDs apagados (el texto lo pone el cliente)
for (let p = 0; p < 3; p++) {
  guardar(TEX, `marcador_fiesta_${p}`, new Lienzo(32).pintar((x, y) => {
    if (y < 6 || y > 25) return null;
    const borde = y === 6 || y === 25 || (p === 0 && x === 0) || (p === 2 && x === 31);
    if (borde) return [90, 90, 96];
    if (x % 2 === 0 && y % 2 === 1) return [52, 14, 16];
    return [10, 10, 12];
  }));
  json(`models/block/marcador_fiesta_${p}.json`, enPared(`marcador_fiesta_${p}`, 2));
}
{
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) {
    for (let p = 0; p < 3; p++) for (let tipo = 0; tipo < 2; tipo++) v[`facing=${f},parte=${p},tipo=${tipo}`] = { model: t(`marcador_fiesta_${p}`), ...(y ? { y } : {}) };
  }
  json('blockstates/marcador_fiesta.json', { variants: v });
  json('items/marcador_fiesta.json', { model: { type: 'minecraft:model', model: t('marcador_fiesta_1') } });
}

// el noclip: un cuadrado negro que no para de corromperse (animado)
{
  const cuadros = [];
  for (let k = 0; k < 8; k++) {
    cuadros.push(new Lienzo(16).pintar((x, y) => {
      const h = hash(x + k * 17, y + k * 31, 491);
      if (h > 0.96) return [190, 60, 255];
      if (h > 0.92) return [40, 255, 160];
      const franja = hash(0, y + k * 7, 492) > 0.85;
      return franja ? [30, 10, 40] : escala([8, 6, 12], 0.8 + hash(x, y, 493) * 0.4);
    }));
  }
  guardar(TEX, 'noclip', Lienzo.tira(cuadros));
  fs.writeFileSync(path.join(TEX, 'noclip.png.mcmeta'), JSON.stringify({ animation: { frametime: 2 } }, null, 2) + '\n');
  json('models/block/noclip.json', mod({ n: t('noclip'), particle: t('noclip') }, [
    { from: [0, 15.9, 0], to: [16, 15.9, 16], shade: false, light_emission: 4, faces: { up: { texture: '#n' }, down: { texture: '#n' } } }
  ]));
  json('blockstates/noclip.json', { variants: { '': { model: t('noclip') } } });
  json('items/noclip.json', { model: { type: 'minecraft:model', model: t('noclip') } });
}

/* ============================================================ el castillo de bolas */

// el poste acolchado amarillo: espuma con las costuras en espiral
guardar(TEX, 'poste_castillo', new Lienzo(16).pintar((x, y) => {
  let c = escala(AMARILLO, 0.88 + Math.sin((x + 0.5) / 16 * Math.PI) * 0.18);
  if ((x + y * 2) % 10 === 0) c = escala(AMARILLO, 0.62);
  return c;
}));
json('models/block/poste_castillo.json', mod({ p: t('poste_castillo'), particle: t('poste_castillo') }, [
  { from: [4, 0, 4], to: [12, 16, 12], faces: caras('#p') }
]));
estados('poste_castillo', 4, () => 'poste_castillo', true);

// la red negra: malla fina de cuadros (seis por bloque), con los nudos un poco mas gruesos
guardar(TEX, 'red_castillo', new Lienzo(48).pintar((x, y) => {
  if (x % 8 === 0 || y % 8 === 0) return escala([30, 30, 34], 0.9 + hash(x, y, 503) * 0.2);
  return null;
}));
json('models/block/red_castillo.json', mod({ r: t('red_castillo'), particle: t('red_castillo') }, [
  { from: [0, 0, 8], to: [16, 16, 8], faces: { north: { texture: '#r' }, south: { texture: '#r' } } }
]));
estados('red_castillo', 4, () => 'red_castillo');

// el semaforo pequeno: caja negra con la roja arriba y la verde abajo, sobre su trozo de red
{
  const caja = (luz) => new Lienzo(16).pintar((x, y) => {
    if (x < 4 || x > 11 || y < 1 || y > 14) return null;
    const lampara = (cy, col, on) => {
      const d = Math.hypot(x - 7.5, y - cy);
      if (d > 2.6) return null;
      return on ? mezcla([255, 255, 240], col, Math.min(1, d / 2)) : escala(col, 0.25);
    };
    return lampara(4.5, [255, 58, 46], luz === 1) || lampara(10.5, [72, 255, 110], luz === 2) || escala([24, 24, 28], 0.9 + hash(x, y, 501) * 0.15);
  });
  guardar(TEX, 'semaforo_castillo_carcasa', new Lienzo(16).pintar((x, y) => escala([24, 24, 28], 0.9 + hash(x, y, 502) * 0.15)));
  for (let luz = 0; luz <= 2; luz++) {
    guardar(TEX, `semaforo_castillo_${luz}`, caja(luz));
    json(`models/block/semaforo_castillo_${luz}.json`, mod({ f: t(`semaforo_castillo_${luz}`), c: t('semaforo_castillo_carcasa'), r: t('red_castillo'),
      particle: t('semaforo_castillo_carcasa') }, [
      { from: [0, 0, 8], to: [16, 16, 8], faces: { north: { texture: '#r' }, south: { texture: '#r' } } },
      { from: [4, 1, 6], to: [12, 15, 10], faces: { north: { texture: '#f', uv: [4, 1, 12, 15] },
        south: { texture: '#c' }, east: { texture: '#c' }, west: { texture: '#c' }, up: { texture: '#c' }, down: { texture: '#c' } },
      ...(luz ? { shade: false, light_emission: 12 } : {}) }
    ]));
  }
  const v = {};
  for (const [f, y] of Object.entries(FACINGS)) for (let luz = 0; luz <= 2; luz++) v[`facing=${f},luz=${luz}`] = { model: t(`semaforo_castillo_${luz}`), ...(y ? { y } : {}) };
  json('blockstates/semaforo_castillo.json', { variants: v });
  json('items/semaforo_castillo.json', { model: { type: 'minecraft:model', model: t('semaforo_castillo_1') } });
}

// el plastico rojo del tobogan, con el reflejo
guardar(TEX, 'tobogan', new Lienzo(32).pintar((x, y) => {
  let c = escala([206, 30, 38], 0.9 + fbm(x, y, 511, 32) * 0.08);
  const r = Math.abs(((x + y * 0.4) % 32) - 9);
  if (r < 2.5) c = mezcla(c, [255, 200, 200], (2.5 - r) / 2.5 * 0.45);
  return c;
}));
simple('tobogan');

/* ============================================================ la sala central y el salon final */

// moqueta de carreteras: cesped, carreteras grises con su raya, casitas y arboles
guardar(TEX, 'moqueta_ciudad', new Lienzo(32).pintar((x, y) => {
  const rx = x % 16;
  const ry = y % 16;
  const carretera = (rx >= 6 && rx <= 9) || (ry >= 6 && ry <= 9);
  if (carretera) {
    let c = escala([86, 88, 92], 0.92 + hash(x, y, 521) * 0.1);
    if ((rx === 7 || rx === 8) && ry % 4 < 2 && !(ry >= 6 && ry <= 9)) c = [236, 236, 230];
    if ((ry === 7 || ry === 8) && rx % 4 < 2 && !(rx >= 6 && rx <= 9)) c = [236, 236, 230];
    return c;
  }
  let c = escala([70, 150, 70], 0.86 + fbm(x, y, 522, 32) * 0.2);
  const casa = hash(Math.floor(x / 8), Math.floor(y / 8), 523);
  if (rx >= 1 && rx <= 4 && ry >= 1 && ry <= 4 && casa > 0.5) c = [ROJO, AMARILLO, AZUL, [240, 240, 236]][Math.floor(casa * 8) % 4];
  if (rx >= 11 && rx <= 13 && ry >= 11 && ry <= 13 && casa < 0.4) c = [36, 96, 44];
  return c;
}));
simple('moqueta_ciudad');

// la pared verde agua y su franja de abajo (azul con una raya amarilla arriba)
guardar(TEX, 'pared_central', new Lienzo(32).pintar((x, y) => escala([72, 176, 156], 0.93 + fbm(x, y, 531, 32) * 0.07)));
guardar(TEX, 'pared_central_baja', new Lienzo(32).pintar((x, y) => {
  if (y < 5) return escala([240, 200, 60], 0.95 + hash(x, y, 532) * 0.05);
  if (y === 5) return [150, 120, 30];
  return escala([40, 60, 150], 0.9 + fbm(x, y, 533, 32) * 0.08);
}));
simple('pared_central');
simple('pared_central_baja');

// la valla blanca del salon final: tablillas con punta y dos travesanos
guardar(TEX, 'valla_fiesta', new Lienzo(16).pintar((x, y) => escala([240, 238, 232], 0.9 + hash(x, y, 541) * 0.1 - (y > 12 ? 0.1 : 0))));
json('models/block/valla_fiesta.json', mod({ v: t('valla_fiesta'), particle: t('valla_fiesta') }, [
  { from: [1, 0, 7], to: [4, 13, 9], faces: caras('#v') },
  { from: [6.5, 0, 7], to: [9.5, 14, 9], faces: caras('#v') },
  { from: [12, 0, 7], to: [15, 13, 9], faces: caras('#v') },
  { from: [0, 3, 6.5], to: [16, 5, 7], faces: caras('#v') },
  { from: [0, 9, 6.5], to: [16, 11, 7], faces: caras('#v') }
]));
estados('valla_fiesta', 4, () => 'valla_fiesta');

console.log('  laberinto, puertas, flechas, rotulos, castillo, sala central y salon final');

/* ============================================================ nombres */

const NOMBRES = {
  puerta_fiesta: ['Puerta de la fiesta', 'Party door'],
  semaforo_castillo: ['Semáforo', 'Traffic light'],
  localizador_salida: ['Localizador de salida', 'Exit locator'],
  marcador_fiesta: ['Marcador', 'Scoreboard'],
  noclip: ['Noclip', 'Noclip'],
  pared_laberinto: ['Muro del laberinto', 'Maze wall'],
  zocalo_laberinto: ['Zócalo granate', 'Maroon skirting'],
  techo_laberinto: ['Techo oscuro', 'Dark ceiling'],
  columna_roja: ['Columna roja', 'Red column'],
  columna_amarilla: ['Columna amarilla', 'Yellow column'],
  columna_verde: ['Columna verde', 'Green column'],
  columna_azul: ['Columna azul', 'Blue column'],
  columna_morada: ['Columna morada', 'Purple column'],
  damero_blanco: ['Baldosa blanca', 'White tile'],
  damero_azul: ['Baldosa azul', 'Blue tile'],
  damero_amarillo: ['Baldosa amarilla', 'Yellow tile'],
  damero_rojo: ['Baldosa roja', 'Red tile'],
  damero_morado: ['Baldosa morada', 'Purple tile'],
  banderines: ['Banderines', 'Bunting'],
  flecha_techo: ['Flecha de neón', 'Neon arrow'],
  letrero_fiesta: ['Rótulo luminoso', 'Lit sign'],
  nube_fiesta: ['Nube', 'Cloud'],
  poste_castillo: ['Poste acolchado', 'Padded post'],
  red_castillo: ['Red', 'Net'],
  tobogan: ['Tobogán', 'Slide'],
  moqueta_ciudad: ['Moqueta de carreteras', 'Road carpet'],
  pared_central: ['Pared verde agua', 'Teal wall'],
  pared_central_baja: ['Franja de la pared', 'Wall band'],
  valla_fiesta: ['Valla blanca', 'White fence']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, v] of Object.entries(NOMBRES)) lang[`block.${NS}.${id}`] = v[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
console.log('  lang: el laberinto');
