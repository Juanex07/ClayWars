package dev.claywars.sim;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Crónica de los reinos: nacimientos, muertes y eventos. Guarda solo las últimas 300 entradas. */
public class Chronicle extends SavedData {
    private static final int MAX_ENTRIES = 300;
    private final List<String> entries = new ArrayList<>();

    public static Chronicle get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Chronicle::new, Chronicle::load, null), "clay_wars_chronicle");
    }

    public static void add(ServerLevel level, Team team, String text) {
        Chronicle c = get(level);
        long day = level.getServer().overworld().getDayTime() / 24000L;
        c.entries.add("Día " + day + " [" + team.label() + "] " + text);
        while (c.entries.size() > MAX_ENTRIES) c.entries.remove(0);
        c.setDirty();
    }

    public List<String> last(int n) {
        return new ArrayList<>(entries.subList(Math.max(0, entries.size() - n), entries.size()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (String s : entries) list.add(StringTag.valueOf(s));
        tag.put("entries", list);
        return tag;
    }

    private static Chronicle load(CompoundTag tag, HolderLookup.Provider registries) {
        Chronicle c = new Chronicle();
        ListTag list = tag.getList("entries", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) c.entries.add(list.getString(i));
        return c;
    }
}
