package com.portallink.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Measures a lit Nether portal and reads the blocks in its four frame corners. */
final class PortalScanner {

    private static final int MAX_PORTAL_BLOCKS = 600; // largest vanilla portal is 21 x 21 = 441

    private PortalScanner() {
    }

    /** A measured portal: its lowest interior block ({@link #anchor()}) and its signature. */
    record Frame(Direction.Axis axis, int fixed, int minW, int minY, int width, int height, String signature) {
        BlockPos anchor() {
            return at(axis, fixed, minW, minY);
        }
    }

    static BlockPos at(Direction.Axis axis, int fixed, int w, int y) {
        return axis == Direction.Axis.X ? new BlockPos(w, y, fixed) : new BlockPos(fixed, y, w);
    }

    /** @return null if {@code start} is not a portal block or the portal is implausibly large */
    static Frame scan(BlockGetter level, BlockPos start) {
        BlockState first = level.getBlockState(start);
        if (!first.is(Blocks.NETHER_PORTAL)) return null;

        Direction.Axis axis = first.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction[] steps = {along, along.getOpposite(), Direction.UP, Direction.DOWN};

        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start.asLong());
        queue.add(start);

        int minW = Integer.MAX_VALUE;
        int maxW = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        while (!queue.isEmpty()) {
            if (seen.size() > MAX_PORTAL_BLOCKS) return null;
            BlockPos p = queue.poll();
            int w = axis == Direction.Axis.X ? p.getX() : p.getZ();
            minW = Math.min(minW, w);
            maxW = Math.max(maxW, w);
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            for (Direction d : steps) {
                BlockPos n = p.relative(d);
                if (seen.contains(n.asLong())) continue;
                BlockState ns = level.getBlockState(n);
                if (ns.is(Blocks.NETHER_PORTAL) && ns.getValue(BlockStateProperties.HORIZONTAL_AXIS) == axis) {
                    seen.add(n.asLong());
                    queue.add(n);
                }
            }
        }

        int fixed = axis == Direction.Axis.X ? start.getZ() : start.getX();
        int width = maxW - minW + 1;
        int height = maxY - minY + 1;

        // the four frame corners (diagonally outside the portal interior)
        int[][] corners = {
                {minW - 1, minY - 1}, {minW + width, minY - 1},
                {minW - 1, minY + height}, {minW + width, minY + height}
        };
        List<String> paths = new ArrayList<>();
        for (int[] c : corners) {
            BlockState state = level.getBlockState(at(axis, fixed, c[0], c[1]));
            paths.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
        }
        return new Frame(axis, fixed, minW, minY, width, height, PortalSignature.of(paths));
    }
}
