'use strict';
/**
 * Instalacion y arranque de Minecraft, sin librerias de terceros.
 *
 * Se lee el json oficial de la version (y el del loader, que hereda de el),
 * se baja y verifica por SHA1 cada pieza (jar del cliente, librerias,
 * recursos, config de log4j) y se construye la linea de comandos de Java con
 * las mismas reglas que usa el launcher oficial.
 */

const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');
const AdmZip = require('adm-zip');
const { getJson, download, pool } = require('./net');

const MANIFIESTO_VERSIONES = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const RECURSOS = 'https://resources.download.minecraft.net';
const LIBRERIAS = 'https://libraries.minecraft.net/';
const PERFIL_LOADER = {
  fabric: (mc, v) => `https://meta.fabricmc.net/v2/versions/loader/${mc}/${v}/profile/json`,
  quilt: (mc, v) => `https://meta.quiltmc.org/v3/versions/loader/${mc}/${v}/profile/json`
};

/* ----------------------------------------------------------------- reglas */

/** Evalua las `rules` de una libreria o argumento para Windows x64. */
function reglasPermiten (rules, features = {}) {
  if (!rules || !rules.length) return true;
  let permitido = false;
  for (const r of rules) {
    let encaja = true;
    if (r.os) {
      if (r.os.name && r.os.name !== 'windows') encaja = false;
      // "x86" en las reglas significa Java de 32 bits; aqui siempre es x64.
      if (r.os.arch && r.os.arch !== 'x64' && r.os.arch !== 'x86_64') encaja = false;
      if (r.os.version) {
        try { if (!new RegExp(r.os.version).test(os.release())) encaja = false; } catch { encaja = false; }
      }
    }
    if (r.features) {
      for (const [k, v] of Object.entries(r.features)) {
        if (Boolean(features[k]) !== v) encaja = false;
      }
    }
    if (encaja) permitido = r.action === 'allow';
  }
  return permitido;
}

/** `grupo:artefacto:version[:clasificador][@ext]` -> ruta dentro de libraries/ */
function rutaMaven (nombre) {
  let [coords, ext = 'jar'] = nombre.split('@');
  const [grupo, artefacto, version, clasificador] = coords.split(':');
  const archivo = `${artefacto}-${version}${clasificador ? `-${clasificador}` : ''}.${ext}`;
  return [...grupo.split('.'), artefacto, version, archivo].join('/');
}

function claveLibreria (nombre) {
  const [grupo, artefacto, , clasificador] = nombre.split('@')[0].split(':');
  return `${grupo}:${artefacto}:${clasificador || ''}`;
}

/** Piezas descargables de una libreria (artefacto y, en versiones viejas, natives). */
function artefactosDe (lib) {
  if (!reglasPermiten(lib.rules)) return [];
  const out = [];
  const a = lib.downloads && lib.downloads.artifact;
  if (a && a.path && a.url) {
    out.push({ ruta: a.path, url: a.url, sha1: a.sha1, size: a.size });
  } else if (!lib.downloads && lib.name) {
    // Formato maven de Fabric/Quilt: nombre + repositorio.
    const ruta = rutaMaven(lib.name);
    const base = (lib.url || LIBRERIAS).replace(/\/?$/, '/');
    out.push({ ruta, url: base + ruta, sha1: lib.sha1, size: lib.size });
  }
  if (lib.natives && lib.natives.windows) {
    const clave = lib.natives.windows.replace('${arch}', '64');
    const c = lib.downloads && lib.downloads.classifiers && lib.downloads.classifiers[clave];
    if (c && c.url) {
      out.push({ ruta: c.path, url: c.url, sha1: c.sha1, size: c.size, nativo: (lib.extract && lib.extract.exclude) || ['META-INF/'] });
    }
  }
  return out;
}

