package net.fayber.timber;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

// a clickable chat menu, the mod equivalent of the datapack's tellraw settings
// menu. two pages of [ ✔ ] / [ ❌ ] toggles plus [ ✎ ] value prompts, all with
// the datapack's hover descriptions. works on dedicated servers too (the
// ModMenu GUI is singleplayer-only).
public final class TimberSettingsMenu {
    private TimberSettingsMenu() {}

    public static void open(ServerPlayer player, int page) {
        header(player, page);
        if (page <= 1) {
            pageOne(player);
        } else {
            pageTwo(player);
        }
        footer(player);
    }

    // flips a boolean setting and re-opens the menu on that setting's page.
    public static boolean toggle(ServerPlayer player, String key) {
        String value = String.valueOf(!TimberConfig.getBool(key));
        try {
            if (!TimberConfig.set(key, value)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        open(player, pageOf(key));
        return true;
    }

    private static int pageOf(String key) {
        return switch (key.toLowerCase()) {
            case "wooden_axe", "stone_axe", "copper_axe", "iron_axe", "golden_axe",
                 "diamond_axe", "netherite_axe", "chop_trees", "chop_fungi", "chop_mushrooms" -> 2;
            default -> 1;
        };
    }

    private static void header(ServerPlayer player, int page) {
        player.sendSystemMessage(Component.literal(""));
        MutableComponent title = Component.literal("                 Timber ")
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(" Global Settings    ");
        if (page > 1) {
            title.append(Component.literal("< ").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent.RunCommand("/timber settings 1"))));
        } else {
            title.append(Component.literal("< ").withStyle(ChatFormatting.DARK_GRAY));
        }
        title.append(Component.literal(page + "/2"));
        if (page < 2) {
            title.append(Component.literal(" >").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent.RunCommand("/timber settings 2"))));
        } else {
            title.append(Component.literal(" >").withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(title);
        player.sendSystemMessage(Component.literal("                                                  ")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
    }

    private static void footer(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("                                                  ")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
        player.sendSystemMessage(Component.literal("Click a setting to change it.")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void pageOne(ServerPlayer player) {
        booleanLine(player, "standing", "Works When Standing", null);
        booleanLine(player, "sneaking", "Works When Sneaking", null);
        booleanLine(player, "destroy_leaves", "Destroy Leaves", null);
        booleanLine(player, "plant_sapling", "Auto Plant Saplings",
                "All kind of saplings laying on the ground will get planted every 2 seconds.");
        booleanLine(player, "slow_chop", "Chop Slowly",
                "The tree won't be chopped instantly anymore. This option can increase performance.");
        booleanLine(player, "chop_down", "Enable Chopping Down",
                "Normally when you cut a tree in the middle only the top part gets destroyed. If enabled the bottom part gets destroyed as well.");
        booleanLine(player, "stop_chopping", "Stop Chopping Once Axe Breaks",
                "Stops chopping as soon as your axe doesn't have enough durability for the whole tree.");
        booleanLine(player, "wear_out", "Additionally Wear out Tool Depending on Tree Size",
                "The tool's durability decreases by the amount of destroyed logs. When disabled only one durability is deducted.");
        booleanLine(player, "drop_loot", "Drop Loot Straight into Inventory",
                "Teleports the tree's drops straight into your inventory. Overflow drops on the ground.");
        booleanLine(player, "hunger", "Give Additional Hunger Effect",
                "Gives a hunger effect for 1 second at the same level as the tree size.");
        booleanLine(player, "stop_sound", "Stop Loud Breaking Noises",
                "Suppresses loud breaking noises when chopping a tree.");
        booleanLine(player, "persistent", "Destroy Player-Placed Trees",
                "Also destroys player-placed trees (persistent leaves). Use with care.");
    }

    private static void pageTwo(ServerPlayer player) {
        booleanLine(player, "wooden_axe", "Enable Wooden Axe", null);
        booleanLine(player, "stone_axe", "Enable Stone Axe", null);
        booleanLine(player, "copper_axe", "Enable Copper Axe", null);
        booleanLine(player, "iron_axe", "Enable Iron Axe", null);
        booleanLine(player, "golden_axe", "Enable Golden Axe", null);
        booleanLine(player, "diamond_axe", "Enable Diamond Axe", null);
        booleanLine(player, "netherite_axe", "Enable Netherite Axe", null);
        booleanLine(player, "chop_trees", "Chop Trees", "All kind of trees will get chopped.");
        booleanLine(player, "chop_fungi", "Chop Huge Fungi",
                "Crimson and warped huge fungi (nether trees) will get chopped.");
        booleanLine(player, "chop_mushrooms", "Chop Giant Mushrooms",
                "Giant brown and red mushrooms will get chopped.");
        intLine(player, "blocks_per_chop", "Set Amount of Destroyed Blocks per Chop", "Default: 1");
        intLine(player, "time_between_chops", "Set Time in Ticks Between Chops", "Default: 1");
        intLine(player, "max_tree_size", "Set Max. Tree Size",
                "Trees bigger than this set value won't get further processed. Default: 555");
        intLine(player, "min_leaves_found", "Set Min. Leaves to Valid a Tree",
                "A tree is only valid if it has at least this many naturally generated leaves. Default: 5");
    }

    private static void booleanLine(ServerPlayer player, String key, String label, String hover) {
        boolean on = TimberConfig.getBool(key);
        MutableComponent check = Component.literal(on ? "[ ✔ ]" : "[ ❌ ]")
                .withStyle(style(on ? ChatFormatting.GREEN : ChatFormatting.RED, key, hover));
        player.sendSystemMessage(check.append(Component.literal(" " + label)
                .withStyle(style(on ? ChatFormatting.WHITE : ChatFormatting.WHITE, key, hover))));
    }

    private static void intLine(ServerPlayer player, String key, String label, String hover) {
        int current = TimberConfig.getInt(key);
        MutableComponent edit = Component.literal("[ ✎ ]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent.SuggestCommand("/timber config set " + key + " "))
                        .withHoverEvent(hover(hover)));
        player.sendSystemMessage(edit.append(Component.literal(" " + label + " (Current: " + current + ")")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent.SuggestCommand("/timber config set " + key + " "))
                        .withHoverEvent(hover(hover)))));
    }

    private static Style style(ChatFormatting color, String key, String hover) {
        Style style = Style.EMPTY
                .withColor(color)
                .withClickEvent(new ClickEvent.RunCommand("/timber settings toggle " + key));
        if (hover != null) {
            style = style.withHoverEvent(hover(hover));
        }
        return style;
    }

    private static HoverEvent hover(String text) {
        return new HoverEvent.ShowText(Component.literal(text));
    }
}
