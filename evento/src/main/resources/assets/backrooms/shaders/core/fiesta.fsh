#version 330

// El Nivel Fun (las Feeding Rooms) para el menu de Minecraft: el mismo pasillo de fiesta que el
// fondo del launcher (src/ui/pasillo.js, FRAG_FIESTA). El tiempo y la camara llegan por las UV del
// vertice y el brillo por el rojo del color (el mod manda luz * 0.75).
#moj_import <minecraft:globals.glsl>

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define uRes ScreenSize
#define uTime datos.x
#define uZ datos.y
#define uLuz (color.r / 0.75)

float h1(float n) { return fract(sin(n) * 43758.5453123); }
float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float ruido(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(h2(i), h2(i + vec2(1.0, 0.0)), f.x), mix(h2(i + vec2(0.0, 1.0)), h2(i + vec2(1.0, 1.0)), f.x), f.y);
}

const float ANCHO = 1.6;
const float TECHO = 1.3;
const float SUELO = -1.0;
const float PASO = 3.0;
const float GUIRNALDA = 4.5;
const float GLOBOS = 5.5;

vec3 paleta(float k) {
  k = mod(k, 4.0);
  if (k < 1.0) return vec3(0.89, 0.22, 0.29);
  if (k < 2.0) return vec3(0.96, 0.81, 0.25);
  if (k < 3.0) return vec3(0.23, 0.59, 0.90);
  return vec3(0.29, 0.77, 0.43);
}

float tubo(float idx) {
  float vivo = step(0.05, h1(idx * 3.71 + 1.0));
  if (h1(idx * 7.13) < 0.9) return vivo;
  float t = floor(uTime * 11.0 + idx * 3.0);
  return vivo * step(0.35, h1(t + idx));
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
    acc += vec3(1.0, 0.96, 0.86) * tubo(idx) * lam / (1.0 + d2 * 0.45);
  }
  return acc;
}

