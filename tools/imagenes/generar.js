'use strict';
/**
 * Saca todas las imagenes del launcher y del mod a partir de los dos logos
 * originales de assets/fuentes-ia (generados en Higgsfield sobre fondo
 * magenta; los prompts estan en assets/PROMPTS.md):
 *
 *   - quita el fondo magenta (con su mezcla en los bordes) y recorta,
 *   - el logo de Backrooms y el de PeakMC Studio para el launcher y el mod,
 *   - el icono (la puerta del logo) para el instalador, la ventana del
 *     launcher y la ventana de Minecraft.
 *
 *   npm run imagenes
 */

const fs = require('fs');
const path = require('path');
const png = require('./png');

const RAIZ = path.join(__dirname, '..', '..');
const FUENTES = path.join(RAIZ, 'assets', 'fuentes-ia');
const MOD = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', 'backrooms');

/* ------------------------------------------------------------- utilidades */

function cargar (nombre) {
  return png.leer(fs.readFileSync(path.join(FUENTES, nombre)));
}

function guardar (img, destino) {
  fs.mkdirSync(path.dirname(destino), { recursive: true });
  fs.writeFileSync(destino, png.escribir(img));
  console.log(`  ${path.relative(RAIZ, destino)}  ${img.width}x${img.height}`);
}

function nueva (w, h) {
  return { width: w, height: h, data: new Uint8Array(w * h * 4) };
}

/** Color medio de las cuatro esquinas: el fondo real nunca es #FF00FF exacto. */
function colorFondo (img) {
  const suma = [0, 0, 0];
  let n = 0;
  for (const [cx, cy] of [[0, 0], [img.width - 24, 0], [0, img.height - 24], [img.width - 24, img.height - 24]]) {
    for (let y = cy; y < cy + 24; y++) {
      for (let x = cx; x < cx + 24; x++) {
        const i = (y * img.width + x) * 4;
        suma[0] += img.data[i]; suma[1] += img.data[i + 1]; suma[2] += img.data[i + 2];
        n++;
      }
    }
  }
  return suma.map((v) => v / n);
}

/**
 * Quita el fondo magenta. La "magentez" de un pixel (min(R,B) - G) vale ~255
 * en el fondo y <= 0 en el logo (amarillos, cobres, negros); en los bordes
 * mezclados se interpola el alfa y se descuenta el magenta del color.
 * `opacos` son rectangulos donde el magenta no es fondo sino un reflejo (el
 * suelo de la puerta): ahi se oscurece en vez de volverse transparente.
 */
function quitarMagenta (img, opacos = []) {
  const K = colorFondo(img);
  const mK = Math.min(K[0], K[2]) - K[1];
  const bajo = 28;
  const alto = mK - 22;
  const out = nueva(img.width, img.height);
  for (let y = 0; y < img.height; y++) {
    for (let x = 0; x < img.width; x++) {
      const i = (y * img.width + x) * 4;
      const r = img.data[i];
      const g = img.data[i + 1];
      const b = img.data[i + 2];
      const m = Math.min(r, b) - g;
      if (opacos.some(([x0, y0, x1, y1]) => x >= x0 && x < x1 && y >= y0 && y < y1)) {
        const v = m > 20 ? g : null; // suelo rosado: se vuelve penumbra
        out.data[i] = v == null ? r : Math.min(255, v * 1.15);
        out.data[i + 1] = v == null ? g : v;
        out.data[i + 2] = v == null ? b : v * 0.7;
        out.data[i + 3] = 255;
        continue;
      }
      // el generador deja a veces una marca tenue en las esquinas
      const enMarco = x < 8 || y < 8 || x >= img.width - 8 || y >= img.height - 8;
      const a = enMarco || m >= alto ? 0 : m <= bajo ? 1 : 1 - (m - bajo) / (alto - bajo);
      if (a <= 0) continue;
      const des = (c, k) => Math.max(0, Math.min(255, (c - (1 - a) * k) / a));
      out.data[i] = des(r, K[0]);
      out.data[i + 1] = des(g, K[1]);
      out.data[i + 2] = des(b, K[2]);
      out.data[i + 3] = Math.round(a * 255);
    }
  }
  limpiarMotas(out);
  sanearBordes(out);
  return out;
}

