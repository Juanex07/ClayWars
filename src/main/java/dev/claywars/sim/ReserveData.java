package dev.claywars.sim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** "Bloque grande, recurso pequeño": cada bloque tiene reservas ocultas que los soldados van extrayendo de a poco. */
public class ReserveData extends SavedData {
    private final Map<Long, Integer> reserves = new HashMap<>();

    public static ReserveData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ReserveData::new, ReserveData::load, null), "clay_wars_reserves");
    }

    /** Extrae 1 unidad. Devuelve lo que queda (0 = el bloque se agotó). */
    public int extract(BlockPos pos, int initial) {
        long k = pos.asLong();
        int left = reserves.getOrDefault(k, initial) - 1;
        if (left <= 0) reserves.remove(k); else reserves.put(k, left);
        setDirty();
        return Math.max(left, 0);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        reserves.forEach((k, v) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("p", k); e.putInt("n", v);
            list.add(e);
        });
        tag.put("reserves", list);
        return tag;
    }

    private static ReserveData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReserveData d = new ReserveData();
        for (Tag t : tag.getList("reserves", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            d.reserves.put(e.getLong("p"), e.getInt("n"));
        }
        return d;
    }
}
