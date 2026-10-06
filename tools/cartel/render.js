'use strict';
/* global document, location, FontFace, createImageBitmap, Blob */
/**
 * Proceso de render del cartel (ver main.js). Cada fotograma se pinta al doble de
 * tamano en dos capas:
 *  - fondo: el pasillo (cartel.fsh), sombras, logos. Va tramado en la paleta de mapas.
 *  - texto: los datos del evento y el HUD de la camara. Va sin tramar (letras limpias).
 * Despues se reduce a 896x512 y se pasa a colores de mapa (mapa.js).
 *
 * La vuelta dura 6 s (60 fotogramas de 0,1 s; Yamipa usa un solo retardo para todos):
 * se anda por el pasillo, un tubo falla, se va la luz, la sonrisa aparece en la puerta
 * del logo y vuelve la luz. La camara avanza justo un periodo del plano: empalma.
 */

const fs = require('fs');
const path = require('path');
const { ipcRenderer } = require('electron');
const mapa = require('./mapa');

const cfg = JSON.parse(new URLSearchParams(location.search).get('cfg'));
const W = 896;
const H = 512;
const S = 2; // se pinta al doble y se reduce
const N = 60;
const PASO_S = 0.1;
const MOD = path.join(cfg.raiz, 'evento', 'src', 'main', 'resources', 'assets');
const log = (m) => ipcRenderer.send('log', m);

// colores exactos de la paleta de mapas (MapColor id, brillo)
const CREMA = mapa.color(2, 255);      // SAND
const CREMA_OSC = mapa.color(2, 180);
const ORO = mapa.color(30, 255);       // GOLD
const BLANCO = mapa.color(8, 255);     // SNOW
const GRIS = mapa.color(22, 255);      // COLOR_LIGHT_GRAY
const ROJO = mapa.color(4, 255);       // FIRE

/* --------------------------------------------------------------- lienzos */

function lienzo (w, h) {
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  return c;
}
const cA = lienzo(W * S, H * S);
const a = cA.getContext('2d', { willReadFrequently: true });
const cB = lienzo(W * S, H * S);
const b = cB.getContext('2d', { willReadFrequently: true });
const cA1 = lienzo(W, H);
const a1 = cA1.getContext('2d', { willReadFrequently: true });
const cB1 = lienzo(W, H);
const b1 = cB1.getContext('2d', { willReadFrequently: true });
const glc = lienzo(W * S, H * S);
const gl = glc.getContext('webgl2', { premultipliedAlpha: false, preserveDrawingBuffer: true, antialias: false });

const VERT = '#version 300 es\nvoid main() { vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2)); gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0); }';

function programa (frag) {
  const sh = (tipo, src) => {
    const s = gl.createShader(tipo);
    gl.shaderSource(s, src);
    gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  };
  const p = gl.createProgram();
  gl.attachShader(p, sh(gl.VERTEX_SHADER, VERT));
  gl.attachShader(p, sh(gl.FRAGMENT_SHADER, frag));
  gl.linkProgram(p);
  if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
  return p;
}

async function imagen (archivo) {
  return createImageBitmap(new Blob([fs.readFileSync(archivo)], { type: 'image/png' }));
}

async function atlasNivel0 () {
  const nombres = ['papel_pintado', 'papel_pintado_sucio', 'zocalo', 'moqueta', 'techo', 'fluorescente', 'fluorescente_apagado', 'moqueta_mojada'];
  const c = lienzo(256, 32);
  const x = c.getContext('2d');
  x.imageSmoothingEnabled = false;
  for (let i = 0; i < nombres.length; i++) {
    const img = await imagen(path.join(MOD, 'backrooms_evento', 'textures', 'block', nombres[i] + '.png'));
    x.drawImage(img, 0, 0, img.width, img.width, i * 32, 0, 32, 32);
  }
  const t = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, t);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, c);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  return t;
}

/* ----------------------------------------------------------------- logos */

let logo, studio, puerta;

/**
 * El logo de Backrooms: quita el borde verde que dejo el recorte y abre el hueco de
 * la puerta (el negro del centro) para ver el pasillo por ella. Devuelve tambien
 * donde queda el centro de la puerta.
 */
