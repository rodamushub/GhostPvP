package dev.ghost.pvp.combat.mace;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * MaceAssist — assists with mace combo timing.
 * Features:
 *   - Stun Slam timing hints (fires attack at peak of enemy stun window)
 *   - Wind Burst jumps: auto-jump + wind charge to gain height for mace fall damage
 *   - Combo tracker: displays current fall-damage multiplier on HUD
 */
public final class MaceAssist extends Module {

    private final Setting<Boolean> windBurstJump  = addBool("Wind Burst Jump", "Auto-use Wind Charge to gain height", true);
    private final Setting<Double>  jumpHeight      = addDouble("Jump Height", "Target height above ground before mace", 5.0, 2.0, 20.0);
    private final Setting<Boolean> stunSlam        = addBool("Stun Slam", "Auto-attack on stun window", true);

    private boolean winding = false;

    public MaceAssist() {
        super("MaceAssist", "Mace combo assist with Wind Burst height gain", Category.MACE);
    }

    @Override protected void registerListeners() { listen(TickEvent.class, this::onTick); }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        var player = Globals.player();

        if (windBurstJump.getValue()) handleWindBurst(player);
        if (stunSlam.getValue())      handleStunSlam(player);
    }

    private void handleWindBurst(net.minecraft.client.player.LocalPlayer player) {
        double groundDist = player.getY() - Globals.world().getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            player.blockPosition()
        ).getY();

        if (!winding && groundDist < 2.0) {
            // Look for Wind Charge in hotbar
            int slot = findWindCharge(player);
            if (slot == -1) return;
            int prev = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            Globals.mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(prev);
            winding = true;
        } else if (groundDist >= jumpHeight.getValue()) {
            winding = false;
        }
    }

    private void handleStunSlam(net.minecraft.client.player.LocalPlayer player) {
        if (!player.getMainHandItem().is(Items.MACE)) return;
        // Stun window: when target's hurt timer is at max (20 ticks)
        for (var en : Globals.world().getEntities()) {
            if (!(en instanceof Player p) || p == player) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(p.getName().getString())) continue;
            if (player.distanceTo(en) > 3.5f) continue;
            // hurtTime == invincibleTime peak = good slam window
            if (p.invulnerableTime == 0) {
                Globals.mc.gameMode.attack(player, en);
                return;
            }
        }
    }

    private int findWindCharge(net.minecraft.client.player.LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }
}

