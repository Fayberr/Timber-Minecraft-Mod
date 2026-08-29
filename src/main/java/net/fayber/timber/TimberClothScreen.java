package net.fayber.timber;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// cloth config based config screen (the nicer ModMenu GUI used by Chat Heads and
// most other mods). optional dependency: when Cloth Config is installed, ModMenu
// opens this instead of the hand-rolled TimberConfigScreen.
public final class TimberClothScreen {
    private TimberClothScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Timber"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory chopping = builder.getOrCreateCategory(Component.literal("Chopping"));
        chopping.addEntry(bool(eb, "standing", "Works When Standing", true,
                "Chop trees while not sneaking."));
        chopping.addEntry(bool(eb, "sneaking", "Works When Sneaking", false,
                "Chop trees while sneaking."));
        chopping.addEntry(bool(eb, "chop_trees", "Chop Trees", true,
                "All kind of trees will get chopped."));
        chopping.addEntry(bool(eb, "chop_fungi", "Chop Huge Fungi", false,
                "Crimson and warped huge fungi (nether trees) will get chopped."));
        chopping.addEntry(bool(eb, "chop_mushrooms", "Chop Giant Mushrooms", false,
                "Giant brown and red mushrooms will get chopped."));
        chopping.addEntry(bool(eb, "chop_down", "Enable Chopping Down", false,
                "Normally when you cut a tree in the middle only the top part gets destroyed. "
                        + "If enabled the bottom part gets destroyed as well."));
        chopping.addEntry(bool(eb, "destroy_leaves", "Destroy Leaves", true,
                "Destroys the tree's leaves together with the logs."));
        chopping.addEntry(bool(eb, "plant_sapling", "Auto Plant Saplings", false,
                "All kind of saplings laying on the ground will get planted every 2 seconds."));
        chopping.addEntry(bool(eb, "slow_chop", "Chop Slowly", false,
                "The tree won't be chopped instantly anymore. This option can increase performance."));

        ConfigCategory effects = builder.getOrCreateCategory(Component.literal("Effects"));
        effects.addEntry(bool(eb, "stop_chopping", "Stop Chopping Once Axe Breaks", true,
                "Stops chopping as soon as your axe doesn't have enough durability for the whole tree."));
        effects.addEntry(bool(eb, "wear_out", "Wear out Tool Depending on Tree Size", true,
                "The tool's durability decreases by the amount of destroyed logs. "
                        + "When disabled only one durability is deducted."));
        effects.addEntry(bool(eb, "drop_loot", "Drop Loot Straight into Inventory", false,
                "Teleports the tree's drops straight into your inventory. Overflow drops on the ground."));
        effects.addEntry(bool(eb, "hunger", "Give Additional Hunger Effect", false,
                "Gives a hunger effect for 1 second at the same level as the tree size."));
        effects.addEntry(bool(eb, "stop_sound", "Stop Loud Breaking Noises", false,
                "Suppresses loud breaking noises when chopping a tree."));
        effects.addEntry(bool(eb, "persistent", "Destroy Player-Placed Trees", false,
                "Also destroys player-placed trees (persistent leaves). Use with care."));

        ConfigCategory axes = builder.getOrCreateCategory(Component.literal("Axes"));
        axes.addEntry(bool(eb, "wooden_axe", "Enable Wooden Axe", true,
                "Wooden axes can chop down trees."));
        axes.addEntry(bool(eb, "stone_axe", "Enable Stone Axe", true,
                "Stone axes can chop down trees."));
        axes.addEntry(bool(eb, "copper_axe", "Enable Copper Axe", true,
                "Copper axes can chop down trees."));
        axes.addEntry(bool(eb, "iron_axe", "Enable Iron Axe", true,
                "Iron axes can chop down trees."));
        axes.addEntry(bool(eb, "golden_axe", "Enable Golden Axe", true,
                "Golden axes can chop down trees."));
        axes.addEntry(bool(eb, "diamond_axe", "Enable Diamond Axe", true,
                "Diamond axes can chop down trees."));
        axes.addEntry(bool(eb, "netherite_axe", "Enable Netherite Axe", true,
                "Netherite axes can chop down trees."));

        ConfigCategory limits = builder.getOrCreateCategory(Component.literal("Limits"));
        limits.addEntry(slider(eb, "blocks_per_chop", "Amount of Destroyed Blocks per Chop",
                1, 1, 64, "Default: 1"));
        limits.addEntry(slider(eb, "time_between_chops", "Time in Ticks Between Chops",
                1, 1, 40, "Default: 1"));
        limits.addEntry(slider(eb, "max_tree_size", "Max. Tree Size",
                555, 10, 2000, "Trees bigger than this set value won't get further processed. Default: 555"));
        limits.addEntry(slider(eb, "min_leaves_found", "Min. Leaves to Valid a Tree",
                5, 1, 100, "A tree is only valid if it has at least this many naturally generated leaves. Default: 5"));

        return builder.build();
    }

    private static AbstractConfigListEntry bool(ConfigEntryBuilder eb, String key, String label,
                                                boolean defaultValue, String tooltip) {
        return eb.startBooleanToggle(Component.literal(label), TimberConfig.getBool(key))
                .setDefaultValue(defaultValue)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(value -> TimberConfig.set(key, String.valueOf(value)))
                .build();
    }

    private static AbstractConfigListEntry slider(ConfigEntryBuilder eb, String key, String label,
                                                  int defaultValue, int min, int max, String tooltip) {
        return eb.startIntSlider(Component.literal(label), TimberConfig.getInt(key), min, max)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(value -> TimberConfig.set(key, String.valueOf(value)))
                .build();
    }
}
