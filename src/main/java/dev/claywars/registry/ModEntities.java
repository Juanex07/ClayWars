package dev.claywars.registry;

import dev.claywars.ClayWars;
import dev.claywars.entity.MiniCreeperEntity;
import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Team;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ClayWars.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> RED_SOLDIER = soldier("red_soldier", Team.RED);
    public static final DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> BLUE_SOLDIER = soldier("blue_soldier", Team.BLUE);

    public static final DeferredHolder<EntityType<?>, EntityType<MiniCreeperEntity>> MINI_CREEPER = ENTITIES.register("mini_creeper",
            () -> EntityType.Builder.<MiniCreeperEntity>of(MiniCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.2f, 0.35f).clientTrackingRange(8).build(ClayWars.MODID + ":mini_creeper"));

    private static DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> soldier(String name, Team team) {
        return ENTITIES.register(name, () -> EntityType.Builder
                .<SoldierEntity>of((type, level) -> new SoldierEntity(type, level, team), MobCategory.CREATURE)
                .sized(0.2f, 0.4f)
                .clientTrackingRange(8)
                .build(ClayWars.MODID + ":" + name));
    }
}
