package dev.ghost.pvp.ui.notifications;

import dev.ghost.pvp.ui.clickgui.GuiRenderer;
import dev.ghost.pvp.ui.theme.Theme;
import dev.ghost.pvp.util.Globals;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

/**
 * NotificationManager — renders toast notifications in the top-right corner.
 * Each notification slides in from the right and fades out.
 */
public final class NotificationManager {

    private final Deque<Notification> queue = new ArrayDeque<>();
    private Theme theme = Theme.DARK();

    public void setTheme(Theme t) { this.theme = t; }

    public void push(String title, String message, long durationMs) {
        queue.addLast(new Notification(title, message, durationMs));
    }

    public void render(GuiGraphics g, float delta) {
        if (Globals.mc == null) return;
        int sw   = Globals.mc.getWindow().getGuiScaledWidth();
        var font = Globals.mc.font;
        long now = System.currentTimeMillis();

        queue.removeIf(n -> now > n.expiresAt + 400); // 400ms exit anim

        int y = 32;
        int notifW = 160;

        for (Notification n : queue) {
            long remaining = n.expiresAt - now;
            float progress; // 0 = hidden, 1 = fully shown
            if (now < n.createdAt + 200) {
                progress = (float)(now - n.createdAt) / 200f;
            } else if (remaining > 0) {
                progress = 1f;
            } else {
                progress = Math.max(0, 1f + remaining / 400f);
            }

            int slideX = (int)((1f - progress) * (notifW + 4));
            int x      = sw - notifW - 4 + slideX;
            int alpha  = (int)(progress * 220);

            int bgColor = Theme.withAlpha(theme.backgroundDark, progress * 0.9f);
            int barColor= Theme.withAlpha(theme.accent, progress);

            // Background
            GuiRenderer.roundedRect(g, x, y, notifW, 28, 4f, bgColor);
            // Left accent bar
            g.fill(x, y + 2, x + 3, y + 26, barColor);
            // Title
            g.drawString(font, n.title, x + 7, y + 4, Theme.withAlpha(0xFFFFFF, progress), false);
            // Message
            g.drawString(font, n.message, x + 7, y + 4 + font.lineHeight + 2,
                Theme.withAlpha(theme.textSecondary, progress), false);

            y += 32;
        }
    }

    private static final class Notification {
        final String title, message;
        final long   createdAt;
        final long   expiresAt;

        Notification(String title, String message, long durationMs) {
            this.title     = title;
            this.message   = message;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = createdAt + durationMs;
        }
    }
}

