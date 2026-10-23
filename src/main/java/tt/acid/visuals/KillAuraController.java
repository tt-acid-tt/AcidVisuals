package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Client-side combat helper using normal Minecraft interaction and cooldown mechanics. */
public final class KillAuraController {
    private static int ticksUntilNextAttack;

    private KillAuraController() {}

    public static void tick(MinecraftClient client) {
        if (!KillAuraSettings.enabled || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) {
            ticksUntilNextAttack = 0;
            return;
        }

        if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;

        double range = MathHelper.clamp(KillAuraSettings.range, 1.0f, 3.0f);
        double bestDistance = range * range;
        LivingEntity target = null;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == client.player
                    || living instanceof ArmorStandEntity || !living.isAlive() || living.isRemoved()
                    || living.isSpectator() || living.isInvisible()) continue;

            Vec3d targetPoint = living.getBoundingBox().getCenter();
            double distance = client.player.getEyePos().squaredDistanceTo(targetPoint);
            if (distance <= bestDistance) {
                bestDistance = distance;
                target = living;
            }
        }

        if (target == null) return;
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
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) -(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI));

        int speed = MathHelper.clamp(KillAuraSettings.aimSpeed, 1, 10);
        float smoothing = 0.055f + speed * 0.022f;
        float maxTurnPerTick = 1.0f + speed * 1.15f;
        float yawDelta = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        float pitchDelta = targetPitch - client.player.getPitch();

        float yawStep = MathHelper.clamp(yawDelta * smoothing, -maxTurnPerTick, maxTurnPerTick);
        float pitchStep = MathHelper.clamp(pitchDelta * smoothing, -maxTurnPerTick, maxTurnPerTick);
        client.player.setYaw(client.player.getYaw() + yawStep);
        client.player.setPitch(MathHelper.clamp(client.player.getPitch() + pitchStep, -90.0f, 90.0f));
    }
}
