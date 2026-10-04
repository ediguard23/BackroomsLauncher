'use strict';
/**
 * Descargas con verificacion.
 *
 * Todo archivo se escribe primero en `<destino>.part`, se calcula su hash
 * mientras llega y solo se renombra al destino si coincide. Asi un corte de
 * red nunca deja un jar a medias con el nombre bueno (que es lo que acaba en
 * NoClassDefFoundError o en un zip "corrupto" al abrir el juego).
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Readable } = require('stream');

const USER_AGENT = `BackroomsLauncher/${require('../../package.json').version}`;

function sleep (ms) { return new Promise((r) => setTimeout(r, ms)); }

async function fetchConReintentos (url, { timeout = 30000, retries = 3 } = {}) {
  let ultimo;
  for (let intento = 0; intento <= retries; intento++) {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), timeout);
    try {
      const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal: ctrl.signal, redirect: 'follow' });
      if (res.ok) return { res, cancelar: () => clearTimeout(t) };
      clearTimeout(t);
      ultimo = new Error(`HTTP ${res.status} en ${url}`);
      ultimo.status = res.status;
      // 4xx no se arregla reintentando (salvo 429).
      if (res.status >= 400 && res.status < 500 && res.status !== 429) throw ultimo;
    } catch (err) {
      clearTimeout(t);
      ultimo = err.name === 'AbortError' ? new Error(`Tiempo de espera agotado en ${url}`) : err;
      if (ultimo.status && ultimo.status < 500 && ultimo.status !== 429) throw ultimo;
    }
    if (intento < retries) await sleep(800 * (intento + 1));
  }
  throw ultimo;
}

async function getJson (url, opts) {
  const { res, cancelar } = await fetchConReintentos(url, opts);
  try { return await res.json(); } finally { cancelar(); }
}

function hashArchivo (file, algo = 'sha1') {
  return new Promise((resolve, reject) => {
    const h = crypto.createHash(algo);
    fs.createReadStream(file)
      .on('data', (c) => h.update(c))
      .on('end', () => resolve(h.digest('hex')))
      .on('error', reject);
  });
}

/**
 * Descarga `url` en `dest` comprobando sha1/sha256/size si se dan.
 * `onBytes(n)` recibe los bytes de cada trozo, para la barra de progreso.
 */
async function download (url, dest, { sha1, sha256, size, onBytes, timeout = 60000, retries = 3 } = {}) {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const part = `${dest}.part`;
  let ultimo;
  for (let intento = 0; intento <= retries; intento++) {
    let recibidos = 0;
    try {
      const { res, cancelar } = await fetchConReintentos(url, { timeout, retries: 0 });
      const h1 = sha1 ? crypto.createHash('sha1') : null;
      const h256 = sha256 ? crypto.createHash('sha256') : null;
      await new Promise((resolve, reject) => {
        const out = fs.createWriteStream(part);
        const src = Readable.fromWeb(res.body);
        src.on('data', (c) => {
          recibidos += c.length;
          if (h1) h1.update(c);
          if (h256) h256.update(c);
          if (onBytes) onBytes(c.length);
        });
        src.on('error', reject);
        out.on('error', reject);
        out.on('finish', resolve);
        src.pipe(out);
      }).finally(cancelar);

      if (size && recibidos !== size) throw new Error(`Tamano incorrecto (${recibidos} de ${size} bytes)`);
      if (h1 && h1.digest('hex') !== sha1.toLowerCase()) throw new Error('El SHA1 no coincide');
      if (h256 && h256.digest('hex') !== sha256.toLowerCase()) throw new Error('El SHA256 no coincide');
      fs.renameSync(part, dest);
      return dest;
    } catch (err) {
      // Lo que ya se conto en la barra se descuenta para no pasar del 100 %.
      if (onBytes && recibidos) onBytes(-recibidos);
      try { fs.unlinkSync(part); } catch { /* no existia */ }
      ultimo = err;
      if (err.status && err.status < 500 && err.status !== 429) break;
      if (intento < retries) await sleep(1000 * (intento + 1));
    }
  }
  throw new Error(`No se pudo descargar ${path.basename(dest)}: ${ultimo.message}`);
}

/** Ejecuta `fn` sobre `items` con como mucho `n` a la vez. */
async function pool (items, n, fn) {
  let i = 0;
  const workers = Array.from({ length: Math.min(n, items.length) }, async () => {
    while (i < items.length) {
      const item = items[i++];
      await fn(item);
    }
  });
  await Promise.all(workers);
}

module.exports = { USER_AGENT, getJson, download, hashArchivo, pool, fetchConReintentos };
