package dev.claywars.sim;

/** Oficios. Cada soldado prefiere las tareas de su oficio, pero ayuda con otras si el reino lo necesita con urgencia. */
public enum Role {
    FARMER("granjero"),
    LUMBERJACK("leñador"),
    BUILDER("constructor");

    private final String label;
    Role(String label) { this.label = label; }
    public String label() { return label; }
}
