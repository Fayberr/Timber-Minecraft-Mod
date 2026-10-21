package net.fayber.timber;

import org.junit.Test;
import static org.junit.Assert.*;

public class FellingEffectsScopeTest {
    @Test public void inactiveMiningDoesNotSuppressAndLevelsAreIsolated() {
        Object level = new Object();
        try (var ignored = FellingEffectsScope.open(level)) {
            assertNull(FellingEffectsScope.current(level));
            FellingEffectsScope.activate(level, FellingEffects.MINIMIZED, 5);
            assertEquals(5, FellingEffectsScope.current(level).particles());
            assertNull(FellingEffectsScope.current(new Object()));
        }
        assertNull(FellingEffectsScope.current(level));
    }

    @Test public void nestedBlocksRestoreOuterOriginAndExceptionsClearState() {
        Object level = new Object();
        try {
            try (var ignored = FellingEffectsScope.open(level)) {
                FellingEffectsScope.activate(level, FellingEffects.SUPPRESSED, 2);
                try (var nested = FellingEffectsScope.open(level)) {
                    FellingEffectsScope.activate(level, FellingEffects.MINIMIZED, 7);
                    assertEquals(FellingEffects.MINIMIZED, FellingEffectsScope.current(level).mode());
                }
                assertEquals(FellingEffects.SUPPRESSED, FellingEffectsScope.current(level).mode());
                throw new IllegalStateException("simulated failure");
            }
        } catch (IllegalStateException expected) { }
        assertNull(FellingEffectsScope.current(level));
    }
}
