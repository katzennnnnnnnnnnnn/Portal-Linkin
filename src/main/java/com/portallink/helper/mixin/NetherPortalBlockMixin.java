package com.portallink.helper.mixin;

import com.portallink.helper.PortalLinker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {

    @Inject(method = "getPortalDestination", at = @At("HEAD"))
    private void portallink$begin(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<?> cir) {
        PortalLinker.begin(level, entity, pos);
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"))
    private void portallink$end(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<?> cir) {
        PortalLinker.end();
    }
}
