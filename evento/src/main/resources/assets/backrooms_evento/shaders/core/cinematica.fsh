#version 330

// Cinematica del /start, grabada como la cinta de una videocamara, con el
// ascensor hecho de bloques de Minecraft: cabina de 3x3 de hierro y roble
// oscuro, puertas de hierro con sus ventanitas, lampara de redstone en el
// techo y el hueco de ladrillos de piedra con un rellano y una antorcha por
// planta (mas abajo pizarra profunda, piedra negra y magma).
//
// Las texturas son las del propio juego: AtlasCinematica.java las copia en
// tiempo de ejecucion a Sampler0 (16 x 4 casillas). La luz va por niveles
// como en Minecraft (15 menos la distancia en bloques, la curva del lightmap
// y el sombreado por cara) y las particulas son cuadrados de cara a camara.
//
// Todo es funcion del tiempo, que llega por la U de los vertices.
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define T datos.x

/* ------------------------------------------------------------ guion */

const float T_CIERRA0 = 2.0;   // las puertas empiezan a cerrarse
const float T_CIERRA1 = 3.4;
const float T_BAJA = 3.6;      // empieza a bajar
const float T_PARPADEO = 18.0; // la luz falla
const float T_ATASCO = 20.0;   // frenazo: se queda atascado
const float T_ROTURA = 26.5;   // se parte el cable
const float T_FUEGO = 29.0;    // el freno de emergencia echa chispas y prende
const float T_GRIETAS = 29.5;  // las puertas se van rompiendo
const float T_PUERTAS = 33.0;  // revientan
const float T_IMPACTO = 39.0;

const float VEL = 3.0;         // bloques por segundo bajando
const float PLANTA = 4.0;      // bloques entre plantas
const float FOVK = 0.7002;     // tan(35): el FOV 70 de Minecraft

/* ---------------------------------------------------------- atlas */

const int HIERRO = 0, PIEDRA_LISA = 1, ROBLE_OSCURO = 2, PUERTA_ARRIBA = 3, PUERTA_ABAJO = 4;
const int LAMPARA_ON = 5, LAMPARA_OFF = 6, LADRILLOS = 7, LADRILLOS_ROTOS = 8, PIZARRA = 9;
const int HORMIGON_NEGRO = 10, ANTORCHA = 11, ANTORCHA_REDSTONE = 12, LOSA = 13, ANDESITA = 14, PIEDRANEGRA_PULIDA = 15;
const int FUEGO0 = 16, LAVA0 = 24, GRIETA0 = 32, CUARZO = 42, FAROL_MAR = 43, ROCA = 44, MAGMA = 45, PIEDRANEGRA = 46;
const int P_LLAMA = 48, P_LAVA = 49, P_HUMO0 = 50, P_POLVO = 53;

vec4 tex(int casilla, vec2 uv) {
  vec2 px = floor(clamp(uv, 0.0, 0.9999) * 16.0) + 0.5;
  vec2 c = vec2(float(casilla % 16), float(casilla / 16));
  return texture(Sampler0, (c * 16.0 + px) / vec2(256.0, 64.0));
}

// cara de un bloque: u a la derecha, v hacia arriba (la imagen va de arriba abajo)
vec4 cara(int casilla, vec2 p) {
  vec2 f = fract(p);
  return tex(casilla, vec2(f.x, 1.0 - f.y));
}

/* ----------------------------------------------------------- utiles */

float h1(float n) { return fract(sin(n) * 43758.5453123); }
float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float ruido(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(h2(i), h2(i + vec2(1.0, 0.0)), f.x), mix(h2(i + vec2(0.0, 1.0)), h2(i + vec2(1.0, 1.0)), f.x), f.y);
}

/* ------------------------------------------------------- movimiento */

