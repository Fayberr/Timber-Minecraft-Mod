package net.fayber.timber;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

/**
 * Destroys a set of blocks, either immediately or spread over several server
 * ticks (the datapack's "slow chop" mode). Also handles loot drops:
 * drop_loot=0 spawns items at the block position, drop_loot=1 gives them to
 * the breaking player's inventory (also during slow chop, as long as the
 * player is still online).
 */
public class SlowChopManager {

    private record PendingChop(ServerLevel level, UUID playerUuid, ItemStack tool, ArrayDeque<BlockPos> positions) {}

    private final TimberConfig config;
    private final List<PendingChop> pending = new ArrayList<>();
    private int ticks;

    public SlowChopManager(TimberConfig config) {
        this.config = config;
    }

    /** Queue a set of positions to be destroyed over the next several ticks. */
    public void enqueue(ServerLevel level, ServerPlayer player, ItemStack tool, Collection<BlockPos> positions) {
        UUID uuid = player != null ? player.getUUID() : null;
        pending.add(new PendingChop(level, uuid, tool.copy(), new ArrayDeque<>(positions)));
    }

    /** Called every server tick. Advances the slow-chop clock. */
    public void tick(ServerLevel level) {
        if (pending.isEmpty()) {
            return;
        }
        ticks++;
        if (ticks < config.timeBetweenChops) {
            return;
        }
        ticks = 0;

        int toDestroy = config.blocksPerChop;
        for (PendingChop chop : pending) {
            if (chop.level() != level) {
                continue;
            }
            ServerPlayer player = chop.playerUuid() != null
                    ? level.getServer().getPlayerList().getPlayer(chop.playerUuid())
                    : null;
            while (toDestroy > 0 && !chop.positions().isEmpty()) {
                destroyOne(level, player, chop.tool(), chop.positions().poll(), config.dropLoot);
                toDestroy--;
            }
        }
        pending.removeIf(c -> c.positions().isEmpty());
    }

    /** Immediate destruction (non slow-chop mode). */
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

        // Break particles + sound for everyone nearby.
        level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);

        // Replace with air.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
