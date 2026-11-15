package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SafeAnchor / AnchorMacro — places and charges a Respawn Anchor on enemies,
 * or uses one defensively. Requires Overworld/End (Nether anchors explode naturally).
 *
 * Modes:
 *   PLACE  — place anchor on/near enemy then charge+use
 *   MACRO  — binds a key sequence to place, charge x4, explode
 *   DOUBLE — double anchor (place two adjacent for sequential explosions)
 */
public final class SafeAnchor extends Module {

    public enum AnchorMode { PLACE, MACRO, DOUBLE }

    private final Setting<AnchorMode> mode   = addEnum("Mode", "Anchor operation mode", AnchorMode.MACRO);
    private final Setting<Double>     range  = addDouble("Range", "Placement range", 4.5, 1.0, 6.0);
    private final Setting<Integer>    delay  = addInt("Delay", "ms between charges", 60, 0, 300);

    private long lastAction  = 0;
    private int  chargeCount = 0;

    public SafeAnchor() {
        super("SafeAnchor", "Respawn Anchor placement and explosion macro", Category.CRYSTAL);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;

        var player = Globals.player();

        switch (mode.getValue()) {
            case MACRO  -> runMacro(player);
            case PLACE  -> autoPlace(player);
            case DOUBLE -> doubleAnchor(player);
        }
    }

    private void runMacro(net.minecraft.client.player.LocalPlayer player) {
        Level world = Globals.world();
        if (chargeCount == 0) {
            int slot = findSlot(player, Items.RESPAWN_ANCHOR);
            if (slot == -1) return;
            player.getInventory().setSelectedSlot(slot);
            placeAt(player.blockPosition().below());
            chargeCount = 1;
        } else if (chargeCount < 5) {
            int slot = findSlot(player, Items.GLOWSTONE);
            if (slot == -1) { chargeCount = 0; return; }
            player.getInventory().setSelectedSlot(slot);
            placeAt(player.blockPosition().below());
            chargeCount++;
        } else {
            BlockPos pos = player.blockPosition().below();
            if (world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                placeAt(pos);
            }
            chargeCount = 0;
            setEnabled(false);
        }
        lastAction = System.currentTimeMillis();
    }

    private void autoPlace(net.minecraft.client.player.LocalPlayer player) {
        Level world = Globals.world();
        double r2   = range.getValue() * range.getValue();

        for (var entity : world.getEntities()) {
            if (!(entity instanceof net.minecraft.world.entity.player.Player p) || p == player) continue;
            if (player.distanceToSqr(p) > r2) continue;

            BlockPos target = p.blockPosition().below();
            if (!world.getBlockState(target).isAir()) target = target.above();

            int slot = findSlot(player, Items.RESPAWN_ANCHOR);
            if (slot == -1) return;
            player.getInventory().setSelectedSlot(slot);
            placeAt(target);
            lastAction = System.currentTimeMillis();
            return;
        }
    }

    private void doubleAnchor(net.minecraft.client.player.LocalPlayer player) {
        if (chargeCount == 0) {
            placeAt(player.blockPosition().below());
            placeAt(player.blockPosition().below().east());
            chargeCount = 1;
        } else {
            runMacro(player);
        }
    }

    private void placeAt(BlockPos pos) {
        var hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        Globals.mc.gameMode.useItemOn(Globals.player(), InteractionHand.MAIN_HAND, hitResult);
    }

    private int findSlot(net.minecraft.client.player.LocalPlayer p, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }
}

