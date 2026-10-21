# Timber verification

Run `./gradlew build` for the unit tests and production jar.
Run `./gradlew runGameTest` for the real 26.3 dedicated-server regression.
The game test uses a disposable world and a fake player's captured outbound packets.

The vine regression checks all three effect modes through a real player block break:

- The origin log, a Timber-removed log, and their hanging vine chain lose support normally.
- A vine with a second stone support survives with that face retained.
- Unsuppressed sends vanilla vine break events, including their sound, exactly once per vine.
- Minimized sends one reduced particle packet per vine with the configured count.
- Suppressed sends neither full nor reduced vine effects.
- Log and vine loot remains vanilla, and ordinary vine breaks after the chop still emit full effects.
- Queued slow chopping uses the same settings, including a different particle count.

Unit tests check mode migration, count bounds, inactive scopes, level isolation,
nested scope restoration, and cleanup after an exception.
