'use strict';
/**
 * Cartel animado del evento para Yamipa (cuadro de 7x4 bloques = 896x512 pixeles de mapa):
 *
 *   npx electron tools/cartel/main.js              video/cartel/backrooms-cartel-7x4.gif + .png
 *   npx electron tools/cartel/main.js --fotos=0,38,45   esos fotogramas en PNG (ya en colores de mapa)
 *
 * El fondo es el pasillo del Nivel 0 con las texturas del mod (cartel.fsh); encima los
 * logos y los datos del evento. El GIF va en la paleta de los mapas (mapa.js), que es
 * con la que lo pinta Yamipa. En el servidor: /image place backrooms-cartel-7x4.gif 7 4
 */

const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');

const RAIZ = path.join(__dirname, '..', '..');
const args = Object.fromEntries(process.argv.slice(2).filter((a) => a.startsWith('--')).map((a) => {
  const [k, v] = a.slice(2).split('=');
  return [k, v ?? true];
}));

app.whenReady().then(async () => {
  const cfg = {
    raiz: RAIZ,
    salida: process.env.CARTEL_SALIDA || path.join(RAIZ, 'video', 'cartel'),
    fotos: typeof args.fotos === 'string' ? args.fotos.split(',').map(Number) : null
  };
  const win = new BrowserWindow({
    show: false,
    width: 900,
    height: 520,
    webPreferences: { nodeIntegration: true, contextIsolation: false, backgroundThrottling: false }
  });
  ipcMain.on('log', (e, m) => console.log(m));
  ipcMain.on('fin', (e, codigo) => app.exit(codigo));
  win.webContents.on('console-message', (e, nivel, mensaje) => { if (nivel >= 2) console.log('[render]', mensaje); });
  win.webContents.on('render-process-gone', (e, d) => { console.log('El render se cayo:', d.reason); app.exit(1); });
  await win.loadFile(path.join(__dirname, 'render.html'), { query: { cfg: JSON.stringify(cfg) } });
});
