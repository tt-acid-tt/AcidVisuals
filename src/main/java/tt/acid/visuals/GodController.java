package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/** Client-side movement and ESP helpers; no chat commands are sent. */
public final class GodController {
    private static boolean previousFlight;
    private GodController() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        var player = client.player;

        if (GodSettings.flight) {
            player.getAbilities().allowFlying = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlySpeed(0.06f);
            player.fallDistance = 0.0f;
        } else if (previousFlight && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.fallDistance = 0.0f;
        }
        previousFlight = GodSettings.flight;

        if (GodSettings.invulnerability) {
            player.fallDistance = 0.0f;
            player.setFireTicks(0);
            if (player.age % 30 == 0) {
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, 80, 4, false, false, false));
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.REGENERATION, 80, 2, false, false, false));
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE, 100, 0, false, false, false));
            }
        }

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == player) continue;
            // Glowing is local rendering only and does not modify the remote server entity.
            if (GodSettings.esp && living.isAlive() && !living.isGlowing()) living.setGlowing(true);
            else if (!GodSettings.esp && living.isGlowing()) living.setGlowing(false);
        }
    }
}
