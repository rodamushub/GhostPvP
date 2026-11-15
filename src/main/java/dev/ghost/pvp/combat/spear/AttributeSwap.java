package dev.ghost.pvp.combat.spear;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.AttackEvent;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

/**
 * AttributeSwap (Smart Mode)
 *
 * On pre-attack, silently swaps to a weapon with the desired attribute component,
 * sends the attack, then swaps back — all within the same tick.
 *
 * Sub-modes (each looks for a hotbar slot with the matching item/attribute):
 *   LUNGE    — Spear with Lunge attribute → extended lunge on hit
 *   REACH    — Spear with Reach attribute → longer attack range
 *   COOLDOWN — Mace/Spear with reduced cooldown modifier
 *   BREACH   — Mace with Breach enchantment (bypasses armor)
 *   DENSITY  — Mace with Density enchantment (fall damage bonus)
 *   SPEAR_TO_MACE — swap spear→mace for follow-up wind burst
 */
public final class AttributeSwap extends Module {

    public enum SwapMode { LUNGE, REACH, COOLDOWN, BREACH, DENSITY, SPEAR_TO_MACE }

    private final Setting<SwapMode>  mode         = addEnum("Mode", "Which attribute to swap for", SwapMode.LUNGE);
    private final Setting<Boolean>   smartMode    = addBool("Smart Mode", "Auto-detect best mode per situation", true);
    private final Setting<Boolean>   swapBack     = addBool("Swap Back", "Return to original slot after attack", true);
    private final Setting<Integer>   swapDelay    = addInt("Swap Delay", "Ticks before swapping back", 1, 0, 5);

    private int  savedSlot    = -1;
    private int  pendingSwap  = -1;
    private int  swapBackTick = 0;

    public AttributeSwap() {
        super("AttributeSwap", "Silent attribute-weapon swap on attack tick", Category.SPEAR);
    }

    @Override protected void registerListeners() {
        listen(AttackEvent.class, this::onAttack);
        listen(TickEvent.class, this::onTick);
    }

    private void onAttack(AttackEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        SwapMode target = smartMode.getValue() ? detectBestMode(e.target) : mode.getValue();
        int slot = findSlot(target);
        if (slot == -1 || slot == player.getInventory().getSelectedSlot()) return;

        savedSlot   = player.getInventory().getSelectedSlot();
        pendingSwap = swapDelay.getValue();

        silentSwitch(slot);
    }

    private void onTick(TickEvent e) {
        if (pendingSwap <= 0 || savedSlot == -1) return;
        if (--pendingSwap <= 0 && swapBack.getValue()) {
            silentSwitch(savedSlot);
            savedSlot   = -1;
            pendingSwap = -1;
        }
    }

    private SwapMode detectBestMode(Entity target) {
        if (!(target instanceof net.minecraft.world.entity.LivingEntity le)) return mode.getValue();
        // High armor → breach/density; low HP → lunge for kill; default reach
        float armor = le.getArmorValue();
        float hp    = le.getHealth();
        if (hp < 6f)   return SwapMode.LUNGE;
        if (armor > 15) return SwapMode.BREACH;
        return SwapMode.REACH;
    }

    private int findSlot(SwapMode m) {
        var player = Globals.player();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (matchesMode(stack, m)) return i;
        }
        return -1;
    }

    private boolean matchesMode(ItemStack stack, SwapMode m) {
        // MC 26.x uses component-based attributes; check for item type and relevant component.
        // Specific component keys depend on MC 26.3 data-driven attribute system.
        // Here we check item type as minimum; full component check requires the attribute API.
        return switch (m) {
            case LUNGE, REACH, COOLDOWN, SPEAR_TO_MACE ->
                // Spear item — in MC 26.x this is a new item type
                stack.getItem().getDescriptionId().contains("spear");
            case BREACH, DENSITY ->
                stack.getItem() == Items.MACE;
        };
    }

    private void silentSwitch(int slot) {
        var conn = Globals.mc.getConnection();
        if (conn != null) conn.send(new ServerboundSetCarriedItemPacket(slot));
        Globals.player().getInventory().setSelectedSlot(slot);
    }
}

