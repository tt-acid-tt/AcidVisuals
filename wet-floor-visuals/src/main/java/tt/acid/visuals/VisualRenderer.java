package tt.acid.visuals;

// Client-side visuals; rendering effects are local and require no server commands.

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class VisualRenderer {
    private static final Identifier HOTBAR = id("textures/gui/hotbar");
    private static final Identifier SLOT = id("textures/gui/slot");
    private static final Identifier SELECTED = id("textures/gui/selected_slot");
    private static final Identifier JUMP_RING = id("textures/fx/jump_ring");
    private static final Identifier ORB = id("textures/fx/orb");
    private static final Identifier HUD_CARD = id("textures/gui/button");
    private static final Identifier BLACK_HOLE_OBJECT = id("textures/environment/object_blackhole");
    private static final Identifier BLACK_HOLE_REFLECTIONS = id("textures/environment/sky_void_reflections");
    private static final int SIN_TABLE_SIZE = 4096;
    private static final float[] SIN_TABLE = buildSinTable();
    private static final Identifier[] SKY_TEXTURES = {
        id("textures/environment/sky_aurora"),
        id("textures/environment/sky_nebula"),
        id("textures/environment/sky_sunset"),
        id("textures/environment/sky_dream"),
        id("textures/environment/sky_void"),
        id("textures/environment/sky_emerald"),
        id("textures/environment/sky_supernova"),
        id("textures/environment/sky_cyber")
    };
    private static final Identifier FONT = id("acid");
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<GlowParticle> PARTICLES = new ArrayList<>();
    private static final List<WetTile> WET_TILES = new ArrayList<>();
    private static final List<RainDrop> RAIN_DROPS = new ArrayList<>();
    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static boolean wasGrounded = true;
    private static long lastSurfaceScan = -1000;
    private static float smoothSlotX = Float.NaN;
    private static long clientTicks;
    private static boolean surfaceScanRequested;
    private static int surfaceScanDelay;
    private static final float[] KEY_ANIMATION = new float[6];
    private static final float[] KEY_RENDER_ANIMATION = new float[6];
    private static final float[] KEY_TARGETS = new float[6];
    private static LivingEntity lastHudTarget;
    private static int targetHoldTicks;
    private static float targetHudAlpha;
    private static float targetHealthSmooth;
    private static CloudRenderMode previousCloudMode;
    private static boolean forcedCloudsOff;

    private VisualRenderer() {}

    private static Identifier id(String path) {
        if (path.equals("acid")) return Identifier.of("acidvisuals", "acid");
        return Identifier.of("acidvisuals", path + ".png");
    }

    public static void tick(MinecraftClient client) {
        syncClouds(client);
        if (client.player == null || client.world == null) {
            RINGS.clear();
            PARTICLES.clear();
            WET_TILES.clear();
            RAIN_DROPS.clear();
            RIPPLES.clear();
            wasGrounded = true;
            return;
        }
        clientTicks++;
        var player = client.player;
        boolean grounded = player.isOnGround();
        if (VisualSettings.get("jump_circle") && wasGrounded && !grounded && player.getVelocity().y > 0.02) {
            RINGS.add(new Ring(player.getX(), player.getY()+0.025, player.getZ()));
        }
        wasGrounded = grounded;

        Iterator<Ring> ringIterator = RINGS.iterator();
        while (ringIterator.hasNext()) {
            Ring ring = ringIterator.next();
            ring.age++;
            if (ring.age >= 28) ringIterator.remove();
        }

        Iterator<GlowParticle> particleIterator = PARTICLES.iterator();
        while (particleIterator.hasNext()) {
            GlowParticle p = particleIterator.next();
            p.prevX = p.x; p.prevY = p.y; p.prevZ = p.z;
            p.x += p.vx; p.y += p.vy; p.z += p.vz;
            p.vy += 0.0003;
            p.age++;
            if (p.age >= p.life) particleIterator.remove();
        }

        if (VisualSettings.get("ambient_particles") && clientTicks % Math.max(1, (int)(4-VisualSettings.slider("particle_density")*3)) == 0) {
            spawnAround(player, 1 + (int)(VisualSettings.slider("particle_density")*2));
            Entity targeted = client.targetedEntity;
            if (targeted instanceof LivingEntity living && living.isAlive()
                    && living.squaredDistanceTo(player) < 24*24) spawnAround(living, 2);
        }

        if (VisualSettings.get("wet_floor")) {
            if (surfaceScanRequested && surfaceScanDelay > 0) surfaceScanDelay--;
            if ((surfaceScanRequested && surfaceScanDelay <= 0) || client.world.getTime() - lastSurfaceScan > 40) {
                scanWetTiles(client);
                lastSurfaceScan = client.world.getTime();
                surfaceScanRequested = false;
                surfaceScanDelay = 0;
            }
            tickRain(client);
        } else {
            WET_TILES.clear();
            RAIN_DROPS.clear();
            RIPPLES.clear();
            surfaceScanRequested = false;
        }
        tickHudAnimations(client);
    }

    public static void requestSurfaceScan() {
        if (!surfaceScanRequested) {
            surfaceScanRequested = true;
            surfaceScanDelay = 2;
        }
    }

    private static void tickHudAnimations(MinecraftClient client) {
        if (client.player == null || client.options == null) return;
        boolean[] pressed = {
            client.options.forwardKey.isPressed(),
            client.options.leftKey.isPressed(),
            client.options.backKey.isPressed(),
            client.options.rightKey.isPressed(),
            client.options.attackKey.isPressed(),
            client.options.useKey.isPressed()
        };
        for (int i = 0; i < KEY_ANIMATION.length; i++) {
            KEY_TARGETS[i] = pressed[i] ? 1.0f : 0.0f;
            KEY_ANIMATION[i] += (KEY_TARGETS[i] - KEY_ANIMATION[i]) * 0.24f;
            if (Math.abs(KEY_TARGETS[i] - KEY_ANIMATION[i]) < 0.004f) KEY_ANIMATION[i] = KEY_TARGETS[i];
        }
        if (client.targetedEntity instanceof LivingEntity living && living.isAlive() && living != client.player) {
            lastHudTarget = living;
            targetHoldTicks = 16;
        } else if (targetHoldTicks > 0) {
            targetHoldTicks--;
        } else if (lastHudTarget != null && !lastHudTarget.isAlive()) {
            lastHudTarget = null;
        }
        float targetAlpha = lastHudTarget != null && targetHoldTicks > 0 && lastHudTarget.isAlive() ? 1.0f : 0.0f;
        targetHudAlpha += (targetAlpha - targetHudAlpha) * 0.22f;
        if (Math.abs(targetAlpha - targetHudAlpha) < 0.005f) targetHudAlpha = targetAlpha;
        if (lastHudTarget != null && lastHudTarget.isAlive()) {
            float hp = lastHudTarget.getMaxHealth() <= 0 ? 0 : Math.max(0, Math.min(1, lastHudTarget.getHealth() / lastHudTarget.getMaxHealth()));
            targetHealthSmooth += (hp - targetHealthSmooth) * 0.2f;
        } else {
            targetHealthSmooth += (0 - targetHealthSmooth) * 0.2f;
        }
    }

    private static void spawnAround(Entity entity, int amount) {
        for (int i=0;i<amount;i++) {
            double angle=RANDOM.nextDouble()*Math.PI*2;
            double radius=0.25+RANDOM.nextDouble()*0.75;
            double x=entity.getX()+Math.cos(angle)*radius;
            double y=entity.getY()+0.25+RANDOM.nextDouble()*Math.max(0.35,entity.getHeight());
            double z=entity.getZ()+Math.sin(angle)*radius;
            PARTICLES.add(new GlowParticle(x,y,z,(RANDOM.nextDouble()-.5)*0.006,0.006+RANDOM.nextDouble()*0.007,(RANDOM.nextDouble()-.5)*0.006,25+RANDOM.nextInt(28)));
        }
    }

    private static void scanWetTiles(MinecraftClient client) {
        WET_TILES.clear();
        var player = client.player;
        int chunks = client.options.getViewDistance().getValue();
        int radius = Math.max(64, Math.min(304, chunks * 16 + 20));
        int radiusSq = radius * radius;

        // Cover the visible area rather than stopping at a small player-centered puddle.
        // One heightmap query per column avoids scanning every Y level and stays more practical.
        // Plants/flowers above a solid block are permitted, so they no longer block reflections.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;
                int worldX = player.getBlockX() + dx;
                int worldZ = player.getBlockZ() + dz;
                BlockPos column = new BlockPos(worldX, player.getBlockY(), worldZ);
                BlockPos top = client.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
                BlockPos floor = top.down();
                BlockState state = client.world.getBlockState(floor);
                BlockState above = client.world.getBlockState(top);
                if (!state.isOpaqueFullCube() || above.isOpaqueFullCube()) continue;

                int stableTurn = Math.floorMod(worldX * 73428767 + worldZ * 912931 + top.getY() * 31337, 8);
                WET_TILES.add(new WetTile(worldX + 0.5, top.getY() + 0.012, worldZ + 0.5, stableTurn));
            }
        }
    }

    private static void syncClouds(MinecraftClient client) {
        if (client == null || client.options == null) return;
        boolean shouldHide = client.world != null && VisualSettings.get("sky_overlay");
        CloudRenderMode current = client.options.getCloudRenderModeValue();
        if (shouldHide) {
            if (!forcedCloudsOff) {
                previousCloudMode = current;
                forcedCloudsOff = true;
            }
            if (current != CloudRenderMode.OFF) {
                client.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            }
        } else if (forcedCloudsOff) {
            if (client.options.getCloudRenderModeValue() == CloudRenderMode.OFF && previousCloudMode != null) {
                client.options.getCloudRenderMode().setValue(previousCloudMode);
            }
            previousCloudMode = null;
            forcedCloudsOff = false;
        }
    }

    private static void tickRain(MinecraftClient client) {
        boolean raining = client.world.isRaining();
        if (raining && clientTicks % 2 == 0 && RAIN_DROPS.size() < 90) {
            var player = client.player;
            int radius = Math.max(3, Math.min(8, Math.round(VisualSettings.slider("puddle_radius"))));
            int spawnCount = 1 + RANDOM.nextInt(2);
            for (int i = 0; i < spawnCount && RAIN_DROPS.size() < 90; i++) {
                double x = player.getX() + (RANDOM.nextDouble() * 2.0 - 1.0) * radius;
                double z = player.getZ() + (RANDOM.nextDouble() * 2.0 - 1.0) * radius;
                BlockPos column = new BlockPos(net.minecraft.util.math.MathHelper.floor(x), player.getBlockY(), net.minecraft.util.math.MathHelper.floor(z));
                BlockPos top = client.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
                if (Math.abs(top.getY() - player.getY()) > 11.0) continue;
                if (!client.world.getBlockState(top.down()).isOpaqueFullCube() || !client.world.getBlockState(top).isAir()) continue;
                double targetY = top.getY() + 0.024;
                double startY = Math.max(player.getY() + 3.0, targetY + 3.0 + RANDOM.nextDouble() * 5.5);
                RAIN_DROPS.add(new RainDrop(x, startY, z, targetY, 0.24 + RANDOM.nextDouble() * 0.12));
            }
        }

        Iterator<RainDrop> drops = RAIN_DROPS.iterator();
        while (drops.hasNext()) {
            RainDrop drop = drops.next();
            drop.y -= drop.speed;
            drop.age++;
            if (drop.y <= drop.targetY) {
                if (VisualSettings.get("rain_ripples")) {
                    RIPPLES.add(new Ripple(drop.x, drop.targetY, drop.z, RANDOM.nextInt(360)));
                    if (RIPPLES.size() > 100) RIPPLES.remove(0);
                }
                drops.remove();
            } else if (drop.age > 70 || drop.y - drop.targetY > 14.0) {
                drops.remove();
            }
        }

        Iterator<Ripple> ripples = RIPPLES.iterator();
        while (ripples.hasNext()) {
            Ripple ripple = ripples.next();
            ripple.age++;
            if (ripple.age >= 18) ripples.remove();
        }
    }

    public static void renderWorld(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || context.matrixStack() == null || context.consumers() == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int accent = VisualSettings.accent();
        int ar = (accent >> 16) & 255, ag = (accent >> 8) & 255, ab = accent & 255;

        // The world renderer shares buffered consumers between layers. Requesting another layer can
        // flush the previous builder, so never reuse a VertexConsumer after switching layers.
        if (VisualSettings.get("entity_boxes")) {
            VertexConsumer boxLines = consumers.getBuffer(RenderLayer.getLines());
            double rangeSq = VisualSettings.slider("box_range") * VisualSettings.slider("box_range");
            for (Entity entity : client.world.getEntities()) {
                if (entity == client.player || !entity.isAlive() || entity.squaredDistanceTo(client.player) > rangeSq) continue;
                if (!(entity instanceof LivingEntity)) continue;
                drawRoundedBox(boxLines, matrix, entity.getBoundingBox(), camera, ar, ag, ab, entity instanceof PlayerEntity ? 235 : 170);
            }
        }

        // Text is a separate render layer, so draw all names only after outline vertices are done.
        if (VisualSettings.get("player_names")) {
            double rangeSq = VisualSettings.slider("box_range") * VisualSettings.slider("box_range");
            for (Entity entity : client.world.getEntities()) {
                if (entity == client.player || !(entity instanceof LivingEntity) || !entity.isAlive()) continue;
                if (entity.squaredDistanceTo(client.player) > rangeSq) continue;
                if (VisualSettings.get("entity_boxes") || entity instanceof PlayerEntity) {
                    drawName(entity, matrices, consumers, camera, client, ar, ag, ab);
                }
            }
        }

        if (VisualSettings.get("jump_circle") && !RINGS.isEmpty()) {
            // Emit all textured rings before switching to the line layer.
            for (Ring ring : RINGS) {
                float progress = (ring.age + tickDelta) / 28.0f;
                float radius = 0.22f + progress * VisualSettings.slider("jump_size");
                int alpha = (int) (220 * (1 - progress));
                VertexConsumer ringTexture = consumers.getBuffer(VisualRenderLayers.translucentNoDepthWrite(JUMP_RING));
                drawHorizontalTexture(ringTexture, matrix, ring.x - camera.x, ring.y - camera.y,
                    ring.z - camera.z, radius, radius, ar, ag, ab, alpha);
            }
            VertexConsumer ringLines = consumers.getBuffer(RenderLayer.getLines());
            for (Ring ring : RINGS) {
                float progress = (ring.age + tickDelta) / 28.0f;
                float radius = 0.22f + progress * VisualSettings.slider("jump_size");
                int alpha = (int) (220 * (1 - progress));
                drawRing(ringLines, matrix, ring.x - camera.x, ring.y - camera.y, ring.z - camera.z,
                    radius, ar, ag, ab, alpha, 48);
                if (progress < 0.5f) {
                    drawRing(ringLines, matrix, ring.x - camera.x, ring.y - camera.y + 0.004,
                        ring.z - camera.z, radius * 0.76f, 255, 255, 255,
                        (int) (100 * (1 - progress * 2)), 40);
                }
            }
        }

        if (VisualSettings.get("wet_floor") && !WET_TILES.isEmpty()) {
            float intensity = VisualSettings.slider("wet_intensity");
            long time = client.world.getTime();
            Identifier reflectedSky = VisualSettings.skyStyle() == 4 ? BLACK_HOLE_REFLECTIONS : SKY_TEXTURES[VisualSettings.skyStyle()];

            // The large bundled panorama is the only wet-floor texture: each face samples the
            // reflected sky with wave distortion and a Fresnel-like edge response. No blue overlay.
            VertexConsumer reflection = consumers.getBuffer(VisualRenderLayers.translucentNoDepthWrite(reflectedSky));
            for (WetTile tile : WET_TILES) {
                drawReflectionTile(reflection, matrix, tile.x, tile.y, tile.z, camera, intensity, time + tickDelta, ar, ag, ab);
            }
        }

        // The texture pass is complete before requesting line buffers; no consumer is reused across layers.
        if (VisualSettings.get("wet_floor") && (!RAIN_DROPS.isEmpty() || !RIPPLES.isEmpty())) {
            VertexConsumer wetLines = consumers.getBuffer(RenderLayer.getLines());
            for (RainDrop drop : RAIN_DROPS) {
                double rx = drop.x - camera.x;
                double ry = drop.y - camera.y;
                double rz = drop.z - camera.z;
                segment(wetLines, matrix, rx, ry, rz, rx - 0.012, ry - 0.17, rz - 0.012,
                    165 + ar * 90 / 255, 165 + ag * 90 / 255, 165 + ab * 90 / 255, 205);
            }
            float intensity = VisualSettings.slider("wet_intensity");
            for (Ripple ripple : RIPPLES) {
                float progress = ripple.age / 18.0f;
                double rx = ripple.x - camera.x;
                double ry = ripple.y - camera.y + 0.017;
                double rz = ripple.z - camera.z;
                float radius = 0.035f + progress * 0.22f;
                int alpha = (int) (185 * (1.0f - progress) * intensity);
                drawRing(wetLines, matrix, rx, ry, rz, radius, ar, ag, ab, alpha, 24);
                if (progress > 0.38f) {
                    drawRing(wetLines, matrix, rx, ry + 0.002, rz, radius * 0.58f,
                        240, 250, 255, (int) (80 * (1.0f - progress)), 16);
                }
            }
        }

        if (VisualSettings.get("ambient_particles") && !PARTICLES.isEmpty()) {
            for (GlowParticle particle : PARTICLES) {
                float fade = Math.max(0.0f, 1.0f - particle.age / (float) particle.life);
                double px = particle.prevX + (particle.x - particle.prevX) * tickDelta;
                double py = particle.prevY + (particle.y - particle.prevY) * tickDelta;
                double pz = particle.prevZ + (particle.z - particle.prevZ) * tickDelta;
                fade = Math.max(0.0f, 1.0f - (particle.age - 1 + tickDelta) / (float) particle.life);
                drawGlowBillboard(px - camera.x, py - camera.y, pz - camera.z,
                    0.035f + 0.045f * fade, ar, ag, ab, (int) (210 * fade), matrices, consumers, client);
            }
        }
        matrices.pop();
    }

    private static void drawName(Entity entity,MatrixStack matrices,VertexConsumerProvider consumers,Vec3d camera,MinecraftClient client,int r,int g,int b){
        matrices.push();
        matrices.translate(entity.getX()-camera.x,entity.getY()+entity.getHeight()+0.28-camera.y,entity.getZ()-camera.z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.024f,-0.024f,0.024f);
        double distance=Math.sqrt(entity.squaredDistanceTo(client.player));
        String raw=entity.getName().getString()+"  •  "+String.format(java.util.Locale.ROOT,"%.1fm",distance);
        Text label=Text.literal(raw).setStyle(Style.EMPTY.withFont(FONT));
        int width=client.textRenderer.getWidth(label);
        int labelColor = 0xFF000000 | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
        client.textRenderer.draw(label,-width/2f,0,labelColor,false,matrices.peek().getPositionMatrix(),consumers,
            TextRenderer.TextLayerType.SEE_THROUGH,0x80101D2A,LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }

    private static void drawRoundedBox(VertexConsumer out,Matrix4f m,Box b,Vec3d camera,int r,int g,int blue,int alpha){
        double x0=b.minX-camera.x,x1=b.maxX-camera.x,y0=b.minY-camera.y,y1=b.maxY-camera.y,z0=b.minZ-camera.z,z1=b.maxZ-camera.z;
        double[][] p={{x0,y0,z0},{x1,y0,z0},{x0,y1,z0},{x1,y1,z0},{x0,y0,z1},{x1,y0,z1},{x0,y1,z1},{x1,y1,z1}};
        int[][] edges={{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
        for(int[] e:edges)segment(out,m,p[e[0]][0],p[e[0]][1],p[e[0]][2],p[e[1]][0],p[e[1]][1],p[e[1]][2],r,g,blue,alpha);
        double radius=0.055;
        for(double[] c:p){
            circlePlane(out,m,c[0],c[1],c[2],radius,r,g,blue,alpha,12,0);
            circlePlane(out,m,c[0],c[1],c[2],radius,r,g,blue,alpha,12,1);
            circlePlane(out,m,c[0],c[1],c[2],radius,r,g,blue,alpha,12,2);
        }
    }

    private static void circlePlane(VertexConsumer out,Matrix4f m,double x,double y,double z,double radius,int r,int g,int b,int a,int points,int plane){
        double prevX=0,prevY=0,prevZ=0;
        for(int i=0;i<=points;i++){
            double angle=2*Math.PI*i/points;
            double u=Math.cos(angle)*radius,v=Math.sin(angle)*radius;
            double px=x+(plane==0?u:0), py=y+(plane==1?u:plane==0?v:0), pz=z+(plane==2?u:plane==1?v:0);
            if(i>0)segment(out,m,prevX,prevY,prevZ,px,py,pz,r,g,b,a);
            prevX=px;prevY=py;prevZ=pz;
        }
    }

    private static void drawRing(VertexConsumer out,Matrix4f m,double x,double y,double z,double radius,int r,int g,int b,int alpha,int points){
        if(alpha<=0)return;
        double lastX=x+radius,lastZ=z;
        for(int i=1;i<=points;i++){
            double a=(Math.PI*2*i)/points;
            double nx=x+Math.cos(a)*radius,nz=z+Math.sin(a)*radius;
            segment(out,m,lastX,y,lastZ,nx,y,nz,r,g,b,alpha);
            lastX=nx;lastZ=nz;
        }
    }

    private static void segment(VertexConsumer out,Matrix4f m,double x1,double y1,double z1,double x2,double y2,double z2,int r,int g,int b,int a){
        if(a<=0)return;
        out.vertex(m,(float)x1,(float)y1,(float)z1).color(r,g,b,a).normal(0,1,0);
        out.vertex(m,(float)x2,(float)y2,(float)z2).color(r,g,b,a).normal(0,1,0);
    }

    private static void drawHorizontalTexture(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                                               double rx, double rz, int r, int g, int b, int alpha) {
        out.vertex(matrix,(float)(x-rx),(float)y,(float)(z-rz)).color(r,g,b,alpha).texture(0,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x-rx),(float)y,(float)(z+rz)).color(r,g,b,alpha).texture(0,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x+rx),(float)y,(float)(z+rz)).color(r,g,b,alpha).texture(1,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x+rx),(float)y,(float)(z-rz)).color(r,g,b,alpha).texture(1,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
    }

    private static float clamp01(double value) {
        return (float)Math.max(0.0, Math.min(1.0, value));
    }

    private static float[] buildSinTable() {
        float[] table = new float[SIN_TABLE_SIZE];
        for (int i = 0; i < table.length; i++) {
            table[i] = (float)Math.sin(i * (Math.PI * 2.0 / SIN_TABLE_SIZE));
        }
        return table;
    }

    private static float fastSin(double angle) {
        double turns = angle / (Math.PI * 2.0);
        turns -= Math.floor(turns);
        double indexValue = turns * SIN_TABLE_SIZE;
        int index = (int)indexValue;
        int next = (index + 1) & (SIN_TABLE_SIZE - 1);
        float f = (float)(indexValue - index);
        return SIN_TABLE[index] + (SIN_TABLE[next] - SIN_TABLE[index]) * f;
    }

    private static float fastCos(double angle) {
        return fastSin(angle + Math.PI * 0.5);
    }

    private static float fract(double value) {
        return (float)(value - Math.floor(value));
    }

    private static void drawReflectionTile(VertexConsumer out, Matrix4f matrix, double centerX, double worldY, double centerZ,
                                           Vec3d camera, float intensity, double time, int ar, int ag, int ab) {
        double distanceXZ = Math.sqrt((centerX - camera.x) * (centerX - camera.x)
            + (centerZ - camera.z) * (centerZ - camera.z));
        int subdivisions = distanceXZ < 22 ? 4 : (distanceXZ < 48 ? 2 : 1);

        // Calculate the mirrored sky direction and its local gradient once per block, then
        // interpolate UVs across the face. The wave field itself is continuous in world space.
        double dx = centerX - camera.x;
        double dy = worldY - camera.y;
        double dz = centerZ - camera.z;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (distance < 0.001) return;
        double centerU = Math.atan2(dz, dx) / (Math.PI * 2.0) + time * 0.00008;
        double centerRy = -dy / distance;
        double centerV = Math.acos(Math.max(-1.0, Math.min(1.0, centerRy))) / Math.PI;
        double horizontalSq = Math.max(4.0, dx*dx + dz*dz);
        double duDx = -dz / (horizontalSq * Math.PI * 2.0);
        double duDz = dx / (horizontalSq * Math.PI * 2.0);
        double vDenominator = Math.max(0.015, distance*distance * Math.sqrt(Math.max(0.0004, 1.0 - centerRy*centerRy)) * Math.PI);
        double dvDx = centerRy * dx / vDenominator;
        double dvDz = centerRy * dz / vDenominator;

        double edgeStart = Math.max(0, Math.max(64, Math.min(304,
            MinecraftClient.getInstance().options.getViewDistance().getValue() * 16 + 20)) - 14);
        float edgeFade = (float)Math.max(0, Math.min(1, (edgeStart + 14 - distanceXZ) / 14.0));
        edgeFade = edgeFade * edgeFade * (3 - 2 * edgeFade);
        double viewCosine = Math.abs(dy) / distance;
        float fresnel = (float)(0.13 + Math.pow(1.0 - clamp01(viewCosine), 3.1) * 0.87);
        int alpha = (int)Math.max(0, Math.min(224,
            (28 + intensity * 45 + fresnel * (112 + intensity * 38)) * edgeFade));

        float waveStrength = VisualSettings.slider("reflection_wave");
        for (int iz = 0; iz < subdivisions; iz++) {
            for (int ix = 0; ix < subdivisions; ix++) {
                double x0 = centerX - 0.5 + ix / (double)subdivisions;
                double x1 = centerX - 0.5 + (ix + 1) / (double)subdivisions;
                double z0 = centerZ - 0.5 + iz / (double)subdivisions;
                double z1 = centerZ - 0.5 + (iz + 1) / (double)subdivisions;
                emitReflectionVertex(out, matrix, camera, centerX, centerZ, worldY, x0, z0, centerU, centerV,
                    duDx, duDz, dvDx, dvDz, time, intensity, waveStrength, alpha, ar, ag, ab);
                emitReflectionVertex(out, matrix, camera, centerX, centerZ, worldY, x0, z1, centerU, centerV,
                    duDx, duDz, dvDx, dvDz, time, intensity, waveStrength, alpha, ar, ag, ab);
                emitReflectionVertex(out, matrix, camera, centerX, centerZ, worldY, x1, z1, centerU, centerV,
                    duDx, duDz, dvDx, dvDz, time, intensity, waveStrength, alpha, ar, ag, ab);
                emitReflectionVertex(out, matrix, camera, centerX, centerZ, worldY, x1, z0, centerU, centerV,
                    duDx, duDz, dvDx, dvDz, time, intensity, waveStrength, alpha, ar, ag, ab);
            }
        }
    }

    private static void emitReflectionVertex(VertexConsumer out, Matrix4f matrix, Vec3d camera,
                                             double centerX, double centerZ, double baseY,
                                             double wx, double wz, double centerU, double centerV,
                                             double duDx, double duDz, double dvDx, double dvDz, double time,
                                             float intensity, float waveStrength, int alpha,
                                             int ar, int ag, int ab) {
        double waveA = fastSin(wx * 0.73 + wz * 0.37 + time * 0.075);
        double waveB = fastCos(wx * 0.29 - wz * 0.81 + time * 0.052);
        double crossWave = fastSin((wx + wz) * 0.91 - time * 0.047);
        double wy = baseY + (waveA * waveB * 0.0032 + crossWave * 0.0018) * waveStrength * intensity;

        double localX = wx - centerX;
        double localZ = wz - centerZ;
        double u = centerU + localX * duDx + localZ * duDz + waveA * 0.012 * waveStrength;
        double v = clamp01(centerV + localX * dvDx + localZ * dvDz
            + (waveB + crossWave) * 0.006 * waveStrength);
        int tintR = 242 + ar * 13 / 255;
        int tintG = 242 + ag * 13 / 255;
        int tintB = 242 + ab * 13 / 255;
        out.vertex(matrix, (float)(wx - camera.x), (float)(wy - camera.y), (float)(wz - camera.z))
            .color(tintR, tintG, tintB, alpha).texture((float)u, (float)v)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
    }

        public static void renderSkyAtmosphere(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualSettings.get("sky_overlay") || client.world == null || context.matrixStack() == null || context.consumers() == null) return;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        Identifier skyTexture = SKY_TEXTURES[VisualSettings.skyStyle()];
        VertexConsumer out = context.consumers().getBuffer(VisualRenderLayers.translucentNoDepthWrite(skyTexture));
        final int latitudeSteps = 24;
        final int longitudeSteps = 64;
        final float radius = 460.0f;
        long time = client.world.getTime();
        double phase = time * 0.000035;
        double animationTime = time + context.tickCounter().getTickDelta(false);
        int accent = VisualSettings.accent();
        int ar = (accent >> 16) & 255, ag = (accent >> 8) & 255, ab = accent & 255;

        for (int lat = 0; lat < latitudeSteps; lat++) {
            float v0 = lat / (float) latitudeSteps;
            float v1 = (lat + 1) / (float) latitudeSteps;
            float theta0 = (float) (Math.PI * v0);
            float theta1 = (float) (Math.PI * v1);
            for (int lon = 0; lon < longitudeSteps; lon++) {
                float u0 = (float)(lon / (double)longitudeSteps + phase);
                float u1 = (float)((lon + 1) / (double)longitudeSteps + phase);
                float phi0 = (float) (Math.PI * 2.0 * (lon / (double)longitudeSteps));
                float phi1 = (float) (Math.PI * 2.0 * ((lon + 1) / (double)longitudeSteps));

                emitSkyVertex(out, entry, matrix, radius, theta0, phi0, u0, v0, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi0, u0, v1, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi1, u1, v1, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta0, phi1, u1, v0, ar, ag, ab, time);

                // Double-sided sky dome avoids disappearing wedges on different graphics settings.
                emitSkyVertex(out, entry, matrix, radius, theta0, phi1, u1, v0, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi1, u1, v1, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi0, u0, v1, ar, ag, ab, time);
                emitSkyVertex(out, entry, matrix, radius, theta0, phi0, u0, v0, ar, ag, ab, time);
            }
        }

        // The black hole is rendered as a separate transparent panorama layer. Its accretion
        // disk spins and its position drifts independently from the background panorama.
        if (VisualSettings.skyStyle() == 4) {
            VertexConsumer object = context.consumers().getBuffer(VisualRenderLayers.translucentNoDepthWrite(BLACK_HOLE_OBJECT));
            double angle = animationTime * 0.018;
            double shiftU = Math.sin(animationTime * 0.0027) * 0.014;
            double shiftV = Math.sin(animationTime * 0.0019) * 0.006;
            double pulse = 1.0 + Math.sin(animationTime * 0.009) * 0.018;
            for (int lat = 0; lat < latitudeSteps; lat++) {
                float v0 = lat / (float) latitudeSteps;
                float v1 = (lat + 1) / (float) latitudeSteps;
                float theta0 = (float)(Math.PI * v0);
                float theta1 = (float)(Math.PI * v1);
                for (int lon = 0; lon < longitudeSteps; lon++) {
                    float u0 = (float)(lon / (double)longitudeSteps);
                    float u1 = (float)((lon + 1) / (double)longitudeSteps);
                    float phi0 = (float)(Math.PI * 2.0 * (lon / (double)longitudeSteps));
                    float phi1 = (float)(Math.PI * 2.0 * ((lon + 1) / (double)longitudeSteps));
                    emitSkyObjectVertex(object, entry, matrix, radius, theta0, phi0, u0, v0, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta1, phi0, u0, v1, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta1, phi1, u1, v1, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta0, phi1, u1, v0, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta0, phi1, u1, v0, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta1, phi1, u1, v1, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta1, phi0, u0, v1, angle, shiftU, shiftV, pulse);
                    emitSkyObjectVertex(object, entry, matrix, radius, theta0, phi0, u0, v0, angle, shiftU, shiftV, pulse);
                }
            }
        }
        matrices.pop();
    }

    private static void emitSkyObjectVertex(VertexConsumer out, MatrixStack.Entry entry, Matrix4f matrix,
                                             float radius, float theta, float phi, float u, float v,
                                             double angle, double shiftU, double shiftV, double pulse) {
        double centerU = 0.5 + shiftU;
        double centerV = 0.48 + shiftV;
        double du = u - centerU;
        while (du > 0.5) du -= 1.0;
        while (du < -0.5) du += 1.0;
        double x = du * 2.0;
        double y = v - centerV;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double objectScale = 0.34;
        double sampleU = 0.5 + (x * cos + y * sin) / (2.0 * pulse * objectScale);
        double sampleV = 0.48 + (-x * sin + y * cos) / (pulse * objectScale);
        sampleU -= Math.floor(sampleU);
        sampleV = clamp01(sampleV);

        float sphereSin = (float)Math.sin(theta);
        float xPos = radius * sphereSin * (float)Math.cos(phi);
        float yPos = radius * (float)Math.cos(theta);
        float zPos = radius * sphereSin * (float)Math.sin(phi);
        out.vertex(matrix, xPos, yPos, zPos).color(255,255,255,255)
            .texture((float)sampleU,(float)sampleV).overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, -xPos/radius, -yPos/radius, -zPos/radius);
    }

    private static void emitSkyVertex(VertexConsumer out, MatrixStack.Entry entry, Matrix4f matrix,
                                      float radius, float theta, float phi, float u, float v,
                                      int ar, int ag, int ab, long time) {
        float sin = (float)Math.sin(theta);
        float x = radius * sin * (float)Math.cos(phi);
        float y = radius * (float)Math.cos(theta);
        float z = radius * sin * (float)Math.sin(phi);
        float pulse = (float)(0.96 + 0.04 * Math.sin(time * 0.025 + theta * 2.0));
        int tintR = Math.min(255, (int)((150 + ar * 105 / 255) * pulse));
        int tintG = Math.min(255, (int)((150 + ag * 105 / 255) * pulse));
        int tintB = Math.min(255, (int)((150 + ab * 105 / 255) * pulse));
        out.vertex(matrix, x, y, z).color(tintR,tintG,tintB,255).texture(u,v)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, -x/radius, -y/radius, -z/radius);
    }

    private static void drawGlowBillboard(double x,double y,double z,float size,int r,int g,int b,int alpha,MatrixStack matrices,VertexConsumerProvider consumers,MinecraftClient client){
        matrices.push();
        matrices.translate(x,y,z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        VertexConsumer out=consumers.getBuffer(VisualRenderLayers.translucentNoDepthWrite(ORB));
        var entry=matrices.peek();
        Matrix4f m=entry.getPositionMatrix();
        out.vertex(m,-size,-size,0).color(r,g,b,alpha).texture(0,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry,0,0,1);
        out.vertex(m,-size,size,0).color(r,g,b,alpha).texture(0,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry,0,0,1);
        out.vertex(m,size,size,0).color(r,g,b,alpha).texture(1,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry,0,0,1);
        out.vertex(m,size,-size,0).color(r,g,b,alpha).texture(1,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry,0,0,1);
        matrices.pop();
    }

    public static void renderHotbar(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualSettings.get("custom_hotbar") || client.player == null || client.world == null || client.currentScreen != null) return;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int barW = 182;
        int barH = 22;
        int x = (screenW - barW) / 2;
        int y = screenH - barH;

        // This opaque base hides the vanilla hotbar art beneath our custom skin.
        fillRounded(context, x, y, barW, barH, 0xFF2F343C);
        context.drawTexture(RenderLayer::getGuiTextured, HOTBAR, x, y, 0, 0, barW, barH, 1456, 176, 1456, 176);

        // Nine matte graphite slots, drawn square rather than stretching pill-shaped art.
        for (int i = 0; i < 9; i++) {
            int sx = x + 1 + i * 20;
            context.drawTexture(RenderLayer::getGuiTextured, SLOT, sx, y + 1, 0, 0, 20, 20, 256, 256, 256, 256);
        }

        int selected = client.player.getInventory().selectedSlot;
        float target = x + 1 + selected * 20;
        if (Float.isNaN(smoothSlotX)) smoothSlotX = target;
        smoothSlotX += (target - smoothSlotX) * 0.32f;
        if (Math.abs(target - smoothSlotX) < 0.05f) smoothSlotX = target;
        context.drawTexture(RenderLayer::getGuiTextured, SELECTED, Math.round(smoothSlotX), y + 1,
            0, 0, 20, 20, 256, 256, 256, 256);

        // Align icons to vanilla's exact positions, avoiding duplicated/misaligned stacks.
        for (int i = 0; i < 9; i++) {
            int sx = x + 3 + i * 20;
            int sy = y + 3;
            var stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                context.drawItem(stack, sx, sy);
                context.drawStackOverlay(client.textRenderer, stack, sx, sy);
            }
        }
    }

    public static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualSettings.get("hud_enabled") || client.player == null || client.world == null || client.options.hudHidden) return;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int accent = VisualSettings.accent();
        int x = 10;
        int y = 10;

        if (VisualSettings.get("hud_watermark")) {
            drawHudCard(context, x, y, 146, 28, accent);
            context.drawTextWithShadow(client.textRenderer, hudText("ACIDVISUALS"), x + 10, y + 7, 0xFFF6FAFF);
            context.drawTextWithShadow(client.textRenderer, hudText("1.1"), x + 108, y + 7, accent);
        }

        List<String[]> stats = new ArrayList<>();
        if (VisualSettings.get("hud_fps")) stats.add(new String[]{"FPS", Integer.toString(client.getCurrentFps())});
        if (VisualSettings.get("hud_coords")) {
            var p = client.player.getPos();
            stats.add(new String[]{"XYZ", String.format(java.util.Locale.ROOT, "%.1f  %.1f  %.1f", p.x, p.y, p.z)});
        }
        if (VisualSettings.get("hud_speed")) {
            var velocity = client.player.getVelocity();
            double bps = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
            stats.add(new String[]{"BPS", String.format(java.util.Locale.ROOT, "%.2f", bps)});
        }
        if (VisualSettings.get("hud_direction")) {
            stats.add(new String[]{"DIR", client.player.getHorizontalFacing().asString().toUpperCase(java.util.Locale.ROOT)});
        }

        if (!stats.isEmpty()) {
            int cardY = VisualSettings.get("hud_watermark") ? y + 34 : y;
            int cardW = 212;
            for (String[] row : stats) {
                cardW = Math.max(cardW, 22 + client.textRenderer.getWidth(hudText(row[0])) + client.textRenderer.getWidth(hudText(row[1])) + 26);
            }
            cardW = Math.min(320, cardW);
            int cardH = 8 + stats.size() * 21;
            drawHudCard(context, x, cardY, cardW, cardH, accent);
            for (int i = 0; i < stats.size(); i++) {
                String[] row = stats.get(i);
                int rowY = cardY + 5 + i * 21;
                fillRounded(context, x + 5, rowY + 2, 3, 12, accent);
                context.drawTextWithShadow(client.textRenderer, hudText(row[0]), x + 13, rowY + 4, 0xFFAABBC8);
                String value = row[1];
                context.drawTextWithShadow(client.textRenderer, hudText(value),
                    x + cardW - 10 - client.textRenderer.getWidth(hudText(value)), rowY + 4, 0xFFF0F7FC);
                if (i < stats.size() - 1) fillRounded(context, x + 10, rowY + 18, cardW - 20, 1, 0x302D3948);
            }
        }

        if (VisualSettings.get("hud_target") && targetHudAlpha > 0.01f && lastHudTarget != null && lastHudTarget.isAlive()) {
            int tx = screenW - 204;
            int ty = 10;
            int tw = 194;
            int alpha = Math.max(0, Math.min(255, (int)(218 * targetHudAlpha)));
            fillRounded(context, tx, ty, tw, 62, withAlpha(0xFF161D28, alpha));
            fillRounded(context, tx, ty + 7, 3, 48, withAlpha(accent, alpha));
            fillRounded(context, tx + tw - 1, ty + 7, 1, 48, withAlpha(accent, alpha));
            String name = lastHudTarget.getName().getString();
            if (name.length() > 18) name = name.substring(0, 17) + "…";
            int textAlpha = Math.max(0, Math.min(255, (int)(255 * targetHudAlpha)));
            context.drawTextWithShadow(client.textRenderer, hudText(name), tx + 11, ty + 8, withAlpha(0xFFF5FAFF, textAlpha));
            String hp = String.format(java.util.Locale.ROOT, "%.1f / %.1f", lastHudTarget.getHealth(), lastHudTarget.getMaxHealth());
            context.drawTextWithShadow(client.textRenderer, hudText(hp),
                tx + tw - 10 - client.textRenderer.getWidth(hudText(hp)), ty + 8, withAlpha(0xFFD4E2ED, textAlpha));
            int barX = tx + 11, barY = ty + 34, barW = tw - 22;
            fillRounded(context, barX, barY, barW, 7, withAlpha(0xFF303744, textAlpha));
            fillRounded(context, barX, barY, Math.max(1, Math.round(barW * targetHealthSmooth)), 7, withAlpha(accent, textAlpha));
            context.drawTextWithShadow(client.textRenderer, hudText("TARGET"), tx + 11, ty + 46, withAlpha(0xFF91A6B6, textAlpha));
        }

        if (VisualSettings.get("hud_armor")) {
            int cardW = 78;
            int gap = 5;
            int groupWidth = 4 * cardW + 3 * gap;
            int armorX = (screenW - groupWidth) / 2;
            int armorY = screenH - 59;
            int index = 0;
            for (net.minecraft.item.ItemStack stack : client.player.getArmorItems()) {
                int slotX = armorX + index * (cardW + gap);
                drawHudCard(context, slotX, armorY, cardW, 31, accent);
                if (!stack.isEmpty()) context.drawItem(stack, slotX + 5, armorY + 6);
                String percent;
                int ratio;
                if (stack.isEmpty()) {
                    percent = "--";
                    ratio = 0;
                } else if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
                    percent = "100%";
                    ratio = 100;
                } else {
                    ratio = Math.max(0, Math.min(100, Math.round((stack.getMaxDamage() - stack.getDamage()) * 100.0f / stack.getMaxDamage())));
                    percent = ratio + "%";
                }
                int pctColor = ratio <= 20 && !stack.isEmpty() ? 0xFFFFA2A2 : accent;
                context.drawTextWithShadow(client.textRenderer, hudText(percent), slotX + 27, armorY + 4, pctColor);
                fillRounded(context, slotX + 27, armorY + 19, cardW - 33, 5, 0xFF28313C);
                int fillW = Math.round((cardW - 33) * ratio / 100.0f);
                if (fillW > 0) fillRounded(context, slotX + 27, armorY + 19, fillW, 5, pctColor);
                index++;
            }
        }

        if (VisualSettings.get("hud_keystrokes")) {
            int kx = 12;
            int ky = screenH - 74;
            drawHudKey(context, kx + 24, ky, "W", renderKeyAnimation(0), accent);
            drawHudKey(context, kx, ky + 24, "A", renderKeyAnimation(1), accent);
            drawHudKey(context, kx + 24, ky + 24, "S", renderKeyAnimation(2), accent);
            drawHudKey(context, kx + 48, ky + 24, "D", renderKeyAnimation(3), accent);
            drawHudKey(context, kx + 82, ky + 24, "L", renderKeyAnimation(4), accent);
            drawHudKey(context, kx + 106, ky + 24, "R", renderKeyAnimation(5), accent);
            context.drawTextWithShadow(client.textRenderer, hudText("KEYS"), kx + 3, ky + 52, 0xFF91A6B6);
        }
    }

    private static float renderKeyAnimation(int index) {
        float target = KEY_ANIMATION[index];
        KEY_RENDER_ANIMATION[index] += (target - KEY_RENDER_ANIMATION[index]) * 0.28f;
        if (Math.abs(target - KEY_RENDER_ANIMATION[index]) < 0.003f) KEY_RENDER_ANIMATION[index] = target;
        return KEY_RENDER_ANIMATION[index];
    }

    private static Text hudText(String value) {
        return Text.literal(value).setStyle(Style.EMPTY.withFont(FONT));
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static void drawHudCard(DrawContext context, int x, int y, int w, int h, int accent) {
        context.drawTexture(RenderLayer::getGuiTextured, HUD_CARD, x, y, 0, 0, w, h, 512, 128, 512, 128);
        fillRounded(context, x + 4, y + 5, 3, Math.max(4, h - 10), accent);
    }

    private static void fillRounded(DrawContext context, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int radius = Math.min(h / 2, w / 2);
        if (radius <= 0) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int dy = 0; dy < h; dy++) {
            double offsetY = dy < radius ? radius - dy - 0.5
                : (dy >= h - radius ? dy - (h - radius) + 0.5 : 0.0);
            int inset = offsetY == 0.0 ? 0 : Math.max(0, radius - (int)Math.floor(Math.sqrt(Math.max(0, radius*radius - offsetY*offsetY))));
            context.fill(x + inset, y + dy, x + w - inset, y + dy + 1, color);
        }
    }

    private static void drawRoundedBorder(DrawContext context, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) return;
        fillRounded(context, x, y, w, h, color);
        fillRounded(context, x + 1, y + 1, w - 2, h - 2, 0xD9161D28);
    }

    private static void drawHudKey(DrawContext context, int x, int y, String key, float intensity, int accent) {
        drawHudCard(context, x, y, 21, 21, accent);
        fillRounded(context, x + 1, y + 1, 19, 19, withAlpha(accent, (int)(130 * intensity)));
        int textColor = blendColor(0xFFF5FAFF, accent, intensity * 0.55f);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, hudText(key),
            x + 10, y + 6, textColor);
    }

    private static int blendColor(int from, int to, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int r = Math.round(((from >>> 16) & 255) * (1-t) + ((to >>> 16) & 255) * t);
        int g = Math.round(((from >>> 8) & 255) * (1-t) + ((to >>> 8) & 255) * t);
        int b = Math.round((from & 255) * (1-t) + (to & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static final class Ring {
        final double x,y,z;
        int age;
        Ring(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
    }
    private static final class GlowParticle {
        double x,y,z,vx,vy,vz;
        double prevX,prevY,prevZ;
        int age,life;
        GlowParticle(double x,double y,double z,double vx,double vy,double vz,int life){
            this.x=x;this.y=y;this.z=z;this.prevX=x;this.prevY=y;this.prevZ=z;
            this.vx=vx;this.vy=vy;this.vz=vz;this.life=life;
        }
    }
    private static final class WetTile {
        final double x, y, z;
        final int turn;
        WetTile(double x, double y, double z, int turn) {
            this.x = x; this.y = y; this.z = z; this.turn = turn;
        }
    }
    private static final class RainDrop {
        final double x, z, targetY, speed;
        double y;
        int age;
        RainDrop(double x, double y, double z, double targetY, double speed) {
            this.x = x; this.y = y; this.z = z; this.targetY = targetY; this.speed = speed;
        }
    }
    private static final class Ripple {
        final double x, y, z;
        final int seed;
        int age;
        Ripple(double x, double y, double z, int seed) {
            this.x = x; this.y = y; this.z = z; this.seed = seed;
        }
    }
}
