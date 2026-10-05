'use strict';
/**
 * Backrooms Launcher — proceso principal.
 *
 * Una sola accion importa: JUGAR. Al pulsarla se renueva la cuenta, se carga
 * el manifest del evento, se instala y verifica Minecraft y su loader, se
 * consigue el Java que pide la version, se sincroniza el pack (los mods que
 * decide el organizador) y se lanza el juego.
 */

const { app, BrowserWindow, ipcMain, shell, safeStorage } = require('electron');
const os = require('os');
const fs = require('fs');
const path = require('path');

const evento = require('./src/event.json');
const Config = require('./src/core/config');
const Verificados = require('./src/core/verificados');
const auth = require('./src/core/auth');
const manifestMod = require('./src/core/manifest');
const { instalar, argumentos, lanzar } = require('./src/core/game');
const { asegurarJava } = require('./src/core/java');
const { sincronizar } = require('./src/core/sync');
const { ping } = require('./src/core/status');

const VERSION = require('./package.json').version;

if (!app.requestSingleInstanceLock()) {
  app.quit();
}

let win = null;
let config = null;
let juego = null;           // proceso de Java mientras el juego esta abierto
let ocupado = false;        // instalando / arrancando
let cerrarAlSalir = false;  // el jugador cerro el launcher con el juego abierto
let ultimoManifest = null;

const root = () => process.env.BACKROOMS_ROOT || path.join(app.getPath('appData'), evento.carpeta);
const origenManifest = () => process.env.BACKROOMS_MANIFEST || evento.manifest;

function enviar (canal, datos) {
  if (win && !win.isDestroyed()) win.webContents.send(canal, datos);
}

function memoriaTotalMB () { return Math.floor(os.totalmem() / 1048576); }

function ramPorDefecto (recomendada) {
  const total = memoriaTotalMB();
  const sugerida = recomendada || (total <= 8192 ? 3072 : total <= 16384 ? 4096 : 6144);
  return Math.max(2048, Math.min(sugerida, total - 2048));
}

function cuentaPublica (c) {
  return c ? { type: c.type, name: c.name, uuid: c.uuid, skin: c.skin || null } : null;
}

/* ------------------------------------------------------------------ ventana */

function crearVentana () {
  win = new BrowserWindow({
    width: 1180,
    height: 720,
    minWidth: 980,
    minHeight: 620,
    frame: false,
    backgroundColor: '#14110a',
    show: false,
    title: 'Backrooms Launcher',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      // El zumbido de ambiente suena nada mas abrir, sin esperar un clic.
      autoplayPolicy: 'no-user-gesture-required'
    }
  });
  win.removeMenu();
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  win.webContents.on('will-navigate', (e) => e.preventDefault());
  win.once('ready-to-show', () => win.show());
  win.loadFile(path.join(__dirname, 'src', 'ui', 'index.html'));
  ganchosDePrueba();
}

/**
 * Solo en desarrollo: BACKROOMS_CAPTURA=<carpeta> guarda capturas de la
 * ventana cada BACKROOMS_CAPTURA_MS, y BACKROOMS_PRUEBA=<archivo.js> ejecuta
 * ese guion dentro de la interfaz. Sirven para probarla sin manos.
 */
function ganchosDePrueba () {
  if (app.isPackaged) return;
  const carpeta = process.env.BACKROOMS_CAPTURA;
  const guion = process.env.BACKROOMS_PRUEBA;
  if (guion) {
    win.webContents.once('did-finish-load', () => {
      win.webContents.executeJavaScript(fs.readFileSync(guion, 'utf8')).catch((e) => console.error('guion:', e));
    });
  }
  if (carpeta) {
    let n = 0;
    fs.mkdirSync(carpeta, { recursive: true });
    setInterval(async () => {
      if (!win || win.isDestroyed()) return;
      const img = await win.webContents.capturePage().catch(() => null);
      if (!img || img.isEmpty()) return; // minimizada: no hay nada que capturar
      fs.writeFileSync(path.join(carpeta, `captura-${String(++n).padStart(2, '0')}.png`), img.toPNG());
    }, Number(process.env.BACKROOMS_CAPTURA_MS) || 4000);
  }
}

