package dev.claywars;

import dev.claywars.entity.MiniCreeperEntity;
import dev.claywars.entity.SoldierEntity;
import dev.claywars.registry.ModBlocks;
import dev.claywars.registry.ModEntities;
import dev.claywars.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(ClayWars.MODID)
public class ClayWars {
    public static final String MODID = "clay_wars";

    public ClayWars(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTab);
    }

    private void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.RED_SOLDIER.get(), SoldierEntity.createAttributes().build());
        e.put(ModEntities.BLUE_SOLDIER.get(), SoldierEntity.createAttributes().build());
        e.put(ModEntities.MINI_CREEPER.get(), MiniCreeperEntity.createAttributes().build());
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(ModItems.RADAR.get());
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(ModItems.RED_SPAWN_EGG.get());
            e.accept(ModItems.BLUE_SPAWN_EGG.get());
            e.accept(ModItems.MINI_CREEPER_SPAWN_EGG.get());
        }
    }
}
