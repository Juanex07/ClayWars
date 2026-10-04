package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** Un adulto listo para tener hijos busca a su pareja y ambos se juntan antes de que nazca el bebé. */
public class SeekMateGoal extends Goal {
    private final SoldierEntity mob;
    @Nullable private SoldierEntity mate;
    private int nextCheck, repath, runTicks;

    public SeekMateGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (mob.tickCount < nextCheck) return false;
        nextCheck = mob.tickCount + 40 + mob.getRandom().nextInt(40);
        if (!mob.isReadyToBreed() || !mob.colonyHasRoom()) return false;
        mate = mob.findMate(24.0);
        return mate != null;
    }

    @Override public boolean canContinueToUse() {
        return mate != null && mate.isAlive() && mob.isReadyToBreed() && mate.isReadyToBreed() && runTicks < 500;
    }

    @Override public void start() { repath = 0; runTicks = 0; }
    @Override public void stop() { mate = null; mob.getNavigation().stop(); }

    @Override public void tick() {
        if (mate == null) return;
        runTicks++;
        mob.getLookControl().setLookAt(mate, 30f, 30f);
        if (mob.distanceTo(mate) > 2.0f) {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(mate, mob.workSpeed());
            }
        } else {
            mob.getNavigation().stop();
            mob.tryBreedNow();
        }
    }
}
