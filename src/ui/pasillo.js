'use strict';
/**
 * Pasillo infinito del Nivel 0, dibujado en tiempo real con un shader.
 *
 * Cada pixel lanza un rayo contra las cuatro caras de un pasillo recto
 * (paredes, techo y suelo) y las pinta por procedimiento: papel pintado con
 * manchas de humedad, placas del techo con tubos fluorescentes (algunos
 * parpadean y alguno esta fundido), moqueta mojada y puertas laterales que
 * dan a otros pasillos. La camara avanza despacio, como quien camina.
 *
 * Se dibuja a media resolucion: mas barato y con el emborronado de una
 * camara de video.
 *
 * Tambien sabe dibujar las Piscinas (Nivel 37), el proximo evento: la
 * interfaz salta a ese pasillo de vez en cuando como avance (ponerNivel).
 */

(function () {
  const VERT = `
attribute vec2 p;
void main() { gl_Position = vec4(p, 0.0, 1.0); }`;

  const FRAG = `
precision highp float;
uniform vec2 uRes;
uniform float uTime;
uniform float uLuz;
uniform float uZ;

float h1(float n) { return fract(sin(n) * 43758.5453123); }
float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float ruido(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(h2(i), h2(i + vec2(1.0, 0.0)), f.x), mix(h2(i + vec2(0.0, 1.0)), h2(i + vec2(1.0, 1.0)), f.x), f.y);
}
float fbm(vec2 p) {
  float v = 0.0, a = 0.5;
  for (int i = 0; i < 4; i++) { v += a * ruido(p); p *= 2.03; a *= 0.5; }
  return v;
}

const float ANCHO = 1.7;
const float TECHO = 1.25;
const float SUELO = -1.0;
const float PASO = 3.2;

float tubo(float idx) {
  float vivo = step(0.07, h1(idx * 3.71 + 1.0));
  if (h1(idx * 7.13) < 0.84) return vivo;
  float t = floor(uTime * 13.0 + idx * 3.0);
  return vivo * step(0.32, h1(t + idx)) * (0.55 + 0.45 * h1(t * 1.3));
}

vec3 luces(vec3 p, vec3 n) {
  float base = floor(p.z / PASO);
  vec3 acc = vec3(0.0);
  for (int k = -2; k <= 3; k++) {
    float idx = base + float(k);
    vec3 lp = vec3(0.0, TECHO - 0.05, idx * PASO + PASO * 0.5);
    vec3 d = lp - p;
    float d2 = dot(d, d);
    float lam = max(dot(n, normalize(d)), 0.0);
    acc += vec3(1.0, 0.95, 0.78) * tubo(idx) * lam / (1.0 + d2 * 0.5);
  }
  return acc;
}

void main() {
  vec2 frag = gl_FragCoord.xy;
  vec2 uv = (frag - 0.5 * uRes) / uRes.y;

  // banda de tracking del VHS: desplaza unas lineas en horizontal
  float yb = fract(uTime * 0.045);
  float banda = smoothstep(0.035, 0.0, abs(frag.y / uRes.y - yb));
  uv.x += banda * (ruido(vec2(frag.y * 0.35, uTime * 24.0)) - 0.5) * 0.05;

  vec3 ro = vec3(sin(uTime * 0.11) * 0.12, 0.04 + sin(uTime * 1.6) * 0.012, uZ);
  float giro = sin(uTime * 0.07) * 0.05;
  vec3 rd = normalize(vec3(uv.x, uv.y - 0.04, 1.25));
  rd.xz = mat2(cos(giro), -sin(giro), sin(giro), cos(giro)) * rd.xz;

  float tx = abs(rd.x) < 1e-4 ? 1e4 : ((rd.x > 0.0 ? ANCHO : -ANCHO) - ro.x) / rd.x;
  float ty = abs(rd.y) < 1e-4 ? 1e4 : ((rd.y > 0.0 ? TECHO : SUELO) - ro.y) / rd.y;
  float t = min(tx, ty);
  vec3 p = ro + rd * t;

  vec3 col;
  vec3 n;
  float emite = 0.0;

  if (tx < ty) {
    n = vec3(-sign(rd.x), 0.0, 0.0);
    float lado = sign(rd.x);
    float seg = mod(p.z + lado * 6.0, 19.0);
    vec2 w = vec2(p.z, p.y);
    if (seg > 7.0 && seg < 9.3 && p.y < 0.62) {
      // hueco de puerta: otro pasillo en penumbra
      float fondo = fbm(w * 2.5);
      col = vec3(0.17, 0.14, 0.06) * (0.55 + 0.5 * fondo);
      float marco = min(seg - 7.0, 9.3 - seg);
      col *= 0.55 + 0.45 * smoothstep(0.0, 0.18, marco);
      n = vec3(0.0, 1.0, 0.0);
    } else {
      float rayas = 0.5 + 0.5 * sin(w.x * 19.0);
      float motivo = smoothstep(0.62, 0.7, abs(sin(w.x * 9.0 + sin(w.y * 13.0) * 0.7)) * abs(sin(w.y * 9.5)));
      vec3 papel = vec3(0.80, 0.70, 0.37);
      papel *= 0.93 + 0.05 * rayas;
      papel = mix(papel, papel * 0.83, motivo * 0.55);
      papel *= 0.84 + 0.26 * fbm(w * vec2(0.6, 1.3) + 3.0);
      float humedad = smoothstep(-0.35, -0.95, p.y) * fbm(w * 1.7 + 11.0);
      papel = mix(papel, vec3(0.42, 0.35, 0.14), humedad * 0.6);
      if (p.y < -0.9) papel = vec3(0.40, 0.34, 0.17) * (0.9 + 0.1 * ruido(w * 30.0));
      col = papel;
    }
  } else if (rd.y > 0.0) {
    n = vec3(0.0, -1.0, 0.0);
    vec2 c = p.xz;
    vec2 g = abs(fract(c / vec2(0.85, 1.07)) - 0.5);
    float junta = smoothstep(0.46, 0.5, max(g.x, g.y));
    vec3 placa = vec3(0.78, 0.74, 0.56) * (0.88 + 0.12 * fbm(c * 3.5));
    placa = mix(placa, vec3(0.55, 0.48, 0.26), smoothstep(0.6, 0.8, fbm(c * 0.9 + 5.0)) * 0.5);
    col = mix(placa, placa * 0.55, junta);
    float idx = floor(p.z / PASO);
    float lz = mod(p.z, PASO) - PASO * 0.5;
    if (abs(p.x) < 0.42 && abs(lz) < 0.52) {
      float f = tubo(idx);
      col = mix(vec3(0.30, 0.28, 0.21), vec3(1.7, 1.62, 1.3), f);
      emite = 1.0;
    }
  } else {
    n = vec3(0.0, 1.0, 0.0);
    float fibra = ruido(p.xz * 70.0) * 0.5 + ruido(p.xz * 14.0) * 0.5;
    vec3 moqueta = vec3(0.56, 0.47, 0.23) * (0.78 + 0.3 * fibra);
    float mojado = smoothstep(0.52, 0.74, fbm(p.xz * 0.33 + 7.0));
    col = mix(moqueta, moqueta * 0.5, mojado * 0.75);
  }

  vec3 luz = emite > 0.5 ? vec3(1.0) : vec3(0.34, 0.31, 0.18) + luces(p, n) * 2.5;
  col *= luz * uLuz;

  float niebla = 1.0 - exp(-t * 0.06);
  col = mix(col, vec3(0.46, 0.4, 0.19) * uLuz, niebla);

  col = pow(max(col, 0.0), vec3(0.95, 1.0, 1.18));
  vec2 q = frag / uRes;
  col *= 0.42 + 0.58 * pow(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.3);
  col *= 0.93 + 0.07 * sin(frag.y * 3.14159);
  col += (h2(frag + fract(uTime) * 91.0) - 0.5) * 0.06;
  gl_FragColor = vec4(col, 1.0);
}`;

  // Las Piscinas (Nivel 37), el proximo evento: el mismo shader que el menu del
  // mod para ese nivel (mod/.../shaders/core/piscinas.fsh), pasado a WebGL.
  const FRAG_PISCINAS = `
precision highp float;
uniform vec2 uRes;
uniform float uTime;
uniform float uLuz;
uniform float uZ;

float h1(float n) { return fract(sin(n) * 43758.5453123); }
float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
vec2 h22(vec2 p) { return fract(sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) * 43758.5453); }
float ruido(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(h2(i), h2(i + vec2(1.0, 0.0)), f.x), mix(h2(i + vec2(0.0, 1.0)), h2(i + vec2(1.0, 1.0)), f.x), f.y);
}

const float ANCHO = 1.35;   // medio ancho del pasillo
const float TECHO = 1.55;
const float AGUA = -0.55;
const float FONDO = -1.15;
const float ARCO = 6.4;     // separacion de los arcos laterales

/* Causticas: bordes de celdas de Voronoi que se mueven (F2 - F1 pequeno = linea de luz). */
float celdas(vec2 p, float t) {
  vec2 i = floor(p), f = fract(p);
  float f1 = 8.0, f2 = 8.0;
  for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
    vec2 g = vec2(float(x), float(y));
    vec2 o = h22(i + g);
    o = 0.5 + 0.42 * sin(t * (0.6 + o * 0.5) + 6.2831 * o);
    float d = length(g + o - f);
    if (d < f1) { f2 = f1; f1 = d; } else if (d < f2) { f2 = d; }
  }
  return 1.0 - smoothstep(0.0, 0.11, f2 - f1);
}
float caustica(vec2 p, float t) {
  float a = celdas(p * 1.6, t);
  float b = celdas(p * 2.3 + 7.3, t * 1.3);
  return a * 0.65 + b * 0.5 + a * b;
}

/* Olas del agua: altura y su gradiente. */
vec2 olas(vec2 p, float t, float zCam) {
  vec2 g = vec2(0.0);
  // gotas: cada pocos segundos cae una delante de la camara y abre un anillo
  for (int k = 0; k < 3; k++) {
    float fk = float(k);
    float periodo = 3.1 + fk * 1.3;
    float ciclo = floor((t + fk * 1.7) / periodo);
    float edad = mod(t + fk * 1.7, periodo);
    vec2 c = vec2((h1(ciclo * 7.1 + fk) - 0.5) * 2.0, zCam + 2.5 + h1(ciclo * 3.3 + fk * 5.0) * 5.0);
    vec2 dv = p - c;
    float rr = length(dv) + 1e-4;
    float frente = edad * 0.55;
    float onda = cos((rr - frente) * 34.0) * exp(-abs(rr - frente) * 9.0) * exp(-edad * 0.9);
    g += (dv / rr) * onda * 0.09;
  }
  vec2 k1 = vec2(1.7, 2.9), k2 = vec2(-3.1, 1.3), k3 = vec2(4.3, -3.7), k4 = vec2(-6.1, -5.3);
  g += k1 * cos(dot(k1, p) - t * 1.1) * 0.010;
  g += k2 * cos(dot(k2, p) - t * 1.4) * 0.008;
  g += k3 * cos(dot(k3, p) - t * 1.9) * 0.005;
  g += k4 * cos(dot(k4, p) - t * 2.6) * 0.003;
  return g;
}

/* Azulejo: color segun la posicion en la superficie (u, v) en metros. */
vec3 azulejo(vec2 uv, float tam) {
  vec2 c = uv / tam;
  vec2 f = abs(fract(c) - 0.5);
  float junta = smoothstep(0.455, 0.49, max(f.x, f.y));
  float var = h2(floor(c));
  vec3 base = mix(vec3(0.95, 0.97, 0.96), vec3(0.88, 0.94, 0.95), var);
  base *= 0.96 + 0.04 * ruido(uv * 9.0);
  return mix(base, vec3(0.55, 0.68, 0.70), junta);
}

/* Hueco de arco en una pared: devuelve 1 dentro del hueco. */
float hueco(float z, float y, float lado) {
  float zz = mod(z + (lado > 0.0 ? ARCO * 0.5 : 0.0), ARCO) - ARCO * 0.5;
  float medio = 0.82;
  float alto = 0.3;
  if (abs(zz) > medio + 0.07) return 0.0;
  float r = max(y - alto, 0.0);
  float dist = sqrt(zz * zz + r * r);
  if (y < alto) dist = abs(zz);
  if (dist < medio) return 1.0;          // hueco
  if (dist < medio + 0.07) return 0.5;   // marco
  return 0.0;
}

/* Sombrea un punto de pared o techo (sin agua). */
vec3 sombrear(vec3 p, vec3 rd, float esPared, float lado, float t, out float emite) {
  emite = 0.0;
  vec3 col;
  if (esPared > 0.5) {
    float hu = hueco(p.z, p.y, lado);
    if (hu > 0.25 && hu < 0.75) {
      // marco del arco: azulejo de canto, algo mas oscuro
      return vec3(0.62, 0.74, 0.76) * (0.9 + 0.1 * ruido(vec2(p.z, p.y) * 20.0));
    }
    if (hu > 0.75) {
      // luz de dia al otro lado del arco
      emite = 1.0;
      float glow = 0.85 + 0.15 * ruido(vec2(p.z * 2.0, p.y * 2.0 + t * 0.2));
      return vec3(1.55, 1.62, 1.6) * glow;
    }
    col = azulejo(vec2(p.z, p.y), 0.22);
    // borde mojado cerca del agua y causticas reflejadas
    float cerca = exp(-(p.y - AGUA) * 2.2);
    col *= 1.0 - 0.12 * smoothstep(0.08, 0.0, p.y - AGUA);
    col += vec3(0.55, 0.85, 0.9) * caustica(vec2(p.z, p.y * 1.4), t) * cerca * 0.55;
  } else {
    col = azulejo(p.xz, 0.3);
    col += vec3(0.5, 0.8, 0.85) * caustica(p.xz * 0.9, t * 0.8) * 0.4;
  }
  // luz que entra por los arcos: mas clara cerca de cada uno
  float zz = mod(p.z, ARCO * 0.5) - ARCO * 0.25;
  float luz = 0.9 + 0.32 * exp(-zz * zz * 1.2);
  return col * luz;
}

/* Lanza un rayo (sin agua) contra paredes y techo; para reflejos. */
vec3 trazarSeco(vec3 ro, vec3 rd, float t) {
  float tx = abs(rd.x) < 1e-4 ? 1e4 : ((rd.x > 0.0 ? ANCHO : -ANCHO) - ro.x) / rd.x;
  float ty = rd.y > 1e-4 ? (TECHO - ro.y) / rd.y : 1e4;
  float d = min(tx, ty);
  if (d > 45.0) return vec3(0.93, 0.98, 1.0);
  vec3 p = ro + rd * d;
  float emite;
  vec3 c = sombrear(p, rd, tx < ty ? 1.0 : 0.0, sign(rd.x), t, emite);
  return mix(c, vec3(0.93, 0.98, 1.0), 1.0 - exp(-d * 0.05));
}

void main() {
  vec2 frag = gl_FragCoord.xy;
  vec2 uv = (frag - 0.5 * uRes) / uRes.y;

  float yb = fract(uTime * 0.04);
  float banda = smoothstep(0.03, 0.0, abs(frag.y / uRes.y - yb));
  uv.x += banda * (ruido(vec2(frag.y * 0.35, uTime * 24.0)) - 0.5) * 0.04;

  float t = uTime;
  vec3 ro = vec3(sin(t * 0.13) * 0.28, -0.05 + sin(t * 0.9) * 0.025, uZ);
  float giro = sin(t * 0.09) * 0.10;
  float cabeceo = sin(t * 0.07) * 0.03 - 0.06;
  vec3 rd = normalize(vec3(uv.x, uv.y + cabeceo, 1.15));
  rd.xz = mat2(cos(giro), -sin(giro), sin(giro), cos(giro)) * rd.xz;

  float tx = abs(rd.x) < 1e-4 ? 1e4 : ((rd.x > 0.0 ? ANCHO : -ANCHO) - ro.x) / rd.x;
  float tyT = rd.y > 1e-4 ? (TECHO - ro.y) / rd.y : 1e4;
  float tyA = rd.y < -1e-4 ? (AGUA - ro.y) / rd.y : 1e4;
  float d = min(tx, min(tyT, tyA));
  vec3 p = ro + rd * d;
  vec3 col;
  float emite = 0.0;

  if (d > 45.0) {
    col = vec3(0.93, 0.98, 1.0);
  } else if (tyA <= tx && tyA <= tyT) {
    // agua: reflejo + lo que se ve debajo, mezclados con Fresnel
    vec2 g = olas(p.xz, t, ro.z);
    vec3 n = normalize(vec3(-g.x, 1.0, -g.y));
    vec3 rr = reflect(rd, n);
    vec3 refl = trazarSeco(p + rr * 0.001, rr, t);
    vec3 rf = refract(rd, n, 0.75);
    float tf = rf.y < -1e-3 ? (FONDO - p.y) / rf.y : 1e4;
    float txf = abs(rf.x) < 1e-4 ? 1e4 : ((rf.x > 0.0 ? ANCHO : -ANCHO) - p.x) / rf.x;
    float df = min(tf, txf);
    vec3 q = p + rf * df;
    vec3 bajo = tf < txf ? azulejo(q.xz, 0.3) : azulejo(vec2(q.z, q.y), 0.22);
    bajo += vec3(0.6, 0.95, 1.0) * caustica(q.xz * 1.3, t * 1.2) * 0.6;
    bajo *= mix(vec3(1.0), vec3(0.35, 0.78, 0.82), clamp(df * 1.6, 0.0, 1.0));
    float fres = 0.04 + 0.96 * pow(1.0 - max(dot(-rd, n), 0.0), 5.0);
    col = mix(bajo, refl, clamp(fres * 1.4 + 0.12, 0.0, 1.0));
  } else {
    col = sombrear(p, rd, tx < tyT ? 1.0 : 0.0, sign(rd.x), t, emite);
  }

  // niebla blanca: lo lejano se disuelve en luz
  float niebla = 1.0 - exp(-d * 0.055);
  col = mix(col, vec3(0.93, 0.98, 1.0), niebla);

  // grado: cian suave, negros levantados, un poco sobreexpuesto
  col = pow(max(col, 0.0), vec3(1.05, 0.98, 0.95));
  col = col / (1.0 + col * 0.18);
  vec2 q = frag / uRes;
  col *= 0.68 + 0.32 * pow(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.3);
  col *= 0.95 + 0.05 * sin(frag.y * 3.14159);
  col += (h2(frag + fract(uTime) * 91.0) - 0.5) * 0.05;
  gl_FragColor = vec4(col * uLuz, 1.0);
}`;

  class Pasillo {
    constructor (canvas) {
      this.canvas = canvas;
      this.gl = canvas.getContext('webgl', { antialias: false, preserveDrawingBuffer: false, powerPreference: 'low-power' });
      this.activo = Boolean(this.gl) && this._compilar();
      this.luz = 1;
      this.mover = true;
      this.velocidad = 1;
      this.z = 0;
      this.ultimo = 0;
      this.t0 = performance.now();
      this.corriendo = false;
      this._redimensionar = this._redimensionar.bind(this);
      this._frame = this._frame.bind(this);
      if (this.activo) {
        window.addEventListener('resize', this._redimensionar);
        this._redimensionar();
      }
    }

    _shader (tipo, src) {
      const gl = this.gl;
      const s = gl.createShader(tipo);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
        console.error(gl.getShaderInfoLog(s));
        return null;
      }
      return s;
    }

    _programa (frag) {
      const gl = this.gl;
      const vs = this._shader(gl.VERTEX_SHADER, VERT);
      const fs = this._shader(gl.FRAGMENT_SHADER, frag);
      if (!vs || !fs) return null;
      const prog = gl.createProgram();
      gl.attachShader(prog, vs);
      gl.attachShader(prog, fs);
      gl.bindAttribLocation(prog, 0, 'p');
      gl.linkProgram(prog);
      if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) return null;
      return {
        prog,
        res: gl.getUniformLocation(prog, 'uRes'),
        time: gl.getUniformLocation(prog, 'uTime'),
        luz: gl.getUniformLocation(prog, 'uLuz'),
        z: gl.getUniformLocation(prog, 'uZ')
      };
    }

    _compilar () {
      const gl = this.gl;
      this.programas = { 0: this._programa(FRAG), 37: this._programa(FRAG_PISCINAS) };
      if (!this.programas[0]) return false;
      const buf = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, buf);
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
      gl.enableVertexAttribArray(0);
      gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
      this.nivel = 0;
      this.u = this.programas[0];
      gl.useProgram(this.u.prog);
      return true;
    }

    /** true si este equipo puede dibujar las Piscinas (su shader es mas pesado). */
    get conPiscinas () {
      return Boolean(this.activo && this.programas[37]);
    }

    /** Cambia de nivel: 0 (el del evento) o 37 (las Piscinas, el proximo). */
    ponerNivel (nivel) {
      const p = this.activo && this.programas[nivel];
      if (!p || this.nivel === nivel) return;
      this.nivel = nivel;
      this.u = p;
      this.gl.useProgram(p.prog);
      if (!this.corriendo) this._dibujar((performance.now() - this.t0) / 1000);
    }

    _redimensionar () {
      const escala = 0.5;
      const w = Math.max(320, Math.floor(window.innerWidth * escala));
      const h = Math.max(180, Math.floor(window.innerHeight * escala));
      this.canvas.width = w;
      this.canvas.height = h;
      this.gl.viewport(0, 0, w, h);
      if (!this.corriendo) this._dibujar((performance.now() - this.t0) / 1000);
    }

    _dibujar (t) {
      const gl = this.gl;
      gl.uniform2f(this.u.res, this.canvas.width, this.canvas.height);
      gl.uniform1f(this.u.time, t);
      gl.uniform1f(this.u.luz, this.luz);
      gl.uniform1f(this.u.z, this.z);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    }

    _frame (ahora) {
      if (!this.corriendo) return;
      const dt = Math.min(0.1, (ahora - (this.ultimo || ahora)) / 1000);
      this.ultimo = ahora;
      if (this.mover) this.z += dt * (this.nivel === 37 ? 0.45 : 0.55) * this.velocidad;
      this._dibujar((ahora - this.t0) / 1000);
      requestAnimationFrame(this._frame);
    }

    iniciar () {
      if (!this.activo || this.corriendo) return;
      this.corriendo = true;
      this.ultimo = 0;
      requestAnimationFrame(this._frame);
    }

    parar () {
      this.corriendo = false;
      if (this.activo) this._dibujar((performance.now() - this.t0) / 1000);
    }
  }

  /** Grano de video: ruido de baja resolucion refrescado a ~15 fps. */
  function grano (canvas) {
    const ctx = canvas.getContext('2d');
    canvas.width = 192;
    canvas.height = 108;
    const img = ctx.createImageData(canvas.width, canvas.height);
    let activo = true;
    let ultimo = 0;
    function paso (ahora) {
      if (activo && ahora - ultimo > 66) {
        ultimo = ahora;
        const d = img.data;
        for (let i = 0; i < d.length; i += 4) {
          const v = (Math.random() * 255) | 0;
          d[i] = d[i + 1] = d[i + 2] = v;
          d[i + 3] = 255;
        }
        ctx.putImageData(img, 0, 0);
      }
      requestAnimationFrame(paso);
    }
    requestAnimationFrame(paso);
    return { set activo (v) { activo = v; } };
  }

  window.Pasillo = Pasillo;
  window.crearGrano = grano;
})();
