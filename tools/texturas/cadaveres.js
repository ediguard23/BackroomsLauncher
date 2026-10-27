'use strict';
/**
 * Las pieles de los cuerpos de las Piscinas (CadaverEntidad / CadaverRenderer): 64x64 con el
 * formato de piel de jugador (capa base y capa exterior) y, en el hueco libre de (56,16), la
 * cuerda de los colgados. Todo dibujado aqui (sin pieles de terceros):
 *
 *   cadaver_0  pelo gris revuelto, jersey de rayas negras y grises, mucha sangre
 *   cadaver_1  figura negra con capucha, mangas gris morado y manchas verde agua
 *   cadaver_2  todo negro, ojos y sonrisa rojos, costuras rojas
 *   cadaver_3  explorador con el traje amarillo, el visor roto
 *   cadaver_4  explorador con el traje rasgado y la cara al aire
 *   cadaver_5  Steve, el del trailer: traje abierto en el pecho, en la pasarela de la Estacion
 *
 *   node tools/texturas/cadaveres.js
 */

const path = require('path');
const { hash, fbm, mezcla, escala, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const TEX = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', NS, 'textures', 'entity');

/**
 * Pinta una caja del formato de piel: (u, v) la esquina, w ancho, h alto, d fondo. `fn(cara,
 * x, y, W, H)` da el color de cada pixel de cada cara (x, y dentro de la cara) o null.
 */
function caja (l, u, v, w, h, d, fn) {
  const caras = [
    ['arriba', u + d, v, w, d], ['abajo', u + d + w, v, w, d],
    ['der', u, v + d, d, h], ['frente', u + d, v + d, w, h],
    ['izq', u + d + w, v + d, d, h], ['atras', u + 2 * d + w, v + d, w, h]
  ];
  for (const [cara, x0, y0, W, H] of caras) {
    for (let y = 0; y < H; y++) {
      for (let x = 0; x < W; x++) {
        const c = fn(cara, x, y, W, H);
        if (c) l.punto(x0 + x, y0 + y, c, c.length > 3 ? c[3] : 1);
      }
    }
  }
}

const PARTES = {
  cabeza: [0, 0, 8, 8, 8], sombrero: [32, 0, 8, 8, 8],
  cuerpo: [16, 16, 8, 12, 4], chaqueta: [16, 32, 8, 12, 4],
  brazoD: [40, 16, 4, 12, 4], mangaD: [40, 32, 4, 12, 4],
  brazoI: [32, 48, 4, 12, 4], mangaI: [48, 48, 4, 12, 4],
  piernaD: [0, 16, 4, 12, 4], pantD: [0, 32, 4, 12, 4],
  piernaI: [16, 48, 4, 12, 4], pantI: [0, 48, 4, 12, 4]
};

const SANGRE = [112, 14, 12];
const SECA = [62, 12, 10];

/** Salpicaduras de sangre: devuelve el color mezclado si cae mancha en (x, y) de esa parte. */
function sangre (c, x, y, canal, cuanta) {
  const n = fbm(x * 1.7, y * 1.7, canal, 64);
  if (n > 1 - cuanta) return mezcla(c, hash(x, y, canal) > 0.5 ? SANGRE : SECA, 0.85);
  if (n > 1 - cuanta * 1.25 && hash(x, y, canal + 1) > 0.6) return mezcla(c, SECA, 0.6);
  return c;
}

function piel (nombre, def) {
  const l = new Lienzo(64);
  for (const [parte, [u, v, w, h, d]] of Object.entries(PARTES)) {
    const fn = def[parte];
    if (fn) caja(l, u, v, w, h, d, (cara, x, y, W, H) => fn(cara, x, y, W, H));
  }
  // la cuerda: fibra marron retorcida
  caja(l, 56, 16, 1, 16, 1, (cara, x, y) => escala([104, 84, 54], (y + (cara === 'frente' ? 0 : 1)) % 3 === 0 ? 0.7 : 0.95 + hash(x, y, 990) * 0.1));
  guardar(TEX, nombre, l);
}

/* ---- 0: pelo gris, jersey de rayas, mucha sangre */
{
  const PIEL = [232, 214, 206];
  const PELO = [150, 144, 148];
  const raya = (y) => (Math.floor(y / 2) % 2 === 0 ? [30, 30, 33] : [92, 92, 96]);
  const pelo = (x, y) => escala(PELO, 0.8 + fbm(x, y, 1001, 16) * 0.35);
  piel('cadaver_0', {
    cabeza: (cara, x, y) => {
      if (cara === 'arriba' || cara === 'atras') return pelo(x, y);
      if (cara === 'der' || cara === 'izq') return y < 5 ? pelo(x, y) : escala(PIEL, 0.92);
      if (cara === 'abajo') return PIEL;
      // la cara: flequillo que tapa un ojo, el otro abierto y rojo, sangre en la mejilla
      if (y < 2 || (y < 5 && x < 4 + (y > 2 ? 1 : 0))) return pelo(x, y);
      let c = PIEL;
      if (y === 4 && x === 5) c = [24, 20, 20];
      if (y === 4 && x === 6) c = [150, 20, 20];
      if (y === 6 && x >= 3 && x <= 4) c = [120, 70, 70];
      return sangre(c, x, y, 1002, 0.35);
    },
    sombrero: (cara, x, y) => (cara !== 'abajo' && y < 3 && hash(x, y, 1003) > 0.4 ? pelo(x, y) : null),
    cuerpo: (cara, x, y) => sangre(raya(y), x, y, 1004, 0.3),
    brazoD: (cara, x, y, W, H) => (y >= H - 3 ? sangre(PIEL, x, y, 1005, 0.5) : sangre(raya(y), x, y, 1006, 0.5)),
    brazoI: (cara, x, y, W, H) => (y >= H - 3 ? PIEL : sangre(raya(y), x, y, 1007, 0.18)),
    piernaD: (cara, x, y, W, H) => (y >= H - 2 ? [20, 20, 22] : sangre(escala([44, 44, 48], 0.9 + fbm(x, y, 1008, 16) * 0.15), x, y, 1009, 0.3)),
    piernaI: (cara, x, y, W, H) => (y >= H - 2 ? [20, 20, 22] : sangre(escala([44, 44, 48], 0.9 + fbm(x, y, 1010, 16) * 0.15), x, y, 1011, 0.22))
  });
}

/* ---- 1: figura negra con capucha, mangas gris morado y manchas verde agua */
{
  const NEGRO = [16, 16, 20];
  const MORADO = [74, 66, 82];
  const AGUA = [94, 142, 136];
  const CLARO = [170, 210, 204];
  const mancha = (c, x, y, canal) => {
    const n = fbm(x * 1.4, y * 1.4, canal, 64);
    return n > 0.68 ? (n > 0.76 ? CLARO : AGUA) : c;
  };
  const tela = (x, y, canal) => escala(NEGRO, 0.85 + fbm(x, y, canal, 16) * 0.4);
  piel('cadaver_1', {
    cabeza: (cara, x, y) => {
      if (cara === 'frente') {
        // la mascara: una franja clara que brilla, el resto en sombra
        if (x >= 2 && x <= 5 && y >= 1 && y <= 3) return y === 2 ? CLARO : AGUA;
        return tela(x, y, 1101);
      }
      return mancha(tela(x, y, 1102), x, y, 1103);
    },
    sombrero: (cara, x, y) => (cara === 'arriba' || (cara !== 'abajo' && cara !== 'frente' && y < 4) ? tela(x, y, 1104) : null),
    cuerpo: (cara, x, y) => sangre(mancha(tela(x, y, 1105), x + 3, y, 1106), x, y, 1107, 0.12),
    brazoD: (cara, x, y) => mancha(escala(MORADO, 0.85 + fbm(x, y, 1108, 16) * 0.3), x, y, 1109),
    brazoI: (cara, x, y) => escala(MORADO, 0.85 + fbm(x, y, 1110, 16) * 0.3),
    piernaD: (cara, x, y) => mancha(tela(x, y, 1111), x, y + 5, 1112),
    piernaI: (cara, x, y) => tela(x, y, 1113)
  });
}

/* ---- 2: todo negro, ojos y sonrisa rojos, costuras rojas */
{
  const NEGRO = [10, 10, 10];
  const ROJO = [236, 76, 76];
  const OSCURO = [96, 14, 14];
  const negro = (x, y, canal) => escala(NEGRO, 0.8 + hash(x, y, canal) * 0.5);
  const costura = (x, y, W, H, canal) => (x === Math.floor(W / 2) && hash(x, y, canal) > 0.35) ? OSCURO : null;
  piel('cadaver_2', {
    cabeza: (cara, x, y) => {
      if (cara !== 'frente') return negro(x, y, 1201);
      if (y === 3 && (x === 1 || x === 2 || x === 5 || x === 6)) return ROJO;     // los ojos
      if (y === 2 && (x === 2 || x === 5)) return [250, 250, 250];               // el brillo
      if (y === 4 && x === 6) return OSCURO;                                    // lo que gotea
      if (y === 5 && x === 6) return OSCURO;
      if ((y === 6 && x >= 1 && x <= 6) || (y === 5 && (x === 1 || x === 6))) return ROJO; // la sonrisa
      if (y === 7 && (x === 2 || x === 4)) return ROJO;
      return negro(x, y, 1202);
    },
    cuerpo: (cara, x, y, W, H) => costura(x, y, W, H, 1203) || (y === 6 && hash(x, y, 1204) > 0.5 ? OSCURO : negro(x, y, 1205)),
    brazoD: (cara, x, y) => (y % 5 === 0 && hash(x, y, 1206) > 0.4 ? OSCURO : negro(x, y, 1207)),
    brazoI: (cara, x, y) => (y % 5 === 2 && hash(x, y, 1208) > 0.4 ? OSCURO : negro(x, y, 1209)),
    piernaD: (cara, x, y, W, H) => costura(x, y, W, H, 1210) || negro(x, y, 1211),
    piernaI: (cara, x, y, W, H) => costura(x, y, W, H, 1212) || negro(x, y, 1213)
  });
}

/* ---- 3 y 4: exploradores con el traje antirradiacion (como los jugadores) */
for (const [nombre, rasgado, canal] of [['cadaver_3', false, 1300], ['cadaver_4', true, 1400]]) {
  const AMARILLO = [214, 172, 40];
  const traje = (x, y, k) => {
    let c = escala(AMARILLO, 0.82 + fbm(x, y, canal + k, 16) * 0.3);
    if (fbm(x * 2, y * 2, canal + k + 1, 32) > 0.66) c = mezcla(c, [96, 84, 54], 0.5); // mugre
    return c;
  };
  const roto = (c, x, y, k) => (rasgado && fbm(x * 1.6, y * 1.6, canal + k + 2, 64) > 0.7 ? mezcla([140, 40, 34], [70, 14, 12], hash(x, y, k)) : c);
  piel(nombre, {
    cabeza: (cara, x, y) => {
      if (cara !== 'frente') return traje(x, y, 1);
      if (!rasgado) {
        // el visor negro, con la raja
        if (x >= 1 && x <= 6 && y >= 2 && y <= 5) return (x - y === 1 || x - y === 2) && y < 5 ? [200, 210, 214] : [24, 28, 32];
        return sangre(traje(x, y, 3), x, y, canal + 4, 0.25);
      }
      // la capucha arrancada: la cara al aire, palida y ensangrentada
      if (y < 2) return traje(x, y, 5);
      let c = [214, 196, 188];
      if (y === 3 && (x === 2 || x === 5)) c = [20, 16, 16];
      return sangre(c, x, y, canal + 6, 0.45);
    },
    cuerpo: (cara, x, y) => {
      let c = traje(x, y, 7);
      if (y === 8) c = [30, 30, 30]; // el cinturon
      if (cara === 'frente' && y >= 2 && y <= 4 && x >= 5 && x <= 6) c = [60, 60, 64]; // el filtro
      return roto(sangre(c, x, y, canal + 8, 0.28), x, y, 9);
    },
    brazoD: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : roto(sangre(traje(x, y, 10), x, y, canal + 11, 0.4), x, y, 12)),
    brazoI: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : roto(traje(x, y, 13), x, y, 14)),
    piernaD: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : roto(sangre(traje(x, y, 15), x, y, canal + 16, 0.2), x, y, 17)),
    piernaI: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : roto(traje(x, y, 18), x, y, 19))
  });
}

