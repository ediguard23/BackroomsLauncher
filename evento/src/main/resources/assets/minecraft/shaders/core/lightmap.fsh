#version 330

// Mapa de luz del evento. Fuera de las Piscinas hace lo mismo que el de Minecraft
// 1.21.11 (luz de bloque que tira a amarillo a media intensidad, cielo, gamma...).
// En las Piscinas (LightTextureMixin manda el color de ambiente con 2 de mas en el
// azul) la luz de los bloques es blanca y fria y las sombras toman ese color.

layout(std140) uniform LightmapInfo {
    float AmbientLightFactor;
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float DarkenWorldFactor;
    float BrightnessFactor;
    vec3 SkyLightColor;
    vec3 AmbientColor;
} info;

in vec2 texCoord;

out vec4 fragColor;

float brillo(float nivel) {
    return nivel / (4.0 - 3.0 * nivel);
}

// la curva de "brillo" de las opciones: sube los oscuros sin pasarse de 1
vec3 levantar(vec3 c) {
    float m = max(max(c.x, c.y), c.z);
    float k = 1.0 - pow(1.0 - m, 4.0);
    return c * (k / m);
}

void main() {
    float bloque = brillo(floor(texCoord.x * 16.0) / 15.0) * info.BlockFactor;
    float cielo = brillo(floor(texCoord.y * 16.0) / 15.0) * info.SkyFactor;
    bool piscinas = info.AmbientColor.b > 1.5;
    vec3 ambiente = piscinas ? info.AmbientColor - vec3(0.0, 0.0, 2.0) : info.AmbientColor;

    vec3 c;
    if (piscinas) {
        c = vec3(bloque) * vec3(0.90, 0.98, 1.0);
    } else {
        c = vec3(bloque, bloque * ((bloque * 0.6 + 0.4) * 0.6 + 0.4), bloque * (bloque * bloque * 0.6 + 0.4));
    }
    c = mix(c, ambiente, info.AmbientLightFactor);
    c += info.SkyLightColor * cielo;
    c = mix(c, vec3(0.75), 0.04);

    if (info.AmbientLightFactor == 0.0) {
        c = mix(c, c * vec3(0.7, 0.6, 0.6), info.DarkenWorldFactor);
    }
    if (info.NightVisionFactor > 0.0) {
        float m = max(c.r, max(c.g, c.b));
        if (m < 1.0) {
            c = mix(c, c / m, info.NightVisionFactor);
        }
    }
    if (info.AmbientLightFactor == 0.0) {
        c -= vec3(info.DarknessScale);
    }
    c = clamp(c, 0.0, 1.0);
    c = mix(c, levantar(c), info.BrightnessFactor);
    c = mix(c, vec3(0.75), 0.04);
    fragColor = vec4(c, 1.0);
}