function prepararLogo (img) {
  const c = lienzo(img.width, img.height);
  const x = c.getContext('2d', { willReadFrequently: true });
  x.drawImage(img, 0, 0);
  const d = x.getImageData(0, 0, c.width, c.height);
  const p = d.data;
  // el hueco: pixeles oscuros dentro del muro (el muro ocupa el 60 % de arriba)
  let x0 = c.width; let x1 = 0; let y0 = c.height; let y1 = 0;
  for (let y = Math.round(c.height * 0.2); y < Math.round(c.height * 0.72); y++) {
    for (let xx = Math.round(c.width * 0.3); xx < Math.round(c.width * 0.7); xx++) {
      const o = (y * c.width + xx) * 4;
      if (p[o + 3] > 200 && p[o] + p[o + 1] + p[o + 2] < 45) {
        x0 = Math.min(x0, xx); x1 = Math.max(x1, xx); y0 = Math.min(y0, y); y1 = Math.max(y1, y);
      }
    }
  }
  for (let y = 0; y < c.height; y++) {
    for (let xx = 0; xx < c.width; xx++) {
      const o = (y * c.width + xx) * 4;
      // borde verde del recorte: el logo es amarillo (r >= g), el verde no
      if (p[o + 1] > p[o]) p[o + 1] = p[o];
      if (xx >= x0 && xx <= x1 && y >= y0 && y <= y1) {
        const lum = (p[o] + p[o + 1] + p[o + 2]) / 3;
        const k = Math.min(1, Math.max(0, (lum - 10) / 30));
        p[o + 3] = Math.round(p[o + 3] * k * k * (3 - 2 * k));
      }
    }
  }
  x.putImageData(d, 0, 0);
  return { canvas: c, puerta: { x: (x0 + x1) / 2 / c.width, y: (y0 + y1) / 2 / c.height, w: (x1 - x0) / c.width, h: (y1 - y0) / c.height } };
}

// donde va cada cosa (en pixeles del cartel, 896x512)
const LOGO = { alto: 292, arriba: 22 };
const ST = { x: 20, y: 18, ancho: 172 };

/* ---------------------------------------------------------- utilidades */

const hash = (n) => { const s = Math.sin(n) * 43758.5453123; return s - Math.floor(s); };

function texto (ctx, s, x, y, tam, espaciado, color, alinear = 'center', fuente = 'VT323', alfa = 1) {
  ctx.save();
  ctx.globalAlpha = alfa;
  ctx.font = `${tam * S}px ${fuente}`;
  ctx.letterSpacing = `${espaciado * tam * S}px`;
  ctx.textBaseline = 'top';
  ctx.textAlign = alinear;
  ctx.fillStyle = color;
  const dx = alinear === 'center' ? espaciado * tam * S / 2 : alinear === 'right' ? espaciado * tam * S : 0;
  ctx.fillText(s, x * S + dx, y * S);
  ctx.restore();
}

function linea (ctx, x0, y0, x1, y1, color, ancho = 1) {
  ctx.fillStyle = color;
  ctx.fillRect(Math.min(x0, x1) * S, Math.min(y0, y1) * S, Math.max(ancho, Math.abs(x1 - x0)) * S, Math.max(ancho, Math.abs(y1 - y0)) * S);
}

/** Corta la capa en tiras y las desplaza (tiron de cinta). */
function tiras (ctx, fuerza, semilla) {
  const w = W * S;
  const h = H * S;
  const img = ctx.getImageData(0, 0, w, h);
  const copia = new Uint8ClampedArray(img.data);
  const alto = Math.round(h / 34);
  for (let y0 = 0; y0 < h; y0 += alto) {
    if (hash(semilla + y0 * 0.37) > 0.4) continue;
    const dx = Math.round((hash(semilla * 3.3 + y0) - 0.5) * w * 0.06 * fuerza);
    for (let y = y0; y < Math.min(h, y0 + alto); y++) {
      const fila = y * w * 4;
      img.data.set(copia.subarray(fila + Math.max(0, -dx) * 4, fila + (w - Math.max(0, dx)) * 4), fila + Math.max(0, dx) * 4);
    }
  }
  ctx.putImageData(img, 0, 0);
}

/* -------------------------------------------------------------- el guion */