app.on('second-instance', () => {
  if (!win) return;
  if (win.isMinimized()) win.restore();
  win.show();
  win.focus();
});

app.whenReady().then(() => {
  config = new Config(app.getPath('userData'), safeStorage);
  crearVentana();
  iniciarActualizador();
});

app.on('window-all-closed', () => app.quit());

/* ------------------------------------------------------- login de Microsoft */

function abrirLoginMicrosoft (url, prefijo) {
  return new Promise((resolve, reject) => {
    const login = new BrowserWindow({
      width: 500,
      height: 680,
      parent: win,
      modal: true,
      autoHideMenuBar: true,
      title: 'Iniciar sesion con Microsoft',
      backgroundColor: '#ffffff',
      webPreferences: {
        // Sesion en memoria y nueva cada vez: permite cambiar de cuenta.
        partition: `mslogin-${Date.now()}`,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: true
      }
    });
    let terminado = false;
    const revisar = (e, destino) => {
      if (terminado || !destino || !destino.startsWith(prefijo)) return;
      terminado = true;
      if (e && e.preventDefault) e.preventDefault();
      resolve(destino);
      setImmediate(() => { if (!login.isDestroyed()) login.close(); });
    };
    login.webContents.on('will-redirect', revisar);
    login.webContents.on('will-navigate', revisar);
    login.webContents.on('did-redirect-navigation', (e, u) => revisar(null, u));
    login.webContents.on('did-navigate', (e, u) => revisar(null, u));
    login.webContents.setWindowOpenHandler(({ url: u }) => {
      shell.openExternal(u);
      return { action: 'deny' };
    });
    login.on('closed', () => {
      if (!terminado) reject(new Error('Inicio de sesion cancelado'));
    });
    login.loadURL(url);
  });
}

/* ---------------------------------------------------------------------- IPC */

/**
 * Los errores que cruzan IPC pierden sus propiedades (y llegan con el texto
 * "Error invoking remote method..." delante), asi que se devuelven como dato.
 */
async function resultado (fn) {
  try {
    return { ok: true, datos: await fn() };
  } catch (err) {
    console.error(err);
    return { ok: false, error: err.message, relogin: Boolean(err.relogin) };
  }
}

ipcMain.handle('estado', () => {
  const cfg = config.get();
  return {
    config: { ...cfg, ramMB: cfg.ramMB || ramPorDefecto(ultimoManifest && ultimoManifest.ram && ultimoManifest.ram.recommended) },
    cuenta: cuentaPublica(config.cuenta()),
    evento: { nombre: evento.nombre, subtitulo: evento.subtitulo },
    version: VERSION,
    memoriaMB: memoriaTotalMB(),
    juegoAbierto: Boolean(juego)
  };
});

ipcMain.handle('config:set', (e, cambios) => config.set(cambios || {}));

ipcMain.handle('cuenta:login', () => resultado(async () => {
  const cuenta = await auth.loginMicrosoft(abrirLoginMicrosoft);
  config.guardarCuenta(cuenta);
  config.set({ premium: true });
  return cuentaPublica(cuenta);
}));

ipcMain.handle('cuenta:logout', () => {
  config.guardarCuenta(null);
  config.set({ premium: false });
  return true;
});

ipcMain.handle('evento:manifest', () => resultado(async () => {
  const { manifest, desdeCache, aviso } = await manifestMod.cargar(root(), origenManifest());
  ultimoManifest = manifest;
  return { ...manifestMod.publico(manifest), desdeCache, aviso: aviso || null };
}));

ipcMain.handle('evento:ping', async () => {
  if (!ultimoManifest) return { online: false };
  const s = ultimoManifest.server;
  return ping(s.host, s.port || 25565);
});

