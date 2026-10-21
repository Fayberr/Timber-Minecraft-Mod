package net.fayber.timber;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;

public class VineEffectsGameTest {
    static final class Capture extends Connection {
        final List<Packet<?>> packets = new ArrayList<>();
        Capture() { super(PacketFlow.SERVERBOUND); }
        @Override public void send(Packet<?> packet) { packets.add(packet); }
        @Override public void send(Packet<?> packet, ChannelFutureListener listener) { packets.add(packet); }
        @Override public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) { packets.add(packet); }
    }

    @GameTest
    public void vinesFollowFellingEffectsAndKeepVanillaSupports(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "TimberVines"), false);
        var player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        var capture = new Capture();
        new EmbeddedChannel(capture);
        server.getPlayerList().placeNewPlayer(capture, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(base.getX(), base.getY(), base.getZ() + 3);
        var config = TimberConfig.get();
        boolean oldSlow = config.slowChop, oldDrop = config.dropLoot, oldWear = config.wearOut;
        int oldMin = config.minLeavesFound, oldParticles = config.minimizedParticles;
        var oldMode = config.fellingEffects;
        try {
            config.slowChop = false;
            config.dropLoot = false;
            config.wearOut = false;
            config.minLeavesFound = 0;
            config.minimizedParticles = 5;
            for (var mode : FellingEffects.values()) {
                config.fellingEffects = mode;
                setup(level, base);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
                int logsBefore = dropped(level, base, Items.JUNGLE_LOG);
                int vinesBefore = dropped(level, base, Items.VINE);
                capture.packets.clear();
                helper.assertTrue(player.gameMode.destroyBlock(base), "vanilla origin is mined");
                helper.assertTrue(level.getBlockState(base.east()).isAir(), "origin vine loses support");
                helper.assertTrue(level.getBlockState(base.above().east()).isAir(), "upper vine loses support");
                helper.assertTrue(level.getBlockState(base.east().below()).isAir(), "hanging vine chain falls");
                helper.assertTrue(level.getBlockState(base.west()).is(Blocks.VINE), "vine with a second support survives");
                helper.assertTrue(level.getBlockState(base.west()).getValue(VineBlock.NORTH), "remaining face retained");
                checkEffects(helper, capture, mode, 3, 5);
                if (mode == FellingEffects.UNSUPPRESSED) {
                    helper.assertTrue(capture.packets.stream().filter(packet -> packet instanceof ClientboundLevelEventPacket event
                            && event.getType() == 2001 && event.getPos().equals(base.above())
                            && Block.stateById(event.getData()).is(Blocks.JUNGLE_LOG)).count() == 1,
                            "Timber log gets one sound-bearing vanilla event");
                }
                helper.assertTrue(dropped(level, base, Items.JUNGLE_LOG) - logsBefore == 2, "log drops preserved in " + mode);
                helper.assertTrue(dropped(level, base, Items.VINE) == vinesBefore, "unsupported vines keep vanilla no-tool drops");
                // An unrelated support break after the chop must keep vanilla effects.
                setup(level, base);
                capture.packets.clear();
                level.setBlock(base.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                helper.assertTrue(vineEvents(capture) > 0, "ordinary vine breaks remain unsuppressed after felling");
            }
            // Exercise the actual queued block path one step at a time with live settings.
            var slowConfig = new TimberConfig();
            slowConfig.timeBetweenChops = 1;
            slowConfig.blocksPerChop = 1;
            slowConfig.fellingEffects = FellingEffects.MINIMIZED;
            slowConfig.minimizedParticles = 7;
            var manager = new SlowChopManager(slowConfig);
            setup(level, base);
            capture.packets.clear();
            manager.enqueue(level, player, new ItemStack(Items.DIAMOND_AXE), List.of(base.above()));
            manager.tick(server);
            helper.assertTrue(level.getBlockState(base.above().east()).isAir(), "slow chop breaks supported vine");
            checkEffects(helper, capture, FellingEffects.MINIMIZED, 1, 7);
            helper.succeed();
        } finally {
            config.slowChop = oldSlow;
            config.dropLoot = oldDrop;
            config.wearOut = oldWear;
            config.minLeavesFound = oldMin;
            config.minimizedParticles = oldParticles;
            config.fellingEffects = oldMode;
            server.getPlayerList().remove(player);
        }
    }

    private static void setup(net.minecraft.server.level.ServerLevel level, BlockPos base) {
        // Suppress updates during fixture assembly; normal updates drive every tested removal.
        for (int y = -1; y <= 2; y++) for (int x = -2; x <= 1; x++)
            level.setBlock(base.offset(x, y, 0), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(base, Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        level.setBlock(base.above(), Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        level.setBlock(base.east(), Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true), 2);
        level.setBlock(base.above().east(), Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true), 2);
        level.setBlock(base.east().below(), Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true), 2);
        level.setBlock(base.west().north(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(base.west(), Blocks.VINE.defaultBlockState()
                .setValue(VineBlock.EAST, true).setValue(VineBlock.NORTH, true), 2);
    }

    private static int dropped(net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(5)).stream()
                .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static long vineEvents(Capture capture) {
        return capture.packets.stream().filter(packet -> packet instanceof ClientboundLevelEventPacket event
                && event.getType() == 2001 && Block.stateById(event.getData()).is(Blocks.VINE)).count();
    }

    private static void checkEffects(GameTestHelper helper, Capture capture, FellingEffects mode, int expected, int count) {
        var particles = capture.packets.stream().filter(packet -> packet instanceof ClientboundLevelParticlesPacket event
                && event.particle() instanceof BlockParticleOption option && option.getState().is(Blocks.VINE))
                .map(packet -> (ClientboundLevelParticlesPacket) packet).toList();
        if (mode == FellingEffects.UNSUPPRESSED) {
            helper.assertTrue(vineEvents(capture) == expected, "full vine effects once per destroyed vine");
            helper.assertTrue(particles.isEmpty(), "no duplicate reduced effects");
        } else {
            helper.assertTrue(vineEvents(capture) == 0, "no full vine particle/sound events in " + mode + ": " + capture.packets.stream().filter(packet -> packet instanceof ClientboundLevelEventPacket event && event.getType() == 2001 && Block.stateById(event.getData()).is(Blocks.VINE)).map(packet -> ((ClientboundLevelEventPacket) packet).getPos().toString()).toList());
            helper.assertTrue(particles.size() == (mode == FellingEffects.MINIMIZED ? expected : 0), "reduced vine particle packet count");
            helper.assertTrue(particles.stream().allMatch(event -> event.count() == count), "configured particles per vine");
        }
    }
}
