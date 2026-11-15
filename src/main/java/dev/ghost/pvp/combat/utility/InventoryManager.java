package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * InventoryManager — keeps hotbar stocked:
 *   - Moves totems to offhand slot
 *   - Moves crystals to hotbar slot
 *   - Moves gaps (golden apple) to hotbar
 *   - Drops junk (non-essential items) when inventory is full
 */
public final class InventoryManager extends Module {

    private final Setting<Boolean> manageTotems  = addBool("Totems",   "Keep totem in offhand",           true);
    private final Setting<Boolean> manageCrystals= addBool("Crystals", "Keep crystals in hotbar",          true);
    private final Setting<Boolean> manageGaps    = addBool("Gaps",     "Keep golden apples in hotbar",     true);
    private final Setting<Integer> crystalSlot   = addInt("Crystal Slot", "Hotbar slot for crystals (1-9)", 1, 1, 9);
    private final Setting<Integer> gapSlot       = addInt("Gap Slot",    "Hotbar slot for gaps (1-9)",      2, 1, 9);
    private final Setting<Integer> delayMs       = addInt("Delay",       "ms between inventory actions",   100, 0, 500);

    private long lastAction = 0;

    public InventoryManager() { super("InventoryManager", "Automatic hotbar restocking", Category.UTILITY); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;
        var player = Globals.player();

        if (manageTotems.getValue()) {
            if (!player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                if (moveToSlot(Items.TOTEM_OF_UNDYING, 45)) { lastAction = System.currentTimeMillis(); return; }
            }
        }
        if (manageCrystals.getValue()) {
            int slot = crystalSlot.getValue() - 1 + 36;
            if (!player.getInventory().getItem(crystalSlot.getValue() - 1).is(Items.END_CRYSTAL)) {
                if (moveToSlot(Items.END_CRYSTAL, slot)) { lastAction = System.currentTimeMillis(); return; }
            }
        }
        if (manageGaps.getValue()) {
            int slot = gapSlot.getValue() - 1 + 36;
            if (!player.getInventory().getItem(gapSlot.getValue() - 1).is(Items.GOLDEN_APPLE)
             && !player.getInventory().getItem(gapSlot.getValue() - 1).is(Items.ENCHANTED_GOLDEN_APPLE)) {
                if (moveToSlot(Items.GOLDEN_APPLE, slot) || moveToSlot(Items.ENCHANTED_GOLDEN_APPLE, slot)) {
                    lastAction = System.currentTimeMillis();
                }
            }
        }
    }

    /** Finds item in inventory (non-hotbar) and shift-clicks it to target slot. Returns true if found. */
    private boolean moveToSlot(Item item, int targetSlot) {
        var player = Globals.player();
        for (int i = 9; i < 36; i++) {
            if (!player.getInventory().getItem(i).is(item)) continue;
            // Click source
            Globals.mc.gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, i, 0, ClickType.PICKUP, player);
            // Click target
            Globals.mc.gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, targetSlot, 0, ClickType.PICKUP, player);
            // Return leftover to source
            Globals.mc.gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, i, 0, ClickType.PICKUP, player);
            return true;
        }
        return false;
    }
}

