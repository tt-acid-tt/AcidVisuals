package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Animated client menu. Rounded surfaces are rasterized into thin horizontal
 * spans so the corners are smooth at GUI scale without relying on the old
 * GameRenderer.getPositionColorProgram API (removed in modern Minecraft).
 */
public final class AcidVisualsScreen extends Screen {
    private static final int BG = 0xF20B0E14;
    private static final int SURFACE = 0xFF121720;
    private static final int SURFACE_HOVER = 0xFF202638;
    private static final int ACCENT = 0xFF9B7BFF;
    private static final int TEXT = 0xFFF4F5FB;
    private static final int MUTED = 0xFF929DB0;

    private static final String[] TABS = {"RENDER", "VISUAL", "UTILITY"};
    private int selectedTab = 0;
    private final boolean[] enabled = {true, false, true, false, true, false};
    private final float[] hover = new float[12];
    private float openProgress = 0.0F;
    private float closeProgress = 0.0F;
    private boolean closing;
    private long lastFrameNanos = System.nanoTime();

    public AcidVisualsScreen() {
        super(Text.literal("AcidVisuals"));
    }

    @Override
    protected void init() {
        openProgress = 0.0F;
        closeProgress = 0.0F;
        closing = false;
        lastFrameNanos = System.nanoTime();
    }

