package dev.ghost.pvp.render;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * TargetESP — bright pulsing box + corner brackets on the targeted player.
 * Reads target from KillAura module if present.
 */
public final class TargetESP extends Module {

    private final Setting<Boolean> pulse    = addBool("Pulse", "Animate opacity", true);
    private final Setting<Boolean> brackets = addBool("Brackets", "Draw corner brackets", true);

    private float pulsePhase = 0f;

    public TargetESP() { super("TargetESP", "Highlights your current combat target", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;

        // Find nearest enemy as a proxy for target (KillAura shares no ref here; use closest)
        var    player = Globals.player();
        Player target = null;
        double bd     = Double.MAX_VALUE;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == player) continue;
            if (GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = player.distanceToSqr(p);
            if (d < bd) { target = p; bd = d; }
        }
        if (target == null) return;

        var cam = Globals.mc.gameRenderer.getCamera().getPosition();
        AABB box = target.getBoundingBox().move(-cam.x, -cam.y, -cam.z).inflate(0.08);

        pulsePhase = (pulsePhase + 0.05f) % (2f * (float) Math.PI);
        int alpha  = pulse.getValue() ? (int)(180 + 75 * Math.sin(pulsePhase)) : 255;

        RenderUtil.drawBox(e.poseStack, box, 255, 50, 50, alpha, 2.0f);
        RenderUtil.drawFilledBox(e.poseStack, box, 255, 50, 50, 15);
    }
}

