package net.fayber.timber;

/** Effects apply only to the synchronous update cascade of a validated Timber chop. */
public final class FellingEffectsScope implements AutoCloseable {
    public record Settings(FellingEffects mode, int particles) { }

    private static final ThreadLocal<FellingEffectsScope> CURRENT = new ThreadLocal<>();
    private final FellingEffectsScope previous;
    private final Object level;
    private Settings settings;

    private FellingEffectsScope(Object level) {
        this.level = level;
        this.previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static FellingEffectsScope open(Object level) {
        return new FellingEffectsScope(level);
    }

    public static void activate(Object level, FellingEffects mode, int particles) {
        FellingEffectsScope scope = CURRENT.get();
        if (scope != null && scope.level == level) scope.settings = new Settings(mode, particles);
    }

    public static Settings current(Object level) {
        FellingEffectsScope scope = CURRENT.get();
        return scope != null && scope.level == level ? scope.settings : null;
    }

    @Override
    public void close() {
        if (this.previous == null) CURRENT.remove();
        else CURRENT.set(this.previous);
    }
}
