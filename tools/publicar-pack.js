#!/usr/bin/env node
'use strict';
/**
 * Publica el pack del evento.
 *
 *   node tools/publicar-pack.js            sube a GitHub Releases (tag "pack"); necesita GH_TOKEN
 *   node tools/publicar-pack.js --limpiar  ademas borra de la release los archivos que ya no se usan
 *   node tools/publicar-pack.js --local    genera dist-pack/ y lo sirve en http://127.0.0.1:8765/ para probar
 *   ... --ficha=pack/.evento-local.json    usa otra ficha (los archivos que empiezan por punto no viajan en el pack)
 *
 * Estructura de la carpeta pack/ (no se sube al repositorio):
 *
 *   pack/evento.json      version de Minecraft, loader, servidor, enlaces, noticias... (ver README)
 *   pack/mods/*.jar       en el disco del jugador se guardan con nombre ofuscado
 *   pack/una-vez/**       se copian solo si no existen (options.txt...): el jugador conserva sus ajustes
 *   pack/<lo demas>/**    config/, resourcepacks/, shaderpacks/... se imponen tal cual
 *
 * Cada archivo se sube con su SHA1 como nombre: si no cambia, no se vuelve a subir.
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const crypto = require('crypto');

const RAIZ = path.join(__dirname, '..');
const PACK = path.join(RAIZ, 'pack');
const SALIDA = path.join(RAIZ, 'dist-pack');
const TAG = 'pack';

const args = process.argv.slice(2);
const LOCAL = args.includes('--local');
const LIMPIAR = args.includes('--limpiar');
const PUERTO = Number((args.find((a) => a.startsWith('--puerto=')) || '').split('=')[1]) || 8765;
// --ficha=<archivo>: otra ficha del evento (p. ej. pack/.evento-local.json, con el servidor de pruebas)
const FICHA = (args.find((a) => a.startsWith('--ficha=')) || '').slice('--ficha='.length);

function sha1 (buf) { return crypto.createHash('sha1').update(buf).digest('hex'); }

function recorrer (dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) recorrer(full, out);
    else out.push(full);
  }
  return out;
}

function leerPack () {
  const fichaRuta = FICHA ? path.resolve(FICHA) : path.join(PACK, 'evento.json');
  if (!fs.existsSync(fichaRuta)) {
    console.error('Falta pack/evento.json. Copia pack-ejemplo/ a pack/ y rellenalo.');
    process.exit(1);
  }
  const ficha = JSON.parse(fs.readFileSync(fichaRuta, 'utf8'));
  for (const k of ['minecraft', 'server']) {
    if (!ficha[k]) { console.error(`pack/evento.json: falta "${k}"`); process.exit(1); }
  }

  const archivos = [];
  for (const full of recorrer(PACK)) {
    const rel = path.relative(PACK, full).split(path.sep).join('/');
    if (rel === 'evento.json' || path.basename(rel).startsWith('.')) continue;
    const buf = fs.readFileSync(full);
    const hash = sha1(buf);
    let destino = rel;
    let once = false;
    if (rel.startsWith('una-vez/')) {
      destino = rel.slice('una-vez/'.length);
      once = true;
    } else if (/^mods\/[^/]+\.jar$/i.test(rel)) {
      // Nombre ofuscado: en la carpeta del jugador no se ve que mod es cada uno.
      destino = `mods/${hash.slice(0, 16)}.jar`;
    }
    archivos.push({ origen: full, nombre: rel, path: destino, sha1: hash, size: buf.length, once });
  }
  const repetidos = archivos.map((a) => a.path.toLowerCase()).filter((p, i, l) => l.indexOf(p) !== i);
  if (repetidos.length) { console.error('Rutas repetidas en el pack:', repetidos); process.exit(1); }
  return { ficha, archivos };
}

function crearManifest (ficha, archivos, urlDe) {
  const huella = sha1(archivos.map((a) => `${a.path}:${a.sha1}`).sort().join('\n')).slice(0, 8);
  const d = new Date();
  const dos = (n) => String(n).padStart(2, '0');
  return {
    name: ficha.name,
    level: ficha.level != null ? String(ficha.level) : '0',
    packVersion: `${d.getFullYear()}.${dos(d.getMonth() + 1)}.${dos(d.getDate())}-${huella}`,
    minecraft: ficha.minecraft,
    loader: ficha.loader || 'vanilla',
    loaderVersion: ficha.loaderVersion,
    server: ficha.server,
    links: ficha.links || {},
    eventStart: ficha.eventStart || null,
    news: ficha.news || [],
    staff: ficha.staff || [],
    ram: ficha.ram || null,
    strict: ficha.strict,
    files: archivos.map((a) => {
      const f = { path: a.path, sha1: a.sha1, size: a.size, url: urlDe(a) };
      if (a.once) f.once = true;
      return f;
    })
  };
}

function resumen (archivos) {
  const mods = archivos.filter((a) => a.path.startsWith('mods/'));
  console.log(`\n${archivos.length} archivos (${mods.length} mods), ${(archivos.reduce((s, a) => s + a.size, 0) / 1048576).toFixed(1)} MB`);
  for (const m of mods) console.log(`  ${m.path}  <-  ${m.nombre}`);
}

/* ------------------------------------------------------------------- local */

