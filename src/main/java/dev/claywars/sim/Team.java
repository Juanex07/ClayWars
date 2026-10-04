package dev.claywars.sim;

public enum Team {
    RED, BLUE;
    public String id() { return name().toLowerCase(); }
    public String label() { return this == RED ? "ROJO" : "AZUL"; }
}
