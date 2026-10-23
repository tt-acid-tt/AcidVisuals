package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class GodScreen extends Screen {
    public GodScreen() { super(Text.literal("GOD OF THE WORLD")); }

    @Override protected void init() {
        clearChildren();
        int panelW = Math.min(500, width - 24);
        int left = (width - panelW) / 2 + 14;
        int top = height / 2 - 135;
        int gap = 10;
        int w = (panelW - 38) / 2;
        int h = 24;

        addDrawableChild(toggle("Полёт без урона", () -> GodSettings.flight, v -> GodSettings.flight = v, left, top, w, h));
        addDrawableChild(toggle("Бессмертие", () -> GodSettings.invulnerability, v -> GodSettings.invulnerability = v, left + w + gap, top, w, h));
        addDrawableChild(toggle("Kill Aura", () -> GodSettings.killAura, v -> GodSettings.killAura = v, left, top + 32, w, h));
        addDrawableChild(toggle("Ответный удар", () -> GodSettings.retaliation, v -> GodSettings.retaliation = v, left + w + gap, top + 32, w, h));
        addDrawableChild(toggle("Home Lantern • B", () -> GodSettings.homeLantern, v -> GodSettings.homeLantern = v, left, top + 64, w, h));
        addDrawableChild(toggle("Лазеры из глаз • G", () -> GodSettings.eyeLaser, v -> GodSettings.eyeLaser = v, left + w + gap, top + 64, w, h));
        addDrawableChild(toggle("Остановка времени", () -> GodSettings.timeStop, v -> GodSettings.timeStop = v, left, top + 96, w, h));
        addDrawableChild(toggle("Телекинез • H", () -> GodSettings.telekinesis, v -> GodSettings.telekinesis = v, left + w + gap, top + 96, w, h));
        addDrawableChild(toggle("Jump Circle", () -> GodSettings.jumpCircle, v -> GodSettings.jumpCircle = v, left, top + 128, w, h));
        addDrawableChild(toggle("Wet Floor • эффекты", () -> GodSettings.wetFloor, v -> GodSettings.wetFloor = v, left + w + gap, top + 128, w, h));

        addDrawableChild(ButtonWidget.builder(Text.literal("Небо: " + skyName()), b -> {
            GodSettings.skyPreset = (GodSettings.skyPreset + 1) % 4;
            b.setMessage(Text.literal("Небо: " + skyName()));
        }).dimensions(left, top + 162, w, h).build());

        addDrawableChild(new SliderWidget(left + w + gap, top + 162, w, h,
                Text.literal("Радиус: " + (int) GodSettings.killRange),
                (GodSettings.killRange - 4.0) / 60.0) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Радиус: " + (int) GodSettings.killRange));
            }
            @Override protected void applyValue() {
                GodSettings.killRange = (float)(4.0 + value * 60.0);
            }
        });

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width / 2 - 75, top + 200, 150, h).build());
    }

    private String skyName() {
        return switch (GodSettings.skyPreset) {
            case 1 -> "Aurora";
            case 2 -> "Void";
            case 3 -> "Sunset";
            default -> "Default";
        };
    }

    private ButtonWidget toggle(String label, java.util.function.BooleanSupplier get,
                                java.util.function.Consumer<Boolean> set, int x, int y, int w, int h) {
        return ButtonWidget.builder(Text.literal(label + "  " + (get.getAsBoolean() ? "●" : "○")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Text.literal(label + "  " + (get.getAsBoolean() ? "●" : "○")));
        }).dimensions(x, y, w, h).build();
    }

    private void rounded(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < radius) {
                double d = radius - row - 0.5;
                inset = (int)Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - d * d)));
            } else if (row >= h - radius) {
                double d = row - (h - radius) + 0.5;
                inset = (int)Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - d * d)));
            }
            ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelW = Math.min(500, width - 24), panelH = 270;
        int x = (width - panelW) / 2, y = height / 2 - 145;
        rounded(context, x, y, panelW, panelH, 14, 0xF011141B);
        rounded(context, x + 1, y + 1, panelW - 2, panelH - 2, 13, 0xF011141B);
        rounded(context, x + 16, y + 7, panelW - 32, 2, 1, 0xFF6AE6F5);
        context.drawCenteredTextWithShadow(textRenderer, "GOD OF THE WORLD", width / 2, y + 16, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Управление одиночным миром • Fabric 1.21.4", width / 2, y + 30, 0xFF9CA8B9);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, "G — лазер   H — телекинез   B — Home Lantern", width / 2, y + panelH - 18, 0xFFB8C7D8);
    }

    @Override public boolean shouldPause() { return false; }
}
