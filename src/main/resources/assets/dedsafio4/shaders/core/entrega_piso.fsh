#version 150

// EL PISO DE LA ENTREGA EN EL PILAR (client/ruleta/EntregaCliente; del add-on: entrega.py, PIEZAS 1 y 2, con sus medidas y sus
// tiempos, sacados de los videos del server). Allá eran cientos de imágenes (cuadros del agua, del cierre y del aro) a
// 20-60 por segundo; aquí se calcula todo en cada píxel, a los cuadros de la pantalla:
//   - 8 PIEZAS de agua de colores separadas por una franja, redondas por fuera y con el borde de adentro difuminado,
//     un poco transparentes (las crestas más), con ONDAS que se tuercen y "respiran" y una OLA BRILLANTE por pieza que
//     corre despacio hacia el pilar;
//   - el piso entero CRECE desde el centro al empezar;
//   - EL ARO DE AFUERA: una franja que brilla pegada al borde, con los colores del arcoíris corridos;
//   - a los 4.6 s cada pieza (verde, amarilla, naranja, roja, rosa, morada, azul, celeste) SE ENCOGE HACIA EL ARO, una
//     detrás de otra: antes su ola se acelera y se enciende, las ondas se apagan y queda lisa, como su pedazo del ARO
//     CHICO (con una rayita entre pedazos); su pedazo del aro de afuera se apaga con ella;
//   - a los 7.95 s, con el golpe del sonido, EL ARO GRANDE (el de afuera) sale del chico y se agranda hasta el borde
//     mientras se apaga.
// Sale con su alfa (premultiplicado): se mezcla, no se suma.

uniform vec4 Tiempo;             // x: s desde que se tocó el pilar; z: el Vacío Cósmico de quien mira (0..1)

in vec2 local;

out vec4 fragColor;

const float PI = 3.14159265, SECTOR = 2.0 * PI / 8.0;
const float RADIO = 8.0, HUECO = 0.8, DIFUMINADO = 2.6, SEPARACION = 0.35;
const float T_ESPERA = 0.1, T_ENTRA = 1.4, ESCALA_INICIAL = 0.1;
const float T_SALE = 4.6, ESCALON = 0.15, T_PIEZA = 0.8, OLA_ANTES = 1.8;
const float ARO_RADIO = 1.38, ARO_ANCHO = 0.32, ARO_RAYITA = 0.07, ARO_SIN_OLAS = 0.35, ARO_LUZ = 0.9;
const float T_ARO = 7.95, T_EXPANDE = 0.8, ARO_GRANDE_MAX = 8.5;
const float AFUERA_ANCHO = 0.85, AFUERA_ALFA = 0.5, AFUERA_CORRE = 1.75;
const float PIEZA_ALFA = 0.3, LUZ_AGUA = 0.75, RESPIRA = 0.035, ANILLO = 5.0, PERIODO = 4.0, CIERRE_ANILLOS = 2.6;

vec3 colorPieza(int i) {
    if (i == 0) return vec3(190, 35, 55) / 255.0;
    if (i == 1) return vec3(215, 110, 30) / 255.0;
    if (i == 2) return vec3(205, 190, 45) / 255.0;
    if (i == 3) return vec3(55, 185, 60) / 255.0;
    if (i == 4) return vec3(35, 185, 185) / 255.0;
    if (i == 5) return vec3(40, 70, 205) / 255.0;
    if (i == 6) return vec3(115, 45, 195) / 255.0;
    return vec3(200, 50, 175) / 255.0;
}

