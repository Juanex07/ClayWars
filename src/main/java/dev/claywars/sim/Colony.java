package dev.claywars.sim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Cerebro compartido de un equipo: inventario común, hogar y (más adelante) era y pizarra de necesidades. */
public class Colony extends SavedData {
    private final EnumMap<Resource, Integer> stock = new EnumMap<>(Resource.class);
    @Nullable private BlockPos home;
    private int houses;
    @Nullable private BlockPos site;
    private final List<BlockPos> houseSites = new ArrayList<>();
    private boolean genderToggle;
    private final List<BlockPos> farmPlots = new ArrayList<>();
    private int roleCounter;
    private boolean starterSeeds;
    private long busyUntil;   // no se guarda: evita que dos soldados construyan a la vez
    public static final int BASE_CAPACITY = 10;

    public static Colony get(ServerLevel level, Team team) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Colony::new, Colony::load, null), "clay_wars_colony_" + team.id());
    }

    public int get(Resource r) { return stock.getOrDefault(r, 0); }

    public void add(Resource r, int n) { stock.merge(r, n, Integer::sum); setDirty(); }

    public boolean take(Resource r, int n) {
        if (get(r) < n) return false;
        stock.put(r, get(r) - n);
        setDirty();
        return true;
    }

    @Nullable public BlockPos home() { return home; }

    /** Población máxima: base + 4 por cada casa terminada. */
    public int capacity() { return BASE_CAPACITY + houses * 4; }
    public int houses() { return houses; }
    public void addHouse() { houses++; setDirty(); }

    @Nullable public BlockPos site() { return site; }
    public void setSite(BlockPos pos) { site = pos.immutable(); houseSites.add(site); setDirty(); }
    public void clearSite() { site = null; setDirty(); }
    public List<BlockPos> houseSites() { return houseSites; }
    public long busyUntil() { return busyUntil; }
    public void setBusy(long until) { busyUntil = until; }

    /** Alterna mujer/hombre para que los primeros soldados siempre sean una pareja. */
    public boolean nextGenderFemale() { genderToggle = !genderToggle; setDirty(); return genderToggle; }

    public List<BlockPos> farmPlots() { return farmPlots; }
    public void addPlot(BlockPos pos) { farmPlots.add(pos.immutable()); setDirty(); }

    /** Reparte oficios en ciclo: granjero, leñador, constructor... */
    public Role nextRole() { Role r = Role.values()[roleCounter % Role.values().length]; roleCounter++; setDirty(); return r; }

    /** Semillas de inicio (una sola vez) para arrancar la agricultura. */
    public void grantStarterSeeds() {
        if (!starterSeeds) { starterSeeds = true; stock.merge(Resource.SEEDS, 6, Integer::sum); setDirty(); }
    }

    public void setHomeIfAbsent(BlockPos pos) {
        if (home == null) { home = pos.immutable(); setDirty(); }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Resource r : Resource.values()) tag.putInt(r.name(), get(r));
        if (home != null) tag.putLong("home", home.asLong());
        tag.putInt("houses", houses);
        if (site != null) tag.putLong("site", site.asLong());
        long[] arr = new long[houseSites.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = houseSites.get(i).asLong();
        tag.putLongArray("houseSites", arr);
        tag.putBoolean("genderToggle", genderToggle);
        tag.putInt("roleCounter", roleCounter);
        tag.putBoolean("starterSeeds", starterSeeds);
        long[] plots = new long[farmPlots.size()];
        for (int i = 0; i < plots.length; i++) plots[i] = farmPlots.get(i).asLong();
        tag.putLongArray("farmPlots", plots);
        return tag;
    }

    private static Colony load(CompoundTag tag, HolderLookup.Provider registries) {
        Colony c = new Colony();
        for (Resource r : Resource.values()) c.stock.put(r, tag.getInt(r.name()));
        if (tag.contains("home")) c.home = BlockPos.of(tag.getLong("home"));
        c.houses = tag.getInt("houses");
        if (tag.contains("site")) c.site = BlockPos.of(tag.getLong("site"));
        for (long l : tag.getLongArray("houseSites")) c.houseSites.add(BlockPos.of(l));
        c.genderToggle = tag.getBoolean("genderToggle");
        c.roleCounter = tag.getInt("roleCounter");
        c.starterSeeds = tag.getBoolean("starterSeeds");
        for (long l : tag.getLongArray("farmPlots")) c.farmPlots.add(BlockPos.of(l));
        return c;
    }
}
