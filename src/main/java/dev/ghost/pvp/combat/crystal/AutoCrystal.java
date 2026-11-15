package dev.ghost.pvp.combat.crystal;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.TickEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.crystal.CrystalDamageCalc;
import dev.ghost.pvp.util.crystal.CrystalPlace;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.*;

/**
 * AutoCrystal
 *
 * Place logic: finds best obsidian/bedrock pos → places crystal.
 * Explode logic: finds closest placed crystal hitting target ≥ minDmg → attacks.
 * Async damage calc on a dedicated thread to keep main thread smooth.
 * All delays randomized around a configurable base ± jitter.
 */
public final class AutoCrystal extends Module {

    // ── Settings ───────────────────────────────────────────────────────────────
    private final Setting<Double>  placeRange      = addDouble("Place Range",    "Max block place dist", 4.5, 1.0, 6.0);
    private final Setting<Double>  explodeRange    = addDouble("Explode Range",  "Max crystal explode dist", 5.0, 1.0, 7.0);
    private final Setting<Double>  minDmg          = addDouble("Min Damage",     "Min damage to explode", 6.0, 0.0, 36.0);
    private final Setting<Double>  maxSelf         = addDouble("Max Self",       "Max self-damage to allow", 8.0, 0.0, 36.0);
    private final Setting<Double>  maxFriend       = addDouble("Max Friend",     "Max friend damage to allow", 4.0, 0.0, 36.0);
    private final Setting<Boolean> lethalFirst     = addBool("Lethal First",     "Prioritize lethal hits", true);
    private final Setting<Boolean> antiSuicide     = addBool("Anti Suicide",     "Never explode if self would die", true);
    private final Setting<Boolean> facePlace       = addBool("Face Place",       "Place on face/side of block", true);
    private final Setting<Boolean> wallPlace       = addBool("Wall Place",       "Place through walls (via packet)", true);
    private final Setting<Boolean> multiPlace      = addBool("Multi Place",      "Place multiple crystals per tick", false);
    private final Setting<Integer> placeDelayBase  = addInt("Place Delay",      "Base ms between places", 50, 0, 500);
    private final Setting<Integer> placeJitter     = addInt("Place Jitter",     "Random ± ms on place delay", 20, 0, 100);
    private final Setting<Integer> explodeDelayBase= addInt("Explode Delay",    "Base ms between explodes", 50, 0, 500);
    private final Setting<Integer> explodeJitter   = addInt("Explode Jitter",   "Random ± ms on explode delay", 20, 0, 100);
    private final Setting<Boolean> pauseOnEat      = addBool("Pause Eating",    "Pause while eating/drinking", true);
    private final Setting<Boolean> pauseOnInv      = addBool("Pause Inventory", "Pause while inventory open", true);
    private final Setting<Boolean> silentSwitch    = addBool("Silent Switch",   "Switch to crystal silently via packet", true);
    private final Setting<Double>  targetRange     = addDouble("Target Range",  "Max enemy distance to target", 10.0, 1.0, 20.0);

    // ── State ──────────────────────────────────────────────────────────────────
    private final ExecutorService  calcThread      = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ghost-crystal-calc");
        t.setDaemon(true);
        return t;
    });

    private volatile CrystalPlace bestPlace    = null;
    private volatile EndCrystal   bestExplode  = null;

    private long lastPlaceTime   = 0;
    private long lastExplodeTime = 0;
    private long nextPlaceDelay  = 0;
    private long nextExplodeDelay= 0;

    private final Random rng = new Random();

    public AutoCrystal() {
        super("AutoCrystal", "Automatic End Crystal placement and detonation", Category.CRYSTAL);
    }

    @Override
    protected void registerListeners() {
        listen(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent e) {
        if (!Globals.inGame()) return;
        LocalPlayer player = Globals.player();
        Level world        = Globals.world();

        if (shouldPause(player)) return;

        // Find nearest valid target
        Entity target = findTarget(player, world);
        if (target == null) return;

        long now = System.currentTimeMillis();

        // Async calc: submit placement + explosion search to bg thread
        calcThread.submit(() -> {
            bestPlace   = CrystalDamageCalc.findBestPlace(player, target, world,
                placeRange.getValue(), maxSelf.getValue(), maxFriend.getValue(),
                minDmg.getValue(), lethalFirst.getValue(), facePlace.getValue(),
                wallPlace.getValue());
            bestExplode = CrystalDamageCalc.findBestExplode(player, target, world,
                explodeRange.getValue(), minDmg.getValue(), maxSelf.getValue(),
                maxFriend.getValue(), antiSuicide.getValue());
        });

        // Explode — runs on game thread using last calc result
        if (bestExplode != null && now - lastExplodeTime >= nextExplodeDelay) {
            attackCrystal(player, bestExplode);
            lastExplodeTime  = now;
            nextExplodeDelay = randomDelay(explodeDelayBase.getValue(), explodeJitter.getValue());
        }

        // Place
        if (bestPlace != null && now - lastPlaceTime >= nextPlaceDelay) {
            placeCrystal(player, bestPlace);
            lastPlaceTime  = now;
            nextPlaceDelay = randomDelay(placeDelayBase.getValue(), placeJitter.getValue());
        }
    }

    private void attackCrystal(LocalPlayer player, EndCrystal crystal) {
        Globals.mc.gameMode.attack(player, crystal);
    }

    private void placeCrystal(LocalPlayer player, CrystalPlace place) {
        int prev = player.getInventory().getSelectedSlot();
        int crystalSlot = findCrystalSlot(player);
        if (crystalSlot == -1) return;

        if (silentSwitch.getValue()) {
            CrystalDamageCalc.silentSwitch(crystalSlot);
        } else {
            player.getInventory().setSelectedSlot(crystalSlot);
        }

        CrystalDamageCalc.placeBlock(place.pos(), place.direction());

        if (silentSwitch.getValue()) {
            CrystalDamageCalc.silentSwitch(prev);
        } else {
            player.getInventory().setSelectedSlot(prev);
        }
    }

    private Entity findTarget(LocalPlayer self, Level world) {
        double rangeSquared = targetRange.getValue() * targetRange.getValue();
        Entity best         = null;
        double bestDist     = Double.MAX_VALUE;

        for (Entity e : world.getEntities()) {
            if (e == self || !(e instanceof net.minecraft.world.entity.player.Player)) continue;
            if (dev.ghost.pvp.GhostClient.get().getFriendManager().isFriend(e.getName().getString())) continue;
            double d = self.distanceToSqr(e);
            if (d < rangeSquared && d < bestDist) {
                best     = e;
                bestDist = d;
            }
        }
        return best;
    }

    private int findCrystalSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    private boolean shouldPause(LocalPlayer player) {
        if (pauseOnEat.getValue() && player.isUsingItem()) return true;
        if (pauseOnInv.getValue() && Globals.mc.getOverlay() != null) return true;
        return false;
    }

    private long randomDelay(int base, int jitter) {
        return base + (jitter > 0 ? (long)(rng.nextInt(jitter * 2 + 1) - jitter) : 0);
    }

    @Override
    protected void onDisable() {
        bestPlace   = null;
        bestExplode = null;
    }
}

