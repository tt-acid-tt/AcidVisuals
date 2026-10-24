package tt.acid.visuals;

// Client-side visuals; rendering effects are local and require no server commands.

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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
    private static final Identifier WET_SURFACE = id("textures/fx/wet_surface");
    private static final Identifier SKY = id("textures/environment/sky_overlay");
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

    private VisualRenderer() {}

    private static Identifier id(String path) {
        if (path.equals("acid")) return Identifier.of("acidvisuals", "acid");
        return Identifier.of("acidvisuals", path + ".png");
    }

    public static void tick(MinecraftClient client) {
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
            if (client.world.getTime() - lastSurfaceScan > 40) {
                scanWetTiles(client);
                lastSurfaceScan = client.world.getTime();
            }
            tickRain(client);
        } else {
            WET_TILES.clear();
            RAIN_DROPS.clear();
            RIPPLES.clear();
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
        int radius = Math.round(VisualSettings.slider("puddle_radius"));
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;
                BlockPos column = new BlockPos(player.getBlockX() + dx, player.getBlockY(), player.getBlockZ() + dz);
                BlockPos top = client.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
                if (Math.abs(top.getY() - player.getY()) > 9.0) continue;
                BlockPos floor = top.down();
                BlockState state = client.world.getBlockState(floor);
                if (!state.isOpaqueFullCube() || !client.world.getBlockState(top).isAir()) continue;

                int stableTurn = Math.floorMod(top.getX() * 73428767 + top.getZ() * 912931, 4);
                WET_TILES.add(new WetTile(top.getX() + 0.5, top.getY() + 0.012, top.getZ() + 0.5, stableTurn));
                if (WET_TILES.size() >= 380) return;
            }
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
                float progress = ring.age / 28.0f;
                float radius = 0.22f + progress * VisualSettings.slider("jump_size");
                int alpha = (int) (220 * (1 - progress));
                VertexConsumer ringTexture = consumers.getBuffer(RenderLayer.getEntityTranslucent(JUMP_RING));
                drawHorizontalTexture(ringTexture, matrix, ring.x - camera.x, ring.y - camera.y,
                    ring.z - camera.z, radius, radius, alpha);
            }
            VertexConsumer ringLines = consumers.getBuffer(RenderLayer.getLines());
            for (Ring ring : RINGS) {
                float progress = ring.age / 28.0f;
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
            VertexConsumer surface = consumers.getBuffer(RenderLayer.getEntityTranslucent(WET_SURFACE));
            int index = 0;
            for (WetTile tile : WET_TILES) {
                int alpha = 178 + (int) (intensity * 65) + (tile.turn % 2) * 8;
                drawWetTile(surface, matrix, tile.x - camera.x, tile.y - camera.y,
                    tile.z - camera.z, alpha, tile.turn);
                index++;
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
                    194, 232, 255, 190);
            }
            float intensity = VisualSettings.slider("wet_intensity");
            for (Ripple ripple : RIPPLES) {
                float progress = ripple.age / 18.0f;
                double rx = ripple.x - camera.x;
                double ry = ripple.y - camera.y + 0.017;
                double rz = ripple.z - camera.z;
                float radius = 0.035f + progress * 0.22f;
                int alpha = (int) (185 * (1.0f - progress) * intensity);
                drawRing(wetLines, matrix, rx, ry, rz, radius, 204, 240, 255, alpha, 20);
                if (progress > 0.38f) {
                    drawRing(wetLines, matrix, rx, ry + 0.002, rz, radius * 0.58f,
                        240, 250, 255, (int) (80 * (1.0f - progress)), 16);
                }
            }
        }

        if (VisualSettings.get("ambient_particles") && !PARTICLES.isEmpty()) {
            for (GlowParticle particle : PARTICLES) {
                float fade = Math.max(0.0f, 1.0f - particle.age / (float) particle.life);
                drawGlowBillboard(particle.x - camera.x, particle.y - camera.y, particle.z - camera.z,
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
        client.textRenderer.draw(label,-width/2f,0,0xFFFFFFFF,false,matrices.peek().getPositionMatrix(),consumers,
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

    private static void drawHorizontalTexture(VertexConsumer out,Matrix4f matrix,double x,double y,double z,double rx,double rz,int alpha){
        out.vertex(matrix,(float)(x-rx),(float)y,(float)(z-rz)).color(255,255,255,alpha).texture(0,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x-rx),(float)y,(float)(z+rz)).color(255,255,255,alpha).texture(0,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x+rx),(float)y,(float)(z+rz)).color(255,255,255,alpha).texture(1,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        out.vertex(matrix,(float)(x+rx),(float)y,(float)(z-rz)).color(255,255,255,alpha).texture(1,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
    }

    private static void drawWetTile(VertexConsumer out, Matrix4f matrix, double x, double y, double z, int alpha, int rotation) {
        double x0 = x - 0.5, x1 = x + 0.5, z0 = z - 0.5, z1 = z + 0.5;
        float[][] uv = switch (rotation & 3) {
            case 1 -> new float[][]{{0,1},{1,1},{1,0},{0,0}};
            case 2 -> new float[][]{{1,1},{1,0},{0,0},{0,1}};
            case 3 -> new float[][]{{1,0},{0,0},{0,1},{1,1}};
            default -> new float[][]{{0,0},{0,1},{1,1},{1,0}};
        };
        double[][] p = {{x0,y,z0},{x0,y,z1},{x1,y,z1},{x1,y,z0}};
        for (int i = 0; i < 4; i++) {
            out.vertex(matrix, (float)p[i][0], (float)p[i][1], (float)p[i][2])
                .color(255,255,255,alpha).texture(uv[i][0],uv[i][1])
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,1,0);
        }
    }

    public static void renderSkyAtmosphere(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualSettings.get("sky_overlay") || client.world == null || context.matrixStack() == null || context.consumers() == null) return;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer out = context.consumers().getBuffer(RenderLayer.getEntityTranslucent(SKY));
        final int latitudeSteps = 24;
        final int longitudeSteps = 64;
        final float radius = 480.0f;

        for (int lat = 0; lat < latitudeSteps; lat++) {
            float v0 = lat / (float) latitudeSteps;
            float v1 = (lat + 1) / (float) latitudeSteps;
            float theta0 = (float) (Math.PI * v0);
            float theta1 = (float) (Math.PI * v1);
            for (int lon = 0; lon < longitudeSteps; lon++) {
                float u0 = lon / (float) longitudeSteps;
                float u1 = (lon + 1) / (float) longitudeSteps;
                float phi0 = (float) (Math.PI * 2.0 * u0);
                float phi1 = (float) (Math.PI * 2.0 * u1);

                emitSkyVertex(out, entry, matrix, radius, theta0, phi0, u0, v0);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi0, u0, v1);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi1, u1, v1);
                emitSkyVertex(out, entry, matrix, radius, theta0, phi1, u1, v0);

                // Reverse faces as well: this remains visible with either culling convention.
                emitSkyVertex(out, entry, matrix, radius, theta0, phi1, u1, v0);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi1, u1, v1);
                emitSkyVertex(out, entry, matrix, radius, theta1, phi0, u0, v1);
                emitSkyVertex(out, entry, matrix, radius, theta0, phi0, u0, v0);
            }
        }
        matrices.pop();
    }

    private static void emitSkyVertex(VertexConsumer out, MatrixStack.Entry entry, Matrix4f matrix,
                                      float radius, float theta, float phi, float u, float v) {
        float sin = (float) Math.sin(theta);
        float x = radius * sin * (float) Math.cos(phi);
        float y = radius * (float) Math.cos(theta);
        float z = radius * sin * (float) Math.sin(phi);
        out.vertex(matrix, x, y, z).color(255,255,255,255).texture(u,v)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(entry, -x/radius, -y/radius, -z/radius);
    }

    private static void drawGlowBillboard(double x,double y,double z,float size,int r,int g,int b,int alpha,MatrixStack matrices,VertexConsumerProvider consumers,MinecraftClient client){
        matrices.push();
        matrices.translate(x,y,z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        VertexConsumer out=consumers.getBuffer(RenderLayer.getEntityTranslucent(ORB));
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
        context.fill(x, y, x + barW, y + barH, 0xFF081321);
        context.drawTexture(RenderLayer::getGuiTextured, HOTBAR, x, y, 0, 0, barW, barH, 1456, 176);

        // Nine matte graphite slots, drawn square rather than stretching pill-shaped art.
        for (int i = 0; i < 9; i++) {
            int sx = x + 1 + i * 20;
            context.drawTexture(RenderLayer::getGuiTextured, SLOT, sx, y + 1, 0, 0, 20, 20, 256, 256);
        }

        int selected = client.player.getInventory().selectedSlot;
        float target = x + 1 + selected * 20;
        if (Float.isNaN(smoothSlotX)) smoothSlotX = target;
        smoothSlotX += (target - smoothSlotX) * 0.32f;
        if (Math.abs(target - smoothSlotX) < 0.05f) smoothSlotX = target;
        context.drawTexture(RenderLayer::getGuiTextured, SELECTED, Math.round(smoothSlotX), y + 1,
            0, 0, 20, 20, 256, 256);

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

    private static final class Ring {    private static final class Ring {
        final double x,y,z;
        int age;
        Ring(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
    }
    private static final class GlowParticle {
        double x,y,z,vx,vy,vz;
        int age,life;
        GlowParticle(double x,double y,double z,double vx,double vy,double vz,int life){
            this.x=x;this.y=y;this.z=z;this.vx=vx;this.vy=vy;this.vz=vz;this.life=life;
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
