'use strict';
/**
 * Objetos sueltos del evento (mod evento/): el casete de las misiones.
 *
 *   npm run texturas
 *
 * El casete es una cinta tipo VHS-C: carcasa negra, dos ventanas con las
 * bobinas y una etiqueta escrita a mano. Cada jugador encuentra los suyos
 * tirados por el Nivel 0 (solo los ve el).
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

console.log('Objetos');
{
  const l = new Lienzo(16);
  const carcasa = [28, 28, 30];
  l.pintar((x, y) => (y >= 3 && y <= 12 ? escala(carcasa, 0.9 + hash(x, y, 81) * 0.2) : null));
  l.rect(0, 3, 15, 3, [60, 60, 64]);                    // canto de arriba con brillo
  l.rect(0, 12, 15, 12, [14, 14, 16]);
  l.rect(2, 4, 13, 7, [226, 214, 170]);                 // etiqueta
  l.rect(3, 5, 6, 5, [40, 50, 110]);                    // letra a boli
  l.rect(8, 5, 11, 5, [40, 50, 110]);
  l.rect(3, 6, 9, 6, [40, 50, 110]);
  l.rect(12, 4, 13, 5, [200, 60, 50]);                  // marca roja: "REC"
  for (const cx of [4, 11]) {                           // ventanas con las bobinas
    l.rect(cx - 2, 8, cx + 1, 11, [70, 66, 60]);
    l.rect(cx - 1, 9, cx, 10, [230, 226, 210]);
  }
  l.rect(6, 9, 9, 10, [20, 20, 22]);                    // cinta entre bobinas
  guardar(path.join(ASSETS, 'textures', 'item'), 'casete', l);
  json('models/item/casete.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/casete` } });
  json('items/casete.json', { model: { type: 'minecraft:model', model: `${NS}:item/casete` } });
}

for (const [archivo, nombre] of [['es_es.json', 'Casete'], ['en_us.json', 'Cassette tape']]) {
  const f = path.join(ASSETS, 'lang', archivo);
  const lang = JSON.parse(fs.readFileSync(f, 'utf8'));
  lang[`item.${NS}.casete`] = nombre;
  lang[`entity.${NS}.casete`] = nombre;
  fs.writeFileSync(f, JSON.stringify(lang, null, 2) + '\n');
}
