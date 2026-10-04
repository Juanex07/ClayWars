package dev.claywars.registry;

import dev.claywars.ClayWars;
import dev.claywars.item.RadarItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ClayWars.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> RED_SPAWN_EGG = ITEMS.register("red_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RED_SOLDIER, 0xC62828, 0xEFD9A8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLUE_SPAWN_EGG = ITEMS.register("blue_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLUE_SOLDIER, 0x1565C0, 0xEFD9A8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MINI_CREEPER_SPAWN_EGG = ITEMS.register("mini_creeper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MINI_CREEPER, 0x0DA70B, 0x000000, new Item.Properties()));

    public static final DeferredItem<RadarItem> RADAR = ITEMS.register("radar",
            () -> new RadarItem(new Item.Properties().stacksTo(1)));
}
