#version 330

// Pasillo de las Piscinas (Poolrooms) para el menu de Minecraft.
// El tiempo y la posicion de la camara llegan por las UV del vertice y el
// brillo por el rojo del color: el GuiRenderer no deja enlazar uniforms
// propios, pero estos datos son iguales en los cuatro vertices.
#moj_import <minecraft:globals.glsl>

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define uRes ScreenSize
#define uTime datos.x
#define uZ datos.y
#define uLuz color.r

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
  // brillo: el mod manda luz * 0.75 en el canal rojo (cabe hasta 1.33)
  fragColor = vec4(col * (uLuz / 0.75), 1.0);
}
