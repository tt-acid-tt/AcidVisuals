#version 330
#moj_import <acidvisuals:shape.glsl>

in vec2 LocalPx;
flat in vec2 SizePx;
flat in float RadiusPx;
flat in float Smoothness;
in vec4 FragColor;
out vec4 OutColor;

void main() {
    float alpha = ralpha1(SizePx, LocalPx, RadiusPx, Smoothness);
    if (alpha <= 0.001) discard;
    OutColor = vec4(FragColor.rgb, FragColor.a * alpha);
}