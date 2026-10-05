#version 330

// Efectos del Nivel 0 sobre la imagen del mundo (ver EfectosMundo.java):
// apagon con linternas, alarma roja, camara (y su vision nocturna), cordura y
// miedo (una Bacteria cazandote cerca, ver Miedo.java).

uniform sampler2D ColorSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform Efectos {
    mat4 InvProyVista;
    vec4 Estado;   // oscuridad 0-1, alarma 0-1, tiempo (s), cordura baja 0-1
    vec4 Estado2;  // camara levantada 0-1, linterna propia, flash blanco, numero de luces
    vec4 Estado3;  // numero de brillos, aspecto, susto, -
    vec4 Estado4;  // miedo 0-1, golpe del latido 0-1, -, -
    vec4 Luces[32];  // por luz: (pos relativa, intensidad) y (direccion, coseno del cono)
    vec4 Brillos[8]; // (pos relativa, radio)
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 posicion(vec2 uv, float profundidad) {
    vec4 p = InvProyVista * vec4(uv * 2.0 - 1.0, profundidad * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

void main() {
    float t = Estado.z;
    float cordura = Estado.w;
    float camara = Estado2.x;
    float miedo = Estado4.x;
    float golpe = Estado4.y * miedo;
    vec2 uv = texCoord;

    // ---- miedo: con cada latido la vista se encoge un poco
    if (miedo > 0.0) {
        uv = 0.5 + (uv - 0.5) * (1.0 - 0.012 * golpe);
    }

    // ---- cordura baja: la imagen respira y ondula
    if (cordura > 0.0) {
        vec2 c = uv - 0.5;
        float r = length(c);
        uv += c * sin(t * 0.9) * 0.012 * cordura;
        uv += vec2(sin(uv.y * 14.0 + t * 1.7), cos(uv.x * 11.0 + t * 1.3)) * 0.0035 * cordura * (0.4 + r);
    }
    // ---- camara: un poco de cinta (lineas que tiemblan)
    if (camara > 0.0) {
        float linea = floor(uv.y * 240.0);
        uv.x += (hash(vec2(linea, floor(t * 24.0))) - 0.5) * 0.0016 * camara;
        float tiron = step(0.985, hash(vec2(floor(t * 8.0), 3.1)));
        uv.x += tiron * 0.01 * camara * smoothstep(0.2, 0.0, abs(uv.y - fract(t * 0.37)));
    }

    vec3 col = texture(ColorSampler, uv).rgb;
    float sep = 0.0025 * cordura + 0.0012 * camara + (0.001 + 0.0035 * Estado4.y) * miedo;
    if (sep > 0.0) {
        col.r = texture(ColorSampler, uv + vec2(sep, 0.0)).r;
        col.b = texture(ColorSampler, uv - vec2(sep, 0.0)).b;
    }

    float profundidad = texture(DepthSampler, uv).r;
    vec3 pos = posicion(uv, profundidad);
    float dist = length(pos);
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    if (dot(normal, pos) > 0.0) {
        normal = -normal;
    }

    // ---- apagon: solo se ve lo que alumbran las linternas
    float oscuridad = Estado.x;
    if (oscuridad > 0.0) {
        float luz = 0.0;
        int n = int(Estado2.w + 0.5);
        for (int i = 0; i < 16; i++) {
            if (i >= n) {
                break;
            }
            vec4 a = Luces[i * 2];
            vec4 b = Luces[i * 2 + 1];
            vec3 L = pos - a.xyz;
            float d = max(length(L), 0.001);
            vec3 dir = L / d;
            float c = dot(dir, b.xyz);
            float cono = smoothstep(b.w, mix(b.w, 1.0, 0.45), c);
            // centro mas fuerte y un anillo, como el reflector de una linterna de verdad
            float centro = smoothstep(0.975, 0.997, c) * 0.55;
            float anillo = smoothstep(0.93, 0.945, c) * smoothstep(0.96, 0.945, c) * 0.18;
            float caida = 1.0 / (1.0 + d * d * 0.010) * smoothstep(34.0, 14.0, d);
            float cara = clamp(dot(normal, -dir), 0.0, 1.0) * 0.8 + 0.2;
            luz += (cono * (0.95 + centro + anillo) * caida * cara) * a.w;
            // lo poco que salpica alrededor de quien la lleva
            luz += a.w * 0.06 / (1.0 + d * d * 0.6);
        }
        // camara a oscuras: infrarrojos de corto alcance
        float ir = camara * smoothstep(13.0, 3.0, dist) * 0.85;
        // las caras de los Smilers brillan solas
        int nb = int(Estado3.x + 0.5);
        float brillo = 0.0;
        for (int i = 0; i < 8; i++) {
            if (i >= nb) {
                break;
            }
            float d = length(pos - Brillos[i].xyz);
            brillo = max(brillo, smoothstep(Brillos[i].w, Brillos[i].w * 0.55, d));
        }
        vec3 bombilla = vec3(1.0, 0.93, 0.80);
        vec3 alumbrado = col * min(luz, 1.6) * bombilla;
        vec3 nocturno = vec3(0.25, 1.0, 0.35) * luma(col) * ir * 1.2;
        vec3 oscuro = max(alumbrado, nocturno);
        oscuro = max(oscuro, col * brillo);
        // grano en lo negro: los ojos buscando algo
        oscuro += (hash(uv * 911.0 + t) - 0.5) * 0.012;
        col = mix(col, max(oscuro, 0.0), oscuridad);
    }

    // ---- alarma: luz roja que late
    float alarma = Estado.y;
    if (alarma > 0.0) {
        float latido = 0.55 + 0.45 * sin(t * 5.2);
        float l = luma(col);
        vec3 rojo = vec3(l * 1.55, l * 0.22, l * 0.18) * (0.55 + 0.6 * latido);
        col = mix(col, rojo, alarma * 0.92);
    }

    // ---- camara: color de videocamara barata, grano y vineta
    if (camara > 0.0) {
        vec3 cam = mix(vec3(luma(col)), col, 0.75) * vec3(1.02, 1.0, 0.94);
        cam = (cam - 0.5) * 1.12 + 0.5;
        cam += (hash(uv * 600.0 + fract(t)) - 0.5) * 0.07;
        cam *= 0.94 + 0.06 * sin(uv.y * 900.0 + t * 30.0);
        vec2 v = uv - 0.5;
        cam *= 1.0 - dot(v, v) * 0.9;
        col = mix(col, cam, camara);
    }

    // ---- cordura baja: se cierra la vista y se apaga el color
    if (cordura > 0.0) {
        vec2 v = texCoord - 0.5;
        float pulso = 0.85 + 0.15 * sin(t * 2.4);
        col = mix(col, vec3(luma(col)), cordura * 0.55);
        col *= 1.0 - smoothstep(0.15, 0.75, length(v) * (1.0 + cordura * pulso)) * cordura;
    }

    // ---- miedo: el color se apaga un poco y los bordes se cierran y laten
    if (miedo > 0.0) {
        float borde = smoothstep(0.22, 0.8, length(texCoord - 0.5) * (1.0 + 0.25 * golpe));
        col = mix(col, vec3(luma(col)), miedo * 0.3);
        col *= 1.0 - borde * (0.45 + 0.3 * golpe) * miedo;
        col.r += borde * 0.06 * golpe;
    }

    // ---- susto (alucinacion): un fogonazo de negativo
    float susto = Estado3.z;
    if (susto > 0.0) {
        col = mix(col, vec3(1.0) - col.gbr, susto);
    }

    // ---- flash del Smiler: blanco que ciega
    col = mix(col, vec3(1.0), Estado2.z);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
