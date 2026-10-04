package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Colony;
import dev.claywars.sim.Resource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/** Si tiene hambre y hay comida en el almacén, va al hogar y se queda comiendo (con partículas y sonido). */
public class EatGoal extends Goal {
    private static final int EAT_TICKS = 40;

    private final SoldierEntity mob;
    private boolean done;
    private int eatTimer;

    public EatGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (mob.hunger() > 10f) return false;
        Colony c = mob.colony();
        return c.home() != null && c.get(Resource.FOOD) > 0;
    }

    @Override public void start() { done = false; eatTimer = 0; }
    @Override public boolean canContinueToUse() { return !done && mob.hunger() < 18f; }

    @Override public void tick() {
        BlockPos home = mob.colony().home();
        if (home == null) { done = true; return; }
        if (mob.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 6.0) {
            eatTimer = 0;
            if (mob.tickCount % 10 == 0)
                mob.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.4);
            return;
        }
        mob.getNavigation().stop();
        eatTimer++;
        if (eatTimer % 8 == 1) {
            ServerLevel level = (ServerLevel) mob.level();
            mob.swing(InteractionHand.MAIN_HAND);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.APPLE)),
                    mob.getX(), mob.getY() + 0.3, mob.getZ(), 4, 0.05, 0.05, 0.05, 0.03);
            level.playSound(null, mob.blockPosition(), SoundEvents.FOX_EAT, SoundSource.NEUTRAL, 0.5f, 1.8f);
        }
        if (eatTimer >= EAT_TICKS) {
            eatTimer = 0;
            if (mob.colony().take(Resource.FOOD, 1)) mob.eat(6f); else done = true;
        }
    }
}
