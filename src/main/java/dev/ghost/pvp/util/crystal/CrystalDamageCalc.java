package dev.ghost.pvp.util.crystal;

import dev.ghost.pvp.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal damage calculation using vanilla explosion damage formula.
 *
 * Crystal explosion: base power 6.0 (End Crystal).
 * Damage = power * (1 - distance/power) * exposure * difficulty_factor
 *   exposure = ray-traced fraction of rays not blocked (0-1).
 *
 * This is an approximation; exact server exposure requires full world ray trace
 * which is expensive. We use a 7-ray AABB sample for perf.
 */
public final class CrystalDamageCalc {

    private static final float CRYSTAL_POWER = 6.0f;

    private CrystalDamageCalc() {}

    // ── Damage ─────────────────────────────────────────────────────────────────

    /**
     * Approximate explosion damage from a crystal placed at crystalPos to target entity.
     * Returns raw pre-armor damage (caller applies armor reduction separately for display;
     * for simple comparison raw is fine).
     */
    public static float calculateDamage(Vec3 explosionCenter, Entity target, Level world) {
        double dist = explosionCenter.distanceTo(target.getBoundingBox().getCenter());
        if (dist > CRYSTAL_POWER) return 0f;

        float exposure = calculateExposure(explosionCenter, target, world);
        float impact   = (float)((1.0 - dist / CRYSTAL_POWER) * exposure);
        float damage   = (impact * impact + impact) / 2f * 7f * (CRYSTAL_POWER * 2f) + 1f;

        // Blast protection reduces this further, but we can't trivially calculate
        // enchant levels server-side; use armor factor approximation
        return applyArmorReduction(damage, target);
    }

