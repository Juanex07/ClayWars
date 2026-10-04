package dev.claywars.registry;

import dev.claywars.ClayWars;
import dev.claywars.block.HouseBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ClayWars.MODID);

    public static final DeferredBlock<HouseBlock> HOUSE = BLOCKS.registerBlock("house", HouseBlock::new,
            BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().sound(SoundType.WOOD));
}