// estado de la luz en el fotograma f (0..N-1)
function guion (f) {
  let luz = 1;
  let sonrisa = 0;
  let glitch = 0;
  let destello = 0;
  // el tubo que falla: casi siempre encendido, con cortes
  let fallo = hash(f * 7.31 + 2) > 0.28 ? 1 : 0.08;
  if (f >= 30) fallo = hash(f * 3.7) > 0.5 ? 1 : 0;
  // se va la luz a tirones
  const tirones = { 35: 0.35, 36: 1, 37: 0, 38: 0.55, 39: 0 };
  if (f in tirones) { luz = tirones[f]; glitch = 0.6; }
  if (f >= 40 && f <= 49) luz = 0;
  // la sonrisa en la oscuridad
  const cara = { 42: 0.35, 43: 0.8, 44: 1, 45: 1, 46: 1, 47: 0.6, 48: 1, 49: 0.25 };
  if (f in cara) sonrisa = cara[f];
  if (f === 49) glitch = 1;
  if (f === 50) { destello = 0.22; glitch = 0.8; }
  if (f === 51) destello = 0.08;
  return { luz, sonrisa, fallo, glitch, destello };
}

/* ------------------------------------------------------------- las capas */

let prog;

function fondo (f, g) {
  const t = f * PASO_S;
  gl.viewport(0, 0, W * S, H * S);
  gl.useProgram(prog);
  const u = (k) => gl.getUniformLocation(prog, k);
  gl.uniform2f(u('ScreenSize'), W * S, H * S);
  gl.uniform1f(u('X'), 2.5 + 12 * f / N);
  gl.uniform1f(u('T'), t);
  gl.uniform1f(u('Luz'), g.luz);
  gl.uniform1f(u('Sonrisa'), g.sonrisa);
  gl.uniform1f(u('Fallo'), g.fallo);
  gl.uniform2f(u('Centro'), puerta.cx, puerta.cy);
  gl.activeTexture(gl.TEXTURE0);
  gl.bindTexture(gl.TEXTURE_2D, tAtlas);
  gl.uniform1i(u('Atlas'), 0);
  gl.drawArrays(gl.TRIANGLES, 0, 3);

  a.setTransform(1, 0, 0, 1, 0, 0);
  a.globalAlpha = 1;
  a.drawImage(glc, 0, 0);

  // banda de arriba y panel de abajo para que se lean los textos
  let gr = a.createLinearGradient(0, 0, 0, 110 * S);
  gr.addColorStop(0, 'rgba(10,8,4,0.78)');
  gr.addColorStop(1, 'rgba(10,8,4,0)');
  a.fillStyle = gr;
  a.fillRect(0, 0, W * S, 110 * S);
  gr = a.createLinearGradient(0, 300 * S, 0, H * S);
  gr.addColorStop(0, 'rgba(10,8,4,0)');
  gr.addColorStop(0.24, 'rgba(10,8,4,0.86)');
  gr.addColorStop(1, 'rgba(10,8,4,0.96)');
  a.fillStyle = gr;
  a.fillRect(0, 300 * S, W * S, (H - 300) * S);
  // vineta
  gr = a.createRadialGradient(W * S / 2, 170 * S, 120 * S, W * S / 2, 220 * S, 560 * S);
  gr.addColorStop(0, 'rgba(0,0,0,0)');
  gr.addColorStop(1, 'rgba(0,0,0,0.45)');
  a.fillStyle = gr;
  a.fillRect(0, 0, W * S, H * S);

  // logo de Backrooms (con sombra para separarlo del pasillo)
  const ancho = LOGO.alto * logo.width / logo.height;
  const lx = (W - ancho) / 2;
  a.save();
  a.globalAlpha = 0.35 + 0.65 * Math.min(1, g.luz);
  a.shadowColor = 'rgba(0,0,0,0.85)';
  a.shadowBlur = 26 * S;
  a.imageSmoothingQuality = 'high';
  a.drawImage(logo, lx * S, LOGO.arriba * S, ancho * S, LOGO.alto * S);
  a.restore();

  // PeakMC Studio
  a.save();
  a.imageSmoothingQuality = 'high';
  a.drawImage(studio, ST.x * S, ST.y * S, ST.ancho * S, ST.ancho * studio.height / studio.width * S);
  a.restore();

  if (g.destello > 0) {
    a.fillStyle = `rgba(255,236,170,${g.destello})`;
    a.fillRect(0, 0, W * S, H * S);
  }
  if (g.glitch > 0) tiras(a, g.glitch, f * 17 + 3);
}

