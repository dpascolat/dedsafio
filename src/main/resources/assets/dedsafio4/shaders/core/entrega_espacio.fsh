#version 150

// EL VACÍO CÓSMICO (el efecto de poción "Vacío Cósmico", client/ruleta/EntregaCliente; la entrega en el Pilar), COMO EN EL
// SERVER (los videos que pasó el usuario): TODO lo que se dibujó antes de las criaturas —el terreno y el cielo; en un
// lugar cerrado, como el del server, también las paredes, el techo y el piso— se vuelve, parejo, un espacio casi negro
// con estrellas finas y pocas, alguna más brillante, y una bruma de nebulosa morada tenue en algunas zonas, en todas las
// direcciones (también abajo: flota en el espacio). La luz de los bloques se apaga aparte (PilarEntrega.apagarLuz), así
// tampoco queda nada encendido. Quedan lo que se dibuja después (los jugadores, las criaturas, el piso de colores) y el
// pilar de la entrega (su caja). Sólo lo ve quien tiene el efecto. Todo suave: cada estrella una gaussiana de su tamaño,
// nunca más chica que un píxel (antes eran puntos de un píxel: "se ve todo pixelado").

uniform sampler2D Sampler2;      // la profundidad del mundo

uniform mat4 InvVistaProy;
uniform vec4 Forma;              // xy: hacia dónde miraba al empezar (x, z del mundo: el espacio se pone a su frente); w: segundos
uniform vec4 Fundido;            // x: el vacío (0..1); y: las estrellas (0..1); z: 1 si es el de la fogata (sus colores)
uniform vec4 PilarMin;           // la caja del pilar de la entrega (desde el ojo): no se tapa
uniform vec4 PilarMax;

in vec2 pantalla;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash3(vec3 p) {
    return vec3(hash(p), hash(p + 17.31), hash(p + 41.77));
}

float ruido(vec3 x) {
    vec3 i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash(i), hash(i + vec3(1, 0, 0)), f.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y);
    float b = mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y);
    return mix(a, b, f.z);
}

float fbm(vec3 p, int octavas) {
    float t = 0.0, a = 0.5, s = 0.0;
    for (int i = 0; i < octavas; i++) {
        t += a * ruido(p);
        s += a;
        p = p * 2.03 + 7.1;
        a *= 0.5;
    }
    return t / s;
}

// ---------------------------------------------------------------- las estrellas

vec3 temperatura(float h) {
    if (Fundido.z > 0.5) {                     // el de la fogata: celestes y lima, del color de sus aros
        if (h < 0.45) return vec3(0.31, 0.92, 0.95);
        if (h < 0.88) return vec3(0.81, 0.98, 0.39);
        return vec3(0.88, 1.00, 0.95);
    }
    if (h < 0.28) return vec3(0.72, 0.82, 1.00);
    if (h < 0.68) return vec3(1.00, 0.97, 0.94);
    if (h < 0.90) return vec3(1.00, 0.87, 0.68);
    return vec3(1.00, 0.68, 0.50);
}

// una estrella de radio r a la distancia d (en radianes): una gaussiana nunca más angosta que el píxel (px), con la
// misma luz total (así las chiquitas se ven tenues y suaves, no puntos duros que parpadean)
float estrella(float d, float r, float px) {
    float sg = max(r, 0.6 * px);
    return (r * r) / (sg * sg) * exp(-0.5 * d * d / (sg * sg));
}

// una capa de estrellas chicas: una por celda (de 1/k radianes), si le toca, dentro de su celda
vec3 capa(vec3 n, float k, float dens, float rMin, float rMax, float px, float s, float luz) {
    vec3 p = n * k;
    vec3 c = floor(p);
    if (hash(c + 3.7) > dens) return vec3(0.0);
    vec3 q = normalize(c + 0.3 + 0.4 * hash3(c)) * k;
    float d = length(p - q) / k;
    float b = hash(c + 9.1);
    float r = mix(rMin, rMax, b * b);
    float brillo = luz * (0.3 + 0.7 * b * b * b);
    float respira = 0.85 + 0.15 * sin(s * (0.7 + 2.0 * hash(c + 5.3)) + 6.2832 * hash(c + 1.9));
    return temperatura(hash(c + 7.7)) * brillo * respira * estrella(d, r, px);
}

