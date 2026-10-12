#version 150

// El cielo de Reviil (client/CieloRender), como en el stream: una bola de nubes alrededor del jugador (cielo,
// horizonte y vacío) que se abre desde un punto (hacia Polo: el punto alto sobre el 0,0) como una mancha de
// tinta: negra, de borde lobulado y muy borroso, con jirones carmesí que van girando en espiral alrededor del
// centro. Hacia el centro domina el negro; hacia el horizonte, el rojo. Encima, la grieta: un rayo que se abre en
// una grieta morada con estrellas por la que se asoma Reviil (Sampler0).
// Todo en direcciones (la bola está "en el infinito"): el ruido es 3D sobre la dirección, sin costuras.

uniform sampler2D Sampler0;     // la cabeza 3D de Reviil dibujada en una imagen (client/CabezaReviil)
uniform sampler2D Sampler1;     // la textura de ruido (128 x 128, se repite)

uniform vec3 Camara;            // posición de la cámara (mundo)
uniform vec3 Polo;              // hacia dónde está el centro del remolino (punto del mundo)
uniform vec4 Cielo;             // x = frente (rad), y = tiempo (s), z = opacidad, w = segundos desde que empezó
uniform vec4 Forma;             // x = hacia dónde se alarga el óvalo, y = hacia dónde está lo gordo del huevo (rad),
                                // z = la animación de las nubes (0..3)
uniform vec4 Grieta;            // x = largo del rayo (0..1), y = abertura (0..1), z = opacidad, w = ángulo
uniform vec4 GrietaExtra;       // x = semilla, y = ramas (0..1), z = medio largo de la grieta (rad), w = s desde que empezó
uniform vec4 Pulso;             // x = destello (0..1), y = radio del anillo (rad), z = su opacidad, w = partículas
uniform vec4 Latido;            // el cabezazo de Reviil: x = s desde el último (< 0: no hay), y = cuál

in vec3 direccion;

out vec4 fragColor;

const float PI = 3.1415927;

// ------------------------------------------------------------------ ruido

// (hash13 de Dave Hoskins: sin rayas aunque una coordenada no cambie)
float azar3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

// ruido 3D con una sola lectura de la textura de ruido (Sampler1, tools/cielo/ruido.py): G es la capa z y R la capa
// z+1 (G está corrido 37, 17: lo de la capa z+1 en un punto es lo de la capa z en el punto corrido); el filtro lineal
// de la tarjeta mezcla en x e y. Mucho más barato que 8 azares.
float ruido3(vec3 x) {
    vec3 i = floor(x), f = fract(x);
    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);     // suave también en la pendiente: la luz no saca rayas
    vec2 uv = i.xy + vec2(37.0, 17.0) * i.z + f.xy;
    vec2 gr = textureLod(Sampler1, (uv + 0.5) / 128.0, 0.0).yx;
    return mix(gr.x, gr.y, f.z);
}

// cada octava va girada (una rotación sin ejes en común con el mundo): si no, la cuadrícula del ruido queda
// alineada con los ejes y, mirando al horizonte, la luz de las nubes la marca como rayas verticales
const mat3 GIRO = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

float fbm3(vec3 p, int octavas) {
    float s = 0.0, a = 0.5, total = 0.0;
    p = GIRO * p;
    for (int i = 0; i < 5; i++) {
        if (i >= octavas) break;
        s += a * ruido3(p);
        total += a;
        p = GIRO * p * 2.03 + vec3(3.1, 1.7, 5.3);
        a *= 0.5;
    }
    return s / total;
}

// bultos redondos separados por pliegues (como coliflor): lo que hace que el humo se vea como nubes
float bultos(vec3 p, int octavas) {
    float s = 0.0, a = 0.5, total = 0.0;
    p = GIRO * p;
    for (int i = 0; i < 4; i++) {
        if (i >= octavas) break;
        float r = 2.0 * ruido3(p) - 1.0;
        s += a * sqrt(r * r + 0.09);                    // el pliegue redondeado: bultos esponjosos, no rayas
        total += a;
        p = GIRO * p * 2.07 + vec3(1.3, 4.1, 2.9);
        a *= 0.5;
    }
    return s / total;
}

