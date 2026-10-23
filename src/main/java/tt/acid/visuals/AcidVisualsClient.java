package tt.acid.visuals;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
public final class AcidVisualsClient implements ClientModInitializer {
 private boolean wasDown;
 @Override public void onInitializeClient(){ ClientTickEvents.END_CLIENT_TICK.register(c->{ if(c.getWindow()==null)return; boolean down=GLFW.glfwGetKey(c.getWindow().getHandle(),GLFW.GLFW_KEY_RIGHT_SHIFT)==GLFW.GLFW_PRESS; if(down&&!wasDown){if(c.currentScreen instanceof AcidVisualsScreen)c.setScreen(null);else if(c.currentScreen==null)c.setScreen(new AcidVisualsScreen());} wasDown=down; }); }
}
