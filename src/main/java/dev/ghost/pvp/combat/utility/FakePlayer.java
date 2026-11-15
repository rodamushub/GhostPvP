package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.event.events.KeyPressEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import org.lwjgl.glfw.GLFW;

/**
 * FakePlayer — spawns a client-side dummy player at current position for testing
 * crystal placement / module calibration without a real target.
 * Key R (by default) spawns/removes the dummy.
 */
public final class FakePlayer extends Module {

    private RemotePlayer dummy = null;

    public FakePlayer() { super("FakePlayer", "Spawns a dummy player for module testing", Category.UTILITY); }

    @Override protected void registerListeners() { listen(KeyPressEvent.class, this::onKey); }

    @Override protected void onDisable() { removeDummy(); }

    private void onKey(KeyPressEvent e) {
        if (e.action != GLFW.GLFW_PRESS || e.key != GLFW.GLFW_KEY_R) return;
        if (!Globals.inGame()) return;
        if (dummy == null) spawnDummy();
        else removeDummy();
    }

    private void spawnDummy() {
        var player = Globals.player();
        var world  = (ClientLevel) Globals.world();
        // RemotePlayer is a client-only entity; it won't sync to server
        dummy = new RemotePlayer(world, player.getGameProfile());
        dummy.setPos(player.getX(), player.getY(), player.getZ()); {
            dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }
        world.addFreshEntity( dummy);
    }

    private void removeDummy() {
        if (dummy == null) return;
        var world = (ClientLevel) Globals.world();
        if (world != null) world.removeEntity(dummy.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        dummy = null;
    }
}

