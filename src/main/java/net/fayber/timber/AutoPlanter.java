package net.fayber.timber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// replants saplings that end up on the ground after a chop, every 40 ticks.
// mirrors the datapack's autoplant behaviour.
public class AutoPlanter {

    private static final int INTERVAL = 40;

    // 26.2 removed BlockTags.SAPLINGS; the vanilla #minecraft:saplings tag still exists.
    private static final TagKey<Block> SAPLINGS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "saplings"));

    private final Map<ServerLevel, List<BlockPos>> pending = new HashMap<>();
    private int tick;

    // remember the chop site; saplings near it will be planted on the next sweep.
    public void schedule(ServerLevel level, BlockPos pos) {
        pending.computeIfAbsent(level, k -> new ArrayList<>()).add(pos.immutable());
    }

    // called once per server tick.
    public void tick(MinecraftServer server) {
        tick++;
        if (tick < INTERVAL) {
            return;
        }
        tick = 0;

        for (ServerLevel level : server.getAllLevels()) {
            List<BlockPos> positions = pending.remove(level);
            if (positions == null || positions.isEmpty()) {
                continue;
            }
            for (BlockPos origin : positions) {
                scanAndPlant(level, origin);
            }
        }
    }

    private void scanAndPlant(ServerLevel level, BlockPos origin) {
        AABB box = new AABB(origin).inflate(16.0, 5.0, 16.0);
        List<ItemEntity> items = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), box, e -> true);
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            BlockState saplingState = blockItem.getBlock().defaultBlockState();
            if (!saplingState.getBlock().builtInRegistryHolder().is(SAPLINGS)) {
                continue;
            }

            BlockPos pos = item.blockPosition();
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!isGrowable(level.getBlockState(pos.below()))) {
                continue;
            }

            // Drop the sapling in as a falling block so it lands and becomes the block.
            FallingBlockEntity.fall(level, pos, saplingState);

            stack.shrink(1);
            if (stack.isEmpty()) {
                item.discard();
            }
        }
    }

    private boolean isGrowable(BlockState state) {
        return state.getBlock().builtInRegistryHolder().is(BlockTags.DIRT)
                || state.getBlock() == Blocks.GRASS_BLOCK
                || state.getBlock() == Blocks.MOSS_BLOCK
                || state.getBlock().builtInRegistryHolder().is(BlockTags.NYLIUM);
    }
}
