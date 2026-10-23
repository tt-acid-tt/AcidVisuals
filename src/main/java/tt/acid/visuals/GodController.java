package tt.acid.visuals;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public final class GodController {
    private static boolean laserWasDown;
    private static int tick;
    private GodController() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tick++;
        var player = client.player;

        if (GodSettings.flight) {
            player.getAbilities().allowFlying = true;
            if (!player.getAbilities().flying) player.getAbilities().flying = true;
            player.getAbilities().setFlySpeed(0.08f);
            player.sendAbilitiesUpdate();
        }
        if (GodSettings.invulnerability && tick % 40 == 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 4, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 2, false, false, false));
            player.setFireTicks(0);
        }
        if (GodSettings.killAura && tick % 3 == 0) {
            double range = Math.max(1.0, Math.min(128.0, GodSettings.killRange));
            for (Entity entity : client.world.getOtherEntities(player, player.getBoundingBox().expand(range))) {
                if (entity instanceof LivingEntity living && living.isAlive()
                        && player.distanceTo(living) <= range && living != player) {
                    client.interactionManager.attackEntity(player, living);
                }
            }
        }
        if (GodSettings.timeStop && tick % 5 == 0) {
            for (Entity entity : client.world.getOtherEntities(player, player.getBoundingBox().expand(18))) {
                if (entity instanceof LivingEntity living && living != player) {
                    living.setVelocity(Vec3d.ZERO);
                    living.velocityModified = true;
                }
            }
        }
        if (GodSettings.telekinesis && tick % 2 == 0) {
            Vec3d pullTo = player.getPos().add(0, 1, 0);
            for (Entity entity : client.world.getOtherEntities(player, player.getBoundingBox().expand(16))) {
                if (entity instanceof LivingEntity living && living != player) {
                    Vec3d delta = pullTo.subtract(living.getPos()).normalize().multiply(0.22);
                    living.addVelocity(delta.x, delta.y + 0.03, delta.z);
                }
            }
        }

        boolean laserDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (GodSettings.homeLantern && GodSettings.eyeLaser && laserDown && !laserWasDown) fireLaser(client);
        laserWasDown = laserDown;
    }

    private static void fireLaser(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(48.0));
        BlockHitResult hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, player));
        Vec3d stop = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
        Vec3d direction = stop.subtract(start).normalize();
        for (int i = 0; i < 48; i++) {
            Vec3d p = start.add(direction.multiply(i));
            world.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, 0.01, 0);
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 0, 0, 0);
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos().offset(hit.getSide());
            if (world.isClient) {
                for (int i = 0; i < 20; i++) {
                    world.addParticle(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
                }
            }
            // The local single-player integrated server handles the actual world effect through the vanilla command-free interaction.
            client.player.networkHandler.sendChatCommand("summon minecraft:lightning_bolt " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
    }

    public static void strikeLightning(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        BlockPos pos = client.player.getBlockPos().offset(client.player.getHorizontalFacing(), 4);
        client.player.networkHandler.sendChatCommand("summon minecraft:lightning_bolt " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    public static void teleportForward(MinecraftClient client) {
        if (client.player == null) return;
        Vec3d p = client.player.getPos().add(client.player.getRotationVec(1.0f).multiply(8.0));
        client.player.networkHandler.sendChatCommand("tp @s " + p.x + " " + p.y + " " + p.z);
    }
}
