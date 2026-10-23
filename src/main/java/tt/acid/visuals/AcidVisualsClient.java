package tt.acid.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AcidVisualsClient implements ClientModInitializer {
    private boolean wasDown;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GodController.tick(client);
            KillAuraController.tick(client);

            if (client.getWindow() == null) return;
            boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !wasDown) {
                if (client.currentScreen instanceof GodScreen) client.setScreen(null);
                else if (client.currentScreen == null) client.setScreen(new GodScreen());
            }
            wasDown = down;
        });
    }
}
