package tt.acid.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AcidVisualsClient implements ClientModInitializer {
    private boolean wasDown;
    private static KeyBinding laserKey;
    private static KeyBinding telekinesisKey;
    private static KeyBinding lanternKey;

    @Override public void onInitializeClient() {
        laserKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.god_of_the_world.laser", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.god_of_the_world"));
        telekinesisKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.god_of_the_world.telekinesis", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.god_of_the_world"));
        lanternKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.god_of_the_world.lantern", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.god_of_the_world"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GodController.tick(client);
            while (laserKey.wasPressed()) {
                GodSettings.eyeLaser = !GodSettings.eyeLaser;
                if (GodSettings.eyeLaser) GodController.fireLaser(client);
            }
            while (telekinesisKey.wasPressed()) GodSettings.telekinesis = !GodSettings.telekinesis;
            while (lanternKey.wasPressed()) GodSettings.homeLantern = !GodSettings.homeLantern;

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
