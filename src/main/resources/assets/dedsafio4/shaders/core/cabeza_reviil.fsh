#version 150

// La cabeza 3D de Reviil: su textura (pixel art, sin filtro), una luz de frente y de arriba, la luz roja de la grieta
// desde abajo y su brillo propio (la textura "glow" del modelo). Lo transparente de la textura no se dibuja.
// (Lo mismo que tools/cielo/modelo.py Imagen.FS, para el visor.)

uniform sampler2D Sampler0;     // la textura del modelo
uniform sampler2D Sampler1;     // su brillo

in vec2 uv;
in vec3 normal;

out vec4 fragColor;

void main() {
    vec4 c = texture(Sampler0, uv);
    if (c.a < 0.1) discard;
    vec3 n = normalize(normal);
    vec3 col = c.rgb * (0.5 + 0.5 * max(dot(n, normalize(vec3(0.25, 0.55, -0.8))), 0.0));
    col += c.rgb * vec3(0.9, 0.1, 0.08) * pow(max(dot(n, normalize(vec3(0.0, -0.6, 0.3))), 0.0), 1.5) * 0.7;
    vec4 g = texture(Sampler1, uv);
    col += g.rgb * g.a * 1.3;
    fragColor = vec4(col, 1.0);
}
