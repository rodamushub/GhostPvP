package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;

public final class StorageESP extends Module {

    private final Setting<Boolean> chests    = addBool("Chests",    "Highlight chests",          true);
    private final Setting<Boolean> shulkers  = addBool("Shulkers",  "Highlight shulker boxes",   true);
    private final Setting<Boolean> furnaces  = addBool("Furnaces",  "Highlight furnaces",        false);
    private final Setting<Boolean> hoppers   = addBool("Hoppers",   "Highlight hoppers",         false);
    private final Setting<Integer> range     = addInt("Range",      "Scan range",                48, 1, 128);

    public StorageESP() { super("StorageESP", "Highlights storage blocks in the world", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;
        var cam    = Globals.mc.gameRenderer.getCamera().getPosition();
        var player = Globals.player();
        double r2  = range.getValue() * (double) range.getValue();

        for (var be : Globals.world().getBlockEntities()) {
            if (player.distanceToSqr(be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ()) > r2) continue;

            int[] color = null;
            if      (be instanceof ChestBlockEntity      && chests.getValue())   color = new int[]{255, 170, 0};
            else if (be instanceof ShulkerBoxBlockEntity && shulkers.getValue()) color = new int[]{148, 0, 211};
            else if (be instanceof AbstractFurnaceBlockEntity && furnaces.getValue()) color = new int[]{255, 80, 0};
            else if (be instanceof HopperBlockEntity     && hoppers.getValue())  color = new int[]{80, 80, 80};

            if (color == null) continue;

            AABB box = new AABB(be.getBlockPos()).move(-cam.x, -cam.y, -cam.z).inflate(0.01);
            RenderUtil.drawBox(e.poseStack, box, color[0], color[1], color[2], 255, 1.2f);
            RenderUtil.drawFilledBox(e.poseStack, box, color[0], color[1], color[2], 20);
        }
    }
}

