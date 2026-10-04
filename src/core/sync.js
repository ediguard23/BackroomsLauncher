'use strict';
/**
 * Sincroniza la carpeta del juego con el pack del evento.
 *
 * - Cada archivo del manifest se comprueba ENTERO (SHA1) en cada arranque y
 *   se repone si falta o cambio.
 * - En las carpetas estrictas (mods, resourcepacks, shaderpacks por defecto)
 *   se borra todo lo que no este en el manifest: nadie puede anadir ni quitar
 *   mods entre partidas.
 * - Los archivos con `once: true` (p. ej. options.txt) solo se ponen si no
 *   existen, para que cada jugador conserve sus ajustes de video y controles.
 */

const fs = require('fs');
const path = require('path');
const { download, pool } = require('./net');

const ESTRICTAS = ['mods', 'resourcepacks', 'shaderpacks'];

function clave (rel) { return rel.replace(/\\/g, '/').toLowerCase(); }

function destinoDe (gameDir, rel) {
  const dest = path.resolve(gameDir, ...rel.split('/'));
  if (!dest.startsWith(path.resolve(gameDir) + path.sep)) throw new Error(`Ruta fuera del juego: ${rel}`);
  return dest;
}

function listar (dir, base = dir, out = []) {
  let entradas;
  try { entradas = fs.readdirSync(dir, { withFileTypes: true }); } catch { return out; }
  for (const e of entradas) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) listar(full, base, out);
    else out.push(full);
  }
  return out;
}

function borrarVacias (dir, raiz) {
  let entradas;
  try { entradas = fs.readdirSync(dir, { withFileTypes: true }); } catch { return; }
  for (const e of entradas) if (e.isDirectory()) borrarVacias(path.join(dir, e.name), raiz);
  if (dir !== raiz && !fs.readdirSync(dir).length) fs.rmdirSync(dir);
}

async function sincronizar ({ gameDir, manifest, verificados, onEstado = () => {}, onProgreso = () => {} }) {
  const archivos = manifest.files || [];
  const estrictas = Array.isArray(manifest.strict) ? manifest.strict : ESTRICTAS;

  onEstado('Verificando el pack del evento');
  const faltan = [];
  let revisados = 0;
  await pool(archivos, 8, async (f) => {
    const dest = destinoDe(gameDir, f.path);
    const vale = f.once
      ? fs.existsSync(dest)
      : await verificados.estaBien(dest, f.sha1, f.size, { siempre: true });
    if (!vale) faltan.push({ ...f, dest });
    onProgreso(++revisados, archivos.length);
  });

  if (faltan.length) {
    const total = faltan.reduce((s, f) => s + (f.size || 0), 0);
    let hecho = 0;
    onEstado(`Descargando el pack del evento (${faltan.length} archivos)`);
    await pool(faltan, 6, async (f) => {
      await download(f.url, f.dest, { sha1: f.sha1, size: f.size, onBytes: (n) => { hecho += n; onProgreso(hecho, total); } });
      verificados.marcar(f.dest, f.sha1);
    });
  }

  // Limpieza de las carpetas estrictas.
  const permitidos = new Set(archivos.map((f) => clave(f.path)));
  let borrados = 0;
  for (const carpeta of estrictas) {
    const dir = path.join(gameDir, carpeta);
    fs.mkdirSync(dir, { recursive: true });
    for (const file of listar(dir)) {
      const rel = clave(path.relative(gameDir, file));
      if (permitidos.has(rel)) continue;
      fs.rmSync(file, { force: true });
      borrados++;
    }
    borrarVacias(dir, dir);
  }
  verificados.guardar();
  return { descargados: faltan.length, borrados };
}

module.exports = { sincronizar, ESTRICTAS };
