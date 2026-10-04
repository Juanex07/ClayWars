package dev.claywars.sim;

/** Etapas de vida. Los tiempos están en ticks (20 ticks = 1 segundo). Bájalos para probar más rápido. */
public enum LifeStage {
    BABY("bebé", 0.55f),
    YOUTH("joven", 0.8f),
    ADULT("adulto", 1.0f),
    ELDER("viejo", 0.95f);

    public static final int BABY_END = 4800;     // 4 min
    public static final int YOUTH_END = 9600;    // 8 min
    public static final int ADULT_END = 48000;   // 40 min
    public static final int MAX_AGE = 60000;     // 50 min: muere de viejo

    private final String label;
    private final float renderScale;

    LifeStage(String label, float renderScale) { this.label = label; this.renderScale = renderScale; }

    public String label() { return label; }
    public float renderScale() { return renderScale; }

    public static LifeStage fromAge(int ticks) {
        if (ticks < BABY_END) return BABY;
        if (ticks < YOUTH_END) return YOUTH;
        if (ticks < ADULT_END) return ADULT;
        return ELDER;
    }
}
