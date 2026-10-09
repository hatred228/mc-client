#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Radius;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / InSize;
    vec4 sum = vec4(0.0);
    float total = 0.0;
    for (int x = -4; x <= 4; x++) {
        for (int y = -4; y <= 4; y++) {
            float w = exp(-(x*x + y*y) / (2.0 * Radius * Radius));
            vec2 off = vec2(float(x), float(y)) * texel;
            sum += texture(DiffuseSampler, texCoord + off) * w;
            total += w;
        }
    }
    fragColor = sum / total;
}