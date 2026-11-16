#version 150

// Rift Bloom: what sits inside the cut.
//
// The petal is a single flat quad with no thickness. The sense of depth comes from sampling
// the night sky as three backdrops at different distances behind that quad:
//
//   screen = the fragment's view ray, so the sky always fills the tear;
//   coord  = screen * K + cameraWorld * (K / depth) + per layer seed
//
// The (K / depth) term is the parallax. A near backdrop shifts strongly as the camera moves,
// a far one barely shifts, so moving the camera makes the layers slide across each other.
// That is what reads as "a slit cut into a curtain with space behind it" instead of a star
// decal pasted on the surface.
//
// One pass only: the silhouette is the blade mask itself, with no separate rim colour.
// ASCII only: GLSL is specified as ASCII and some drivers reject non-ASCII in comments.

uniform sampler2D Sampler0;

uniform float RiftTime;
uniform float RiftWiden;
uniform float RiftStars;
uniform vec3 RiftCam;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

const vec3 NIGHT_DEEP = vec3(0.006, 0.010, 0.028);
const vec3 NIGHT_MID = vec3(0.018, 0.030, 0.068);
const vec3 NIGHT_HAZE = vec3(0.065, 0.075, 0.145);
const vec3 STAR_COOL = vec3(0.78, 0.86, 1.00);
const vec3 STAR_WARM = vec3(1.00, 0.95, 0.88);
const vec3 STAR_CYAN = vec3(0.60, 0.93, 0.97);

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float maskAt(vec2 uv) {
    return texture(Sampler0, uv).a;
}

// Only widens the hairline stroke enough for the tear to have a body. One UV unit spans the
// whole quad, so this value is much stronger than it looks: keep it tiny.
float woundBody(vec2 uv, float width) {
    float body = maskAt(uv);
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.7853982;
        vec2 offset = vec2(cos(angle), sin(angle)) * width;
        body = max(body, maskAt(uv + offset));
    }
    return body;
}

// Fragment direction in a screen-like space. Independent of the quad's own size, so the sky
// does not stretch when the petal flies outward.
vec2 screenRay(vec3 p) {
    return p.xy / max(-p.z, 0.05);
}

vec2 layerCoord(vec3 p, float cells, float depth, float seed) {
    vec2 ray = screenRay(p) * cells;
    vec2 parallax = RiftCam.xz * (cells / depth);
    return ray + parallax + vec2(seed * 37.0, seed * 61.0);
}

// One chunky star per cell: bright core plus a short cross. `size` varies the star by layer
// instead of the cell count; the cell count has to stay equal across layers, otherwise the
// finer far layer would shift more per block of camera movement than the near one and the
// parallax ordering would read backwards.
float starCell(vec2 coordinate, float occupancy, float size, out vec3 tint) {
    vec2 cell = floor(coordinate);
    vec2 local = fract(coordinate) - 0.5;
    tint = STAR_COOL;
    if (hash21(cell + vec2(0.7, 1.3)) < occupancy) {
        return 0.0;
    }
    float pick = hash21(cell + 5.3);
    if (pick < 0.14) {
        tint = STAR_WARM;
    } else if (pick < 0.32) {
        tint = STAR_CYAN;
    }
    vec2 center = (vec2(hash21(cell + 1.9), hash21(cell + 8.1)) - 0.5) * 0.45;
    vec2 d = abs(local - center);
    float core = step(max(d.x, d.y), 0.075 * size);
    float arms = step(min(d.x, d.y), 0.030 * size) * step(max(d.x, d.y), 0.28 * size);
    float twinkle = 0.55 + 0.45 * sin(RiftTime * 1.3 + hash21(cell + 21.7) * 6.283);
    return (core + arms * 0.5) * twinkle * (0.45 + 0.55 * hash21(cell + 13.7));
}

// Faint milky band so the navy never looks like flat paint.
float band(vec2 coordinate, float seed) {
    float wave = sin(coordinate.x * 0.55 + hash21(vec2(seed, 2.0)) * 6.283) * 0.5 + 0.5;
    float grain = hash21(floor(coordinate * 0.7) + seed);
    return smoothstep(0.52, 1.0, wave * 0.65 + grain * 0.5);
}

void main() {
    vec2 uv = texCoord0;
    float seed = vertexColor.r;
    float variant = vertexColor.b;
    float alpha = vertexColor.a;
    if (alpha <= 0.004) {
        discard;
    }

    float stroke = maskAt(uv);
    float body = woundBody(uv, max(RiftWiden, 0.0) * (0.8 + 0.4 * variant));

    // Hard cut. A wide smoothstep here turns the mark into smoke; the texture is only 64 px
    // spread over roughly seven blocks, so its own edge is already blurry and needs a
    // threshold rather than more softening. The two narrow bounds keep one texel of anti
    // aliasing so the outline does not crawl.
    float bodyMask = smoothstep(0.42, 0.58, body);
    if (bodyMask <= 0.01) {
        discard;
    }

    vec3 p = viewPosition;

    // Three backdrops at 3.5, 12 and 40 blocks behind the tear. Same cell count, so the
    // camera shift per block is ordered by depth: near stars stream, far stars hold.
    const float CELLS = 14.0;
    vec3 nearTint;
    vec3 midTint;
    vec3 farTint;
    float nearStars = starCell(layerCoord(p, CELLS, 3.5, seed), 0.74, 1.30, nearTint);
    float midStars = starCell(layerCoord(p, CELLS, 12.0, seed + 17.0), 0.80, 1.00, midTint);
    float farStars = starCell(layerCoord(p, CELLS, 40.0, seed + 41.0), 0.86, 0.72, farTint);

    float haze = band(layerCoord(p, 1.6, 12.0, seed + 5.0), seed);
    vec3 sky = mix(NIGHT_DEEP, NIGHT_MID, 0.30 + 0.45 * haze);
    sky += NIGHT_HAZE * haze * 0.35;
    sky += farStars * farTint * 0.48 * RiftStars;
    sky += midStars * midTint * 0.78 * RiftStars;
    sky += nearStars * nearTint * 1.10 * RiftStars;

    // The cut itself: the outer band of the mark is a near black gap, which is what makes
    // the edge read as a blade slit with a curtain's thickness behind it. The night sky only
    // shows in the core of the mark, so the taper of the original stroke is preserved.
    float slit = smoothstep(0.40, 0.80, stroke);
    vec3 gap = vec3(0.002, 0.003, 0.008);
    vec3 color = mix(gap, sky, slit);

    fragColor = vec4(color, bodyMask * alpha);
}
