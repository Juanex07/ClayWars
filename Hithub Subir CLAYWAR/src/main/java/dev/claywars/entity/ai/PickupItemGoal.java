package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Colony;
import dev.claywars.sim.Resource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/** Recoge comida, troncos y semillas que el jugador suelta dentro de la zona del reino y los guarda en el almacén. */
public class PickupItemGoal extends Goal {
    private final SoldierEntity mob;
    @Nullable private ItemEntity target;
    private int nextScan, repath, runTicks;

    public PickupItemGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (!mob.canWork() || mob.tickCount < nextScan) return false;
        nextScan = mob.tickCount + 40;
        Colony c = mob.colony();
        if (c.home() == null) return false;
        double r = mob.leashRadius();
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, new AABB(c.home()).inflate(r),
                e -> e.isAlive() && Resource.fromStack(e.getItem()) != null);
        double best = Double.MAX_VALUE;
        target = null;
        for (ItemEntity e : items) {
            double d = mob.distanceToSqr(e);
            if (d < best) { best = d; target = e; }
        }
        return target != null;
    }

    @Override public boolean canContinueToUse() { return target != null && target.isAlive() && runTicks < 400; }
    @Override public void start() { repath = 0; runTicks = 0; }
    @Override public void stop() { target = null; mob.getNavigation().stop(); nextScan = mob.tickCount; }

    @Override public void tick() {
        if (target == null) return;
        runTicks++;
        if (mob.distanceToSqr(target) > 1.2) {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), mob.workSpeed());
            }
            return;
        }
        Resource r = Resource.fromStack(target.getItem());
        if (r != null) {
            mob.colony().add(r, target.getItem().getCount());
            ((ServerLevel) mob.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + 0.4, mob.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
        }
        target.discard();
        target = null;
    }
}
