package dev.ghost.pvp.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ghost.pvp.ui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Thin wrapper around GuiGraphics + raw Tesselator for rounded rects and gradients.
 * All coordinates are in GUI scaled pixels.
 */
public final class GuiRenderer {

    private GuiRenderer() {}

    // ── Filled rounded rectangle ───────────────────────────────────────────────

    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, float r, int color) {
        // Approximate with 1-px corner fill + fill body — real rounding via fragment shader
        // is not available in vanilla; we simulate with overlapping rects.
        int cr = Math.min((int) r, Math.min(w / 2, h / 2));
        // Center body
        g.fill(x + cr, y,      x + w - cr, y + h, color);
        // Left/right strips
        g.fill(x,          y + cr, x + cr,      y + h - cr, color);
        g.fill(x + w - cr, y + cr, x + w,       y + h - cr, color);
        // Corners (brute-force circle pixels)
        drawCornerCircle(g, x + cr,     y + cr,     cr, color, 180, 270);
        drawCornerCircle(g, x + w - cr, y + cr,     cr, color, 270, 360);
        drawCornerCircle(g, x + w - cr, y + h - cr, cr, color, 0,   90);
        drawCornerCircle(g, x + cr,     y + h - cr, cr, color, 90,  180);
    }

    private static void drawCornerCircle(GuiGraphics g, int cx, int cy, int r, int color,
                                         int startDeg, int endDeg) {
        for (int deg = startDeg; deg < endDeg; deg++) {
            double rad0 = Math.toRadians(deg);
            double rad1 = Math.toRadians(deg + 1);
            int x0 = cx + (int)(Math.cos(rad0) * r);
            int y0 = cy + (int)(Math.sin(rad0) * r);
            int x1 = cx + (int)(Math.cos(rad1) * r);
            int y1 = cy + (int)(Math.sin(rad1) * r);
            g.fill(Math.min(cx, x0), Math.min(cy, y0), Math.max(cx, x1) + 1, Math.max(cy, y1) + 1, color);
        }
    }

    // ── Outlined rounded rectangle ─────────────────────────────────────────────

    public static void roundedRectOutline(GuiGraphics g, int x, int y, int w, int h, float r, int color, int thick) {
        for (int t = 0; t < thick; t++) {
            roundedRect(g, x + t, y + t, w - t * 2, h - t * 2, r - t, withAlpha(color, 1f));
            roundedRect(g, x + t + 1, y + t + 1, w - t * 2 - 2, h - t * 2 - 2, r - t, 0);
        }
    }

    // ── Gradient rect (vertical) ───────────────────────────────────────────────

    public static void gradientRect(GuiGraphics g, int x, int y, int w, int h, int colorTop, int colorBottom) {
        g.fillGradient(x, y, x + w, y + h, colorTop, colorBottom);
    }

    // ── Scissor clipping ────────────────────────────────────────────────────────

    public static void enableScissor(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    public static void disableScissor(GuiGraphics g) {
        g.disableScissor();
    }

    // ── Text ───────────────────────────────────────────────────────────────────

    public static void drawString(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, true);
    }

    public static void drawCenteredString(GuiGraphics g, Font font, String text, int cx, int y, int color) {
        g.drawCenteredString(font, text, cx, y, color);
    }

    public static int textWidth(Font font, String text) { return font.width(text); }

    // ── Slider ─────────────────────────────────────────────────────────────────

    public static void drawSlider(GuiGraphics g, Font font, int x, int y, int w, int h,
                                  double value, double min, double max,
                                  String label, Theme theme) {
        float pct = (float)((value - min) / (max - min));
        int   fw  = (int)(w * pct);

        // Track
        roundedRect(g, x, y, w, h, 3f, theme.backgroundDark);
        // Fill
        roundedRect(g, x, y, fw, h, 3f, theme.accent);
        // Label
        String valStr = label + ": " + (value == (int)value
            ? String.valueOf((int) value)
            : String.format("%.2f", value));
        drawCenteredString(g, font, valStr, x + w / 2, y + (h - font.lineHeight) / 2, theme.textPrimary);
    }

    // ── Toggle ─────────────────────────────────────────────────────────────────

    public static void drawToggle(GuiGraphics g, int x, int y, boolean on, Theme theme) {
        int trackColor = on ? theme.toggleOn : theme.toggleOff;
        int tw = 24, th = 12;
        roundedRect(g, x, y, tw, th, th / 2f, trackColor);
        int knobX = on ? x + tw - th : x;
        roundedRect(g, knobX, y, th, th, th / 2f, 0xFFFFFFFF);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static int withAlpha(int color, float factor) {
        return Theme.withAlpha(color, factor);
    }
}

