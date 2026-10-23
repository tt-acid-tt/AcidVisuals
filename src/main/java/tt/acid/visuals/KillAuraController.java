package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Conservative client-side combat helper. It uses normal Minecraft interaction
 * and cooldown mechanics; it does not send custom packets or bypass server checks.
 */
public final class KillAuraController {
    private static int ticksUntilNextAttack;

    private KillAuraController() {}

    public static void tick(MinecraftClient client) {
        if (!KillAuraSettings.enabled || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) {
            ticksUntilNextAttack = 0;
            return;
        }

        if (ticksUntilNextAttack > 0) {
            ticksUntilNextAttack--;
        }

        // Keep the configured distance within a conservative vanilla-like range.
        double range = MathHelper.clamp(KillAuraSettings.range, 1.0f, 3.0f);
        double bestDistance = range * range;
        LivingEntity target = null;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == client.player
                    || living instanceof ArmorStandEntity || !living.isAlive() || living.isRemoved()
                    || living.isSpectator() || living.isInvisible()) {
                continue;
            }

            // Measure from the player's eyes to the target's bounding-box center.
            Vec3d targetPoint = living.getBoundingBox().getCenter();
            double distance = client.player.getEyePos().squaredDistanceTo(targetPoint);
            if (distance <= bestDistance) {
                bestDistance = distance;
                target = living;
            }
        }

        if (target == null) {
            return;
        }

        aimAtSmoothly(client, target);

        float requiredCharge = MathHelper.clamp(KillAuraSettings.damageCharge / 100.0f, 0.1f, 1.0f);
        int attacksPerSecond = MathHelper.clamp(KillAuraSettings.attacksPerSecond, 1, 4);
        if (ticksUntilNextAttack <= 0
                && client.player.getAttackCooldownProgress(0.0f) >= requiredCharge) {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            ticksUntilNextAttack = Math.max(1, 20 / attacksPerSecond);
        }
    }

    private static void aimAtSmoothly(MinecraftClient client, LivingEntity target) {
        Vec3d from = client.player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) -(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI));

        // Limit rotation change per tick to avoid instant snaps.
        float maxTurnPerTick = 18.0f;
        float yawDelta = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        float pitchDelta = targetPitch - client.player.getPitch();
        client.player.setYaw(client.player.getYaw()
                + MathHelper.clamp(yawDelta, -maxTurnPerTick, maxTurnPerTick));
        client.player.setPitch(MathHelper.clamp(
                client.player.getPitch() + MathHelper.clamp(pitchDelta, -maxTurnPerTick, maxTurnPerTick),
                -90.0f, 90.0f));
    }
}
