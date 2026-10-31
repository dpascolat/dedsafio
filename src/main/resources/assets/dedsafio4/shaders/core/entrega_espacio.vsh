#version 150

// Las nubes del Cielo Meteoritos (nubes_meteoritos.fsh): un rectángulo que cubre toda la pantalla (ya viene en
// coordenadas de pantalla, -1..1); el fragment saca de ahí el rayo de cada píxel.

in vec3 Position;

out vec2 pantalla;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    pantalla = Position.xy;
}
