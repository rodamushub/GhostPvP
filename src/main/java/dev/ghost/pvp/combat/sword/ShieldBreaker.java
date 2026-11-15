package dev.ghost.pvp.combat.sword;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * ShieldBreaker — detects when target is blocking and auto-switches
 * to an axe to cancel the shield (axes bypass shields on hit).
 */
public final class ShieldBreaker extends Module {

    private final Setting<Double>  range       = addDouble("Range", "Target range", 4.0, 1.0, 6.0);
    private final Setting<Boolean> autoSwitch  = addBool("Auto Switch", "Switch to axe automatically", true);
    private final Setting<Boolean> switchBack  = addBool("Switch Back", "Return to previous slot after", true);

    private int savedSlot = -1;

    public ShieldBreaker() {
        super("ShieldBreaker", "Auto-switches to axe to break shields", Category.COMBAT);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame() || !autoSwitch.getValue()) return;
        var player = Globals.player();
        double r2  = range.getValue() * range.getValue();

        boolean enemyBlocking = false;
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == player) continue;
            if (player.distanceToSqr(p) > r2) continue;
            if (p.isBlocking()) { enemyBlocking = true; break; }
        }

        if (enemyBlocking) {
            if (!isHoldingAxe(player)) {
                savedSlot = player.getInventory().getSelectedSlot();
                int axeSlot = findAxe(player);
                if (axeSlot != -1) player.getInventory().setSelectedSlot(axeSlot);
            }
        } else if (savedSlot != -1 && switchBack.getValue()) {
            player.getInventory().setSelectedSlot(savedSlot);
            savedSlot = -1;
        }
    }

    private boolean isHoldingAxe(Player p) {
        return isAxe(p.getMainHandItem());
    }

    private boolean isAxe(ItemStack stack) {
        return stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)
            || stack.is(Items.IRON_AXE) || stack.is(Items.GOLDEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.WOODEN_AXE);
    }

    private int findAxe(Player p) {
        for (int i = 0; i < 9; i++) {
            if (isAxe(p.getInventory().getItem(i))) return i;
        }
        return -1;
    }
}

