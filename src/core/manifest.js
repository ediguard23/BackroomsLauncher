'use strict';
/**
 * El manifest del evento: lo publica el organizador con tools/publicar-pack.js
 * y dice que version de Minecraft, que loader, que archivos (mods, configs,
 * resourcepacks...) y a que servidor se entra.
 *
 * Si no hay red pero ya se descargo una vez, se usa la copia guardada: se
 * puede seguir jugando con el ultimo pack verificado.
 */

const fs = require('fs');
const path = require('path');
const { getJson } = require('./net');

const LOADERS = ['vanilla', 'fabric', 'quilt'];

/** Una ruta del manifest es segura si es relativa y no sale de la carpeta del juego. */
function rutaSegura (p) {
  if (typeof p !== 'string' || !p || p.includes('\\') || p.startsWith('/') || /^[a-z]:/i.test(p)) return false;
  return !p.split('/').some((t) => t === '..' || t === '' || t === '.');
}

function validar (m) {
  const err = (msg) => { throw new Error(`Manifest invalido: ${msg}`); };
  if (!m || typeof m !== 'object') err('no es un objeto');
  if (typeof m.minecraft !== 'string') err('falta "minecraft"');
  const loader = m.loader || 'vanilla';
  if (!LOADERS.includes(loader)) err(`loader "${loader}" no soportado`);
  if (loader !== 'vanilla' && typeof m.loaderVersion !== 'string') err('falta "loaderVersion"');
  if (!m.server || typeof m.server.host !== 'string') err('falta "server.host"');
  if (!Array.isArray(m.files)) err('falta "files"');
  for (const f of m.files) {
    if (!rutaSegura(f.path)) err(`ruta no permitida: ${f.path}`);
    if (!/^[0-9a-f]{40}$/i.test(f.sha1 || '')) err(`sha1 incorrecto en ${f.path}`);
    if (typeof f.url !== 'string' || !/^https?:\/\//.test(f.url)) err(`url incorrecta en ${f.path}`);
  }
  return m;
}

async function leer (origen) {
  if (/^https?:\/\//.test(origen)) {
    const sep = origen.includes('?') ? '&' : '?';
    return getJson(`${origen}${sep}t=${Date.now()}`, { retries: 2, timeout: 15000 });
  }
  return JSON.parse(fs.readFileSync(origen, 'utf8'));
}

/** Devuelve { manifest, desdeCache, aviso }. */
async function cargar (root, origen) {
  const cache = path.join(root, 'cache', 'manifest.json');
  try {
    const m = validar(await leer(origen));
    fs.mkdirSync(path.dirname(cache), { recursive: true });
    fs.writeFileSync(cache, JSON.stringify(m, null, 2));
    return { manifest: m, desdeCache: false };
  } catch (err) {
    if (fs.existsSync(cache)) {
      try {
        return { manifest: validar(JSON.parse(fs.readFileSync(cache, 'utf8'))), desdeCache: true, aviso: err.message };
      } catch { /* la cache tampoco vale */ }
    }
    throw new Error(`No se pudo cargar la configuracion del evento (${err.message})`);
  }
}

/** Lo que puede ver la interfaz: sin lista de archivos ni staff. */
function publico (m) {
  return {
    nombre: m.name || null,
    nivel: m.level != null ? String(m.level) : '0',
    packVersion: m.packVersion || null,
    minecraft: m.minecraft,
    server: { host: m.server.host, port: m.server.port || 25565 },
    links: { discord: (m.links && m.links.discord) || '', tienda: (m.links && m.links.tienda) || '' },
    eventStart: m.eventStart || null,
    news: Array.isArray(m.news) ? m.news.slice(0, 20) : [],
    ram: m.ram || null
  };
}

module.exports = { cargar, publico, validar, rutaSegura };
