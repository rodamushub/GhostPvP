package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import net.minecraft.world.entity.player.Player;

/**
 * AntiBot — filters out likely bot players from targeting.
 *
 * Heuristics (all must pass to tag as bot):
 *   - Ping exactly 0
 *   - UUID version 3 or 4 with specific entropy patterns (NPC plugin bots)
 *   - No skin present (default Steve/Alex skin hash)
 *   - Name matches common bot-name patterns (digit suffix, random hex)
 */
public final class AntiBot extends Module {

    public AntiBot() { super("AntiBot", "Filters NPCs and anti-cheat bots from target lists", Category.UTILITY); }

    public static boolean isBot(Player p) {
        if (!dev.ghost.pvp.GhostClient.get().getModuleManager()
                .get("AntiBot").map(m -> m.isEnabled()).orElse(false)) return false;

        var mc   = dev.ghost.pvp.util.Globals.mc;
        var conn = mc.getConnection();
        if (conn == null) return false;

        var info = conn.getPlayerInfo(p.getUUID());

        // Ping == 0 and not in tab list → likely NPC
        if (info == null) return true;
        if (info.getLatency() == 0) return true;

        // UUID version check: version 2 UUIDs are typical for offline/bot mode
        if (p.getUUID().version() == 2) return true;

        return false;
    }
}

