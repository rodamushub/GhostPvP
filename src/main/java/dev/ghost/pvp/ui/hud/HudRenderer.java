package dev.ghost.pvp.ui.hud;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.module.ModuleManager;
import dev.ghost.pvp.render.CombatHUD;
import dev.ghost.pvp.render.CustomCrosshair;
import dev.ghost.pvp.ui.notifications.NotificationManager;
import dev.ghost.pvp.ui.theme.Theme;
import dev.ghost.pvp.util.Globals;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.List;

public final class HudRenderer {

    private final ModuleManager       mm;
    private final NotificationManager nm;

    private int totemPops = 0;
    private int comboHits = 0;

    public HudRenderer(ModuleManager mm, NotificationManager nm) {
        this.mm = mm;
        this.nm = nm;
    }

    public void incrementTotemPops() { totemPops++; }
    public void incrementCombo()     { comboHits++; }
    public void resetCombo()         { comboHits = 0; }

    public void render(GuiGraphics g, float delta) {
        if (!Globals.inGame()) return;

        renderArraylist(g);
        renderCombatHUD(g);
        renderCrosshair(g);
        renderWorldTextOverlay(g);
        nm.render(g, delta);
    }

    // ── Arraylist ──────────────────────────────────────────────────────────────

    private void renderArraylist(GuiGraphics g) {
        var font = Globals.mc.font;
        int sw   = Globals.mc.getWindow().getGuiScaledWidth();
        int y    = 2;

        List<Module> enabled = mm.getModules().stream()
            .filter(Module::isEnabled)
            .sorted((a, b) -> font.width(b.getName()) - font.width(a.getName()))
            .toList();

        for (Module m : enabled) {
            String name = m.getName();
            int    w    = font.width(name);
            int    x    = sw - w - 3;
            int[]  col  = hexToRGB(m.getCategory().color);

            // Shadow
            g.fill(x - 1, y - 1, x + w + 1, y + font.lineHeight + 1, 0x40000000);
            // Category color bar
            g.fill(sw - 2, y, sw, y + font.lineHeight, withA(m.getCategory().color, 200));
            // Text
            g.drawString(font, name, x, y, withA(m.getCategory().color, 255), false);

            y += font.lineHeight + 1;
        }
    }

    // ── CombatHUD ──────────────────────────────────────────────────────────────

    private void renderCombatHUD(GuiGraphics g) {
        var mod = mm.get("CombatHUD");
        if (mod.isEmpty() || !mod.get().isEnabled()) return;
        var hud  = (CombatHUD) mod.get();
        var font = Globals.mc.font;
        var player = Globals.player();
        int x = hud.posX.getValue();
        int y = hud.posY.getValue();
        int lh = font.lineHeight + 2;

        // Panel background
        int panelW = 120, panelH = 0;
        if (hud.totemCounter.getValue()) panelH += lh;
        if (hud.armorDurability.getValue()) panelH += lh;
        if (hud.targetHealth.getValue()) panelH += lh + 4;
        if (hud.comboCounter.getValue()) panelH += lh;

        g.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, 0xAA111118);

        int row = y;

        if (hud.totemCounter.getValue()) {
            int totems = countItem(player, Items.TOTEM_OF_UNDYING);
            g.drawString(font, "🪄 Totems: " + totems, x, row, 0xFFD700, true);
            g.drawString(font, "🪦 Pops: " + totemPops, x + 70, row, 0xFF8888, true);
            row += lh;
        }

        if (hud.armorDurability.getValue()) {
            renderArmorDurability(g, x, row);
            row += lh;
        }

        if (hud.targetHealth.getValue()) {
            renderTargetHealth(g, x, row, panelW);
            row += lh + 4;
        }

