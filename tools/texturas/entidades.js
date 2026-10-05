'use strict';
/**
 * Modelos y texturas de las entidades del evento (mod evento/), dibujados
 * por codigo: la Bacteria y el Smiler.
 *
 *   node tools/texturas/entidades.js [carpeta-de-vistas]
 *
 * Cada modelo es una lista de piezas en el espacio de modelos de Minecraft
 * (y hacia abajo, 16 = un bloque, y=24 los pies, la cara mira a -z). De la
 * lista salen tres cosas, para que nunca se desincronicen:
 *  - la clase Java con la malla (cliente/entidad/Malla<Nombre>.java),
 *  - la textura (textures/entity/<nombre>.png), con cada cubo en su sitio
 *    del atlas (UV de caja de Minecraft) y pintado segun su material,
 *  - si se pide, una vista previa en PNG (frente, lado y tres cuartos).
 *
 * La Bacteria sigue el estilo de la de Escape the Backrooms (y de la de
 * ModelEngine que el usuario tomo de referencia), pero es un modelo propio:
 * espina fina y encorvada, brazos larguisimos que llegan al suelo con garras
 * de tres dedos, piernas quebradas y una cabeza de caja con la boca abierta
 * llena de dientes. Mide unos 3,3 bloques: cabe en los pasillos de 4.
 */

const fs = require('fs');
const path = require('path');
const png = require('../imagenes/png');
const { hash, ruido, escala, Lienzo, guardar } = require('./lienzo');

const RAIZ = path.join(__dirname, '..', '..');
const ASSETS = path.join(RAIZ, 'evento', 'src', 'main', 'resources', 'assets', 'backrooms_evento');
const JAVA = path.join(RAIZ, 'evento', 'src', 'main', 'java', 'net', 'backrooms', 'evento', 'cliente', 'entidad');
const VISTAS = process.argv[2];

const g = (d) => d * Math.PI / 180;

/* ============================================================ Bacteria */

// pieza: [nombre, padre, pivote [x,y,z], rotacion en grados [x,y,z], cubos]
// cubo: [x, y, z, ancho, alto, fondo, material, inflado]
const BACTERIA = {
  nombre: 'bacteria',
  tex: [128, 128],
  piezas: [
    ['cadera', null, [0, -2, 2], [0, 0, 0], [
      [-3, -1, -1.5, 6, 2, 3, 'fibra'],
      [-1, -4, -1, 2, 4, 2, 'fibra']
    ]],
    // espina: tres tramos que se encorvan hacia delante
    ['espina', 'cadera', [0, -3, 0], [18, 0, 0], [
      [-1, -10, -1, 2, 10, 2, 'fibra'],
      [-4, -7, -0.5, 8, 1, 1, 'hebra'],
      [-3.5, -3, -0.5, 7, 1, 1, 'hebra'],
      [1, -9, -2, 1, 6, 1, 'hebra']
    ]],
    ['pecho', 'espina', [0, -10, 0], [14, 0, 0], [
      [-1, -9, -1, 2, 9, 2, 'fibra'],
      [-5, -8, -0.5, 10, 1, 1, 'hebra'],
      [-6, -5, -0.5, 12, 1, 1, 'hebra'],
      [-4.5, -2, -0.5, 9, 1, 1, 'hebra'],
      [-2, -7, -1.5, 4, 5, 3, 'fibra']
    ]],
    ['hombros', 'pecho', [0, -9, 0], [-10, 0, 0], [
      [-11, -1.5, -1.5, 22, 3, 3, 'fibra'],
      [-13, -4, -1, 3, 4, 2, 'fibra'],
      [10, -4, -1, 3, 4, 2, 'fibra'],
      [-3, -3, -1, 6, 2, 2, 'fibra'],
      [-8, 1.5, -0.5, 1, 9, 1, 'hebra'],
      [6, 1.5, -0.5, 1, 7, 1, 'hebra'],
      [-5, 1.5, 0.5, 1, 5, 1, 'hebra']
    ]],
    ['cuello', 'hombros', [0, -1.5, 0], [-28, 0, 0], [
      [-1, -6, -1, 2, 6, 2, 'fibra'],
      [-2, -4, 0, 4, 1, 1, 'hebra']
    ]],
    // cabeza de caja, larga hacia delante, y la mandibula aparte para abrirla
    ['cabeza', 'cuello', [0, -6, 0], [30, 0, 0], [
      [-5.5, -9, -12, 11, 7, 13, 'cabeza'],
      [-5, -2, -11.5, 10, 2, 1, 'dientes'],
      [-5, -2, -10.5, 1, 2, 8, 'dientes'],
      [4, -2, -10.5, 1, 2, 8, 'dientes'],
      [-4.5, -2.01, -10.5, 9, 0.01, 9, 'boca'],
      [-5, -10, 0, 10, 4, 2, 'cabeza'],
      [-1, -13, -6, 2, 4, 2, 'hebra'],
      [2.5, -12, -2, 1, 3, 1, 'hebra'],
      [-4, -11, -9, 1, 2, 1, 'hebra'],
      [-6.5, -7, -8, 1, 6, 1, 'hebra'],
      [5.5, -6, -4, 1, 7, 1, 'hebra']
    ]],
    ['mandibula', 'cabeza', [0, -2, 0], [26, 0, 0], [
      [-5, 0, -11, 10, 2, 11, 'cabeza'],
      [-4.5, -2, -10.5, 9, 2, 1, 'dientes'],
      [-4.5, -2, -9.5, 1, 2, 8, 'dientes'],
      [3.5, -2, -9.5, 1, 2, 8, 'dientes'],
      [-4, 0, -10, 8, 0.01, 9, 'boca'],
      [-1, 2, -8, 1, 5, 1, 'hebra']
    ]],
    // brazos: hombro, brazo, antebrazo y una mano de tres garras que toca el suelo
    ...brazo('izq', 1),
    ...brazo('der', -1),
    ...pierna('izq', 1),
    ...pierna('der', -1)
  ]
};

