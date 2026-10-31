#version 150

// El piso de la entrega en el Pilar (pilar_piso.fsh): un cuadrado plano alrededor del pilar; UV0 = dónde (en bloques
// desde el centro del pilar: x al este, y al sur).

in vec3 Position;
in vec2 UV0;

uniform mat4 VistaProy;

out vec2 local;

void main() {
    gl_Position = VistaProy * vec4(Position, 1.0);
    local = UV0;
}