function textos (f, g) {
  b.setTransform(1, 0, 0, 1, 0, 0);
  b.clearRect(0, 0, W * S, H * S);

  // PRESENTA bajo el logo del estudio
  const altoSt = ST.ancho * studio.height / studio.width;
  texto(b, 'PRESENTA', ST.x + ST.ancho / 2, ST.y + altoSt + 5, 17, 0.42, CREMA);

  // HUD de la videocamara con la fecha del evento
  if (Math.floor(f / 5) % 2 === 0) {
    b.fillStyle = ROJO;
    b.beginPath();
    b.arc((W - 92) * S, 31 * S, 7 * S, 0, Math.PI * 2);
    b.fill();
  }
  texto(b, 'REC', W - 20, 18, 28, 0.08, BLANCO, 'right');
  const seg = 13 + Math.floor(f * PASO_S);
  texto(b, `PM 9:00:${String(seg).padStart(2, '0')}`, W - 20, 48, 19, 0.08, BLANCO, 'right');
  texto(b, 'OCT. 31 2026', W - 20, 68, 19, 0.08, BLANCO, 'right');

  // bajo el logo
  texto(b, 'NIVEL 0  ·  NO OS SEPARÉIS', W / 2, 322, 19, 0.12, CREMA, 'center', 'SpecialElite');

  // tres columnas: cuando, cuantos, como entrar
  const yL = 354;
  const yG = 371;
  const yD = 421;
  const cols = [
    ['SÁBADO', '31 OCT', '9:00 PM (GMT-6)'],
    ['CUPO INICIAL', '100 PLAZAS', 'SOLO UNOS POCOS ESCAPAN'],
    ['ENTRADA', '2 USD', 'TIENDA.PEAKMC.LAT']
  ];
  for (let i = 0; i < 3; i++) {
    const cx = 150 + i * 298;
    texto(b, cols[i][0], cx, yL, 18, 0.3, GRIS);
    texto(b, cols[i][1], cx, yG, 50, 0.04, i === 2 ? ORO : CREMA);
    texto(b, cols[i][2], cx, yD, 20, 0.1, i === 2 ? ORO : BLANCO);
  }
  linea(b, 299, 360, 299, 446, CREMA_OSC);
  linea(b, 597, 360, 597, 446, CREMA_OSC);

  // pie
  linea(b, 40, 461, W - 40, 461, CREMA_OSC);
  texto(b, 'EVENTO EN VIVO  ·  LAUNCHER GRATIS  ·  DISCORD: DC.PEAKMC.LAT', W / 2, 472, 19, 0.14, CREMA);

  // separacion de color al irse la luz
  if (g.glitch > 0) {
    const img = b.getImageData(0, 0, W * S, H * S);
    const src = new Uint8ClampedArray(img.data);
    const dx = Math.round((2 + 4 * g.glitch) * S) * (hash(f) > 0.5 ? 1 : -1);
    const w = W * S;
    for (let y = 0; y < H * S; y++) {
      for (let x = 0; x < w; x++) {
        const o = (y * w + x) * 4;
        const xr = Math.min(w - 1, Math.max(0, x - dx));
        const or = (y * w + xr) * 4;
        // canal rojo desplazado: lo que queda fuera de la letra sale rojo, el otro lado cian
        if (src[or + 3] > 0 && src[o + 3] === 0) {
          img.data[o] = 255; img.data[o + 1] = 0; img.data[o + 2] = 0; img.data[o + 3] = src[or + 3];
        } else if (src[o + 3] > 0 && src[or + 3] === 0) {
          img.data[o] = 0; img.data[o + 1] = 220; img.data[o + 2] = 255;
        }
      }
    }
    b.putImageData(img, 0, 0);
  }
}

function reducir () {
  a1.imageSmoothingEnabled = true;
  a1.imageSmoothingQuality = 'high';
  a1.drawImage(cA, 0, 0, W, H);
  b1.clearRect(0, 0, W, H);
  b1.imageSmoothingEnabled = true;
  b1.imageSmoothingQuality = 'high';
  b1.drawImage(cB, 0, 0, W, H);
  return mapa.cuantizar(a1.getImageData(0, 0, W, H).data, b1.getImageData(0, 0, W, H).data, W, H, 16);
}

