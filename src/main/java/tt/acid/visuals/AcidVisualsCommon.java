package tt.acid.visuals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AcidVisualsCommon implements ModInitializer {
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!GodSettings.flight) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isCreative() || player.isSpectator()) continue;
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
            if (GodSettings.invulnerability) {
                if (GodSettings.retaliation && attacker != null && attacker != player && attacker.isAlive())
                    attacker.damage(player.getDamageSources().magic(), 10000.0F);
                return false;
            }
            if (GodSettings.retaliation && attacker != null && attacker != player && attacker.isAlive())
                attacker.damage(player.getDamageSources().magic(), 10000.0F);
            return true;
        });
    }
}