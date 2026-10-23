package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Fresh menu implementation, independent from the previous screen. */
public final class AcidVisualsStudioScreen extends Screen {
    private static final int PANEL=0xF20D1018, SURFACE=0xE91A1F2B, HOVER=0xFF262D3B;
    private static final int WHITE=0xFFF4F5FA, MUTED=0xFF929BAE, ACCENT=0xFFB5A0FF;
    private static final String[] PAGES={"Общее","Визуалы","HUD","Защита LAN"};
    private int page;
    private float opening;
    private long lastFrame=System.nanoTime();
    public AcidVisualsStudioScreen(){super(Text.literal("AcidVisuals"));}
    @Override public boolean shouldPause(){return false;}
    @Override public void tick(){long now=System.nanoTime(); opening=Math.min(1f,opening+Math.min(.05f,(now-lastFrame)/1_000_000_000f)*4.8f);lastFrame=now;}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        float a=1f-(float)Math.pow(1f-opening,3);
        c.fill(0,0,width,height,((int)(105*a)<<24)|0x05070B);
        int w=Math.min(820,width-32),h=Math.min(500,height-28);
        if(w<420||h<300){c.drawCenteredTextWithShadow(textRenderer,Text.literal("Увеличьте размер окна Minecraft"),width/2,height/2,WHITE);super.render(c,mx,my,delta);return;}
        int x=(width-w)/2,y=(height-h)/2+(int)((1-a)*18);
        round(c,x,y,w,h,22,PANEL);round(c,x+1,y+1,w-2,1,1,0x35FFFFFF);round(c,x+20,y+20,3,h-40,2,ACCENT);
        txt(c,"ACID",x+39,y+25,WHITE);txt(c,"VISUALS",x+82,y+25,ACCENT);muted(c,"CLIENT / FABRIC 1.21.4",x+39,y+46);muted(c,"RIGHT SHIFT · ESC",x+w-153,y+31);
        round(c,x+30,y+68,w-60,1,1,0xFF343A49);
        int sx=x+30,sy=y+86,sw=178;
        for(int i=0;i<PAGES.length;i++){int ry=sy+i*48;boolean active=page==i;round(c,sx,ry,sw,40,12,active?0xFF302A47:hit(mx,my,sx,ry,sw,40)?HOVER:0x00000000);if(active){round(c,sx+9,ry+10,3,20,2,ACCENT);round(c,sx+sw-18,ry+18,4,4,2,ACCENT);}txt(c,PAGES[i],sx+22,ry+14,active?WHITE:MUTED);}
        round(c,x+225,y+86,1,h-112,1,0xFF343A49);
        int cx=x+248,cy=y+91,cw=w-278;
        txt(c,title(),cx,cy,WHITE);muted(c,subtitle(),cx,cy+22);
        String[][] rows=rows();
        for(int i=0;i<rows.length;i++){int ry=cy+52+i*54;round(c,cx,ry,cw,46,12,hit(mx,my,cx,ry,cw,46)?HOVER:SURFACE);txt(c,rows[i][0],cx+14,ry+8,WHITE);muted(c,rows[i][1],cx+14,ry+26);boolean on=enabled(i);int tx=cx+cw-53;round(c,tx,ry+14,39,19,10,on?0xFF7865E8:0xFF3A4150);round(c,tx+(on?22:3),ry+17,13,13,7,0xFFFFFFFF);}
        muted(c,"ACIDVISUALS · NEW UI",x+31,y+h-27);muted(c,"ESC — закрыть",x+w-110,y+h-27);super.render(c,mx,my,delta);
    }
    private String title(){return switch(page){case 0->"Параметры";case 1->"Визуальные модули";case 2->"Интерфейс цели";default->"Защита мира по LAN";};}
    private String subtitle(){return switch(page){case 0->"Управление отображением и поведением модификации";case 1->"Эффекты и отображение сущностей";case 2->"HUD цели и информация под прицелом";default->"Проверки игроков должны выполняться на стороне хоста";};}
    private String[][] rows(){return switch(page){
        case 0->new String[][]{{"Плавные переходы","Анимация элементов интерфейса"},{"Компактный интерфейс","Плотность расположения элементов"}};
        case 1->new String[][]{{"Jump Circle","Круг и волны при прыжке"},{"Player ESP","Подсветка игроков и имена"},{"Entity Hitboxes","Контуры границ сущностей"}};
        case 2->new String[][]{{"Target HUD","Карточка цели под прицелом"},{"Удерживать цель","Показывать цель ещё 2 секунды"}};
        default->new String[][]{{"LAN AntiCheat","Запрет полёта в режиме выживания"},{"Проверка движения","Проверять подозрительное перемещение"}};
    };}
    private boolean enabled(int i){return switch(page){
        case 0->i==0&&AcidVisualsClientSettings.smoothUiEnabled;
        case 1->i==0?AcidVisualsClientSettings.jumpCircleEnabled:i==1?AcidVisualsClientSettings.playerEspEnabled:AcidVisualsClientSettings.entityHitboxesEnabled;
        case 2->i==0?AcidVisualsClientSettings.targetHudEnabled:true;
        default->i==0?AcidVisualsClientSettings.antiCheatEnabled:AcidVisualsClientSettings.movementChecksEnabled;
    };}
    private void toggle(int i){switch(page){
        case 0->{if(i==0)AcidVisualsClientSettings.smoothUiEnabled=!AcidVisualsClientSettings.smoothUiEnabled;}
        case 1->{if(i==0)AcidVisualsClientSettings.jumpCircleEnabled=!AcidVisualsClientSettings.jumpCircleEnabled;else if(i==1)AcidVisualsClientSettings.playerEspEnabled=!AcidVisualsClientSettings.playerEspEnabled;else AcidVisualsClientSettings.entityHitboxesEnabled=!AcidVisualsClientSettings.entityHitboxesEnabled;}
        case 2->{if(i==0)AcidVisualsClientSettings.targetHudEnabled=!AcidVisualsClientSettings.targetHudEnabled;}
        default->{if(i==0)AcidVisualsClientSettings.antiCheatEnabled=!AcidVisualsClientSettings.antiCheatEnabled;else AcidVisualsClientSettings.movementChecksEnabled=!AcidVisualsClientSettings.movementChecksEnabled;}
    }}
    @Override public boolean mouseClicked(double mx,double my,int b){if(b!=0)return super.mouseClicked(mx,my,b);int w=Math.min(820,width-32),h=Math.min(500,height-28),x=(width-w)/2,y=(height-h)/2,sx=x+30,sy=y+86,sw=178;for(int i=0;i<PAGES.length;i++)if(hit(mx,my,sx,sy+i*48,sw,40)){page=i;return true;}int cx=x+248,cy=y+91,cw=w-278;for(int i=0;i<rows().length;i++)if(hit(mx,my,cx,cy+52+i*54,cw,46)){toggle(i);return true;}if(!hit(mx,my,x,y,w,h)){closeMenu();return true;}return true;}
    @Override public boolean keyPressed(int k,int s,int m){if(k==GLFW.GLFW_KEY_ESCAPE||k==GLFW.GLFW_KEY_RIGHT_SHIFT){closeMenu();return true;}return super.keyPressed(k,s,m);}
    private void closeMenu(){if(client!=null)client.setScreen(null);}
    private void txt(DrawContext c,String s,int x,int y,int col){c.drawTextWithShadow(textRenderer,Text.literal(s),x,y,col);}
    private void muted(DrawContext c,String s,int x,int y){c.drawText(textRenderer,Text.literal(s),x,y,MUTED,false);}
    private static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    private static void round(DrawContext c,int x,int y,int w,int h,int r,int col){if(w<=0||h<=0)return;r=Math.max(0,Math.min(r,Math.min(w/2,h/2)));c.fill(x+r,y,x+w-r,y+h,col);c.fill(x,y+r,x+w,y+h-r,col);for(int i=0;i<r;i++){int q=(int)Math.ceil(r-Math.sqrt(Math.max(0,r*r-(r-i-.5)*(r-i-.5))));c.fill(x+q,y+i,x+w-q,y+i+1,col);c.fill(x+q,y+h-i-1,x+w-q,y+h-i,col);}}
}
