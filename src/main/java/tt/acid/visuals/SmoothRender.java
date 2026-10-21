package tt.acid.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL30;

/**
 * Shared OpenGL state helpers for custom AcidVisuals rendering.
 * Call only from the Minecraft render thread.
 */
public final class SmoothRender {
    private SmoothRender() {}

    public static void beginPositionColor() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    public static void end() {
        RenderSystem.disableBlend();
    }

    /**
     * Apply linear filtering to the currently bound 2D texture.
     * This smooths texture sampling when scaled; it does not create detail
     * that is absent from the original image.
     */
    public static void setLinearTextureFiltering() {
        RenderSystem.texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        RenderSystem.texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
    }
}
