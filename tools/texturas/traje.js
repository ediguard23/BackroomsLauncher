'use strict';
/**
 * Traje antirradiacion del evento (mod evento/): textura de armadura sobre el
 * cuerpo (plantilla de 64x32 de Minecraft), iconos del inventario y los JSON
 * del equipo y de los objetos.
 *
 *   npm run texturas
 *
 * Capucha amarilla con visor y respirador de dos filtros, chaqueta con
 * cremallera, banda reflectante y el simbolo de radiacion a la espalda,
 * guantes negros y botas de goma.
 */

const fs = require('fs');
const path = require('path');
const { hash, escala, Lienzo, guardar } = require('./lienzo');

const NS = 'backrooms_evento';
const ASSETS = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', NS);
const json = (rel, obj) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};

const AMARILLO = [214, 178, 40];
const NEGRO = [30, 30, 32];
const GOMA = [22, 22, 24];
const REFLEJO = [196, 198, 196];
const VISOR = [44, 58, 70];

/** Amarillo de tela con algo de grano y una sombra por fila (pliegues). */
const tela = (x, y, base = AMARILLO) => escala(base, 0.9 + hash(x, y, 3) * 0.12 - (y % 6 === 5 ? 0.05 : 0));

console.log('Traje antirradiacion');

// ---------------------------------------------------------------- cuerpo
{
  const l = new Lienzo(64, 32);
  // cabeza (8x8x8 en 0,0): arriba, abajo, derecha, frente, izquierda, detras
  l.pintar((x, y) => {
    if (y < 8 && x >= 8 && x < 24) return tela(x, y);
    if (y >= 8 && y < 16 && x < 32) return tela(x, y);
    return null;
  });
  l.rect(8, 9, 15, 9, escala(AMARILLO, 0.75));          // borde de la capucha
  l.rect(9, 10, 14, 12, VISOR);                         // visor
  l.rect(9, 10, 10, 10, [120, 150, 170]);               // brillo del visor
  l.rect(10, 13, 13, 15, NEGRO);                        // respirador
  l.rect(8, 14, 9, 15, [70, 70, 72]);                   // filtro izquierdo
  l.rect(14, 14, 15, 15, [70, 70, 72]);                 // filtro derecho
  l.rect(8, 15, 9, 15, [96, 96, 98]);
  l.rect(14, 15, 15, 15, [96, 96, 98]);
  for (const x0 of [0, 16]) l.rect(x0, 12, x0 + 7, 12, NEGRO); // correa del respirador
  l.rect(24, 12, 31, 12, NEGRO);

  // torso (8x12x4 en 16,16)
  l.pintar((x, y) => (y >= 16 && y < 20 && x >= 20 && x < 36) || (y >= 20 && x >= 16 && x < 40) ? tela(x, y) : null);
  l.rect(23, 20, 24, 29, [60, 60, 62]);                 // cremallera
  l.rect(20, 25, 27, 25, REFLEJO);                      // banda reflectante delante
  l.rect(32, 25, 39, 25, REFLEJO);                      // y detras
  l.rect(16, 30, 39, 31, NEGRO);                        // cinturon
  l.rect(23, 30, 24, 31, [150, 140, 90]);               // hebilla
  // simbolo de radiacion a la espalda (32..39, 20..31)
  l.disco(35.5, 22.5, 2.6, NEGRO);
  l.disco(35.5, 22.5, 1.6, AMARILLO);
  l.disco(35.5, 22.5, 0.6, NEGRO);
  l.rect(35, 20, 36, 20, AMARILLO);

  // brazos (4x12x4 en 40,16): guantes negros abajo
  l.pintar((x, y) => (y >= 16 && y < 20 && x >= 44 && x < 52) || (y >= 20 && x >= 40 && x < 56) ? tela(x, y) : null);
  l.rect(40, 22, 55, 22, REFLEJO);
  l.rect(40, 27, 55, 27, [60, 60, 62]);
  l.rect(40, 28, 55, 31, NEGRO);
  l.rect(48, 16, 51, 19, NEGRO);                        // palma (abajo)

  // piernas en esta capa = botas (4x12x4 en 0,16), solo la parte de abajo
  l.rect(0, 26, 15, 31, GOMA);
  l.rect(0, 26, 15, 26, [70, 70, 72]);
  l.rect(8, 16, 11, 19, GOMA);                          // suela

  guardar(path.join(ASSETS, 'textures', 'entity', 'equipment', 'humanoid'), 'traje', l);
}