float azar(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float azar1(float x) {                  // (hash11 de Dave Hoskins: sin(x) grande daba saltos en el rayo)
    x = fract(x * 0.1031);
    x *= x + 33.33;
    x *= x + x;
    return fract(x);
}

float ruido(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(azar(i), azar(i + vec2(1.0, 0.0)), f.x), mix(azar(i + vec2(0.0, 1.0)), azar(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float s = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        s += a * ruido(p);
        p = p * 2.03 + vec2(17.1, 5.3);
        a *= 0.5;
    }
    return s / 0.9375;
}

float ruido1(float x) {
    float i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(azar1(i), azar1(i + 1.0), f);
}

float fbm1(float x) {
    float s = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        s += a * ruido1(x);
        x = x * 2.13 + 3.7;
        a *= 0.5;
    }
    return s / 0.96875;
}

vec2 girar(vec2 p, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// gira v alrededor del eje k (unitario)
vec3 girarEje(vec3 v, vec3 k, float a) {
    float c = cos(a), s = sin(a);
    return v * c + cross(k, v) * s + k * dot(k, v) * (1.0 - c);
}

// ------------------------------------------------------------------ la bola de nubes

// La espiral en la dirección dr: 0..1 (0.5 = sin brazo). Dos espirales parejas entrelazadas (cada vuelta separada lo
// mismo) que dan 2 vueltas cada una desde el centro hasta el horizonte (lo pidió el usuario, muy importante: "las nubes
// dan 4 vueltas antes de bajar del horizonte... son 2 espirales, las 2 dan 2 vueltas"): del centro al horizonte se
// cruzan 4 franjas, una cada π/8 rad (22.5°). Cerca del centro se deshacen (el centro es una zona oscura y borrosa,
// sin gancho); hacia el vacío (justo abajo) se pierden para que no se junten en un punto.
const float ESPIRALES = 2.0, VUELTAS = 2.0;
float brazos(vec3 dr, vec3 u, vec3 v, float rn, float th, float desfase, float tuerce) {
    float phi = atan(dot(dr, v), dot(dr, u));
    // cada brazo da VUELTAS vueltas en π/2 rad: φ = 4·VUELTAS·th
    float b = 0.5 + 0.5 * cos(ESPIRALES * (phi - 4.0 * VUELTAS * th) + desfase + tuerce);
    return mix(0.5, b, smoothstep(0.06, 0.32, rn) * (1.0 - smoothstep(2.3, 2.9, th)));
}

// El cielo de Reviil. d: dirección; polo, (u, v): el centro y dos ejes perpendiculares; th: ángulo desde el centro;
// frente: hasta dónde llegó la apertura (rad); t: tiempo (s); efecto: segundos desde que empezó a abrirse. Devuelve
// color y opacidad. El ruido es 3D sobre la esfera misma (aplanada, bajo el horizonte se estiraba y salían rayas).
// El cielo es rojo muy oscuro con una nebulosa tenue, y la espiral la forman nubes esponjosas iluminadas desde atrás
// (orillas que brillan), con un halo suave alrededor de cada nube y jirones finos a lo largo de la espiral (hecho a
// gusto, "que queden bonitas", sin seguir la referencia). Sin bultos negros.
// Las nubes no giran ni entran al centro (lo pidió el usuario / el flujo hacia el centro, con el tiempo, estiraba el
// humo en rayas): sólo cambian de forma despacio. Todo lo que se mueve depende sólo del tiempo, que siempre avanza:
// al cerrarse se encoge al centro, pero las nubes siguen hacia adelante (si dependiera de cuánto está abierto, se
// veía como un video en reversa).
// El borde: un huevo/óvalo ondulado, con un degradado suave del mismo ancho en toda la orilla; el cielo oscuro y las
// nubes se abren juntos, a la misma velocidad en todo el cielo.
vec4 nubes(vec3 d, vec3 polo, vec3 u, vec3 v, float th, float frente, float t, float efecto) {
    const float ESCALA = 0.75;                                       // tamaño de las nubes
    const float SEMILLA = 7.31;
    float rn = th / ESCALA;
    float centro = 1.0 - smoothstep(0.02, 0.3, rn);                  // el centro: bruma oscura, sin gancho
    // la animación (Forma.z, /reviil ajustar cielo_animacion): 0 = quietas, 1 = olas de luz, 2 = relámpagos,
    // 3 = las dos. Las nubes nunca giran (lo pidió el usuario): sólo cambian de forma despacio, y lo que se mueve
    // es la luz
    int anim = int(Forma.z + 0.5);
    bool olas = anim == 1 || anim == 3, rayos = anim >= 2;
    // cuánto cambiaron de forma las nubes: sólo con el tiempo, siempre hacia adelante. Con los segundos desde que se
    // abrió (chicos), no con la hora del juego: con miles de segundos el ruido perdía precisión y salía rayado
    float cambia = (anim == 0 ? 0.012 : 0.03) * efecto;              // quietas / hierven despacio

    // el borde: un círculo deforme, como un huevo u óvalo (lo pidió el usuario): alargado hacia un lado y más gordo de
    // una punta (cambia cada vez que se abre: Forma), con ondas chicas que cambian despacio. Mientras es chico todo va
    // a su medida; ya grande, el huevo hasta ±0.31 rad y las ondas ±0.15 rad. El degradado tiene el mismo ancho en
    // toda la orilla.
    float phi = atan(dot(d, v), dot(d, u));
    float huevo = 0.17 * cos(2.0 * (phi - Forma.x)) + 0.09 * cos(phi - Forma.y);
    float medida = 0.9 * tanh(frente / 0.9) + 0.04;
    float lob = fbm3((d - polo) / medida * 0.8 + vec3(SEMILLA, 0.0, efecto * 0.1), 3);
    float hasta = frente + min(frente, 1.2) * huevo + 0.3 * min(frente, 0.5) * clamp((lob - 0.5) * 3.0, -1.0, 1.0);
    float ancho = min(0.5 * frente + 0.015, 0.22);                   // el degradado (más difuminado: lo pidió el usuario)
    if (th > hasta + ancho) return vec4(0.0);                         // todavía no llega aquí
    // el cielo oscuro y las nubes se abren con el mismo borde; las nubes aparecen recién donde el cielo ya está casi
    // oscuro (si no, por ser claras se veían antes de que llegara el borde)
    float fondoA = 1.0 - smoothstep(hasta - ancho, hasta + ancho, th);
    float llega = 1.0 - smoothstep(hasta - 2.0 * ancho, hasta - 0.4 * ancho, th);

    vec3 q0 = (d - polo) / ESCALA;
    // la deformación (formas orgánicas, nada de compás); casi quieta
    vec3 w = vec3(fbm3(q0 * 0.8 + vec3(0.0, SEMILLA, cambia), 2), fbm3(q0 * 0.8 + vec3(5.2, 1.3, cambia + 8.3), 2),
                  fbm3(q0 * 0.8 + vec3(2.7, 9.1, cambia + 4.1), 2)) - 0.5;
    // hacia afuera del centro, sobre la bola: de ahí viene la luz (en el centro no tiene dirección: luz pareja)
    vec3 radial = d - polo * dot(d, polo);
    radial /= max(length(radial), 1e-4);
    float conLuz = smoothstep(0.08, 0.5, th);

    // ---- la espiral: 2 espirales de 2 vueltas (ver brazos) que serpentean un poco
    float ondA = fbm3(q0 * 0.9 + vec3(11.0, 3.0, 0.0), 3) - 0.5;
    float thw = th + 0.05 * ondA;
    float a = phi - 4.0 * VUELTAS * thw;                              // de un brazo al otro (a lo largo, no cambia)
    float perfil = 0.5 + 0.5 * cos(ESPIRALES * a + 1.6 * w.y);        // 1 en el medio de cada brazo, 0 entre brazos
    float vida = smoothstep(0.06, 0.32, rn) * (1.0 - smoothstep(2.3, 2.9, th));     // ni en el centro ni en el vacío
    // hacia el centro del cielo las nubes se DIFUMINAN (lo pidió el usuario: si no, se nota mucho la espiral): hasta
    // ~50° del centro la espiral se va perdiendo y las nubes se vuelven una bruma suave, más transparente
    float difuso = 1.0 - smoothstep(0.2, 0.85, th);
    float perfilD = mix(perfil, 0.5, 0.85 * difuso);

    // ---- el fondo: rojo muy oscuro (casi negro en el centro) con una nebulosa tenue, granate y un poco violeta
    float neb = fbm3(q0 * 0.7 + vec3(3.0, 1.0, 0.5 * cambia), 3);
    vec3 fondo = mix(vec3(0.012, 0.0, 0.004), vec3(0.045, 0.002, 0.01), smoothstep(0.05, 0.5, th)) * (0.7 + 0.7 * neb);
    fondo += vec3(0.02, 0.0, 0.016) * smoothstep(0.55, 0.8, neb);
    // el borde de la apertura, rojizo oscuro (lo pidió el usuario)
    fondo = mix(fondo, vec3(0.11, 0.006, 0.012), smoothstep(hasta - 2.0 * ancho, hasta, th) * 0.85);

    // ---- jirones: hilos finos de nube estirados a lo largo de la espiral, en la orilla de los brazos (se nota el
    // remolino sin que se mueva). A lo largo del brazo cambian despacio; de través, rápido: hilos
    float hilo = fbm3(vec3(cos(a) * 4.0, sin(a) * 4.0, th * 3.0 + 1.0 + cambia), 3);
    float orillaBrazo = smoothstep(0.2, 0.5, perfil) * (1.0 - smoothstep(0.62, 0.92, perfil));
    float cortaHilo = smoothstep(0.35, 0.65, fbm3(q0 * 2.5 + vec3(9.0, 4.0, 2.0), 2));   // cortados, no líneas
    float jirones = smoothstep(0.6, 0.8, hilo) * cortaHilo * orillaBrazo * vida * (1.0 - difuso) * llega;

    // ---- las nubes: cuerpos esponjosos (bultos grandes, medianos y chicos) en los brazos. Iluminadas desde atrás y
    // desde afuera: lo grueso queda oscuro, las orillas finas brillan (como nubes con fuego detrás) y la cara que mira
    // hacia afuera se aclara
    // (más grandes, lo pidió el usuario: bultos ~1.5 veces más grandes y brazos más gruesos)
    vec3 q2 = q0 * 1.6 + 1.3 * w;
    float forma = fbm3(q2 * 1.05 + vec3(0.0, 7.0, 0.0), 4);
    float grumo = fbm3(q2 * 4.2 + vec3(3.0, 1.0, 5.0), 3);
    // al llegar el borde las nubes se FORMAN (crecen desde su centro), no sólo se destapan
    float nace = 0.7 * (1.0 - llega) + 0.12 * difuso;
    float pGrumo = 0.45 * (1.0 - 0.7 * difuso);                       // en el centro, sin bultitos: más suaves
    float D = mix(-0.4, perfilD * 1.1 + (forma - 0.5) * 1.25 + (grumo - 0.5) * pGrumo - 0.65, vida) - 0.5 * centro - nace;
    // alrededor de cada nube, un halo suave de su luz (con la forma de la nube, no una espiral lisa aparte: el
    // usuario vio "un espiral plano atrás de las nubes" cuando el brillo seguía la espiral perfecta)
    float Dsuave = mix(-0.4, perfilD * 1.1 + (forma - 0.5) * 1.25 - 0.65, vida) - 0.5 * centro - nace;
    float halo = smoothstep(-0.35, 0.15, Dsuave) * (0.6 + 0.6 * neb) * llega;
    // la orilla de las nubes, difuminada (lo pidió el usuario); en el centro, mucho más, y la nube más transparente
    float blando = 0.32 + 0.4 * difuso;
    float cubre = smoothstep(-0.06 - 0.12 * difuso, blando, D) * (1.0 - 0.4 * difuso) * llega;
    float grueso = smoothstep(0.03, 0.85 + 0.3 * difuso, D);
    vec3 qf = q2 + radial * (0.08 * conLuz);                           // un poco hacia la luz
    float perfilF = mix(0.5 + 0.5 * cos(ESPIRALES * (phi - 4.0 * VUELTAS * (thw + 0.03 * conLuz)) + 1.6 * w.y), 0.5, 0.85 * difuso);
    float Df = mix(-0.4, perfilF * 1.1 + (fbm3(qf * 1.05 + vec3(0.0, 7.0, 0.0), 4) - 0.5) * 1.25 + (grumo - 0.5) * pGrumo - 0.65, vida)
            - 0.5 * centro - nace;
    float cara = mix(0.5, clamp(0.5 + (D - Df) * 3.5, 0.0, 1.0), conLuz); // la cara que mira hacia afuera
    vec3 cuerpo = mix(vec3(0.07, 0.0, 0.02), vec3(0.42, 0.014, 0.032), cara);   // sombra granate-violeta -> carmesí
    cuerpo *= 1.0 - 0.35 * grueso;                                      // el corazón de la nube, más oscuro
    float filo = (1.0 - grueso) * smoothstep(-0.04, blando, D);         // lo fino deja pasar la luz de atrás
    vec3 cNube = cuerpo + vec3(0.95, 0.22, 0.08) * filo * filo * (0.15 + 0.85 * cara) * 0.6;
    cNube *= 0.8 + 0.4 * grumo;                                         // los bultitos

    // ---- la animación: se mueve la luz, no las nubes
    float ola = 0.0, destello = 0.0;
    if (olas) {
        // olas de luz que bajan por los brazos hacia el centro, como si el remolino se tragara la luz: un frente en
        // espiral (de una sola vuelta y torcido al revés que las nubes: no es un anillo que se encoge) y deformado por
        // la nebulosa; a cada punto le llega una cada ~7 s, y avanza 0.1 rad/s
        float fase = 9.0 * th + phi + 5.0 * (neb - 0.5) + 0.9 * efecto;
        ola = pow(0.5 + 0.5 * cos(fase), 14.0) * (0.3 + 0.8 * neb) * vida * llega;
    }
    if (rayos) {
        // relámpagos dentro de las nubes: el cielo en celdas de ~0.55 rad; cada celda, cada 6-14 s, a veces se
        // enciende desde adentro (un destello y uno o dos rebotes, ~0.6 s), apagándose antes de su orilla
        vec3 qc = q0 * 1.4;
        vec3 celda = floor(qc);
        float periodo = 6.0 + 8.0 * azar3(celda + 11.0);
        float x = efecto + azar3(celda + 23.0) * periodo;
        float vez = floor(x / periodo);
        x -= vez * periodo;
        if (x < 0.8 && azar3(celda + vez * 7.13 + 1.7) < 0.5) {
            vec3 f = qc - celda;
            vec3 sitio = 0.25 + 0.5 * vec3(azar3(celda + 3.1), azar3(celda + 5.3), azar3(celda + 8.9));
            // dónde cae, sobre el cielo (la celda es 3D: su punto puede quedar lejos de la superficie)
            vec3 dc = normalize(polo + (celda + sitio) * (ESCALA / 1.4));
            vec3 borde = min(f, 1.0 - f);
            float golpes = exp(-x / 0.08) + 0.75 * step(0.2, x) * exp(-(x - 0.2) / 0.06)
                    + 0.5 * step(0.42, x) * exp(-(x - 0.42) / 0.14);
            destello = golpes * exp(-dot(d - dc, d - dc) / 0.02)
                    * smoothstep(0.0, 0.18, min(borde.x, min(borde.y, borde.z))) * llega;
        }
    }
    // la luz entra por lo fino de las nubes (la orilla y lo que mira afuera); lo grueso se enciende poco
    vec3 brillo = vec3(1.0, 0.3, 0.1) * ola * (0.15 * cara + 1.2 * filo + 0.04)
                + vec3(1.0, 0.42, 0.22) * destello * (0.5 + 1.1 * filo + 0.35 * (1.0 - grueso));
    cNube += brillo;

    // los jirones, sólo pegados a las nubes (hilos que se desprenden de ellas, no líneas de espiral sueltas)
    jirones *= smoothstep(-0.25, 0.05, Dsuave);
    vec3 col = fondo + vec3(0.13, 0.004, 0.016) * halo * (1.0 + 2.0 * ola) + vec3(0.5, 0.08, 0.04) * halo * destello;
    col = mix(col, vec3(0.45, 0.02, 0.035) * (1.0 + 1.5 * ola + 2.0 * destello), jirones * 0.4);
    col = mix(col, cNube, cubre);
    return vec4(col, fondoA);
}

// ------------------------------------------------------------------ rayo y grieta (coordenadas planas alrededor del centro, en rad)

// la cara: la cabeza 3D de Reviil (el modelo del jefe de ModMinds, sólo la cabeza: client/CabezaReviil) dibujada cada
// cuadro en una imagen de 1024 x 768, sobre transparente, con su animación (se mece, los ojos giran, el cabezazo)
const float ASPECTO_CARA = 1024.0 / 768.0;       // ancho / alto de la imagen
const float CARA_ANCHO = 1.05;                   // rad: el ancho de la imagen (la cabeza ~0.75 rad: grande, como en el stream)
const float CARA_V = 0.5;                        // la altura de la imagen que va en el medio de la grieta (ojos y boca)
const float BULTO_ANCHO = 0.385;                 // rad: medio ancho del ensanche alrededor de la cara
const float MEDIA = 0.09;                        // rad: medio alto de la franja
const float BULTO = 0.035;                       // rad: cuánto se ensancha alrededor de la cara
// El CABEZAZO de Reviil (/reviil cabezazo), le = s desde que se pidió. Cuánto se agranda la cara (1 = normal): se echa
// para atrás, se lanza, GOLPEA a los 0.2 s y vuelve con un rebote
float escalaGolpe(float le) {
    if (le < 0.0) return 1.0;
    if (le < 0.14) return 1.0 - 0.07 * smoothstep(0.0, 0.14, le);
    if (le < 0.2) {
        float k = (le - 0.14) / 0.06;
        return 0.93 + 0.39 * k * k;
    }
    float t = le - 0.2;
    return 1.0 + 0.32 * exp(-t / 0.13) * cos(t * 13.0);
}

// lo fuerte del golpe (1 justo al golpear y se apaga): el glitch, el neón que se enciende
float impacto(float le) {
    return le >= 0.2 ? exp(-(le - 0.2) / 0.15) : 0.0;
}

// el temblor del golpe: TODO el efecto (nubes, grieta, cara, anillos, pedazos; el usuario: "que el temblor abarque todo
// el efecto"), un poco más largo que el impacto
float temblor(float le) {
    return le >= 0.2 && le < 1.8 ? exp(-(le - 0.2) / 0.28) : 0.0;
}

// la onda del golpe: su radio (rad, sale rápido y frena) y cuánto se ve
float radioOnda(float le) {
    return 0.15 + 1.5 * (1.0 - exp(-max(le - 0.22, 0.0) / 0.3));
}
float vidaOnda(float le) {
    return smoothstep(0.2, 0.24, le) * (1.0 - smoothstep(0.45, 0.7, le));
}

// la línea del rayo: curva suave + quiebres chicos (como un rayo: tramos casi rectos con esquinas). xn = x / medio
// largo del rayo (-1..1); L = medio largo de la grieta (todo va a su medida)
float linea(float xn, float s, float L) {
    // (cada quiebre es un pico que vale 0 en las puntas del tramo: si no, entre tramos el rayo daba un salto y su
    // brillo salía con rayas)
    float tramo = floor(xn * 14.0);
    float quiebre = 0.5 - abs(fract(xn * 14.0) - 0.5);
    return L * (0.09 * (fbm1(xn * 2.2 + s) - 0.5) + 0.03 * (fbm1(xn * 10.0 + s * 3.1) - 0.5)
            + 0.035 * quiebre * (azar1(tramo + s * 1.7) - 0.5));
}

// una capa de estrellas: en celdas de 1/escala rad, en `cuantas` de ellas (0..1) una estrella de radio `tam` (de la
// celda) en un lugar al azar, de brillo al azar, que titila
float estrellas(vec2 q, float T, float escala, float cuantas, float tam, float semilla) {
    vec2 g = q * escala + semilla;
    vec2 id = floor(g);
    float h = azar(id + semilla);
    if (h < 1.0 - cuantas) return 0.0;
    vec2 sitio = vec2(azar(id + 3.1), azar(id + 7.7)) - 0.5;
    float dd = length(fract(g) - 0.5 - sitio * 0.6);
    float titila = 0.6 + 0.4 * sin(T * (1.5 + h * 5.0) + h * 40.0);
    return (smoothstep(tam, 0.0, dd) + 0.12 * smoothstep(2.5 * tam, 0.0, dd)) * titila * (0.35 + 0.65 * azar(id + 11.3));
}

// el medio alto de la grieta a lo largo (u = 0 en el medio, 1 en las puntas): lleno en el medio y en PICO en las
// puntas (las orillas llegan en ángulo, no redondas)
float pico(float u) {
    u = clamp(u, 0.0, 1.0);
    return (1.0 - u * u) * (1.0 - 0.25 * u * u);
}

// lo que se ve por la grieta: un espacio violeta profundo, con nebulosa, hebras magenta y MUCHAS estrellas que titilan
// (el usuario: "el fondo de espacio tiene más estrellas", como en el stream): muchas chiquitas y tenues, otras medianas
// y unas pocas grandes
vec3 espacio(vec2 q, float T) {
    float n = fbm(q * 9.0 + vec2(T * 0.03, 3.0));
    float n2 = fbm(q * 24.0 + vec2(7.0, T * 0.05));
    vec3 c = mix(vec3(0.05, 0.015, 0.12), vec3(0.3, 0.11, 0.5), smoothstep(0.3, 0.75, n));
    c += vec3(0.25, 0.05, 0.3) * smoothstep(0.55, 0.8, n2) * n;
    float e = 0.75 * estrellas(q, T, 170.0, 0.32, 0.2, 0.0)
            + estrellas(q, T, 90.0, 0.2, 0.15, 5.3)
            + 1.4 * estrellas(q, T, 40.0, 0.08, 0.1, 9.1);
    c += vec3(1.0, 0.9, 0.97) * e;
    return c;
}

// El rayo y la grieta (fases 3 y 4). El rayo es una línea de neón rojo (núcleo claro, halo rojo) con ramas que van
// creciendo; la grieta se abre a lo largo de él: el mismo trazo se parte en sus dos orillas. La grieta es una ELIPSE
// larga de orillas algo rasgadas (más ancha en el medio, se afina pareja hasta las puntas), un poco más
// corta (grieta_grados en la config, 100° por defecto; en el stream cruzaba todo el cielo), y se ensancha alrededor de
// la CARA de Reviil: grande, con sus colores, que se sale un poco de la grieta (tapa el neón). Se abre primero donde
// está la cara y después a lo largo; adentro, el espacio; de las orillas se desprenden chispas, y la grieta abierta
// enciende de rojo lo de alrededor.
vec4 grieta(vec2 p) {
    float s = GrietaExtra.x;
    float L = GrietaExtra.z;                                      // medio largo de la grieta (rad)
    // el rayo llega justo a sus puntas (si pasaba más allá, quedaban líneas que no se abrían: lo vio el usuario)
    float LB = L;
    float T = GrietaExtra.w;                                      // s desde que empezó (chico: sin rayas)
    vec2 q = girar(p, -Grieta.w);
    float imp = impacto(Latido.x);                                // (con el cabezazo tiembla todo: main)
    float x = q.x;
    if (abs(x) > LB + 0.4 || abs(q.y) > 0.55) return vec4(0.0);  // lejos de todo (también de las ramas)
    float largo = LB * Grieta.x;
    float abre = Grieta.y;
    float xn = x / LB;
    float dy = q.y - linea(xn, s, L);
    float u = abs(x) / L;
    // la grieta: larga, más ancha en el medio y se afina pareja hasta las puntas (eligió el usuario; recta con la punta
    // de golpe "parecía un rectángulo con triángulos"), y las puntas EN PICO (el usuario: "en los costados es redondo
    // cuando debería ser en forma de pico"; con una elipse, sqrt(1 - u²), se cerraban redondas): sus orillas se juntan
    // en ángulo. Orillas algo rasgadas (distintas arriba y abajo; sin dientes más chicos: los brillos del borde los
    // copiaban como rayitas); abierta hasta las puntas (no quedan líneas sin abrir) y se ensancha alrededor de la cara
    float lado = dy >= 0.0 ? 1.0 : -1.0;
    float rasgo = 0.12 * (fbm1(xn * 7.0 + s * 5.0 + lado * 13.0) - 0.5);
    float ab = smoothstep(0.0, 1.0, abre * 1.6 - u * 0.6);       // primero donde está la cara
    float kc = min(1.0, 1.1 * L / 0.7);                           // la cara, más chica si la grieta es muy corta
    float cw = CARA_ANCHO * kc;
    float fw = BULTO_ANCHO * kc;
    float bulto = BULTO * kc * pow(max(1.0 - x * x / (fw * fw), 0.0), 1.5);
    float media = (MEDIA * 1.15 * pico(u) * (1.0 + 2.0 * rasgo) + bulto) * ab * (1.0 + 0.18 * imp);
    float e = abs(dy) - media;                                    // < 0: adentro
    float alcanza = 1.0 - smoothstep(largo - 0.02, largo, abs(x));     // la punta del rayo se afina
    vec3 col = vec3(0.0);
    float alfa = 0.0;
    if (e < 0.0) {
        vec3 c = espacio(q, T);
        // las orillas, por dentro, encendidas
        c += vec3(1.0, 0.22, 0.14) * exp(e / (0.15 * MEDIA + 0.003)) * 0.55;
        col = c;
        alfa = 1.0;
    }
    // Reviil: su cabeza 3D (la imagen que dibuja CabezaReviil cada cuadro, con su animación: se mece, los ojos en espiral
    // giran, las alas y los huesos se balancean), grande, en el ensanche; se sale un poco de la grieta (el neón de la
    // orilla pasa por delante), se acerca un poco mientras se abre. El CABEZAZO (/reviil cabezazo) lo anima la cabeza en
    // 3D (se echa para atrás y se lanza hacia la cámara); aquí: sale de la grieta por encima del neón y, al golpear,
    // glitch (franjas corridas y colores separados)
    float le = Latido.x;
    float salida = clamp((escalaGolpe(le) - 1.0) / 0.3, 0.0, 1.0);    // cuánto sale de la grieta
    float esc = cw * mix(0.9, 1.0, smoothstep(0.3, 1.0, abre));
    float yc = linea(0.0, s, L);
    vec2 uv = vec2(x / esc + 0.5, CARA_V - (q.y - yc) * ASPECTO_CARA / esc);
    float margen = 0.018 * ab;
    float caraT = 0.0;
    vec4 f = vec4(0.0);
    bool enCara = abs(x) < fw && uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0;
    if (enCara && e < margen + 0.07 * salida) {
        float tira = 0.0;
        if (imp > 0.05) {
            float hh = azar1(floor(uv.y * 26.0) * 1.7 + floor(le * 24.0) * 9.1);
            tira = hh > 0.65 ? (hh - 0.82) * 0.18 * imp : 0.0;
        }
        vec2 uvi = vec2(uv.x + tira, 1.0 - uv.y);                  // la imagen tiene el 0 abajo
        f = textureLod(Sampler0, uvi, 0.0);
        if (imp > 0.05) {
            vec2 sep = vec2(0.008 * imp, 0.0);
            f.r = textureLod(Sampler0, uvi + sep, 0.0).r;
            f.b = textureLod(Sampler0, uvi - sep, 0.0).b;
        }
        f.rgb /= max(f.a, 1e-3);                                   // viene sobre transparente (negro)
        caraT = f.a * (1.0 - smoothstep(0.0, max(margen, 1e-4), e));
        col = mix(col, f.rgb, caraT);
        alfa = max(alfa, caraT);
    }
    // el trazo de neón (medido en el stream, mirando arriba: ~0.008 rad de grueso con halo de ~0.032 rad): un hilo
    // casi blanco en el medio, una franja de rojo vivo y el halo rojo hacia afuera
    float fino = 0.55 + 0.45 * alcanza;
    float nucleo = exp(-abs(e) / ((0.0025 + 0.0015 * ab) * fino)) * alcanza * (1.0 + 1.2 * imp);
    float neon = exp(-abs(e) / ((0.008 + 0.005 * ab) * fino * (1.0 + 0.6 * imp))) * alcanza;
    float halo = e > -0.003 ? exp(-max(e, 0.0) / ((0.032 + 0.016 * ab) * (1.0 + imp))) * alcanza : 0.0;
    // las ramas: sólo se calculan cerca de cada una (antes todas en cada píxel: ~2 ms por cuadro)
    float ramas = 0.0, ramasNeon = 0.0, halo2 = 0.0;
    float crece = GrietaExtra.y;                                  // cuánto crecieron (0..1)
    if (crece > 0.0) {
        for (int i = 0; i < 9; i++) {
            float fi = float(i);
            float xb = (azar1(s + fi * 7.3) - 0.5) * 1.5 * LB;
            if (abs(xb) > largo - 0.02) continue;                 // el rayo todavía no llega ahí
            float lb = L * (0.15 + 0.3 * azar1(s + fi * 9.1)) * clamp(crece * 1.4 - azar1(s + fi * 2.3) * 0.4, 0.0, 1.0);
            if (length(q - vec2(xb, 0.0)) > lb + 0.12 * L) continue;
            float hacia = xb >= 0.0 ? 1.0 : -1.0;
            float ang = (xb >= 0.0 ? 0.0 : PI) + hacia * (azar1(s + fi * 3.1) < 0.5 ? -1.0 : 1.0)
                    * (0.45 + 0.5 * azar1(s + fi * 5.7));
            vec2 vv = girar(q - vec2(xb, linea(xb / LB, s, L)), -ang);
            if (vv.x < 0.0 || vv.x > lb + 0.01 || abs(vv.y) > 0.07) continue;
            float dd = abs(vv.y - 0.03 * L * (fbm1(vv.x / L * 16.0 + fi * 13.0) - 0.5) * vv.x / max(lb, 1e-3) * 4.0);
            float punta = clamp(1.0 - vv.x / max(lb, 1e-3), 0.0, 1.0);
            float finas = (0.35 + 0.65 * punta) * smoothstep(0.0, 0.15, punta);
            ramas = max(ramas, exp(-dd / (0.0018 * (0.35 + 0.65 * punta))) * finas);
            ramasNeon = max(ramasNeon, exp(-dd / (0.0055 * (0.35 + 0.65 * punta))) * finas);
            halo2 = max(halo2, exp(-dd / 0.021) * punta * 0.7);
        }
        // con la grieta abierta, las ramas se apagan (queda la grieta)
        ramas *= 1.0 - abre;
        ramasNeon *= 1.0 - abre;
        halo2 *= 1.0 - abre;
    }
    // chispas: pedacitos rojos que se desprenden de las orillas y se alejan despacio, apagándose
    float chispa = 0.0;
    if (ab > 0.0 && e > 0.0 && e < 0.12 && u < 1.0) {
        vec2 g = vec2(x * 45.0, (e - 0.012 * T) * 45.0 + lado * 31.0);
        vec2 id = floor(g);
        float h = azar(id + 5.0);
        if (h > 0.9) {
            vec2 sitio = vec2(azar(id + 1.3), azar(id + 2.9)) - 0.5;
            float dd = length(fract(g) - 0.5 - sitio * 0.5);
            chispa = smoothstep(0.16, 0.0, dd) * exp(-e / 0.05) * ab * (1.0 - u * u)
                    * (0.6 + 0.4 * sin(T * 6.0 + h * 60.0));
        }
    }
    // la grieta abierta enciende de rojo las nubes de alrededor
    float resplandor = exp(-max(e, 0.0) / (0.3 * L)) * ab * (1.0 - smoothstep(0.7, 1.3, u)) * 0.4;
    float luz = max(nucleo, ramas);
    vec3 claro = mix(vec3(1.0, 0.55, 0.5), vec3(1.0, 0.92, 0.75), max(Pulso.x, imp));   // con el destello o el golpe, casi blanco
    vec3 rojo = vec3(0.86, 0.02, 0.03);
    float h = clamp(max(max(halo, halo2) * 0.9, resplandor), 0.0, 1.0);
    col = mix(col, rojo, h * (1.0 - alfa * 0.6) * (1.0 - 0.75 * caraT));   // sobre la cara, poco: que no la ensucie
    col = mix(col, vec3(1.0, 0.45, 0.15), clamp(chispa, 0.0, 1.0));
    neon = max(neon, ramasNeon);
    col = mix(col, vec3(1.0, 0.06, 0.07), clamp(neon, 0.0, 1.0));
    col = mix(col, claro, clamp(luz, 0.0, 1.0));
    alfa = max(alfa, clamp(max(max(h, max(luz, neon)), chispa), 0.0, 1.0));
    if (salida > 0.0 && f.a > 0.0) {
        float sale = f.a * salida * (1.0 - smoothstep(0.0, 0.07 * salida + 1e-4, e));
        col = mix(col, f.rgb, sale);
        alfa = max(alfa, sale);
    }
    return vec4(col, alfa * Grieta.z);
}

// El cabezazo de Reviil (/reviil cabezazo; el usuario mandó un video: RPReplay_Final1791211066): los PEDAZOS de la
// grieta que salen disparados desde la cara hacia todos lados, frenando, girando y titilando, y se apagan en ~1-2.5 s.
// Son del MATERIAL DE LA GRIETA, su neón rojo, no del espacio de adentro (el usuario: "es del material de la grieta no
// del espacio"): rombos de neón rojo vivo con el corazón casi blanco (como el trazo de la orilla) y su brillo rojo
// alrededor. El cielo alrededor del centro va en gajos (2 capas corridas medio gajo): en cada gajo, un pedazo; cada
// píxel mira su gajo y los de al lado (si no, el brillo se cortaba).
// p: coordenadas planas alrededor del centro (rad); le: s desde el cabezazo; n: cuál (otra forma cada vez).
// Devuelve (adentro, el hilo de su orilla, su brillo, su corazón).
vec4 pedazos(vec2 p, float le, float n) {
    float t = le - 0.2;                                            // salen al golpear
    if (t < 0.0 || t > 2.6) return vec4(0.0);
    const float K = 22.0;
    float ang = atan(p.y, p.x) + PI;                               // 0..2π
    float adentro = 0.0, hilo = 0.0, brillo = 0.0, corazon = 0.0;
    for (int capa = 0; capa < 2; capa++) {
        float desf = 0.5 * float(capa);
        float i0 = floor(ang / (2.0 * PI) * K + desf);
        for (int j = -1; j <= 1; j++) {
            float i = i0 + float(j);
            float semilla = mod(i, K) * 3.17 + n * 11.3 + float(capa) * 57.1;
            float h1 = azar1(semilla), h2 = azar1(semilla + 1.7), h3 = azar1(semilla + 4.1), h4 = azar1(semilla + 7.3);
            if (h4 < 0.15) continue;                               // algunos gajos sin pedazo
            float a = (i - desf + 0.2 + 0.6 * h1) * 2.0 * PI / K - PI;
            float rr = 0.07 + 0.2 * h3 + (0.5 + 1.1 * h2) * 0.55 * (1.0 - exp(-t / 0.55));   // salen rápido y frenan
            vec2 dq = girar(p - vec2(cos(a), sin(a)) * rr, -(a + t * (2.0 + 4.0 * h1) * (h2 < 0.5 ? -1.0 : 1.0)));
            float tam = 0.012 + 0.014 * h3;
            float rombo = abs(dq.x) / (tam * 1.6) + abs(dq.y) / tam;   // 1 = su orilla
            // la estela: mientras van rápido dejan un rastro rojo detrás (hacia el centro)
            vec2 dir = vec2(cos(a), sin(a));
            vec2 dr = p - dir * rr;
            float rapido = (0.5 + 1.1 * h2) * exp(-t / 0.55);
            float largoE = 0.12 * rapido;
            float atrasE = -dot(dr, dir);
            float estela = atrasE > 0.0 && atrasE < largoE
                    ? exp(-abs(dot(dr, vec2(-dir.y, dir.x))) / (tam * 0.35)) * (1.0 - atrasE / largoE) : 0.0;
            if (rombo > 3.0 && estela <= 0.0) continue;
            float vida = smoothstep(0.0, 0.06, t) * (1.0 - smoothstep(0.6 + 0.9 * h4, 1.2 + 1.3 * h4, t))
                    * (0.75 + 0.25 * sin(t * 9.0 + h1 * 30.0));
            float ancho = 0.07 / (0.6 + h3);                        // el hilo, más o menos igual de fino en todos
            adentro = max(adentro, (1.0 - smoothstep(1.0 - 2.0 * ancho, 1.0 - ancho, rombo)) * vida);
            hilo = max(hilo, exp(-abs(rombo - 1.0 + ancho) / ancho) * vida);
            brillo = max(brillo, max(exp(-max(rombo - 1.0, 0.0) * 2.3), estela * 0.8) * vida);
            corazon = max(corazon, (1.0 - smoothstep(0.0, 0.6, rombo)) * vida);
        }
    }
    return vec4(adentro, hilo, brillo, corazon);
}

// partículas rojas que flotan (puntitos), sobre la esfera, girando despacio con el cielo
float particulas(vec3 d, vec3 polo, float t) {
    vec3 dr = girarEje(d, polo, -0.04 * t) * 70.0;
    vec3 id = floor(dr);
    float h = azar3(id + 17.0);
    if (h < 0.985) return 0.0;
    vec3 sitio = vec3(azar3(id + 3.1), azar3(id + 7.7), azar3(id + 1.9)) * 0.6 + 0.2;
    float dd = length(fract(dr) - sitio);
    float titila = 0.6 + 0.4 * sin(t * (1.5 + h * 30.0) + h * 70.0);
    return smoothstep(0.12, 0.02, dd) * titila;
}

void main() {
    vec3 d = normalize(direccion);
    vec3 polo = normalize(Polo - Camara);
    // coordenadas alrededor del centro: radianes desde el centro, hacia donde está (sin costura)
    vec3 u = normalize(cross(polo, abs(polo.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 v = cross(polo, u);
    // con el cabezazo tiembla TODO (las nubes, la grieta con la cara, los anillos y los pedazos): se corre la dirección
    float tiembla = temblor(Latido.x);
    if (tiembla > 0.0) {
        float k = Latido.x * 38.0;
        d = normalize(d + 0.03 * tiembla * ((ruido1(k) - 0.5) * 2.0 * u + (ruido1(k + 17.0) - 0.5) * 2.0 * v));
    }
    float th = acos(clamp(dot(d, polo), -1.0, 1.0));
    float t = Cielo.y;

    vec2 plano = vec2(dot(d, u), dot(d, v));
    vec2 p = plano / max(length(plano), 1e-5) * th;

    // la onda del cabezazo empuja las nubes hacia afuera al pasar (se ve deformarse el cielo)
    vec3 dn = d;
    float thn = th;
    if (Latido.x >= 0.2 && Latido.x < 0.75) {
        float onda = exp(-pow((th - radioOnda(Latido.x)) / 0.08, 2.0)) * vidaOnda(Latido.x);
        vec3 radial = d - polo * dot(d, polo);
        dn = normalize(d - radial / max(length(radial), 1e-5) * 0.06 * onda);
        thn = acos(clamp(dot(dn, polo), -1.0, 1.0));
    }
    vec4 r = Cielo.z > 0.0 ? nubes(dn, polo, u, v, thn, Cielo.x, t, Cielo.w) : vec4(0.0);
    vec3 col = r.rgb;
    float alfa = r.a * Cielo.z;
    // los pulsos (fase 3): todo el cielo destella rojo, una chispa naranja en el centro y un anillo de neón que
    // sale del centro y se apaga; después flotan partículas rojas
    if (Pulso.x > 0.0 || Pulso.z > 0.0 || Pulso.w > 0.0) {
        col = mix(col, vec3(0.86, 0.02, 0.02), Pulso.x * 0.8 * alfa);
        col += vec3(1.0, 0.42, 0.16) * exp(-th / 0.06) * Pulso.x * alfa;
        // el anillo: un arco grueso de luz que no cierra del todo (como un brazo del remolino que se enciende)
        float rw = 0.02 + 0.05 * Pulso.y;
        float giroAro = atan(p.y, p.x) - 1.4 * th;
        float arco = 0.2 + 0.8 * smoothstep(0.2, 0.85, 0.5 + 0.5 * cos(giroAro + 7.31));
        float aro = exp(-abs(th - Pulso.y) / rw) * Pulso.z * arco * (0.6 + 0.4 * fbm3(d * 4.0 + 3.0, 3));
        col = mix(col, vec3(0.95, 0.08, 0.06), clamp(aro, 0.0, 1.0) * alfa);
        col = mix(col, vec3(0.9, 0.06, 0.05), particulas(d, polo, GrietaExtra.w) * Pulso.w * alfa);
    }
    // el cabezazo: primero un brillo rojo detrás de la cara (el destello de todo el cielo viene en Pulso.x)
    if (Latido.x >= 0.0) {
        float brillo = exp(-pow((Latido.x - 0.24) / 0.09, 2.0)) * exp(-th / 0.14);
        col = mix(col, vec3(1.0, 0.07, 0.07), clamp(brillo, 0.0, 1.0) * alfa);
    }
    if (Grieta.z > 0.0) {
        vec4 g = grieta(p);
        float sale = g.a + alfa * (1.0 - g.a);
        col = sale > 0.0 ? (g.rgb * g.a + col * alfa * (1.0 - g.a)) / sale : col;
        alfa = sale;
    }
    // ...después un anillo de neón rojo, cerrado y grueso, que sale de la cara y se abre rápido (~2 rad/s) apagándose,
    // y los pedazos; encima de todo, también de la grieta (sobre lo morado se ve rosa, como en el video)
    if (Latido.x >= 0.0) {
        float le = Latido.x;
        float va = vidaOnda(le);
        if (va > 0.0) {
            float dr = abs(th - radioOnda(le));
            float hilo = exp(-dr / 0.013) * va;                     // una banda de neón gruesa (no un hilo)
            float luz = exp(-dr / 0.05) * va;
            // un segundo anillo, más fino, que sale un poco después
            float dr2 = abs(th - radioOnda(le - 0.07));
            float hilo2 = exp(-dr2 / 0.006) * vidaOnda(le - 0.07) * 0.6;
            col += vec3(0.95, 0.06, 0.12) * luz * 1.2;
            col = mix(col, vec3(1.0, 0.22, 0.28), clamp(max(hilo, hilo2), 0.0, 1.0));
            alfa = max(alfa, clamp(max(max(luz, hilo), hilo2), 0.0, 1.0));
        }
        vec4 pz = pedazos(p, le, Latido.y);
        if (pz.z > 0.0) {
            // del material de la grieta, su neón: rojo vivo, la orilla un poco más clara, el corazón casi blanco (como
            // el hilo del trazo) y su brillo rojo alrededor
            col += vec3(0.95, 0.06, 0.1) * pz.z * 0.8;
            col = mix(col, vec3(0.92, 0.03, 0.05), clamp(pz.x, 0.0, 1.0));
            col = mix(col, vec3(1.0, 0.2, 0.2), clamp(pz.y, 0.0, 1.0));
            col = mix(col, vec3(1.0, 0.72, 0.62), clamp(pz.w, 0.0, 1.0));
            alfa = max(alfa, clamp(max(max(pz.x, pz.y), pz.z * 0.8), 0.0, 1.0));
        }
    }
    if (alfa < 0.002) discard;
    // un granito casi invisible: en los rojos muy oscuros no se ven escalones de color
    col += (azar(gl_FragCoord.xy) - 0.5) / 255.0;
    fragColor = vec4(col, alfa);
}
