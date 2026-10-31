#version 150

// LA LUZ SOBRE EL PILAR Y SUS CHISPAS (client/ruleta/EntregaCliente; del add-on: entrega.py, PIEZA 4, y en el video del server:
// destellos chicos de colores sobre el pilar, que se vuelven una bola de luz blanca con bordes de arcoíris que titilan
// y se mueven, con chispitas alrededor, y al final se achica y se apaga). Cuadrados de frente a la cámara, sumados.
// Color del vértice: a = qué es (0: la luz, 0.5: su halo, 0.82: un destello del Vacío Cósmico, 1: una chispa); rgb: el
// color de la chispa o el destello (de la luz y el
// halo, r = cuánto). UV: -1..1 en el cuadrado.

uniform vec4 Tiempo;             // x: s desde que se tocó; y: segundos (para que los lóbulos giren)

in vec3 punto;
in vec2 uv;
in vec4 color;

out vec4 fragColor;

const float PI = 3.14159265;

void main() {
    float r = length(uv);
    if (r >= 1.0) discard;
    float s = Tiempo.y;
    vec3 col;
    if (color.a < 0.25) {
        // la luz: el corazón blanco y 6 lóbulos de colores que giran a su ritmo (una vuelta en 1.6 s o menos)
        float k = color.r;
        col = vec3(1.0, 0.98, 0.96) * exp(-r * r * 9.0) * 1.6;
        vec3 colores[6] = vec3[](vec3(1.0, 0.32, 0.72), vec3(1.0, 0.86, 0.25), vec3(0.35, 1.0, 0.45),
                vec3(0.3, 0.85, 1.0), vec3(0.7, 0.4, 1.0), vec3(1.0, 0.55, 0.22));
        float vueltas[6] = float[](1.0, -1.0, 2.0, 1.0, -2.0, -1.0);
        for (int i = 0; i < 6; i++) {
            float a = 2.0 * PI * (float(i) / 6.0 + vueltas[i] * s / 1.6);
            float rr = 0.42 + 0.06 * sin(s * 3.1 + float(i) * 1.7);
            vec2 c = rr * vec2(cos(a), sin(a));
            float g = exp(-dot(uv - c, uv - c) * 14.0);
            float tit = 0.7 + 0.3 * sin(s * 9.0 + float(i) * 2.3);
            col += colores[i] * g * tit * 0.95;
        }
        col *= k * smoothstep(1.0, 0.6, r);
    } else if (color.a < 0.75) {
        // su halo: un brillo suave y grande
        float k = color.r;
        col = vec3(1.0, 0.82, 0.95) * exp(-r * r * 4.0) * 0.45 * k;
    } else if (color.a < 0.9) {
        // un destello del Vacío Cósmico: una estrellita de cuatro puntas, suave, con el centro claro
        float g = exp(-r * r * 22.0);
        float puntas = exp(-abs(uv.x) * 26.0 - abs(uv.y) * 3.2) + exp(-abs(uv.y) * 26.0 - abs(uv.x) * 3.2);
        float m = max(max(color.r, color.g), color.b);
        col = color.rgb * (g * 1.3 + puntas * 0.75 + exp(-r * 3.5) * 0.18) + vec3(1.0) * exp(-r * r * 90.0) * 0.7 * m;
        col *= smoothstep(1.0, 0.7, r);
    } else {
        // una chispa: un puntito con el centro claro
        float g = exp(-r * r * 6.0);
        col = color.rgb * g * 1.2 + vec3(1.0) * exp(-r * r * 30.0) * 0.6 * max(max(color.r, color.g), color.b);
    }
    fragColor = vec4(col, 0.0);
}
