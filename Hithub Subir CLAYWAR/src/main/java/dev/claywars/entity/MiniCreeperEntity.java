package dev.claywars.entity;

import dev.claywars.entity.ai.ExplodeOnSoldierGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;

/** Depredador natural: persigue SOLO a los soldaditos y explota sin romper bloques ni dañar al jugador. */
public class MiniCreeperEntity extends PathfinderMob {
    public MiniCreeperEntity(EntityType<? extends MiniCreeperEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, SoldierEntity.BASE_SPEED)
                .add(Attributes.FOLLOW_RANGE, 10.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ExplodeOnSoldierGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Solo ataca soldaditos: el jugador es un gigante que ignora por completo.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    protected void updateWalkAnimation(float distance) {
        float target = getPose() == Pose.STANDING ? Math.min(distance * 6.0f * SoldierEntity.ANIM_BOOST, 3.0f) : 0f;
        this.walkAnimation.update(target, 0.4f);
    }
}