float bajada(float t) {
  float tc = clamp(t - T_BAJA, 0.0, T_ATASCO - T_BAJA);
  float s = VEL * tc - VEL * 0.8 * (1.0 - exp(-tc / 0.8));
  s += 0.25 * smoothstep(23.2, 23.32, t) + 0.35 * smoothstep(25.0, 25.1, t); // se escurre
  if (t > T_ROTURA) {
    float u = t - T_ROTURA;
    s += 0.8 * smoothstep(0.0, 0.15, u);
    float uf = max(u - 0.3, 0.0);
    s += 4.9 * uf * uf;
  }
  return s;
}

float velocidad(float t) { return (bajada(t + 0.01) - bajada(t - 0.01)) / 0.02; }

// golpe de camara como el de recibir dano: inclina y vuelve
float golpe(float t, float t0, float k) {
  float u = t - t0;
  return u < 0.0 ? 0.0 : sin(u * 22.0) * exp(-u * 5.0) * k;
}

/* ------------------------------------------------------------ luces */

float lampara(float t) {
  if (t < T_PARPADEO) return 1.0;
  if (t < T_ATASCO) return step(0.45, h1(floor(t * 11.0)));
  if (t < T_ATASCO + 0.5) return 0.0;
  if (t < T_ROTURA) return step(0.97, h1(floor(t * 9.0) + 3.0)); // algun chispazo
  return 0.0;
}

float emergencia(float t) { return smoothstep(T_ATASCO + 0.4, T_ATASCO + 0.6, t); }

float fuego(float t) {
  float f = smoothstep(T_FUEGO, T_FUEGO + 2.0, t) * 0.7 + 0.5 * smoothstep(T_PUERTAS, T_PUERTAS + 0.4, t);
  return f * (0.8 + 0.2 * ruido(vec2(t * 15.0, 3.0)));
}

float brilloNivel(float L) {
  float f = clamp(L, 0.0, 15.0) / 15.0;
  return f / (4.0 - 3.0 * f);
}

float sombreado(vec3 n) {
  return n.y > 0.5 ? 1.0 : n.y < -0.5 ? 0.5 : abs(n.z) > 0.5 ? 0.8 : 0.6;
}

vec3 fuente(vec3 q, vec3 pos, float nivel, vec3 c) {
  vec3 d = abs(q - pos);
  return c * brilloNivel(nivel - (d.x + d.y + d.z));
}

// la antorcha del rellano que pasa por delante de la puerta (en coords de cabina)
float antorchaY(float t, out float viva) {
  float s = bajada(t);
  float piso = floor((1.6 + s) / PLANTA);
  float mejor = 1e3;
  viva = 0.0;
  for (int k = -1; k <= 1; k++) {
    float pk = -piso + float(k);
    float y = pk * PLANTA + s + 2.6;
    if (abs(y - 1.6) < abs(mejor - 1.6)) { mejor = y; viva = step(0.1, h1(pk * 1.7)); }
  }
  return mejor;
}

vec3 luz(vec3 p, vec3 n, float t) {
  vec3 q = p + n * 0.5;
  vec3 c = vec3(0.012);
  c += fuente(q, vec3(0.0, 3.5, 0.0), 15.0 * lampara(t), vec3(1.0, 0.96, 0.88));
  float e = emergencia(t);
  c += fuente(q, vec3(-1.2, 2.5, 0.5), 9.0 * e, vec3(1.0, 0.22, 0.15) * (0.85 + 0.15 * sin(t * 3.0)));
  c += fuente(q, vec3(1.2, 2.5, 0.5), 9.0 * e, vec3(1.0, 0.22, 0.15) * (0.85 + 0.15 * sin(t * 3.0 + 1.0)));
  float fu = fuego(t);
  c += fuente(q, vec3(0.0, 0.2, -2.0), 15.0 * fu, vec3(1.0, 0.6, 0.3));
  c += fuente(q, vec3(0.0, 1.5, -2.0), 13.0 * smoothstep(T_PUERTAS, T_PUERTAS + 0.3, t) * fu, vec3(1.0, 0.55, 0.25));
  // antorchas del hueco: se cuelan por las ventanitas (y a toda velocidad parpadean)
  float viva;
  float ya = antorchaY(t, viva);
  float ancho = 1.0 + velocidad(t) / 25.0;
  c += fuente(q, vec3(0.0, ya, -2.2), (13.0 - (ancho - 1.0) * 2.0) * viva, vec3(1.0, 0.9, 0.7)) * min(1.0, 2.0 / ancho);
  // al final, el fondo del hueco es magma
  c += vec3(1.0, 0.45, 0.15) * smoothstep(T_IMPACTO - 3.0, T_IMPACTO, t) * 0.8 * (0.6 + 0.4 * (1.0 - p.y / 3.0));
  return c;
}

