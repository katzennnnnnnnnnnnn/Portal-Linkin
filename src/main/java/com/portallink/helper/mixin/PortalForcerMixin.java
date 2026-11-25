package com.portallink.helper.mixin;

import com.portallink.helper.PortalLinker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {

    @Inject(method = "findClosestPortal", at = @At("HEAD"), cancellable = true)
    private void portallink$find(CallbackInfoReturnable<Optional<BlockPos>> cir) {
        Optional<BlockPos> match = PortalLinker.find();
        if (match.isPresent()) {
            cir.setReturnValue(match);
        }
    }
}
