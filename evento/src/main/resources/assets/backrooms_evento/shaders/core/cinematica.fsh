#version 330

// Cinematica del /start, grabada como la cinta de una videocamara y de un
// solo plano: la sala de reuniones del Proyecto Umbral (te acercan el
// consentimiento, lo lees y lo firmas), el pasillo hasta el ascensor y la
// bajada. El ascensor esta hecho de bloques de Minecraft: cabina de 3x3 de
// hierro y roble oscuro, puertas de hierro con sus ventanitas, lampara de
// redstone en el techo y el hueco de ladrillos de piedra con un rellano y una
// antorcha por planta (mas abajo pizarra profunda, piedra negra y magma).
//
// Las texturas son las del propio juego: AtlasCinematica.java las copia en
// tiempo de ejecucion a Sampler0 (16 x 7 casillas). Sampler1 es el lienzo
// (LienzoCinematica.java): el contrato con el nombre del jugador, su firma y
// los carteles. La luz va por niveles
// como en Minecraft (15 menos la distancia en bloques, la curva del lightmap
// y el sombreado por cara) y las particulas son cuadrados de cara a camara.
//
// Todo es funcion del tiempo, que llega por la U de los vertices. La V dice el
// guion (G):
//   0  el /start: de la sala de reuniones al Nivel 0 (cabina de hierro de 3x3)
//   2  la bajada por el ascensor de salida (acero, 5x5): se atasca, lo arreglas
//      a golpes, vuelve a fallar y cae; se abre a medias abajo
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 datos;
in vec4 color;
out vec4 fragColor;

#define T datos.x
#define G int(datos.y + 0.5)

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

// guion 2 (bajada por el ascensor de salida)
const float A_FRENO = 7.6;     // frena en seco
const float A_GIRO = 8.4;      // te das la vuelta hacia el panel
const float A_GOLPE1 = 10.6, A_GOLPE2 = 11.6, A_GOLPE3 = 12.5; // golpes al panel
const float A_ARREGLO = 13.2;  // vuelve la luz
const float A_VUELTA = 14.0;   // vuelves a mirar a las puertas
const float A_SIGUE = 15.0;    // sigue bajando
const float A_PARPADEO2 = 19.4;
const float A_CAE = 20.4;      // se escurre el cable: cae
const float A_FRENA = 21.6;    // el freno de emergencia lo para
const float A_ABRE = 24.0;     // las puertas se abren a medias (las Piscinas)
const float A_ANDA = 25.5;     // andas hacia la abertura
const float A_FIN = 27.8;      // se acaba la cinta

// guion 0: el prologo, antes del ascensor (en segundos desde el principio; el
// ascensor va en te = t - PRO, ver main)
const float PRO = 36.5;
const float P_DESLIZA0 = 2.4, P_DESLIZA1 = 4.0;   // te acerca el papel por la mesa
const float P_BOLI0 = 4.1, P_BOLI1 = 4.8;         // y el boligrafo
const float P_LEE0 = 6.0, P_LEE1 = 18.0;          // te inclinas a leer: 12 s hasta la linea de la firma
const float P_COGE0 = 17.4, P_COGE1 = 18.7;       // coges el boligrafo
const float P_FIRMA0 = 18.9, P_FIRMA_DUR = 3.4;   // firmas
const float P_SUELTA0 = 22.7, P_SUELTA1 = 23.5;   // lo dejas en la mesa
const float P_ALZA0 = 23.4, P_ALZA1 = 24.7;       // te incorporas
const float P_RECOGE0 = 24.5, P_RECOGE1 = 25.6;   // se lleva el papel
const float P_LEVANTA0 = 26.0, P_LEVANTA1 = 27.2; // te levantas
const float P_ANDA0 = 28.0, P_ANDA1 = 35.2;       // le sigues hasta el ascensor
const float P_GIRA0 = 34.8, P_GIRA1 = 36.3;       // dentro, te das la vuelta
const float I_ANDA0 = 25.4, I_ANDA1 = 31.4;       // el investigador va delante
const float I_LLAMA = 31.5;                       // llama al ascensor
const float I_PUERTA0 = 34.0, I_PUERTA1 = 35.4;   // se pone delante de las puertas
const float I_ADIOS = 36.7;                       // "le veremos al otro lado"
const float T_ABRE0 = I_LLAMA + 0.5 - PRO, T_ABRE1 = I_LLAMA + 1.7 - PRO; // se abren (tiempo del ascensor)

// cuanto estan abiertas las puertas del ascensor del /start (medio ancho del hueco)
float puertaAscensor(float te) {
  return smoothstep(T_ABRE0, T_ABRE1, te) * (1.0 - smoothstep(T_CIERRA0, T_CIERRA1, te));
}

// donde esta la camara (para que el investigador te mire y el boligrafo se oriente)
vec3 camGlobal = vec3(0.0);

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
// fila 6: la sala de reuniones y el pasillo (como la recepcion)
const int TERRACOTA_CIAN = 96, DIORITA = 97, LANA_GRIS = 98, LANA_CIAN = 99, LUZ_TECHO = 100, PUERTA_OSCURA_ARRIBA = 101;
const int PUERTA_OSCURA_ABAJO = 102, TRONCO_DESC = 103, ABETO = 104, LIBRERIA = 105, MACETA = 106, AZALEA = 107;
const int TRONCO_OSCURO = 110;

