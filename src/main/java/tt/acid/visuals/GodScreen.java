package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class GodScreen extends Screen {
    public GodScreen() { super(Text.literal("GOD OF THE WORLD")); }

    @Override protected void init() {
        int left = this.width / 2 - 190;
        int top = this.height / 2 - 118;
        int w = 180;
        int gap = 12;
        int h = 22;
        addDrawableChild(toggle("Полёт в выживании", () -> GodSettings.flight, v -> GodSettings.flight = v, left, top, w, h));
        addDrawableChild(toggle("Бессмертие", () -> GodSettings.invulnerability, v -> GodSettings.invulnerability = v, left + w + gap, top, w, h));
        addDrawableChild(toggle("Kill Aura", () -> GodSettings.killAura, v -> GodSettings.killAura = v, left, top + 31, w, h));
        addDrawableChild(toggle("Наказание за удар", () -> GodSettings.retaliation, v -> GodSettings.retaliation = v, left + w + gap, top + 31, w, h));
        addDrawableChild(toggle("Форма Home Lantern", () -> GodSettings.homeLantern, v -> GodSettings.homeLantern = v, left, top + 62, w, h));
        addDrawableChild(toggle("Лазер из глаз (ПКМ)", () -> GodSettings.eyeLaser, v -> GodSettings.eyeLaser = v, left + w + gap, top + 62, w, h));
        addDrawableChild(toggle("Остановка времени", () -> GodSettings.timeStop, v -> GodSettings.timeStop = v, left, top + 93, w, h));
        addDrawableChild(toggle("Телекинез", () -> GodSettings.telekinesis, v -> GodSettings.telekinesis = v, left + w + gap, top + 93, w, h));
        addDrawableChild(ButtonWidget.builder(Text.literal("Удар молнии"), b -> GodController.strikeLightning(client))
                .dimensions(left, top + 128, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Телепорт вперёд"), b -> GodController.teleportForward(client))
                .dimensions(left + w + gap, top + 128, w, h).build());
        addDrawableChild(new SliderWidget(left, top + 164, w * 2 + gap, h,
                Text.literal("Радиус Kill Aura: " + (int) GodSettings.killRange), (GodSettings.killRange - 4.0) / 60.0) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Радиус Kill Aura: " + (int) GodSettings.killRange));
            }
            @Override protected void applyValue() {
                GodSettings.killRange = (float) (4.0 + value * 60.0);
            }
        });
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(this.width / 2 - 90, top + 197, 180, h).build());
    }

    private ButtonWidget toggle(String label, java.util.function.BooleanSupplier get,
                                java.util.function.Consumer<Boolean> set, int x, int y, int w, int h) {
        return ButtonWidget.builder(Text.literal(label + ": " + (get.getAsBoolean() ? "ВКЛ" : "ВЫКЛ")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Text.literal(label + ": " + (get.getAsBoolean() ? "ВКЛ" : "ВЫКЛ")));
        }).dimensions(x, y, w, h).build();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelW = 410, panelH = 280;
        int x = width / 2 - panelW / 2, y = height / 2 - panelH / 2;
        context.fill(x, y, x + panelW, y + panelH, 0xF012151C);
        context.fill(x, y, x + panelW, y + 2, 0xFF55DDF5);
        context.drawCenteredTextWithShadow(textRenderer, "GOD OF THE WORLD", width / 2, y + 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Minecraft 1.21.4 • Fabric • одиночный мир", width / 2, y + 25, 0xFF9BA8B8);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