/** Une el json del loader con el de la version de la que hereda. */
function fusionar (base, hijo) {
  if (!hijo) return { ...base, idJar: base.id };
  const vistas = new Set();
  const libraries = [];
  for (const lib of [...(hijo.libraries || []), ...(base.libraries || [])]) {
    const k = lib.name ? claveLibreria(lib.name) : JSON.stringify(lib);
    if (vistas.has(k)) continue;
    vistas.add(k);
    libraries.push(lib);
  }
  const fusion = { ...base, id: hijo.id, idJar: base.id, mainClass: hijo.mainClass || base.mainClass, libraries };
  if (base.arguments) {
    fusion.arguments = {
      game: [...(base.arguments.game || []), ...((hijo.arguments && hijo.arguments.game) || [])],
      jvm: [...(base.arguments.jvm || []), ...((hijo.arguments && hijo.arguments.jvm) || [])]
    };
  } else {
    // Version antigua (minecraftArguments): los jvm del loader van aparte.
    fusion.jvmExtra = (hijo.arguments && hijo.arguments.jvm) || [];
  }
  return fusion;
}

/* ------------------------------------------------------------- instalacion */

async function manifiestoVersiones (root) {
  const cache = path.join(root, 'cache', 'version_manifest_v2.json');
  try {
    const m = await getJson(MANIFIESTO_VERSIONES, { retries: 1 });
    fs.mkdirSync(path.dirname(cache), { recursive: true });
    fs.writeFileSync(cache, JSON.stringify(m));
    return m;
  } catch (err) {
    if (fs.existsSync(cache)) return JSON.parse(fs.readFileSync(cache, 'utf8'));
    throw err;
  }
}

async function jsonVerificado (url, file, sha1, verificados) {
  if (!(await verificados.estaBien(file, sha1))) {
    await download(url, file, { sha1 });
    verificados.marcar(file, sha1);
  }
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

async function perfilLoader (root, loader, mc, loaderVersion) {
  const id = `${loader}-loader-${loaderVersion}-${mc}`;
  const file = path.join(root, 'versions', id, `${id}.json`);
  if (fs.existsSync(file)) {
    try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch { /* corrupto: se vuelve a pedir */ }
  }
  const crear = PERFIL_LOADER[loader];
  if (!crear) throw new Error(`Loader no soportado: ${loader}`);
  const perfil = await getJson(crear(mc, loaderVersion));
  if (!perfil || !perfil.mainClass) throw new Error(`No existe ${loader} ${loaderVersion} para ${mc}`);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(perfil, null, 2));
  return perfil;
}

/**
 * Deja instalado y verificado todo lo necesario para arrancar.
 * Devuelve lo que necesita `argumentos()`.
 */
