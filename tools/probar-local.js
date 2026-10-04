#!/usr/bin/env node
'use strict';
/**
 * npm run probar: sirve pack/ en local y abre el launcher apuntando a el.
 * Sirve para ver cambios del pack o de la interfaz sin publicar nada.
 * Al cerrar el launcher se para tambien el servidor del pack.
 */

const { spawn } = require('child_process');
const path = require('path');

const RAIZ = path.join(__dirname, '..');
const PUERTO = 8765;

const servidor = spawn(process.execPath, [path.join(__dirname, 'publicar-pack.js'), '--local', `--puerto=${PUERTO}`], { cwd: RAIZ });
servidor.stderr.pipe(process.stderr);

let abierto = false;
servidor.stdout.on('data', (c) => {
  process.stdout.write(c);
  if (abierto || !String(c).includes('Sirviendo')) return;
  abierto = true;
  const electron = require('electron');
  const app = spawn(electron, ['.'], {
    cwd: RAIZ,
    stdio: 'inherit',
    env: { ...process.env, BACKROOMS_MANIFEST: `http://127.0.0.1:${PUERTO}/manifest.json` }
  });
  app.on('close', () => { servidor.kill(); process.exit(0); });
});
servidor.on('close', (codigo) => { if (!abierto) process.exit(codigo || 1); });