// las brillantes: pocas, con halo y puntas en cruz (se miran las celdas de alrededor: el halo pasa de la suya)
vec3 brillantes(vec3 n, float k, float dens, float px, float s) {
    vec3 p = n * k;
    vec3 c0 = floor(p);
    vec3 acc = vec3(0.0);
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 c = c0 + vec3(float(x), float(y), float(z));
                if (hash(c + 11.3) > dens) continue;
                vec3 dirS = normalize(c + 0.2 + 0.6 * hash3(c + 2.1));
                vec3 dv = n - dirS;
                float d = length(dv);
                if (d > 0.07) continue;
                float b = hash(c + 4.4);
                vec3 tc = temperatura(hash(c + 8.8));
                float nucleo = estrella(d, 0.0008 + 0.0014 * b, px) * (1.0 + 1.6 * b);
                float halo = exp(-d / (0.004 + 0.008 * b)) * 0.12 * (0.3 + b);
                vec3 t1 = normalize(cross(dirS, vec3(0.0, 1.0, 0.0)) + 1e-4);
                vec3 t2 = cross(dirS, t1);
                float a1 = dot(dv, t1), a2 = dot(dv, t2);
                float fino = max(0.6 * px, 0.0003), largo = 0.004 + 0.022 * b;
                float puntas = (exp(-abs(a1) / fino - abs(a2) / largo) + exp(-abs(a2) / fino - abs(a1) / largo)) * 0.4 * b * b;
                float respira = 0.9 + 0.1 * sin(s * (0.6 + 1.2 * hash(c + 6.6)) + 6.2832 * hash(c));
                acc += tc * (nucleo + halo + puntas) * respira;
            }
        }
    }
    return acc;
}

// ---------------------------------------------------------------- el espacio en esa dirección

vec3 espacio(vec3 n, float cuanto, float s, float px) {
    vec3 col = vec3(0.006, 0.006, 0.014);
    // la bruma de nebulosa: morada y algo azul, tenue, sólo en algunas zonas (como la del video, arriba a la derecha)
    vec3 w = vec3(fbm(n * 2.6 + 1.3, 3), fbm(n * 2.6 + 7.9, 3), fbm(n * 2.6 + 4.2, 3));
    float zona = smoothstep(0.45, 0.75, fbm(n * 1.2 + vec3(7.0, 3.0, 1.0), 3));
    float neb = fbm(n * 2.2 + 1.2 * w + vec3(2.0, 5.0, 9.0), 4);
    vec3 cNeb = mix(vec3(0.42, 0.10, 0.55), vec3(0.12, 0.20, 0.55), smoothstep(0.4, 0.7, w.x));
    col += cNeb * smoothstep(0.42, 0.82, neb) * zona * 0.16 * cuanto;
    // las estrellas: finas y pocas, alguna de color; unas pocas más brillantes con un halo chico
    vec3 e = capa(n, 120.0, 0.05, 0.0002, 0.0005, px, s, 1.8)
           + capa(n, 55.0, 0.05, 0.0004, 0.0009, px, s * 0.7, 2.2)
           + brillantes(n, 12.0, 0.07, px, s) * 0.6;
    return col + e * cuanto;
}

void main() {
    float vacio = Fundido.x, cuanto = Fundido.y, s = Forma.w;
    if (vacio <= 0.001) discard;
    vec2 uvp = pantalla * 0.5 + 0.5;
    float z = texture(Sampler2, uvp).r;
    vec4 lejos = InvVistaProy * vec4(pantalla, 1.0, 1.0);
    vec3 dir = normalize(lejos.xyz / lejos.w);
    float px = length(fwidth(dir));
    if (z < 1.0) {
        vec4 w4 = InvVistaProy * vec4(pantalla, z * 2.0 - 1.0, 1.0);
        vec3 punto = w4.xyz / w4.w;
        if (all(greaterThanEqual(punto, PilarMin.xyz)) && all(lessThanEqual(punto, PilarMax.xyz))) discard;
    }
    // el espacio, girado para quien entra (adelante = -z, la derecha = +x): las zonas de nebulosa le quedan igual siempre
    vec2 fr = normalize(Forma.xy + vec2(0.0, 1e-5));
    vec3 n = vec3(dir.x * -fr.y + dir.z * fr.x, dir.y, -(dir.x * fr.x + dir.z * fr.y));
    fragColor = vec4(espacio(n, cuanto, s, px) * vacio, vacio);
}
