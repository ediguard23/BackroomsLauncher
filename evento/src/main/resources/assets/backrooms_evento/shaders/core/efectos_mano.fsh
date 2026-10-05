#version 330

// Oscurece la mano igual que el mundo (ver EfectosMundo.java). Se mezcla
// multiplicando: donde no hay mano sale blanco y no cambia nada.

uniform sampler2D DepthSampler;

layout(std140) uniform Efectos {
    mat4 InvProyVista;
    vec4 Estado;
    vec4 Estado2;
    vec4 Estado3;
    vec4 Luces[32];
    vec4 Brillos[8];
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texture(DepthSampler, texCoord).r >= 1.0) {
        fragColor = vec4(1.0);
        return;
    }
    // con la linterna propia encendida la mano se ve con lo que rebota
    float luz = mix(0.02, 0.55, Estado2.y);
    vec3 f = vec3(mix(1.0, luz, Estado.x));
    float latido = 0.55 + 0.45 * sin(Estado.z * 5.2);
    f *= mix(vec3(1.0), vec3(1.25, 0.3, 0.25) * (0.6 + 0.5 * latido), Estado.y * 0.85);
    f = mix(f, vec3(1.0), Estado2.z);
    fragColor = vec4(f, 1.0);
}