function publicarLocal () {
  const { ficha, archivos } = leerPack();
  fs.rmSync(SALIDA, { recursive: true, force: true });
  fs.mkdirSync(SALIDA, { recursive: true });
  const base = `http://127.0.0.1:${PUERTO}/`;
  for (const a of archivos) fs.copyFileSync(a.origen, path.join(SALIDA, a.sha1));
  const m = crearManifest(ficha, archivos, (a) => base + a.sha1);
  fs.writeFileSync(path.join(SALIDA, 'manifest.json'), JSON.stringify(m, null, 2));
  resumen(archivos);
  console.log(`\nManifest local: ${base}manifest.json`);
  console.log(`Arranca el launcher con:  BACKROOMS_MANIFEST=${base}manifest.json npm start\n`);

  http.createServer((req, res) => {
    const nombre = path.basename(decodeURIComponent((req.url || '/').split('?')[0]));
    const file = path.join(SALIDA, nombre);
    if (!nombre || !fs.existsSync(file)) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'Content-Type': nombre.endsWith('.json') ? 'application/json' : 'application/octet-stream', 'Content-Length': fs.statSync(file).size });
    fs.createReadStream(file).pipe(res);
  }).listen(PUERTO, '127.0.0.1', () => console.log(`Sirviendo dist-pack/ en ${base} (Ctrl+C para parar)`));
}

/* ------------------------------------------------------------------ github */

function repoDelEvento () {
  const ev = require('../src/event.json');
  const m = /github\.com\/([^/]+)\/([^/]+)\/releases/.exec(ev.manifest || '');
  if (!m) { console.error('src/event.json: "manifest" no apunta a una release de GitHub'); process.exit(1); }
  return { owner: m[1], repo: m[2] };
}

async function gh (token, url, opts = {}) {
  const res = await fetch(url.startsWith('http') ? url : `https://api.github.com${url}`, {
    ...opts,
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json', 'User-Agent': 'backrooms-publicar-pack', ...(opts.headers || {}) }
  });
  if (res.status === 404 && !opts.esperar404) return null;
  if (!res.ok && res.status !== 404) throw new Error(`GitHub ${res.status}: ${await res.text()}`);
  return res.status === 204 ? {} : res.json().catch(() => ({}));
}

async function subir (token, release, nombre, buf, tipo = 'application/octet-stream') {
  const url = `https://uploads.github.com/repos/${release.repoPath}/releases/${release.id}/assets?name=${encodeURIComponent(nombre)}`;
  return gh(token, url, { method: 'POST', body: buf, headers: { 'Content-Type': tipo, 'Content-Length': String(buf.length) } });
}

async function publicarGithub () {
  const token = process.env.GH_TOKEN;
  if (!token) { console.error('Falta GH_TOKEN (token de GitHub con permiso "repo").'); process.exit(1); }
  const { owner, repo } = repoDelEvento();
  const { ficha, archivos } = leerPack();

  let release = await gh(token, `/repos/${owner}/${repo}/releases/tags/${TAG}`);
  if (!release) {
    console.log(`Creando la release "${TAG}" en ${owner}/${repo}...`);
    // make_latest=false: si fuera la "latest", el autoactualizador del
    // launcher la tomaria por una version del programa.
    release = await gh(token, `/repos/${owner}/${repo}/releases`, {
      method: 'POST',
      body: JSON.stringify({ tag_name: TAG, name: 'Pack del evento', body: 'Archivos del pack. Los gestiona tools/publicar-pack.js: no tocar a mano.', prerelease: true, make_latest: 'false' })
    });
  }
  release.repoPath = `${owner}/${repo}`;
  const assets = [];
  for (let page = 1; ; page++) {
    const lote = await gh(token, `/repos/${owner}/${repo}/releases/${release.id}/assets?per_page=100&page=${page}`);
    if (!lote || !lote.length) break;
    assets.push(...lote);
  }
  const yaSubidos = new Set(assets.map((a) => a.name));

  const unicos = [...new Map(archivos.map((a) => [a.sha1, a])).values()];
  const nuevos = unicos.filter((a) => !yaSubidos.has(a.sha1));
  console.log(`${unicos.length} archivos en el pack, ${nuevos.length} por subir.`);
  for (const a of nuevos) {
    process.stdout.write(`  subiendo ${a.nombre} (${(a.size / 1048576).toFixed(1)} MB)... `);
    await subir(token, release, a.sha1, fs.readFileSync(a.origen));
    console.log('ok');
  }

  const base = `https://github.com/${owner}/${repo}/releases/download/${TAG}/`;
  const m = crearManifest(ficha, archivos, (a) => base + a.sha1);
  const viejo = assets.find((a) => a.name === 'manifest.json');
  if (viejo) await gh(token, `/repos/${owner}/${repo}/releases/assets/${viejo.id}`, { method: 'DELETE' });
  await subir(token, release, 'manifest.json', Buffer.from(JSON.stringify(m, null, 2)), 'application/json');

  if (LIMPIAR) {
    const usados = new Set(unicos.map((a) => a.sha1));
    const sobran = assets.filter((a) => a.name !== 'manifest.json' && !usados.has(a.name));
    for (const a of sobran) await gh(token, `/repos/${owner}/${repo}/releases/assets/${a.id}`, { method: 'DELETE' });
    if (sobran.length) console.log(`Borrados ${sobran.length} archivos que ya no usa el pack.`);
  }

  resumen(archivos);
  console.log(`\nPublicado pack ${m.packVersion}. Los jugadores lo reciben la proxima vez que pulsen JUGAR.`);
}

(LOCAL ? Promise.resolve(publicarLocal()) : publicarGithub()).catch((err) => {
  console.error(err.message);
  process.exit(1);
});
