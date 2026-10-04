package dev.claywars.sim;

import dev.claywars.block.HouseBlock;
import dev.claywars.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Informe de texto para el radar. Cada sección empieza con "#CLAVE" y sus líneas tienen formato "tipo|datos":
 *   bar|Etiqueta|valor|máximo      txt|texto      head|col1|col2...      row|col1|col2...
 */
public class RadarReport {
    private static final int MAX_CHARS = 30000;

    public static String build(ServerLevel level) {
        StringBuilder sb = new StringBuilder();
        for (Team t : Team.values()) {
            Colony c = Colony.get(level, t);
            BlockPos home = c.home();
            List<SoldierEntity> list = new ArrayList<>();
            if (home != null)
                list.addAll(level.getEntitiesOfClass(SoldierEntity.class, new AABB(home).inflate(64), s -> s.team() == t && s.isAlive()));
            list.sort(Comparator.comparing(s -> s.getName().getString()));
            String k = t.name();

            sb.append('#').append(k).append("|RES\n");
            bar(sb, "Madera", c.get(Resource.WOOD), Resource.WOOD.targetStock);
            bar(sb, "Comida", c.get(Resource.FOOD), Resource.FOOD.targetStock);
            bar(sb, "Semillas", c.get(Resource.SEEDS), Resource.SEEDS.targetStock);
            bar(sb, "Población", list.size(), c.capacity());
            bar(sb, "Casas", c.houses(), 6);
            bar(sb, "Parcelas de cultivo", c.farmPlots().size(), 14);
            BlockPos site = c.site();
            if (site != null && level.getBlockState(site).getBlock() instanceof HouseBlock)
                sb.append("txt|Casa en obra: etapa ").append(level.getBlockState(site).getValue(HouseBlock.STAGE)).append(" de 3\n");
            else sb.append("txt|Casa en obra: ninguna\n");
            sb.append("txt|Hogar: ").append(home == null ? "sin fundar" : home.toShortString()).append('\n');

            int[] n = new int[LifeStage.values().length];
            int fem = 0;
            int[] roles = new int[Role.values().length];
            for (SoldierEntity s : list) {
                n[s.stage().ordinal()]++;
                if (s.isFemale()) fem++;
                if (s.role() != null) roles[s.role().ordinal()]++;
            }
            int total = Math.max(1, list.size());
            sb.append('#').append(k).append("|POP\n");
            bar(sb, "Bebés", n[0], total);
            bar(sb, "Jóvenes", n[1], total);
            bar(sb, "Adultos", n[2], total);
            bar(sb, "Viejos", n[3], total);
            sb.append("txt|\n");
            sb.append("txt|Mujeres: ").append(fem).append("     Hombres: ").append(list.size() - fem).append('\n');
            sb.append("txt|\n");
            for (Role r : Role.values()) bar(sb, "Oficio: " + r.label(), roles[r.ordinal()], total);

            sb.append('#').append(k).append("|LIST\n");
            sb.append("head|Nombre|Edad|Oficio|Vida|Hambre|Posición\n");
            int shown = 0;
            for (SoldierEntity s : list) {
                if (shown++ >= 60) break;
                sb.append("row|").append(s.getName().getString()).append(s.isFemale() ? " (F)" : " (M)").append('|')
                  .append(s.stage().label()).append('|')
                  .append(s.role() == null ? "-" : s.role().label()).append('|')
                  .append((int) s.getHealth()).append('/').append((int) s.getMaxHealth()).append('|')
                  .append((int) s.hunger()).append('|')
                  .append(s.blockPosition().getX()).append(',').append(s.blockPosition().getY()).append(',').append(s.blockPosition().getZ()).append('\n');
            }
            if (list.isEmpty()) sb.append("txt|Nadie con vida en este reino.\n");
        }
        sb.append("#HIST\n");
        List<String> hist = Chronicle.get(level).last(40);
        for (int i = hist.size() - 1; i >= 0; i--) sb.append("txt|").append(hist.get(i)).append('\n');
        if (hist.isEmpty()) sb.append("txt|Aún no hay historia que contar.\n");

        String out = sb.toString();
        return out.length() > MAX_CHARS ? out.substring(0, MAX_CHARS) : out;
    }

    private static void bar(StringBuilder sb, String label, int value, int max) {
        sb.append("bar|").append(label).append('|').append(value).append('|').append(Math.max(1, max)).append('\n');
    }
}
