package net.fayber.timber;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod config, stored as {@code config/timber.json}. Defaults mirror the
 * Vanilla Tweaks Timber datapack. Values can be changed in-game with
 * {@code /timber config} or, in singleplayer, from the ModMenu config screen.
 *
 * <p>The shared instance is mutated in place by {@link #load()} so the
 * {@link TreeFeller} and {@link SlowChopManager} (which hold a reference to
 * {@link #get()}) always see the current values, including after a reload.
 */
public final class TimberConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("timber.json");
    private static final Logger LOGGER = LoggerFactory.getLogger("timber");

    private static final TimberConfig INSTANCE = new TimberConfig();

    // Chopping trigger and behaviour.
    public boolean standing = true;
    public boolean sneaking = false;
    public boolean chopTrees = true;
    public boolean chopFungi = false;
    public boolean chopDown = false;
    public boolean destroyLeaves = true;
    public boolean plantSapling = false;
    public boolean slowChop = false;
    public boolean stopChopping = true;
    public boolean wearOut = true;
    public boolean dropLoot = false;
    public boolean hunger = false;
    public boolean stopSound = false;
    public boolean persistent = false;

    // Which axes may fell.
    public boolean woodenAxe = true;
    public boolean stoneAxe = true;
    public boolean copperAxe = true;
    public boolean ironAxe = true;
    public boolean goldenAxe = true;
    public boolean diamondAxe = true;
    public boolean netheriteAxe = true;

    // Slow-chop pacing and size limits.
    public int blocksPerChop = 1;
    public int timeBetweenChops = 1;
    public int maxTreeSize = 555;
    public int minLeavesFound = 5;

    public static TimberConfig get() {
        return INSTANCE;
    }

    /** Loads {@code config/timber.json} into the shared instance, then writes it back. */
    public static void load() {
        if (Files.exists(PATH)) {
            try {
                Raw raw = GSON.fromJson(Files.readString(PATH), Raw.class);
                if (raw != null) {
                    apply(raw);
                }
            } catch (Exception e) {
                LOGGER.error("[Timber] Failed to read config, using defaults", e);
            }
        }
        save();
    }

    private static void apply(Raw raw) {
        TimberConfig c = INSTANCE;
        if (raw.standing != null) c.standing = raw.standing;
        if (raw.sneaking != null) c.sneaking = raw.sneaking;
        if (raw.chop_trees != null) c.chopTrees = raw.chop_trees;
        if (raw.chop_fungi != null) c.chopFungi = raw.chop_fungi;
        if (raw.chop_down != null) c.chopDown = raw.chop_down;
        if (raw.destroy_leaves != null) c.destroyLeaves = raw.destroy_leaves;
        if (raw.plant_sapling != null) c.plantSapling = raw.plant_sapling;
        if (raw.slow_chop != null) c.slowChop = raw.slow_chop;
        if (raw.stop_chopping != null) c.stopChopping = raw.stop_chopping;
        if (raw.wear_out != null) c.wearOut = raw.wear_out;
        if (raw.drop_loot != null) c.dropLoot = raw.drop_loot;
        if (raw.hunger != null) c.hunger = raw.hunger;
        if (raw.stop_sound != null) c.stopSound = raw.stop_sound;
        if (raw.persistent != null) c.persistent = raw.persistent;
        if (raw.wooden_axe != null) c.woodenAxe = raw.wooden_axe;
        if (raw.stone_axe != null) c.stoneAxe = raw.stone_axe;
        if (raw.copper_axe != null) c.copperAxe = raw.copper_axe;
        if (raw.iron_axe != null) c.ironAxe = raw.iron_axe;
        if (raw.golden_axe != null) c.goldenAxe = raw.golden_axe;
        if (raw.diamond_axe != null) c.diamondAxe = raw.diamond_axe;
        if (raw.netherite_axe != null) c.netheriteAxe = raw.netherite_axe;
        if (raw.blocks_per_chop != null) c.blocksPerChop = raw.blocks_per_chop;
        if (raw.time_between_chops != null) c.timeBetweenChops = raw.time_between_chops;
        if (raw.max_tree_size != null) c.maxTreeSize = raw.max_tree_size;
        if (raw.min_leaves_found != null) c.minLeavesFound = raw.min_leaves_found;
    }

    public static void save() {
        Raw raw = new Raw();
        raw.standing = INSTANCE.standing;
        raw.sneaking = INSTANCE.sneaking;
        raw.chop_trees = INSTANCE.chopTrees;
        raw.chop_fungi = INSTANCE.chopFungi;
        raw.chop_down = INSTANCE.chopDown;
        raw.destroy_leaves = INSTANCE.destroyLeaves;
        raw.plant_sapling = INSTANCE.plantSapling;
        raw.slow_chop = INSTANCE.slowChop;
        raw.stop_chopping = INSTANCE.stopChopping;
        raw.wear_out = INSTANCE.wearOut;
        raw.drop_loot = INSTANCE.dropLoot;
        raw.hunger = INSTANCE.hunger;
        raw.stop_sound = INSTANCE.stopSound;
        raw.persistent = INSTANCE.persistent;
        raw.wooden_axe = INSTANCE.woodenAxe;
        raw.stone_axe = INSTANCE.stoneAxe;
        raw.copper_axe = INSTANCE.copperAxe;
        raw.iron_axe = INSTANCE.ironAxe;
        raw.golden_axe = INSTANCE.goldenAxe;
        raw.diamond_axe = INSTANCE.diamondAxe;
        raw.netherite_axe = INSTANCE.netheriteAxe;
        raw.blocks_per_chop = INSTANCE.blocksPerChop;
        raw.time_between_chops = INSTANCE.timeBetweenChops;
        raw.max_tree_size = INSTANCE.maxTreeSize;
        raw.min_leaves_found = INSTANCE.minLeavesFound;
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(raw));
        } catch (IOException e) {
            LOGGER.error("[Timber] Failed to save config", e);
        }
    }

    /** Sets a key by name (command / ModMenu); returns false if unknown. */
    public static boolean set(String key, String value) {
        TimberConfig c = INSTANCE;
        switch (key.toLowerCase()) {
            case "standing" -> c.standing = parseBool(value);
            case "sneaking" -> c.sneaking = parseBool(value);
            case "chop_trees" -> c.chopTrees = parseBool(value);
            case "chop_fungi" -> c.chopFungi = parseBool(value);
            case "chop_down" -> c.chopDown = parseBool(value);
            case "destroy_leaves" -> c.destroyLeaves = parseBool(value);
            case "plant_sapling" -> c.plantSapling = parseBool(value);
            case "slow_chop" -> c.slowChop = parseBool(value);
            case "stop_chopping" -> c.stopChopping = parseBool(value);
            case "wear_out" -> c.wearOut = parseBool(value);
            case "drop_loot" -> c.dropLoot = parseBool(value);
            case "hunger" -> c.hunger = parseBool(value);
            case "stop_sound" -> c.stopSound = parseBool(value);
            case "persistent" -> c.persistent = parseBool(value);
            case "wooden_axe" -> c.woodenAxe = parseBool(value);
            case "stone_axe" -> c.stoneAxe = parseBool(value);
            case "copper_axe" -> c.copperAxe = parseBool(value);
            case "iron_axe" -> c.ironAxe = parseBool(value);
            case "golden_axe" -> c.goldenAxe = parseBool(value);
            case "diamond_axe" -> c.diamondAxe = parseBool(value);
            case "netherite_axe" -> c.netheriteAxe = parseBool(value);
            case "blocks_per_chop" -> c.blocksPerChop = parseInt(value);
            case "time_between_chops" -> c.timeBetweenChops = parseInt(value);
            case "max_tree_size" -> c.maxTreeSize = parseInt(value);
            case "min_leaves_found" -> c.minLeavesFound = parseInt(value);
            default -> {
                return false;
            }
        }
        save();
        return true;
    }

    /** Reads a boolean key; the ModMenu screen uses this so key names stay in one place. */
    public static boolean getBool(String key) {
        TimberConfig c = INSTANCE;
        return switch (key.toLowerCase()) {
            case "standing" -> c.standing;
            case "sneaking" -> c.sneaking;
            case "chop_trees" -> c.chopTrees;
            case "chop_fungi" -> c.chopFungi;
            case "chop_down" -> c.chopDown;
            case "destroy_leaves" -> c.destroyLeaves;
            case "plant_sapling" -> c.plantSapling;
            case "slow_chop" -> c.slowChop;
            case "stop_chopping" -> c.stopChopping;
            case "wear_out" -> c.wearOut;
            case "drop_loot" -> c.dropLoot;
            case "hunger" -> c.hunger;
            case "stop_sound" -> c.stopSound;
            case "persistent" -> c.persistent;
            case "wooden_axe" -> c.woodenAxe;
            case "stone_axe" -> c.stoneAxe;
            case "copper_axe" -> c.copperAxe;
            case "iron_axe" -> c.ironAxe;
            case "golden_axe" -> c.goldenAxe;
            case "diamond_axe" -> c.diamondAxe;
            case "netherite_axe" -> c.netheriteAxe;
            default -> false;
        };
    }

    /** Reads an integer key; the ModMenu screen uses this so key names stay in one place. */
    public static int getInt(String key) {
        TimberConfig c = INSTANCE;
        return switch (key.toLowerCase()) {
            case "blocks_per_chop" -> c.blocksPerChop;
            case "time_between_chops" -> c.timeBetweenChops;
            case "max_tree_size" -> c.maxTreeSize;
            case "min_leaves_found" -> c.minLeavesFound;
            default -> 0;
        };
    }

    private static boolean parseBool(String v) {
        return v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }

    private static int parseInt(String v) {
        return Integer.parseInt(v);
    }

    @Override
    public String toString() {
        return "standing=" + standing
                + ", sneaking=" + sneaking
                + ", chop_trees=" + chopTrees
                + ", chop_fungi=" + chopFungi
                + ", chop_down=" + chopDown
                + ", destroy_leaves=" + destroyLeaves
                + ", plant_sapling=" + plantSapling
                + ", slow_chop=" + slowChop
                + ", stop_chopping=" + stopChopping
                + ", wear_out=" + wearOut
                + ", drop_loot=" + dropLoot
                + ", hunger=" + hunger
                + ", stop_sound=" + stopSound
                + ", persistent=" + persistent
                + ", wooden_axe=" + woodenAxe
                + ", stone_axe=" + stoneAxe
                + ", copper_axe=" + copperAxe
                + ", iron_axe=" + ironAxe
                + ", golden_axe=" + goldenAxe
                + ", diamond_axe=" + diamondAxe
                + ", netherite_axe=" + netheriteAxe
                + ", blocks_per_chop=" + blocksPerChop
                + ", time_between_chops=" + timeBetweenChops
                + ", max_tree_size=" + maxTreeSize
                + ", min_leaves_found=" + minLeavesFound;
    }

    /** JSON shape on disk; boxed so missing keys keep their defaults. */
    private static class Raw {
        Boolean standing;
        Boolean sneaking;
        Boolean chop_trees;
        Boolean chop_fungi;
        Boolean chop_down;
        Boolean destroy_leaves;
        Boolean plant_sapling;
        Boolean slow_chop;
        Boolean stop_chopping;
        Boolean wear_out;
        Boolean drop_loot;
        Boolean hunger;
        Boolean stop_sound;
        Boolean persistent;
        Boolean wooden_axe;
        Boolean stone_axe;
        Boolean copper_axe;
        Boolean iron_axe;
        Boolean golden_axe;
        Boolean diamond_axe;
        Boolean netherite_axe;
        Integer blocks_per_chop;
        Integer time_between_chops;
        Integer max_tree_size;
        Integer min_leaves_found;
    }
}
