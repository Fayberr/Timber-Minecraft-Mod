# Timber

A Fabric mod that fells whole trees (and optionally giant fungi) when you break
one log, instead of leaving the rest of the tree floating in the air.

Inspired by the classic Vanilla Tweaks Timber datapack.

## Commands

Requires operator level 2.

- `/timber toggle` toggles Timber felling for yourself.
- `/timber config` (or `/timber config get`) prints the current config.
- `/timber config set <key> <value>` changes a setting.
- `/timber settings` (or `/timber settings 1` / `2`) opens a clickable chat
  menu that mirrors the original datapack's tellraw settings menu. Click a
  setting to toggle it; use the `<` / `>` arrows to switch pages. This works on
  dedicated servers too, not just in singleplayer.

## Configuration

The config is stored in `config/timber.json` and defaults to the original
datapack settings. You can change values in-game with `/timber config set` or
`/timber settings`, edit the JSON file directly, or in singleplayer open the
Mods screen (Mod Menu) and pick Timber for a GUI. When Cloth Config is
installed the GUI uses the standard Cloth Config screen; without it a simple
built-in screen is used instead.

| Key | Default | Meaning |
| --- | --- | --- |
| `standing` | true | Chop while not sneaking. |
| `sneaking` | false | Also chop while sneaking. |
| `chop_trees` | true | Fell trees (logs and leaves). |
| `chop_fungi` | false | Fell giant fungi (stems and caps). |
| `chop_down` | false | Also search one block below each log. |
| `destroy_leaves` | true | Destroy the attached leaves. |
| `plant_sapling` | false | Auto-replant a dropped sapling after a chop. |
| `slow_chop` | false | Break the tree gradually over several ticks instead of instantly. |
| `stop_chopping` | true | Stop mid-tree if the axe would break. |
| `wear_out` | true | Axes lose one durability point per chopped log. |
| `drop_loot` | false | False drops items at the block, true gives them to your inventory. |
| `hunger` | false | Apply hunger scaled to the tree size. |
| `stop_sound` | false | Silence the multi-block break noise. |
| `persistent` | false | Also chop persistent (placed) leaves. |
| `wooden_axe` | true | Wooden axes may fell trees. |
| `stone_axe` | true | Stone axes may fell trees. |
| `copper_axe` | true | Copper axes may fell trees. |
| `iron_axe` | true | Iron axes may fell trees. |
| `golden_axe` | true | Golden axes may fell trees. |
| `diamond_axe` | true | Diamond axes may fell trees. |
| `netherite_axe` | true | Netherite axes may fell trees. |
| `blocks_per_chop` | 1 | Blocks destroyed per slow-chop tick. |
| `time_between_chops` | 1 | Ticks between slow-chop steps. |
| `max_tree_size` | 555 | Maximum number of logs counted as one tree. |
| `min_leaves_found` | 5 | Minimum leaves attached before a tree is felled. |

## Details

- Runs on the server. The integrated server in singleplayer also works.
- Clients do not need the mod installed.
- The Mod Menu config screen is optional and client-side only (needs Mod Menu installed in singleplayer). Cloth Config is optional and gives the nicer Cloth Config GUI.
- Axe durability respects the Unbreaking enchantment.
- Slow chop and auto-replant run per server tick.

## Requirements

- Fabric Loader 0.19.3 or newer for Minecraft 26.1.2.
- Fabric API for 26.1.2.
- Java 25.
- Optional: Mod Menu and Cloth Config (for the in-game GUI).

## Building

JDK 25 and Gradle 9.7 or newer (Loom 1.17.19).

```bash
./gradlew build
```

The jar is in `build/libs/`.

## License

GPL-3.0-or-later