/* ---- 5: Steve, el del trailer, muerto en la pasarela de la Estacion: el traje abierto en el pecho
 * (se ven la camiseta cian y la herida), la capucha arrancada y la cara al aire. Dibujado aqui. */
{
  const AMARILLO = [214, 172, 40];
  const PIEL = [190, 140, 104];
  const PELO = [64, 42, 24];
  const CAMISETA = [40, 168, 176];
  const traje = (x, y, k) => {
    let c = escala(AMARILLO, 0.82 + fbm(x, y, 1500 + k, 16) * 0.3);
    if (fbm(x * 2, y * 2, 1501 + k, 32) > 0.66) c = mezcla(c, [96, 84, 54], 0.5);
    return c;
  };
  // el desgarro del pecho: borde irregular de tela, debajo la camiseta y la herida
  const desgarro = (x, y) => {
    const borde = 2.2 + fbm(y * 1.3, 0, 1502, 16) * 1.6;
    return Math.abs(x - 3.5) < borde && y >= 1 && y <= 9;
  };
  piel('cadaver_5', {
    cabeza: (cara, x, y) => {
      if (cara === 'arriba' || cara === 'atras') return escala(PELO, 0.85 + hash(x, y, 1503) * 0.25);
      if (cara === 'der' || cara === 'izq') return y < 3 ? escala(PELO, 0.9) : escala(PIEL, 0.9);
      if (cara === 'abajo') return PIEL;
      if (y < 2) return escala(PELO, 0.9 + hash(x, y, 1504) * 0.2);
      let c = escala(PIEL, 0.94 + hash(x, y, 1505) * 0.08);
      if (y === 4 && (x === 1 || x === 6)) c = [236, 236, 236];
      if (y === 4 && (x === 2 || x === 5)) c = [52, 64, 140];
      if (y === 6 && x >= 2 && x <= 5) c = [110, 70, 50];
      if (y === 7 && x >= 3 && x <= 4) c = [120, 30, 26];
      return sangre(c, x, y, 1506, 0.3);
    },
    // los restos de la capucha, colgando por detras
    sombrero: (cara, x, y) => (cara === 'atras' || (cara !== 'abajo' && cara !== 'frente' && cara !== 'arriba' && y > 4 && hash(x, y, 1507) > 0.3)
      ? roto2(traje(x, y, 8), x, y) : null),
    cuerpo: (cara, x, y) => {
      let c = traje(x, y, 9);
      if (y === 8) c = [30, 30, 30];
      if (cara === 'frente' && desgarro(x, y)) {
        c = escala(CAMISETA, 0.85 + hash(x, y, 1508) * 0.15);
        if (Math.abs(x - 3.5) < 1.6 && y >= 3 && y <= 7) c = mezcla([150, 16, 14], [70, 10, 10], hash(x, y, 1509));
      }
      return sangre(c, x, y, 1510, cara === 'frente' ? 0.5 : 0.2);
    },
    chaqueta: (cara, x, y) => (cara === 'frente' && desgarro(x, y) && !desgarro(x + (x < 4 ? 1 : -1), y) ? escala(AMARILLO, 0.7) : null),
    brazoD: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : y < 5 ? sangre(escala(PIEL, 0.9), x, y, 1511, 0.6) : sangre(traje(x, y, 12), x, y, 1512, 0.45)),
    brazoI: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : sangre(traje(x, y, 13), x, y, 1513, 0.25)),
    piernaD: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : sangre(traje(x, y, 14), x, y, 1514, 0.3)),
    piernaI: (cara, x, y, W, H) => (y >= H - 3 ? [36, 36, 38] : sangre(traje(x, y, 15), x, y, 1515, 0.15))
  });
  function roto2 (c, x, y) { return fbm(x * 1.6, y * 1.6, 1516, 64) > 0.6 ? mezcla([140, 40, 34], [70, 14, 12], hash(x, y, 1517)) : c; }
}

console.log('Cadaveres: 6 pieles');
