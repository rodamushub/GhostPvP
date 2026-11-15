package dev.ghost.pvp.combat.movement;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * ElytraAssist — auto-deploys elytra and auto-swaps armor↔elytra.
 *
 * AUTO_DEPLOY  — opens elytra when falling
 * AUTO_SWAP    — equips elytra before flight, swaps back to chestplate on land
 */
public final class ElytraAssist extends Module {

    public enum ElytraMode { AUTO_DEPLOY, AUTO_SWAP, BOTH }

    private final Setting<ElytraMode> mode       = addEnum("Mode", "Elytra assist mode", ElytraMode.BOTH);
    private final Setting<Double>     deployVel   = addDouble("Deploy Velocity", "Fall speed before deploy", 0.5, 0.1, 3.0);

    private boolean swapped = false;

    public ElytraAssist() { super("ElytraAssist", "Auto-deploy and armor swap for elytra", Category.MOVEMENT); }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        boolean falling = player.getDeltaMovement().y < -deployVel.getValue() && !player.onGround();
        boolean wearingElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);

        if ((mode.getValue() == ElytraMode.AUTO_DEPLOY || mode.getValue() == ElytraMode.BOTH)
            && falling && wearingElytra && !player.isFallFlying()) {
            Globals.mc.options.keyJump.setDown(true);
        }

        if (mode.getValue() == ElytraMode.AUTO_SWAP || mode.getValue() == ElytraMode.BOTH) {
            if (falling && !wearingElytra && !swapped) {
                swapChestpiece(player, true);
                swapped = true;
            } else if (player.onGround() && swapped) {
                swapChestpiece(player, false);
                swapped = false;
            }
        }
    }

    private void swapChestpiece(net.minecraft.client.player.LocalPlayer player, boolean toElytra) {
        // Find elytra or chestplate in inventory and swap to chest slot
        for (int i = 9; i < 36; i++) {
            var stack = player.getInventory().getItem(i);
            boolean match = toElytra ? stack.is(Items.ELYTRA) : isChestplate(stack);
            if (match) {
                // Click to chest slot (6 in MC inventory)
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, i, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, player);
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, 6, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, player);
                Globals.mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId, i, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, player);
                return;
            }
        }
    }

    private boolean isChestplate(net.minecraft.world.item.ItemStack s) {
        return s.is(Items.NETHERITE_CHESTPLATE) || s.is(Items.DIAMOND_CHESTPLATE)
            || s.is(Items.IRON_CHESTPLATE) || s.is(Items.GOLDEN_CHESTPLATE)
            || s.is(Items.CHAINMAIL_CHESTPLATE) || s.is(Items.LEATHER_CHESTPLATE);
    }
}

