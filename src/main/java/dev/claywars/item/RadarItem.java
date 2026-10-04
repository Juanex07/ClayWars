package dev.claywars.item;

import dev.claywars.net.RadarPayload;
import dev.claywars.sim.RadarReport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Al usarlo (clic derecho) abre la pantalla con el estado de los dos reinos. */
public class RadarItem extends Item {
    public RadarItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new RadarPayload(RadarReport.build((ServerLevel) level)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
