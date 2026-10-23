package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

/** Compact, fully opaque configuration screen for the first working module. */
public final class KillAuraScreen extends Screen {
    private static final int BG = 0xFF10131A, PANEL = 0xFF191E28, PANEL_HOVER = 0xFF232A37;
    private static final int TRACK = 0xFF343D4D, TEXT = 0xFFF3F5FA, MUTED = 0xFFABB4C5, ACCENT = 0xFF9D85FF;
    private int dragging = -1;

    public KillAuraScreen() { super(Text.literal("AcidAura")); }
    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, BG); // opaque: no world blur
        int w = Math.min(650, width - 28), h = Math.min(410, height - 28);
        int x = (width - w) / 2, y = (height - h) / 2;
        round(c, x, y, w, h, 18, PANEL);
        round(c, x + 22, y + 22, 4, 46, 2, ACCENT);
        text(c, "ACID", x + 38, y + 23, TEXT); text(c, "AURA", x + 83, y + 23, ACCENT);
        small(c, "FABRIC 1.21.4  /  COMBAT MODULE", x + 38, y + 43);
        small(c, "RIGHT SHIFT  ·  ESC", x + w - 150, y + 30);
        c.fill(x + 22, y + 76, x + w - 22, y + 77, 0xFF343B49);

        int left = x + 28, right = x + w - 28, rowY = y + 94;
        round(c, left, rowY, w - 56, 52, 11, KillAuraSettings.enabled ? 0xFF25243A : PANEL_HOVER);
        text(c, "KillAura", left + 15, rowY + 10, TEXT);
        small(c, KillAuraSettings.enabled ? "Включена · автоматически атакует ближайшую цель" : "Выключена · нажми, чтобы включить", left + 15, rowY + 30);
        round(c, right - 52, rowY + 17, 38, 19, 10, KillAuraSettings.enabled ? ACCENT : TRACK);
        round(c, right - 49 + (KillAuraSettings.enabled ? 19 : 0), rowY + 20, 13, 13, 7, 0xFFFFFFFF);

        slider(c, x, y, w, 0, "Дальность", String.format(Locale.ROOT, "%.1f блока", KillAuraSettings.range), (KillAuraSettings.range - 1f) / 5f);
        slider(c, x, y, w, 1, "Скорость атак", KillAuraSettings.attacksPerSecond + " в секунду (макс.)", (KillAuraSettings.attacksPerSecond - 1f) / 19f);
        slider(c, x, y, w, 2, "Максимальный урон", KillAuraSettings.damageCharge + "% заряда удара", (KillAuraSettings.damageCharge - 10f) / 90f);
        small(c, "Урон зависит от оружия и правил сервера. Ползунок урона задаёт заряд", x + 28, y + h - 39);
        small(c, "ванильной атаки: высокий процент требует более сильного удара.", x + 28, y + h - 25);
        super.render(c, mouseX, mouseY, delta);
    }

    private void slider(DrawContext c, int x, int y, int w, int index, String label, String value, float progress) {
        int top = y + 160 + index * 65, left = x + 30, right = x + w - 30;
        text(c, label, left, top, TEXT);
        text(c, value, right - textRenderer.getWidth(value), top, ACCENT);
        int sx = left, sy = top + 26, sw = right - left;
        round(c, sx, sy, sw, 6, 3, TRACK);
        int fill = Math.max(0, Math.min(sw, Math.round(sw * progress)));
        if (fill > 0) round(c, sx, sy, fill, 6, 3, ACCENT);
        int knobX = sx + fill;
        round(c, knobX - 7, sy - 5, 14, 16, 7, TEXT);
        round(c, knobX - 3, sy - 1, 6, 8, 3, ACCENT);
    }

    private void small(DrawContext c, String s, int x, int y) { c.drawText(textRenderer, Text.literal(s), x, y, MUTED, false); }
    private void text(DrawContext c, String s, int x, int y, int color) { c.drawText(textRenderer, Text.literal(s), x, y, color, false); }
    private static boolean hit(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    private static void round(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        c.fill(x + radius, y, x + w - radius, y + h, color);
        c.fill(x, y + radius, x + w, y + h - radius, color);
        for (int i = 0; i < radius; i++) {
            int q = (int)Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - (radius - i - .5) * (radius - i - .5))));
            c.fill(x + q, y + i, x + w - q, y + i + 1, color);
            c.fill(x + q, y + h - i - 1, x + w - q, y + h - i, color);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int w = Math.min(650, width - 28), h = Math.min(410, height - 28), x = (width - w) / 2, y = (height - h) / 2;
        int left = x + 28, right = x + w - 28, rowY = y + 94;
        if (hit(mx, my, left, rowY, w - 56, 52)) { KillAuraSettings.enabled = !KillAuraSettings.enabled; return true; }
        for (int i = 0; i < 3; i++) {
            int top = y + 160 + i * 65;
            if (hit(mx, my, left, top + 16, right - left, 26)) { dragging = i; updateSlider(mx, x, w, i); return true; }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging >= 0) { updateSlider(mx, (width - Math.min(650, width - 28)) / 2, Math.min(650, width - 28), dragging); return true; }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) { if (button == 0) dragging = -1; return super.mouseReleased(mx, my, button); }
    private void updateSlider(double mx, int x, int w, int index) {
        double minX = x + 30, maxX = x + w - 30;
        float p = (float)Math.max(0, Math.min(1, (mx - minX) / (maxX - minX)));
        if (index == 0) KillAuraSettings.range = Math.round((1f + p * 5f) * 10f) / 10f;
        else if (index == 1) KillAuraSettings.attacksPerSecond = 1 + Math.round(p * 19f);
        else KillAuraSettings.damageCharge = 10 + Math.round(p * 18f) * 5;
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { if (client != null) client.setScreen(null); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
