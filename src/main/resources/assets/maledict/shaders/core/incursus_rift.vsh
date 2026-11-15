#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 viewPosition;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    viewPosition = viewPos.xyz;
    gl_Position = ProjMat * viewPos;

    texCoord0 = UV0;
    vertexColor = Color;
}