        if (hud.comboCounter.getValue() && comboHits > 0) {
            g.drawString(font, "✦ Combo: " + comboHits, x, row, 0xFF55FF, true);
            row += lh;
        }
    }

    private void renderArmorDurability(GuiGraphics g, int x, int y) {
        var player = Globals.player();
        var font   = Globals.mc.font;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] icons = {"⛑", "🧥", "👖", "👟"};
        int bx = x;

        for (int i = 0; i < slots.length; i++) {
            var stack = player.getItemBySlot(slots[i]);
            if (stack.isEmpty()) continue;
            int durability = stack.getMaxDamage() - stack.getDamageValue();
            int maxDur     = stack.getMaxDamage();
            if (maxDur <= 0) continue;
            float pct = (float) durability / maxDur;
            int   col = pct > 0.5f ? 0x55FF55 : pct > 0.25f ? 0xFFAA00 : 0xFF5555;

            // Mini bar
            int barW = 20;
            g.fill(bx, y, bx + barW, y + 4, 0x40FFFFFF);
            g.fill(bx, y, bx + (int)(barW * pct), y + 4, col);
            g.drawString(font, icons[i], bx, y + 5, 0xFFFFFF, false);
            bx += barW + 6;
        }
    }

    private void renderTargetHealth(GuiGraphics g, int x, int y, int w) {
        var font   = Globals.mc.font;
        var self   = Globals.player();
        Player target = null;
        double bd = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == self) continue;
            if (GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = self.distanceToSqr(p);
            if (d < bd) { target = p; bd = d; }
        }
        if (target == null) return;

        float hp    = target.getHealth();
        float maxHp = target.getMaxHealth();
        float pct   = hp / maxHp;
        int   col   = pct > 0.5f ? 0x55FF55 : pct > 0.25f ? 0xFFAA00 : 0xFF5555;

        g.drawString(font, target.getName().getString(), x, y, 0xFFFFFF, true);
        g.fill(x, y + font.lineHeight + 1, x + w, y + font.lineHeight + 5, 0x40FFFFFF);
        g.fill(x, y + font.lineHeight + 1, x + (int)(w * pct), y + font.lineHeight + 5, col);
        g.drawString(font, String.format("%.1f / %.1f", hp, maxHp),
            x + w / 2 - font.width(String.format("%.1f / %.1f", hp, maxHp)) / 2,
            y + font.lineHeight + 6, col, true);
    }

    // ── Crosshair ──────────────────────────────────────────────────────────────

    private void renderCrosshair(GuiGraphics g) {
        var mod = mm.get("CustomCrosshair");
        if (mod.isEmpty() || !mod.get().isEnabled()) return;
        var ch  = (CustomCrosshair) mod.get();
        var mc  = Globals.mc;
        int sw  = mc.getWindow().getGuiScaledWidth();
        int sh  = mc.getWindow().getGuiScaledHeight();
        int cx  = sw / 2, cy = sh / 2;
        int sz  = ch.size.getValue();
        int gap = ch.gap.getValue();
        int col = (ch.alpha.getValue() << 24)|(ch.colorR.getValue()<<16)|(ch.colorG.getValue()<<8)|ch.colorB.getValue();

        switch (ch.style.getValue()) {
            case CROSS -> {
                g.fill(cx - sz - gap, cy - 1, cx - gap, cy + 1, col);
                g.fill(cx + gap,      cy - 1, cx + sz + gap, cy + 1, col);
                g.fill(cx - 1, cy - sz - gap, cx + 1, cy - gap, col);
                g.fill(cx - 1, cy + gap,      cx + 1, cy + sz + gap, col);
            }
            case DOT   -> g.fill(cx - 2, cy - 2, cx + 2, cy + 2, col);
            case CIRCLE -> {
                for (int deg = 0; deg < 360; deg += 5) {
                    double rad = Math.toRadians(deg);
                    int px = cx + (int)(Math.cos(rad) * sz);
                    int py = cy + (int)(Math.sin(rad) * sz);
                    g.fill(px - 1, py - 1, px + 1, py + 1, col);
                }
            }
            case DYNAMIC -> {
                // Expand gap based on player speed
                double speed = Globals.player().getDeltaMovement().horizontalDistance();
                int dynGap   = gap + (int)(speed * 10);
                g.fill(cx - sz - dynGap, cy - 1, cx - dynGap, cy + 1, col);
                g.fill(cx + dynGap,      cy - 1, cx + sz + dynGap, cy + 1, col);
                g.fill(cx - 1, cy - sz - dynGap, cx + 1, cy - dynGap, col);
                g.fill(cx - 1, cy + dynGap,      cx + 1, cy + sz + dynGap, col);
            }
        }
    }

    // ── World Text Overlay ─────────────────────────────────────────────────────

    private void renderWorldTextOverlay(GuiGraphics g) {
        var font    = Globals.mc.font;
        for (var entry : WorldTextOverlay.flushAndGet()) {
            int x = (int) entry.x() - font.width(entry.text()) / 2;
            int y = (int) entry.y();
            g.drawString(font, entry.text(), x, y, entry.color(), true);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int countItem(Player p, net.minecraft.world.item.Item item) {
        int c = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) c++;
        }
        if (p.getOffhandItem().is(item)) c++;
        return c;
    }

    private static int withA(int hex, int a) { return (a << 24) | (hex & 0x00FFFFFF); }
    private static int[] hexToRGB(int hex)   { return new int[]{(hex>>16)&0xFF,(hex>>8)&0xFF,hex&0xFF}; }
}

