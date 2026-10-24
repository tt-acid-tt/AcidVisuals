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
    private static final Identifier PUDDLE = id("textures/fx/puddle");
    private static final Identifier SKY = id("textures/environment/sky_overlay");
    private static final Identifier FONT = id("acid");
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<GlowParticle> PARTICLES = new ArrayList<>();
    private static final List<Puddle> PUDDLES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static boolean wasGrounded = true;
    private static long lastPuddleScan = -1000;
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
            PUDDLES.clear();
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

        if (VisualSettings.get("wet_floor") && client.world.getTime() - lastPuddleScan > 60) {
            scanPuddles(client);
            lastPuddleScan = client.world.getTime();
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

    private static void scanPuddles(MinecraftClient client) {
        PUDDLES.clear();
        var player=client.player;
        int radius=Math.round(VisualSettings.slider("puddle_radius"));
        for(int dx=-radius;dx<=radius;dx++){
            for(int dz=-radius;dz<=radius;dz++){
                if(dx*dx+dz*dz>radius*radius || RANDOM.nextFloat()>0.16f) continue;
                BlockPos top=client.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,new BlockPos(player.getBlockX()+dx,player.getBlockY(),player.getBlockZ()+dz));
                if(Math.abs(top.getY()-player.getY())>3.4) continue;
                BlockPos floor=top.down();
                BlockState state=client.world.getBlockState(floor);
                if(state.isOpaqueFullCube() && client.world.getBlockState(top).isAir()){
                    PUDDLES.add(new Puddle(top.getX()+0.5,top.getY()+0.018,top.getZ()+0.5,RANDOM.nextInt(120)));
                    if(PUDDLES.size()>=42)return;
                }
            }
        }
    }

    public static void renderWorld(WorldRenderContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.player==null||client.world==null||context.matrixStack()==null||context.consumers()==null)return;
        MatrixStack matrices=context.matrixStack();
        VertexConsumerProvider consumers=context.consumers();
        Vec3d camera=context.camera().getPos();
        matrices.push();
        Matrix4f matrix=matrices.peek().getPositionMatrix();
        int accent=VisualSettings.accent();
        int ar=(accent>>16)&255, ag=(accent>>8)&255, ab=accent&255;

        if(VisualSettings.get("entity_boxes")){
            VertexConsumer lines=consumers.getBuffer(RenderLayer.getLines());
            double range=VisualSettings.slider("box_range");
            double rangeSq=range*range;
            for(Entity entity:client.world.getEntities()){
                if(entity==client.player||!entity.isAlive()||entity.squaredDistanceTo(client.player)>rangeSq)continue;
                if(!(entity instanceof LivingEntity))continue;
                drawRoundedBox(lines,matrix,entity.getBoundingBox(),camera,ar,ag,ab,entity instanceof PlayerEntity?235:170);
                if(VisualSettings.get("player_names"))drawName(entity,matrices,consumers,camera,client,ar,ag,ab);
            }
        } else if(VisualSettings.get("player_names")) {
            for(Entity entity:client.world.getEntities()){
                if(entity==client.player||!(entity instanceof PlayerEntity)||!entity.isAlive())continue;
                if(entity.squaredDistanceTo(client.player)<VisualSettings.slider("box_range")*VisualSettings.slider("box_range")) {
                    drawName(entity,matrices,consumers,camera,client,ar,ag,ab);
                }
            }
        }

        if(VisualSettings.get("jump_circle")){
            VertexConsumer lines=consumers.getBuffer(RenderLayer.getLines());
            for(Ring ring:RINGS){
                float progress=ring.age/28.0f;
                float radius=(0.22f+progress*VisualSettings.slider("jump_size"));
                int alpha=(int)(220*(1-progress));
                VertexConsumer ringTexture=consumers.getBuffer(RenderLayer.getEntityTranslucent(JUMP_RING));
                drawHorizontalTexture(ringTexture,matrix,ring.x-camera.x,ring.y-camera.y,ring.z-camera.z,radius,radius,alpha);
                drawRing(lines,matrix,ring.x-camera.x,ring.y-camera.y,ring.z-camera.z,radius,ar,ag,ab,alpha,48);
                if(progress<0.5f)drawRing(lines,matrix,ring.x-camera.x,ring.y-camera.y+0.004,ring.z-camera.z,radius*0.76f,255,255,255,(int)(100*(1-progress*2)),40);
            }
        }

        if(VisualSettings.get("wet_floor")){
            VertexConsumer surface=consumers.getBuffer(RenderLayer.getEntityTranslucent(PUDDLE));
            VertexConsumer lines=consumers.getBuffer(RenderLayer.getLines());
            float intensity=VisualSettings.slider("wet_intensity");
            long time=client.world.getTime();
            int alpha=(int)((VisualSettings.get("soft_lighting")?65:40)+intensity*(VisualSettings.get("soft_lighting")?165:120));
            int index=0;
            for(Puddle puddle:PUDDLES){
                float size=0.42f+(index%4)*0.085f;
                drawHorizontalTexture(surface,matrix,puddle.x-camera.x,puddle.y-camera.y,puddle.z-camera.z,size,size*0.68f,alpha);
                if(VisualSettings.get("rain_ripples")&&client.world.isRaining()){
                    float phase=((time+puddle.seed)%24)/24.0f;
                    float radius=0.08f+phase*size*0.82f;
                    int waveAlpha=(int)(175*(1-phase)*intensity);
                    drawRing(lines,matrix,puddle.x-camera.x,puddle.y-camera.y+0.007,puddle.z-camera.z,radius,ar,ag,ab,waveAlpha,28);
                    if(phase>0.45f)drawRing(lines,matrix,puddle.x-camera.x,puddle.y-camera.y+0.009,puddle.z-camera.z,radius*0.54f,220,246,255,(int)(75*(1-phase)),24);
                } else if(index%3==0) {
                    drawRing(lines,matrix,puddle.x-camera.x,puddle.y-camera.y+0.006,puddle.z-camera.z,size*0.43,205,242,255,(int)(70*intensity),30);
                }
                index++;
            }
        }

        if(VisualSettings.get("ambient_particles")){
            for(GlowParticle p:PARTICLES){
                float fade=Math.max(0.0f,1.0f-p.age/(float)p.life);
                drawGlowBillboard(p.x-camera.x,p.y-camera.y,p.z-camera.z,0.035f+0.045f*fade,ar,ag,ab,(int)(210*fade),matrices,consumers,client);
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
        MinecraftClient client=MinecraftClient.getInstance();
        if(!VisualSettings.get("custom_hotbar")||client.player==null||client.world==null||client.currentScreen!=null)return;
        int screenW=context.getScaledWindowWidth(), screenH=context.getScaledWindowHeight();
        int barW=208,barH=29,x=(screenW-barW)/2,y=screenH-34;
        context.drawTexture(RenderLayer::getGuiTextured,HOTBAR,x,y,0,0,barW,barH,1024,144);
        int selected=client.player.getInventory().selectedSlot;
        float target=x+2+selected*22;
        if(Float.isNaN(smoothSlotX))smoothSlotX=target;
        smoothSlotX+=(target-smoothSlotX)*0.28f;
        context.drawTexture(RenderLayer::getGuiTextured,SELECTED,(int)smoothSlotX,y+1,0,0,26,27,256,128);
        for(int i=0;i<9;i++){
            int sx=x+4+i*22,sy=y+4;
            context.drawTexture(RenderLayer::getGuiTextured,SLOT,sx,sy,0,0,20,20,128,128);
            var stack=client.player.getInventory().getStack(i);
            if(!stack.isEmpty()){
                context.drawItem(stack,sx+2,sy+2);
                context.drawStackOverlay(client.textRenderer,stack,sx+2,sy+2);
            }
        }
    }

    public static void renderSkyOverlay(DrawContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(!VisualSettings.get("sky_overlay")||client.player==null||client.world==null||client.currentScreen!=null)return;
        int w=context.getScaledWindowWidth();
        int h=Math.max(90,context.getScaledWindowHeight()/3);
        context.drawTexture(RenderLayer::getGuiTextured,SKY,0,0,w,h,0,0,2048,1024);
    }

    private static final class Ring {
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
    private static final class Puddle {
        final double x,y,z;
        final int seed;
        Puddle(double x,double y,double z,int seed){this.x=x;this.y=y;this.z=z;this.seed=seed;}
    }
}