vec4 tex(int casilla, vec2 uv) {
  vec2 px = floor(clamp(uv, 0.0, 0.9999) * 16.0) + 0.5;
  vec2 c = vec2(float(casilla % 16), float(casilla / 16));
  return texture(Sampler0, (c * 16.0 + px) / vec2(256.0, 112.0));
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

float bajada(float t) {
  return G == 2 ? bajada2(t) : bajada0(t);
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
  return smoothstep(T_ATASCO + 0.4, T_ATASCO + 0.6, t);
}

float fuego(float t) {
  if (G != 0) return 0.0;
  float f = smoothstep(T_FUEGO, T_FUEGO + 2.0, t) * 0.7 + 0.5 * smoothstep(T_PUERTAS, T_PUERTAS + 0.4, t);
  return f * (0.8 + 0.2 * ruido(vec2(t * 15.0, 3.0)));
}

/* -------------------------------------------------- guion 2: sucesos */

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

// cuanto se vuelven a abrir las puertas al llegar (medio ancho de la abertura)
float abiertas(float t) {
  if (G == 2) return 0.55 * smoothstep(A_ABRE, A_ABRE + 1.6, t) + 0.02 * sin(t * 31.0) * step(A_ABRE, t);
  return 0.0;
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
  // con las puertas abiertas entra la luz del pasillo
  if (G == 0) c += fuente(q, vec3(0.0, 2.2, -2.6), 13.0 * puertaAscensor(t), vec3(1.0, 0.98, 0.94));
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

// un pasillo del Nivel 0 (moqueta, papel, tubos, niebla amarilla) visto por las
// puertas del ascensor de salida
vec3 pasillo(vec3 ro, vec3 rd, float t) {
  vec3 bmin = vec3(-2.5, 0.0, -45.0), bmax = vec3(2.5, 4.0, -2.5);
  vec3 t1 = (bmin - ro) / rd, t2 = (bmax - ro) / rd;
  vec3 tf = max(t1, t2);
  float th = min(min(tf.x, tf.y), tf.z);
  vec3 p = ro + rd * th;
  int pared = PAPEL;
  vec3 c;
  vec3 n;
  bool tubo = false;
  if (th == tf.y) {
    n = vec3(0.0, -sign(rd.y), 0.0);
    if (rd.y > 0.0) {
      tubo = abs(p.x) < 0.5 && mod(p.z, 4.0) < 1.0;
      c = tubo ? cara(TUBO, p.xz).rgb : cara(TECHO_N0, p.xz).rgb;
    } else {
      c = cara(MOQUETA, p.xz).rgb;
    }
  } else if (th == tf.x) {
    n = vec3(-sign(rd.x), 0.0, 0.0);
    c = cara(pared, p.zy).rgb;
  } else {
    n = vec3(0.0, 0.0, 1.0);
    c = cara(pared, p.xy).rgb;
  }
  float luz = 0.95;
  vec3 niebla = vec3(0.62, 0.56, 0.3);
  vec3 col = tubo ? c * (0.4 + luz) * 1.2 : c * luz * sombreado(n);
  col = mix(col, niebla, 1.0 - exp(-th / 16.0));
  return col;
}

/* ------------------------------------------- las Piscinas (guion 2) */

// Lo que se ve al abrirse las puertas abajo: una sala de azulejo blanco con una
// piscina de calles, columnas cuadradas, paneles de luz en el techo y niebla
// clara al fondo (como las salas "piscina" de GeneradorPiscinas). El agua refleja
// la sala y deja ver el fondo con las causticas. Coordenadas de la cabina: la
// entrada esta en z = -2.5 y la sala sigue hacia -z.

const float PS_AGUA = -0.12;
const float PS_FONDO = -2.1;
const float PS_TECHO = 6.0;
const float PS_ANCHO = 9.0;
const float PS_FIN = -38.0;
const vec2 PS_X = vec2(-6.5, 6.5);   // la piscina
const vec2 PS_Z = vec2(-31.0, -5.5);

vec2 h22(vec2 p) { return fract(sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) * 43758.5453); }

float celdasPs(vec2 p, float t) {
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

float causticaPs(vec2 p, float t) {
  float a = celdasPs(p * 1.6, t);
  float b = celdasPs(p * 2.3 + 7.3, t * 1.3);
  return a * 0.65 + b * 0.5 + a * b;
}

// azulejo de 4 piezas por bloque, con la junta clara (como azulejo.png)
vec3 azulejoPs(vec2 uv, vec3 base, vec3 junta) {
  vec2 c = uv * 4.0;
  vec2 f = abs(fract(c) - 0.5);
  float j = smoothstep(0.43, 0.47, max(f.x, f.y));
  vec3 b = base * (0.97 + 0.05 * h2(floor(c)));
  return mix(b, junta, j);
}

bool enPiscinaPs(vec2 xz) {
  return xz.x > PS_X.x && xz.x < PS_X.y && xz.y > PS_Z.x && xz.y < PS_Z.y;
}

// columnas de 1 de lado a x = +-4.5, cada 6 a partir de z = -7
float columnaPs(vec3 ro, vec3 rd, out vec3 n) {
  float mejor = 1e9;
  n = vec3(0.0, 1.0, 0.0);
  for (int k = 0; k < 5; k++) {
    for (int s = -1; s <= 1; s += 2) {
      vec3 c = vec3(4.5 * float(s), 2.0, -7.0 - 6.0 * float(k));
      vec3 h = vec3(0.5, 4.2, 0.5);
      vec3 m = 1.0 / rd;
      vec3 a = (c - h - ro) * m, b = (c + h - ro) * m;
      vec3 t1 = min(a, b), t2 = max(a, b);
      float tn = max(max(t1.x, t1.y), t1.z), tf = min(min(t2.x, t2.y), t2.z);
      if (tn < tf && tn > 0.001 && tn < mejor) {
        mejor = tn;
        n = tn == t1.x ? vec3(-sign(rd.x), 0.0, 0.0) : tn == t1.y ? vec3(0.0, -sign(rd.y), 0.0) : vec3(0.0, 0.0, -sign(rd.z));
      }
    }
  }
  return mejor;
}

// cuanta luz llega a p: la de los paneles del techo (cada 4 en x y z) y un fondo claro
float luzPs(vec3 p, vec3 n) {
  vec2 q = vec2(mod(p.x + 2.0, 4.0) - 2.0, mod(p.z + 1.0, 4.0) - 2.0);
  float d = length(vec3(q.x, PS_TECHO - p.y, q.y));
  float l = 0.62 + 0.55 * exp(-d * d * 0.03);
  return l * sombreado(n);
}

// traza la sala; conAgua: el agua corta el rayo. que: 0 suelo, 1 dentro de la piscina,
// 2 pared, 3 techo, 4 columna, 5 agua, 6 arco oscuro del fondo
float trazaPs(vec3 ro, vec3 rd, bool conAgua, out vec3 n, out int que) {
  float th = 1e9;
  que = 2;
  n = vec3(0.0, 0.0, 1.0);
  if (rd.y > 1e-4) {
    float tt = (PS_TECHO - ro.y) / rd.y;
    if (tt < th) { th = tt; que = 3; n = vec3(0.0, -1.0, 0.0); }
  }
  if (rd.y < -1e-4) {
    float ts = (0.0 - ro.y) / rd.y;
    vec3 ps = ro + rd * ts;
    if (ts > 0.0 && ts < th && !enPiscinaPs(ps.xz)) { th = ts; que = 0; n = vec3(0.0, 1.0, 0.0); }
    float ta = (PS_AGUA - ro.y) / rd.y;
    vec3 pa = ro + rd * ta;
    if (conAgua && ta > 0.0 && ta < th && enPiscinaPs(pa.xz)) { th = ta; que = 5; n = vec3(0.0, 1.0, 0.0); }
    float tf = (PS_FONDO - ro.y) / rd.y;
    vec3 pf = ro + rd * tf;
    if (tf > 0.0 && tf < th && enPiscinaPs(pf.xz)) { th = tf; que = 1; n = vec3(0.0, 1.0, 0.0); }
  }
  // paredes de dentro de la piscina (solo por debajo del borde, vistas desde dentro)
  for (int k = 0; k < 2; k++) {
    float xw = k == 0 ? PS_X.x : PS_X.y;
    float tw = (xw - ro.x) / rd.x;
    vec3 pw = ro + rd * tw;
    if (tw > 1e-3 && tw < th && pw.y < 0.0 && pw.y > PS_FONDO && pw.z > PS_Z.x && pw.z < PS_Z.y && sign(rd.x) == (k == 0 ? -1.0 : 1.0)) {
      th = tw; que = 1; n = vec3(-sign(rd.x), 0.0, 0.0);
    }
    float zw = k == 0 ? PS_Z.x : PS_Z.y;
    float tz = (zw - ro.z) / rd.z;
    vec3 pz = ro + rd * tz;
    if (tz > 1e-3 && tz < th && pz.y < 0.0 && pz.y > PS_FONDO && pz.x > PS_X.x && pz.x < PS_X.y && sign(rd.z) == (k == 0 ? -1.0 : 1.0)) {
      th = tz; que = 1; n = vec3(0.0, 0.0, -sign(rd.z));
    }
  }
  float tx = ((rd.x > 0.0 ? PS_ANCHO : -PS_ANCHO) - ro.x) / rd.x;
  if (tx > 0.0 && tx < th) { th = tx; que = 2; n = vec3(-sign(rd.x), 0.0, 0.0); }
  if (rd.z < 0.0) {
    float tz = (PS_FIN - ro.z) / rd.z;
    if (tz > 0.0 && tz < th) {
      th = tz; que = 2; n = vec3(0.0, 0.0, 1.0);
      vec3 pf = ro + rd * tz;
      // arcos oscuros en la pared del fondo
      float ax = mod(pf.x + 3.0, 6.0) - 3.0;
      if (abs(ax) < 1.1 && pf.y > 0.0 && pf.y < 2.2 + sqrt(max(0.0, 1.21 - ax * ax))) que = 6;
    }
  }
  vec3 nc;
  float tc = columnaPs(ro, rd, nc);
  if (tc < th) { th = tc; que = 4; n = nc; }
  return th;
}

vec3 colorPs(vec3 p, vec3 n, int que, float t) {
  vec3 blanco = vec3(0.93, 0.97, 0.96), junta = vec3(0.70, 0.78, 0.79);
  if (que == 6) return vec3(0.02, 0.05, 0.06);
  if (que == 3) {
    vec2 q = vec2(mod(p.x + 2.0, 4.0) - 2.0, mod(p.z + 1.0, 4.0) - 2.0);
    if (abs(q.x) < 0.5 && abs(q.y) < 0.5) return vec3(1.25, 1.3, 1.32); // panel encendido
    return azulejoPs(p.xz, blanco, junta) * luzPs(p, n) * 0.9;
  }
  vec2 uv = abs(n.y) > 0.5 ? p.xz : abs(n.x) > 0.5 ? p.zy : p.xy;
  if (que == 1) {
    vec3 c = azulejoPs(uv, vec3(0.80, 0.93, 0.93), vec3(0.55, 0.74, 0.76));
    // franjas de las calles en el fondo
    if (n.y > 0.5 && abs(mod(p.x + 1.5, 3.0) - 1.5) < 0.25 && p.z < PS_Z.y - 1.5 && p.z > PS_Z.x + 1.5) c = vec3(0.17, 0.27, 0.58);
    c += vec3(0.55, 0.85, 0.9) * causticaPs(p.xz * 0.9 + p.y * 0.3, t) * 0.45;
    return c * luzPs(p, n) * 0.9;
  }
  bool borde = que == 0 && abs(p.x - clamp(p.x, PS_X.x, PS_X.y)) < 0.8 && abs(p.z - clamp(p.z, PS_Z.x, PS_Z.y)) < 0.8;
  vec3 c = azulejoPs(uv, blanco, borde ? vec3(0.2, 0.36, 0.73) : junta);
  // la luz que el agua refleja en las paredes y columnas, cerca del suelo
  if (que == 2 || que == 4) c += vec3(0.4, 0.7, 0.75) * causticaPs(vec2(p.z + p.x, p.y * 1.4), t) * exp(-max(p.y, 0.0) * 0.9) * 0.25;
  return c * luzPs(p, n);
}

vec3 nieblaPs(vec3 c, float d) {
  return mix(c, vec3(0.78, 0.89, 0.91), 1.0 - exp(-d / 24.0));
}

vec3 piscinas(vec3 ro, vec3 rd, float t) {
  vec3 n;
  int que;
  float th = trazaPs(ro, rd, true, n, que);
  vec3 p = ro + rd * th;
  if (que != 5) return nieblaPs(colorPs(p, n, que, t), th);
  // el agua: un poco de oleaje, lo que refleja y lo que deja ver
  vec2 g = vec2(cos(p.x * 1.7 + p.z * 2.9 - t * 1.1), cos(-p.x * 3.1 + p.z * 1.3 - t * 1.4)) * 0.03
    + vec2(cos(p.x * 4.3 - p.z * 3.7 - t * 1.9), cos(p.x * 6.1 + p.z * 5.3 - t * 2.6)) * 0.012;
  vec3 nw = normalize(vec3(-g.x, 1.0, -g.y));
  vec3 rr = reflect(rd, nw);
  vec3 n2;
  int q2;
  float t2 = trazaPs(p + rr * 0.01, rr, false, n2, q2);
  vec3 refl = nieblaPs(colorPs(p + rr * t2, n2, q2, t), th + t2);
  vec3 rf = refract(rd, nw, 0.75);
  float t3 = trazaPs(p + rf * 0.01, rf, false, n2, q2);
  vec3 bajo = colorPs(p + rf * t3, n2, q2, t) * mix(vec3(1.0), vec3(0.32, 0.80, 0.80), clamp(t3 * 0.5, 0.0, 1.0));
  float fres = 0.04 + 0.96 * pow(1.0 - max(dot(-rd, nw), 0.0), 5.0);
  return nieblaPs(mix(bajo, refl, clamp(fres * 1.3 + 0.1, 0.0, 1.0)), th);
}

// lo que hay al otro lado de las puertas del ascensor de salida (guion 2): al salir,
// el Nivel 0; al llegar abajo, las Piscinas
vec3 exterior(vec3 ro, vec3 rd, float t) {
  return t < T_CIERRA1 + 0.2 ? pasillo(ro, rd, t) : piscinas(ro, rd, t);
}

/* --------------------------------------------------------- cabina */

vec3 fuera(vec3 ro, vec3 rd, float te); // lo de fuera de la cabina (el prologo, abajo)

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
  float g = puertaAscensor(t); // hueco abierto por cada lado
  if (abs(x) < g) return fuera(p + rd * 1e-3, rd, t);
  // las hojas de hierro deslizan hacia el centro
  float u = x < 0.0 ? x + 1.0 + g : 1.0 - (x - g);
  float temblor = t > T_ROTURA ? (h1(floor(t * 30.0)) - 0.5) * 0.03 : 0.0;
  vec4 d = y < 1.0 ? cara(PUERTA_ABAJO, vec2(u + temblor, y)) : cara(PUERTA_ARRIBA, vec2(u + temblor, y - 1.0));
  if (d.a < 0.5) {
    return t < T_CIERRA1 ? fuera(p + rd * 1e-3, rd, t) : vistaHueco(ro, rd, t);
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

/* ------------------------------------- guion 2: el ascensor de salida */

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
  // por las puertas abiertas entra la luz de fuera: la amarilla del Nivel 0 al salir y,
  // abajo, la blanca de las Piscinas, que el agua hace temblar
  if (t < T_CIERRA1) {
    c += fuente(q, vec3(0.0, 2.0, -3.2), 12.0 * (1.0 - smoothstep(T_CIERRA0, T_CIERRA1, t)), vec3(1.0, 0.92, 0.6));
  } else if (G == 2) {
    float ola = 0.9 + 0.1 * sin(t * 2.3 + q.x * 3.0 + q.z * 2.0) * sin(t * 1.7 - q.x * 2.0);
    c += fuente(q, vec3(0.0, 1.6, -3.4), 13.0 * abiertas(t) * 1.3 * ola, vec3(0.78, 0.95, 1.0));
  }
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
  // polvo del techo en cada frenazo
  for (int i = 0; i < 24; i++) {
    float fi = float(i);
    float t0 = i < 12 ? A_FRENO : A_FRENA;
    float u = t - t0 - h1(fi * 3.3) * 0.5;
    if (u < 0.0 || u > 2.4) continue;
    vec3 P = vec3((h1(fi + floor(t0)) - 0.5) * 3.6, 3.95 - u * 1.1, (h1(fi * 1.9 + floor(t0)) - 0.5) * 3.6);
    vec3 tinte = vec3(0.75, 0.72, 0.65) * (0.25 + 0.7 * lampara(t) + emergencia(t) * 0.3);
    particula(col, c, uv, zEscena, P, 0.07, P_POLVO, tinte, 1.0);
  }
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

/* ============================================ el prologo (guion 0) */

// Antes del ascensor y en el mismo plano, sin cortes: la sala de reuniones 3
// del Proyecto Umbral (te acercan el consentimiento, lo lees y lo firmas), el
// pasillo y la cabina. Todo va en las coordenadas de la cabina: sus puertas
// dan a z = -1.5, el pasillo sigue hacia -z y la sala esta al fondo. Los
// bloques estan centrados como los de la cabina (las juntas caen en x + 0.5 y
// z + 0.5). El reloj t de aqui empieza en la sala; el del ascensor es
// te = t - PRO (ver main).

const vec3 SALA0 = vec3(-4.5, 0.0, -21.5), SALA1 = vec3(4.5, 4.0, -13.5);
const vec3 PAS0 = vec3(-2.5, 0.0, -13.5), PAS1 = vec3(2.5, 4.0, -1.5);
const vec3 CAB0 = vec3(-1.5, 0.0, -1.5), CAB1 = vec3(1.5, 3.0, 1.5);
const vec3 SENTADO = vec3(0.0, 1.13, -15.55);
const vec3 DE_PIE = vec3(-0.55, 1.62, -15.0);
const float MESA_Y = 0.75;
const float PX_INV = 0.05625; // un pixel del investigador (mide 32: 1.8 bloques)

// el investigador rodea la mesa y sale por el centro de la puerta (sin rozar el marco ni la hoja)
const int NI = 8;
const vec2 RUTA_I[8] = vec2[](vec2(0.0, -18.05), vec2(1.75, -17.85), vec2(1.75, -15.2), vec2(0.6, -14.6),
  vec2(0.0, -14.1), vec2(0.0, -12.6), vec2(0.5, -4.0), vec2(1.25, -2.35));
const vec2 PUESTO_I = vec2(0.35, -3.8); // donde se queda, delante de las puertas
const int NJ = 6;
const vec2 RUTA_J[8] = vec2[](vec2(-0.55, -15.0), vec2(-0.35, -14.3), vec2(0.0, -13.7), vec2(0.0, -13.0),
  vec2(0.0, -2.5), vec2(0.0, 0.9), vec2(0.0, 0.9), vec2(0.0, 0.9));

/* ------------------------------------------------------ utiles */

float tramo(float a, float b, float c, float d, float t) { return smoothstep(a, b, t) * (1.0 - smoothstep(c, d, t)); }

// de 0 a 1 con arranque y frenada suaves (a: la parte que acelera y la que frena)
float avance(float u, float a) {
  u = clamp(u, 0.0, 1.0);
  float v = 1.0 / (1.0 - a);
  if (u < a) return 0.5 * v * u * u / a;
  if (u > 1.0 - a) return 1.0 - 0.5 * v * (1.0 - u) * (1.0 - u) / a;
  return v * (u - 0.5 * a);
}

// lo rapido que va avance() (0 parado, 1 a toda marcha)
float marcha(float u, float a) {
  if (u <= 0.0 || u >= 1.0) return 0.0;
  return min(1.0, min(u, 1.0 - u) / a);
}

// el rumbo (yaw de la camara) de una direccion en el plano xz, entre 0 y 2pi
float rumbo(vec2 d) {
  if (dot(d, d) < 1e-8) return 0.0;
  float y = atan(d.x, -d.y);
  return y < 0.0 ? y + 6.2831853 : y;
}

float largoRuta(vec2 r[8], int n) {
  float l = 0.0;
  for (int i = 1; i < 8; i++) if (i < n) l += length(r[i] - r[i - 1]);
  return l;
}

vec2 enRuta(vec2 r[8], int n, float s) {
  s = max(s, 0.0);
  for (int i = 1; i < 8; i++) {
    if (i >= n) break;
    float l = length(r[i] - r[i - 1]);
    if (s <= l || i == n - 1) return mix(r[i - 1], r[i], clamp(s / max(l, 1e-4), 0.0, 1.0));
    s -= l;
  }
  return r[n - 1];
}

vec3 rotF(vec3 v, float a) { float c = cos(a), s = sin(a); return vec3(v.x, c * v.y + s * v.z, -s * v.y + c * v.z); } // -Y hacia +Z
vec3 rotL(vec3 v, float b) { float c = cos(b), s = sin(b); return vec3(c * v.x - s * v.y, s * v.x + c * v.y, v.z); } // -Y hacia +X
vec3 rotY(vec3 v, float a) { float c = cos(a), s = sin(a); return vec3(c * v.x + s * v.z, v.y, -s * v.x + c * v.z); } // +Z hacia +X

/* ------------------------------------------------------ el lienzo */

// (ver LienzoCinematica.java: las mismas posiciones)
vec4 lienzo(ivec2 p) { return texelFetch(Sampler1, p, 0); }

float muestrasFirma() {
  vec4 m = lienzo(ivec2(0, 511)) * 255.0 + 0.5;
  return floor(m.r) * 256.0 + floor(m.g);
}

// la muestra i del recorrido de la punta: (x, y) en pixeles de la hoja y si llega levantada
vec3 muestraFirma(int i) {
  ivec4 v = ivec4(lienzo(ivec2(i % 512, 504 + i / 512)) * 255.0 + 0.5);
  float x = float(((v.r & 127) << 8) | v.g) / 8.0;
  float y = float((v.b << 8) | v.a) / 8.0;
  return vec3(x, y, float(v.r >> 7));
}

// cuantas muestras de la firma van ya escritas
float progresoFirma(float tg) {
  return clamp((tg - P_FIRMA0) / P_FIRMA_DUR, 0.0, 1.0) * muestrasFirma();
}

// el color de la hoja en (x, y) pixeles, con la firma hasta donde vaya
vec3 colorHoja(vec2 px, float tg) {
  ivec2 ip = ivec2(clamp(floor(px), vec2(0.0), vec2(319.0, 455.0)));
  vec3 c = lienzo(ip).rgb;
  ivec2 m = ip - ivec2(22, 348);
  if (m.x >= 0 && m.y >= 0 && m.x < 184 && m.y < 60) {
    vec4 k = lienzo(ivec2(324, 0) + m);
    if (k.a > 0.5) {
      float idx = floor(k.r * 255.0 + 0.5) * 256.0 + floor(k.g * 255.0 + 0.5);
      float f = progresoFirma(tg);
      if (idx < f) {
        // la tinta recien puesta brilla un poco mas
        vec3 tinta = mix(vec3(0.10, 0.17, 0.56), vec3(0.06, 0.10, 0.42), smoothstep(f - 40.0, f - 140.0, idx));
        c = mix(c, tinta, clamp(k.b * 1.15, 0.0, 1.0));
      }
    }
  }
  return c;
}

// un cartel del lienzo (x, y, ancho, alto) en las coordenadas uv (0..1, v hacia abajo)
vec3 cartel(ivec4 r, vec2 uv) {
  ivec2 p = r.xy + ivec2(clamp(floor(uv * vec2(r.zw)), vec2(0.0), vec2(r.zw) - 1.0));
  return lienzo(p).rgb;
}

const ivec4 L_PLACA = ivec4(324, 64, 128, 32);
const ivec4 L_PIZARRA = ivec4(324, 100, 176, 88);
const ivec4 L_ARCHIVO = ivec4(324, 192, 88, 14);
const ivec4 L_LAB = ivec4(416, 192, 88, 14);
const ivec4 L_SALA = ivec4(324, 210, 88, 14);
const ivec4 L_ASCENSOR = ivec4(416, 210, 88, 14);
const ivec4 L_BOLI = ivec4(324, 228, 40, 8);

/* ---------------------------------------------------- la luz */

// la luz de bloque en q (0..15): el maximo de los paneles del techo de su zona,
// 15 menos la distancia en bloques, como en Minecraft
float nivelProlo(vec3 q) {
  float L = 0.0;
  if (q.z < -13.5) {
    for (int i = 0; i < 6; i++) {
      vec2 c = i == 0 ? vec2(0.0, -17.0) : i == 1 ? vec2(0.0, -16.0) : i == 2 ? vec2(-3.0, -20.0)
        : i == 3 ? vec2(3.0, -20.0) : i == 4 ? vec2(-3.0, -15.0) : vec2(3.0, -15.0);
      vec3 d = abs(q - vec3(c.x, 4.5, c.y));
      L = max(L, 15.0 - (d.x + d.y + d.z));
    }
  } else {
    for (int k = 1; k <= 4; k++) {
      vec3 d = abs(q - vec3(0.0, 4.5, -3.0 * float(k)));
      L = max(L, 15.0 - (d.x + d.y + d.z));
    }
  }
  return L;
}

// como brilloNivel pero con el brillo del juego subido: una oficina bien iluminada
float brilloOficina(float L) {
  float f = clamp(L, 0.0, 15.0) / 15.0;
  return mix(f / (4.0 - 3.0 * f), 1.0 - pow(1.0 - f, 4.0), 0.55);
}

// sombreado por cara de Minecraft, para normales giradas
float sombreadoSuave(vec3 n) {
  return n.y * n.y * (n.y > 0.0 ? 1.0 : 0.5) + n.x * n.x * 0.6 + n.z * n.z * 0.8;
}

vec3 luzProlo(vec3 alb, vec3 p, vec3 n) {
  return alb * brilloOficina(nivelProlo(p + n * 0.5)) * sombreadoSuave(n) * vec3(1.0, 0.99, 0.965);
}

// la oclusion de las esquinas (la luz suave de Minecraft): mas oscuro junto a otra pared
float esquinas(vec3 p, vec3 n, vec3 b0, vec3 b1) {
  vec3 d = min(p - b0, b1 - p);
  float a = 1.0;
  if (abs(n.x) < 0.5) a *= 0.74 + 0.26 * clamp(d.x, 0.0, 1.0);
  if (abs(n.y) < 0.5) a *= 0.74 + 0.26 * clamp(d.y, 0.0, 1.0);
  if (abs(n.z) < 0.5) a *= 0.74 + 0.26 * clamp(d.z, 0.0, 1.0);
  return a;
}

/* --------------------------------------------- lo que se mueve */

// la hoja: su centro (sobre la mesa) en cada momento. Te la acerca de un empujon y
// al final se la lleva.
vec3 papelCentro(float tg) {
  float k = smoothstep(P_DESLIZA0 + 0.2, P_DESLIZA1, tg);
  k = 1.0 - pow(1.0 - k, 3.0);
  vec3 p = mix(vec3(0.0, MESA_Y, -17.1), vec3(0.0, MESA_Y, -16.265), k);
  return mix(p, vec3(0.0, MESA_Y, -17.25), smoothstep(P_RECOGE0 + 0.25, P_RECOGE1 - 0.15, tg));
}

// de pixeles de la hoja a un punto del mundo (sobre el papel)
vec3 enHoja(vec2 px, float tg) {
  vec3 c = papelCentro(tg);
  return vec3(c.x - 0.25 + px.x / 640.0, MESA_Y + 0.0025, c.z - 0.35625 + px.y / 640.0);
}

// el boligrafo: la punta y hacia donde va (de la punta al capuchon)
void boligrafo(float tg, out vec3 punta, out vec3 eje, out float mano) {
  // tumbado en la mesa, a la derecha de la hoja; se lo acerca rodando por la mesa
  vec3 ejeMesa = normalize(vec3(0.24, 0.0, 0.97));
  float r = smoothstep(P_BOLI0, P_BOLI1, tg);
  r = 1.0 - pow(1.0 - r, 2.5);
  vec3 puntaMesa = mix(vec3(0.36, MESA_Y + 0.0115, -17.3), vec3(0.33, MESA_Y + 0.0115, -16.5), r);
  punta = puntaMesa;
  eje = ejeMesa;
  mano = 0.0;
  if (tg < P_COGE0 || tg > P_SUELTA1) return;
  // la mano: llega (0.5 s), lo levanta hasta el principio de la firma, firma y lo devuelve
  mano = smoothstep(P_COGE0, P_COGE0 + 0.6, tg) * (1.0 - smoothstep(P_SUELTA0 + 0.45, P_SUELTA1, tg));
  float f = progresoFirma(tg);
  float n = max(muestrasFirma(), 2.0);
  int i0 = int(min(floor(f), n - 2.0));
  float fr = clamp(f - float(i0), 0.0, 1.0);
  vec3 a = muestraFirma(i0);
  vec3 b = muestraFirma(i0 + 1);
  vec2 px = mix(a.xy, b.xy, fr);
  float alza = b.z > 0.5 ? sin(fr * 3.14159) * 0.009 : 0.0;
  // antes de empezar y al acabar, la punta queda un poco en el aire
  alza += 0.012 * (1.0 - smoothstep(P_FIRMA0 - 0.25, P_FIRMA0, tg)) + 0.02 * smoothstep(P_FIRMA0 + P_FIRMA_DUR, P_SUELTA0, tg);
  vec3 puntaFirma = enHoja(px, tg) + vec3(0.0, alza, 0.0);
  // al escribir se mueve un poco con la mano
  vec3 ejeFirma = normalize(vec3(0.7 + 0.04 * sin(f * 0.21), 0.64, 0.14 + 0.03 * cos(f * 0.17)));
  float k = smoothstep(P_COGE0 + 0.55, P_COGE1, tg) * (1.0 - smoothstep(P_SUELTA0, P_SUELTA0 + 0.5, tg));
  // de la mesa a la hoja, por el aire
  punta = mix(puntaMesa, puntaFirma, k) + vec3(0.0, 0.05 * sin(k * 3.14159), 0.0);
  eje = normalize(mix(ejeMesa, ejeFirma, k));
}

struct Pose { vec3 pie; float yaw; float inclina; float paso; float anda; float brazoD; float ladoD; float brazoI; float cabeza; float giro; };

Pose investigador(float tg, vec3 cam) {
  Pose q;
  float L = largoRuta(RUTA_I, NI);
  float u = (tg - I_ANDA0) / (I_ANDA1 - I_ANDA0);
  float s = L * avance(u, 0.15);
  vec2 xz = enRuta(RUTA_I, NI, s);
  vec2 dir = enRuta(RUTA_I, NI, min(s + 0.45, L)) - enRuta(RUTA_I, NI, max(s - 0.45, 0.0));
  float anda = marcha(u, 0.15);
  float paso = s;
  if (tg > I_PUERTA0) {
    // cuando ya has pasado, se pone delante de las puertas
    float v = (tg - I_PUERTA0) / (I_PUERTA1 - I_PUERTA0);
    vec2 a = RUTA_I[NI - 1];
    xz = mix(a, PUESTO_I, avance(v, 0.3));
    dir = PUESTO_I - a;
    anda = marcha(v, 0.3);
    paso = L + length(PUESTO_I - a) * avance(v, 0.3);
  }
  q.pie = vec3(xz.x, 0.0, xz.y);
  float yMira = rumbo(cam.xz - xz);
  float yAnda = rumbo(dir);
  yAnda += 6.2831853 * floor((yMira - yAnda) / 6.2831853 + 0.5);
  q.yaw = mix(yMira, yAnda, smoothstep(0.0, 0.4, anda));
  q.anda = anda;
  q.paso = paso;
  q.inclina = 0.7 * tramo(2.1, 2.6, 3.4, 4.1, tg) + 0.45 * tramo(24.4, 24.8, 25.3, 25.8, tg) + 0.12 * tramo(3.9, 4.2, 4.6, 5.0, tg);
  q.brazoD = 1.5 * tramo(2.1, 2.6, 3.5, 4.2, tg) + 1.1 * tramo(3.9, 4.2, 4.6, 5.0, tg) + 1.45 * tramo(24.4, 24.8, 25.4, 25.9, tg)
    + 1.2 * tramo(I_LLAMA - 0.45, I_LLAMA - 0.15, I_LLAMA + 0.15, I_LLAMA + 0.5, tg)
    + 0.3 * tramo(I_ADIOS, I_ADIOS + 0.4, I_ADIOS + 1.3, I_ADIOS + 1.9, tg);
  q.ladoD = 0.25 * tramo(3.9, 4.2, 4.6, 5.0, tg) + 0.15 * tramo(I_LLAMA - 0.45, I_LLAMA - 0.15, I_LLAMA + 0.15, I_LLAMA + 0.5, tg);
  q.brazoI = 0.35;
  // la cabeza te sigue (y asiente al despedirse)
  vec3 cl = cam - (q.pie + vec3(0.0, 1.55, 0.0));
  float rel = yMira - q.yaw;
  rel -= 6.2831853 * floor(rel / 6.2831853 + 0.5);
  q.giro = clamp(rel, -0.9, 0.9);
  q.cabeza = clamp(atan(cl.y, length(cl.xz)), -0.6, 0.45) + q.inclina * 0.8 - 0.3 * tramo(I_ADIOS + 0.2, I_ADIOS + 0.5, I_ADIOS + 0.8, I_ADIOS + 1.2, tg);
  return q;
}

/* --------------------------------------------------- objetos */

struct Obj { float t; int id; vec3 n; vec3 q; vec3 h; };

const int O_MESA = 1, O_PATA = 2, O_ASIENTO = 3, O_RESPALDO = 4, O_PIE_SILLA = 5, O_HOJA_PUERTA = 6, O_LIBRERIA = 7;
const int O_MACETA = 8, O_HOJAS = 9, O_PAPEL = 10, O_BANCO = 11, O_EXTINTOR = 12, O_MANO = 13, O_DEDO = 14, O_BRAZO = 15;
const int O_BOLI = 16, O_INV = 20; // 20..27: las partes del investigador

void caja(inout Obj o, vec3 ro, vec3 rd, vec3 b0, vec3 b1, int id) {
  vec3 m = 1.0 / rd;
  vec3 a = (b0 - ro) * m, b = (b1 - ro) * m;
  vec3 t1 = min(a, b), t2 = max(a, b);
  float tn = max(max(t1.x, t1.y), t1.z), tf = min(min(t2.x, t2.y), t2.z);
  if (tn < tf && tn > 0.0 && tn < o.t) {
    o.t = tn;
    o.id = id;
    o.n = tn == t1.x ? vec3(-sign(rd.x), 0.0, 0.0) : tn == t1.y ? vec3(0.0, -sign(rd.y), 0.0) : vec3(0.0, 0.0, -sign(rd.z));
    o.q = ro + rd * tn - b0;
    o.h = b1 - b0;
  }
}

// una caja girada: M (filas = ejes de la caja) y medio tamano h alrededor de c
void cajaGirada(inout Obj o, vec3 ro, vec3 rd, vec3 c, mat3 M, vec3 h, int id) {
  vec3 lo = M * (ro - c), ld = M * rd;
  Obj l;
  l.t = o.t;
  l.id = -1;
  caja(l, lo, ld, -h, h, id);
  if (l.id >= 0) {
    o.t = l.t;
    o.id = id;
    o.n = transpose(M) * l.n;
    o.q = l.q;
    o.h = l.h;
  }
}

// la silla de oficina: mira hacia d (en el plano)
void silla(inout Obj o, vec3 ro, vec3 rd, vec2 c, vec2 d) {
  vec3 p = vec3(c.x, 0.0, c.y);
  caja(o, ro, rd, p + vec3(-0.28, 0.42, -0.28), p + vec3(0.28, 0.5, 0.28), O_ASIENTO);
  vec2 b = c - d * 0.27;
  vec2 w = abs(vec2(d.y, d.x)) * 0.27 + abs(d) * 0.035;
  caja(o, ro, rd, vec3(b.x - w.x, 0.5, b.y - w.y), vec3(b.x + w.x, 1.06, b.y + w.y), O_RESPALDO);
  caja(o, ro, rd, p + vec3(-0.04, 0.05, -0.04), p + vec3(0.04, 0.42, 0.04), O_PIE_SILLA);
  caja(o, ro, rd, p + vec3(-0.26, 0.0, -0.03), p + vec3(0.26, 0.05, 0.03), O_PIE_SILLA);
  caja(o, ro, rd, p + vec3(-0.03, 0.0, -0.26), p + vec3(0.03, 0.05, 0.26), O_PIE_SILLA);
}

// el investigador: cada parte es una caja en pixeles del modelo de Minecraft
void partesInvestigador(inout Obj o, vec3 ro, vec3 rd, Pose q) {
  vec3 Z = vec3(sin(q.yaw), 0.0, -cos(q.yaw));
  vec3 Y = vec3(0.0, 1.0, 0.0);
  vec3 X = cross(Z, Y);
  mat3 Rb = transpose(mat3(X, Y, Z));
  vec3 bo = Rb * (ro - q.pie) / PX_INV, bd = Rb * rd;
  // primero la caja que lo envuelve
  {
    Obj e;
    e.t = 1e9;
    e.id = -1;
    caja(e, bo, bd, vec3(-10.0, -0.5, -8.0), vec3(10.0, 34.0, 16.0), 0);
    bool dentro = all(greaterThan(bo, vec3(-10.0, -0.5, -8.0))) && all(lessThan(bo, vec3(10.0, 34.0, 16.0)));
    if (!dentro && (e.id < 0 || e.t * PX_INV > o.t)) return;
  }
  float sw = sin(q.paso * 6.2831853 / 2.4) * 0.6 * q.anda;
  Obj l;
  l.t = o.t / PX_INV;
  l.id = -1;
  vec3 nl;
  int parte = -1;
  // piernas (la derecha del modelo es +X)
  for (int k = 0; k < 2; k++) {
    float sx = k == 0 ? 2.0 : -2.0;
    float a = k == 0 ? sw : -sw;
    vec3 po = rotF(bo - vec3(sx, 12.0, 0.0), -a), pd = rotF(bd, -a);
    Obj m = l;
    caja(m, po, pd, vec3(-2.0, -12.0, -2.0), vec3(2.0, 0.0, 2.0), 20 + k);
    if (m.t < l.t) { l = m; nl = rotF(m.n, a); parte = 20 + k; }
  }
  // los faldones de la bata
  {
    Obj m = l;
    caja(m, bo, bd, vec3(-4.3, 6.0, -2.3), vec3(4.3, 12.0, 2.3), 22);
    if (m.t < l.t) { l = m; nl = m.n; parte = 22; }
  }
  // de cintura para arriba, inclinado hacia delante
  vec3 H = vec3(0.0, 12.0, 0.0);
  vec3 uo = H + rotF(bo - H, q.inclina), ud = rotF(bd, q.inclina);
  {
    Obj m = l;
    caja(m, uo, ud, vec3(-4.0, 12.0, -2.0), vec3(4.0, 24.0, 2.0), 23);
    if (m.t < l.t) { l = m; nl = rotF(m.n, -q.inclina); parte = 23; }
  }
  {
    vec3 N = vec3(0.0, 24.0, 0.0);
    vec3 po = rotF(rotY(uo - N, -q.giro), -q.cabeza), pd = rotF(rotY(ud, -q.giro), -q.cabeza);
    Obj m = l;
    caja(m, po, pd, vec3(-4.0, 0.0, -4.0), vec3(4.0, 8.0, 4.0), 24);
    if (m.t < l.t) { l = m; nl = rotF(rotY(rotF(m.n, q.cabeza), q.giro), -q.inclina); parte = 24; }
  }
  for (int k = 0; k < 2; k++) {
    float sx = k == 0 ? 6.0 : -6.0;
    float a = k == 0 ? q.brazoD - sw * 0.8 : q.brazoI + sw * 0.5;
    float b = k == 0 ? q.ladoD : -0.05;
    vec3 P = vec3(sx, 22.0, 0.0);
    vec3 po = rotF(rotL(uo - P, -b), -a), pd = rotF(rotL(ud, -b), -a);
    Obj m = l;
    caja(m, po, pd, vec3(-2.0, -10.0, -2.0), vec3(2.0, 2.0, 2.0), 25 + k);
    if (m.t < l.t) { l = m; nl = rotF(rotL(rotF(m.n, a), b), -q.inclina); parte = 25 + k; }
    if (k == 1) {
      // la tablilla con los papeles, en la mano izquierda
      Obj c = l;
      caja(c, po, pd, vec3(-2.6, -12.5, -1.0), vec3(-2.1, -5.5, 4.2), 27);
      if (c.t < l.t) { l = c; nl = rotF(rotL(rotF(c.n, a), b), -q.inclina); parte = 27; }
    }
  }
  if (parte >= 0) {
    o.t = l.t * PX_INV;
    o.id = parte;
    o.n = transpose(Rb) * nl;
    o.q = l.q;
    o.h = l.h;
  }
}

// marco de la mano (alrededor del boligrafo): siempre hacia arriba y hacia ti, para
// que la mano no cambie de lado al moverse por la hoja
void marcoBoli(vec3 punta, vec3 eje, vec3 cam, out vec3 bx, out vec3 by) {
  vec3 ac = normalize(vec3(-0.25, 1.0, 0.45));
  by = normalize(ac - dot(ac, eje) * eje);
  bx = cross(by, eje);
}

void manoYBrazo(inout Obj o, vec3 ro, vec3 rd, vec3 punta, vec3 eje, float mano, vec3 cam) {
  if (mano <= 0.0) return;
  vec3 bx, by;
  marcoBoli(punta, eje, cam, bx, by);
  vec3 lejos = vec3(0.45, 0.3, 0.5) * (1.0 - mano);
  mat3 M = transpose(mat3(bx, by, eje));
  vec3 c = punta + eje * 0.108 - bx * 0.03 - by * 0.004 + lejos;
  cajaGirada(o, ro, rd, c, M, vec3(0.03, 0.025, 0.045), O_MANO);
  // el indice, por encima del boligrafo, y el pulgar por el otro lado
  cajaGirada(o, ro, rd, punta + eje * 0.082 - bx * 0.004 + by * 0.0125 + lejos, M, vec3(0.0105, 0.0065, 0.024), O_DEDO);
  cajaGirada(o, ro, rd, punta + eje * 0.088 + bx * 0.013 - by * 0.004 + lejos, M, vec3(0.006, 0.008, 0.02), O_DEDO);
  // el antebrazo, hacia ti y a la derecha
  vec3 a0 = c + eje * 0.04 - bx * 0.005;
  vec3 dir = normalize(vec3(0.86, -0.02, 0.5));
  vec3 ay = normalize(vec3(0.0, 1.0, 0.0) - dot(vec3(0.0, 1.0, 0.0), dir) * dir);
  vec3 ax = cross(ay, dir);
  cajaGirada(o, ro, rd, a0 + dir * 0.3, transpose(mat3(ax, ay, dir)), vec3(0.029, 0.026, 0.3), O_BRAZO);
}

void objetos(inout Obj o, vec3 ro, vec3 rd, float tg, float te, vec3 cam) {
  // la sala
  if (ro.z < -13.0 || rd.z < 0.0) {
    caja(o, ro, rd, vec3(-1.25, 0.69, -17.6), vec3(1.25, MESA_Y, -15.85), O_MESA);
    for (int i = 0; i < 4; i++) {
      vec2 c = vec2(i % 2 == 0 ? -1.12 : 1.12, i < 2 ? -17.47 : -15.98);
      caja(o, ro, rd, vec3(c.x - 0.06, 0.0, c.y - 0.06), vec3(c.x + 0.06, 0.69, c.y + 0.06), O_PATA);
    }
    silla(o, ro, rd, vec2(0.0, -18.75), vec2(0.0, 1.0));
    silla(o, ro, rd, vec2(-1.75, -17.1), vec2(1.0, 0.0));
    silla(o, ro, rd, vec2(-1.75, -16.2), vec2(1.0, 0.0));
    caja(o, ro, rd, vec3(3.5, 0.0, -21.5), vec3(4.5, 2.0, -19.5), O_LIBRERIA);
    caja(o, ro, rd, vec3(-4.2, 0.0, -21.2), vec3(-3.8, 0.375, -20.8), O_MACETA);
    caja(o, ro, rd, vec3(-4.4, 0.375, -21.4), vec3(-3.6, 1.15, -20.6), O_HOJAS);
    if (tg < P_RECOGE1 - 0.05) {
      vec3 c = papelCentro(tg);
      caja(o, ro, rd, c - vec3(0.25, 0.0, 0.35625), c + vec3(0.25, 0.0025, 0.35625), O_PAPEL);
    }
  }
  // el pasillo: la puerta de la sala, abierta hacia fuera contra la pared
  caja(o, ro, rd, vec3(0.5, 0.0, -13.5), vec3(0.6875, 2.0, -12.5), O_HOJA_PUERTA);
  caja(o, ro, rd, vec3(-2.1, 0.0, -2.6), vec3(-1.7, 0.375, -2.2), O_MACETA);
  caja(o, ro, rd, vec3(-2.2, 0.375, -2.7), vec3(-1.6, 0.95, -2.1), O_HOJAS);
  caja(o, ro, rd, vec3(1.95, 0.42, -11.4), vec3(2.5, 0.5, -9.4), O_BANCO);
  caja(o, ro, rd, vec3(2.0, 0.0, -11.3), vec3(2.45, 0.42, -11.2), O_BANCO);
  caja(o, ro, rd, vec3(2.0, 0.0, -9.6), vec3(2.45, 0.42, -9.5), O_BANCO);
  caja(o, ro, rd, vec3(-2.5, 0.85, -5.25), vec3(-2.32, 1.42, -5.0), O_EXTINTOR);
  partesInvestigador(o, ro, rd, investigador(tg, cam));
  if (tg > P_BOLI0 && tg < 30.0) {
    vec3 punta, eje;
    float mano;
    boligrafo(tg, punta, eje, mano);
    manoYBrazo(o, ro, rd, punta, eje, mano, cam);
  }
}

/* ------------------------------------------------- el boligrafo */

// Un boligrafo de verdad (de 0.2 bloques, grande): bola y punta de metal, cono de
// plastico, agarre de goma, cuerpo hexagonal blanco con "UMBRAL" impreso, el
// capuchon azul puesto atras con su clip. Va por distancias, en su marco: z a lo
// largo, de la punta (0) al capuchon.

float sdConoZ(vec3 p, float z0, float z1, float r0, float r1) {
  float h = 0.5 * (z1 - z0);
  vec2 q = vec2(length(p.xy), p.z - (z0 + h));
  vec2 k1 = vec2(r1, h);
  vec2 k2 = vec2(r1 - r0, 2.0 * h);
  vec2 ca = vec2(q.x - min(q.x, q.y < 0.0 ? r0 : r1), abs(q.y) - h);
  vec2 cb = q - k1 + k2 * clamp(dot(k1 - q, k2) / dot(k2, k2), 0.0, 1.0);
  float s = (cb.x < 0.0 && ca.y < 0.0) ? -1.0 : 1.0;
  return s * sqrt(min(dot(ca, ca), dot(cb, cb)));
}

float sdCilZ(vec3 p, float z0, float z1, float r) {
  vec2 d = abs(vec2(length(p.xy), p.z - 0.5 * (z0 + z1))) - vec2(r, 0.5 * (z1 - z0));
  return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdHexZ(vec3 p, float z0, float z1, float r) {
  const vec3 k = vec3(-0.8660254, 0.5, 0.57735);
  p.z -= 0.5 * (z0 + z1);
  p = abs(p);
  p.xy -= 2.0 * min(dot(k.xy, p.xy), 0.0) * k.xy;
  vec2 d = vec2(length(p.xy - vec2(clamp(p.x, -k.z * r, k.z * r), r)) * sign(p.y - r), p.z - 0.5 * (z1 - z0));
  return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdCaja(vec3 p, vec3 h) {
  vec3 q = abs(p) - h;
  return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

float sdBoli(vec3 p, out int mat) {
  float d = length(p - vec3(0.0, 0.0, 0.0013)) - 0.0013;
  mat = 0;
  float c = sdConoZ(p, 0.001, 0.013, 0.0011, 0.0034);
  if (c < d) { d = c; mat = 1; }
  c = sdConoZ(p, 0.013, 0.042, 0.0034, 0.0106);
  if (c < d) { d = c; mat = 2; }
  c = sdCilZ(p, 0.042, 0.078, 0.0116);
  if (c < d) { d = c; mat = 3; }
  c = sdHexZ(p, 0.078, 0.17, 0.0113);
  if (c < d) { d = c; mat = 4; }
  c = sdCilZ(p, 0.15, 0.203, 0.0118) - 0.0007;
  if (c < d) { d = c; mat = 5; }
  c = sdCaja(p - vec3(0.0, 0.0138, 0.181), vec3(0.0021, 0.0011, 0.021)) - 0.0003;
  if (c < d) { d = c; mat = 6; }
  return d;
}

// traza el boligrafo; devuelve t (o 1e9), la normal y el punto en su marco
float trazaBoli(vec3 ro, vec3 rd, float tMax, vec3 punta, vec3 eje, vec3 bx, vec3 by, out vec3 n, out vec3 lp, out int mat) {
  mat3 M = transpose(mat3(bx, by, eje));
  vec3 lo = M * (ro - punta), ld = M * rd;
  // entra en el cilindro que lo envuelve
  float a = dot(ld.xy, ld.xy), b = dot(lo.xy, ld.xy), c = dot(lo.xy, lo.xy) - 0.017 * 0.017;
  float disc = b * b - a * c;
  if (disc < 0.0) return 1e9;
  float t0 = max((-b - sqrt(disc)) / a, 0.0);
  float t1 = (-b + sqrt(disc)) / a;
  if (t1 < 0.0 || t0 > tMax) return 1e9;
  float t = t0;
  for (int i = 0; i < 64; i++) {
    vec3 p = lo + ld * t;
    float d = sdBoli(p, mat);
    if (d < 0.00004) {
      if (p.z < -0.002 || p.z > 0.21) return 1e9;
      const vec2 e = vec2(0.00015, 0.0);
      int mm;
      vec3 nl = normalize(vec3(sdBoli(p + e.xyy, mm) - sdBoli(p - e.xyy, mm), sdBoli(p + e.yxy, mm) - sdBoli(p - e.yxy, mm),
        sdBoli(p + e.yyx, mm) - sdBoli(p - e.yyx, mm)));
      n = transpose(M) * nl;
      lp = p;
      return t;
    }
    t += d;
    if (t > t1 || t > tMax) break;
  }
  return 1e9;
}

vec3 colorBoli(vec3 lp, vec3 n, vec3 rd, int mat, vec3 pw) {
  const float PXB = 0.0018; // un pixel del boligrafo
  float ang = atan(lp.y, lp.x);
  ivec2 c = ivec2(floor(lp.z / PXB), floor((ang + 3.14159) / (6.2831853 / 40.0)));
  float var = 0.96 + 0.06 * h2(vec2(c) + float(mat) * 7.0);
  vec3 alb;
  float brillo = 0.35;
  float dureza = 26.0;
  if (mat == 0 || mat == 1 || mat == 6) {
    alb = vec3(0.74, 0.75, 0.78) * (c.x % 2 == 0 ? 1.0 : 0.86);
    if (mat == 0) alb = vec3(0.5, 0.5, 0.54);
    brillo = 0.9;
    dureza = 40.0;
  } else if (mat == 2) {
    alb = vec3(0.86, 0.89, 0.91) * (c.x % 4 == 0 ? 0.9 : 1.0);
  } else if (mat == 3) {
    alb = vec3(0.2, 0.21, 0.24) * ((c.x % 2 == 0) ? 1.0 : 1.35);
    if ((c.x + c.y) % 4 == 0) alb *= 0.8;
    brillo = 0.12;
    dureza = 10.0;
  } else if (mat == 4) {
    alb = vec3(0.93, 0.935, 0.94);
    // los anillos: plata al empezar y el cian del proyecto antes del capuchon
    if (lp.z < 0.0815) alb = vec3(0.72, 0.74, 0.77);
    if (lp.z > 0.144 && lp.z < 0.1494) alb = vec3(0.13, 0.55, 0.58);
    // "UMBRAL" en la cara que mira a la camara
    if (lp.y > 0.0095 && lp.z > 0.088 && lp.z < 0.144) {
      vec2 uv = vec2((lp.z - 0.088) / 0.056, 0.5 - lp.x / 0.013);
      if (uv.y > 0.0 && uv.y < 1.0) alb = cartel(L_BOLI, uv);
    }
  } else {
    alb = vec3(0.16, 0.26, 0.7);
    if (lp.z > 0.198) alb = vec3(0.11, 0.18, 0.52);
    if (lp.z < 0.153) alb = vec3(0.24, 0.36, 0.82);
  }
  alb *= var;
  vec3 l = normalize(vec3(0.15, 1.0, 0.3));
  float L = brilloOficina(nivelProlo(pw + n * 0.3));
  float dif = 0.42 + 0.58 * max(dot(n, l), 0.0);
  float esp = pow(max(dot(reflect(-l, n), -rd), 0.0), dureza) * brillo;
  return alb * dif * L + vec3(esp) * L;
}

/* ------------------------------------------------ las texturas */

// la piel del investigador: bata blanca, gafas, pelo castano, pantalon gris
vec3 pielInvestigador(int parte, vec3 n, vec3 q, vec3 h, float tg) {
  int lado; // 0 delante, 1 detras, 2 su derecha, 3 su izquierda, 4 arriba, 5 abajo
  vec2 uv;
  if (n.z > 0.5) { lado = 0; uv = vec2(h.x - q.x, h.y - q.y); }
  else if (n.z < -0.5) { lado = 1; uv = vec2(q.x, h.y - q.y); }
  else if (n.x > 0.5) { lado = 2; uv = vec2(h.z - q.z, h.y - q.y); }
  else if (n.x < -0.5) { lado = 3; uv = vec2(q.z, h.y - q.y); }
  else if (n.y > 0.5) { lado = 4; uv = q.xz; }
  else { lado = 5; uv = q.xz; }
  ivec2 p = ivec2(floor(uv));
  float r = 0.95 + 0.08 * h2(vec2(p) + float(parte * 31 + lado * 7));
  vec3 piel = vec3(0.86, 0.67, 0.55);
  vec3 pelo = vec3(0.27, 0.18, 0.12);
  vec3 bata = vec3(0.93, 0.93, 0.9);
  vec3 pliegue = vec3(0.8, 0.81, 0.79);
  vec3 pantalon = vec3(0.21, 0.22, 0.25);
  vec3 c = bata;
  if (parte == 24) {
    // la cabeza
    c = piel;
    if (lado == 4) c = pelo;
    else if (lado == 5) c = piel * 0.85;
    else if (lado == 1) c = p.y < 6 ? pelo : piel;
    else if (lado == 2 || lado == 3) {
      c = (p.y < 3 || (p.x >= 4 && p.y < 6)) ? pelo : piel;
      if (p.y == 4 && p.x == 3) c = piel * 0.8; // la oreja
      if (p.y == 3 && p.x < 3) c = vec3(0.13); // la patilla de las gafas
    } else {
      c = p.y < 2 ? pelo : piel;
      if (p.y == 2 && (p.x == 0 || p.x == 7)) c = pelo;
      // las gafas: dos cristales que solo reflejan la luz
      if (p.y == 3 && p.x >= 1 && p.x <= 6 && p.x != 7) c = vec3(0.12);
      if (p.y == 4 && (p.x == 1 || p.x == 3 || p.x == 4 || p.x == 6)) c = vec3(0.12);
      if (p.y == 4 && (p.x == 2 || p.x == 5)) c = vec3(0.86, 0.93, 0.96) * (1.1 + 0.25 * sin(tg * 0.7 + float(p.x)));
      if (p.y == 5 && (p.x == 3 || p.x == 4)) c = piel * 0.88;
      if (p.y == 6 && (p.x == 3 || p.x == 4)) c = vec3(0.55, 0.36, 0.33);
    }
  } else if (parte == 23) {
    // el cuerpo: la bata abierta arriba, camisa, corbata y la acreditacion
    if (lado == 0) {
      bool camisa = p.y < 6 && abs(float(p.x) - 3.5) < 1.6 - float(p.y) * 0.18 + 0.6;
      if (camisa) c = vec3(0.64, 0.75, 0.84);
      if (camisa && (p.x == 3 || p.x == 4) && p.y >= 1) c = vec3(0.1, 0.4, 0.43);
      if (p.y == 0 && (p.x == 2 || p.x == 5)) c = vec3(0.95);
      if (!camisa && p.y < 7 && (p.x == 1 || p.x == 6)) c = pliegue;
      if (p.y >= 6 && p.x == 3) c = pliegue;
      if (p.y == 7 || p.y == 9) { if (p.x == 4) c = vec3(0.7); }
      if (p.x >= 5 && p.x <= 6 && p.y >= 3 && p.y <= 4) c = p.y == 3 ? vec3(0.95) : vec3(0.15, 0.6, 0.65);
      if (p.y == 8 && (p.x == 1 || p.x == 2)) c = pliegue;
    } else if (lado == 1) {
      if (p.x == 3 && p.y > 5) c = pliegue;
    } else if (lado == 4) {
      if (abs(float(p.x) - 3.5) < 1.0) c = piel;
    }
  } else if (parte == 25 || parte == 26) {
    // los brazos: manga, puno y la mano
    c = p.y < 9 ? bata : p.y == 9 ? pliegue : piel;
    if (lado == 5) c = piel;
    if (lado == 4) c = bata;
    if (p.y == 5 && lado != 4 && lado != 5) c = mix(bata, pliegue, 0.6);
  } else if (parte == 20 || parte == 21) {
    c = p.y < 10 ? pantalon : vec3(0.09, 0.08, 0.07);
    if (lado == 5) c = vec3(0.07);
    if (p.y < 10 && p.x == 0 && lado == 0) c = pantalon * 0.85;
  } else if (parte == 22) {
    // los faldones: blancos, abiertos por delante
    if (lado == 0 && (p.x == 3 || p.x == 4)) c = pliegue;
    if (p.y == 5) c = pliegue;
  } else {
    // la tablilla: aglomerado, la pinza de metal y, cuando ya la tiene, tu contrato
    c = vec3(0.46, 0.33, 0.2);
    if (lado == 3 || lado == 2) {
      if (uv.y < 1.2) c = vec3(0.6, 0.62, 0.66);
      else if (tg > P_RECOGE1 && uv.y > 1.6 && uv.y < 6.8 && uv.x > 0.5 && uv.x < 4.7) {
        c = colorHoja(vec2((uv.x - 0.5) / 4.2 * 320.0, (uv.y - 1.6) / 5.2 * 456.0), tg);
      }
    }
  }
  return c * r;
}

// la mano y el antebrazo de quien firma (tu)
vec3 colorMano(int id, vec3 q, vec3 n, vec3 h) {
  float px = 0.0055;
  vec3 piel = vec3(0.88, 0.69, 0.56);
  vec2 uv = abs(n.x) > 0.5 ? q.zy : abs(n.y) > 0.5 ? q.xz : q.xy;
  ivec2 p = ivec2(floor(uv / px));
  float r = 0.95 + 0.08 * h2(vec2(p) + float(id));
  if (id == O_BRAZO) {
    // la manga oscura, con el puno de la camisa
    vec3 c = q.z < 0.03 ? vec3(0.82, 0.84, 0.86) : vec3(0.2, 0.23, 0.29);
    if (q.z < 0.012) c = piel * 0.9;
    return c * r;
  }
  vec3 c = piel;
  if (id == O_MANO && abs(n.y) > 0.5 && p.x % 4 == 0) c *= 0.9; // los dedos
  if (id == O_DEDO && q.z > h.z - 0.006) c = vec3(0.92, 0.8, 0.74); // la una
  return c * r;
}

vec3 colorObjeto(Obj o, vec3 ro, vec3 rd, float tg) {
  vec3 p = ro + rd * o.t;
  vec3 n = o.n;
  vec2 uv = abs(n.y) > 0.5 ? p.xz : abs(n.x) > 0.5 ? p.zy : p.xy;
  vec3 alb;
  if (o.id == O_MESA) {
    alb = cara(ROBLE_OSCURO, uv * 1.0 + 0.5).rgb;
  } else if (o.id == O_PATA || o.id == O_PIE_SILLA) {
    alb = cara(PIEDRANEGRA_PULIDA, uv * 4.0).rgb * 0.8;
  } else if (o.id == O_ASIENTO || o.id == O_RESPALDO) {
    alb = cara(LANA_GRIS, uv * 2.0).rgb;
  } else if (o.id == O_HOJA_PUERTA) {
    // la puerta abierta: se ve de canto o de cara
    vec2 u = vec2(p.z + 13.5, p.y);
    alb = abs(n.x) > 0.5 ? (p.y < 1.0 ? cara(PUERTA_OSCURA_ABAJO, u).rgb : cara(PUERTA_OSCURA_ARRIBA, u).rgb) : cara(TRONCO_OSCURO, uv * 4.0).rgb;
  } else if (o.id == O_LIBRERIA) {
    alb = n.y > 0.5 ? cara(56, p.xz + 0.5).rgb : cara(LIBRERIA, uv + 0.5).rgb;
  } else if (o.id == O_MACETA) {
    alb = cara(MACETA, uv * 2.0).rgb;
  } else if (o.id == O_HOJAS) {
    alb = cara(AZALEA, uv * 1.3).rgb;
  } else if (o.id == O_PAPEL) {
    vec3 c = papelCentro(tg);
    vec2 px = vec2(p.x - (c.x - 0.25), p.z - (c.z - 0.35625)) * 640.0;
    alb = n.y > 0.5 ? colorHoja(px, tg) : vec3(0.85, 0.83, 0.78);
    // la sombra de la mano y del boligrafo sobre el papel
    if (tg > P_COGE0 && tg < P_SUELTA1) {
      vec3 punta, eje;
      float mano;
      boligrafo(tg, punta, eje, mano);
      float alto = max(punta.y - MESA_Y, 0.0);
      vec2 dp = p.xz - (punta.xz + vec2(0.012, 0.01));
      float s = exp(-dot(dp, dp) / (0.00006 + alto * 0.002)) * 0.35;
      vec2 dm = p.xz - (punta.xz + eje.xz * 0.11 + vec2(0.04, 0.03));
      s += exp(-dot(dm, dm) / 0.0035) * 0.35 * mano;
      alb *= 1.0 - min(s, 0.5);
    }
    return alb * brilloOficina(nivelProlo(p + vec3(0.0, 0.5, 0.0))) * 1.02;
  } else if (o.id == O_BANCO) {
    alb = cara(ABETO, uv * 2.0).rgb;
  } else if (o.id == O_EXTINTOR) {
    alb = vec3(0.72, 0.1, 0.08);
    if (p.y > 1.33) alb = vec3(0.15);
    if (p.y > 1.12 && p.y < 1.2) alb = vec3(0.9, 0.88, 0.8);
  } else {
    alb = vec3(1.0, 0.0, 1.0);
  }
  float sombra = 1.0;
  // bajo la mesa
  if (o.id != O_MESA && p.y < 0.69 && abs(p.x) < 1.3 && p.z > -17.7 && p.z < -15.75) sombra = 0.72;
  return luzProlo(alb, p, n) * sombra;
}

/* ------------------------------------------- paredes, suelo y techo */

vec3 paredOficina(vec2 uv, float y);
vec3 reloj(vec3 c, vec2 d, float tg);
vec3 marcoPuertaSala(vec3 c, vec3 p, bool dentro);

// la sala: moqueta gris con borde cian, paredes de la recepcion, pizarra, reloj y placa
vec3 colorSala(vec3 p, vec3 n, float tg) {
  vec2 cel = floor(p.xz + 0.5);
  vec3 c;
  if (n.y > 0.5) {
    bool borde = cel.x == -4.0 || cel.x == 4.0 || cel.y == -21.0 || cel.y == -14.0;
    c = cara(borde ? LANA_CIAN : LANA_GRIS, p.xz + 0.5).rgb;
    if (abs(p.x) < 1.3 && p.z > -17.7 && p.z < -15.75) c *= 0.75; // la sombra de la mesa
  } else if (n.y < -0.5) {
    bool panel = (cel.x == 0.0 && (cel.y == -17.0 || cel.y == -16.0)) || (abs(cel.x) == 3.0 && (cel.y == -20.0 || cel.y == -15.0));
    if (panel) return cara(LUZ_TECHO, p.xz + 0.5).rgb * 1.08;
    c = cara(CUARZO_LISO, p.xz + 0.5).rgb;
  } else {
    vec2 uv = abs(n.x) > 0.5 ? vec2(p.z + 0.5, p.y) : vec2(p.x + 0.5, p.y);
    c = paredOficina(uv, p.y);
    if (n.z > 0.5) {
      // la pared del fondo: pizarra, placa y reloj
      if (abs(p.x) < 1.5 && p.y > 1.25 && p.y < 2.7) {
        bool marco = abs(p.x) > 1.44 || p.y < 1.31 || p.y > 2.64;
        c = marco ? vec3(0.72, 0.74, 0.76) : cartel(L_PIZARRA, vec2((p.x + 1.44) / 2.88, (2.64 - p.y) / 1.33));
      }
      if (abs(p.x) < 0.7 && p.y > 1.2 && p.y < 1.25) c = vec3(0.6, 0.62, 0.64);
      if (p.x > -3.4 && p.x < -2.2 && p.y > 2.05 && p.y < 2.35) c = cartel(L_PLACA, vec2((p.x + 3.4) / 1.2, (2.35 - p.y) / 0.3));
      c = reloj(c, p.xy - vec2(2.6, 2.45), tg);
    }
    if (n.z < -0.5) c = marcoPuertaSala(c, p, true);
  }
  return luzProlo(c, p, n) * esquinas(p, n, SALA0, SALA1);
}

// pared de oficina como la de la recepcion: zocalo de andesita, blanco, la franja
// cian y una moldura de cuarzo bajo el techo
vec3 paredOficina(vec2 uv, float y) {
  int b = y < 1.0 ? ANDESITA : y < 2.0 ? HORMIGON_BLANCO : y < 3.0 ? TERRACOTA_CIAN : CUARZO_LISO;
  return cara(b, uv).rgb;
}

// el reloj de pared (a las 8:59, como la cinta), con la aguja de los segundos
vec3 reloj(vec3 c, vec2 d, float tg) {
  float r = length(d);
  if (r > 0.32) return c;
  vec2 q = floor(d / 0.04) + 0.5;
  float rq = length(q) * 0.04;
  if (rq > 0.28) return vec3(0.12, 0.13, 0.14);
  vec3 col = vec3(0.95, 0.95, 0.92);
  float ang = atan(q.x, q.y);
  // las marcas de las horas
  if (rq > 0.21 && abs(fract(ang / 0.5236 + 0.5) - 0.5) < 0.12) col = vec3(0.2);
  float seg = floor(tg) + 52.0;
  float aM = (59.0 + seg / 60.0) / 60.0 * 6.2831853;
  float aH = (8.0 + 59.0 / 60.0) / 12.0 * 6.2831853;
  float aS = seg / 60.0 * 6.2831853;
  vec2 qq = q * 0.04;
  for (int k = 0; k < 3; k++) {
    float a = k == 0 ? aH : k == 1 ? aM : aS;
    float largo = k == 0 ? 0.13 : k == 1 ? 0.2 : 0.22;
    vec2 dir = vec2(sin(a), cos(a));
    float along = dot(qq, dir);
    float across = abs(qq.x * dir.y - qq.y * dir.x);
    if (along > -0.02 && along < largo && across < 0.025) col = k == 2 ? vec3(0.75, 0.12, 0.1) : vec3(0.1);
  }
  return col;
}

// el marco de la puerta de la sala (de troncos de roble oscuro sin corteza) y su cartel
vec3 marcoPuertaSala(vec3 c, vec3 p, bool dentro) {
  if (abs(p.x) < 1.5 && p.y < 3.0 && !(abs(p.x) < 0.5 && p.y < 2.0)) c = cara(TRONCO_DESC, vec2(p.x + 0.5, p.y)).rgb;
  if (!dentro && p.x > 1.55 && p.x < 2.45 && p.y > 2.2 && p.y < 2.35) c = cartel(L_SALA, vec2((p.x - 1.55) / 0.9, (2.35 - p.y) / 0.15));
  return c;
}

// una puerta cerrada en una pared lateral del pasillo (u a lo largo de la pared)
vec3 puertaLateral(vec3 c, float u, float y, float centro, ivec4 cartelL, bool espejo) {
  float d = u - centro;
  if (abs(d) < 1.5 && y < 3.0) {
    if (abs(d) < 0.5 && y < 2.0) {
      vec2 f = vec2(espejo ? 0.5 - d : d + 0.5, y);
      vec4 pu = y < 1.0 ? cara(PUERTA_OSCURA_ABAJO, f) : cara(PUERTA_OSCURA_ARRIBA, vec2(f.x, y - 1.0));
      c = pu.a > 0.5 ? pu.rgb : vec3(0.08, 0.07, 0.06);
    } else {
      c = cara(TRONCO_DESC, vec2(u + 0.5, y)).rgb;
    }
  }
  if (abs(d) < 0.45 && y > 3.12 && y < 3.27) c = cartel(cartelL, vec2(espejo ? 0.5 - d / 0.9 : d / 0.9 + 0.5, (3.27 - y) / 0.15));
  return c;
}

// el pasillo: suelo ajedrezado de cuarzo y diorita, puertas cerradas, el ascensor al fondo
vec3 colorPasillo(vec3 p, vec3 n, float te) {
  vec2 cel = floor(p.xz + 0.5);
  vec3 c;
  if (n.y > 0.5) {
    c = cara(mod(cel.x + cel.y, 2.0) == 0.0 ? CUARZO_LISO : DIORITA, p.xz + 0.5).rgb;
  } else if (n.y < -0.5) {
    bool panel = cel.x == 0.0 && mod(cel.y, 3.0) == 0.0;
    if (panel) return cara(LUZ_TECHO, p.xz + 0.5).rgb * 1.08;
    c = cara(CUARZO_LISO, p.xz + 0.5).rgb;
  } else if (abs(n.x) > 0.5) {
    c = paredOficina(vec2(p.z + 0.5, p.y), p.y);
    if (n.x > 0.5) c = puertaLateral(c, p.z, p.y, -9.0, L_ARCHIVO, true);
    else c = puertaLateral(c, p.z, p.y, -6.0, L_LAB, false);
  } else if (n.z > 0.5) {
    // la pared de la sala, vista desde el pasillo
    c = marcoPuertaSala(paredOficina(vec2(p.x + 0.5, p.y), p.y), p, false);
  } else {
    // la pared del ascensor: marco de hierro, el indicador y el boton de llamada
    c = paredOficina(vec2(p.x + 0.5, p.y), p.y);
    if (abs(p.x) < 1.5 && p.y < 3.0) c = cara(HIERRO, vec2(p.x + 0.5, p.y)).rgb;
    if (abs(p.x) < 1.0 && p.y < 2.0) {
      // las hojas de la puerta, desde fuera
      float g = puertaAscensor(te);
      float x = p.x;
      float u = x < 0.0 ? x + 1.0 + g : 1.0 - (x - g);
      vec4 d = p.y < 1.0 ? cara(PUERTA_ABAJO, vec2(1.0 - u, p.y)) : cara(PUERTA_ARRIBA, vec2(1.0 - u, p.y - 1.0));
      c = d.a > 0.5 ? d.rgb : cara(HIERRO, vec2(x, p.y)).rgb * 0.55;
    }
    if (abs(p.x) < 0.5 && p.y >= 2.0 && p.y < 3.0) return display(vec2(0.5 - p.x, p.y - 2.0), te);
    if (p.x > 1.62 && p.x < 1.88 && p.y > 1.12 && p.y < 1.5) {
      c = cara(PIEDRANEGRA_PULIDA, p.xy * 4.0).rgb;
      vec2 b = abs(p.xy - vec2(1.75, 1.31));
      if (b.x < 0.05 && b.y < 0.05) {
        float tg = te + PRO;
        bool luce = tg > I_LLAMA && te < T_ABRE1;
        if (luce) return vec3(1.0, 0.72, 0.3) * 1.25;
        c = vec3(0.45, 0.45, 0.47);
      }
    }
    if (abs(p.x) < 0.45 && p.y > 3.12 && p.y < 3.27) c = cartel(L_ASCENSOR, vec2(0.5 - p.x / 0.9, (3.27 - p.y) / 0.15));
  }
  return luzProlo(c, p, n) * esquinas(p, n, PAS0, PAS1);
}

/* ---------------------------------------------- trazar las zonas */

int zonaDe(vec3 p) {
  if (p.z < -13.5) return 0;
  if (p.z < -1.5) return 1;
  return 2;
}

// sigue el rayo de zona en zona (por la puerta de la sala y la del ascensor) hasta
// dar con una pared, el suelo o el techo
float trazaZonas(vec3 ro, vec3 rd, float te, out vec3 n, out int zona) {
  zona = zonaDe(ro);
  float acum = 0.0;
  vec3 o = ro;
  float g = puertaAscensor(te);
  n = vec3(0.0, 1.0, 0.0);
  for (int k = 0; k < 4; k++) {
    vec3 b0 = zona == 0 ? SALA0 : zona == 1 ? PAS0 : CAB0;
    vec3 b1 = zona == 0 ? SALA1 : zona == 1 ? PAS1 : CAB1;
    vec3 t1 = (b0 - o) / rd, t2 = (b1 - o) / rd;
    vec3 tf = max(t1, t2);
    float th = max(min(min(tf.x, tf.y), tf.z), 0.0);
    vec3 nn = th == tf.x ? vec3(-sign(rd.x), 0.0, 0.0) : th == tf.y ? vec3(0.0, -sign(rd.y), 0.0) : vec3(0.0, 0.0, -sign(rd.z));
    vec3 p = o + rd * th;
    int sig = -1;
    if (zona == 0 && nn.z < -0.5 && abs(p.x) < 0.5 && p.y < 2.0) sig = 1;
    else if (zona == 1 && nn.z > 0.5 && abs(p.x) < 0.5 && p.y < 2.0) sig = 0;
    else if (zona == 1 && nn.z < -0.5 && abs(p.x) < g && p.y < 2.0) sig = 2;
    else if (zona == 2 && nn.z > 0.5 && abs(p.x) < g && p.y < 2.0) sig = 1;
    if (sig < 0) {
      n = nn;
      return acum + th;
    }
    acum += th + 1e-4;
    o = p + rd * 1e-4;
    zona = sig;
  }
  return acum;
}

// lo que se ve fuera de la cabina (o desde la puerta abierta, al mirar atras)
vec3 fuera(vec3 ro, vec3 rd, float te) {
  float tg = te + PRO;
  vec3 n;
  int zona;
  float th = trazaZonas(ro, rd, te, n, zona);
  vec3 cam = camGlobal;
  Obj o;
  o.t = th;
  o.id = -1;
  objetos(o, ro, rd, tg, te, cam);
  // el boligrafo
  if (tg > P_BOLI0 && tg < 30.0) {
    vec3 punta, eje;
    float mano;
    boligrafo(tg, punta, eje, mano);
    vec3 bx, by;
    marcoBoli(punta, eje, cam, bx, by);
    vec3 nb, lp;
    int mat;
    float tb = trazaBoli(ro, rd, o.t, punta, eje, bx, by, nb, lp, mat);
    if (tb < o.t) return colorBoli(lp, nb, rd, mat, ro + rd * tb);
  }
  if (o.id >= 0) {
    if (o.id >= O_MANO) {
      // en las cajas giradas, la cara que se toca (en su marco) la dice el punto
      vec3 q = o.q, h = o.h;
      vec3 d = min(q, h - q);
      vec3 nl = d.x < d.y && d.x < d.z ? vec3(q.x > 0.5 * h.x ? 1.0 : -1.0, 0.0, 0.0)
        : d.y < d.z ? vec3(0.0, q.y > 0.5 * h.y ? 1.0 : -1.0, 0.0) : vec3(0.0, 0.0, q.z > 0.5 * h.z ? 1.0 : -1.0);
      vec3 p = ro + rd * o.t;
      vec3 alb = o.id >= O_INV ? pielInvestigador(o.id, nl, q, h, tg) : colorMano(o.id, q, nl, h);
      return luzProlo(alb, p, o.n);
    }
    return colorObjeto(o, ro, rd, tg);
  }
  vec3 p = ro + rd * th;
  if (zona == 0) return colorSala(p, n, tg);
  if (zona == 1) return colorPasillo(p, n, te);
  // dentro de la cabina, vista desde fuera
  if (n.y < -0.5) return techo(p, n, te);
  if (n.y > 0.5) return suelo(p, n, te);
  if (abs(n.x) > 0.5) return paredLado(p, n, te, n.x < 0.0);
  return paredLado(p, n, te, false);
}

/* --------------------------------------------------- la camara */

// donde esta la camara al leer: mirando la fila `fila` de la hoja desde d bloques,
// con elevacion e sobre el papel
void poseLectura(float fila, float dx, float d, float e, out vec3 ro, out float pitch) {
  vec3 c = papelCentro(P_LEE0 + 1.0);
  vec3 obj = vec3(c.x + dx, MESA_Y, c.z - 0.35625 + fila / 640.0);
  ro = obj + d * vec3(0.0, sin(e), cos(e));
  pitch = -e;
}

Camara montar(vec3 ro, float yaw, float pitch, float roll) {
  vec3 f = vec3(sin(yaw) * cos(pitch), sin(pitch), -cos(yaw) * cos(pitch));
  vec3 r = normalize(cross(f, vec3(0.0, 1.0, 0.0)));
  vec3 up = cross(r, f);
  Camara c;
  c.ro = ro;
  c.f = f;
  c.r = r * cos(roll) + up * sin(roll);
  c.u = up * cos(roll) - r * sin(roll);
  return c;
}

Camara camaraPrologo(float t) {
  float te = t - PRO;
  // la camara va quieta (sin temblor de mano) hasta que el ascensor empieza a fallar
  vec3 sh = vec3(0.0);
  // sentado a la mesa: le miras, miras el papel que te acerca, otra vez a el
  vec3 ro = SENTADO + vec3(0.0, 0.006 * sin(t * 1.6), 0.0);
  float pitch = 0.17;
  pitch = mix(pitch, -0.3, smoothstep(2.0, 3.0, t));
  pitch = mix(pitch, 0.14, smoothstep(4.6, 5.4, t));
  pitch = mix(pitch, -0.32, smoothstep(22.5, 23.5, t));
  pitch = mix(pitch, 0.12, smoothstep(25.3, 26.1, t));
  // la lectura: 12 s desde que te inclinas sobre la hoja hasta la linea de la firma
  float fila = 112.0 + (404.0 - 112.0) * avance((t - (P_LEE0 + 1.7)) / (P_LEE1 - P_LEE0 - 1.7), 0.12);
  float dx = -0.035 * smoothstep(P_LEE1 - 1.5, P_LEE1 + 0.5, t);
  float d = mix(0.25, 0.285, smoothstep(P_LEE1 - 2.0, P_LEE1, t));
  float e = mix(1.2, 1.3, smoothstep(P_LEE0 + 1.0, P_LEE1, t));
  vec3 rl;
  float pl;
  poseLectura(fila, dx, d, e, rl, pl);
  float k = smoothstep(P_LEE0, P_LEE0 + 1.5, t) * (1.0 - smoothstep(P_ALZA0, P_ALZA1, t));
  ro = mix(ro, rl + vec3(0.0, 0.003 * sin(t * 1.3), 0.0), k);
  pitch = mix(pitch, pl, k);
  float yaw = 0.0;
  // te levantas y miras como se va hacia la puerta
  float lev = smoothstep(P_LEVANTA0, P_LEVANTA1, t);
  ro = mix(ro, DE_PIE, lev);
  pitch = mix(pitch, -0.02, lev);
  Pose q = investigador(t, ro);
  vec3 ojoInv = q.pie + vec3(0.0, 1.5, 0.0);
  float yInv = rumbo(ojoInv.xz - ro.xz);
  if (yInv > 4.7) yInv -= 6.2831853;
  yaw = mix(yaw, yInv, smoothstep(25.3, 26.6, t) * (1.0 - smoothstep(P_ANDA0 - 0.2, P_ANDA0 + 0.9, t)));
  // le sigues por el pasillo hasta el ascensor
  float L = largoRuta(RUTA_J, NJ);
  float u = (t - P_ANDA0) / (P_ANDA1 - P_ANDA0);
  float s = L * avance(u, 0.12);
  float anda = marcha(u, 0.12);
  if (t > P_ANDA0 - 0.2) {
    vec2 xz = enRuta(RUTA_J, NJ, s);
    vec2 dir = enRuta(RUTA_J, NJ, min(s + 0.9, L)) - enRuta(RUTA_J, NJ, max(s - 0.3, 0.0));
    float yA = rumbo(dir);
    float w = smoothstep(P_ANDA0 - 0.2, P_ANDA0 + 0.9, t);
    yaw = mix(yaw, yA, w);
    ro = vec3(xz.x, DE_PIE.y, xz.y);
    pitch = mix(pitch, -0.06, w);
  }
  // el paso: la camara sube y baja y se mece un poco
  float fase = s * 3.14159 / 1.2;
  vec3 der = vec3(cos(yaw), 0.0, sin(yaw));
  ro.y += 0.016 * (abs(cos(fase)) - 0.64) * anda;
  float roll = 0.0;
  // dentro de la cabina te das la vuelta hacia las puertas
  yaw += 3.14159265 * smoothstep(P_GIRA0, P_GIRA1, t);
  pitch = mix(pitch, -0.08, smoothstep(P_GIRA0, P_GIRA1, t));
  ro += sh;
  return montar(ro, yaw + sh.x * 1.5, pitch + sh.y * 1.5, roll + sh.z * 2.0);
}

/* ---------------------------------------------------------- escena */

// guion 2: un jugador de pie en medio del ascensor de salida, con la videocamara
vec3 escenaF(vec2 uv, float t) {
  float sac = 0.004;
  if (t > A_CAE && t < A_FRENA + 0.6) sac = 0.03;
  vec3 sh = (vec3(ruido(vec2(t * 13.0, 1.0)), ruido(vec2(t * 11.0, 5.0)), ruido(vec2(t * 9.0, 9.0))) - 0.5) * 2.0 * sac;
  vec3 ro = vec3(0.0, 1.62, 0.0) + sh;
  // empieza mirando al panel que acaba de pulsar y se gira hacia las puertas
  float yaw = 3.14159 * (1.0 - smoothstep(0.9, 2.1, t));
  float pitch = -0.08;
  float roll = sh.z * 2.0;
  {
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

// la camara del ascensor del /start (t: segundos desde que estas dentro mirando a las puertas)
Camara camaraAscensor(float t) {
  // un jugador de pie en la cabina, con la videocamara en la mano: quieta hasta que se parte el cable
  float sac = 0.0;
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
  return c;
}

// la cabina vista desde dentro
vec3 cabina(Camara c, vec2 uv, float t) {
  vec3 ro = c.ro;
  vec3 f = c.f;
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

// el /start entero: t desde el principio (la sala); el ascensor va en te = t - PRO
vec3 escena(vec2 uv, float t) {
  float te = t - PRO;
  Camara c;
  if (te < 0.0) c = camaraPrologo(t);
  else c = camaraAscensor(te);
  camGlobal = c.ro;
  if (c.ro.z > -1.5 && abs(c.ro.x) < 1.5) return cabina(c, uv, te);
  return fuera(c.ro, normalize(c.f + (uv.x * c.r + uv.y * c.u) * FOVK), te);
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
  } else {
    float te = t - PRO;
    // las rayas de la cinta solo cuando el ascensor falla (antes la imagen va limpia)
    glitch = 0.12 * smoothstep(T_ATASCO - 0.5, T_ATASCO, te) + abs(golpe(te, T_ATASCO, 0.6) + golpe(te, T_ROTURA, 0.9) + golpe(te, T_PUERTAS, 0.6));
    glitch += 0.3 * smoothstep(T_ROTURA, T_IMPACTO, te);
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
    float te = t - PRO;
    col += vec3(1.0, 0.5, 0.2) * smoothstep(T_IMPACTO - 1.0, T_IMPACTO, te) * 0.5;
    if (te > T_IMPACTO) {
      float u = te - T_IMPACTO;
      if (u < 0.1) col = vec3(1.6);
      else {
        float n = h2(floor(frag / vec2(3.0, 1.0)) + floor(t * 50.0));
        col = mix(vec3(n) * 0.8, vec3(0.0), smoothstep(0.1, 0.4, u));
      }
    }
  } else {
    // el final de la cinta: la imagen se rompe en nieve y negro
    float fin = A_FIN;
    float rompe = smoothstep(fin - 0.6, fin, t);
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
  fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
