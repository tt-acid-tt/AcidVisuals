package tt.acid.visuals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AcidVisualsCommon implements ModInitializer {
    @Override public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            // This is authoritative only in a world/server where this mod is installed server-side.
            if (GodSettings.invulnerability) return false;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
            if (GodSettings.retaliation && attacker != null && attacker != player && attacker.isAlive()) {
                attacker.damage(player.getServerWorld(), player.getDamageSources().magic(), 10000.0F);
            }
            return true;
        });
    }
}
