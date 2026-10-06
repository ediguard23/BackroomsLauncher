#version 300 es
precision highp float;
precision highp int;

// El pasillo del Nivel 0 para el cartel animado (ver render.js). Es despertar.fsh del
// trailer con dos cambios: el plano se repite cada 2 celdas en x (la camara avanza
// justo eso en cada vuelta y el GIF empalma sin salto) y la luz la manda render.js.
//
//  X        posicion de la camara en el pasillo
//  T        segundos (bamboleo y el tubo que falla)
//  Luz      1 encendido, 0 apagon
//  Sonrisa  0..1, la cara en la oscuridad
//  Fallo    0..1, el tubo que parpadea

uniform sampler2D Atlas; // 8 casillas de 32 px
uniform vec2 ScreenSize;
uniform float X;
uniform float T;
uniform float Luz;
uniform float Sonrisa;
uniform float Fallo;
uniform vec2 Centro; // donde cae el punto de fuga en la pantalla (-1..1)
out vec4 fragColor;

const int PAPEL = 0, PAPEL_SUCIO = 1, ZOCALO = 2, MOQUETA = 3, TECHO = 4, TUBO = 5, TUBO_OFF = 6, MOJADA = 7;
const float G = 6.0;
const float ALTO = 4.0;
const float FOVK = 0.62;
const float PERIODO = 2.0; // celdas
// mas palida que la del juego: en la paleta de mapas la original cae en naranja
const vec3 NIEBLA = vec3(0.72, 0.70, 0.52);

float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

// la celda que decide el aspecto: se repite cada PERIODO celdas en x
vec2 rep(vec2 c) { return vec2(mod(c.x, PERIODO), c.y); }

vec4 tex(int c, vec2 uv) {
  vec2 px = floor(clamp(uv, 0.0, 0.9999) * 32.0) + 0.5;
  return texture(Atlas, (vec2(float(c) * 32.0, 0.0) + px) / vec2(256.0, 32.0));
}
vec4 cara(int c, vec2 p) {
  vec2 f = fract(p);
  return tex(c, vec2(f.x, 1.0 - f.y));
}

bool pared(vec2 b) {
  vec2 c = floor(b / G);
  vec2 l = b - c * G;
  vec2 k = rep(c);
  if (l.x == 0.0 && l.y == 0.0) return true;
  float salon = h2(k * 1.31 + 7.0);
  if (l.x == 0.0) {
    if (c.y == 0.0) return false; // el pasillo, infinito
    if (salon < 0.3) return false;
    float p = floor(h2(k + 3.1) * 3.0) + 1.0;
    return !(l.y >= p && l.y < p + 2.0);
  }
  if (l.y == 0.0) {
    if (salon < 0.3 && c.y != 0.0 && c.y != 1.0) return false;
    float p = floor(h2(k + 9.7) * 3.0) + 1.0;
    bool abre = h2(k + 4.4) > 0.45;
    return !(abre && l.x >= p && l.x < p + 2.0);
  }
  return false;
}

float panel(vec2 c) {
  if (Luz <= 0.0) return 0.0;
  vec2 k = rep(c);
  if (c.y != 0.0 && h2(k * 2.7 + 1.0) < 0.12) return 0.0;   // fundidos en las salas
  if (k.x == 1.0 && c.y == 0.0) return Fallo * Luz;          // el que falla
  return Luz;
}

bool esPanel(vec2 b) {
  vec2 l = b - floor(b / G) * G;
  return (l.x == 2.0 || l.x == 3.0) && (l.y == 2.0 || l.y == 3.0);
}

float brilloNivel(float L) {
  float f = clamp(L, 0.0, 15.0) / 15.0;
  return f / (4.0 - 3.0 * f);
}

float sombreado(vec3 n) {
  return n.y > 0.5 ? 1.0 : n.y < -0.5 ? 0.5 : abs(n.z) > 0.5 ? 0.8 : 0.6;
}

vec3 luz(vec3 p, vec3 n) {
  vec3 q = p + n * 0.5;
  vec2 c0 = floor(q.xz / G);
  float mejor = 0.0;
  for (int i = -1; i <= 1; i++) {
    for (int j = -1; j <= 1; j++) {
      vec2 c = c0 + vec2(float(i), float(j));
      float on = panel(c);
      if (on <= 0.0) continue;
      vec2 centro = clamp(q.xz, c * G + 2.5, c * G + 3.5);
      float d = abs(q.x - centro.x) + abs(q.z - centro.y) + abs(q.y - 4.5);
      mejor = max(mejor, (15.0 - d) * on);
    }
  }
  float g = brilloNivel(mejor);
  float amb = 0.2 * Luz;
  return vec3(1.0, 0.97, 0.86) * (g + amb * (1.0 - g));
}

float muros(vec3 ro, vec3 rd, out vec3 n) {
  vec2 p = ro.xz;
  vec2 d = rd.xz;
  vec2 b = floor(p);
  vec2 paso = sign(d);
  vec2 tDelta = abs(1.0 / d);
  vec2 tMax = (b + max(paso, 0.0) - p) / d;
  float t = 0.0;
  vec2 nn = vec2(0.0);
  for (int i = 0; i < 140; i++) {
    if (pared(b)) { n = vec3(nn.x, 0.0, nn.y); return t; }
    if (tMax.x < tMax.y) { t = tMax.x; tMax.x += tDelta.x; b.x += paso.x; nn = vec2(-paso.x, 0.0); }
    else { t = tMax.y; tMax.y += tDelta.y; b.y += paso.y; nn = vec2(0.0, -paso.y); }
    if (t > 90.0) break;
  }
  n = vec3(0.0);
  return 1e4;
}

