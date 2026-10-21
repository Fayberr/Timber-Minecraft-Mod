package net.fayber.timber.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fayber.timber.FellingEffectsScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow protected ServerLevel level;

    @WrapMethod(method = "destroyBlock")
    private boolean timber$scopeOriginBreak(BlockPos pos, Operation<Boolean> original) {
        // Starts inactive. TreeFeller activates it only after accepting a valid tree,
        // and it covers the original mined block's vine updates after Fabric's callback.
        try (var ignored = FellingEffectsScope.open(this.level)) {
            return original.call(pos);
        }
    }
}
