package net.fayber.timber;

import java.util.Locale;

public enum FellingEffects {
    UNSUPPRESSED("Full"), MINIMIZED("Less"), SUPPRESSED("None");

    private final String label;

    FellingEffects(String label) { this.label = label; }

    public String label() { return label; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public FellingEffects next() { return values()[(ordinal() + 1) % values().length]; }

    public static int particleCount(int count) {
        if (count < 1 || count > 64) throw new IllegalArgumentException("Particle count must be between 1 and 64");
        return count;
    }

    public static FellingEffects parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static FellingEffects resolve(String mode, Boolean legacySuppression) {
        if (mode != null) return parse(mode);
        if (legacySuppression != null) return legacySuppression ? SUPPRESSED : UNSUPPRESSED;
        return MINIMIZED;
    }
}
