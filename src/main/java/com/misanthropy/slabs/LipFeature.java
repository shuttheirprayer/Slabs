package com.misanthropy.slabs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.Map;

public class LipFeature extends Feature<NoneFeatureConfiguration> {
    static final double BROAD = 1.0 / 96.0;
    static final double FINE = 1.0 / 10.0;
    static final double SPREAD = 0.5;
    static final double THRESHOLD = -0.05;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private record Patches(long seed, PerlinSimplexNoise broad, PerlinSimplexNoise fine) {
        double at(int x, int z) {
            return SPREAD * broad.getValue(x * BROAD, z * BROAD, false) + fine.getValue(x * FINE, z * FINE, false);
        }
    }

    private static volatile Patches patches;

    public LipFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static Patches patches(long seed) {
        Patches p = patches;
        if (p == null || p.seed != seed) {
            p = new Patches(seed, new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x51AB5L)), List.of(0)),
                    new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x1195L)), List.of(0)));
            patches = p;
        }
        return p;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!SlabsConfig.ENABLED.get()) {
            return false;
        }
        WorldGenLevel level = context.level();
        Map<Block, Block> terrain = Slabs.terrain();
        Patches noise = patches(level.getSeed());
        Regions region = Regions.of(level.getSeed(), SlabsConfig.COVERAGE.get());
        int size = SlabsConfig.SIZE.get();
        ChunkPos chunk = new ChunkPos(context.origin());
        BlockPos.MutableBlockPos spot = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int x = chunk.getMinBlockX() + dx;
                int z = chunk.getMinBlockZ() + dz;
                if (!region.in(x, z, size) || noise.at(x, z) < THRESHOLD) {
                    continue;
                }
                spot.set(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
                BlockState ground = level.getBlockState(spot);
                Block slab = terrain.get(ground.getBlock());
                if (slab == null) {
                    continue;
                }
                spot.move(Direction.UP);
                BlockState here = level.getBlockState(spot);
                boolean open = here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty() && !here.is(Blocks.SNOW);
                if (!open || !level.getBlockState(probe.setWithOffset(spot, Direction.UP)).isAir() || !lip(level, terrain, spot, probe)) {
                    continue;
                }
                level.setBlock(spot, slab.defaultBlockState(), 2);
                if (ground.getBlock() instanceof SnowyDirtBlock) {
                    level.setBlock(spot.move(Direction.DOWN), Blocks.DIRT.defaultBlockState(), 2);
                }
                placed = true;
            }
        }
        return placed;
    }

    private static boolean lip(WorldGenLevel level, Map<Block, Block> terrain, BlockPos spot, BlockPos.MutableBlockPos probe) {
        for (Direction side : SIDES) {
            if (!terrain.containsKey(level.getBlockState(probe.setWithOffset(spot, side)).getBlock())) {
                continue;
            }
            BlockState above = level.getBlockState(probe.move(Direction.UP));
            if (!above.isAir() && !(above.getBlock() instanceof SlabBlock) && !(above.canBeReplaced() && above.getFluidState().isEmpty())) {
                continue;
            }
            if (!terrain.containsKey(level.getBlockState(probe.setWithOffset(spot, side.getOpposite())).getBlock())) {
                return true;
            }
        }
        return false;
    }
}
