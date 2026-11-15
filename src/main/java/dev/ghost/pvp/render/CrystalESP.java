package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;

public final class CrystalESP extends Module {

    private final Setting<Boolean> showFill = addBool("Fill", "Filled box", false);

    public CrystalESP() { super("CrystalESP", "Highlights End Crystals", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;
        var camPos = Globals.mc.gameRenderer.getCamera().getPosition();

        for (var entity : Globals.world().getEntities()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            AABB box = crystal.getBoundingBox().move(-camPos.x, -camPos.y, -camPos.z);
            RenderUtil.drawBox(e.poseStack, box, 255, 0, 255, 255, 1.0f);
            if (showFill.getValue())
                RenderUtil.drawFilledBox(e.poseStack, box, 255, 0, 255, 25);
        }
    }
}

