package tt.acid.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AcidVisualsClient implements ClientModInitializer {
    private boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        VisualSettings.load();
        WorldRenderEvents.BEFORE_ENTITIES.register(VisualRenderer::renderSkyAtmosphere);
        WorldRenderEvents.AFTER_ENTITIES.register(VisualRenderer::renderWorld);
        HudLayerRegistrationCallback.EVENT.register(layers -> {
            layers.attachLayerAfter(IdentifiedLayer.HOTBAR_AND_BARS, Identifier.of("acidvisuals", "custom_hotbar"),
                (context, tickCounter) -> VisualRenderer.renderHotbar(context));
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VisualRenderer.tick(client);
            if (client.getWindow() == null) return;
            boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !rightShiftWasDown) {
                if (client.currentScreen instanceof VisualMenuScreen) client.setScreen(null);
                else if (client.currentScreen == null) client.setScreen(new VisualMenuScreen());
            }
            rightShiftWasDown = down;
        });
    }
}