function brazo (lado, s) {
  const x = 11 * s;
  return [
    [`brazo_${lado}`, 'hombros', [x, -1, 0], [-8, 0, s * -8], [
      [-1, 0, -1, 2, 17, 2, 'fibra'],
      [s > 0 ? 1 : -2, 4, -0.5, 1, 7, 1, 'hebra']
    ]],
    [`antebrazo_${lado}`, `brazo_${lado}`, [0, 16.5, 0], [-14, 0, s * 4], [
      [-1, 0, -1, 2, 19, 2, 'fibra'],
      [-1.5, 6, -1.5, 3, 3, 3, 'fibra']
    ]],
    [`mano_${lado}`, `antebrazo_${lado}`, [0, 18.5, 0], [24, 0, 0], [
      [-2, 0, -1.5, 4, 2, 3, 'fibra']
    ]],
    // tres garras largas y abiertas, como un tripode
    [`garra_${lado}_a`, `mano_${lado}`, [0, 1.5, -1], [-18, 0, 0], [[-0.5, 0, -0.5, 1, 9, 1, 'garra']]],
    [`garra_${lado}_b`, `mano_${lado}`, [-1.5, 1.5, 0.5], [10, 0, 22], [[-0.5, 0, -0.5, 1, 8, 1, 'garra']]],
    [`garra_${lado}_c`, `mano_${lado}`, [1.5, 1.5, 0.5], [10, 0, -22], [[-0.5, 0, -0.5, 1, 8, 1, 'garra']]]
  ];
}

function pierna (lado, s) {
  return [
    [`muslo_${lado}`, 'cadera', [3 * s, 0, 0], [-26, 0, s * -6], [
      [-1, 0, -1, 2, 11, 2, 'fibra'],
      [-1.5, 3, -1.5, 3, 4, 3, 'fibra']
    ]],
    [`pierna_${lado}`, `muslo_${lado}`, [0, 10.5, 0], [48, 0, 0], [
      [-1, 0, -1, 2, 12, 2, 'fibra'],
      [s > 0 ? 0.5 : -1.5, 3, 0.5, 1, 6, 1, 'hebra']
    ]],
    [`pie_${lado}`, `pierna_${lado}`, [0, 11.5, 0], [-22, 0, 0], [
      [-1, 0, -1, 2, 3, 2, 'fibra'],
      [-1.5, 2, -5, 1, 1, 5, 'garra'],
      [0.5, 2, -5, 1, 1, 5, 'garra']
    ]]
  ];
}

/* ============================================================== Smiler */

