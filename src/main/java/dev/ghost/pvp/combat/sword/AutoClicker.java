package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;

import java.util.Random;

/** AutoClicker — simulates left clicks at configurable CPS. */
public final class AutoClicker extends Module {

    private final Setting<Integer> cpsMin = addInt("CPS Min", "Min clicks per second", 10, 1, 20);
    private final Setting<Integer> cpsMax = addInt("CPS Max", "Max clicks per second", 14, 1, 20);
    private final Setting<Boolean> onlyHeld = addBool("Only Held", "Only click when LMB held", true);

    private long lastClick = 0;
    private long nextDelay = 100;
    private final Random rng = new Random();

    public AutoClicker() { super("AutoClicker", "Simulates clicks at configurable CPS", Category.COMBAT); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (onlyHeld.getValue() && !Globals.mc.options.keyAttack.isDown()) return;

        long now = System.currentTimeMillis();
        if (now - lastClick < nextDelay) return;

        int cps = cpsMin.getValue() + rng.nextInt(Math.max(1, cpsMax.getValue() - cpsMin.getValue() + 1));
        nextDelay = 1000L / cps + rng.nextInt(10) - 5;
        lastClick = now;

        if (Globals.mc.hitResult != null) {
            if (Globals.mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                Globals.mc.gameMode.attack(Globals.player(), ehr.getEntity());
            }
        }
    }
}

