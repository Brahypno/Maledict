#version 150

uniform sampler2D DiffuseSampler;
uniform float time;
uniform float Strength;
uniform float Flash;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    float grain = fract(sin(dot(texCoord * 1024.0 + time, vec2(12.9898, 78.233))) * 43758.5453);
    float gray = clamp((luminance - 0.5) * 1.12 + 0.55 + (grain - 0.5) * 0.003, 0.0, 1.0);
    float vignette = smoothstep(0.25, 0.72, length(texCoord - 0.5));
    vec3 color = mix(source.rgb, vec3(gray) * (1.0 - vignette * 0.12), clamp(Strength, 0.0, 1.0));
    fragColor = vec4(mix(color, vec3(1.0), clamp(Flash, 0.0, 1.0)), source.a);
}
