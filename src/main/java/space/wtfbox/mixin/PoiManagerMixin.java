package space.wtfbox.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.wtfbox.FireSuppressorMod;


@Mixin(PoiManager.class)
public class PoiManagerMixin {
    @Inject(
        at=@At("HEAD"),
        method="remove(Lnet/minecraft/core/BlockPos;)V",   // cspell:disable-line
        cancellable=false
    )
    void on_poi_removed(BlockPos at, CallbackInfo parent) {
        PoiManager self = (PoiManager) (Object) this;
        FireSuppressorMod.on_poi_removed(self, at);
    }
}
