package com.portallink.helper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public class PortalLinkMod implements ModInitializer {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final int SCAN_RADIUS = 16;

    private int ticks;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                PortalLinker.REGISTRY.load(server.getWorldPath(LevelResource.ROOT).resolve("portal-link-helper.txt")));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PortalLinker.REGISTRY.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PortalLinker.REGISTRY.clear());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks < SCAN_INTERVAL_TICKS) return;
            ticks = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() instanceof ServerLevel level) {
                    PortalLinker.scanAround(level, player.blockPosition(), SCAN_RADIUS);
                }
            }
        });
    }
}
