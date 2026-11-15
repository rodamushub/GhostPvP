package dev.ghost.pvp.render;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * PlayerESP — draws boxes + nametags over players with:
 *   health bar, totem count, ping indicator, armor durability.
 */
public final class PlayerESP extends Module {

    private final Setting<Boolean> showBox      = addBool("Box",      "Draw bounding box",         true);
    private final Setting<Boolean> showFill     = addBool("Fill",     "Fill box with color",        true);
    private final Setting<Boolean> showName     = addBool("Name",     "Show player name",           true);
    private final Setting<Boolean> showHealth   = addBool("Health",   "Show health bar",            true);
    private final Setting<Boolean> showTotems   = addBool("Totems",   "Show totem count",           true);
    private final Setting<Boolean> showPing     = addBool("Ping",     "Show ping",                  true);
    private final Setting<Boolean> showArmor    = addBool("Armor",    "Show armor durability",      false);
    private final Setting<Boolean> showFriends  = addBool("Friends",  "Render friends differently", true);
    private final Setting<Double>  range        = addDouble("Range",  "Max render range",           64.0, 1.0, 256.0);

    // Colors — friend vs enemy
    private static final int COLOR_ENEMY  = 0xFF5555;
    private static final int COLOR_FRIEND = 0x55FF55;
    private static final int COLOR_FILL_A = 30; // fill alpha

    public PlayerESP() { super("PlayerESP", "ESP boxes and nametags for players", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;
        LocalPlayer self   = Globals.player();
        var         camPos = Globals.mc.gameRenderer.getCamera().getPosition();
        Font        font   = Globals.mc.font;
        double      r2     = range.getValue() * range.getValue();

        for (var entity : Globals.world().getEntities()) {
            if (!(entity instanceof Player p) || p == self) continue;
            if (self.distanceToSqr(p) > r2) continue;

            boolean isFriend = GhostClient.get().getFriendManager().isFriend(p.getName().getString());
            int[] col = RenderUtil.colorFromHex(isFriend && showFriends.getValue() ? COLOR_FRIEND : COLOR_ENEMY);

            // Camera-relative AABB
            AABB box = p.getBoundingBox().move(-camPos.x, -camPos.y, -camPos.z)
                        .inflate(0.05);

            if (showBox.getValue())
                RenderUtil.drawBox(e.poseStack, box, col[0], col[1], col[2], 255, 1.5f);
            if (showFill.getValue())
                RenderUtil.drawFilledBox(e.poseStack, box, col[0], col[1], col[2], COLOR_FILL_A);

            // Screen-space tags
            Vec3 topWorld = new Vec3(p.getX(), p.getBoundingBox().maxY + 0.3, p.getZ());
            float[] screen = RenderUtil.worldToScreen(topWorld);
            if (screen == null) continue;

            float sx = screen[0], sy = screen[1];
            int lineHeight = font.lineHeight + 1;
            int line = 0;

            if (showName.getValue()) {
                String name = p.getName().getString();
                drawCenteredString(e, font, name, sx, sy - lineHeight * line++, 0xFFFFFF);
            }
            if (showHealth.getValue()) {
                float hp     = p.getHealth();
                float maxHp  = p.getMaxHealth();
                int   red    = (int)(255 * (1f - hp / maxHp));
                int   green  = (int)(255 * (hp / maxHp));
                String hpStr = String.format("%.1f❤", hp);
                drawCenteredString(e, font, hpStr, sx, sy - lineHeight * line++,
                    (red << 16) | (green << 8));
            }
            if (showTotems.getValue()) {
                int totems = countItem(p, Items.TOTEM_OF_UNDYING);
                drawCenteredString(e, font, "🪄" + totems, sx, sy - lineHeight * line++, 0xFFD700);
            }
            if (showPing.getValue()) {
                var info = Globals.mc.getConnection() == null ? null
                    : Globals.mc.getConnection().getPlayerInfo(p.getUUID());
                if (info != null) {
                    int ping = info.getLatency();
                    int pCol = ping < 80 ? 0x55FF55 : ping < 150 ? 0xFFAA00 : 0xFF5555;
                    drawCenteredString(e, font, ping + "ms", sx, sy - lineHeight * line++, pCol);
                }
            }
        }
    }

    private int countItem(Player p, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) count++;
        }
        // Also check offhand
        if (p.getOffhandItem().is(item)) count++;
        return count;
    }

    private void drawCenteredString(RenderWorldEvent e, Font font, String text, float x, float y, int color) {
        // 2D text overlay drawn via GuiGraphics workaround in world render
        // Actual implementation routes through HudRenderer's overlay layer.
        // Here we store pending draws; HudRenderer flushes them each frame.
        dev.ghost.pvp.ui.hud.WorldTextOverlay.queue(text, x, y, color);
    }
}

