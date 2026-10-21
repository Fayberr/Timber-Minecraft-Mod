package net.fayber.timber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

public final class BreakEffects {
    private BreakEffects() { }

    public static void emit(ServerLevel level, BlockPos pos, BlockState state, FellingEffects mode, int particles) {
        if (mode == FellingEffects.UNSUPPRESSED) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        } else if (mode == FellingEffects.MINIMIZED) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    particles, 0.25, 0.25, 0.25, 0.05);
        }
    }
}
