// Signed-distance functions adapted for AcidVisuals UI rendering.
const float SQUIRCLE_EXPONENT = 4.0;
const float CIRCLE_EXPONENT = 2.0;

float rexponent(vec2 sizePx, float radius) {
    float fill = radius / max(min(sizePx.x, sizePx.y) * 0.5, 0.0001);
    return mix(SQUIRCLE_EXPONENT, CIRCLE_EXPONENT, smoothstep(0.75, 1.0, fill));
}

float rdist1(vec2 p, vec2 sizePx, float radiusPx) {
    float radius = min(radiusPx, min(sizePx.x, sizePx.y) * 0.5);
    vec2 halfSize = sizePx * 0.5;
    vec2 q = abs(p - halfSize) - (halfSize - vec2(radius));
    vec2 corner = max(q, vec2(0.0));
    float cornerDistance;
    if (radius > 0.0 && corner.x > 0.0 && corner.y > 0.0) {
        float exponent = rexponent(sizePx, radius);
        cornerDistance = pow(pow(corner.x, exponent) + pow(corner.y, exponent), 1.0 / exponent);
    } else {
        cornerDistance = max(corner.x, corner.y);
    }
    return min(max(q.x, q.y), 0.0) + cornerDistance - radius;
}

float rsoftness(float dist, float smoothness) {
    return smoothness <= 0.0 ? 0.0001 : max(fwidth(dist) * 0.5, 0.0001);
}

float ralpha1(vec2 sizePx, vec2 localPx, float radiusPx, float smoothness) {
    float dist = rdist1(localPx, sizePx, radiusPx);
    float softness = rsoftness(dist, smoothness);
    return 1.0 - smoothstep(-softness, softness, dist);
}