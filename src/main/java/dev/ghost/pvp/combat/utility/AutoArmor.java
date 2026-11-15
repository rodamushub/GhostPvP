package dev.ghost.pvp.combat.utility;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * AutoArmor — equips the highest-protection armor from inventory automatically.
 * Scores each piece by protection level + enchantment value.
 */
public final class AutoArmor extends Module {

    private final Setting<Integer> delayMs = addInt("Delay", "ms between equip actions", 200, 0, 1000);
    private long lastAction = 0;

    public AutoArmor() { super("AutoArmor", "Automatically equips best available armor", Category.UTILITY); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (System.currentTimeMillis() - lastAction < delayMs.getValue()) return;
        var player = Globals.player();

        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        // Container slot indices for armor: 5=head, 6=chest, 7=legs, 8=feet
        int[] containerSlots = {5, 6, 7, 8};

        for (int i = 0; i < armorSlots.length; i++) {
            var current = player.getItemBySlot(armorSlots[i]);
            int bestSlot = -1;
            float bestScore = current.isEmpty() ? -1 : armorScore(current);

            for (int inv = 9; inv < 45; inv++) {
                var stack = player.getInventory().getItem(inv);
                if (stack.isEmpty()) continue;
                if (!fitsSlot(stack, armorSlots[i])) continue;
                float score = armorScore(stack);
                if (score > bestScore) { bestScore = score; bestSlot = inv; }
            }

            if (bestSlot != -1) {
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, bestSlot, 0, ClickType.PICKUP, player);
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, containerSlots[i], 0, ClickType.PICKUP, player);
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, bestSlot, 0, ClickType.PICKUP, player);
                lastAction = System.currentTimeMillis();
                return;
            }
        }
    }

    private float armorScore(ItemStack stack) {
        Item item = stack.getItem();
        // Approximate: netherite > diamond > iron > gold > chain > leather
        int base = 0;
        if (item instanceof ArmorItem ai) base = ai.getArmorValue();
        return base + stack.getEnchantments().size() * 2f;
    }

    private boolean fitsSlot(ItemStack stack, EquipmentSlot slot) {
        if (!(stack.getItem() instanceof ArmorItem ai)) return false;
        return ai.getType().getSlot() == slot;
    }
}