vec3 sombrearPared(vec3 p, vec3 n) {
  vec2 uv = abs(n.x) > 0.5 ? vec2(p.z, p.y) : vec2(p.x, p.y);
  vec2 xz = vec2(mod(p.x, G * PERIODO), p.z);
  vec2 uvr = abs(n.x) > 0.5 ? vec2(p.z, p.y) : vec2(xz.x, p.y);
  int bloque = p.y < 1.0 ? ZOCALO : (h2(floor(uvr) + floor(xz * 0.2) * 3.0) > 0.86 ? PAPEL_SUCIO : PAPEL);
  return cara(bloque, uv).rgb * luz(p, n) * sombreado(n);
}

vec3 escena(vec3 ro, vec3 rd, out float dist) {
  vec3 n;
  float tw = muros(ro, rd, n);
  float tp = 1e4;
  vec3 np = vec3(0.0);
  if (rd.y < 0.0) { tp = (0.0 - ro.y) / rd.y; np = vec3(0.0, 1.0, 0.0); }
  else if (rd.y > 0.0) { tp = (ALTO - ro.y) / rd.y; np = vec3(0.0, -1.0, 0.0); }
  vec3 col;
  if (tw < tp) {
    vec3 p = ro + rd * tw;
    col = sombrearPared(p, n);
    dist = tw;
  } else {
    vec3 p = ro + rd * tp;
    dist = tp;
    if (np.y > 0.0) {
      vec2 xz = vec2(mod(p.x, G * PERIODO), p.z);
      int b = h2(floor(xz * 0.25) + 5.0) > 0.93 ? MOJADA : MOQUETA;
      col = cara(b, p.xz).rgb * luz(p, np) * sombreado(np);
    } else {
      vec2 b = floor(p.xz);
      if (esPanel(b)) {
        float on = panel(floor(b / G));
        col = mix(cara(TUBO_OFF, p.xz).rgb * luz(p, np) * 0.5, cara(TUBO, p.xz).rgb * 1.15, on);
      } else {
        col = cara(TECHO, p.xz).rgb * luz(p, np) * sombreado(np);
      }
    }
  }
  float f = clamp((dist - 4.0) / 48.0, 0.0, 1.0);
  col = mix(col, NIEBLA * 0.9 * Luz, f);
  return col;
}

// la sonrisa del trailer, a 6 bloques por delante en el pasillo
vec3 sonrisa(vec2 uv, vec3 ro, vec3 f, vec3 r, vec3 u) {
  if (Sonrisa <= 0.0) return vec3(0.0);
  vec3 S = vec3(ro.x + 6.0, 1.7, 3.0);
  vec3 op = S - ro;
  float z = dot(op, f);
  if (z < 0.5) return vec3(0.0);
  vec2 c = vec2(dot(op, r), dot(op, u)) / (z * FOVK);
  vec2 q = (uv - c) * z * FOVK / 1.8;
  float brillo = 0.0;
  for (int k = -1; k <= 1; k += 2) {
    vec2 e = (q - vec2(float(k) * 0.17, 0.14)) / vec2(0.075, 0.03);
    brillo = max(brillo, smoothstep(1.0, 0.6, length(e)));
  }
  float x = q.x;
  if (abs(x) < 0.36) {
    float arriba = -0.05 + 0.75 * x * x;
    float abajo = -0.16 + 1.25 * x * x;
    if (q.y < arriba && q.y > abajo) {
      float diente = step(0.18, fract(x / 0.045 + 0.5));
      float encia = smoothstep(0.0, 0.015, arriba - q.y) * smoothstep(0.0, 0.015, q.y - abajo);
      brillo = max(brillo, diente * encia);
    }
  }
  float halo = exp(-dot(q, q) * 9.0) * 0.14;
  return vec3(0.95, 0.94, 0.88) * (brillo + halo) * Sonrisa;
}

void main() {
  vec2 res = ScreenSize;
  vec2 uv = (gl_FragCoord.xy / res * 2.0 - 1.0) * vec2(res.x / res.y, 1.0);
  uv -= Centro * vec2(res.x / res.y, 1.0);

  // 16 pasos por vuelta (12 bloques): el bamboleo tambien empalma
  float paso = X * 16.0 / (G * PERIODO);
  float bob = abs(sin(paso * 3.14159)) * 0.045;
  vec3 ro = vec3(X, 1.62 + bob, 3.0);
  float yaw = 0.035 * sin(T * 6.2831853 / 6.0);
  float pitch = radians(-1.5) + 0.012 * sin(T * 6.2831853 / 3.0);
  float roll = sin(paso * 3.14159 * 0.5) * 0.01;

  vec3 f = normalize(vec3(cos(pitch) * cos(yaw), sin(pitch), cos(pitch) * sin(yaw)));
  vec3 r = normalize(cross(f, vec3(0.0, 1.0, 0.0)));
  vec3 up = cross(r, f);
  vec3 r2 = r * cos(roll) + up * sin(roll);
  vec3 u2 = up * cos(roll) - r * sin(roll);
  vec3 rd = normalize(f + (uv.x * r2 + uv.y * u2) * FOVK);

  float dist;
  vec3 col = escena(ro, rd, dist);
  col += sonrisa(uv, ro, f, r2, u2);
  fragColor = vec4(clamp(col * vec3(1.24, 1.16, 1.02), 0.0, 1.0), 1.0); // mas luz y algo mas calido
}
