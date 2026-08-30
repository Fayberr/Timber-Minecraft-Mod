package net.fayber.timber;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimberMod implements ModInitializer {
    public static final String MOD_ID = "timber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final TimberConfig config = TimberConfig.get();
    private final SlowChopManager slowChopManager = new SlowChopManager(config);
    private final AutoPlanter autoPlanter = new AutoPlanter();
    private final TreeFeller treeFeller = new TreeFeller(config, slowChopManager, autoPlanter);

    @Override
    public void onInitialize() {
        TimberConfig.load();

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                treeFeller.onBlockBreakBefore(level, player, pos, state));

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register(this::registerCommands);

        LOGGER.info("[Timber] Initialized. Config: {}", config);
    }

    private void onServerTick(MinecraftServer server) {
        slowChopManager.tick(server);
        autoPlanter.tick(server);
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext buildContext,
                                  Commands.CommandSelection selection) {
        // /timber settings is driven by clickable chat links (run_command). The
        // client shows a "Confirm Command Execution" prompt for clicked commands
        // whose node requires elevated permissions, so the settings subtree must
        // not declare a permission requirement. It is checked server-side in the
        // executors instead.
        dispatcher.register(
            Commands.literal("timber")
                .then(
                    Commands.literal("toggle")
                        .requires(source -> isOperator(source))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean disabled = treeFeller.toggle(player);
                            player.sendSystemMessage(Component.literal(
                                    disabled ? "Timber felling disabled for you."
                                             : "Timber felling enabled for you."));
                            return 1;
                        })
                )
                .then(
                    Commands.literal("config")
                        .requires(source -> isOperator(source))
                        .executes(context -> showConfig(context.getSource()))
                        .then(
                            Commands.literal("get")
                                .executes(context -> showConfig(context.getSource()))
                        )
                        .then(
                            Commands.literal("set")
                                .then(
                                    Commands.argument("key", StringArgumentType.word())
                                        .then(
                                            Commands.argument("value", StringArgumentType.word())
                                                .executes(context -> setConfig(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "key"),
                                                    StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("settings")
                        .executes(context -> openSettings(context.getSource(), 1))
                        .then(
                            Commands.literal("1")
                                .executes(context -> openSettings(context.getSource(), 1))
                        )
                        .then(
                            Commands.literal("2")
                                .executes(context -> openSettings(context.getSource(), 2))
                        )
                        .then(
                            Commands.literal("toggle")
                                .then(
                                    Commands.argument("key", StringArgumentType.word())
                                        .executes(context -> toggleSetting(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "key")
                                        ))
                                )
                        )
                )
        );
    }

    private static boolean isOperator(CommandSourceStack source) {
        if (source.getServer() != null && source.getServer().isSingleplayer()) {
            return true;
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private int openSettings(CommandSourceStack source, int page) throws CommandSyntaxException {
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("[Timber] This command requires operator level 2."));
            return 0;
        }
        TimberSettingsMenu.open(source.getPlayerOrException(), page);
        return 1;
    }

    private int showConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[Timber] Config: " + TimberConfig.get()), false);
        return 1;
    }

    private int setConfig(CommandSourceStack source, String key, String value) {
        try {
            if (TimberConfig.set(key, value)) {
                source.sendSuccess(() -> Component.literal("[Timber] Set " + key
                        + " to " + value + ". New config: " + TimberConfig.get()), true);
                return 1;
            }
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("[Timber] " + key + " expects a number."));
            return 0;
        }
        source.sendFailure(Component.literal("[Timber] Unknown config key '" + key
                + "'. Use /timber config get to list valid keys."));
        return 0;
    }

    private int toggleSetting(CommandSourceStack source, String key) throws CommandSyntaxException {
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("[Timber] This command requires operator level 2."));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (!TimberSettingsMenu.toggle(player, key)) {
            source.sendFailure(Component.literal("[Timber] Unknown setting '" + key
                    + "'. Boolean settings only."));
            return 0;
        }
        return 1;
    }
}
