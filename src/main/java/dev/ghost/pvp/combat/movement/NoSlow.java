package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.event.events.PacketEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/**
 * NoSlow — prevents speed reduction while using items (eating, blocking, drawing bow).
 *
 * Method: on each tick the client sends action START_DESTROY_BLOCK / ABORT_DESTROY_BLOCK
 * to cancel the using-item state server-side while still applying the effect client-side.
 * More reliable method: spoof via packet that player is not "using" at movement time.
 */
public final class NoSlow extends Module {

    public NoSlow() { super("NoSlow", "Full speed while eating, blocking, or using items", Category.MOVEMENT); }

    @Override protected void registerListeners() {
        listen(dev.ghost.pvp.event.events.TickEvent.class, this::onTick);
    }

    private void onTick(dev.ghost.pvp.event.events.TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();
        var conn   = Globals.mc.getConnection();
        if (conn == null) return;

        // If using an item and moving, send a release + re-use in same tick
        if (player.isUsingItem()) {
            conn.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.Direction.DOWN
            ));
        }
    }
}