// una sombra flotando con la cara: ojos blancos y una sonrisa enorme. Los
// ojos y los dientes brillan aparte (smiler_brillo.png, capa de "ojos").
const SMILER = {
  nombre: 'smiler',
  tex: [64, 64],
  piezas: [
    ['cuerpo', null, [0, 13, 0], [0, 0, 0], [
      [-4, -14, -2, 8, 16, 4, 'sombra'],
      [-3, 2, -1.5, 6, 6, 3, 'sombra'],
      [-2, 8, -1, 4, 5, 2, 'sombra']
    ]],
    ['cabeza', 'cuerpo', [0, -14, 0], [0, 0, 0], [
      [-7, -12, -4, 14, 12, 7, 'cara']
    ]],
    ['brazo_izq', 'cuerpo', [4.5, -13, 0], [0, 0, -12], [
      [0, 0, -1, 2, 20, 2, 'sombra']
    ]],
    ['brazo_der', 'cuerpo', [-4.5, -13, 0], [0, 0, 12], [
      [-2, 0, -1, 2, 20, 2, 'sombra']
    ]]
  ]
};

/* ====================================================== atlas y pintura */

/** Coloca cada cubo en el atlas (UV de caja: 2*(fondo+ancho) x (fondo+alto)). */
function empaquetar (modelo) {
  const [W, H] = modelo.tex;
  let x = 0; let y = 0; let fila = 0;
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      const w = Math.ceil(c[3]); const h = Math.ceil(c[4]); const d = Math.ceil(c[5]);
      const ancho = 2 * (d + w);
      const alto = d + h;
      if (x + ancho > W) { x = 0; y += fila; fila = 0; }
      if (y + alto > H) throw new Error(`${modelo.nombre}: no cabe en ${W}x${H}`);
      c.uv = [x, y];
      x += ancho;
      fila = Math.max(fila, alto);
    }
  }
}

/** Rectangulos de cada cara de un cubo en el atlas (como ModelPart.Cube). */
function carasUV (c) {
  const [u, v] = c.uv;
  const w = Math.ceil(c[3]); const h = Math.ceil(c[4]); const d = Math.ceil(c[5]);
  return {
    up: [u + d, v, u + d + w, v + d],
    down: [u + d + w, v, u + d + w + w, v + d],
    east: [u, v + d, u + d, v + d + h],
    north: [u + d, v + d, u + d + w, v + d + h],
    west: [u + d + w, v + d, u + d + w + d, v + d + h],
    south: [u + d + w + d, v + d, u + d + w + d + w, v + d + h]
  };
}

const MATERIAL = {
  // hilos negros de Hay Bacillus con vetas grises a lo largo
  fibra: (x, y, k) => {
    const veta = ruido(x * 0.6, y * 3, 6, 400 + k, 32);
    const b = 14 + veta * 34 + hash(x, y, 401 + k) * 10;
    return [b, b * 0.98, b * 0.92];
  },
  hebra: (x, y, k) => { const b = 30 + hash(x, y, 410 + k) * 26; return [b, b, b * 0.94]; },
  garra: (x, y, k) => { const b = 46 + hash(x, y, 420 + k) * 30; return [b * 1.05, b, b * 0.9]; },
  cabeza: (x, y, k) => {
    const veta = ruido(x * 2, y * 0.8, 6, 430 + k, 32);
    const b = 18 + veta * 30 + hash(x, y, 431 + k) * 8;
    return [b, b * 0.97, b * 0.9];
  },
  dientes: (x, y, k) => ((x + k) % 2 === 0 ? [236, 228, 200] : (y % 2 ? [120, 110, 90] : [196, 186, 158])),
  boca: () => [70, 10, 12],
  sombra: (x, y, k) => { const b = 6 + hash(x, y, 440 + k) * 10; return [b, b, b + 2]; },
  cara: (x, y, k) => { const b = 8 + hash(x, y, 450 + k) * 8; return [b, b, b + 2]; }
};

function pintar (modelo) {
  const [W, H] = modelo.tex;
  const l = new Lienzo(W, H);
  let k = 0;
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      const mat = MATERIAL[c[6]];
      for (const [lado, [u0, v0, u1, v1]] of Object.entries(carasUV(c))) {
        for (let y = v0; y < v1; y++) {
          for (let x = u0; x < u1; x++) l.punto(x, y, mat(x, y, k, lado));
        }
      }
      k++;
    }
  }
  return l;
}

