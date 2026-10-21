package net.fayber.timber.mixin;

import net.fayber.timber.BreakEffects;
import net.fayber.timber.FellingEffects;
import net.fayber.timber.FellingEffectsScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD"), cancellable = true)
    private void timber$vineEffects(Entity source, int event, BlockPos pos, int data, CallbackInfo ci) {
        if (event != LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK
                && event != LevelEvent.PARTICLES_DESTROY_BLOCK) return;
        ServerLevel level = (ServerLevel) (Object) this;
        var settings = FellingEffectsScope.current(level);
        if (settings == null || settings.mode() == FellingEffects.UNSUPPRESSED) return;
        var state = Block.stateById(data);
        if (!state.is(Blocks.VINE)) return;
        BreakEffects.emit(level, pos, state, settings.mode(), settings.particles());
        ci.cancel();
    }
}
