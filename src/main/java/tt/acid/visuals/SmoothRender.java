package tt.acid.visuals;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * OpenGL state helpers for custom AcidVisuals rendering.
 *
 * Minecraft 1.21.11 no longer exposes the legacy RenderSystem blend methods
 * or GameRenderer.getPositionColorProgram() used by older examples. GUI
 * primitives should use DrawContext's supported rendering pipeline; these
 * helpers only manage OpenGL state for custom texture work.
 */
public final class SmoothRender {
    private SmoothRender() {}

    public static void beginPositionColor() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void end() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Apply linear filtering to the currently bound 2D texture.
     * This affects texture sampling, not glyph geometry or vector edges.
     */
    public static void setLinearTextureFiltering() {
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
    }
}
