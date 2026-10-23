package tt.acid.visuals;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
public final class AcidVisualsCommon implements ModInitializer {
 @Override public void onInitialize() {
  ServerTickEvents.END_SERVER_TICK.register(server -> {
   if (!AcidVisualsClientSettings.antiCheatEnabled) return;
   for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
    if (p.isCreative() || p.isSpectator()) continue;
    if (p.getAbilities().flying || p.getAbilities().allowFlying) {
     p.getAbilities().flying=false; p.getAbilities().allowFlying=false; p.sendAbilitiesUpdate();
     p.sendMessage(Text.literal("§c[AcidVisuals AntiCheat] Полёт в выживании запрещён."),false);
    }
   }
  });
 }
}
