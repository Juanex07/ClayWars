package dev.claywars.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Radar de reinos: dos filas de botones (reino / sección), barras para recursos y población, y tabla de soldados. */
public class RadarScreen extends Screen {
    private static final int W = 360, BTN_W = 116, GAP = 6, LINE_H = 13;
    private static final int[] COLS = {0, 92, 142, 206, 246, 290};      // columnas de la tabla de soldados

    private final Map<String, List<String[]>> data = new HashMap<>();
    private String team = "RED";
    private String tab = "RES";
    private int scroll = 0;

    public RadarScreen(String text) {
        super(Component.literal("Radar de reinos"));
        List<String[]> current = null;
        for (String line : text.split("\n")) {
            if (line.startsWith("#")) {
                current = new ArrayList<>();
                data.put(line.substring(1), current);
            } else if (current != null && !line.isEmpty()) {
                current.add(line.split("\\|", -1));
            }
        }
    }

    private int left() { return (width - W) / 2; }

    @Override
    protected void init() {
        int x = left();
        addRenderableWidget(Button.builder(Component.literal("Reino rojo"), b -> select("RED", tab.equals("HIST") ? "RES" : tab)).bounds(x, 12, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reino azul"), b -> select("BLUE", tab.equals("HIST") ? "RES" : tab)).bounds(x + BTN_W + GAP, 12, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Historial"), b -> select(team, "HIST")).bounds(x + 2 * (BTN_W + GAP), 12, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Recursos"), b -> select(team, "RES")).bounds(x, 38, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Población"), b -> select(team, "POP")).bounds(x + BTN_W + GAP, 38, BTN_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Soldados"), b -> select(team, "LIST")).bounds(x + 2 * (BTN_W + GAP), 38, BTN_W, 20).build());
    }

    private void select(String newTeam, String newTab) {
        team = newTeam;
        tab = newTab;
        scroll = 0;
    }

    private List<String[]> currentRows() {
        String key = tab.equals("HIST") ? "HIST" : team + "|" + tab;
        return data.getOrDefault(key, List.of(new String[]{"txt", "Sin datos."}));
    }

    private int teamColor() { return team.equals("RED") ? 0xFFFF6B6B : 0xFF6B8CFF; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x1 = left(), x2 = x1 + W, y1 = 66, y2 = height - 10;

        // marca de la selección actual
        boolean hist = tab.equals("HIST");
        int teamIdx = hist ? 2 : (team.equals("RED") ? 0 : 1);
        g.fill(x1 + teamIdx * (BTN_W + GAP), 33, x1 + teamIdx * (BTN_W + GAP) + BTN_W, 35, hist ? 0xFFFFFFFF : teamColor());
        int tabIdx = switch (tab) { case "RES" -> 0; case "POP" -> 1; case "LIST" -> 2; default -> -1; };
        if (tabIdx >= 0) g.fill(x1 + tabIdx * (BTN_W + GAP), 59, x1 + tabIdx * (BTN_W + GAP) + BTN_W, 61, 0xFFFFFFFF);

        g.fill(x1 - 2, y1 - 2, x2 + 2, y2 + 2, 0xFF2A2F36);
        g.fill(x1, y1, x2, y2, 0xE0101418);

        String title = hist ? "Historial de los reinos"
                : (team.equals("RED") ? "REINO ROJO" : "REINO AZUL") + "  -  " + switch (tab) {
                    case "POP" -> "Población";
                    case "LIST" -> "Soldados";
                    default -> "Recursos";
                };
        g.drawString(font, title, x1 + 8, y1 + 6, hist ? 0xFFFFFFFF : teamColor());
        g.fill(x1 + 6, y1 + 18, x2 - 6, y1 + 19, 0x55FFFFFF);

        List<String[]> rows = currentRows();
        int top = y1 + 24;
        int visible = Math.max(1, (y2 - top - 14) / LINE_H);
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - visible));
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            int y = top + i * LINE_H;
            String[] r = rows.get(i + scroll);
            switch (r[0]) {
                case "bar" -> drawBar(g, x1 + 8, x2 - 8, y, r);
                case "head" -> drawTable(g, x1 + 8, y, r, 0xFFAAB4C0, false);
                case "row" -> {
                    if ((i + scroll) % 2 == 0) g.fill(x1 + 4, y - 2, x2 - 4, y + LINE_H - 2, 0x20FFFFFF);
                    drawTable(g, x1 + 8, y, r, 0xFFE6E6E6, true);
                }
                default -> g.drawString(font, font.plainSubstrByWidth(r.length > 1 ? r[1] : "", W - 16), x1 + 8, y, 0xFFDDDDDD);
            }
        }
        if (rows.size() > visible)
            g.drawString(font, "Rueda del ratón: desplazar", x1 + 8, y2 - 11, 0xFF808080);
    }

    private void drawBar(GuiGraphics g, int x, int xEnd, int y, String[] r) {
        String label = r[1];
        int value = parse(r[2]), max = Math.max(1, parse(r[3]));
        g.drawString(font, font.plainSubstrByWidth(label, 118), x, y, 0xFFE6E6E6);
        int bx = x + 124, bw = xEnd - bx - 56;
        g.fill(bx, y + 1, bx + bw, y + 9, 0xFF2B323A);
        int fill = Mth.clamp(bw * value / max, 0, bw);
        g.fill(bx, y + 1, bx + fill, y + 9, barColor(label, value, max));
        g.drawString(font, value + " / " + max, bx + bw + 6, y, 0xFFFFFFFF);
    }

    private int barColor(String label, int value, int max) {
        if (label.startsWith("Comida") && value * 4 < max) return 0xFFE05A4F;        // poca comida: rojo
        if (label.startsWith("Madera")) return 0xFFB07A3C;
        if (label.startsWith("Comida") || label.startsWith("Parcelas")) return 0xFF63B85A;
        if (label.startsWith("Semillas")) return 0xFFD8C94A;
        if (label.startsWith("Población")) return 0xFF5DADE2;
        if (label.startsWith("Casas")) return 0xFF9AA0A6;
        return 0xFF8FA3B8;
    }

    private void drawTable(GuiGraphics g, int x, int y, String[] r, int color, boolean data) {
        for (int c = 1; c < r.length && c - 1 < COLS.length; c++) {
            int col = color;
            if (data && c == 5 && parse(r[c]) < 6) col = 0xFFFF7777;                 // hambre baja: rojo
            int maxW = (c - 1 + 1 < COLS.length ? COLS[c] - COLS[c - 1] : 70) - 4;
            g.drawString(font, font.plainSubstrByWidth(r[c], maxW), x + COLS[c - 1], y, col);
        }
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 2;
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
