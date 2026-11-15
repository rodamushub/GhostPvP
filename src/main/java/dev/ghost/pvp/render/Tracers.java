package dev.ghost.pvp.render;

import dev.ghost.pvp.GhostClient;
import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Tracers extends Module {

    private final Setting<Double>  range       = addDouble("Range", "Max draw distance", 128.0, 1.0, 512.0);
    private final Setting<Boolean> colorByDist = addBool("Color by Dist", "Fade color with distance", true);

    public Tracers() { super("Tracers", "Lines from screen center to players", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;
        var  cam    = Globals.mc.gameRenderer.getCamera();
        var  camPos = cam.getPosition();
        var  player = Globals.player();
        double r2   = range.getValue() * range.getValue();

        // Tracer origin: camera position
        Vec3 origin = Vec3.ZERO; // relative to camera = zero in translated space

        for (var entity : Globals.world().getEntities()) {
            if (!(entity instanceof Player p) || p == player) continue;
            if (player.distanceToSqr(p) > r2) continue;

            boolean isFriend = GhostClient.get().getFriendManager().isFriend(p.getName().getString());
            int r = isFriend ? 85 : 255;
            int g = isFriend ? 255 : 85;
            int b = 85;

            if (colorByDist.getValue()) {
                double dist = Math.sqrt(player.distanceToSqr(p));
                float  fade = (float) Math.max(0, 1.0 - dist / range.getValue());
                int    a    = (int)(100 + 155 * fade);
                Vec3   dest = new Vec3(p.getX() - camPos.x, p.getY() + p.getBbHeight() / 2 - camPos.y, p.getZ() - camPos.z);
                RenderUtil.drawTracer(e.poseStack, origin, dest, r, g, b, a, 1.0f);
            } else {
                Vec3 dest = new Vec3(p.getX() - camPos.x, p.getY() + p.getBbHeight() / 2 - camPos.y, p.getZ() - camPos.z);
                RenderUtil.drawTracer(e.poseStack, origin, dest, r, g, b, 200, 1.0f);
            }
        }
    }
}