async function instalar ({ root, gameDir, minecraft, loader, loaderVersion, verificados, onEstado = () => {}, onProgreso = () => {} }) {
  onEstado('Buscando la version de Minecraft');
  const vm = await manifiestoVersiones(root);
  const entrada = vm.versions.find((v) => v.id === minecraft);
  if (!entrada) throw new Error(`Minecraft ${minecraft} no existe`);

  const base = await jsonVerificado(entrada.url, path.join(root, 'versions', minecraft, `${minecraft}.json`), entrada.sha1, verificados);
  const hijo = loader && loader !== 'vanilla' ? await perfilLoader(root, loader, minecraft, loaderVersion) : null;
  const version = fusionar(base, hijo);

  // --- lista completa de piezas
  const piezas = [];
  const jarCliente = path.join(root, 'versions', minecraft, `${minecraft}.jar`);
  const cli = base.downloads.client;
  piezas.push({ file: jarCliente, url: cli.url, sha1: cli.sha1, size: cli.size });

  let logConfig = null;
  const log = base.logging && base.logging.client;
  if (log && log.file) {
    logConfig = { file: path.join(root, 'assets', 'log_configs', log.file.id), argumento: log.argument };
    piezas.push({ file: logConfig.file, url: log.file.url, sha1: log.file.sha1, size: log.file.size });
  }

  const classpath = [];
  const nativos = [];
  const vistos = new Set();
  for (const lib of version.libraries) {
    for (const a of artefactosDe(lib)) {
      const file = path.join(root, 'libraries', ...a.ruta.split('/'));
      if (vistos.has(file)) continue;
      vistos.add(file);
      piezas.push({ file, url: a.url, sha1: a.sha1, size: a.size });
      if (a.nativo) nativos.push({ file, excluir: a.nativo });
      else classpath.push(file);
    }
  }
  classpath.push(jarCliente);

  const ai = base.assetIndex;
  const indice = await jsonVerificado(ai.url, path.join(root, 'assets', 'indexes', `${ai.id}.json`), ai.sha1, verificados);
  const objetos = Object.entries(indice.objects || {});
  for (const [, o] of objetos) {
    const sub = o.hash.slice(0, 2);
    piezas.push({ file: path.join(root, 'assets', 'objects', sub, o.hash), url: `${RECURSOS}/${sub}/${o.hash}`, sha1: o.hash, size: o.size });
  }

  // --- verificar lo que ya hay
  onEstado('Verificando archivos del juego');
  const faltan = [];
  let revisadas = 0;
  await pool(piezas, 32, async (p) => {
    if (!(await verificados.estaBien(p.file, p.sha1, p.size))) faltan.push(p);
    revisadas++;
    if (revisadas % 200 === 0) onProgreso(revisadas, piezas.length);
  });
  onProgreso(piezas.length, piezas.length);

  // --- descargar lo que falta
  if (faltan.length) {
    const total = faltan.reduce((s, p) => s + (p.size || 0), 0);
    let hecho = 0;
    onEstado(`Descargando Minecraft ${minecraft} (${faltan.length} archivos)`);
    await pool(faltan, 16, async (p) => {
      await download(p.url, p.file, { sha1: p.sha1, size: p.size, onBytes: (n) => { hecho += n; onProgreso(hecho, total); } });
      verificados.marcar(p.file, p.sha1);
    });
  }
  verificados.guardar();

  // --- recursos de versiones antiguas (antes de 1.7.10 iban sueltos)
  let gameAssets = path.join(root, 'assets');
  if (indice.virtual || indice.map_to_resources) {
    const dest = indice.map_to_resources ? path.join(gameDir, 'resources') : path.join(root, 'assets', 'virtual', ai.id);
    for (const [nombre, o] of objetos) {
      const destino = path.join(dest, ...nombre.split('/'));
      if (fs.existsSync(destino)) continue;
      fs.mkdirSync(path.dirname(destino), { recursive: true });
      fs.copyFileSync(path.join(root, 'assets', 'objects', o.hash.slice(0, 2), o.hash), destino);
    }
    gameAssets = dest;
  }

  // --- natives: desde 1.19 LWJGL se los extrae solo; antes los saca el launcher
  const nativesDir = path.join(root, 'natives', minecraft);
  fs.mkdirSync(nativesDir, { recursive: true });
  if (nativos.length && !fs.readdirSync(nativesDir).length) {
    onEstado('Preparando librerias nativas');
    for (const n of nativos) {
      const zip = new AdmZip(n.file);
      for (const e of zip.getEntries()) {
        if (e.isDirectory || n.excluir.some((x) => e.entryName.startsWith(x))) continue;
        const destino = path.join(nativesDir, e.entryName);
        fs.mkdirSync(path.dirname(destino), { recursive: true });
        fs.writeFileSync(destino, e.getData());
      }
    }
  }

  return {
    version,
    classpath,
    nativesDir,
    assetsRoot: path.join(root, 'assets'),
    assetsIndex: ai.id,
    gameAssets,
    logConfig,
    javaMajor: (base.javaVersion && base.javaVersion.majorVersion) || 8
  };
}

/* ---------------------------------------------------------------- arranque */

function expandir (lista, vars, features) {
  const sustituir = (s) => s.replace(/\$\{(\w+)\}/g, (m, k) => (k in vars ? vars[k] : m));
  const out = [];
  for (const item of lista) {
    if (typeof item === 'string') out.push(sustituir(item));
    else if (item && reglasPermiten(item.rules, features)) out.push(...[].concat(item.value).map(sustituir));
  }
  return out;
}

