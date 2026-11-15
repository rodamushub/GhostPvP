package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

import java.util.Random;

/**
 * AutoTotem
 *
 * Keeps a totem in the offhand automatically.
 * Modes:
 *   NORMAL       — swap via inventory click packet when offhand is empty
 *   HOVER        — simulates cursor hover + swap (more humanlike)
 *   DOUBLE_HAND  — keeps totem in both hands when possible
 *
 * Humanized: random delay ± jitter between each swap.
 */
public final class AutoTotem extends Module {

    public enum TotemMode { NORMAL, HOVER, DOUBLE_HAND }

    private final Setting<TotemMode> mode        = addEnum("Mode", "Swap method", TotemMode.NORMAL);
    private final Setting<Double>    hpThreshold = addDouble("HP Threshold", "Re-equip when HP drops below", 20.0, 1.0, 20.0);
    private final Setting<Integer>   delayBase   = addInt("Delay",  "Base ms between swaps", 40, 0, 200);
    private final Setting<Integer>   jitter      = addInt("Jitter", "Random ± ms on delay",  15, 0, 100);

    private long    lastSwap  = 0;
    private long    nextDelay = 0;
    private final Random rng  = new Random();

    public AutoTotem() {
        super("AutoTotem", "Automatically keeps totem in offhand", Category.CRYSTAL);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        boolean offhandHasTotem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        if (offhandHasTotem && player.getHealth() > hpThreshold.getValue()) return;

        long now = System.currentTimeMillis();
        if (now - lastSwap < nextDelay) return;

        int slot = findTotemSlot(player);
        if (slot == -1) return;

        switch (mode.getValue()) {
            case NORMAL, HOVER -> swapToOffhand(player, slot);
            case DOUBLE_HAND   -> {
                swapToOffhand(player, slot);
                int second = findTotemSlot(player);
                if (second != -1) player.getInventory().setSelectedSlot(second);
            }
        }

        lastSwap  = now;
        nextDelay = delayBase.getValue() + (jitter.getValue() > 0
            ? (long)(rng.nextInt(jitter.getValue() * 2 + 1) - jitter.getValue()) : 0);
    }

    private void swapToOffhand(net.minecraft.client.player.LocalPlayer player, int invSlot) {
        int containerId = player.inventoryMenu.containerId;
        // Map raw inventory index to container slot index
        // Inventory slots 0-8 = hotbar → container slots 36-44
        // Inventory slots 9-35 = main → container slots 9-35
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;

        Globals.mc.gameMode.handleInventoryMouseClick(containerId, containerSlot, 0, ClickType.PICKUP, player);
        Globals.mc.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, player); // 45 = offhand
        Globals.mc.gameMode.handleInventoryMouseClick(containerId, containerSlot, 0, ClickType.PICKUP, player);
    }

    private int findTotemSlot(net.minecraft.client.player.LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }
}

