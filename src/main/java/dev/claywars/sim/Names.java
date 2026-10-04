package dev.claywars.sim;

import net.minecraft.util.RandomSource;

public class Names {
    private static final String[] NAMES = {
        "Tomás", "Lucía", "Pedro", "Marta", "Hugo", "Elena", "Iván", "Rosa", "Bruno", "Clara",
        "Mateo", "Sofía", "Diego", "Inés", "Pablo", "Julia", "Nico", "Alma", "Raúl", "Vera",
        "Leo", "Nora", "Saúl", "Ada", "Omar", "Lola", "Gael", "Irene", "Teo", "Mina",
        "Fermín", "Greta", "Lalo", "Paz", "Ciro", "Dora", "Beto", "Anita", "Rafa", "Cleo"
    };

    private static final String[] MALE = {
        "Tomás", "Pedro", "Hugo", "Iván", "Bruno", "Mateo", "Diego", "Pablo", "Nico", "Raúl",
        "Leo", "Saúl", "Omar", "Gael", "Teo", "Fermín", "Lalo", "Ciro", "Beto", "Rafa"
    };
    private static final String[] FEMALE = {
        "Lucía", "Marta", "Elena", "Rosa", "Clara", "Sofía", "Inés", "Julia", "Alma", "Vera",
        "Nora", "Ada", "Lola", "Irene", "Mina", "Greta", "Paz", "Dora", "Anita", "Cleo"
    };

    public static String random(RandomSource r) { return NAMES[r.nextInt(NAMES.length)]; }

    public static String random(RandomSource r, boolean female) {
        String[] list = female ? FEMALE : MALE;
        return list[r.nextInt(list.length)];
    }
}
