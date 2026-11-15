package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.MoveEvent;
import dev.ghost.pvp.event.events.PacketEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

/**
 * Velocity / Anti-Knockback
 *
 * Modes:
 *   MODIFY  — reduces velocity on incoming motion packet (most compatible)
 *   ZERO    — zeroes velocity entirely
 *   JUMP    — jumps on hit to negate knockback naturally (legit)
 *   CANCEL  — cancels the motion packet outright (aggressive, easy to detect)
 */
public final class Velocity extends Module {

    public enum VeloMode { MODIFY, ZERO, JUMP, CANCEL }

    private final Setting<VeloMode> mode   = addEnum("Mode", "Anti-KB method", VeloMode.MODIFY);
    private final Setting<Double>   hMulti = addDouble("Horizontal", "Horizontal velocity multiplier", 0.0, 0.0, 1.0);
    private final Setting<Double>   vMulti = addDouble("Vertical",   "Vertical velocity multiplier",   0.0, 0.0, 1.0);

    public Velocity() { super("Velocity", "Reduces or cancels knockback", Category.MOVEMENT); }

    @Override protected void registerListeners() {
        listen(PacketEvent.class, this::onPacket);
    }

    private void onPacket(PacketEvent e) {
        if (e.direction != PacketEvent.Direction.RECEIVE) return;
        if (!(e.packet instanceof ClientboundSetEntityMotionPacket pkt)) return;
        var player = Globals.player();
        if (player == null || pkt.getId() != player.getId()) return;

        switch (mode.getValue()) {
            case CANCEL -> e.setCancelled(true);
            case ZERO   -> { zeroMotion(pkt); }
            case MODIFY -> { modifyMotion(pkt); }
            case JUMP   -> {
                if (player.onGround()) player.jumpFromGround();
            }
        }
    }

    private void zeroMotion(ClientboundSetEntityMotionPacket pkt) {
        // Reflect-set fields to zero — in MC 26.x these are public records or have setters
        // Values are encoded as short * 8000; zero = 0
        // We rely on our PacketEvent mixin to allow field mutation before dispatch
        try {
            var xa = pkt.getClass().getDeclaredField("xa");
            var ya = pkt.getClass().getDeclaredField("ya");
            var za = pkt.getClass().getDeclaredField("za");
            xa.setAccessible(true); ya.setAccessible(true); za.setAccessible(true);
            xa.setShort(pkt, (short) 0);
            ya.setShort(pkt, (short) 0);
            za.setShort(pkt, (short) 0);
        } catch (Exception ex) { /* field names differ — per-build derivation required */ }
    }

    private void modifyMotion(ClientboundSetEntityMotionPacket pkt) {
        try {
            var xa = pkt.getClass().getDeclaredField("xa");
            var ya = pkt.getClass().getDeclaredField("ya");
            var za = pkt.getClass().getDeclaredField("za");
            xa.setAccessible(true); ya.setAccessible(true); za.setAccessible(true);
            short hm = (short)(hMulti.getValue() * 10000);
            short vm = (short)(vMulti.getValue() * 10000);
            xa.setShort(pkt, (short)(xa.getShort(pkt) * hMulti.getValue()));
            ya.setShort(pkt, (short)(ya.getShort(pkt) * vMulti.getValue()));
            za.setShort(pkt, (short)(za.getShort(pkt) * hMulti.getValue()));
        } catch (Exception ex) { /* field names differ — per-build derivation required */ }
    }
}

