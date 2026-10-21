package net.fayber.timber;

public final class FellingEffectsTest {
    @org.junit.Test
    public void cyclesAndMigratesModes() {
        check(FellingEffects.UNSUPPRESSED.next() == FellingEffects.MINIMIZED);
        check(FellingEffects.MINIMIZED.next() == FellingEffects.SUPPRESSED);
        check(FellingEffects.SUPPRESSED.next() == FellingEffects.UNSUPPRESSED);
        check(FellingEffects.resolve(null, null) == FellingEffects.MINIMIZED);
        check(FellingEffects.resolve(null, true) == FellingEffects.SUPPRESSED);
        check(FellingEffects.resolve(null, false) == FellingEffects.UNSUPPRESSED);
        check(FellingEffects.resolve("minimized", true) == FellingEffects.MINIMIZED);
        check(FellingEffects.resolve("UNSUPPRESSED", null) == FellingEffects.UNSUPPRESSED);
        try {
            FellingEffects.parse("typo");
            throw new AssertionError("Invalid modes must be rejected");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Felling effect cycle, defaults, migration and validation passed");
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }
}
