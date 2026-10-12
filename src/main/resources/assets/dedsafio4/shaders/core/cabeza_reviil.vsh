#version 150

// La cabeza 3D de Reviil (client/CabezaReviil), dibujada en su imagen. Las posiciones y normales ya vienen animadas.

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 uv;
out vec3 normal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    uv = UV0;
    normal = Normal;
}
