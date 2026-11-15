package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Scaffold / FastBridge — places blocks below the player as they walk.
 * LEGIT mode: looks down slightly (visible rotation change).
 * SILENT mode: places via packet without rotating.
 */
public final class Scaffold extends Module {

    public enum ScaffoldMode { LEGIT, SILENT }

    private final Setting<ScaffoldMode> mode      = addEnum("Mode", "Scaffold method", ScaffoldMode.SILENT);
    private final Setting<Boolean>      tower      = addBool("Tower", "Tower upward on jump key", false);
    private final Setting<Boolean>      safeWalk   = addBool("Safe Walk", "Prevent walking off edge", true);

    private float savedYaw, savedPitch;

    public Scaffold() { super("Scaffold", "Automatic block bridging beneath player", Category.UTILITY); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        // Find block slot
        int slot = findBlockSlot(player);
        if (slot == -1) return;

        BlockPos placePos = player.blockPosition().below();
        if (!Globals.world().getBlockState(placePos).isAir()) {
            if (tower.getValue() && Globals.mc.options.keyJump.isDown()) {
                placePos = player.blockPosition().above();
            } else return;
        }

        int prev = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);

        if (mode.getValue() == ScaffoldMode.LEGIT) {
            savedYaw   = player.getYRot();
            savedPitch = player.getXRot();
            player.setXRot(80f);
        }

        var hitResult = new BlockHitResult(
            Vec3.atCenterOf(placePos), Direction.UP, placePos, false);
        Globals.mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);

        if (mode.getValue() == ScaffoldMode.LEGIT) {
            player.setYRot(savedYaw);
            player.setXRot(savedPitch);
        }

        player.getInventory().setSelectedSlot(prev);
    }

    private int findBlockSlot(net.minecraft.client.player.LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}

