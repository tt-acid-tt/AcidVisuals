package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GodScreen extends Screen {
    private static final Identifier PANEL = id("gui/panel");
    private static final Identifier BUTTON = id("gui/button");
    private static final Identifier BUTTON_HOVER = id("gui/button_hover");
    private static final Identifier TOGGLE_ON = id("gui/toggle_on");
    private static final Identifier TOGGLE_OFF = id("gui/toggle_off");
    private static final Identifier SLIDER_TRACK = id("gui/slider_track");
    private static final Identifier SLIDER_THUMB = id("gui/slider_thumb");
    private static final Identifier ORB = id("gui/orb");

    public GodScreen() { super(Text.literal("GOD OF THE WORLD")); }
    private static Identifier id(String path) { return Identifier.of("god_of_the_world", "textures/" + path + ".png"); }

    @Override protected void init() {
        clearChildren();
        int panelW = Math.min(540, width - 24);
        int left = (width - panelW) / 2 + 22;
        int top = height / 2 - 142;
        int gap = 12;
        int w = (panelW - 56) / 2;
        int h = 30;

        addDrawableChild(toggle("Полёт без урона", () -> GodSettings.flight, v -> GodSettings.flight = v, left, top, w, h));
        addDrawableChild(toggle("Бессмертие", () -> GodSettings.invulnerability, v -> GodSettings.invulnerability = v, left+w+gap, top, w, h));
        addDrawableChild(toggle("Kill Aura", () -> GodSettings.killAura, v -> GodSettings.killAura = v, left, top+38, w, h));
        addDrawableChild(toggle("Ответный удар", () -> GodSettings.retaliation, v -> GodSettings.retaliation = v, left+w+gap, top+38, w, h));
        addDrawableChild(toggle("Home Lantern • B", () -> GodSettings.homeLantern, v -> GodSettings.homeLantern = v, left, top+76, w, h));
        addDrawableChild(toggle("Лазеры из глаз • G", () -> GodSettings.eyeLaser, v -> GodSettings.eyeLaser = v, left+w+gap, top+76, w, h));
        addDrawableChild(toggle("Остановка времени", () -> GodSettings.timeStop, v -> GodSettings.timeStop = v, left, top+114, w, h));
        addDrawableChild(toggle("Телекинез • H", () -> GodSettings.telekinesis, v -> GodSettings.telekinesis = v, left+w+gap, top+114, w, h));
        addDrawableChild(toggle("Jump Circle", () -> GodSettings.jumpCircle, v -> GodSettings.jumpCircle = v, left, top+152, w, h));
        addDrawableChild(toggle("Wet Floor", () -> GodSettings.wetFloor, v -> GodSettings.wetFloor = v, left+w+gap, top+152, w, h));

        addDrawableChild(new ArtButton(left, top+192, w, h, Text.literal("Небо: "+skyName()), b -> {
            GodSettings.skyPreset=(GodSettings.skyPreset+1)%4;
            b.setMessage(Text.literal("Небо: "+skyName()));
        }, () -> BUTTON));
        addDrawableChild(new ArtSlider(left+w+gap, top+192, w, h));
        addDrawableChild(new ArtButton(width/2-75, top+232, 150, h, Text.literal("Закрыть"), b -> close(), () -> BUTTON));
    }

    private String skyName() {
        return switch (GodSettings.skyPreset) { case 1 -> "Aurora"; case 2 -> "Void"; case 3 -> "Sunset"; default -> "Default"; };
    }

    private ArtButton toggle(String label, BooleanSupplier get, Consumer<Boolean> set, int x, int y, int w, int h) {
        return new ArtButton(x,y,w,h,Text.literal(label+"   "+(get.getAsBoolean()?"ON":"OFF")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Text.literal(label+"   "+(get.getAsBoolean()?"ON":"OFF")));
        }, () -> get.getAsBoolean()?TOGGLE_ON:TOGGLE_OFF);
    }

    private static void texture(DrawContext ctx, Identifier id, int x, int y, int w, int h, int tw, int th) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, w, h, tw, th);
    }

    private static final class ArtButton extends ButtonWidget {
        private final Supplier<Identifier> art;
        ArtButton(int x,int y,int w,int h,Text text,PressAction action,Supplier<Identifier> art) {
            super(x,y,w,h,text,action,DEFAULT_NARRATION); this.art=art;
        }
        @Override protected void renderWidget(DrawContext ctx,int mouseX,int mouseY,float delta) {
            Identifier texture = isHovered() && art.get().equals(BUTTON) ? BUTTON_HOVER : art.get();
            int tw = texture.equals(TOGGLE_ON)||texture.equals(TOGGLE_OFF) ? 256 : 512;
            int th = texture.equals(TOGGLE_ON)||texture.equals(TOGGLE_OFF) ? 128 : 96;
            GodScreen.texture(ctx,texture,getX(),getY(),width,height,tw,th);
            int color = active ? 0xFFF2FAFF : 0xFF8996A8;
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),getX()+width/2,getY()+(height-8)/2,color);
        }
    }

    private static final class ArtSlider extends SliderWidget {
        ArtSlider(int x,int y,int w,int h) {
            super(x,y,w,h,Text.literal("Радиус: "+(int)GodSettings.killRange),(GodSettings.killRange-4.0)/60.0);
        }
        @Override protected void updateMessage() { setMessage(Text.literal("Радиус: "+(int)GodSettings.killRange)); }
        @Override protected void applyValue() { GodSettings.killRange=(float)(4.0+value*60.0); }
        @Override protected void renderWidget(DrawContext ctx,int mouseX,int mouseY,float delta) {
            GodScreen.texture(ctx,SLIDER_TRACK,getX(),getY()+height/4,width,height/2,1024,96);
            int thumbX=getX()+(int)(value*(width-16));
            GodScreen.texture(ctx,SLIDER_THUMB,thumbX,getY()+height/2-8,16,16,128,128);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),getX()+width/2,getY()+2,0xFFF2FAFF);
        }
    }

    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        context.fill(0,0,width,height,0xB9060A12);
        int panelW=Math.min(540,width-24), panelH=292;
        int x=(width-panelW)/2,y=height/2-153;
        texture(context,PANEL,x,y,panelW,panelH,1024,768);
        texture(context,ORB,x+panelW-50,y+12,28,28,256,256);
        context.drawCenteredTextWithShadow(textRenderer,"GOD OF THE WORLD",width/2,y+15,0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,"CUSTOM HIGH-RESOLUTION INTERFACE",width/2,y+30,0xFF9BDCEC);
        super.render(context,mouseX,mouseY,delta);
        context.drawCenteredTextWithShadow(textRenderer,"G — лазер   H — телекинез   B — Home Lantern",width/2,y+panelH-15,0xFFB8C7D8);
    }
    @Override public boolean shouldPause(){return false;}
}