    private static float calculateExposure(Vec3 origin, Entity target, Level world) {
        var bb         = target.getBoundingBox();
        int hits       = 0;
        int total      = 0;
        double stepX   = 1.0 / (bb.maxX - bb.minX) * 2;
        double stepY   = 1.0 / (bb.maxY - bb.minY) * 2;
        double stepZ   = 1.0 / (bb.maxZ - bb.minZ) * 2;

        // Sample rays at 2-step AABB grid
        for (double x = 0; x <= 1.0; x += stepX) {
            for (double y = 0; y <= 1.0; y += stepY) {
                for (double z = 0; z <= 1.0; z += stepZ) {
                    total++;
                    Vec3 point = new Vec3(
                        bb.minX + (bb.maxX - bb.minX) * x,
                        bb.minY + (bb.maxY - bb.minY) * y,
                        bb.minZ + (bb.maxZ - bb.minZ) * z
                    );
                    var hitResult = world.clip(new net.minecraft.world.level.ClipContext(
                        origin, point,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        target
                    ));
                    if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) hits++;
                }
            }
        }
        return total == 0 ? 0f : (float) hits / total;
    }

    private static float applyArmorReduction(float damage, Entity entity) {
        if (!(entity instanceof Player player)) return damage;
        int armor       = player.getArmorValue();
        float toughness = player.getArmorToughness();
        // Vanilla armor reduction formula
        float reduction = Math.min(20f, Math.max(armor / 5f, armor - 4f * damage / (toughness + 8f)));
        return damage * (1f - reduction / 25f);
    }

    // ── Place Search ───────────────────────────────────────────────────────────

    /**
     * Finds best block position to place a crystal on to maximize damage to target
     * while keeping self-damage ≤ maxSelf and friend damage ≤ maxFriend.
     */
    public static CrystalPlace findBestPlace(
        LocalPlayer self, Entity target, Level world,
        double range, double maxSelf, double maxFriend,
        double minDmg, boolean lethalFirst, boolean facePlace, boolean wallPlace
    ) {
        BlockPos selfPos   = self.blockPosition();
        Vec3     selfEyePos= self.getEyePosition();

        CrystalPlace best      = null;
        float        bestScore = -Float.MAX_VALUE;

        int r = (int) Math.ceil(range);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = selfPos.offset(x, y, z);

                    if (!isValidPlacement(world, pos)) continue;
                    if (selfEyePos.distanceTo(Vec3.atCenterOf(pos)) > range) continue;

                    Vec3  crystalCenter = Vec3.atCenterOf(pos).add(0, 1, 0);
                    float targetDmg     = calculateDamage(crystalCenter, target, world);
                    float selfDmg       = calculateDamage(crystalCenter, self, world);

                    if (targetDmg < minDmg) continue;
                    if (selfDmg > maxSelf)  continue;

                    // Check friend damage
                    float friendDmg = maxFriendDmg(crystalCenter, world, maxFriend);
                    if (friendDmg > maxFriend) continue;

                    Direction dir = wallPlace
                        ? Direction.UP
                        : findPlaceDirection(world, pos, selfEyePos, facePlace);
                    if (dir == null) continue;

                    // Score: lethal target prioritized; then maximise targetDmg - selfDmg
                    boolean lethal = targetDmg >= getHealth(target);
                    float score = (lethalFirst && lethal ? 1000f : 0f) + targetDmg - selfDmg * 0.5f;

                    if (score > bestScore) {
                        bestScore = score;
                        best      = new CrystalPlace(pos, dir, targetDmg, selfDmg);
                    }
                }
            }
        }
        return best;
    }

    private static boolean isValidPlacement(Level world, BlockPos pos) {
        var base = world.getBlockState(pos);
        if (base.getBlock() != Blocks.OBSIDIAN && base.getBlock() != Blocks.BEDROCK) return false;
        // Crystal occupies pos+1 and pos+2
        return world.getBlockState(pos.above()).isAir() && world.getBlockState(pos.above(2)).isAir()
            && world.getEntitiesOfClass(EndCrystal.class,
                new net.minecraft.world.phys.AABB(pos.above())).isEmpty();
    }

    private static Direction findPlaceDirection(Level world, BlockPos pos, Vec3 eyes, boolean facePlace) {
        // Try UP first (normal placement)
        if (canSeeBlock(world, eyes, Vec3.atCenterOf(pos.above()))) return Direction.UP;
        if (!facePlace) return null;
        // Try side faces
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (canSeeBlock(world, eyes, Vec3.atCenterOf(pos.relative(d)))) return d;
        }
        return null;
    }

    private static boolean canSeeBlock(Level world, Vec3 from, Vec3 to) {
        var result = world.clip(new net.minecraft.world.level.ClipContext(
            from, to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            null
        ));
        return result.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private static float maxFriendDmg(Vec3 origin, Level world, double maxFriend) {
        var friends = dev.ghost.pvp.GhostClient.get().getFriendManager();
        float max = 0;
        for (Entity e : world.getEntities()) {
            if (!(e instanceof Player p)) continue;
            if (!friends.isFriend(p.getName().getString())) continue;
            max = Math.max(max, calculateDamage(origin, e, world));
        }
        return max;
    }

    // ── Explode Search ─────────────────────────────────────────────────────────

    /**
     * Finds the placed EndCrystal that would deal the most damage to target
     * within explodeRange, while respecting self/friend damage limits.
     */
    public static EndCrystal findBestExplode(
        LocalPlayer self, Entity target, Level world,
        double range, double minDmg, double maxSelf, double maxFriend,
        boolean antiSuicide
    ) {
        EndCrystal best      = null;
        float      bestScore = -Float.MAX_VALUE;

        for (Entity e : world.getEntities()) {
            if (!(e instanceof EndCrystal crystal)) continue;
            double dist = self.distanceTo(crystal);
            if (dist > range) continue;

            Vec3  c         = crystal.position();
            float targetDmg = calculateDamage(c, target, world);
            float selfDmg   = calculateDamage(c, self, world);

            if (targetDmg < minDmg)  continue;
            if (selfDmg > maxSelf)   continue;
            if (antiSuicide && selfDmg >= self.getHealth()) continue;

            float friendDmg = maxFriendDmg(c, world, (float) maxFriend);
            if (friendDmg > maxFriend) continue;

            float score = targetDmg - selfDmg * 0.5f;
            if (score > bestScore) {
                bestScore = score;
                best      = crystal;
            }
        }
        return best;
    }

    // ── Packet helpers ─────────────────────────────────────────────────────────

    /** Silent hotbar switch — sends packet without updating client-side selected slot display. */
    public static void silentSwitch(int slot) {
        Globals.mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }

    /**
     * Place block interaction using the interaction manager.
     * Uses current held item (caller must switch first).
     */
    public static void placeBlock(BlockPos pos, Direction face) {
        var mc     = Globals.mc;
        var player = mc.player;
        if (player == null) return;

        var hitResult = new net.minecraft.world.phys.BlockHitResult(
            Vec3.atCenterOf(pos), face, pos, false
        );
        mc.gameMode.useItemOn(player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private static float getHealth(Entity e) {
        if (e instanceof net.minecraft.world.entity.LivingEntity le) return le.getHealth();
        return Float.MAX_VALUE;
    }
}

