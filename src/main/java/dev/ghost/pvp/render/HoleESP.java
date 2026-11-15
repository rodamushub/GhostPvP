package dev.ghost.pvp.render;

import dev.ghost.pvp.config.Setting;
import dev.ghost.pvp.event.events.RenderWorldEvent;
import dev.ghost.pvp.module.Category;
import dev.ghost.pvp.module.Module;
import dev.ghost.pvp.util.Globals;
import dev.ghost.pvp.util.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * HoleESP — scans within render radius for 1×1 holes made of obsidian/bedrock.
 * Safe holes (all 6 sides obsidian) = green.
 * Semi-safe (mixed obsidian/bedrock) = yellow.
 */
public final class HoleESP extends Module {

    private final Setting<Integer> scanRadius = addInt("Radius", "Scan radius in blocks", 10, 1, 24);
    private final Setting<Boolean> onlySafe   = addBool("Only Safe", "Only show fully-obsidian holes", false);

    public HoleESP() { super("HoleESP", "Highlights safe crystal-safe holes", Category.RENDER); }

    @Override protected void registerListeners() { listen(RenderWorldEvent.class, this::onRender); }

    private void onRender(RenderWorldEvent e) {
        if (!Globals.inGame()) return;
        var  world  = Globals.world();
        var  player = Globals.player();
        var  cam    = Globals.mc.gameRenderer.getCamera().getPosition();
        int  r      = scanRadius.getValue();
        BlockPos origin = player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -3; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);

                    // Hole: air at pos and pos+1, surrounded by solid blocks
                    if (!world.getBlockState(pos).isAir()) continue;
                    if (!world.getBlockState(pos.above()).isAir()) continue;

                    boolean safe = isSafeBlock(world, pos.below())
                        && isSafeBlock(world, pos.north())
                        && isSafeBlock(world, pos.south())
                        && isSafeBlock(world, pos.east())
                        && isSafeBlock(world, pos.west());

                    boolean semiSafe = world.getBlockState(pos.below()).isSolid()
                        && world.getBlockState(pos.north()).isSolid()
                        && world.getBlockState(pos.south()).isSolid()
                        && world.getBlockState(pos.east()).isSolid()
                        && world.getBlockState(pos.west()).isSolid();

                    if (!safe && onlySafe.getValue()) continue;
                    if (!safe && !semiSafe) continue;

                    int r2 = safe ? 0 : 255;
                    int g2 = safe ? 255 : 165;
                    int b2 = 0;

                    AABB box = new AABB(pos).move(-cam.x, -cam.y, -cam.z).inflate(-0.01);
                    RenderUtil.drawBox(e.poseStack, box, r2, g2, b2, 200, 1.5f);
                    RenderUtil.drawFilledBox(e.poseStack, box, r2, g2, b2, 20);
                }
            }
        }
    }

    private boolean isSafeBlock(net.minecraft.world.level.Level world, BlockPos pos) {
        var b = world.getBlockState(pos).getBlock();
        return b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.CRYING_OBSIDIAN
            || b == Blocks.REINFORCED_DEEPSLATE;
    }
}

