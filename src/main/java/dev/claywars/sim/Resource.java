package dev.claywars.sim;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/** Recursos de la colonia. Las eras futuras añadirán STONE, IRON, etc. */
public enum Resource {
    WOOD(64),
    FOOD(30),
    SEEDS(24);

    /** Cuánto stock quiere la colonia antes de dejar de recolectarlo. */
    public final int targetStock;

    Resource(int targetStock) { this.targetStock = targetStock; }

    @Nullable
    public static Resource fromStack(ItemStack s) {
        if (s.has(DataComponents.FOOD)) return FOOD;
        if (s.is(ItemTags.LOGS)) return WOOD;
        if (s.is(Items.WHEAT_SEEDS)) return SEEDS;
        return null;
    }
}
