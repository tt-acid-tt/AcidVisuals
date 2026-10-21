package tt.acid.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class AcidVisualsScreen extends Screen {
    private static final int PANEL = 0xF20D1119;
    private static final int PANEL_INNER = 0xFF151B25;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int MUTED = 0xFF9AA5B5;

    public AcidVisualsScreen() {
        super(Text.literal("AcidVisuals"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x70030810);

        int panelWidth = Math.min(620, width - 36);
        int panelHeight = Math.min(370, height - 36);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        // Layered surfaces keep the interface independent from Minecraft's GUI texture atlas.
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + 4, y + panelHeight, ACCENT);
        context.fill(x + 22, y + 22, x + panelWidth - 22, y + 23, 0x553E4A5F);

        context.drawTextWithShadow(textRenderer, Text.literal("ACID"), x + 24, y + 20, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("VISUALS"), x + 61, y + 20, ACCENT);
        context.drawTextWithShadow(textRenderer, Text.literal("CLIENT INTERFACE  /  1.21.11"), x + 24, y + 43, MUTED);

        int navY = y + 78;
        String[] tabs = {"RENDER", "VISUAL", "UTILITI"};
        for (int i = 0; i < tabs.length; i++) {
            int tx = x + 24 + i * 112;
            boolean active = i == 0;
            context.fill(tx, navY, tx + 100, navY + 27, active ? 0xFF28243D : PANEL_INNER);
            if (active) context.fill(tx, navY + 24, tx + 100, navY + 27, ACCENT);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tabs[i]), tx + 50, navY + 9, active ? 0xFFFFFFFF : MUTED);
        }

        int contentY = navY + 44;
        context.drawTextWithShadow(textRenderer, Text.literal("Visual modules"), x + 24, contentY, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("The custom UI foundation is active."), x + 24, contentY + 19, MUTED);

        drawModule(context, x + 24, contentY + 52, panelWidth - 48, "Jump Circle", "Smooth world-space circle effect", true);
        drawModule(context, x + 24, contentY + 100, panelWidth - 48, "Target HUD", "Custom target information panel", false);

        context.drawTextWithShadow(textRenderer, Text.literal("RIGHT SHIFT  ·  CLOSE"), x + 24, y + panelHeight - 24, MUTED);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawModule(DrawContext context, int x, int y, int w, String title, String subtitle, boolean enabled) {
        context.fill(x, y, x + w, y + 41, PANEL_INNER);
        context.drawTextWithShadow(textRenderer, Text.literal(title), x + 12, y + 8, 0xFFF4F6FB);
        context.drawTextWithShadow(textRenderer, Text.literal(subtitle), x + 12, y + 23, MUTED);

        int sx = x + w - 48;
        context.fill(sx, y + 12, sx + 34, y + 29, enabled ? 0xFF5E4ACD : 0xFF343D4A);
        context.fill(enabled ? sx + 18 : sx + 3, y + 14, enabled ? sx + 31 : sx + 16, y + 27, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