void main() {
  vec2 frag = gl_FragCoord.xy;
  vec2 uv = (frag - 0.5 * uRes) / uRes.y;
  float yb = fract(uTime * 0.045);
  float banda = smoothstep(0.035, 0.0, abs(frag.y / uRes.y - yb));
  uv.x += banda * (ruido(vec2(frag.y * 0.35, uTime * 24.0)) - 0.5) * 0.05;

  vec3 ro = vec3(sin(uTime * 0.1) * 0.15, 0.02 + sin(uTime * 1.5) * 0.012, uZ);
  float giro = sin(uTime * 0.06) * 0.06;
  vec3 rd = normalize(vec3(uv.x, uv.y - 0.03, 1.25));
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
    float seg = mod(p.z + lado * 5.0, 17.0);
    if (seg > 7.0 && seg < 8.9 && p.y < 0.55) {
      // una puerta verde entornada, oscuro detras
      float marco = min(seg - 7.0, 8.9 - seg);
      col = seg < 7.5 ? vec3(0.13, 0.42, 0.22) : vec3(0.05, 0.04, 0.06);
      col *= 0.6 + 0.4 * smoothstep(0.0, 0.12, marco);
    } else if (p.y < -0.45) {
      // zocalo azul marino con su franja amarilla
      col = p.y > -0.53 ? vec3(0.93, 0.78, 0.22) : vec3(0.12, 0.15, 0.33) * (0.9 + 0.1 * ruido(vec2(p.z, p.y) * 20.0));
    } else {
      // pared verde con confeti pintado
      vec2 w = vec2(p.z, p.y) * 5.0;
      vec2 c = floor(w);
      vec2 f = fract(w) - 0.5;
      float r = h2(c);
      col = vec3(0.24, 0.55, 0.47) * (0.9 + 0.1 * ruido(w * 0.3));
      if (r > 0.72 && length(f) < 0.16) col = paleta(floor(r * 40.0));
      // alguna mancha oscura de humedad: no es una fiesta de verdad
      col *= 1.0 - 0.35 * smoothstep(0.62, 0.85, ruido(vec2(p.z, p.y) * 0.8 + 4.0));
    }
  } else if (rd.y > 0.0) {
    n = vec3(0.0, -1.0, 0.0);
    vec2 c = p.xz;
    vec2 g = abs(fract(c / vec2(0.8, 0.8)) - 0.5);
    col = mix(vec3(0.86, 0.84, 0.76), vec3(0.6, 0.58, 0.5), smoothstep(0.46, 0.5, max(g.x, g.y)));
    float idx = floor(p.z / PASO);
    float lz = mod(p.z, PASO) - PASO * 0.5;
    if (abs(p.x) < 0.38 && abs(lz) < 0.38) {
      col = mix(vec3(0.3, 0.29, 0.25), vec3(1.8, 1.72, 1.5), tubo(idx));
      emite = 1.0;
    }
  } else {
    n = vec3(0.0, 1.0, 0.0);
    // moqueta de cuadros de colores con las lineas oscuras
    vec2 c = p.xz / 0.55;
    vec2 g = abs(fract(c) - 0.5);
    float linea = smoothstep(0.44, 0.48, max(g.x, g.y));
    float r = h2(floor(c));
    col = r > 0.55 ? paleta(floor(r * 23.0)) * 0.8 : vec3(0.18, 0.42, 0.22);
    col *= 0.85 + 0.2 * ruido(p.xz * 40.0);
    col = mix(col, vec3(0.1, 0.1, 0.12), linea * 0.7);
  }

  vec3 luz = emite > 0.5 ? vec3(1.0) : vec3(0.36, 0.33, 0.3) + luces(p, n) * 2.4;
  col *= luz;

  // las guirnaldas de banderines, que cruzan el pasillo colgando de una cuerda
  for (int i = 0; i < 5; i++) {
    float zb = (floor(ro.z / GUIRNALDA) + float(i)) * GUIRNALDA + GUIRNALDA * 0.5;
    if (rd.z <= 0.0) break;
    float tb = (zb - ro.z) / rd.z;
    if (tb <= 0.0 || tb >= t) continue;
    vec3 q = ro + rd * tb;
    float s = q.x / ANCHO;
    float cuerda = TECHO - 0.12 - 0.3 * (1.0 - s * s);
    float u = (q.x + ANCHO) * 3.2;
    float fu = fract(u);
    float largo = 0.24 * (1.0 - abs(fu * 2.0 - 1.0));
    float dy = cuerda - q.y;
    if (abs(dy) < 0.012) { col = vec3(0.85, 0.82, 0.75) * 0.6; t = tb; break; }
    if (dy > 0.0 && dy < largo) {
      col = paleta(floor(u) + zb) * (vec3(0.45) + luces(q, vec3(0.0, 0.0, -1.0)) * 1.6);
      t = tb;
      break;
    }
  }

  // globos flotando junto a las paredes (esferas), algunos a cada lado
  for (int i = 0; i < 4; i++) {
    float idx = floor(ro.z / GLOBOS) + float(i);
    float lado = h1(idx * 4.7) > 0.5 ? 1.0 : -1.0;
    vec3 c = vec3(lado * (ANCHO - 0.4), 0.55 + 0.08 * sin(uTime * 0.9 + idx), idx * GLOBOS + 2.0 + h1(idx) * 2.0);
    vec3 oc = ro - c;
    float b = dot(oc, rd);
    float cc = dot(oc, oc) - 0.25 * 0.25;
    float disc = b * b - cc;
    if (disc > 0.0) {
      float tg = -b - sqrt(disc);
      if (tg > 0.0 && tg < t) {
        vec3 q = ro + rd * tg;
        vec3 nn = normalize(q - c);
        vec3 base = paleta(floor(h1(idx * 9.1) * 4.0));
        float l = 0.45 + 0.55 * max(dot(nn, normalize(vec3(0.3, 0.9, -0.4))), 0.0);
        float brillo = pow(max(dot(reflect(rd, nn), normalize(vec3(0.0, 1.0, 0.2))), 0.0), 24.0);
        col = base * l + vec3(1.0) * brillo * 0.7;
        t = tg;
      }
    }
  }

  float niebla = 1.0 - exp(-t * 0.07);
  col = mix(col, vec3(0.2, 0.26, 0.22), niebla);
  col *= uLuz;
  col = pow(max(col, 0.0), vec3(1.0, 0.98, 1.06));
  vec2 q = frag / uRes;
  col *= 0.42 + 0.58 * pow(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.3);
  col *= 0.93 + 0.07 * sin(frag.y * 3.14159);
  col += (h2(frag + fract(uTime) * 91.0) - 0.5) * 0.06;
  fragColor = vec4(col, 1.0);
}