#!/usr/bin/env node
'use strict';
/**
 * Compila un launcher especial para una beta cerrada, SIN publicarlo:
 *
 *   node tools/construir-beta.js [--tag=pack-beta] [--nombre="BETA #1"]
 *
 * Es el launcher de siempre con otra ficha (src/event.json solo durante la compilación):
 *   - baja el pack de la release <tag>, que se publica aparte con
 *     node tools/publicar-pack.js --tag=<tag> --ficha=pack/.evento-beta.json
 *   - usa su propia carpeta del juego (.backrooms-beta) y sus propios ajustes;
 *   - no se actualiza solo (si no, se convertiría en el launcher normal);
 *   - se instala aparte del normal como «Backrooms Beta».
 *
 * El instalador sale en dist-beta/. Se pasa a mano a los probadores.
 */
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const RAIZ = path.join(__dirname, '..');
const args = process.argv.slice(2);
const valor = (nombre, defecto) => (args.find((a) => a.startsWith(`--${nombre}=`)) || '').slice(nombre.length + 3) || defecto;
const TAG = valor('tag', 'pack-beta');
const NOMBRE = valor('nombre', 'BETA');

const ficha = path.join(RAIZ, 'src', 'event.json');
const original = fs.readFileSync(ficha, 'utf8');
const evento = JSON.parse(original);
const beta = {
  ...evento,
  subtitulo: NOMBRE,
  carpeta: '.backrooms-beta',
  manifest: evento.manifest.replace('/download/pack/', `/download/${TAG}/`),
  actualizar: false,
  appId: 'com.backrooms.launcher.beta'
};
if (beta.manifest === evento.manifest) {
  console.error(`No sé cambiar la release en ${evento.manifest}`);
  process.exit(1);
}

console.log(`Launcher de la beta: pack de "${TAG}", carpeta ${beta.carpeta}, subtítulo "${NOMBRE}".`);
let estado = 1;
try {
  fs.writeFileSync(ficha, JSON.stringify(beta, null, 2) + '\n');
  const r = spawnSync(process.execPath, [
    path.join(RAIZ, 'node_modules', 'electron-builder', 'cli.js'),
    '--win', '--publish', 'never',
    '-c.appId=com.backrooms.launcher.beta',
    '-c.productName=Backrooms Beta',
    '-c.extraMetadata.productName=Backrooms Beta',
    '-c.directories.output=dist-beta',
    '-c.nsis.artifactName=Backrooms-Beta-Setup-${version}.${ext}',
    '-c.nsis.shortcutName=Backrooms Beta',
    '-c.nsis.uninstallDisplayName=Backrooms Beta',
    '-c.nsis.differentialPackage=false'
  ], { cwd: RAIZ, stdio: 'inherit' });
  estado = r.status;
} finally {
  fs.writeFileSync(ficha, original);
}
process.exit(estado);