// el arcoíris seguido alrededor (para los aros): el color de las piezas, mezclado entre una y otra
vec3 arcoiris(float ang) {
    float f = ang / SECTOR - 0.5;
    float i0 = floor(f);
    float k = f - i0;
    k = k * k * (3.0 - 2.0 * k);
    return mix(colorPieza(int(mod(i0, 8.0))), colorPieza(int(mod(i0 + 1.0, 8.0))), k);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float ruido(vec2 x) {
    vec2 i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float t = 0.0, a = 1.0, s = 0.0;
    for (int k = 0; k < 4; k++) {
        t += a * ruido(p);
        s += a;
        p = p * 2.0 + 13.7;
        a *= 0.5;
    }
    return t / s;
}

// cuánto se fue la pieza i (0..1) y cuándo empieza a irse: en orden alrededor (la verde primero, la celeste al final)
float demora(int i) {
    return mod(3.0 - float(i) + 8.0, 8.0) * ESCALON;
}

float encogida(int i, float t) {
    float x = clamp((t - T_SALE - demora(i)) / T_PIEZA, 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
}

void main() {
    float t = Tiempo.x;
    if (t < T_ESPERA) discard;
    vec2 q = local;
    // el piso crece desde el centro (con su agua)
    float e = clamp((t - T_ESPERA) / T_ENTRA, 0.0, 1.0);
    e = 1.0 - pow(1.0 - e, 3.0);
    float escala = mix(ESCALA_INICIAL, 1.0, e);
    vec2 p = q / escala;
    float d = length(p);
    float px = max(length(fwidth(p)), 1e-4);
    float ang = atan(p.x, -p.y);
    if (ang < 0.0) ang += 2.0 * PI;
    float fi = floor(ang / SECTOR);
    int i = int(fi);
    float aLocal = ang - (fi + 0.5) * SECTOR;
    float alBorde = d * sin(SECTOR * 0.5 - abs(aLocal));

    vec3 col = vec3(0.0);
    float a = 0.0;
    float hastaAro = 1.0 - smoothstep(T_ARO, T_ARO + 0.12, t);

    // ---- la pieza
    float w = encogida(i, t);
    float rIn = mix(HUECO, ARO_RADIO - ARO_ANCHO * 0.5, w), rOut = mix(RADIO, ARO_RADIO + ARO_ANCHO * 0.5, w);
    float franja = mix(SEPARACION, ARO_RAYITA, w);
    float dentro = clamp((alBorde - franja * 0.5) / px + 0.5, 0.0, 1.0) * clamp((rOut - d) / px + 0.5, 0.0, 1.0)
            * clamp((d - rIn) / px + 0.5, 0.0, 1.0);
    if (dentro > 0.0 && hastaAro > 0.0) {
        // el agua de la pieza en su lugar de siempre (apretada al encogerse)
        float u = clamp((d - rIn) / max(rOut - rIn, 1e-3), 0.0, 1.0);
        float dOrig = HUECO + u * (RADIO - HUECO);
        vec2 pw = (p / max(d, 1e-4)) * dOrig;
        float sale = T_SALE + demora(i);
        float uCierre = clamp((t - (sale - OLA_ANTES)) / (OLA_ANTES + T_PIEZA), 0.0, 1.0);
        float vuelta = 2.0 * PI * t / PERIODO;
        float m = (1.0 + RESPIRA * sin(vuelta)) * (1.0 + 0.7 * uCierre * uCierre);
        vec2 X = pw / m * 0.21;
        vec2 torcer = vec2(fbm(X + 0.06 * vec2(cos(vuelta), sin(vuelta))), fbm(X + vec2(5.2, 1.7) + 0.06 * vec2(-sin(vuelta), cos(vuelta))));
        float onda = fbm(X * 1.6 + 1.2 * torcer + vec2(0.0, t * 0.05));
        float cresta = pow(1.0 - abs(2.0 * onda - 1.0), 3.0);
        float mancha = pow(clamp((fbm(pw * 0.08 + vec2(3.1, 7.3)) - 0.45) / 0.3, 0.0, 1.0), 2.0);
        vec3 c0 = colorPieza(i);
        vec3 agua = c0 * (0.5 + 0.3 * onda + 0.3 * mancha);
        agua += (min(c0 * 1.4 + 60.0 / 255.0, vec3(1.0)) - agua) * 0.3 * cresta;
        // la ola brillante de la pieza (cada una en su lugar), que corre hacia el pilar y al final se acelera
        float lugar = hash(vec2(float(i), 3.0));
        float fase = lugar + t / PERIODO + CIERRE_ANILLOS * uCierre * uCierre;
        float anillo = pow(0.5 + 0.5 * cos(2.0 * PI * (dOrig / ANILLO + fase)), 14.0);
        float luz = LUZ_AGUA + (1.0 - LUZ_AGUA) * min(1.0, 3.0 * uCierre);
        float brillo = clamp(luz * (cresta * cresta + 1.1 * anillo * (0.4 + cresta)), 0.0, 1.0);
        agua += (min(c0 * 1.25 + 125.0 / 255.0, vec3(1.0)) - agua) * brillo;
        float alto = max(cresta, brillo);
        // al encogerse, las ondas se apagan y queda lisa (su pedazo del aro)
        float lisa = smoothstep(0.0, ARO_SIN_OLAS, w);
        vec3 c = mix(agua, c0 * ARO_LUZ, lisa);
        float difu = smoothstep(0.0, 1.0, clamp((dOrig - HUECO) / DIFUMINADO, 0.0, 1.0));
        difu = mix(difu, 1.0, lisa);
        float al = mix(PIEZA_ALFA + (0.85 - PIEZA_ALFA) * alto * alto, PIEZA_ALFA * 1.4, lisa) * difu * dentro * hastaAro;
        col = c * al;
        a = al;
    }

    // ---- el aro de afuera (crece con el piso; cada pedazo se apaga con su pieza, mezclando en las uniones)
    if (d > RADIO - 0.3 && d < RADIO + AFUERA_ANCHO + 0.3) {
        float f = ang / SECTOR - 0.5;
        float k0 = floor(f), kk = f - k0;
        float cruce = clamp((kk - 0.5) * (d * SECTOR) / 0.5 + 0.5, 0.0, 1.0);
        float wA = mix(encogida(int(mod(k0, 8.0)), t), encogida(int(mod(k0 + 1.0, 8.0)), t), cruce);
        float x = (d - RADIO) / AFUERA_ANCHO;
        float banda = smoothstep(-0.12, 0.08, x) * pow(clamp(1.0 - x, 0.0, 1.0), 1.6);
        vec3 c = min(arcoiris(ang + AFUERA_CORRE * SECTOR) * 1.35 + 0.12, vec3(1.0));
        float al = AFUERA_ALFA * banda * (1.0 - wA);
        col = col * (1.0 - al) + c * al;
        a = a + al * (1.0 - a);
    }

    // ---- el aro grande: el de afuera, que sale del chico y se agranda hasta el borde mientras se apaga
    if (t >= T_ARO) {
        float x = clamp((t - T_ARO) / T_EXPANDE, 0.0, 1.0);
        float r = mix(ARO_RADIO, ARO_GRANDE_MAX, 1.0 - pow(1.0 - x, 2.2));
        float y = (d - r) / AFUERA_ANCHO;
        float banda = smoothstep(-0.25, 0.05, y) * pow(clamp(1.0 - y, 0.0, 1.0), 1.6);
        // (fino y brillante, con la orilla de adentro más clara)
        float orilla = exp(-pow(max(y, 0.0) * 6.0, 2.0));
        vec3 c = min(arcoiris(ang + AFUERA_CORRE * SECTOR) * 1.6 + 0.2 + 0.35 * orilla, vec3(1.0));
        float al = 0.92 * banda * pow(1.0 - x, 1.1) * smoothstep(0.0, 0.08, x + 0.02);
        col = col * (1.0 - al) + c * al;
        a = a + al * (1.0 - a);
    }
    if (a <= 0.002) discard;
    // en el Vacío Cósmico (sólo quien entrega): el piso es un tinte pensado para verse sobre el terreno y sobre el
    // espacio negro casi no se veía; en el server se ve vivo, como agua de galaxia: más opaco y más brillante
    float vac = Tiempo.z;
    if (vac > 0.0) {
        float a2 = min(1.0, a * 2.2);
        col *= mix(1.0, 1.2 * a2 / a, vac);
        a = mix(a, a2, vac);
    }
    fragColor = vec4(col, a);
}