vec3 iluminar(vec3 alb, vec3 p, vec3 n, float t) {
  return alb * min(luz(p, n, t), vec3(1.5)) * sombreado(n);
}

/* ----------------------------------------------------- display */

int glifo(int c) {
  // 3x5 px, fila de arriba en los bits altos; 10 '-', 11 'P', 12 'E', 13 'r', 14 ' ', 15 flecha
  if (c == 0) return 31599; if (c == 1) return 11415; if (c == 2) return 29671; if (c == 3) return 29647;
  if (c == 4) return 23497; if (c == 5) return 31183; if (c == 6) return 31215; if (c == 7) return 29257;
  if (c == 8) return 31727; if (c == 9) return 31695; if (c == 10) return 448; if (c == 11) return 31716;
  if (c == 12) return 31207; if (c == 13) return 484; if (c == 15) return 4048;
  return 0;
}

ivec4 textoDisplay(float t) {
  float s = bajada(t);
  int n = int(floor((s + 0.5) / PLANTA)) + 1;
  int flecha = (t > T_BAJA && t < T_ATASCO && fract(t * 1.4) < 0.6) ? 15 : 14;
  if (t < T_BAJA + 0.4) return ivec4(14, 14, 14, 11);
  if (t < T_ATASCO) return ivec4(flecha, n >= 10 ? 10 : 14, n >= 10 ? n / 10 : 10, n % 10);
  if (t < T_ROTURA) {
    if (t > 23.2 && fract(t * 1.5) < 0.5) return ivec4(14, 12, 13, 13);
    return ivec4(14, n >= 10 ? 10 : 14, n >= 10 ? n / 10 : 10, n % 10);
  }
  if (t < T_ROTURA + 1.0) {
    float g = floor(t * 17.0);
    return ivec4(14, int(h1(g) * 10.0), int(h1(g + 1.0) * 10.0), int(h1(g + 2.0) * 10.0));
  }
  if (n > 99 || h1(floor(t * 8.0)) > 0.85) return ivec4(14, 12, 13, 13);
  return ivec4(15, 10, n / 10, n % 10);
}

// cara del bloque de hormigon negro sobre las puertas
vec3 display(vec2 f, float t) {
  vec3 base = cara(HORMIGON_NEGRO, f).rgb * 0.5;
  ivec2 px = ivec2(floor(fract(f) * 16.0));
  int fila = 10 - px.y; // filas 6..10 de la imagen (de arriba abajo)
  bool apagado = (t > T_ROTURA && t < T_ROTURA + 1.0 && h1(floor(t * 20.0)) < 0.3) || t > T_IMPACTO - 0.6;
  if (fila < 0 || fila > 4 || apagado) return base;
  int i = px.x / 4;
  int col = px.x % 4;
  if (col == 3) return base;
  ivec4 txt = textoDisplay(t);
  int g = glifo(txt[i]);
  int bit = 14 - (fila * 3 + col);
  if (((g >> bit) & 1) != 0) return vec3(1.0, 0.16, 0.06) * 1.6;
  return base;
}

/* ------------------------------------------------- hueco y vestibulo */

