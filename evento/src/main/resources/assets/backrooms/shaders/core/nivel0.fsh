#version 330

// Pasillo del Nivel 0 para el menu de Minecraft: el mismo shader que el fondo
// del launcher (src/ui/pasillo.js), para que el juego continue lo que se ve
// en el launcher. Papel pintado con humedad, placas del techo con tubos (alguno
// parpadea, alguno esta fundido), moqueta mojada y puertas a otros pasillos.
// El tiempo y la posicion de la camara llegan por las UV del vertice y el
// brillo por el rojo del color (ver render/Fondo.java).
#moj_import <minecraft:globals.glsl>

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define uRes ScreenSize
#define uTime datos.x
#define uZ datos.y
// el mod manda luz * 0.75 en el canal rojo (cabe hasta 1.33)
#define uLuz (color.r / 0.75)

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
  fragColor = vec4(col, 1.0);
}
