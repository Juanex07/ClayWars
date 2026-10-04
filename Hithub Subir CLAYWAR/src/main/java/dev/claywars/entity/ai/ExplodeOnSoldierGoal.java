package dev.claywars.entity.ai;

import dev.claywars.entity.MiniCreeperEntity;
import dev.claywars.entity.SoldierEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Se acerca al objetivo, enciende la mecha y explota con daño en un radio pequeño, solo a soldaditos. */
public class ExplodeOnSoldierGoal extends Goal {
    private static final int FUSE_TICKS = 20;
    private static final double TRIGGER_DIST = 0.9;
    private static final double BLAST_RADIUS = 1.5;
    private static final float MAX_DAMAGE = 7f;

    private final MiniCreeperEntity mob;
    private int fuse = -1;

    public ExplodeOnSoldierGoal(MiniCreeperEntity mob) { this.mob = mob; }

    @Override public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { fuse = -1; }
    @Override public void stop() { fuse = -1; mob.getNavigation().stop(); }

    @Override public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30f, 30f);
        double distSqr = mob.distanceToSqr(target);

        if (fuse < 0) {
            if (distSqr > TRIGGER_DIST * TRIGGER_DIST) {
                if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(target, 1.4);
            } else {
                fuse = FUSE_TICKS;
                mob.getNavigation().stop();
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.6f, 1.6f);
            }
        } else if (distSqr > (TRIGGER_DIST * 3) * (TRIGGER_DIST * 3)) {
            fuse = -1;                    // el objetivo escapó: se apaga la mecha
        } else if (--fuse <= 0) {
            explode();
        }
    }

    private void explode() {
        ServerLevel level = (ServerLevel) mob.level();
        for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, mob.getBoundingBox().inflate(BLAST_RADIUS))) {
            double d = s.distanceTo(mob);
            if (d <= BLAST_RADIUS) {
                float dmg = (float) (MAX_DAMAGE * (1.0 - d / (BLAST_RADIUS * 1.5)));
                s.hurt(level.damageSources().mobAttack(mob), dmg);
            }
        }
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.2, mob.getZ(), 12, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.2, mob.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.8f, 0.7f);
        mob.discard();
    }
}