/** La cara del Smiler: ojos y sonrisa sobre su frente (la cara north del cubo "cara"). */
function caraSmiler (modelo, l, brillo) {
  const c = modelo.piezas.find((p) => p[0] === 'cabeza')[4][0];
  const [u0, v0] = carasUV(c).north;
  const blanco = [250, 250, 244];
  const pon = (x, y, c) => { l.punto(u0 + x, v0 + y, c); brillo.punto(u0 + x, v0 + y, c); };
  // ojos: rendijas inclinadas hacia el centro, como si sonrieran tambien
  for (const [x0, dir] of [[2, 1], [9, -1]]) {
    for (let x = 0; x < 3; x++) {
      const y = 2 + (dir > 0 ? (x === 2 ? 1 : 0) : (x === 0 ? 1 : 0));
      pon(x0 + x, y, blanco);
      pon(x0 + x, y + 1, blanco);
    }
  }
  // la sonrisa: una media luna de lado a lado, dientes arriba y abajo
  for (let x = 0; x < 14; x++) {
    const c = Math.pow((x - 6.5) / 6.5, 2);
    const arriba = 6 + Math.round(2.6 * (1 - c));
    const grueso = x === 0 || x === 13 ? 1 : 3;
    for (let r = 0; r < grueso; r++) {
      const y = arriba + r - (x === 0 || x === 13 ? 0 : 0);
      let col = blanco;
      if (r === 1 && grueso === 3) col = [40, 6, 6];              // la raja entre los dientes
      else if (x % 2 === 1) col = [176, 170, 150];                 // separacion entre dientes
      pon(x, y, col);
    }
  }
}

/* ================================================================= Java */

function java (modelo) {
  const clase = 'Malla' + modelo.nombre[0].toUpperCase() + modelo.nombre.slice(1);
  const f = (n) => `${Math.round(n * 1000) / 1000}F`;
  const lineas = [];
  for (const [nombre, padre, piv, rot, cubos] of modelo.piezas) {
    const lista = cubos.map((c) => `.texOffs(${c.uv[0]}, ${c.uv[1]}).addBox(${f(c[0])}, ${f(c[1])}, ${f(c[2])}, ${f(c[3])}, ${f(c[4])}, ${f(c[5])}${c[7] ? `, new CubeDeformation(${f(c[7])})` : ''})`).join('\n\t\t\t\t');
    const pose = `PartPose.offsetAndRotation(${f(piv[0])}, ${f(piv[1])}, ${f(piv[2])}, ${f(g(rot[0]))}, ${f(g(rot[1]))}, ${f(g(rot[2]))})`;
    lineas.push(`\t\tPartDefinition ${nombre} = ${padre || 'raiz'}.addOrReplaceChild("${nombre}", CubeListBuilder.create()\n\t\t\t\t${lista},\n\t\t\t${pose});`);
  }
  const fuente = `package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Malla de ${modelo.nombre}. GENERADO por tools/texturas/entidades.js: no
 * editar a mano (se pierde al regenerar); cambiar alli y volver a generar.
 */
public final class ${clase} {
	private ${clase}() {
	}

	@SuppressWarnings("unused")
	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
${lineas.join('\n')}
		return LayerDefinition.create(malla, ${modelo.tex[0]}, ${modelo.tex[1]});
	}
}
`;
  fs.mkdirSync(JAVA, { recursive: true });
  fs.writeFileSync(path.join(JAVA, `${clase}.java`), fuente);
  console.log(`  ${clase}.java`);
}

/* ========================================================= vista previa */

function rotZYX (v, r) {
  let [x, y, z] = v;
  let c = Math.cos(r[0]); let s = Math.sin(r[0]);
  [y, z] = [y * c - z * s, y * s + z * c];
  c = Math.cos(r[1]); s = Math.sin(r[1]);
  [x, z] = [x * c + z * s, -x * s + z * c];
  c = Math.cos(r[2]); s = Math.sin(r[2]);
  [x, y] = [x * c - y * s, x * s + y * c];
  return [x, y, z];
}

