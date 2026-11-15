package dev.ghost.pvp.util.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record CrystalPlace(BlockPos pos, Direction direction, float targetDamage, float selfDamage) {}