// Antes de la hora de inicio solo entra el staff del manifest.
ipcMain.handle('evento:acceso', (e, nombre) => {
  const m = ultimoManifest;
  if (!m || !m.eventStart || Date.now() >= Date.parse(m.eventStart)) return true;
  return esStaff(m, String(nombre || ''));
});

ipcMain.handle('abrir-enlace', (e, cual) => {
  const url = ultimoManifest && ultimoManifest.links && ultimoManifest.links[cual];
  if (typeof url === 'string' && /^https:\/\//.test(url)) shell.openExternal(url);
});

// Olvida que archivos estaban bien: el proximo JUGAR vuelve a hashear todo y
// repone lo que no cuadre (Minecraft, librerias, assets y pack).
ipcMain.handle('reparar', () => resultado(async () => {
  if (juego || ocupado) throw new Error('Cierra el juego antes de reparar');
  fs.rmSync(path.join(root(), 'cache', 'verificados.json'), { force: true });
  return true;
}));

ipcMain.handle('ventana:minimizar', () => win && win.minimize());
ipcMain.handle('ventana:cerrar', () => {
  if (juego) {
    // Con el juego abierto no se mata Java: el launcher se cierra al salir.
    cerrarAlSalir = true;
    win.hide();
    return;
  }
  app.quit();
});

ipcMain.handle('jugar', () => resultado(async () => {
  if (juego) throw new Error('El juego ya esta abierto');
  if (ocupado) throw new Error('Ya se esta preparando el juego');
  ocupado = true;
  try {
    await jugar();
  } finally {
    ocupado = false;
  }
}));

/* -------------------------------------------------------------------- jugar */

function progreso (etapa) {
  return {
    onEstado: (texto) => enviar('progreso', { etapa, texto }),
    onProgreso: (hecho, total) => enviar('progreso', { etapa, hecho, total })
  };
}

function esStaff (m, nombre) {
  return Array.isArray(m.staff) && m.staff.some((n) => String(n).toLowerCase() === nombre.toLowerCase());
}

async function jugar () {
  const cfg = config.get();
  const dir = root();
  const gameDir = dir;

  // 1. Cuenta
  enviar('progreso', { etapa: 'cuenta', texto: 'Comprobando la cuenta' });
  let cuenta;
  if (cfg.premium) {
    const guardada = config.cuenta();
    if (!guardada) throw Object.assign(new Error('Inicia sesion con tu cuenta premium'), { relogin: true });
    cuenta = await auth.renovar(guardada);
    if (cuenta !== guardada) config.guardarCuenta(cuenta);
  } else {
    cuenta = auth.cuentaOffline(cfg.nick);
  }

  // 2. Manifest del evento
  enviar('progreso', { etapa: 'evento', texto: 'Cargando el evento' });
  const { manifest } = await manifestMod.cargar(dir, origenManifest());
  ultimoManifest = manifest;
  if (manifest.eventStart && Date.now() < Date.parse(manifest.eventStart) && !esStaff(manifest, cuenta.name)) {
    throw new Error('El evento todavia no ha empezado');
  }

  // 3. Minecraft + loader
  const verificados = new Verificados(path.join(dir, 'cache', 'verificados.json'));
  const inst = await instalar({
    root: dir,
    gameDir,
    minecraft: manifest.minecraft,
    loader: manifest.loader || 'vanilla',
    loaderVersion: manifest.loaderVersion,
    verificados,
    ...progreso('juego')
  });

  // 4. Java
  const javaPath = await asegurarJava(inst.javaMajor, path.join(dir, 'runtime'), progreso('java'));

  // 5. Pack del evento
  await sincronizar({ gameDir, manifest, verificados, ...progreso('pack') });

  // 6. Datos para el mod del menu: servidor fijo y enlaces
  const cfgMod = path.join(gameDir, 'config', 'backrooms-event.json');
  fs.mkdirSync(path.dirname(cfgMod), { recursive: true });
  const pub = manifestMod.publico(manifest);
  fs.writeFileSync(cfgMod, JSON.stringify({
    nombre: manifest.name || evento.nombre,
    // nivel de las Backrooms del evento: el mod elige con el su fondo, colores y sonidos
    nivel: pub.nivel,
    packVersion: manifest.packVersion || null,
    server: pub.server,
    links: pub.links,
    eventStart: pub.eventStart,
    news: pub.news
  }, null, 2));

  // 7. Arrancar
  enviar('progreso', { etapa: 'arranque', texto: 'Abriendo Minecraft' });
  const ramMB = cfg.ramMB || ramPorDefecto(manifest.ram && manifest.ram.recommended);
  const args = argumentos(inst, {
    root: dir,
    gameDir,
    cuenta,
    ramMB,
    ventana: { width: 1280, height: 720 },
    marca: 'BackroomsLauncher',
    versionMarca: VERSION,
    // Lo que sale en F3 y en los informes de error en vez de "fabric-loader-...".
    nombreVersion: manifest.name || evento.nombre
  });
  if (cfg.pantallaCompleta) args.push('--fullscreen');
  // Solo en desarrollo: BACKROOMS_QUICKPLAY=host:puerto entra directo a ese servidor (pruebas sin manos).
  if (!app.isPackaged && process.env.BACKROOMS_QUICKPLAY) args.push('--quickPlayMultiplayer', process.env.BACKROOMS_QUICKPLAY);
  const hijo = lanzar(javaPath, args, { gameDir, logFile: path.join(dir, 'logs', 'launcher-salida.log') });
  juego = hijo;
  enviar('juego', { estado: 'abriendo' });

  let visto = false;
  let resto = '';
  const leer = (chunk) => {
    resto += chunk.toString('utf8');
    const lineas = resto.split(/\r?\n/);
    resto = lineas.pop();
    for (const l of lineas) {
      if (!visto && /Sound engine started|OpenAL initialized|Created: \d+x\d+x\d+ minecraft:textures/.test(l)) {
        visto = true;
        enviar('juego', { estado: 'jugando' });
        if (win && !win.isDestroyed() && !cerrarAlSalir) win.minimize();
      }
    }
  };
  hijo.stdout.on('data', leer);
  hijo.stderr.on('data', leer);
  hijo.on('error', (err) => {
    juego = null;
    enviar('juego', { estado: 'cerrado', error: `No se pudo abrir Java: ${err.message}` });
  });
  hijo.on('close', (codigo) => {
    juego = null;
    if (cerrarAlSalir) return app.quit();
    enviar('juego', {
      estado: 'cerrado',
      codigo,
      error: codigo && codigo !== 0 ? `Minecraft se cerro con un error (codigo ${codigo})` : null
    });
    if (win && !win.isDestroyed()) {
      if (win.isMinimized()) win.restore();
      win.show();
      win.focus();
    }
  });
}

/* ------------------------------------------------------------ actualizacion */

function iniciarActualizador () {
  if (!app.isPackaged) return;
  let autoUpdater;
  try { ({ autoUpdater } = require('electron-updater')); } catch { return; }
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.on('update-available', (i) => enviar('actualizacion', { estado: 'descargando', version: i.version }));
  autoUpdater.on('download-progress', (p) => enviar('actualizacion', { estado: 'descargando', porcentaje: Math.round(p.percent) }));
  autoUpdater.on('update-downloaded', (i) => enviar('actualizacion', { estado: 'lista', version: i.version }));
  autoUpdater.on('error', () => { /* sin red o sin releases: no molesta */ });
  ipcMain.handle('actualizacion:instalar', () => {
    if (!juego) autoUpdater.quitAndInstall(true, true);
  });
  autoUpdater.checkForUpdates().catch(() => {});
}
