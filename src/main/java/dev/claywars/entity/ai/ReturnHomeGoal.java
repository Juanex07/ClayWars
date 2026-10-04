package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Si se alejó más de su radio permitido (los bebés, muy poco), regresa al hogar o a su casa. */
public class ReturnHomeGoal extends Goal {
    private final SoldierEntity mob;
    private int repath;

    public ReturnHomeGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private double distSqrToAnchor() {
        BlockPos a = mob.anchor();
        return a == null ? 0 : mob.distanceToSqr(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
    }

    @Override public boolean canUse() {
        if (mob.tickCount % 10 != 0 || mob.anchor() == null) return false;
        double r = mob.leashRadius();
        return distSqrToAnchor() > r * r;
    }

    @Override public boolean canContinueToUse() {
        double r = mob.leashRadius() * 0.4;
        return mob.anchor() != null && distSqrToAnchor() > r * r;
    }

    @Override public void start() { repath = 0; }
    @Override public void stop() { mob.getNavigation().stop(); }

    @Override public void tick() {
        BlockPos a = mob.anchor();
        if (a == null) return;
        if (--repath <= 0) {
            repath = 10;
            mob.getNavigation().moveTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, mob.workSpeed());
        }
    }
}
