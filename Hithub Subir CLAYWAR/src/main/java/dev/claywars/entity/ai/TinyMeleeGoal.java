package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Combate cuerpo a cuerpo ajustado a criaturas diminutas: se acercan hasta casi tocarse y luego golpean. */
public class TinyMeleeGoal extends Goal {
    private static final double REACH = 0.4;      // distancia (en bloques) a la que pueden golpear
    private static final int COOLDOWN = 20;

    private final SoldierEntity mob;
    private int cooldown;
    private int repath;

    public TinyMeleeGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return mob.canFight() && t != null && t.isAlive();
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() {
        cooldown = 8 + mob.getRandom().nextInt(12);   // evita que siempre pegue primero el mismo
        repath = 0;
    }

    @Override public void stop() { mob.getNavigation().stop(); }

    @Override public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (cooldown > 0) cooldown--;

        double dist = Math.sqrt(mob.distanceToSqr(target));
        if (dist > REACH) {
            if (dist > 2.5) {
                if (--repath <= 0) {
                    repath = 10;
                    mob.getNavigation().moveTo(target, 1.5);
                }
            } else {
                // de cerca: caminar directo hacia el rival, sin depender del pathfinding
                mob.getNavigation().stop();
                mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.3);
            }
        } else {
            mob.getNavigation().stop();
            if (cooldown <= 0) {
                cooldown = COOLDOWN;
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
            }
        }
    }
}
