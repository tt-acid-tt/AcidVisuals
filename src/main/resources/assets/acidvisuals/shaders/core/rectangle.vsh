#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <acidvisuals:shape_attributes.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec2 LocalPx;
flat out vec2 SizePx;
flat out float RadiusPx;
flat out float Smoothness;
out vec4 FragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    LocalPx = UV0;
    SizePx = vec2(UV1) / 4.0;
    RadiusPx = float(UV2.x >= 0 ? UV2.x : -UV2.x - 1) / 8.0;
    Smoothness = UV2.x >= 0 ? 1.0 : 0.0;
    FragColor = Color;
}