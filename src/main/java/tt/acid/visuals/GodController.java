package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class GodController {
    private static int tick;
    private static boolean jumpWasDown;
    private static boolean wetWasDown;
    private GodController() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.getWindow() == null) return;
        tick++;
        var player = client.player;
        if (GodSettings.flight) {
            player.getAbilities().allowFlying = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlySpeed(0.08f);
            player.fallDistance = 0;
        }

        if (GodSettings.invulnerability && tick % 30 == 0) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, 80, 4, false, false, false));
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.REGENERATION, 80, 2, false, false, false));
            player.setFireTicks(0);
        }

        if (GodSettings.killAura && tick % 4 == 0 && client.interactionManager != null) {
            double range = Math.max(1.0, Math.min(64.0, GodSettings.killRange));
            for (Entity entity : client.world.getOtherEntities(player, player.getBoundingBox().expand(range))) {
                if (entity instanceof LivingEntity living && living.isAlive() && player.distanceTo(living) <= range) {
                    client.interactionManager.attackEntity(player, living);
                }
            }
        }

        if (GodSettings.timeStop && tick % 5 == 0) {
            for (Entity entity : client.world.getOtherEntities(player, player.getBoundingBox().expand(12))) {
                if (entity instanceof LivingEntity living && living != player) {
                    living.setVelocity(Vec3d.ZERO);
                    living.velocityModified = true;
                }
            }
        }

        if (GodSettings.telekinesis && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target && target != player && tick % 2 == 0) {
            Vec3d hold = player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(3.5));
            Vec3d delta = hold.subtract(target.getPos().add(0, target.getHeight() * 0.5, 0));
            Vec3d velocity = delta.multiply(0.18);
            if (velocity.length() > 0.65) velocity = velocity.normalize().multiply(0.65);
            target.setVelocity(velocity);
            target.velocityModified = true;
            client.world.addParticle(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getHeight() * .5, target.getZ(), 0, .025, 0);
        }

        boolean jumpDown = player.isOnGround() && player.getVelocity().y > 0.05;
        if (GodSettings.jumpCircle && jumpDown && !jumpWasDown) {
            for (int i = 0; i < 40; i++) {
                double a = Math.PI * 2 * i / 40.0;
                client.world.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(a) * .8,
                        player.getY() + .08, player.getZ() + Math.sin(a) * .8, 0, .025, 0);
            }
        }
        jumpWasDown = jumpDown;

        if (GodSettings.wetFloor && tick % 8 == 0) {
            BlockPos below = player.getBlockPos().down();
            if (!client.world.getBlockState(below).isAir()) {
                for (int i = 0; i < 10; i++) {
                    double x = player.getX() + (client.world.random.nextDouble() - .5) * 5;
                    double z = player.getZ() + (client.world.random.nextDouble() - .5) * 5;
                    client.world.addParticle(ParticleTypes.DRIPPING_WATER, x, player.getY() + .04, z, 0, 0, 0);
            }
        }
        }
    }

    public static void fireLaser(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d eye = player.getCameraPosVec(1.0f);
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(.105);
        for (int side = -1; side <= 1; side += 2) {
            Vec3d start = eye.add(right.multiply(side));
            Vec3d end = start.add(look.multiply(48));
            BlockHitResult block = world.raycast(new RaycastContext(start, end,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
            EntityHitResult entityHit = ProjectileUtil.raycast(player, start, end,
                    new Box(start, end).expand(.25), entity -> entity.isAlive() && entity != player, 48 * 48);
            Vec3d stop = end;
            if (block.getType() != HitResult.Type.MISS) stop = block.getPos();
            if (entityHit != null && start.squaredDistanceTo(entityHit.getPos()) < start.squaredDistanceTo(stop)) stop = entityHit.getPos();
            Vec3d line = stop.subtract(start);
            int count = Math.max(1, (int)(line.length() * 5));
            for (int i = 0; i <= count; i++) {
                Vec3d p = start.add(line.multiply(i / (double) count));
                world.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
                if (i % 2 == 0) world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 0, 0, 0);
            }
            if (entityHit != null && client.interactionManager != null) {
                client.interactionManager.attackEntity(player, entityHit.getEntity());
                world.addParticle(ParticleTypes.EXPLOSION, stop.x, stop.y, stop.z, 0, 0, 0);
            } else if (block.getType() != HitResult.Type.MISS) {
                Vec3d p = block.getPos();
                for (int i = 0; i < 12; i++) {
                    world.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, (world.random.nextDouble()-.5)*.12, .05, (world.random.nextDouble()-.5)*.12);
                }
                world.addParticle(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }
}