// pared del fondo del hueco en la altura H (del mundo, 0 = planta de salida):
// ladrillos, rellanos, puertas de rellano y antorchas; mas abajo pizarra,
// piedra negra y magma; con fuego en los rellanos cuando arde.
vec3 paredHueco(float x, float H, float t) {
  float piso = floor(H / PLANTA);
  float hr = H - piso * PLANTA;      // altura dentro de la planta
  float prof = -piso * PLANTA;       // cuanto se ha bajado
  int bloque = prof < 40.0 ? LADRILLOS : prof < 140.0 ? PIZARRA : PIEDRANEGRA;
  if (bloque == LADRILLOS && h1(floor(x) * 7.0 + floor(H) * 3.0) > 0.8) bloque = LADRILLOS_ROTOS;
  if (bloque == PIEDRANEGRA && h1(floor(x) * 5.0 + floor(H) * 11.0) > 0.85) bloque = MAGMA;
  vec3 c = cara(bloque, vec2(x, H)).rgb;
  float luzNivel = 4.0;
  bool viva = h1(piso * 1.7) > 0.1;
  if (hr < 0.5) {
    c = cara(LOSA, vec2(x, hr * 2.0)).rgb; // forjado del rellano
  } else if (abs(x) < 1.0 && hr < 2.5) {
    // puertas de rellano
    float u = x < 0.0 ? x + 1.0 : 1.0 - x;
    vec4 d = hr < 1.5 ? cara(PUERTA_ABAJO, vec2(u, hr - 0.5)) : cara(PUERTA_ARRIBA, vec2(u, hr - 1.5));
    c = d.a > 0.5 ? d.rgb : vec3(0.02);
  }
  // antorcha sobre la puerta de cada rellano
  vec2 pa = vec2(x, hr - 2.6);
  if (viva && abs(pa.x) < 0.5 && abs(pa.y) < 0.5) {
    vec4 a = cara(ANTORCHA, pa + 0.5);
    if (a.a > 0.5) return a.rgb * 1.3;
  }
  if (viva) luzNivel = 14.0 - (abs(x) + abs(hr - 2.6));
  // fuego en los rellanos cuando arde
  float fu = fuego(t);
  if (fu > 0.0 && prof > 30.0 && h1(piso * 3.1 + floor(x + 2.0)) > 0.45 && hr > 0.5 && hr < 1.5) {
    int frame = int(mod(t * 16.0 + h1(piso) * 8.0, 8.0));
    vec4 f = cara(FUEGO0 + frame, vec2(x, hr - 0.5));
    if (f.a > 0.5) return f.rgb * 1.4;
    luzNivel = max(luzNivel, 15.0 - abs(hr - 1.0) * 2.0);
  }
  if (bloque == MAGMA) return c * 1.1;
  return c * brilloNivel(luzNivel) * 0.8;
}

vec3 vistaHueco(vec3 ro, vec3 rd, float t) {
  float d = (-2.5 - ro.z) / rd.z;
  vec3 q = ro + rd * d;
  float s = bajada(t);
  float v = velocidad(t);
  const int N = 5;
  vec3 acc = vec3(0.0);
  for (int k = 0; k < N; k++) {
    float H = q.y - s - v * (float(k) / float(N)) / 30.0;
    acc += paredHueco(q.x, H, t);
  }
  vec3 col = acc / float(N);
  col += vec3(1.0, 0.5, 0.15) * fuego(t) * 0.25;
  col += vec3(1.0, 0.45, 0.12) * smoothstep(T_IMPACTO - 3.0, T_IMPACTO, t) * (1.2 - q.y * 0.3);
  return col;
}