/** Pixeles sueltos que el compresor dejo en el fondo: fuera. */
function limpiarMotas (img) {
  const { width: w, height: h, data } = img;
  const quitar = [];
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      if (!data[(y * w + x) * 4 + 3]) continue;
      let vecinos = 0;
      for (let dy = -1; dy <= 1; dy++) {
        for (let dx = -1; dx <= 1; dx++) {
          const xx = x + dx; const yy = y + dy;
          if ((dx || dy) && xx >= 0 && yy >= 0 && xx < w && yy < h && data[(yy * w + xx) * 4 + 3]) vecinos++;
        }
      }
      if (vecinos < 3) quitar.push((y * w + x) * 4 + 3);
    }
  }
  for (const i of quitar) data[i] = 0;
}

/**
 * En el borde, el color mezclado con magenta no se puede recuperar bien (el
 * amarillo pierde verde y sale rosado). Los pixeles que tocan transparencia
 * toman el color medio del interior opaco cercano y conservan su alfa.
 */
function sanearBordes (img) {
  const { width: w, height: h, data } = img;
  const alfa = (x, y) => (x < 0 || y < 0 || x >= w || y >= h ? 0 : data[(y * w + x) * 4 + 3]);
  const interior = new Uint8Array(w * h);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let ok = alfa(x, y) === 255;
      for (let dy = -2; ok && dy <= 2; dy++) for (let dx = -2; ok && dx <= 2; dx++) if (alfa(x + dx, y + dy) < 250) ok = false;
      interior[y * w + x] = ok ? 1 : 0;
    }
  }
  const copia = Uint8Array.from(data);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const i = (y * w + x) * 4;
      if (!data[i + 3] || interior[y * w + x]) continue;
      let r = 0; let g = 0; let b = 0; let n = 0;
      for (let dy = -4; dy <= 4; dy++) {
        for (let dx = -4; dx <= 4; dx++) {
          const xx = x + dx; const yy = y + dy;
          if (xx < 0 || yy < 0 || xx >= w || yy >= h || !interior[yy * w + xx]) continue;
          const j = (yy * w + xx) * 4;
          r += copia[j]; g += copia[j + 1]; b += copia[j + 2]; n++;
        }
      }
      if (n) {
        data[i] = r / n; data[i + 1] = g / n; data[i + 2] = b / n;
      }
    }
  }
}

/** Caja de los pixeles con alfa > umbral. */
function caja (img, umbral = 8) {
  let x0 = img.width; let y0 = img.height; let x1 = -1; let y1 = -1;
  for (let y = 0; y < img.height; y++) {
    for (let x = 0; x < img.width; x++) {
      if (img.data[(y * img.width + x) * 4 + 3] > umbral) {
        if (x < x0) x0 = x; if (x > x1) x1 = x;
        if (y < y0) y0 = y; if (y > y1) y1 = y;
      }
    }
  }
  return { x0, y0, x1: x1 + 1, y1: y1 + 1 };
}

function recortar (img, { x0, y0, x1, y1 }) {
  const out = nueva(x1 - x0, y1 - y0);
  for (let y = y0; y < y1; y++) {
    const src = (y * img.width + x0) * 4;
    out.data.set(img.data.subarray(src, src + (x1 - x0) * 4), (y - y0) * out.width * 4);
  }
  return out;
}

function ajustado (img, margen = 0) {
  const c = caja(img);
  return recortar(img, {
    x0: Math.max(0, c.x0 - margen),
    y0: Math.max(0, c.y0 - margen),
    x1: Math.min(img.width, c.x1 + margen),
    y1: Math.min(img.height, c.y1 + margen)
  });
}

