package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

/**
 * TriggerBot — attacks when crosshair is directly on a player entity.
 *
 * Modes:
 *   DIRECT  — calls attack() directly
 *   VIRTUAL — simulates a click by temporarily enabling left-click key state
 */
public final class TriggerBot extends Module {

    public enum TriggerMode { DIRECT, VIRTUAL }

    private final Setting<TriggerMode> mode      = addEnum("Mode", "Attack method", TriggerMode.DIRECT);
    private final Setting<Integer>     delayMin  = addInt("Delay Min", "Min ms before triggering", 20, 0, 200);
    private final Setting<Integer>     delayMax  = addInt("Delay Max", "Max ms before triggering", 60, 0, 300);

    private long lastAttack = 0;
    private long nextDelay  = 50;
    private final Random rng = new Random();

    public TriggerBot() {
        super("TriggerBot", "Attacks when crosshair is on a player", Category.COMBAT);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var mc = Globals.mc;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof net.minecraft.world.entity.player.Player target)) return;
        if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(target.getName().getString())) return;

        long now = System.currentTimeMillis();
        if (now - lastAttack < nextDelay) return;

        switch (mode.getValue()) {
            case DIRECT  -> mc.gameMode.attack(mc.player, target);
            case VIRTUAL -> { mc.options.keyAttack.setDown(true); }
        }

        lastAttack = now;
        nextDelay  = delayMin.getValue() + rng.nextInt(
            Math.max(1, delayMax.getValue() - delayMin.getValue()));
    }
}