// el vestibulo de cuarzo que se ve mientras se cierran las puertas
vec3 vestibulo(vec3 ro, vec3 rd) {
  vec3 bmin = vec3(-3.5, 0.0, -9.0), bmax = vec3(3.5, 4.0, -1.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  vec3 n;
  vec3 c;
  if (th == tf.y) {
    n = vec3(0.0, -sign(rd.y), 0.0);
    if (rd.y > 0.0) {
      bool farol = mod(floor(p.x) + floor(p.z), 3.0) == 0.0;
      return farol ? cara(FAROL_MAR, p.xz).rgb * 1.2 : cara(CUARZO, p.xz).rgb * 0.95;
    }
    c = cara(ANDESITA, p.xz).rgb;
  } else if (th == tf.x) {
    n = vec3(-sign(rd.x), 0.0, 0.0);
    c = cara(CUARZO, p.zy).rgb;
  } else {
    n = vec3(0.0, 0.0, 1.0);
    c = cara(CUARZO, p.xy).rgb;
  }
  return c * 0.95 * sombreado(n);
}

/* --------------------------------------------------------- cabina */

// pared de las puertas (z = -1.5)
vec3 paredPuertas(vec3 p, vec3 n, vec3 ro, vec3 rd, float t) {
  float x = p.x;
  float y = p.y;
  if (y >= 2.0) {
    if (abs(x) < 0.5 && y < 3.0) return display(vec2(x + 0.5, y - 2.0), t);
    return iluminar(cara(HIERRO, vec2(x + 0.5, y)).rgb, p, n, t);
  }
  if (abs(x) >= 1.0) return iluminar(cara(HIERRO, vec2(x + 0.5, y)).rgb, p, n, t);
  if (t > T_PUERTAS) return vistaHueco(ro, rd, t); // ya no hay puertas
  float g = 1.0 - smoothstep(T_CIERRA0, T_CIERRA1, t); // hueco abierto por cada lado
  if (abs(x) < g) return vestibulo(ro, rd);
  // las hojas de hierro deslizan hacia el centro
  float u = x < 0.0 ? x + 1.0 + g : 1.0 - (x - g);
  float temblor = t > T_ROTURA ? (h1(floor(t * 30.0)) - 0.5) * 0.03 : 0.0;
  vec4 d = y < 1.0 ? cara(PUERTA_ABAJO, vec2(u + temblor, y)) : cara(PUERTA_ARRIBA, vec2(u + temblor, y - 1.0));
  if (d.a < 0.5) {
    return t < T_CIERRA1 ? vestibulo(ro, rd) : vistaHueco(ro, rd, t);
  }
  vec3 c = iluminar(d.rgb, p, n, t);
  // grietas de romper bloques hasta que revientan
  if (t > T_GRIETAS) {
    int etapa = int(clamp((t - T_GRIETAS) / (T_PUERTAS - T_GRIETAS) * 10.0, 0.0, 9.0));
    vec4 gr = cara(GRIETA0 + etapa, vec2(u, y));
    c *= mix(vec3(1.0), gr.rgb * 2.0, gr.a);
  }
  // el fuego del hueco se cuela por la junta de abajo
  c += vec3(1.0, 0.5, 0.15) * fuego(t) * smoothstep(0.25, 0.0, y) * 0.6;
  return c;
}

vec3 paredLado(vec3 p, vec3 n, float t, bool derecha) {
  float z = p.z;
  float y = p.y;
  vec3 alb = y < 1.0 ? cara(ROBLE_OSCURO, vec2(z + 0.5, y)).rgb : cara(HIERRO, vec2(z + 0.5, y)).rgb;
  // botonera: botones de piedra negra pulida en la pared derecha, junto a la puerta
  if (derecha && z > -1.5 && z < -0.5 && y > 1.0 && y < 2.0) {
    ivec2 px = ivec2(floor(vec2(z + 1.5, y - 1.0) * 16.0));
    int bx = px.x < 8 ? px.x - 2 : px.x - 9;
    int by = (px.y - 3) % 4;
    int fila = (px.y - 3) / 4;
    if (px.y >= 3 && px.y < 15 && bx >= 0 && bx < 5 && by < 3) {
      vec3 b = cara(PIEDRANEGRA_PULIDA, vec2(z, y)).rgb;
      float id = float(fila * 2 + (px.x < 8 ? 0 : 1));
      bool encendido = t < T_ATASCO ? id == 0.0 : (t > T_ROTURA && h1(id + floor(t * 7.0)) > 0.5);
      if (encendido) return vec3(1.0, 0.75, 0.3) * 1.2;
      return iluminar(b * 1.1, p, n, t);
    }
  }
  // antorchas de redstone de emergencia
  if (emergencia(t) > 0.0 && z > 0.0 && z < 1.0 && y > 2.0 && y < 3.0) {
    vec4 a = cara(ANTORCHA_REDSTONE, vec2(z, y - 2.0));
    if (a.a > 0.5) return a.rgb * 1.3;
  }
  return iluminar(alb, p, n, t);
}

vec3 techo(vec3 p, vec3 n, float t) {
  if (abs(p.x) < 0.5 && abs(p.z) < 0.5) {
    return lampara(t) > 0.5 ? cara(LAMPARA_ON, p.xz + 0.5).rgb * 1.1 : iluminar(cara(LAMPARA_OFF, p.xz + 0.5).rgb, p, n, t);
  }
  return iluminar(cara(HIERRO, p.xz + 0.5).rgb, p, n, t);
}

vec3 suelo(vec3 p, vec3 n, float t) {
  return iluminar(cara(PIEDRA_LISA, p.xz + 0.5).rgb, p, n, t);
}

/* ------------------------------------------------------- particulas */

struct Camara { vec3 ro; vec3 f; vec3 r; vec3 u; };

// dibuja una particula cuadrada (casilla del atlas) en P sobre col
void particula(inout vec3 col, Camara c, vec2 uv, float zEscena, vec3 P, float lado, int casilla, vec3 tinte, float alfa) {
  vec3 op = P - c.ro;
  float z = dot(op, c.f);
  if (z < 0.08 || z > zEscena) return;
  vec2 centro = vec2(dot(op, c.r), dot(op, c.u)) / (z * FOVK);
  float medio = lado * 0.5 / (z * FOVK);
  vec2 l = (uv - centro) / medio;
  if (abs(l.x) > 1.0 || abs(l.y) > 1.0) return;
  vec4 s = tex(casilla, vec2(l.x * 0.5 + 0.5, 0.5 - l.y * 0.5));
  if (s.a < 0.5) return;
  col = mix(col, s.rgb * tinte, alfa);
}

void particulas(inout vec3 col, Camara c, vec2 uv, float zEscena, float t) {
  // polvo que cae del techo en el frenazo y cada vez que se escurre
  for (int i = 0; i < 24; i++) {
    float fi = float(i);
    float t0 = i < 14 ? T_ATASCO : (i < 19 ? 23.2 : 25.0);
    float u = t - t0 - h1(fi * 3.3) * 0.6;
    if (u < 0.0 || u > 2.6) continue;
    vec3 P = vec3((h1(fi) - 0.5) * 2.6, 2.95 - u * 0.9, (h1(fi * 1.9) - 0.5) * 2.6);
    particula(col, c, uv, zEscena, P, 0.07, P_POLVO, vec3(0.75, 0.72, 0.65) * (0.3 + 0.7 * lampara(t) + emergencia(t) * 0.3), 1.0);
  }
  // chispas (particula de lava) que entran por la junta y luego por el hueco
  if (t > T_FUEGO - 0.5) {
    for (int i = 0; i < 18; i++) {
      float fi = float(i);
      float periodo = 0.8 + h1(fi * 3.1) * 0.6;
      float u = mod(t + h1(fi) * 5.0, periodo);
      vec3 o = t > T_PUERTAS ? vec3((h1(fi * 4.0) - 0.5) * 1.8, 0.2 + h1(fi * 6.0) * 1.6, -1.6) : vec3((h1(fi * 4.0) - 0.5) * 0.4, 0.05, -1.5);
      vec3 vel = vec3((h1(fi * 2.3) - 0.5) * 2.0, 1.0 + h1(fi * 4.1) * 2.0, 1.2 + h1(fi * 6.7) * 2.0);
      vec3 P = o + vel * u + vec3(0.0, -2.0, 0.0) * u * u;
      if (P.y < 0.0) continue;
      particula(col, c, uv, zEscena, P, 0.1, P_LAVA, vec3(1.5), 1.0);
    }
  }
  // llamas en el hueco de la puerta reventada
  if (t > T_PUERTAS) {
    for (int i = 0; i < 12; i++) {
      float fi = float(i);
      float u = mod(t * 1.3 + h1(fi * 9.0), 1.0);
      vec3 P = vec3((h1(fi * 2.0) - 0.5) * 1.9, 0.1 + u * 1.9, -1.55 - h1(fi * 5.0) * 0.4);
      particula(col, c, uv, zEscena, P, 0.16 * (1.0 - u * 0.5), P_LLAMA, vec3(1.4), 1.0);
    }
    // trozos de las puertas reventadas
    for (int i = 0; i < 20; i++) {
      float fi = float(i);
      float u = t - T_PUERTAS;
      if (u > 1.4) break;
      vec3 o = vec3((h1(fi) - 0.5) * 1.9, h1(fi * 3.0) * 2.0, -1.5);
      vec3 vel = vec3((h1(fi * 2.0) - 0.5) * 3.0, (h1(fi * 4.0) - 0.3) * 2.0, 2.0 + h1(fi * 5.0) * 3.0);
      vec3 P = o + vel * u;
      particula(col, c, uv, zEscena, P, 0.12, PUERTA_ABAJO, vec3(0.6) + vec3(0.4, 0.2, 0.0) * fuego(t), 1.0);
    }
  }
  // humo negro cuando arde
  if (t > T_FUEGO + 1.0) {
    for (int i = 0; i < 10; i++) {
      float fi = float(i);
      float periodo = 3.0 + h1(fi) * 2.0;
      float u = mod(t + h1(fi * 7.0) * 6.0, periodo) / periodo;
      vec3 P = vec3((h1(fi * 3.0) - 0.5) * 2.4, 0.3 + u * 2.6, -1.3 + h1(fi * 8.0) * 1.2);
      int frame = P_HUMO0 + int(u * 2.9);
      particula(col, c, uv, zEscena, P, 0.5 + u * 0.6, frame, vec3(0.18, 0.15, 0.13), 0.55 * (1.0 - u));
    }
  }
}

/* ---------------------------------------------------------- escena */

vec3 escena(vec2 uv, float t) {
  // camara: un jugador de pie en la cabina, con la videocamara en la mano
  float sac = 0.004;
  if (t > T_ROTURA) sac = mix(0.03, 0.085, smoothstep(T_ROTURA, T_IMPACTO, t));
  vec3 sh = (vec3(ruido(vec2(t * 13.0, 1.0)), ruido(vec2(t * 11.0, 5.0)), ruido(vec2(t * 9.0, 9.0))) - 0.5) * 2.0 * sac;
  vec3 ro = vec3(0.0, 1.62, 0.9) + sh;
  // en caida libre flotas hacia el techo
  ro.y += 0.55 * smoothstep(T_ROTURA + 0.3, T_ROTURA + 3.0, t);
  ro.x -= 0.3 * smoothstep(T_ROTURA + 0.3, T_ROTURA + 4.0, t);
  // al frenar en seco y al escurrirse, el cuerpo cae un poco
  ro.y -= 0.18 * abs(golpe(t, T_ATASCO, 1.0)) + 0.1 * abs(golpe(t, 23.2, 1.0)) + 0.12 * abs(golpe(t, 25.0, 1.0));
  float yaw = sh.x * 1.5;
  float pitch = -0.08 + sh.y * 1.5;
  // mira el techo cuando cruje el cable, y luego la puerta
  pitch += 0.55 * smoothstep(21.6, 22.8, t) * (1.0 - smoothstep(24.6, 25.4, t));
  yaw += 0.25 * smoothstep(21.6, 22.8, t) * (1.0 - smoothstep(24.6, 25.4, t));
  pitch -= 0.22 * smoothstep(T_ROTURA + 1.0, T_ROTURA + 3.0, t); // flotando, mira hacia la puerta
  float roll = sh.z * 2.0 + golpe(t, T_ATASCO, 0.25) + golpe(t, 23.2, 0.12) + golpe(t, 25.0, 0.15)
    + golpe(t, T_ROTURA, 0.3) + golpe(t, T_PUERTAS, 0.25) + 0.1 * sin(t * 1.7) * smoothstep(T_ROTURA, T_IMPACTO, t);
  vec3 f = normalize(vec3(sin(yaw), sin(pitch), -cos(yaw)));
  vec3 r = normalize(cross(f, vec3(0.0, 1.0, 0.0)));
  vec3 up = cross(r, f);
  Camara c;
  c.ro = ro;
  c.f = f;
  c.r = r * cos(roll) + up * sin(roll);
  c.u = up * cos(roll) - r * sin(roll);
  vec3 rd = normalize(f + (uv.x * c.r + uv.y * c.u) * FOVK);

  vec3 bmin = vec3(-1.5, 0.0, -1.5), bmax = vec3(1.5, 3.0, 1.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  vec3 col;
  if (th == tf.y) {
    vec3 n = vec3(0.0, -sign(rd.y), 0.0);
    col = rd.y > 0.0 ? techo(p, n, t) : suelo(p, n, t);
  } else if (th == tf.x) {
    vec3 n = vec3(-sign(rd.x), 0.0, 0.0);
    col = paredLado(p, n, t, rd.x > 0.0);
  } else if (rd.z < 0.0) {
    col = paredPuertas(p, vec3(0.0, 0.0, 1.0), ro, rd, t);
  } else {
    col = paredLado(p, vec3(0.0, 0.0, -1.0), t, false);
  }
  particulas(col, c, uv, th * dot(rd, f), t);
  return col;
}

/* ------------------------------------------------------- videocamara */

void main() {
  vec2 frag = gl_FragCoord.xy;
  vec2 res = ScreenSize;
  vec2 uv01 = frag / res;
  float t = T;

  if (t < 0.9) {
    // la camara se enciende: nieve
    float n = h2(floor(frag / 3.0) + floor(t * 60.0));
    fragColor = vec4(vec3(n * 0.55) * smoothstep(0.0, 0.2, t), 1.0);
    return;
  }

  float glitch = 0.12 + abs(golpe(t, T_ATASCO, 0.6) + golpe(t, T_ROTURA, 0.9) + golpe(t, T_PUERTAS, 0.6));
  glitch += 0.3 * smoothstep(T_ROTURA, T_IMPACTO, t);
  float banda = fract(t * 0.21 + h1(floor(t * 2.0)) * 0.3);
  float enBanda = smoothstep(0.03, 0.0, abs(uv01.y - banda));
  float dx = (h2(vec2(floor(uv01.y * 160.0), floor(t * 30.0))) - 0.5) * 0.04 * enBanda * glitch;
  float rueda = t < 1.3 ? (1.3 - t) * 1.5 : 0.0;
  vec2 uvv = vec2(uv01.x + dx, fract(uv01.y + rueda));
  vec2 uv = (uvv * 2.0 - 1.0) * vec2(res.x / res.y, 1.0);
  vec3 col = escena(uv, t);

  // el ultimo segundo todo arde; luego el golpe
  col += vec3(1.0, 0.5, 0.2) * smoothstep(T_IMPACTO - 1.0, T_IMPACTO, t) * 0.5;
  if (t > T_IMPACTO) {
    float u = t - T_IMPACTO;
    if (u < 0.1) col = vec3(1.6);
    else {
      float n = h2(floor(frag / vec2(3.0, 1.0)) + floor(t * 50.0));
      col = mix(vec3(n) * 0.8, vec3(0.0), smoothstep(0.1, 0.4, u));
    }
  }

  // look de videocamara, suave para que se vean los pixeles de los bloques
  col = pow(max(col, 0.0), vec3(0.95)) * vec3(1.03, 1.0, 0.93) + vec3(0.015, 0.012, 0.02);
  col *= 0.95 + 0.05 * sin(frag.y * 3.14159);
  col += (h2(frag + fract(t) * 97.0) - 0.5) * 0.05;
  vec2 v = uv01 - 0.5;
  col *= 1.0 - dot(v, v) * 0.7;
  col *= smoothstep(0.9, 1.4, t);
  fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
