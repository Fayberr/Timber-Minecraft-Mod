# Timber

A Fabric mod that fells whole trees (and optionally giant fungi) when you break
one log, instead of leaving the rest of the tree floating in the air. Inspired
by the classic Vanilla Tweaks Timber datapack, this is a full behaviour port
into a server-side Fabric mod for Minecraft 26.1.2 (deobfuscated mappings,
Java 25).

## Command

- `/timber toggle` toggles Timber felling for yourself. Requires operator level 2.

## Configuration

All options live in `TimberConfig.java` and default to the original datapack
settings. Change the field values and rebuild to customise:

- `standing` (true): chop while not sneaking.
- `sneaking` (false): also chop while sneaking.
- `chopTrees` (true): fell trees (logs and leaves).
- `chopFungi` (false): fell giant fungi (stems and caps).
- `chopDown` (false): also search 9 blocks below the broken log.
- `destroyLeaves` (true): destroy the attached leaves.
- `plantSapling` (false): auto-replant a dropped sapling after a chop.
- `slowChop` (false): break the tree gradually over several ticks instead of instantly.
- `stopChopping` (true): stop mid-tree if the axe would break.
- `wearOut` (true): axes lose one durability point per chopped log.
- `dropLoot` (false): false drops items at the block, true gives them to your inventory.
- `hunger` (false): apply hunger scaled to the tree size.
- `stopSound` (false): silence the multi-block break noise.
- `persistent` (false): also chop persistent (placed) leaves.
- `woodenAxe` / `stoneAxe` / `copperAxe` / `ironAxe` / `goldenAxe` / `diamondAxe` / `netheriteAxe` (true): which axe tiers can fell trees.
- `blocksPerChop` (1): blocks destroyed per slow-chop tick.
- `timeBetweenChops` (1): ticks between slow-chop steps.
- `maxTreeSize` (555): maximum number of logs counted as one tree.
- `minLeavesFound` (5): minimum leaves attached before a tree is felled.

## Details

- Runs on the server. The integrated server in singleplayer also works.
- Clients do not need the mod installed.
- Axe durability respects the Unbreaking enchantment.
- Slow chop and auto-replant run per server tick.

## Requirements

- Fabric Loader 0.19.3 or newer for Minecraft 26.1.2.
- Fabric API for 26.1.2.
- Java 25.

## Building

JDK 25 and Gradle 9.7 or newer (Loom 1.17.19).

```bash
./gradlew build
```

The jar is in `build/libs/`.

## License

GPL-3.0-or-later