/** Lo centra en un lienzo cuadrado transparente. */
function cuadrado (img, margen = 0) {
  const lado = Math.max(img.width, img.height) + margen * 2;
  const out = nueva(lado, lado);
  const ox = Math.floor((lado - img.width) / 2);
  const oy = Math.floor((lado - img.height) / 2);
  for (let y = 0; y < img.height; y++) {
    out.data.set(img.data.subarray(y * img.width * 4, (y + 1) * img.width * 4), ((y + oy) * lado + ox) * 4);
  }
  return out;
}

/**
 * Reduce por media de area con alfa premultiplicado (sin halos oscuros en los
 * bordes). Solo reduce: para agrandar ya escala el que dibuja.
 */
function reducir (img, w, h) {
  if (w >= img.width && h >= img.height) return img;
  const out = nueva(w, h);
  const sx = img.width / w;
  const sy = img.height / h;
  for (let y = 0; y < h; y++) {
    const fy0 = y * sy; const fy1 = fy0 + sy;
    for (let x = 0; x < w; x++) {
      const fx0 = x * sx; const fx1 = fx0 + sx;
      let r = 0; let g = 0; let b = 0; let a = 0; let peso = 0;
      for (let yy = Math.floor(fy0); yy < Math.ceil(fy1); yy++) {
        const wy = Math.min(fy1, yy + 1) - Math.max(fy0, yy);
        for (let xx = Math.floor(fx0); xx < Math.ceil(fx1); xx++) {
          const wx = Math.min(fx1, xx + 1) - Math.max(fx0, xx);
          const k = wx * wy;
          const i = (yy * img.width + xx) * 4;
          const al = img.data[i + 3] / 255;
          r += img.data[i] * al * k; g += img.data[i + 1] * al * k; b += img.data[i + 2] * al * k;
          a += al * k; peso += k;
        }
      }
      const o = (y * w + x) * 4;
      if (a > 0) {
        out.data[o] = Math.round(r / a); out.data[o + 1] = Math.round(g / a); out.data[o + 2] = Math.round(b / a);
      }
      out.data[o + 3] = Math.round((a / peso) * 255);
    }
  }
  return out;
}

function anchoMax (img, w) {
  return img.width <= w ? img : reducir(img, w, Math.round(img.height * (w / img.width)));
}

/**
 * Minecraft no hace mipmaps de estas texturas: reducida a menos de la mitad
 * en pantalla, se ve con dientes. Se guardan en varios tamanos (cada uno la
 * mitad del anterior) y el mod elige el que toca (ver render/Logos.java).
 */
function niveles (img, base, cuantos) {
  for (let n = 0; n < cuantos; n++) {
    const w = Math.round(img.width / 2 ** n);
    const h = Math.round(img.height / 2 ** n);
    guardar(n === 0 ? img : reducir(img, w, h), path.join(MOD, 'textures', 'gui', `${base}_${n}.png`));
  }
}

/* ---------------------------------------------------------------- trabajo */

console.log('Logo de Backrooms');
const original = cargar('backrooms-logo-original.png');
// El suelo de la puerta refleja el magenta: alli no hay fondo, hay penumbra.
const backrooms = quitarMagenta(original, [[372, 286, 652, 681]]);
const logo = ajustado(backrooms, 6);
guardar(anchoMax(logo, 720), path.join(RAIZ, 'assets', 'logo-backrooms.png'));
niveles(logo, 'logo', 4);

console.log('Icono (la puerta del logo)');
const puerta = cuadrado(ajustado(recortar(backrooms, { x0: 0, y0: 0, x1: original.width, y1: 684 })), 10);
guardar(reducir(puerta, 512, 512), path.join(RAIZ, 'assets', 'icon.png'));
for (const lado of [16, 32, 48, 128, 256]) {
  guardar(reducir(puerta, lado, lado), path.join(MOD, 'icons', `icon_${lado}x${lado}.png`));
}
guardar(reducir(puerta, 128, 128), path.join(MOD, 'icon.png'));

console.log('Logo de PeakMC Studio');
const studio = ajustado(quitarMagenta(cargar('peakmc-studio-original.png')), 6);
guardar(anchoMax(studio, 640), path.join(RAIZ, 'assets', 'logo-peakmc-studio.png'));
niveles(anchoMax(studio, 1024), 'peakmc_studio', 5);
