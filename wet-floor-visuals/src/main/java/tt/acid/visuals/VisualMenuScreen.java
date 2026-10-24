package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class VisualMenuScreen extends Screen {
    private static final Identifier PANEL = id("gui/panel");
    private static final Identifier BUTTON = id("gui/button");
    private static final Identifier BUTTON_HOVER = id("gui/button_hover");
    private static final Identifier TOGGLE_ON = id("gui/toggle_on");
    private static final Identifier TOGGLE_OFF = id("gui/toggle_off");
    private static final Identifier FONT = Identifier.of("acidvisuals", "acid");
    private static final String[] TABS = {"Визуал", "Wet Floor", "HUD и мир", "Конфиги"};
    private int activeTab;
    private String draggingSlider;

    public VisualMenuScreen() {
        super(Text.literal("AcidVisuals"));
    }

    private static Identifier id(String path) {
        return Identifier.of("acidvisuals", "textures/" + path + ".png");
    }

    private Text fontText(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(FONT));
    }

    private void texture(DrawContext context, Identifier texture, int x, int y, int w, int h, int tw, int th) {
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0, 0, w, h, tw, th);
    }

    private int px() { return (width - Math.min(790, width - 24)) / 2; }
    private int py() { return (height - Math.min(480, height - 20)) / 2; }
    private int pw() { return Math.min(790, width - 24); }
    private int ph() { return Math.min(480, height - 20); }
    private int contentX() { return px() + 216; }
    private int contentW() { return pw() - 240; }
    private int contentY() { return py() + 82; }
    private int sliderOffsetRows() { return switch(activeTab) { case 0 -> 3; case 1 -> 4; case 2 -> 2; default -> 0; }; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB7040912);
        texture(context, PANEL, px(), py(), pw(), ph(), 1024, 768);

        context.drawTextWithShadow(textRenderer, fontText("ACIDVISUALS"), px()+28, py()+21, 0xFFF3FCFF);
        context.drawTextWithShadow(textRenderer, fontText("CLIENT VISUAL SUITE  /  FABRIC 1.21.4"), px()+30, py()+43, 0xFF91DDEB);

        int paletteX = px()+pw()-154;
        context.drawTextWithShadow(textRenderer, fontText("ЦВЕТ"), paletteX-34, py()+25, 0xFFA9C4D5);
        for (int i=0; i<VisualSettings.colorCount(); i++) {
            int sx = paletteX + i*22;
            int color = VisualSettings.colorAt(i);
            context.fill(sx, py()+22, sx+15, py()+37, color);
            context.drawBorder(sx, py()+22, 15, 15, i == VisualSettings.accentIndex() ? 0xFFFFFFFF : 0x805D8194);
        }

        for (int i=0; i<TABS.length; i++) {
            int x=px()+21, y=py()+80+i*47;
            boolean hover=mouseX>=x&&mouseX<=x+174&&mouseY>=y&&mouseY<=y+38;
            texture(context, i==activeTab || hover ? BUTTON_HOVER : BUTTON, x, y, 174, 38, 512, 128);
            context.drawTextWithShadow(textRenderer, fontText(TABS[i]), x+16, y+13,
                i==activeTab ? VisualSettings.accent() : 0xFFE0EDF5);
        }

        context.fill(px()+204, py()+75, px()+206, py()+ph()-25, 0x507AC7DB);
        context.drawTextWithShadow(textRenderer, fontText(switch (activeTab) {
            case 0 -> "ВИЗУАЛЬНЫЕ ЭФФЕКТЫ";
            case 1 -> "МОКРЫЕ ПОВЕРХНОСТИ";
            case 2 -> "СУЩНОСТИ И ИНТЕРФЕЙС";
            default -> "ГОТОВЫЕ ПРОФИЛИ";
        }), contentX(), py()+61, 0xFFF0FAFF);

        switch (activeTab) {
            case 0 -> renderVisualPage(context, mouseX, mouseY);
            case 1 -> renderWetPage(context, mouseX, mouseY);
            case 2 -> renderHudPage(context, mouseX, mouseY);
            default -> renderPresets(context, mouseX, mouseY);
        }

        context.drawCenteredTextWithShadow(textRenderer,
            fontText("RIGHT SHIFT  •  НАСТРОЙКИ СОХРАНЯЮТСЯ  •  БЕЗ КОМАНД MINECRAFT"),
            px()+pw()/2, py()+ph()-19, 0xFF8DA7B8);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderVisualPage(DrawContext c, int mx, int my) {
        drawToggle(c, "jump_circle", "Jump Circle", "Светящееся кольцо во время прыжка", 0, mx, my);
        drawToggle(c, "ambient_particles", "Живые частицы", "Светящиеся частицы рядом с вами и целью", 1, mx, my);
        drawToggle(c, "custom_hotbar", "Liquid Hotbar", "Скруглённая панель и плавная рамка слота", 2, mx, my);
        drawSlider(c, "jump_size", "Размер Jump Circle", "Размер кольца", 0, mx, my);
        drawSlider(c, "particle_density", "Плотность частиц", "Меньше — легче для FPS", 1, mx, my);
    }

    private void renderWetPage(DrawContext c, int mx, int my) {
        drawToggle(c, "wet_floor", "Wet Floor", "Блики, мокрая поверхность и водный оттенок", 0, mx, my);
        drawToggle(c, "rain_ripples", "Капли и волны", "Кольца на лужах во время дождя", 1, mx, my);
        drawToggle(c, "sky_overlay", "Атмосфера неба", "Лёгкое высокодетальное небесное сияние", 2, mx, my);
        drawToggle(c, "soft_lighting", "Мягкие блики", "Более мягкие цветовые акценты", 3, mx, my);
        drawSlider(c, "wet_intensity", "Интенсивность бликов", "Яркость мокрой поверхности", 0, mx, my);
        drawSlider(c, "puddle_radius", "Радиус эффекта", "Дальность поиска поверхностей", 1, mx, my);
    }

    private void renderHudPage(DrawContext c, int mx, int my) {
        drawToggle(c, "entity_boxes", "Rounded Entity ESP", "Контуры, ник и расстояние до сущности", 0, mx, my);
        drawToggle(c, "player_names", "Подписи над целями", "Кастомный текст поверх мира", 1, mx, my);
        drawSlider(c, "box_range", "Дальность ESP", "Радиус отображения от 8 до 64 блоков", 0, mx, my);
    }

    private void renderPresets(DrawContext c, int mx, int my) {
        int x=contentX(), w=contentW();
        String[] title={"CLEAN / ЛЁГКИЙ", "RAIN / WET FLOOR", "NEBULA / VIOLET"};
        String[] desc={"Минималистичный интерфейс и кольцо прыжка", "Мокрые поверхности, блики и дождевые волны", "Фиолетовый акцент, частицы и контуры"};
        for(int i=0;i<3;i++){
            int y=contentY()+i*91;
            boolean hover=mx>=x&&mx<=x+w&&my>=y&&my<=y+76;
            texture(c, hover?BUTTON_HOVER:BUTTON, x,y,w,76,512,128);
            c.drawTextWithShadow(textRenderer,fontText(title[i]),x+18,y+16,VisualSettings.accent());
            c.drawTextWithShadow(textRenderer,fontText(desc[i]),x+18,y+39,0xFFD4E3EC);
            c.drawTextWithShadow(textRenderer,fontText("ПРИМЕНИТЬ  →"),x+w-124,y+29,0xFFECF7FF);
        }
        c.drawTextWithShadow(textRenderer,fontText("Конфиг хранится в config/acidvisuals.json"),x+3,contentY()+285,0xFF9BB3C2);
    }

    private void drawToggle(DrawContext c, String key, String title, String desc, int row, int mx, int my) {
        int x=contentX(), y=contentY()+row*61, w=contentW();
        boolean value=VisualSettings.get(key);
        boolean hover=mx>=x&&mx<=x+w&&my>=y&&my<=y+54;
        texture(c, hover?BUTTON_HOVER:BUTTON, x,y,w,54,512,128);
        c.drawTextWithShadow(textRenderer,fontText(title),x+16,y+11,0xFFF3FAFF);
        c.drawTextWithShadow(textRenderer,fontText(desc),x+16,y+32,0xFFA5BFCE);
        texture(c,value?TOGGLE_ON:TOGGLE_OFF,x+w-65,y+10,52,30,256,128);
        c.drawTextWithShadow(textRenderer,fontText(value?"ON":"OFF"),x+w-104,y+20,value?VisualSettings.accent():0xFF8294A3);
    }

    private void drawSlider(DrawContext c, String key, String title, String desc, int row, int mx, int my) {
        int y=contentY()+(sliderOffsetRows()+row)*61+3, x=contentX(), w=contentW();
        texture(c,BUTTON,x,y,w,55,512,128);
        c.drawTextWithShadow(textRenderer,fontText(title),x+15,y+8,0xFFF0FAFF);
        c.drawTextWithShadow(textRenderer,fontText(desc),x+15,y+30,0xFF9CB7C8);
        int trackX=x+Math.max(190,w/2), trackW=Math.max(70,w-(trackX-x)-22), trackY=y+40;
        c.fill(trackX,trackY,trackX+trackW,trackY+5,0xFF1B3043);
        float min=sliderMin(key), max=sliderMax(key), value=VisualSettings.slider(key);
        int filled=(int)(trackW*(value-min)/(max-min));
        c.fill(trackX,trackY,trackX+filled,trackY+5,VisualSettings.accent());
        int thumbX=trackX+filled-5;
        c.fill(thumbX,trackY-4,thumbX+11,trackY+9,0xFFEAFBFF);
        c.drawTextWithShadow(textRenderer,fontText(sliderValue(key,value)),x+w-75,y+8,VisualSettings.accent());
    }

    private float sliderMin(String key) {
        return switch(key){case "jump_size"->0.55f;case "particle_density"->0.1f;case "wet_intensity"->0.15f;case "puddle_radius"->3f;case "box_range"->8f;default->0f;};
    }
    private float sliderMax(String key) {
        return switch(key){case "jump_size"->2.8f;case "particle_density","wet_intensity"->1f;case "puddle_radius"->10f;case "box_range"->64f;default->1f;};
    }
    private String sliderValue(String key,float v) {
        return switch(key){case "jump_size"->String.format(java.util.Locale.ROOT,"%.2f",v);case "particle_density","wet_intensity"->Math.round(v*100)+"%";default->String.format(java.util.Locale.ROOT,"%.0f",v);};
    }

    private String[] toggleKeys() {
        return switch(activeTab) {
            case 0 -> new String[]{"jump_circle","ambient_particles","custom_hotbar"};
            case 1 -> new String[]{"wet_floor","rain_ripples","sky_overlay","soft_lighting"};
            case 2 -> new String[]{"entity_boxes","player_names"};
            default -> new String[0];
        };
    }
    private String[] sliderKeys() {
        return switch(activeTab) {
            case 0 -> new String[]{"jump_size","particle_density"};
            case 1 -> new String[]{"wet_intensity","puddle_radius"};
            case 2 -> new String[]{"box_range"};
            default -> new String[0];
        };
    }

    private void updateSlider(String key, double mouseX) {
        int x=contentX(), w=contentW();
        int trackX=x+Math.max(190,w/2), trackW=Math.max(70,w-(trackX-x)-22);
        float pct=(float)Math.max(0,Math.min(1,(mouseX-trackX)/(double)trackW));
        VisualSettings.setSlider(key,sliderMin(key)+pct*(sliderMax(key)-sliderMin(key)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX,mouseY,button);
        int paletteX=px()+pw()-154;
        if(mouseY>=py()+20&&mouseY<=py()+40){
            for(int i=0;i<VisualSettings.colorCount();i++){
                int sx=paletteX+i*22;
                if(mouseX>=sx&&mouseX<=sx+15){VisualSettings.setAccentIndex(i);return true;}
            }
        }
        for(int i=0;i<TABS.length;i++){
            int x=px()+21,y=py()+80+i*47;
            if(mouseX>=x&&mouseX<=x+174&&mouseY>=y&&mouseY<=y+38){activeTab=i;return true;}
        }
        if(activeTab==3){
            for(int i=0;i<3;i++){
                int x=contentX(),y=contentY()+i*91;
                if(mouseX>=x&&mouseX<=x+contentW()&&mouseY>=y&&mouseY<=y+76){VisualSettings.applyPreset(i);return true;}
            }
            return super.mouseClicked(mouseX,mouseY,button);
        }
        String[] toggles=toggleKeys();
        for(int i=0;i<toggles.length;i++){
            int y=contentY()+i*61;
            if(mouseX>=contentX()&&mouseX<=contentX()+contentW()&&mouseY>=y&&mouseY<=y+54){
                VisualSettings.set(toggles[i],!VisualSettings.get(toggles[i]));return true;
            }
        }
        String[] sliders=sliderKeys();
        for(int i=0;i<sliders.length;i++){
            int y=contentY()+(sliderOffsetRows()+i)*61+3;
            if(mouseY>=y&&mouseY<=y+55){
                int x=contentX(),w=contentW(),trackX=x+Math.max(190,w/2),trackW=Math.max(70,w-(trackX-x)-22);
                if(mouseX>=trackX-5&&mouseX<=trackX+trackW+5){draggingSlider=sliders[i];updateSlider(draggingSlider,mouseX);return true;}
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double deltaX,double deltaY) {
        if(button==0&&draggingSlider!=null){updateSlider(draggingSlider,mouseX);return true;}
        return super.mouseDragged(mouseX,mouseY,button,deltaX,deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX,double mouseY,int button) {
        if(button==0)draggingSlider=null;
        return super.mouseReleased(mouseX,mouseY,button);
    }

    @Override public boolean shouldPause(){return false;}
}
