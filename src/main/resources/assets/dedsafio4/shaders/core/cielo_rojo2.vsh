#version 150

// Un cubo alrededor de la cámara (sin trasladar: sólo gira con ella, como el cielo). Cada píxel recibe su
// dirección en ejes del mundo y el fragment busca dónde cae en el plano del cielo.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direccion;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    direccion = Position;
}
