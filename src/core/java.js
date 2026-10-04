'use strict';
/**
 * Java automatico.
 *
 * Cada version de Minecraft dice en su json que Java necesita
 * (`javaVersion.majorVersion`: 1.16 pide 8, 1.20.5+ pide 21...). El launcher
 * nunca usa el Java del sistema: descarga el JRE de Temurin de esa version,
 * comprueba su SHA256 y lo guarda en `runtime/java-<n>` para la proxima vez.
 */

const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { getJson, download } = require('./net');

const API = 'https://api.adoptium.net/v3/assets/latest';
// Versiones que Temurin nunca publico: se usa la siguiente que si existe y
// que Minecraft acepta igual (1.17 pide 16 y funciona con 17).
const SUSTITUTO = { 16: 17 };

function javawDe (dir) {
  const candidato = path.join(dir, 'bin', 'javaw.exe');
  return fs.existsSync(candidato) ? candidato : null;
}

function buscarBin (dir, profundidad = 2) {
  const directo = javawDe(dir);
  if (directo) return dir;
  if (profundidad === 0) return null;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (!e.isDirectory()) continue;
    const r = buscarBin(path.join(dir, e.name), profundidad - 1);
    if (r) return r;
  }
  return null;
}

function extraer (zip, dest) {
  return new Promise((resolve, reject) => {
    new AdmZip(zip).extractAllToAsync(dest, true, false, (err) => (err ? reject(err) : resolve()));
  });
}

/**
 * Devuelve la ruta a javaw.exe de la version pedida, descargandola si hace falta.
 * `onEstado(texto)` y `onProgreso(hecho, total)` alimentan la barra del launcher.
 */
async function asegurarJava (major, runtimeDir, { onEstado = () => {}, onProgreso = () => {} } = {}) {
  major = SUSTITUTO[major] || major;
  const destino = path.join(runtimeDir, `java-${major}`);
  const marca = path.join(destino, '.backrooms');
  const listo = fs.existsSync(marca) && javawDe(destino);
  if (listo) return listo;

  onEstado(`Descargando Java ${major}`);
  const lista = await getJson(`${API}/${major}/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse`);
  const pkg = lista && lista[0] && lista[0].binary && lista[0].binary.package;
  if (!pkg) throw new Error(`Temurin no publica Java ${major} para Windows`);

  fs.mkdirSync(runtimeDir, { recursive: true });
  const zip = path.join(runtimeDir, `java-${major}.zip`);
  let hecho = 0;
  await download(pkg.link, zip, {
    sha256: pkg.checksum,
    size: pkg.size,
    timeout: 120000,
    onBytes: (n) => { hecho += n; onProgreso(hecho, pkg.size); }
  });

  onEstado(`Preparando Java ${major}`);
  const tmp = path.join(runtimeDir, `.tmp-java-${major}`);
  fs.rmSync(tmp, { recursive: true, force: true });
  await extraer(zip, tmp);
  const raiz = buscarBin(tmp);
  if (!raiz) throw new Error('El paquete de Java no trae javaw.exe');
  fs.rmSync(destino, { recursive: true, force: true });
  fs.renameSync(raiz, destino);
  fs.rmSync(tmp, { recursive: true, force: true });
  fs.rmSync(zip, { force: true });
  fs.writeFileSync(marca, `${lista[0].release_name}\n`);
  return javawDe(destino);
}

module.exports = { asegurarJava };