    @Override
    public void tick() {
        super.tick();
        float dt = Math.min(0.05F, (System.nanoTime() - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = System.nanoTime();
        if (closing) {
            closeProgress = approach(closeProgress, 1.0F, dt * 5.5F);
            if (closeProgress >= 0.985F && client != null) client.setScreen(null);
        } else {
            openProgress = approach(openProgress, 1.0F, dt * 5.5F);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float t = easeOutCubic(closing ? 1.0F - closeProgress : openProgress);
        int fade = Math.max(0, Math.min(255, Math.round(t * 255.0F)));
        int scrim = withAlpha(0xFF05070C, Math.round(0xB0 * t));
        context.fill(0, 0, width, height, scrim);

        int panelWidth = Math.min(650, width - 28);
        int panelHeight = Math.min(390, height - 28);
        if (panelWidth < 280 || panelHeight < 220) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Увеличьте размер окна"), width / 2, height / 2, TEXT);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        float scale = 0.94F + 0.06F * t;
        int w = Math.round(panelWidth * scale);
        int h = Math.round(panelHeight * scale);
        int x = (width - w) / 2;
        int y = (height - h) / 2 + Math.round((1.0F - t) * 10.0F);

        rounded(context, x, y, w, h, 16, withAlpha(BG, fade));
        rounded(context, x + 1, y + 1, w - 2, h - 2, 15, withAlpha(0xFF151A24, fade));
        rounded(context, x + 18, y + 18, 3, h - 36, 2, withAlpha(ACCENT, fade));

        drawText(context, "ACID", x + 36, y + 23, withAlpha(TEXT, fade));
        drawText(context, "VISUALS", x + 77, y + 23, withAlpha(ACCENT, fade));
        drawSmall(context, "CLIENT INTERFACE  /  1.21.11", x + 36, y + 43, withAlpha(MUTED, fade));
        drawSmall(context, "BETA", x + w - 66, y + 28, withAlpha(ACCENT, fade));
        rounded(context, x + 34, y + 63, w - 68, 1, 1, withAlpha(0xFF303747, fade));

        int navX = x + 35;
        int navY = y + 78;
        int tabWidth = 105;
        for (int i = 0; i < TABS.length; i++) {
            int tx = navX + i * (tabWidth + 8);
            boolean active = selectedTab == i;
            boolean over = inside(mouseX, mouseY, tx, navY, tabWidth, 30);
            float target = over ? 1.0F : 0.0F;
            hover[i] = approach(hover[i], target, 0.22F);
            rounded(context, tx, navY, tabWidth, 30, 9,
                    withAlpha(active ? 0xFF30294A : mix(SURFACE, SURFACE_HOVER, hover[i]), fade));
            if (active) rounded(context, tx + 12, navY + 26, tabWidth - 24, 2, 1, withAlpha(ACCENT, fade));
            centered(context, TABS[i], tx + tabWidth / 2, navY + 10, withAlpha(active ? TEXT : MUTED, fade));
        }

        int contentY = navY + 49;
        drawText(context, selectedTab == 0 ? "Visual modules" : selectedTab == 1 ? "HUD & overlays" : "Utilities",
                x + 36, contentY, withAlpha(TEXT, fade));
        drawSmall(context, "Настройте элементы интерфейса под себя", x + 36, contentY + 19, withAlpha(MUTED, fade));

        String[][] rows = rowsForTab();
        int rowY = contentY + 48;
        int rowW = w - 72;
        for (int i = 0; i < rows.length; i++) {
            int ry = rowY + i * 48;
            int hoverIndex = 3 + i;
            boolean over = inside(mouseX, mouseY, x + 36, ry, rowW, 41);
            float target = over ? 1.0F : 0.0F;
            hover[hoverIndex] = approach(hover[hoverIndex], target, 0.22F);
            rounded(context, x + 36, ry, rowW, 41, 9,
                    withAlpha(mix(SURFACE, SURFACE_HOVER, hover[hoverIndex]), fade));
            drawText(context, rows[i][0], x + 49, ry + 7, withAlpha(TEXT, fade));
            drawSmall(context, rows[i][1], x + 49, ry + 23, withAlpha(MUTED, fade));

            int id = selectedTab * 2 + i;
            boolean on = enabled[id];
            int sx = x + w - 88;
            rounded(context, sx, ry + 12, 36, 17, 9, withAlpha(on ? 0xFF6E56E8 : 0xFF303746, fade));
            int knobX = sx + (on ? 20 : 3);
            rounded(context, knobX, ry + 14, 13, 13, 7, withAlpha(0xFFFFFFFF, fade));
        }

        drawSmall(context, "RIGHT SHIFT  ·  CLOSE", x + 36, y + h - 26, withAlpha(MUTED, fade));
        drawSmall(context, "ACIDVISUALS", x + w - 116, y + h - 26, withAlpha(0xFF5F6A7D, fade));
        super.render(context, mouseX, mouseY, delta);
    }

    private String[][] rowsForTab() {
        return switch (selectedTab) {
            case 1 -> new String[][] {
                    {"Target HUD", "Информация о выбранной цели"},
                    {"Crosshair", "Настраиваемый прицел"}
            };
            case 2 -> new String[][] {
                    {"Performance overlay", "Минимальный экранный монитор"},
                    {"UI animations", "Плавные переходы интерфейса"}
            };
            default -> new String[][] {
                    {"Jump Circle", "Плавный круговой эффект"},
                    {"Entity Glow", "Подсветка выбранных сущностей"}
            };
        };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button != 0 || closing) return super.mouseClicked(click, doubled);
        int panelWidth = Math.min(650, width - 28);
        int panelHeight = Math.min(390, height - 28);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        int navY = y + 78;
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + 35 + i * 113;
            if (inside(mouseX, mouseY, tx, navY, 105, 30)) {
                selectedTab = i;
                return true;
            }
        }

        int contentY = navY + 49;
        int rowY = contentY + 48;
        int rowW = panelWidth - 72;
        for (int i = 0; i < 2; i++) {
            int ry = rowY + i * 48;
            if (inside(mouseX, mouseY, x + 36, ry, rowW, 41)) {
                int id = selectedTab * 2 + i;
                enabled[id] = !enabled[id];
                return true;
            }
        }
        if (mouseX < x || mouseX > x + panelWidth || mouseY < y || mouseY > y + panelHeight) {
            requestClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            requestClose();
            return true;
        }
        return super.keyPressed(input);
    }

    private void requestClose() {
        if (!closing) {
            closing = true;
            closeProgress = 0.0F;
        }
    }

    private void drawText(DrawContext ctx, String value, int x, int y, int color) {
        // Minecraft 1.21.11's supported DrawContext text API is used here.
        // A bundled TTF cannot be loaded by TextRenderer directly; a custom
        // font requires a resource font definition plus bitmap glyph atlas.
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }

    private void drawSmall(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawText(textRenderer, Text.literal(value), x, y, color, false);
    }

    private void centered(DrawContext ctx, String value, int centerX, int y, int color) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(value), centerX, y, color);
    }

    /**
     * Rounded rectangle rasterized as horizontal spans. This avoids square
     * corners; each scanline computes its left/right inset from a circle.
     */
    private static void rounded(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        float rf = r - 0.35F;
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                float dy = rf - row;
                inset = Math.max(0, (int) Math.ceil(rf - (float) Math.sqrt(Math.max(0.0F, rf * rf - dy * dy))));
            } else if (row >= h - r) {
                float dy = rf - (h - 1 - row);
                inset = Math.max(0, (int) Math.ceil(rf - (float) Math.sqrt(Math.max(0.0F, rf * rf - dy * dy))));
            }
            if (w - inset * 2 > 0) ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24)
                | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8)
                | Math.round(ab + (bb - ab) * t);
    }

    private static float approach(float current, float target, float amount) {
        return current + (target - current) * Math.min(1.0F, amount);
    }

    private static float easeOutCubic(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return 1.0F - (float) Math.pow(1.0F - t, 3.0);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
