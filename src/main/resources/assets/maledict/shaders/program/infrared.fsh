#version 150

// 微光视觉：暗的部分压成提亮过的灰度，亮的部分按配置把颜色留下；Strength 负责淡入淡出。
// 顶点着色器复用原版 blit（它提供 texCoord 与 Position），所以这里只写着色部分。

uniform sampler2D DiffuseSampler;

// Lodestone 每帧写进来的秒数，只给噪点当相位用。
uniform float time;

uniform float Strength;
uniform float Gain;
uniform float Lift;
uniform float Contrast;
uniform float HighlightKnee;
uniform float HighlightSoftness;
uniform float NoiseStrength;
uniform float GrayscaleThreshold;
uniform float GrayscaleSoftness;

in vec2 texCoord;

out vec4 fragColor;

// 高光软压缩：拐点之前原样，之后按双曲线收窄，永远逼近 knee + 1/softness 而到不了 1。
// 直接 clamp 会把亮部压成一片纯白，形状全丢。
float compressHighlights(float value) {
    if (value <= HighlightKnee) {
        return value;
    }
    float excess = value - HighlightKnee;
    return HighlightKnee + excess / (1.0 + excess * HighlightSoftness);
}

// 每像素的伪随机，噪点够用。NoiseStrength 为 0 时整项是 0，
// 但那两个 uniform 仍然被引用，免得 GL 把它们优化掉之后 Lodestone 找不到 time。
float hash(vec2 seed) {
    return fract(sin(dot(seed, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float strength = clamp(Strength, 0.0, 1.0);

    if (strength <= 0.0) {
        fragColor = source;
        return;
    }

    // 亮度按人眼权重取，不是三通道平均：绿色占七成，蓝色几乎不参与。
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));

    // 抬暗部 -> 增益 -> 拉对比 -> 压高光。顺序别换：压缩必须在对比之后，
    // 否则对比会把刚压下去的亮部再推回 1.0。
    float enhanced = pow(clamp(luminance, 0.0, 1.0), Lift);
    enhanced *= Gain;
    enhanced = (enhanced - 0.5) * Contrast + 0.5;
    enhanced = compressHighlights(clamp(enhanced, 0.0, 1.0));
    enhanced += (hash(texCoord * 1024.0 + time) - 0.5) * NoiseStrength;

    vec3 infrared = vec3(clamp(enhanced, 0.0, 1.0));

    // 动态灰度：先看这个像素原本有多亮。暗的地方整套转灰并提亮，亮的地方把颜色留下来，
    // 于是同一幅画面里"近处暗处的结构"和"亮处的颜色"能一起读，而不是整屏一把抓。
    // 阈值与过渡带宽来自客户端配置；带宽取个正数下限，免得 0 变成硬 edge 时的抖动。
    float colourKept = smoothstep(GrayscaleThreshold,
                                  GrayscaleThreshold + max(GrayscaleSoftness, 0.001),
                                  luminance);
    vec3 finalColor = mix(source.rgb, infrared, strength * (1.0 - colourKept));

    fragColor = vec4(finalColor, source.a);
}
