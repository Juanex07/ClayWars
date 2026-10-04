package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Resource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Lleva lo recolectado al almacén de la colonia. */
public class DeliverGoal extends Goal {
    private final SoldierEntity mob;
    private final boolean onlyWhenFull;

    public DeliverGoal(SoldierEntity mob, boolean onlyWhenFull) {
        this.mob = mob;
        this.onlyWhenFull = onlyWhenFull;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (mob.carriedAmount() == 0 || mob.colony().home() == null) return false;
        return !onlyWhenFull || mob.carriedAmount() >= SoldierEntity.CARRY_CAPACITY;
    }

    @Override public boolean canContinueToUse() { return mob.carriedAmount() > 0; }

    @Override public void tick() {
        BlockPos home = mob.colony().home();
        if (home == null) return;
        if (mob.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 6.0) {
            if (mob.tickCount % 10 == 0)
                mob.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, mob.workSpeed());
        } else {
            Resource r = mob.carried();
            if (r != null) mob.colony().add(r, mob.carriedAmount());
            mob.clearCarried();
        }
    }
}