async function png (canvas, archivo) {
  const blob = await new Promise((res) => canvas.toBlob(res, 'image/png'));
  fs.writeFileSync(archivo, Buffer.from(await blob.arrayBuffer()));
}

/** Los indices de la paleta, de vuelta a colores (para ver el resultado). */
function verMapa (indices) {
  const c = lienzo(W, H);
  const x = c.getContext('2d');
  const d = x.createImageData(W, H);
  for (let i = 0; i < indices.length; i++) {
    const [r, g, bb] = mapa.PALETA[indices[i]];
    d.data[i * 4] = r; d.data[i * 4 + 1] = g; d.data[i * 4 + 2] = bb; d.data[i * 4 + 3] = 255;
  }
  x.putImageData(d, 0, 0);
  return c;
}

/* ------------------------------------------------------------------ main */

let tAtlas;

(async () => {
  try {
    for (const [nombre, archivo] of [['VT323', 'vt323.woff2'], ['SpecialElite', 'special-elite.woff2']]) {
      const fuente = new FontFace(nombre, fs.readFileSync(path.join(cfg.raiz, 'src', 'ui', 'fonts', archivo)));
      await fuente.load();
      document.fonts.add(fuente);
    }
    const gui = path.join(MOD, 'backrooms', 'textures', 'gui');
    const l = prepararLogo(await imagen(path.join(gui, 'logo_0.png')));
    logo = l.canvas;
    studio = await imagen(path.join(gui, 'peakmc_studio_0.png'));
    // el punto de fuga del pasillo cae en el centro de la puerta del logo
    const ancho = LOGO.alto * logo.width / logo.height;
    const px = (W - ancho) / 2 + l.puerta.x * ancho;
    const py = LOGO.arriba + l.puerta.y * LOGO.alto;
    puerta = { cx: px / W * 2 - 1, cy: 1 - py / H * 2 };
    log(`puerta del logo: ${Math.round(px)},${Math.round(py)} (${Math.round(l.puerta.w * ancho)}x${Math.round(l.puerta.h * LOGO.alto)} px)`);
    prog = programa(fs.readFileSync(path.join(__dirname, 'cartel.fsh'), 'utf8'));
    tAtlas = await atlasNivel0();
    fs.mkdirSync(cfg.salida, { recursive: true });

    if (cfg.fotos) {
      const dir = path.join(cfg.salida, 'fotos');
      fs.mkdirSync(dir, { recursive: true });
      for (const f of cfg.fotos) {
        const g = guion(f);
        fondo(f, g);
        textos(f, g);
        await png(verMapa(reducir()), path.join(dir, `f${String(f).padStart(2, '0')}.png`));
        const c = lienzo(W, H);
        c.getContext('2d').drawImage(cA1, 0, 0);
        c.getContext('2d').drawImage(cB1, 0, 0);
        await png(c, path.join(dir, `f${String(f).padStart(2, '0')}-color.png`));
        log(`foto ${f}`);
      }
      ipcRenderer.send('fin', 0);
      return;
    }

    const fotogramas = [];
    for (let f = 0; f < N; f++) {
      const g = guion(f);
      fondo(f, g);
      textos(f, g);
      fotogramas.push(reducir());
      if (f === 12) {
        // la imagen fija a todo color y al doble (redes, Discord...)
        const c = lienzo(W * S, H * S);
        const x = c.getContext('2d');
        x.drawImage(cA, 0, 0);
        x.drawImage(cB, 0, 0);
        await png(c, path.join(cfg.salida, 'backrooms-cartel.png'));
        await png(verMapa(fotogramas[f]), path.join(cfg.salida, 'backrooms-cartel-en-mapas.png'));
      }
      if (f % 10 === 0) log(`${f}/${N}`);
    }
    const archivo = path.join(cfg.salida, 'backrooms-cartel-7x4.gif');
    fs.writeFileSync(archivo, mapa.gif(fotogramas, W, H, Math.round(PASO_S * 100)));
    log(`Listo: ${archivo} (${(fs.statSync(archivo).size / 1048576).toFixed(1)} MB, ${N} fotogramas)`);
    ipcRenderer.send('fin', 0);
  } catch (e) {
    log('ERROR ' + (e.stack || e));
    ipcRenderer.send('fin', 1);
  }
})();
