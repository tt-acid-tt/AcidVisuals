package tt.acid.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    private static final Identifier ORB = id("gui/orb");

    public GodScreen() { super(Text.literal("GOD OF THE WORLD")); }
    private static Identifier id(String path) { return Identifier.of("god_of_the_world", "textures/" + path + ".png"); }

    @Override protected void init() {
        clearChildren();
        int panelW = Math.min(500, width - 24);
        int left = (width - panelW) / 2 + 20;
        int top = height / 2 - 105;
        int w = (panelW - 52) / 2;
        int h = 32;
        int gap = 12;

        addDrawableChild(toggle("Kill Aura • критические удары", () -> GodSettings.killAura,
                v -> { GodSettings.killAura = v; KillAuraSettings.enabled = v; }, left, top, w, h));
        addDrawableChild(toggle("ESP • подсветка целей", () -> GodSettings.esp,
                v -> GodSettings.esp = v, left+w+gap, top, w, h));
        addDrawableChild(toggle("Безопасный полёт", () -> GodSettings.flight,
                v -> GodSettings.flight = v, left, top+42, w, h));
        addDrawableChild(toggle("Бессмертие", () -> GodSettings.invulnerability,
                v -> GodSettings.invulnerability = v, left+w+gap, top+42, w, h));
        addDrawableChild(toggle("Ответный удар", () -> GodSettings.retaliation,
                v -> GodSettings.retaliation = v, left, top+84, w, h));
        addDrawableChild(new ArtButton(width/2-75, top+132, 150, h, Text.literal("Закрыть"),
                b -> close(), () -> BUTTON));
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
            super(x,y,w,h,text,action,supplier -> supplier.get());
            this.art=art;
        }
        @Override public void renderWidget(DrawContext ctx,int mouseX,int mouseY,float delta) {
            Identifier current = art.get();
            Identifier texture = isHovered() && current.equals(BUTTON) ? BUTTON_HOVER : current;
            boolean toggle = texture.equals(TOGGLE_ON)||texture.equals(TOGGLE_OFF);
            GodScreen.texture(ctx,texture,getX(),getY(),width,height,toggle?256:512,toggle?128:96);
            int color = active ? 0xFFF2FAFF : 0xFF8996A8;
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,getMessage(),getX()+width/2,getY()+(height-8)/2,color);
        }
    }

    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        context.fill(0,0,width,height,0xB9060A12);
        int panelW=Math.min(500,width-24), panelH=230;
        int x=(width-panelW)/2,y=height/2-116;
        texture(context,PANEL,x,y,panelW,panelH,1024,768);
        texture(context,ORB,x+panelW-44,y+10,24,24,256,256);
        context.drawCenteredTextWithShadow(textRenderer,"GOD OF THE WORLD",width/2,y+14,0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,"COMBAT  •  MOVEMENT  •  DEFENCE",width/2,y+29,0xFF9BDCEC);
        super.render(context,mouseX,mouseY,delta);
        context.drawCenteredTextWithShadow(textRenderer,"Только пять функций. Без команд Minecraft.",width/2,y+panelH-14,0xFFB8C7D8);
    }
    @Override public boolean shouldPause(){return false;}
}
