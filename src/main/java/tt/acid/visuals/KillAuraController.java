package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Smooth client-side aim helper. Attacks are attempted only during a valid critical-hit fall. */
public final class KillAuraController {
    private static int ticksUntilNextAttack;
    private KillAuraController() {}

    public static void tick(MinecraftClient client) {
        if (!GodSettings.killAura || !KillAuraSettings.enabled || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) {
            ticksUntilNextAttack = 0;
            return;
        }
        var player = client.player;
        if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;

        double range = MathHelper.clamp(KillAuraSettings.range, 1.0f, 3.2f);
        double bestDistance = range * range;
        LivingEntity target = null;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == player || living instanceof ArmorStandEntity
                    || !living.isAlive() || living.isRemoved() || living.isSpectator() || living.isInvisible()) continue;
            double distance = player.getEyePos().squaredDistanceTo(living.getBoundingBox().getCenter());
            if (distance <= bestDistance) { bestDistance = distance; target = living; }
        }
        if (target == null) return;

        aimAtSmoothly(client, target);
        float yawError = Math.abs(MathHelper.wrapDegrees(targetYaw(player.getEyePos(), target.getBoundingBox().getCenter()) - player.getYaw()));
        float pitchError = Math.abs(targetPitch(player.getEyePos(), target.getBoundingBox().getCenter()) - player.getPitch());
        boolean validCriticalFall = !player.isOnGround() && player.fallDistance > 0.0f
                && !player.isClimbing() && !player.isTouchingWater() && !player.hasVehicle()
                && !player.isSprinting() && !player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
        if (validCriticalFall && yawError < 8.0f && pitchError < 8.0f && ticksUntilNextAttack <= 0
                && player.getAttackCooldownProgress(0.0f) >= 0.92f) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            ticksUntilNextAttack = 2;
        }
    }

    private static void aimAtSmoothly(MinecraftClient client, LivingEntity target) {
        var player = client.player;
        Vec3d from = player.getEyePos(), to = target.getBoundingBox().getCenter();
        float yaw = targetYaw(from, to), pitch = targetPitch(from, to);
        int speed = MathHelper.clamp(KillAuraSettings.aimSpeed, 1, 10);
        float smoothing = 0.025f + speed * 0.009f;
        float maxTurn = 0.35f + speed * 0.5f;
        float yawDelta = MathHelper.wrapDegrees(yaw - player.getYaw());
        float pitchDelta = pitch - player.getPitch();
        player.setYaw(player.getYaw() + MathHelper.clamp(yawDelta * smoothing, -maxTurn, maxTurn));
        player.setPitch(MathHelper.clamp(player.getPitch() + MathHelper.clamp(pitchDelta * smoothing, -maxTurn, maxTurn), -90.0f, 90.0f));
    }

    private static float targetYaw(Vec3d from, Vec3d to) {
        double dx=to.x-from.x, dz=to.z-from.z;
        return (float)(MathHelper.atan2(dz,dx)*(180.0/Math.PI))-90.0f;
    }
    private static float targetPitch(Vec3d from, Vec3d to) {
        double dx=to.x-from.x, dy=to.y-from.y, dz=to.z-from.z;
        return (float)-(MathHelper.atan2(dy,Math.sqrt(dx*dx+dz*dz))*(180.0/Math.PI));
    }
}
