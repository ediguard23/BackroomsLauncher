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
// Todo es funcion del tiempo, que llega por la U de los vertices. La V dice el
// guion (G):
//   0  el /start: del vestibulo al Nivel 0 (cabina de hierro de 3x3)
//   2  la bajada a la fase 2: el ascensor de salida (acero, 5x5) se atasca, lo
//      arreglas a golpes, vuelve a fallar y cae; se abre a medias al sector B
//   3  la bajada a la fase 3: algo cruza el pasillo, se va la luz (vision
//      nocturna), garras por la junta, golpes en el techo y, al abrirse, una
//      sonrisa en la oscuridad del sector C
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define T datos.x
#define G int(datos.y + 0.5)
// el /start desde el vestibulo: por las puertas abiertas se ve el mundo de verdad
// (CinematicaCliente#vistaVestibulo pone alli la camara del juego); llega en el verde del color
#define VIVO (color.g < 0.5)

// cuanto de este pixel es el vestibulo de verdad (sale transparente)
float mundo = 0.0;

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

// guion 2 (bajada a la fase 2)
const float A_FRENO = 7.6;     // frena en seco
const float A_GIRO = 8.4;      // te das la vuelta hacia el panel
const float A_GOLPE1 = 10.6, A_GOLPE2 = 11.6, A_GOLPE3 = 12.5; // golpes al panel
const float A_ARREGLO = 13.2;  // vuelve la luz
const float A_VUELTA = 14.0;   // vuelves a mirar a las puertas
const float A_SIGUE = 15.0;    // sigue bajando
const float A_PARPADEO2 = 19.4;
const float A_CAE = 20.4;      // se escurre el cable: cae
const float A_FRENA = 21.6;    // el freno de emergencia lo para
const float A_ABRE = 24.0;     // las puertas se abren a medias (sector B)
const float A_ANDA = 25.5;     // andas hacia la abertura
const float A_FIN = 27.8;      // se acaba la cinta

// guion 3 (bajada a la fase 3)
const float B_APAGON = 8.6;    // se va la luz del todo
const float B_NOCHE = 9.8;     // la videocamara pasa a vision nocturna
const float B_GARRAS = 11.5;   // algo mete las garras por la junta
const float B_PORTAZO = 12.9;  // y las puertas se cierran de golpe
const float B_MIRA = 13.6;     // miras al techo
const float B_LURCH = 18.6;    // la cabina se escurre
const float B_LUZ = 19.6;      // vuelve la luz (de emergencia)
const float B_ABRE = 21.2;     // las puertas se abren solas
const float B_CARA = 23.6;     // una sonrisa en la oscuridad
const float B_SALTA = 24.6;    // se te echa encima
const float B_FIN = 25.4;      // se acaba la cinta

/* ---------------------------------------------------------- atlas */

const int HIERRO = 0, PIEDRA_LISA = 1, ROBLE_OSCURO = 2, PUERTA_ARRIBA = 3, PUERTA_ABAJO = 4;
const int LAMPARA_ON = 5, LAMPARA_OFF = 6, LADRILLOS = 7, LADRILLOS_ROTOS = 8, PIZARRA = 9;
const int HORMIGON_NEGRO = 10, ANTORCHA = 11, ANTORCHA_REDSTONE = 12, LOSA = 13, ANDESITA = 14, PIEDRANEGRA_PULIDA = 15;
const int FUEGO0 = 16, LAVA0 = 24, GRIETA0 = 32, CUARZO = 42, FAROL_MAR = 43, ROCA = 44, MAGMA = 45, PIEDRANEGRA = 46;
const int P_LLAMA = 48, P_LAVA = 49, P_HUMO0 = 50, P_POLVO = 53, HORMIGON_BLANCO = 57;
// filas 4 y 5: el Nivel 0 y el vestibulo de verdad (texturas del mod) y el ascensor de salida
const int PAPEL = 64, PAPEL_SUCIO = 65, MOQUETA = 66, MOQUETA_MOJADA = 67, TECHO_N0 = 68, TUBO = 69, TUBO_OFF = 70;
const int MADERA_OSCURA = 71, HORMIGON_GRIS = 72, HORMIGON_AMARILLO = 73, PIZARRA_PULIDA = 74, CUARZO_LISO = 75;
const int ACERO = 76, ASC_SUELO = 77, LUZ_ASC = 78, PANEL_ASC = 79, PUERTA_IZQ = 80, PUERTA_DER = 81, BACILO = 82;

