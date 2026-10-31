#version 150

// El fuego del meteorito (meteorito_fuego.fsh): el halo, la estela y las chispas, en una sola tanda. Las posiciones
// vienen relativas al ojo; el color dice qué es cada cosa.

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 VistaProy;

out vec3 punto;
out vec2 uv;
out vec4 color;

void main() {
    gl_Position = VistaProy * vec4(Position, 1.0);
    gl_Position.z = min(gl_Position.z, gl_Position.w * 0.99999);   // (lo lejano también)
    punto = Position;
    uv = UV0;
    color = Color;
}
