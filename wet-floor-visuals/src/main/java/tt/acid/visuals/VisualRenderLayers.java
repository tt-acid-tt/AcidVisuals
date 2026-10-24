package tt.acid.visuals;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

/**
 * Textured translucent overlays that test against the world depth buffer but do not write
 * their own depth. Transparent pixels of a sprite therefore cannot hide water, blocks or
 * other textures drawn behind the sprite.
 */
final class VisualRenderLayers extends RenderLayer {
    private VisualRenderLayers() {
        super("acidvisuals:private", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            VertexFormat.DrawMode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    static RenderLayer translucentNoDepthWrite(Identifier texture) {
        MultiPhaseParameters phases = MultiPhaseParameters.builder()
            .program(ENTITY_TRANSLUCENT_PROGRAM)
            .texture(new Texture(texture, TriState.TRUE, false))
            .transparency(TRANSLUCENT_TRANSPARENCY)
            .depthTest(LEQUAL_DEPTH_TEST)
            .cull(DISABLE_CULLING)
            .lightmap(ENABLE_LIGHTMAP)
            .overlay(ENABLE_OVERLAY_COLOR)
            .writeMaskState(COLOR_MASK)
            .build(false);
        String layerName = "acidvisuals_translucent_no_depth/" + texture.getNamespace() + "/" + texture.getPath();
        return RenderLayer.of(layerName, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            VertexFormat.DrawMode.QUADS, 1536, false, true, phases);
    }
}
