package net.fayber.timber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The core Timber logic: detects a broken log/stem and fells the whole tree
 * or giant fungus connected to it, honouring every datapack config option.
 */
public class TreeFeller {

    // 6 orthogonal directions.
    private static final BlockPos[] ORTHO = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };
    // 14-position flood shape for leaves/caps: 6 ortho + 8 diagonals at y-1.
    private static final BlockPos[] FLOOD_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, -1, 0), new BlockPos(-1, -1, 0),
            new BlockPos(0, -1, 1), new BlockPos(0, -1, -1),
            new BlockPos(1, -1, 1), new BlockPos(1, -1, -1),
            new BlockPos(-1, -1, 1), new BlockPos(-1, -1, -1)
    };

    private final TimberConfig config;
    private final SlowChopManager slowChopManager;
    private final AutoPlanter autoPlanter;
    private final Set<UUID> disabledPlayers = new HashSet<>();

    private final BlockPos[] logOffsets = buildLogOffsets(false);
    private final BlockPos[] fungusOffsets = buildLogOffsets(true);
    private final BlockPos[] downOffsets = buildDownOffsets();

    public TreeFeller(TimberConfig config, SlowChopManager slowChopManager, AutoPlanter autoPlanter) {
        this.config = config;
        this.slowChopManager = slowChopManager;
        this.autoPlanter = autoPlanter;
    }

    public boolean isDisabled(UUID uuid) {
        return disabledPlayers.contains(uuid);
    }

    /** Toggles the player's Timber state. Returns true if now disabled. */
    public boolean toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
            return false;
        }
        disabledPlayers.add(uuid);
        return true;
    }

    /** Entry point, wired to PlayerBlockBreakEvents.BEFORE. Returns true to cancel the vanilla break. */
    public boolean onBlockBreakBefore(Level level, Player player, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (disabledPlayers.contains(serverPlayer.getUUID())) {
            return false;
        }

        ItemStack stack = serverPlayer.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.is(h -> h.is(ItemTags.AXES))) {
            return false;
        }
        if (!isAxeEnabled(stack)) {
            return false;
        }

        boolean sneaking = serverPlayer.isShiftKeyDown();
        boolean allowed = (config.standing && !sneaking) || (config.sneaking && sneaking);
        if (!allowed) {
            return false;
        }

        if (config.chopTrees && isLogBlock(state)) {
            return chop(serverLevel, serverPlayer, pos, stack, false);
        }
        if (config.chopFungi && isStemBlock(state)) {
            return chop(serverLevel, serverPlayer, pos, stack, true);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Block classification
    // ------------------------------------------------------------------

    private boolean isLogBlock(BlockState state) {
        return state.getBlock().builtInRegistryHolder().is(BlockTags.LOGS);
    }

    private boolean isStemBlock(BlockState state) {
        return isLogBlock(state) || state.getBlock() == Blocks.MUSHROOM_STEM;
    }

    private boolean isLeafBlock(BlockState state) {
        if (!state.getBlock().builtInRegistryHolder().is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.PERSISTENT)) {
            boolean persistentFlag = state.getValue(BlockStateProperties.PERSISTENT);
            if (persistentFlag && !config.persistent) {
                return false;
            }
        }
        return true;
    }

    private boolean isCapBlock(BlockState state) {
        Block block = state.getBlock();
        return block.builtInRegistryHolder().is(BlockTags.WART_BLOCKS)
                || block == Blocks.SHROOMLIGHT
                || block == Blocks.BROWN_MUSHROOM_BLOCK
                || block == Blocks.RED_MUSHROOM_BLOCK;
    }

    private boolean isAxeEnabled(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.WOODEN_AXE) {
            return config.woodenAxe;
        }
        if (item == Items.STONE_AXE) {
            return config.stoneAxe;
        }
        if (item == Items.COPPER_AXE) {
            return config.copperAxe;
        }
        if (item == Items.IRON_AXE) {
            return config.ironAxe;
        }
        if (item == Items.GOLDEN_AXE) {
            return config.goldenAxe;
        }
        if (item == Items.DIAMOND_AXE) {
            return config.diamondAxe;
        }
        if (item == Items.NETHERITE_AXE) {
            return config.netheriteAxe;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Main chop
    // ------------------------------------------------------------------

    private boolean chop(ServerLevel level, ServerPlayer player, BlockPos origin, ItemStack stack, boolean fungus) {
        int maxTreeSize = config.maxTreeSize;
        Set<BlockPos> logs = new LinkedHashSet<>();
        Set<BlockPos> leaves = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        // When direct-to-inventory is on we also collect the block the player
        // just broke (by cancelling its vanilla break), so the whole tree goes
        // into the inventory consistently.
        boolean collectOrigin = config.dropLoot;
        boolean unbreakable = isUnbreakable(stack);
        // Vanilla has not yet consumed the origin's durability when collectOrigin
        // is true (we cancel it), so start one point higher in that case.
        int durability = stack.getDamageValue() - (collectOrigin ? 0 : 1);
        int threshold = stack.getMaxDamage();
        boolean exhausted = false;

        BlockPos[] offsets = fungus ? fungusOffsets : logOffsets;

        // Seed the scan from the broken block. With direct-to-inventory we cancel
        // the vanilla break, so the origin is still a log here and gets collected below.
        addNeighbours(level, origin, offsets, queue, fungus);
        if (config.chopDown) {
            addNeighbours(level, origin, downOffsets, queue, fungus);
        }

        while (!queue.isEmpty() && !exhausted && logs.size() < maxTreeSize) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (fungus ? !isStemBlock(state) : !isLogBlock(state)) {
                continue;
            }
            if (logs.contains(pos)) {
                continue;
            }

            // Durability per remaining log/stem. Stop before chopping this log if the axe would break.
            if (config.wearOut && !unbreakable) {
                if (config.stopChopping && durability >= threshold) {
                    exhausted = true;
                    break;
                }
                if (level.getRandom().nextFloat() < unbreakingChance(getUnbreakingLevel(level, stack))) {
                    durability++;
                }
            }

            logs.add(pos);

            // Mark 6-adjacent leaves/caps.
            for (BlockPos dir : ORTHO) {
                BlockPos p = pos.offset(dir);
                BlockState s = level.getBlockState(p);
                if (fungus ? isCapBlock(s) : isLeafBlock(s)) {
                    leaves.add(p);
                }
            }

            addNeighbours(level, pos, offsets, queue, fungus);
            if (config.chopDown) {
                addNeighbours(level, pos, downOffsets, queue, fungus);
            }
        }

        int treeSize = logs.size();
        if (treeSize == 0 || leaves.size() < config.minLeavesFound) {
            return false;
        }

        // Determine the full block set to destroy. The origin goes first so the
        // block the player broke disappears immediately even during slow chop.
        Set<BlockPos> toDestroy = new LinkedHashSet<>();
        if (collectOrigin) {
            toDestroy.add(origin);
        }
        toDestroy.addAll(logs);
        if (fungus) {
            Set<BlockPos> caps = flood(level, logs, true);
            protectOtherStems(level, logs, caps);
            toDestroy.addAll(caps);
        } else if (config.destroyLeaves) {
            toDestroy.addAll(collectTreeLeaves(level, logs));
        }

        // Destroy (immediately or slowly).
        if (config.slowChop) {
            slowChopManager.enqueue(level, player, stack, toDestroy);
        } else {
            slowChopManager.destroyNow(level, player, stack, toDestroy);
        }

        // Wear out / break the axe.
        if (config.wearOut && !unbreakable) {
            if (durability >= threshold) {
                breakAxe(player);
            } else {
                stack.setDamageValue(Math.max(0, durability));
            }
        }

        // Hunger scaled to tree size.
        if (config.hunger) {
            int amplifier = Math.min(treeSize, 255);
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, amplifier, false, false, true));
        }

        // Silence the multi-block break noise.
        if (config.stopSound) {
            stopBreakSounds(level, origin);
        }

        // Autoplant.
        if (config.plantSapling) {
            autoPlanter.schedule(level, origin);
        }

        // Cancel the vanilla break only when we collected the origin block into
        // the inventory; otherwise vanilla breaks it (dropping loot normally).
        return collectOrigin;
    }

    // ------------------------------------------------------------------
    // Neighbour scanning / flooding
    // ------------------------------------------------------------------

    private void addNeighbours(ServerLevel level, BlockPos pos, BlockPos[] offsets, ArrayDeque<BlockPos> queue, boolean fungus) {
        for (BlockPos rel : offsets) {
            BlockPos p = pos.offset(rel);
            BlockState s = level.getBlockState(p);
            if (fungus ? isStemBlock(s) : isLogBlock(s)) {
                queue.add(p);
            }
        }
    }

    /** Multi-source flood from the logs to collect connected leaves or fungus caps. */
    private Set<BlockPos> flood(ServerLevel level, Collection<BlockPos> logs, boolean fungus) {
        Set<BlockPos> result = new LinkedHashSet<>();
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        for (BlockPos log : logs) {
            for (BlockPos dir : ORTHO) {
                BlockPos p = log.offset(dir);
                if (fungus ? isCapBlock(level.getBlockState(p)) : isLeafBlock(level.getBlockState(p))) {
                    if (!distance.containsKey(p)) {
                        distance.put(p, 1);
                        queue.add(p);
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = distance.get(p);
            result.add(p);
            if (d >= config.maxTreeSize) {
                continue;
            }
            for (BlockPos rel : FLOOD_OFFSETS) {
                BlockPos np = p.offset(rel);
                if (fungus ? isCapBlock(level.getBlockState(np)) : isLeafBlock(level.getBlockState(np))) {
                    if (!distance.containsKey(np)) {
                        distance.put(np, d + 1);
                        queue.add(np);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Collects tree leaves to destroy the same way the datapack does: walk from
     * each log through orthogonally adjacent leaves, only ever moving to a leaf
     * whose vanilla {@code distance} property is strictly larger than the
     * previous leaf's. This stops at the boundary between two touching canopies
     * instead of flooding into every neighbouring tree.
     */
    private Set<BlockPos> collectTreeLeaves(ServerLevel level, Collection<BlockPos> logs) {
        Set<BlockPos> destroyed = new LinkedHashSet<>();
        Set<BlockPos> searched = new HashSet<>();
        for (BlockPos log : logs) {
            walkLeaves(level, log, 0, destroyed, searched);
        }
        return destroyed;
    }

    /**
     * Recursive leaf walk. {@code parentDistance} is the vanilla distance of the
     * leaf we came from (0 for a log). A leaf is destroyed only when reached via
     * a strictly increasing distance path, mirroring the datapack's
     * {@code leaf_distance_old < leaf_distance} rule.
     */
    private void walkLeaves(ServerLevel level, BlockPos pos, int parentDistance,
                            Set<BlockPos> destroyed, Set<BlockPos> searched) {
        for (BlockPos dir : ORTHO) {
            BlockPos neighbour = pos.offset(dir);
            if (searched.contains(neighbour)) {
                continue;
            }
            BlockState state = level.getBlockState(neighbour);
            if (!isLeafBlock(state)) {
                continue;
            }
            int distance = leafDistance(state);
            if (parentDistance < distance) {
                searched.add(neighbour);
                destroyed.add(neighbour);
                walkLeaves(level, neighbour, distance, destroyed, searched);
            }
        }
    }

    /** Reads the vanilla {@code distance} leaf property (1..14); 0 if absent. */
    private static int leafDistance(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DISTANCE)) {
            return state.getValue(BlockStateProperties.DISTANCE);
        }
        return 0;
    }

    /** If a cap touches a stem that is not part of this chop, protect all caps within 5 blocks. */
    private void protectOtherStems(ServerLevel level, Set<BlockPos> stems, Set<BlockPos> caps) {
        Set<BlockPos> protectedCaps = new HashSet<>();
        for (BlockPos cap : caps) {
            for (BlockPos dir : ORTHO) {
                BlockPos np = cap.offset(dir);
                if (isStemBlock(level.getBlockState(np)) && !stems.contains(np)) {
                    for (BlockPos c : caps) {
                        if (c.distSqr(np) <= 25.0) {
                            protectedCaps.add(c);
                        }
                    }
                }
            }
        }
        caps.removeAll(protectedCaps);
    }

    // ------------------------------------------------------------------
    // Axe durability helpers
    // ------------------------------------------------------------------

    private boolean isUnbreakable(ItemStack stack) {
        return stack.get(DataComponents.UNBREAKABLE) != null;
    }

    private int getUnbreakingLevel(ServerLevel level, ItemStack stack) {
        Holder<Enchantment> unbreaking = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        return EnchantmentHelper.getItemEnchantmentLevel(unbreaking, stack);
    }

    private static float unbreakingChance(int level) {
        if (level <= 0) {
            return 1.0F;
        }
        if (level == 1) {
            return 0.5F;
        }
        if (level == 2) {
            return 0.33333334F;
        }
        return 0.25F;
    }

    private void breakAxe(ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------
    // Stop-sound
    // ------------------------------------------------------------------

    private void stopBreakSounds(ServerLevel level, BlockPos origin) {
        SoundEvent[] sounds = {
                SoundEvents.GRASS_BREAK,
                SoundEvents.WOOD_BREAK,
                SoundEvents.WART_BLOCK_BREAK,
                SoundEvents.SHROOMLIGHT_BREAK,
                SoundEvents.NETHER_WART_BREAK,
                SoundEvents.STEM_BREAK
        };
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || player.blockPosition().distSqr(origin) > 400.0) {
                continue;
            }
            for (SoundEvent sound : sounds) {
                player.connection.send(new ClientboundStopSoundPacket(sound.location(), SoundSource.BLOCKS));
            }
        }
    }

    // ------------------------------------------------------------------
    // Offset tables
    // ------------------------------------------------------------------

    private static BlockPos[] buildLogOffsets(boolean fungus) {
        List<BlockPos> list = new ArrayList<>();
        // y+1: full 3x3, one block at a time like the datapack's search.mcfunction.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                list.add(new BlockPos(dx, 1, dz));
            }
        }
        // y=0: 8-block ring.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                list.add(new BlockPos(dx, 0, dz));
            }
        }
        if (fungus) {
            // Fungi additionally reach the block two above the stem.
            list.add(new BlockPos(0, 2, 0));
        }
        return list.toArray(new BlockPos[0]);
    }

    private static BlockPos[] buildDownOffsets() {
        List<BlockPos> list = new ArrayList<>();
        // y-1: full 3x3 (search_down.mcfunction).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                list.add(new BlockPos(dx, -1, dz));
            }
        }
        return list.toArray(new BlockPos[0]);
    }
}
