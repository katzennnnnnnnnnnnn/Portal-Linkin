package com.portallink.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The logic: remembers portals, and decides which portal a traveller arrives at. */
public final class PortalLinker {

    static final PortalRegistry REGISTRY = new PortalRegistry();

    private record Context(MinecraftServer server, boolean sourceNether, String signature, BlockPos entry,
                           Entity entity) {
    }

    private static Context context;

    private PortalLinker() {
    }

    // ---------------------------------------------------------------- travelling

    /** Called when a portal travel destination is being worked out (start). */
    public static void begin(ServerLevel level, Entity entity, BlockPos pos) {
        context = null;
        try {
            PortalScanner.Frame frame = PortalScanner.scan(level, pos);
            if (frame == null) {
                for (Direction d : Direction.values()) {
                    frame = PortalScanner.scan(level, pos.relative(d));
                    if (frame != null) break;
                }
            }
            if (frame == null || frame.signature().isEmpty()) return;
            boolean nether = level.dimension() == Level.NETHER;
            BlockPos a = frame.anchor();
            REGISTRY.add(new PortalRegistry.Entry(nether, a.getX(), a.getY(), a.getZ(), frame.signature()));
            context = new Context(level.getServer(), nether, frame.signature(), pos, entity);
        } catch (RuntimeException e) {
            context = null;
        }
    }

    /** Called when the destination has been worked out (end). */
    public static void end() {
        context = null;
    }

    /**
     * Called from the portal search of the destination dimension.
     * @return the matching portal in the destination dimension, or empty to let vanilla decide
     */
    public static Optional<BlockPos> find() {
        Context c = context;
        if (c == null) return Optional.empty();
        try {
            ServerLevel dest = c.server().getLevel(c.sourceNether() ? Level.OVERWORLD : Level.NETHER);
            if (dest == null) return Optional.empty();
            boolean destNether = !c.sourceNether();

            double scale = c.sourceNether() ? 8.0 : 1.0 / 8.0;
            double tx = c.entry().getX() * scale;
            double tz = c.entry().getZ() * scale;

            List<PortalRegistry.Entry> list = REGISTRY.candidates(destNether, c.signature());
            PortalRegistry.Entry best = null;
            PortalScanner.Frame bestFrame = null;
            double bestDist = Double.MAX_VALUE;
            for (PortalRegistry.Entry e : list) {
                BlockPos p = new BlockPos(e.x(), e.y(), e.z());
                PortalScanner.Frame f = dest.getBlockState(p).is(Blocks.NETHER_PORTAL)
                        ? PortalScanner.scan(dest, p) : null;
                if (f == null) {
                    REGISTRY.remove(e);                    // portal is gone
                    continue;
                }
                if (!f.signature().equals(e.signature())) {
                    BlockPos a = f.anchor();               // corners were changed
                    REGISTRY.remove(e);
                    if (!f.signature().isEmpty()) {
                        REGISTRY.add(new PortalRegistry.Entry(destNether, a.getX(), a.getY(), a.getZ(),
                                f.signature()));
                    }
                    continue;
                }
                double dx = e.x() - tx;
                double dz = e.z() - tz;
                double dist = dx * dx + dz * dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                    bestFrame = f;
                }
            }
            if (best == null) return Optional.empty();

            if (c.entity() instanceof ServerPlayer player) {
                player.sendSystemMessage(
                        Component.literal("Portal verbunden: " + PortalSignature.pretty(c.signature())), true);
            }
            return Optional.of(bestFrame.anchor());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------- scanning around players

    /** Registers all signed portals within a small area around {@code center}. */
    public static void scanAround(ServerLevel level, BlockPos center, int radius) {
        boolean nether = level.dimension() == Level.NETHER;
        if (!nether && level.dimension() != Level.OVERWORLD) return;

        List<PortalScanner.Frame> done = new ArrayList<>();
        int dy = Math.min(radius, 12);
        int minY = Math.max(level.getMinY(), center.getY() - dy);
        int maxY = Math.min(level.getMaxY(), center.getY() + dy);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                m.set(x, 0, z);
                if (!level.hasChunkAt(m)) continue;
                for (int y = minY; y <= maxY; y++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).is(Blocks.NETHER_PORTAL)) continue;
                    if (insideAny(done, x, y, z)) continue;
                    PortalScanner.Frame f = PortalScanner.scan(level, m.immutable());
                    if (f == null) continue;
                    done.add(f);
                    BlockPos a = f.anchor();
                    PortalRegistry.Entry e = new PortalRegistry.Entry(nether, a.getX(), a.getY(), a.getZ(),
                            f.signature());
                    if (f.signature().isEmpty()) {
                        REGISTRY.remove(e);
                    } else {
                        REGISTRY.add(e);
                    }
                }
            }
        }
    }

    private static boolean insideAny(List<PortalScanner.Frame> frames, int x, int y, int z) {
        for (PortalScanner.Frame f : frames) {
            int w = f.axis() == Direction.Axis.X ? x : z;
            int fixed = f.axis() == Direction.Axis.X ? z : x;
            if (fixed == f.fixed() && w >= f.minW() && w < f.minW() + f.width()
                    && y >= f.minY() && y < f.minY() + f.height()) {
                return true;
            }
        }
        return false;
    }
}