/**
 * Linea de comandos completa (sin el ejecutable de Java).
 * `cuenta`: { name, uuid, accessToken, userType, xuid }
 */
function argumentos (inst, { root, gameDir, cuenta, ramMB, ventana, marca, versionMarca, nombreVersion, jvmExtra = [] }) {
  const { version } = inst;
  const vars = {
    natives_directory: inst.nativesDir,
    launcher_name: marca,
    launcher_version: versionMarca,
    classpath: inst.classpath.join(path.delimiter),
    classpath_separator: path.delimiter,
    library_directory: path.join(root, 'libraries'),
    version_name: nombreVersion || version.id,
    version_type: version.type || 'release',
    game_directory: gameDir,
    assets_root: inst.assetsRoot,
    game_assets: inst.gameAssets,
    assets_index_name: inst.assetsIndex,
    auth_player_name: cuenta.name,
    auth_uuid: cuenta.uuid.replace(/-/g, ''),
    auth_access_token: cuenta.accessToken,
    auth_session: `token:${cuenta.accessToken}:${cuenta.uuid.replace(/-/g, '')}`,
    auth_xuid: cuenta.xuid || '0',
    clientid: cuenta.clientId || '0',
    user_type: cuenta.userType,
    user_properties: '{}',
    resolution_width: String((ventana && ventana.width) || 1280),
    resolution_height: String((ventana && ventana.height) || 720)
  };
  const features = { has_custom_resolution: Boolean(ventana) };

  const jvm = [
    `-Xms${Math.min(1024, ramMB)}M`,
    `-Xmx${ramMB}M`,
    // Los mismos ajustes de recolector que pone el launcher oficial.
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:+UseG1GC',
    '-XX:G1NewSizePercent=20',
    '-XX:G1ReservePercent=20',
    '-XX:MaxGCPauseMillis=50',
    '-XX:G1HeapRegionSize=32M'
  ];
  let game;
  if (version.arguments) {
    jvm.push(...expandir(version.arguments.jvm || [], vars, features));
    game = expandir(version.arguments.game || [], vars, features);
  } else {
    jvm.push(...expandir(['-Djava.library.path=${natives_directory}', ...(version.jvmExtra || []), '-cp', '${classpath}'], vars, features));
    game = expandir((version.minecraftArguments || '').split(' ').filter(Boolean), vars, features);
    if (ventana) game.push('--width', vars.resolution_width, '--height', vars.resolution_height);
  }
  if (inst.logConfig && inst.logConfig.argumento) {
    // Va antes de -cp: algunas versiones viejas cortan los -D que van detras.
    const i = jvm.indexOf('-cp');
    const arg = inst.logConfig.argumento.replace('${path}', inst.logConfig.file);
    if (i >= 0) jvm.splice(i, 0, arg); else jvm.push(arg);
  }
  if (jvmExtra.length) {
    const i = jvm.indexOf('-cp');
    if (i >= 0) jvm.splice(i, 0, ...jvmExtra); else jvm.push(...jvmExtra);
  }
  return [...jvm, version.mainClass, ...game];
}

/** Lanza Java. Devuelve el proceso hijo. */
function lanzar (javaPath, args, { gameDir, logFile }) {
  fs.mkdirSync(gameDir, { recursive: true });
  const hijo = spawn(javaPath, args, { cwd: gameDir, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  if (logFile) {
    fs.mkdirSync(path.dirname(logFile), { recursive: true });
    const out = fs.createWriteStream(logFile);
    hijo.stdout.pipe(out, { end: false });
    hijo.stderr.pipe(out, { end: false });
    hijo.on('close', () => out.end());
  }
  return hijo;
}

module.exports = { instalar, argumentos, lanzar, reglasPermiten, rutaMaven, fusionar };