vec4 tex(int casilla, vec2 uv) {
  vec2 px = floor(clamp(uv, 0.0, 0.9999) * 16.0) + 0.5;
  vec2 c = vec2(float(casilla % 16), float(casilla / 16));
  return texture(Sampler0, (c * 16.0 + px) / vec2(256.0, 96.0));
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

float bajada0(float t);

// guion 2: baja, frena en seco, sigue tras el arreglo, se escurre y cae hasta el freno
float bajada2(float t) {
  float a = clamp(t - T_BAJA, 0.0, A_FRENO - T_BAJA);
  float s = VEL * a - VEL * 0.8 * (1.0 - exp(-a / 0.8));
  float b = clamp(t - A_SIGUE, 0.0, A_CAE - A_SIGUE);
  s += VEL * b - VEL * 0.8 * (1.0 - exp(-b / 0.8));
  float c = clamp(t - A_CAE, 0.0, A_FRENA - A_CAE);
  return s + 4.9 * c * c;
}

// guion 3: baja despacio y, en la oscuridad, se escurre de golpe
float bajada3(float t) {
  float a = clamp(t - T_BAJA, 0.0, B_LURCH - T_BAJA);
  float s = 2.2 * a - 2.2 * 0.8 * (1.0 - exp(-a / 0.8));
  return s + 1.6 * smoothstep(B_LURCH, B_LURCH + 0.25, t);
}

float bajada(float t) {
  return G == 2 ? bajada2(t) : G == 3 ? bajada3(t) : bajada0(t);
}

float bajada0(float t) {
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
  if (G == 2) {
    if (t < 6.8) return 1.0;
    if (t < A_FRENO) return step(0.4, h1(floor(t * 12.0)));
    if (t < A_ARREGLO) return step(0.985, h1(floor(t * 9.0) + 5.0)); // algun chispazo
    if (t < A_ARREGLO + 0.5) return step(0.5, h1(floor(t * 14.0) + 2.0)); // vuelve a trompicones
    if (t < A_PARPADEO2) return 1.0;
    if (t < A_CAE) return step(0.45, h1(floor(t * 11.0) + 9.0));
    if (t < A_FRENA) return step(0.7, h1(floor(t * 20.0) + 4.0));
    return 0.0;
  }
  if (G == 3) {
    if (t < 7.9) return 1.0;
    if (t < B_APAGON) return step(0.5, h1(floor(t * 10.0) + 7.0));
    if (t < B_LUZ) return 0.0;
    if (t < B_LUZ + 0.6) return step(0.5, h1(floor(t * 13.0) + 1.0)) * 0.6;
    return 0.35 * step(0.15, h1(floor(t * 7.0) + 3.0)); // a medio gas, parpadeando
  }
  if (t < T_PARPADEO) return 1.0;
  if (t < T_ATASCO) return step(0.45, h1(floor(t * 11.0)));
  if (t < T_ATASCO + 0.5) return 0.0;
  if (t < T_ROTURA) return step(0.97, h1(floor(t * 9.0) + 3.0)); // algun chispazo
  return 0.0;
}

float emergencia(float t) {
  if (G == 2) {
    return max(smoothstep(A_FRENO + 0.3, A_FRENO + 0.5, t) * (1.0 - smoothstep(A_ARREGLO + 0.4, A_ARREGLO + 0.6, t)),
      smoothstep(A_FRENA + 0.3, A_FRENA + 0.5, t));
  }
  if (G == 3) return smoothstep(B_LUZ, B_LUZ + 0.3, t);
  return smoothstep(T_ATASCO + 0.4, T_ATASCO + 0.6, t);
}

float fuego(float t) {
  if (G != 0) return 0.0;
  float f = smoothstep(T_FUEGO, T_FUEGO + 2.0, t) * 0.7 + 0.5 * smoothstep(T_PUERTAS, T_PUERTAS + 0.4, t);
  return f * (0.8 + 0.2 * ruido(vec2(t * 15.0, 3.0)));
}

/* ------------------------------------------- guiones 2 y 3: sucesos */

// destello de cada golpe al panel (guion 2)
float chispazo(float t) {
  if (G != 2) return 0.0;
  float c = 0.0;
  for (int k = 0; k < 3; k++) {
    float t0 = k == 0 ? A_GOLPE1 : k == 1 ? A_GOLPE2 : A_GOLPE3;
    float u = t - t0;
    if (u > 0.0) c += exp(-u * 9.0) * (0.6 + 0.4 * step(0.5, h1(floor(t * 40.0) + float(k))));
  }
  return min(c, 1.0);
}

// chispas por la junta de las puertas mientras cae y frena (guion 2)
float juntaChispas(float t) {
  if (G != 2) return 0.0;
  return smoothstep(A_CAE, A_CAE + 0.3, t) * (1.0 - smoothstep(A_FRENA + 0.2, A_FRENA + 0.8, t)) * (0.6 + 0.4 * h1(floor(t * 30.0)));
}

// las garras abren las puertas una rendija (guion 3)
float garras(float t) {
  if (G != 3) return 0.0;
  return smoothstep(B_GARRAS + 0.4, B_GARRAS + 1.0, t) * (1.0 - smoothstep(B_PORTAZO - 0.06, B_PORTAZO, t));
}

// cuanto se vuelven a abrir las puertas al llegar (medio ancho de la abertura)
float abiertas(float t) {
  if (G == 2) return 0.55 * smoothstep(A_ABRE, A_ABRE + 1.6, t) + 0.02 * sin(t * 31.0) * step(A_ABRE, t);
  if (G == 3) return 0.12 * garras(t) + 0.5 * smoothstep(B_ABRE, B_ABRE + 2.0, t);
  return 0.0;
}

// vision nocturna de la videocamara (guion 3)
float noche(float t) {
  if (G != 3) return 0.0;
  return step(B_NOCHE, t) * (1.0 - step(B_LUZ, t));
}

// cuantos golpes en el techo van ya (guion 3) y el ultimo
float golpesTecho(float t, out float ultimo) {
  float n = 0.0;
  ultimo = -10.0;
  for (int k = 0; k < 6; k++) {
    float t0 = k == 0 ? 14.6 : k == 1 ? 15.4 : k == 2 ? 16.0 : k == 3 ? 17.0 : k == 4 ? 17.5 : 18.1;
    if (t > t0) { n += 1.0; ultimo = t0; }
  }
  return n;
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

// el vestibulo que se ve mientras se cierran las puertas, como el de verdad
// (PlanoVestibulo): la puerta de la cabina esta en z = 34 del mundo; delante,
// pizarra pulida, la raya amarilla y negra y el ajedrezado blanco y gris;
// techo de cuarzo liso con tubos y paredes de papel pintado con zocalo
vec3 vestibulo(vec3 ro, vec3 rd) {
  vec3 bmin = vec3(-9.5, 0.0, -40.0), bmax = vec3(9.5, 13.0, -1.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  float wx = floor(p.x);
  float wz = floor(p.z + 35.5);
  vec3 n;
  vec3 c;
  if (th == tf.y) {
    n = vec3(0.0, -sign(rd.y), 0.0);
    if (rd.y > 0.0) {
      bool tubo = mod(wx, 6.0) >= 2.0 && mod(wx, 6.0) < 4.0 && mod(wz, 6.0) >= 2.0 && mod(wz, 6.0) < 4.0;
      return tubo ? cara(TUBO, p.xz).rgb * 1.15 : cara(CUARZO_LISO, p.xz).rgb * 0.85;
    }
    if (wz >= 30.0) c = cara(PIZARRA_PULIDA, p.xz).rgb;
    else if (wz >= 29.0) c = cara(mod(wx, 2.0) == 0.0 ? HORMIGON_AMARILLO : HORMIGON_NEGRO, p.xz).rgb;
    else c = cara(mod(floor(wx / 2.0) + floor(wz / 2.0), 2.0) == 0.0 ? HORMIGON_BLANCO : HORMIGON_GRIS, p.xz).rgb;
  } else if (th == tf.x) {
    n = vec3(-sign(rd.x), 0.0, 0.0);
    c = p.y < 4.0 ? cara(MADERA_OSCURA, p.zy).rgb : cara(PAPEL, p.zy).rgb;
  } else {
    n = vec3(0.0, 0.0, 1.0);
    c = p.y < 4.0 ? cara(MADERA_OSCURA, p.xy).rgb : cara(PAPEL, p.xy).rgb;
  }
  // un poco de bruma hacia el fondo (es un vestibulo enorme)
  return mix(c * 0.9 * sombreado(n), vec3(0.45, 0.42, 0.33), clamp((th - 6.0) / 40.0, 0.0, 0.6));
}

// lo que se ve por las puertas abiertas de la cabina: el vestibulo de verdad si se puede
vec3 verVestibulo(vec3 ro, vec3 rd) {
  if (VIVO) {
    mundo = 1.0;
    return vec3(0.0);
  }
  return vestibulo(ro, rd);
}

// algo alto y encorvado que cruza el fondo del pasillo (guion 3, al cerrarse las puertas)
float silueta(vec3 ro, vec3 rd, float t, float lejos) {
  float u = (t - 1.4) / 1.5;
  if (u < 0.0 || u > 1.0 || rd.z >= 0.0) return 0.0;
  float zf = -14.0;
  float d = (zf - ro.z) / rd.z;
  if (d > lejos) return 0.0;
  vec3 q = ro + rd * d;
  float xs = mix(-3.2, 3.2, u);
  float x = q.x - xs;
  float y = q.y;
  float paso = sin(u * 18.0);
  // cuerpo largo y estrecho, cabeza adelantada, brazos que llegan al suelo
  float cuerpo = step(abs(x - 0.12 * (y - 1.5)), 0.22 - 0.05 * abs(y - 2.2)) * step(0.9, y) * step(y, 3.0);
  float cabeza = step(length(vec2(x - 0.35, y - 3.05) * vec2(1.0, 1.25)), 0.26);
  float brazo = step(abs(x + 0.28 - 0.06 * paso * (3.0 - y)), 0.06) * step(0.2, y) * step(y, 2.8);
  float piernas = step(abs(abs(x) - 0.12 - 0.08 * paso * (0.9 - y)), 0.07) * step(y, 0.95);
  return clamp(cuerpo + cabeza + brazo + piernas, 0.0, 1.0);
}

// una sonrisa y dos ojos que brillan en la oscuridad del sector C (guion 3)
vec3 sonrisa(vec3 ro, vec3 rd, float t) {
  if (t < B_CARA || rd.z >= 0.0) return vec3(0.0);
  float salto = smoothstep(B_SALTA, B_SALTA + 0.35, t);
  float zc = mix(-4.6, -1.5, salto * salto);
  float d = (zc - ro.z) / rd.z;
  vec3 q = ro + rd * d;
  vec2 f = (q.xy - vec2(0.0, 1.65 - 0.15 * salto)) / (0.75 + 0.3 * salto);
  float vis = smoothstep(B_CARA, B_CARA + 0.7, t) * (0.75 + 0.25 * step(0.2, h1(floor(t * 17.0))));
  float ojos = 0.0;
  for (int k = -1; k <= 1; k += 2) {
    vec2 e = (f - vec2(0.2 * float(k), 0.16)) / vec2(0.085, 0.05);
    ojos += step(dot(e, e), 1.0);
  }
  float curva = -0.3 + 0.9 * f.x * f.x;
  float boca = step(abs(f.x), 0.36) * step(abs(f.y - curva + 0.04), 0.075 * (1.0 - abs(f.x) * 1.6));
  float dientes = boca * step(fract(f.x * 13.0 + 0.5), 0.78);
  float brillo = clamp(ojos + dientes, 0.0, 1.0);
  float halo = exp(-dot(f, f) * 3.0) * 0.12;
  return (vec3(0.95, 0.97, 0.9) * brillo * 1.4 + vec3(0.6, 0.62, 0.55) * halo) * vis;
}

// un pasillo del Nivel 0 visto por las puertas del ascensor de salida
//   estilo 0: el Nivel 0 de la fase 1 (moqueta, papel, tubos, niebla amarilla)
//   estilo 1: el sector B (moqueta mojada, papel sucio, tubos que fallan)
//   estilo 2: el sector C (casi a oscuras, el bacilo come las paredes)
vec3 pasillo(vec3 ro, vec3 rd, float t, int estilo) {
  vec3 bmin = vec3(-2.5, 0.0, -45.0), bmax = vec3(2.5, 4.0, -2.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  int pared = estilo == 0 ? PAPEL : PAPEL_SUCIO;
  vec3 c;
  vec3 n;
  bool tubo = false;
  if (th == tf.y) {
    n = vec3(0.0, -sign(rd.y), 0.0);
    if (rd.y > 0.0) {
      tubo = abs(p.x) < 0.5 && mod(p.z, 4.0) < 1.0;
      c = tubo ? cara(estilo == 2 ? TUBO_OFF : TUBO, p.xz).rgb : cara(TECHO_N0, p.xz).rgb;
    } else {
      c = cara(estilo == 0 ? MOQUETA : MOQUETA_MOJADA, p.xz).rgb;
    }
  } else if (th == tf.x) {
    n = vec3(-sign(rd.x), 0.0, 0.0);
    c = cara(pared, p.zy).rgb;
    if (estilo == 2 && ruido(p.zy * 0.7) > 0.45) c = cara(BACILO, p.zy).rgb;
  } else {
    n = vec3(0.0, 0.0, 1.0);
    c = cara(pared, p.xy).rgb;
  }
  float luz;
  vec3 niebla;
  if (estilo == 0) {
    luz = 0.95;
    niebla = vec3(0.62, 0.56, 0.3);
  } else if (estilo == 1) {
    luz = 0.3 * (0.25 + 0.75 * step(0.3, h1(floor(t * 9.0) + 2.0)));
    niebla = vec3(0.07, 0.065, 0.04) * (0.4 + luz * 2.0);
    c *= vec3(0.82, 0.8, 0.62);
  } else {
    // solo llega la luz roja de la cabina
    luz = 0.0;
    niebla = vec3(0.0);
  }
  vec3 col = tubo && estilo != 2 ? c * (0.4 + luz) * 1.2 : c * luz * sombreado(n);
  if (estilo == 2) col = c * vec3(1.0, 0.25, 0.18) * 0.55 * emergencia(t) * exp(-max(0.0, -p.z - 2.5) / 2.2) * sombreado(n);
  col = mix(col, niebla, 1.0 - exp(-th / 16.0));
  if (G == 3 && estilo == 1) col = mix(col, vec3(0.008, 0.007, 0.006), silueta(ro, rd, t, th) * 0.96);
  if (estilo == 2) col += sonrisa(ro, rd, t);
  return col;
}

// lo que hay al otro lado de las puertas del ascensor de salida (guiones 2 y 3)
vec3 exterior(vec3 ro, vec3 rd, float t) {
  if (t < T_CIERRA1 + 0.2) return pasillo(ro, rd, t, G == 2 ? 0 : 1);
  if (G == 3 && t < B_ABRE) return vec3(0.004); // la rendija de las garras: el hueco, a oscuras
  return pasillo(ro, rd, t, G == 2 ? 1 : 2);
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
  if (abs(x) < g) return verVestibulo(ro, rd);
  // las hojas de hierro deslizan hacia el centro
  float u = x < 0.0 ? x + 1.0 + g : 1.0 - (x - g);
  float temblor = t > T_ROTURA ? (h1(floor(t * 30.0)) - 0.5) * 0.03 : 0.0;
  vec4 d = y < 1.0 ? cara(PUERTA_ABAJO, vec2(u + temblor, y)) : cara(PUERTA_ARRIBA, vec2(u + temblor, y - 1.0));
  if (d.a < 0.5) {
    return t < T_CIERRA1 ? verVestibulo(ro, rd) : vistaHueco(ro, rd, t);
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

/* ------------------------------ guiones 2 y 3: el ascensor de salida */

// como en el mundo (GeneradorNivel0#ascensor): 5x5 de acero, suelo de ascensor,
// 3x3 de luz en el techo, la entrada (2 de ancho) en z = -2.5 y el panel en la
// pared de enfrente (z = 2.5). La camara empieza mirando al panel que acaba de pulsar.

vec3 luzF(vec3 p, vec3 n, float t) {
  vec3 q = p + n * 0.5;
  vec3 c = vec3(0.012);
  c += fuente(q, vec3(0.0, 3.5, 0.0), 15.0 * lampara(t), vec3(1.0, 0.97, 0.92));
  float e = emergencia(t);
  vec3 rojo = vec3(1.0, 0.22, 0.15) * (0.75 + 0.25 * sin(t * 4.0));
  c += fuente(q, vec3(-2.0, 3.2, 0.0), 10.0 * e, rojo);
  c += fuente(q, vec3(2.0, 3.2, 0.0), 10.0 * e, rojo);
  c += fuente(q, vec3(0.0, 1.5, 2.3), 14.0 * chispazo(t), vec3(1.0, 0.85, 0.5));
  c += fuente(q, vec3(0.0, 0.8, -2.4), 13.0 * juntaChispas(t), vec3(1.0, 0.6, 0.25));
  // por las puertas abiertas entra la luz del pasillo
  float pas = t < T_CIERRA1 ? 1.0 - smoothstep(T_CIERRA0, T_CIERRA1, t) : (G == 2 ? abiertas(t) * 0.7 : 0.0);
  c += fuente(q, vec3(0.0, 2.0, -3.2), 12.0 * pas, vec3(1.0, 0.92, 0.6));
  // el foco infrarrojo de la videocamara en vision nocturna
  c += vec3(0.42) * noche(t);
  return c;
}

vec3 iluminarF(vec3 alb, vec3 p, vec3 n, float t) {
  return alb * min(luzF(p, n, t), vec3(1.5)) * sombreado(n);
}

vec3 paredPuertasF(vec3 p, vec3 n, vec3 ro, vec3 rd, float t) {
  float x = p.x;
  float y = p.y;
  if (y >= 3.0 || abs(x) >= 1.0) return iluminarF(cara(ACERO, vec2(x + 0.5, y)).rgb, p, n, t);
  float g = max(1.0 - smoothstep(T_CIERRA0, T_CIERRA1, t), abiertas(t));
  if (abs(x) < g) return exterior(ro, rd, t);
  // las dos hojas de acero del rellano, que se juntan en el medio
  float temblor = (G == 2 && t > A_CAE && t < A_FRENA + 0.4) ? (h1(floor(t * 30.0)) - 0.5) * 0.03 : 0.0;
  vec4 d = x < 0.0 ? cara(PUERTA_IZQ, vec2(x + 1.0 + g + temblor, y)) : cara(PUERTA_DER, vec2(x - g + temblor, y));
  vec3 c = iluminarF(d.rgb, p, n, t);
  // chispas por la junta al caer
  c += vec3(1.0, 0.6, 0.2) * juntaChispas(t) * smoothstep(0.1, 0.0, abs(x)) * 1.2;
  // garras: dedos negros y larguisimos que asoman por la rendija y se agarran a las hojas
  float ga = garras(t);
  if (ga > 0.0) {
    float borde = abs(x) - g;
    for (int k = 0; k < 4; k++) {
      float yd = 1.15 + float(k) * 0.32 + 0.03 * sin(t * 7.0 + float(k));
      float largo = (0.2 + 0.08 * h1(float(k) * 3.7 + step(0.0, x))) * ga;
      if (borde < largo && abs(y - yd - borde * 0.3) < 0.06 * (1.0 - borde / max(largo, 0.001)) + 0.012) {
        return vec3(0.015, 0.012, 0.01) + vec3(0.05) * noche(t);
      }
    }
  }
  return c;
}

vec3 paredLadoF(vec3 p, vec3 n, float t) {
  float z = p.z;
  return iluminarF(cara(ACERO, vec2(z + 0.5, p.y)).rgb, p, n, t);
}

// la pared del fondo, con el panel de botones que se pulsa para bajar
vec3 paredFondoF(vec3 p, vec3 n, float t) {
  float x = p.x;
  float y = p.y;
  if (abs(x) < 0.5 && y > 1.0 && y < 2.0) {
    vec4 b = cara(PANEL_ASC, vec2(0.5 - x, y - 1.0));
    vec3 c = iluminarF(b.rgb, p, n, t);
    // la pantallita roja: de error tras el frenazo (parpadea) y apagada sin luz
    bool pantalla = b.r > 0.55 && b.g < 0.35;
    if (pantalla) {
      float on = 1.0;
      if (G == 2 && t > A_FRENO && t < A_ARREGLO) on = step(0.5, fract(t * 2.5));
      if (G == 2 && t > A_FRENA) on = step(0.5, fract(t * 3.0));
      if (G == 3 && t > B_APAGON - 0.3 && t < B_LUZ) on = 0.0;
      return mix(c, vec3(1.0, 0.18, 0.08) * 1.5, on);
    }
    // los botones naranjas se encienden a lo loco con cada golpe
    if (b.r > 0.7 && b.g > 0.4 && b.b < 0.3) return mix(c, vec3(1.0, 0.7, 0.25) * 1.4, chispazo(t));
    return c;
  }
  return iluminarF(cara(ACERO, vec2(x + 0.5, y)).rgb, p, n, t);
}

vec3 techoF(vec3 p, vec3 n, float t) {
  vec3 c;
  if (abs(p.x) < 1.5 && abs(p.z) < 1.5) {
    float l = lampara(t);
    c = l > 0.05 ? cara(LUZ_ASC, p.xz + 0.5).rgb * (0.35 + 0.85 * l) : iluminarF(cara(LUZ_ASC, p.xz + 0.5).rgb * 0.35, p, n, t);
  } else {
    c = iluminarF(cara(ACERO, p.xz + 0.5).rgb, p, n, t);
  }
  // golpes desde arriba: la chapa se abolla (guion 3)
  if (G == 3 && abs(p.x) < 1.3 && abs(p.z) < 1.3) {
    float ultimo;
    float n2 = golpesTecho(t, ultimo);
    if (n2 > 0.0) {
      int etapa = int(clamp(n2 * 1.6, 0.0, 9.0));
      vec4 gr = cara(GRIETA0 + etapa, p.xz * 0.8 + 0.5);
      c *= mix(vec3(1.0), gr.rgb * 1.6, gr.a * 0.85);
    }
  }
  return c;
}

vec3 sueloF(vec3 p, vec3 n, float t) {
  return iluminarF(cara(ASC_SUELO, p.xz + 0.5).rgb, p, n, t);
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

void particulasF(inout vec3 col, Camara c, vec2 uv, float zEscena, float t) {
  float ultimo;
  float n = golpesTecho(t, ultimo);
  // polvo del techo: en cada frenazo (guion 2) y con cada golpe de arriba (guion 3)
  for (int i = 0; i < 24; i++) {
    float fi = float(i);
    float t0;
    if (G == 2) t0 = i < 12 ? A_FRENO : A_FRENA;
    else t0 = i < 18 ? (n > 0.0 ? ultimo : -10.0) : B_LURCH;
    float u = t - t0 - h1(fi * 3.3) * 0.5;
    if (u < 0.0 || u > 2.4) continue;
    vec3 P = vec3((h1(fi + floor(t0)) - 0.5) * 3.6, 3.95 - u * 1.1, (h1(fi * 1.9 + floor(t0)) - 0.5) * 3.6);
    vec3 tinte = vec3(0.75, 0.72, 0.65) * (0.25 + 0.7 * lampara(t) + emergencia(t) * 0.3 + noche(t) * 0.6);
    particula(col, c, uv, zEscena, P, 0.07, P_POLVO, tinte, 1.0);
  }
  if (G != 2) return;
  // chispas del panel con cada golpe
  for (int k = 0; k < 3; k++) {
    float t0 = k == 0 ? A_GOLPE1 : k == 1 ? A_GOLPE2 : A_GOLPE3;
    float u0 = t - t0;
    if (u0 < 0.0 || u0 > 0.9) continue;
    for (int i = 0; i < 10; i++) {
      float fi = float(i + k * 10);
      float u = u0 - h1(fi * 2.1) * 0.12;
      if (u < 0.0) continue;
      vec3 o = vec3((h1(fi) - 0.5) * 0.5, 1.4 + h1(fi * 3.0) * 0.4, 2.42);
      vec3 vel = vec3((h1(fi * 2.3) - 0.5) * 3.0, 0.5 + h1(fi * 4.1) * 2.0, -1.0 - h1(fi * 6.7) * 2.5);
      vec3 P = o + vel * u + vec3(0.0, -4.5, 0.0) * u * u;
      if (P.y < 0.0) continue;
      particula(col, c, uv, zEscena, P, 0.08, P_LAVA, vec3(1.6), 1.0);
    }
  }
  // chispas que entran por la junta de las puertas mientras cae
  if (juntaChispas(t) > 0.0) {
    for (int i = 0; i < 16; i++) {
      float fi = float(i);
      float periodo = 0.6 + h1(fi * 3.1) * 0.5;
      float u = mod(t + h1(fi) * 5.0, periodo);
      vec3 o = vec3((h1(fi * 4.0) - 0.5) * 0.2, 0.2 + h1(fi * 6.0) * 2.4, -2.45);
      vec3 vel = vec3((h1(fi * 2.3) - 0.5) * 2.0, 0.6 + h1(fi * 4.1) * 1.5, 1.5 + h1(fi * 6.7) * 2.0);
      vec3 P = o + vel * u + vec3(0.0, -3.0, 0.0) * u * u;
      if (P.y < 0.0) continue;
      particula(col, c, uv, zEscena, P, 0.09, P_LAVA, vec3(1.5), 1.0);
    }
  }
}

/* ---------------------------------------------------------- escena */

// guiones 2 y 3: un jugador de pie en medio del ascensor de salida, con la videocamara
vec3 escenaF(vec2 uv, float t) {
  float ultimo;
  float n = golpesTecho(t, ultimo);
  float sac = 0.004;
  if (G == 2 && t > A_CAE && t < A_FRENA + 0.6) sac = 0.03;
  if (G == 3) sac += 0.012 * exp(-max(0.0, t - ultimo) * 5.0) + 0.02 * step(B_SALTA, t);
  vec3 sh = (vec3(ruido(vec2(t * 13.0, 1.0)), ruido(vec2(t * 11.0, 5.0)), ruido(vec2(t * 9.0, 9.0))) - 0.5) * 2.0 * sac;
  vec3 ro = vec3(0.0, 1.62, 0.0) + sh;
  // empieza mirando al panel que acaba de pulsar y se gira hacia las puertas
  float yaw = 3.14159 * (1.0 - smoothstep(0.9, 2.1, t));
  float pitch = -0.08;
  float roll = sh.z * 2.0;
  if (G == 2) {
    // frenazo: se gira al panel, lo arregla a golpes, vuelve a mirar a las puertas
    yaw += 3.14159 * smoothstep(A_GIRO, A_GIRO + 1.3, t) + 3.14159 * smoothstep(A_VUELTA, A_VUELTA + 1.3, t);
    pitch -= 0.1 * smoothstep(A_GIRO, A_GIRO + 1.3, t) * (1.0 - smoothstep(A_VUELTA, A_VUELTA + 1.3, t));
    ro.z += 1.3 * smoothstep(A_GIRO + 0.9, A_GOLPE1 - 0.4, t) * (1.0 - smoothstep(A_ARREGLO + 0.2, A_VUELTA + 0.8, t));
    for (int k = 0; k < 3; k++) {
      float t0 = k == 0 ? A_GOLPE1 : k == 1 ? A_GOLPE2 : A_GOLPE3;
      ro.z += 0.35 * smoothstep(t0 - 0.28, t0, t) * (1.0 - smoothstep(t0, t0 + 0.4, t));
      roll += golpe(t, t0, 0.06);
    }
    ro.y -= 0.18 * abs(golpe(t, A_FRENO, 1.0)) + 0.25 * abs(golpe(t, A_FRENA, 1.0));
    ro.y += 0.25 * smoothstep(A_CAE + 0.2, A_FRENA, t) * (1.0 - step(A_FRENA, t)); // al caer flotas un poco
    roll += golpe(t, A_FRENO, 0.25) + golpe(t, A_FRENA, 0.35) + golpe(t, A_CAE, 0.12);
    // al final andas hacia la abertura
    float anda = smoothstep(A_ANDA, A_FIN, t);
    ro.z -= 1.9 * anda;
    ro.y += 0.035 * sin(t * 9.0) * step(A_ANDA, t);
    pitch -= 0.05 * anda;
  } else {
    // mira al techo mientras golpean y vuelve a las puertas cuando hay luz
    float arriba = smoothstep(B_MIRA, B_MIRA + 0.8, t) * (1.0 - smoothstep(B_LUZ, B_LUZ + 1.2, t));
    pitch += 0.75 * arriba;
    yaw += 0.15 * arriba * sin(t * 0.7);
    roll += golpe(t, ultimo, 0.08) + golpe(t, B_LURCH, 0.3) + golpe(t, B_PORTAZO, 0.12) + golpe(t, B_SALTA, 0.4);
    ro.y -= 0.12 * abs(golpe(t, ultimo, 1.0)) + 0.3 * abs(golpe(t, B_LURCH, 1.0));
    // se asoma a la abertura... y retrocede cuando se le echa encima
    ro.z -= 0.6 * smoothstep(B_ABRE + 1.5, B_CARA + 0.8, t) - 0.9 * smoothstep(B_SALTA, B_SALTA + 0.2, t);
  }
  vec3 f = normalize(vec3(sin(yaw) * cos(pitch), sin(pitch), -cos(yaw) * cos(pitch)));
  vec3 r = normalize(cross(f, vec3(0.0, 1.0, 0.0)));
  vec3 up = cross(r, f);
  Camara c;
  c.ro = ro;
  c.f = f;
  c.r = r * cos(roll) + up * sin(roll);
  c.u = up * cos(roll) - r * sin(roll);
  vec3 rd = normalize(f + (uv.x * c.r + uv.y * c.u) * FOVK);

  vec3 bmin = vec3(-2.5, 0.0, -2.5), bmax = vec3(2.5, 4.0, 2.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  vec3 col;
  if (th == tf.y) {
    vec3 nn = vec3(0.0, -sign(rd.y), 0.0);
    col = rd.y > 0.0 ? techoF(p, nn, t) : sueloF(p, nn, t);
  } else if (th == tf.x) {
    col = paredLadoF(p, vec3(-sign(rd.x), 0.0, 0.0), t);
  } else if (rd.z < 0.0) {
    col = paredPuertasF(p, vec3(0.0, 0.0, 1.0), ro, rd, t);
  } else {
    col = paredFondoF(p, vec3(0.0, 0.0, -1.0), t);
  }
  particulasF(col, c, uv, th * dot(rd, f), t);
  return col;
}

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

  float glitch;
  if (G == 2) {
    glitch = 0.12 + abs(golpe(t, A_FRENO, 0.6) + golpe(t, A_GOLPE3, 0.3) + golpe(t, A_FRENA, 0.9)) + 0.6 * smoothstep(A_FIN - 1.0, A_FIN, t);
  } else if (G == 3) {
    float ultimo;
    golpesTecho(t, ultimo);
    glitch = 0.15 + abs(golpe(t, B_PORTAZO, 0.5) + golpe(t, ultimo, 0.4) + golpe(t, B_LURCH, 1.0) + golpe(t, B_SALTA, 1.2)) + 0.25 * noche(t);
  } else {
    glitch = 0.12 + abs(golpe(t, T_ATASCO, 0.6) + golpe(t, T_ROTURA, 0.9) + golpe(t, T_PUERTAS, 0.6));
    glitch += 0.3 * smoothstep(T_ROTURA, T_IMPACTO, t);
  }
  float banda = fract(t * 0.21 + h1(floor(t * 2.0)) * 0.3);
  float enBanda = smoothstep(0.03, 0.0, abs(uv01.y - banda));
  float dx = (h2(vec2(floor(uv01.y * 160.0), floor(t * 30.0))) - 0.5) * 0.04 * enBanda * glitch;
  float rueda = t < 1.3 ? (1.3 - t) * 1.5 : 0.0;
  vec2 uvv = vec2(uv01.x + dx, fract(uv01.y + rueda));
  vec2 uv = (uvv * 2.0 - 1.0) * vec2(res.x / res.y, 1.0);
  vec3 col = G == 0 ? escena(uv, t) : escenaF(uv, t);

  if (G == 0) {
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
  } else {
    // vision nocturna: verde, granulada y con el foco infrarrojo en el centro
    float nv = noche(t);
    if (nv > 0.0) {
      float lum = dot(col, vec3(0.3, 0.59, 0.11));
      vec2 v0 = uv01 - 0.5;
      float foco = 1.15 - dot(v0, v0) * 1.6;
      float grano = (h2(frag * 0.5 + floor(t * 24.0) * 13.0) - 0.5) * 0.18;
      col = vec3(0.32, 1.0, 0.42) * clamp(lum * 1.7 * foco + grano + 0.02, 0.0, 1.2);
    }
    // el final de la cinta: la imagen se rompe en nieve y negro
    float fin = G == 2 ? A_FIN : B_FIN;
    float rompe = G == 2 ? smoothstep(fin - 0.6, fin, t) : smoothstep(B_SALTA + 0.25, B_SALTA + 0.4, t);
    if (rompe > 0.0) {
      float n = h2(floor(frag / vec2(3.0, 1.0)) + floor(t * 50.0));
      col = mix(col, vec3(n) * 0.85, rompe);
      col *= 1.0 - smoothstep(fin - 0.25, fin, t);
    }
  }

  // look de videocamara, suave para que se vean los pixeles de los bloques
  col = pow(max(col, 0.0), vec3(0.95)) * vec3(1.03, 1.0, 0.93) + vec3(0.015, 0.012, 0.02);
  col *= 0.95 + 0.05 * sin(frag.y * 3.14159);
  col += (h2(frag + fract(t) * 97.0) - 0.5) * 0.05;
  vec2 v = uv01 - 0.5;
  col *= 1.0 - dot(v, v) * 0.7;
  col *= smoothstep(0.9, 1.4, t);
  // el vestibulo de verdad se ve detras, con un poco del grano y la vineta de la cinta encima
  fragColor = vec4(clamp(col, 0.0, 1.0), 1.0 - mundo * 0.88 * smoothstep(0.9, 1.4, t));
}
