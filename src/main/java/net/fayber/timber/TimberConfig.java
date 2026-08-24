package net.fayber.timber;

/**
 * All Timber settings. Defaults mirror the original Timber datapack config
 * (timber/config/config.txt).
 */
public class TimberConfig {

    // --- Trigger conditions ---
    public boolean standing = true;     // chop while not sneaking
    public boolean sneaking = false;    // chop while sneaking

    // --- Detection ---
    public boolean chopTrees = true;    // chop trees (logs + leaves)
    public boolean chopFungi = false;   // chop giant fungi (stems + caps)
    public boolean chopDown = false;    // also search 9 blocks below the broken log

    // --- Behaviour ---
    public boolean destroyLeaves = true;
    public boolean plantSapling = false;
    public boolean slowChop = false;
    public boolean stopChopping = true; // stop mid-tree when the axe would break
    public boolean wearOut = true;      // axes lose durability per chopped log
    public boolean dropLoot = false;    // false = drop at position, true = give to inventory
    public boolean hunger = false;      // apply hunger scaled to tree size
    public boolean stopSound = false;   // silence the multi-block break noise
    public boolean persistent = false;  // also chop persistent leaves

    // --- Axes ---
    public boolean woodenAxe = true;
    public boolean stoneAxe = true;
    public boolean copperAxe = true;
    public boolean ironAxe = true;
    public boolean goldenAxe = true;
    public boolean diamondAxe = true;
    public boolean netheriteAxe = true;

    // --- Slow chop ---
    public int blocksPerChop = 1;
    public int timeBetweenChops = 1;

    // --- Limits ---
    public int maxTreeSize = 555;
    public int minLeavesFound = 5;
}
