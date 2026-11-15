package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.AttackEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Criticals — forces critical hits on attack.
 *
 * Packet mode: sends micro Y-offset move packets just before the attack
 * to satisfy the server's falling-frame crit condition.
 * Vanilla crit requires: not on ground, not in liquid, not on ladder, falling.
 */
public final class Criticals extends Module {

    public enum CritMode { PACKET, JUMP, NOGROUND }

    private final Setting<CritMode> mode = addEnum("Mode", "Crit method", CritMode.PACKET);

    public Criticals() {
        super("Criticals", "Forces critical hits on every attack", Category.COMBAT);
    }

    @Override protected void registerListeners() { listen(AttackEvent.class, this::onAttack); }

    private void onAttack(AttackEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();
        var conn   = Globals.mc.getConnection();
        if (conn == null) return;

        switch (mode.getValue()) {
            case PACKET -> {
                // Send fall packets: up slightly then down (vanilla falling frame)
                double x = player.getX(), y = player.getY(), z = player.getZ();
                conn.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false));
                conn.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
                conn.send(new ServerboundMovePlayerPacket.Pos(x, y + 1.1e-7, z, false));
                conn.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
            }
            case JUMP -> {
                if (player.onGround()) {
                    player.jumpFromGround();
                }
            }
            case NOGROUND -> {
                // Temporarily spoof onGround = false via packet
                conn.send(new ServerboundMovePlayerPacket.PosRot(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), false));
            }
        }
    }
}

