package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Client-side KillAura using Minecraft's normal attack and cooldown mechanics. */
public final class KillAuraController {
    private static int ticksUntilNextAttack;
    private KillAuraController() {}

    public static void tick(MinecraftClient client) {
        if (!KillAuraSettings.enabled || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) return;
        if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;

        LivingEntity target = null;
        double bestDistance = KillAuraSettings.range * KillAuraSettings.range;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == client.player
                    || living instanceof ArmorStandEntity || !living.isAlive() || living.isRemoved()
                    || living.isSpectator() || living.isInvisible()) continue;
            double distance = client.player.squaredDistanceTo(living);
            if (distance <= bestDistance) { bestDistance = distance; target = living; }
        }
        if (target == null) return;
        aimAt(client, target);

        float requiredCharge = Math.max(0.1f, Math.min(1f, KillAuraSettings.damageCharge / 100f));
        if (ticksUntilNextAttack <= 0 && client.player.getAttackCooldownProgress(0.0f) >= requiredCharge) {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            ticksUntilNextAttack = Math.max(1, 20 / Math.max(1, Math.min(20, KillAuraSettings.attacksPerSecond)));
        }
    }

    private static void aimAt(MinecraftClient client, LivingEntity target) {
        Vec3d from = client.player.getEyePos();
        Vec3d to = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.85, target.getZ());
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float)(-(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI)));
        client.player.setYaw(yaw);
        client.player.setPitch(MathHelper.clamp(pitch, -90f, 90f));
    }
}
