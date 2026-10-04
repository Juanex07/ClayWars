package dev.claywars.client;

import dev.claywars.ClayWars;
import dev.claywars.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ClayWars.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClayWarsClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.RED_SOLDIER.get(), SoldierRenderer::new);
        e.registerEntityRenderer(ModEntities.BLUE_SOLDIER.get(), SoldierRenderer::new);
        e.registerEntityRenderer(ModEntities.MINI_CREEPER.get(), MiniCreeperRenderer::new);
    }
}
