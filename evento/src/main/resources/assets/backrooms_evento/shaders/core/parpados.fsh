#version 330

// Parpados al despertar tras el golpe: negro fuera de una abertura en forma de
// almendra que se abre y se cierra (parpadeos), con la vineta rojiza de quien
// ve a traves de la sangre de los parpados. La abertura (0 cerrado, 1 abierto)
// llega en la U de los vertices y el tiempo en la V.
#moj_import <minecraft:globals.glsl>

in vec2 datos;
in vec4 color;
out vec4 fragColor;

float h2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

void main() {
  vec2 res = ScreenSize;
  vec2 uv = (gl_FragCoord.xy / res) * 2.0 - 1.0;
  uv.x *= res.x / res.y;
  float abre = datos.x;
  float t = datos.y;
  // almendra: alto maximo en el centro, se cierra hacia los lados
  float ancho = res.x / res.y * 1.15;
  float perfil = max(0.0, 1.0 - pow(abs(uv.x) / ancho, 2.0));
  float alto = abre * 1.25 * perfil + smoothstep(0.75, 1.0, abre) * 1.6;
  // el borde del parpado no es recto: tiembla un poco
  float borde = abs(uv.y + 0.03 * sin(uv.x * 2.0 + t)) - alto;
  float suave = 0.06 + 0.25 * (1.0 - abre);
  float tapado = smoothstep(-suave, suave * 0.3, borde);
  // vineta oscura que pesa mas cuanto mas cerrados estan
  float vin = smoothstep(0.4, 1.6, length(uv * vec2(0.8, 1.0))) * (0.85 - 0.5 * abre);
  float a = clamp(max(tapado, vin), 0.0, 1.0);
  vec3 c = mix(vec3(0.0), vec3(0.16, 0.015, 0.01), (1.0 - tapado) * vin);
  c += (h2(gl_FragCoord.xy + t) - 0.5) * 0.02;
  fragColor = vec4(c, a);
}
