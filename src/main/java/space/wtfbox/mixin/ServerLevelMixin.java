package space.wtfbox.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.wtfbox.FireSuppressorMod;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(
        method = "canSpreadFireAround(Lnet/minecraft/core/BlockPos;)Z",  // cspell:disable-line
        at = @At("RETURN"),
        cancellable = true
    )
    public void can_spread_fire_around(BlockPos at, CallbackInfoReturnable<Boolean> parent) {
        // not enabled or no player nearby
        if ( !parent.getReturnValueZ() ) {
            return;
        }

        // cancel fire spread, if cauldron nearby
        ServerLevel self = (ServerLevel) (Object) this;
        if ( FireSuppressorMod.any_cauldron_nearby(self, at) ) {
            parent.setReturnValue(false);
            parent.cancel();
        }
    }
}
