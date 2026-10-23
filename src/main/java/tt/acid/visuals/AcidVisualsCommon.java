package tt.acid.visuals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AcidVisualsCommon implements ModInitializer {
    @Override public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            // The response is processed before damage is cancelled for the protected player.
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
            if (GodSettings.retaliation && attacker != null && attacker != player && attacker.isAlive()) {
                attacker.damage(player.getServerWorld(), player.getDamageSources().magic(), 10000.0F);
            }

            // Authoritative only when this mod is installed on the integrated/dedicated server.
            if (GodSettings.invulnerability) return false;
            return true;
        });
    }
}
