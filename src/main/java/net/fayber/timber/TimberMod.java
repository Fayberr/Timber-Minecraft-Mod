package net.fayber.timber;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class TimberMod implements ModInitializer {

    private final TimberConfig config = new TimberConfig();
    private final SlowChopManager slowChopManager = new SlowChopManager(config);
    private final AutoPlanter autoPlanter = new AutoPlanter();
    private final TreeFeller treeFeller = new TreeFeller(config, slowChopManager, autoPlanter);

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
                treeFeller.onBlockBreak(level, player, pos, state));

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            slowChopManager.tick(level);
            autoPlanter.tick(level);
        }
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext buildContext,
                                  Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("timber")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean disabled = treeFeller.toggle(player);
                            player.sendSystemMessage(Component.literal(
                                    disabled ? "Timber felling disabled for you."
                                             : "Timber felling enabled for you."));
                            return 1;
                        })));
    }
}