// -------------------------------------------------------------- pantalon
{
  const l = new Lienzo(64, 32);
  l.pintar((x, y) => (y >= 16 && y < 20 && x >= 4 && x < 12) || (y >= 20 && x < 16) ? tela(x, y) : null);
  l.rect(0, 24, 15, 25, escala(AMARILLO, 0.78));        // rodilleras
  l.rect(0, 21, 15, 21, REFLEJO);
  // cintura: la parte baja del torso
  l.pintar((x, y) => y >= 27 && y < 32 && x >= 16 && x < 40 ? tela(x, y) : null);
  guardar(path.join(ASSETS, 'textures', 'entity', 'equipment', 'humanoid_leggings'), 'traje', l);
}

json('equipment/traje.json', {
  layers: {
    humanoid: [{ texture: `${NS}:traje` }],
    humanoid_leggings: [{ texture: `${NS}:traje` }]
  }
});

// --------------------------------------------------------------- iconos
const ICONOS = {
  traje_casco: (l) => {
    l.poligono([[3, 3], [12, 3], [14, 7], [14, 14], [1, 14], [1, 7]], AMARILLO);
    l.rect(4, 6, 11, 9, VISOR);
    l.rect(4, 6, 5, 6, [120, 150, 170]);
    l.rect(6, 10, 9, 13, NEGRO);
    l.rect(2, 11, 4, 13, [80, 80, 82]);
    l.rect(11, 11, 13, 13, [80, 80, 82]);
  },
  traje_chaqueta: (l) => {
    l.poligono([[1, 3], [5, 1], [10, 1], [14, 3], [15, 9], [12, 9], [12, 15], [3, 15], [3, 9], [0, 9]], AMARILLO);
    l.rect(7, 2, 8, 13, [60, 60, 62]);
    l.rect(3, 8, 12, 8, REFLEJO);
    l.rect(3, 13, 12, 14, NEGRO);
    l.rect(0, 9, 2, 10, NEGRO);
    l.rect(13, 9, 15, 10, NEGRO);
  },
  traje_pantalon: (l) => {
    l.poligono([[3, 1], [12, 1], [13, 15], [9, 15], [8, 6], [7, 6], [6, 15], [2, 15]], AMARILLO);
    l.rect(3, 1, 12, 2, NEGRO);
    l.rect(2, 9, 6, 9, REFLEJO);
    l.rect(9, 9, 13, 9, REFLEJO);
  },
  traje_botas: (l) => {
    for (const x0 of [1, 9]) {
      l.rect(x0 + 1, 4, x0 + 4, 11, GOMA);
      l.rect(x0, 11, x0 + 6, 14, GOMA);
      l.rect(x0, 14, x0 + 6, 14, [80, 80, 82]);
      l.rect(x0 + 1, 4, x0 + 4, 5, AMARILLO);
    }
  }
};
for (const [id, fn] of Object.entries(ICONOS)) {
  const l = new Lienzo(16);
  fn(l);
  guardar(path.join(ASSETS, 'textures', 'item'), id, l);
  json(`models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${id}` } });
  json(`items/${id}.json`, { model: { type: 'minecraft:model', model: `${NS}:item/${id}` } });
}

const NOMBRES = {
  traje_casco: ['Capucha antirradiación', 'Hazmat hood'],
  traje_chaqueta: ['Chaqueta antirradiación', 'Hazmat jacket'],
  traje_pantalon: ['Pantalón antirradiación', 'Hazmat trousers'],
  traje_botas: ['Botas antirradiación', 'Hazmat boots']
};
for (const [archivo, k] of [['es_es.json', 0], ['en_us.json', 1]]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  for (const [id, n] of Object.entries(NOMBRES)) lang[`item.${NS}.${id}`] = n[k];
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
