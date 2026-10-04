package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/** Pasea sin alejarse: los bebés cerca de su casa, los demás cerca del hogar. */
public class StrollNearHomeGoal extends Goal {
    private final SoldierEntity mob;
    private double tx, ty, tz;

    public StrollNearHomeGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        BlockPos a = mob.anchor();
        if (a == null || mob.getRandom().nextInt(30) != 0) return false;
        int radius = mob.canWork() ? 8 : 3;
        int x = a.getX() + mob.getRandom().nextInt(radius * 2 + 1) - radius;
        int z = a.getZ() + mob.getRandom().nextInt(radius * 2 + 1) - radius;
        int y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        tx = x + 0.5; ty = y; tz = z + 0.5;
        return true;
    }

    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
    @Override public void start() { mob.getNavigation().moveTo(tx, ty, tz, 0.7); }
    @Override public void stop() { mob.getNavigation().stop(); }
}
