package net.fayber.timber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

// destroys a set of blocks, either immediately or spread over several server
// ticks (the datapack's "slow chop" mode). also handles loot drops:
// drop_loot=0 spawns items at the block position, drop_loot=1 gives them to
// the breaking player's inventory (also during slow chop, as long as the
// player is still online).
public class SlowChopManager {

    private record PendingChop(ServerLevel level, UUID playerUuid, ItemStack tool, ArrayDeque<BlockPos> positions) {}

    private final TimberConfig config;
    private final List<PendingChop> pending = new ArrayList<>();
    private int ticks;

    public SlowChopManager(TimberConfig config) {
        this.config = config;
    }

    // queue a set of positions to be destroyed over the next several ticks.
    public void enqueue(ServerLevel level, ServerPlayer player, ItemStack tool, Collection<BlockPos> positions) {
        UUID uuid = player != null ? player.getUUID() : null;
        pending.add(new PendingChop(level, uuid, tool.copy(), new ArrayDeque<>(positions)));
    }

    // called once per server tick. advances the slow-chop clock once, then
    // advances every pending chop (across all dimensions) by its own budget.
    public void tick(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }
        ticks++;
        if (ticks < config.timeBetweenChops) {
            return;
        }
        ticks = 0;

        for (PendingChop chop : pending) {
            ServerLevel level = chop.level();
            ServerPlayer player = chop.playerUuid() != null
                    ? server.getPlayerList().getPlayer(chop.playerUuid())
                    : null;
            // Each pending tree gets its own blocksPerChop budget so multiple
            // trees chop simultaneously instead of one draining the budget.
            int toDestroy = config.blocksPerChop;
            while (toDestroy > 0 && !chop.positions().isEmpty()) {
                destroyOne(level, player, chop.tool(), chop.positions().poll(), config.dropLoot);
                toDestroy--;
            }
        }
        pending.removeIf(c -> c.positions().isEmpty());
    }

    // immediate destruction (non slow-chop mode).
    public void destroyNow(ServerLevel level, ServerPlayer player, ItemStack tool, Collection<BlockPos> positions) {
        for (BlockPos pos : positions) {
            destroyOne(level, player, tool, pos, config.dropLoot);
        }
    }

    private void destroyOne(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, boolean giveToInventory) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        // Drops. When giveToInventory is true and the player is online, items go
        // straight into their inventory; overflow falls on the ground.
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, tool);
        for (ItemStack drop : drops) {
            boolean given = giveToInventory && player != null && player.getInventory().add(drop);
            if (!given) {
                Block.popResource(level, pos, drop);
            }
        }

        // The vanilla break event already includes the block sound.
        if (config.fellingEffects == FellingEffects.UNSUPPRESSED) {
            level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        } else if (config.fellingEffects == FellingEffects.MINIMIZED) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    config.minimizedParticles, 0.25, 0.25, 0.25, 0.05);
        }

        // Replace with air.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