function vista (modelo, tex, archivo, poses = {}) {
  const porNombre = Object.fromEntries(modelo.piezas.map((p) => [p[0], p]));
  const mundo = (nombre, v) => {
    // de la pieza a sus padres: rotar y luego mover al pivote
    let p = porNombre[nombre];
    let q = v;
    while (p) {
      const extra = poses[p[0]] || [0, 0, 0];
      q = rotZYX(q, p[3].map((a, i) => g(a + extra[i])));
      q = [q[0] + p[2][0], q[1] + p[2][1], q[2] + p[2][2]];
      p = p[1] ? porNombre[p[1]] : null;
    }
    return q;
  };
  const caras = [];
  const ESQ = {
    north: [[0, 0, 0], [1, 0, 0], [1, 1, 0], [0, 1, 0]],
    south: [[1, 0, 1], [0, 0, 1], [0, 1, 1], [1, 1, 1]],
    east: [[0, 0, 1], [0, 0, 0], [0, 1, 0], [0, 1, 1]],
    west: [[1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]],
    up: [[0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]],
    down: [[0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]]
  };
  for (const p of modelo.piezas) {
    for (const c of p[4]) {
      const uvs = carasUV(c);
      for (const [lado, esq] of Object.entries(ESQ)) {
        const pts = esq.map((e) => mundo(p[0], [c[0] + e[0] * c[3], c[1] + e[1] * c[4], c[2] + e[2] * c[5]]));
        const [u0, v0, u1, v1] = uvs[lado];
        caras.push({ pts, uv: [[u0, v0], [u1, v0], [u1, v1], [u0, v1]] });
      }
    }
  }
  const W1 = 360; const H = 520; const giros = [0, 90, -38];
  const out = { width: W1 * 3, height: H, data: new Uint8Array(W1 * 3 * H * 4) };
  for (let i = 0; i < W1 * 3 * H; i++) { const y = Math.floor(i / (W1 * 3)); out.data.set(y > H - 60 ? [150, 128, 70, 255] : [196, 180, 112, 255], i * 4); }
  const esc = 6.2;
  giros.forEach((giro, vi) => {
    const zb = new Float32Array(W1 * H).fill(Infinity);
    for (const c of caras) {
      // y del modelo hacia abajo y 24 = suelo; la camara mira la cara (desde -z)
      const P = c.pts.map((p) => { const q = rotZYX(p, [g(-8), g(giro), 0]); return [W1 / 2 - q[0] * esc, H - 60 - (24 - q[1]) * esc, q[2]]; });
      const a = P[1].map((v, k) => v - P[0][k]); const b = P[3].map((v, k) => v - P[0][k]);
      const nz = a[0] * b[1] - a[1] * b[0];
      const luz = 0.6 + 0.4 * Math.min(1, Math.abs(nz) / (Math.hypot(a[0], a[1]) * Math.hypot(b[0], b[1]) + 1e-6));
      for (const tri of [[0, 1, 2], [0, 2, 3]]) {
        const [A, B, C] = tri.map((i) => P[i]);
        const [ua, ub, uc] = tri.map((i) => c.uv[i]);
        const den = (B[1] - C[1]) * (A[0] - C[0]) + (C[0] - B[0]) * (A[1] - C[1]);
        if (Math.abs(den) < 1e-6) continue;
        for (let y = Math.max(0, Math.floor(Math.min(A[1], B[1], C[1]))); y <= Math.min(H - 1, Math.ceil(Math.max(A[1], B[1], C[1]))); y++) {
          for (let x = Math.max(0, Math.floor(Math.min(A[0], B[0], C[0]))); x <= Math.min(W1 - 1, Math.ceil(Math.max(A[0], B[0], C[0]))); x++) {
            const l1 = ((B[1] - C[1]) * (x + 0.5 - C[0]) + (C[0] - B[0]) * (y + 0.5 - C[1])) / den;
            const l2 = ((C[1] - A[1]) * (x + 0.5 - C[0]) + (A[0] - C[0]) * (y + 0.5 - C[1])) / den;
            const l3 = 1 - l1 - l2;
            if (l1 < -1e-4 || l2 < -1e-4 || l3 < -1e-4) continue;
            const z = l1 * A[2] + l2 * B[2] + l3 * C[2];
            if (z >= zb[y * W1 + x]) continue;
            const u = Math.min(tex.width - 1, Math.floor(l1 * ua[0] + l2 * ub[0] + l3 * uc[0]));
            const v = Math.min(tex.height - 1, Math.floor(l1 * ua[1] + l2 * ub[1] + l3 * uc[1]));
            const ti = (v * tex.width + u) * 4;
            if (tex.data[ti + 3] < 10) continue;
            zb[y * W1 + x] = z;
            const o = (y * W1 * 3 + vi * W1 + x) * 4;
            for (let k = 0; k < 3; k++) out.data[o + k] = Math.min(255, tex.data[ti + k] * luz);
          }
        }
      }
    }
  });
  fs.writeFileSync(archivo, png.escribir(out));
  console.log(`  vista: ${archivo}`);
}

/* =============================================================== todo */

console.log('Entidades');
for (const modelo of [BACTERIA, SMILER]) {
  empaquetar(modelo);
  const tex = pintar(modelo);
  if (modelo === SMILER) {
    const brillo = new Lienzo(...modelo.tex);
    caraSmiler(modelo, tex, brillo);
    guardar(path.join(ASSETS, 'textures', 'entity'), 'smiler_brillo', brillo);
  }
  guardar(path.join(ASSETS, 'textures', 'entity'), modelo.nombre, tex);
  java(modelo);
  if (VISTAS) vista(modelo, tex, path.join(VISTAS, `${modelo.nombre}_vista.png`));
}
