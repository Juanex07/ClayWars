package dev.claywars.entity;

import dev.claywars.block.HouseBlock;
import dev.claywars.entity.ai.BuildGoal;
import dev.claywars.entity.ai.FarmGoal;
import dev.claywars.entity.ai.PickupItemGoal;
import dev.claywars.entity.ai.ReturnHomeGoal;
import dev.claywars.entity.ai.SeekMateGoal;
import dev.claywars.entity.ai.StrollNearHomeGoal;
import dev.claywars.entity.ai.DeliverGoal;
import dev.claywars.entity.ai.EatGoal;
import dev.claywars.entity.ai.GatherGoal;
import dev.claywars.entity.ai.TinyMeleeGoal;
import dev.claywars.sim.Chronicle;
import dev.claywars.sim.Colony;
import dev.claywars.sim.LifeStage;
import dev.claywars.sim.Names;
import dev.claywars.sim.Resource;
import dev.claywars.sim.Role;
import dev.claywars.sim.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class SoldierEntity extends PathfinderMob {
    public static final float MAX_HUNGER = 20f;
    public static final int CARRY_CAPACITY = 4;
    public static final int BREED_COOLDOWN = 6000;
    public static final int BREED_FOOD_COST = 4;
    public static final int SWORD_WOOD_COST = 6;

    // ---- AJUSTES DE MOVIMIENTO (cámbialos aquí) ----
    /** Velocidad base. Original: 0.15. Más alto = más rápidos. */
    public static final double BASE_SPEED = 0.11;
    /** Cadencia de las piernas. Más alto = patitas más rápidas (sube si resbalan, baja si parecen ratas). */
    public static final float ANIM_BOOST = 1.0f;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FEMALE =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);

    private final Team team;
    private float hunger = MAX_HUNGER;
    @Nullable private Resource carried;
    private int carriedAmount;
    private int ageTicks = LifeStage.YOUTH_END;   // los de huevo nacen adultos (Adán y Eva)
    private int breedCooldown = 1200;
    private boolean agedOut;
    private boolean genderSet;
    @Nullable private Role role;

    public SoldierEntity(EntityType<? extends SoldierEntity> type, Level level, Team team) {
        super(type, level);
        this.team = team;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, LifeStage.ADULT.ordinal());
        builder.define(DATA_FEMALE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new AvoidEntityGoal<>(this, MiniCreeperEntity.class, 5.0f, 1.5, 1.9));
        // Huyen del enemigo si no pueden pelear (bebés, jóvenes, viejos) o si tienen poca vida
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, SoldierEntity.class, 6.0f, 1.5, 1.9,
                e -> e instanceof SoldierEntity s && s.team != team
                        && (!canFight() || getHealth() < getMaxHealth() * 0.3f)));
        goalSelector.addGoal(2, new TinyMeleeGoal(this));
        goalSelector.addGoal(3, new EatGoal(this));
        goalSelector.addGoal(4, new DeliverGoal(this, true));
        goalSelector.addGoal(5, new BuildGoal(this));
        goalSelector.addGoal(6, new SeekMateGoal(this));
        goalSelector.addGoal(7, new FarmGoal(this));
        goalSelector.addGoal(8, new PickupItemGoal(this));
        goalSelector.addGoal(9, new GatherGoal(this, Resource.FOOD));
        goalSelector.addGoal(10, new GatherGoal(this, Resource.WOOD));
        goalSelector.addGoal(11, new DeliverGoal(this, false));
        goalSelector.addGoal(12, new ReturnHomeGoal(this));
        goalSelector.addGoal(13, new StrollNearHomeGoal(this));
        goalSelector.addGoal(14, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    // ---------------------------------------------------------------- lógica por tick (servidor)

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) level();
        Colony colony = colony();
        colony.grantStarterSeeds();
        colony.setHomeIfAbsent(blockPosition()); // Adán y Eva: el primero que nace funda el hogar

        if (!genderSet) { genderSet = true; entityData.set(DATA_FEMALE, colony.nextGenderFemale()); }
        if (role == null && stage() != LifeStage.BABY) role = colony.nextRole();
        if (!hasCustomName()) setCustomName(Component.literal(Names.random(getRandom(), isFemale())));

        // Edad
        ageTicks++;
        LifeStage st = LifeStage.fromAge(ageTicks);
        if (st != stage()) {
            entityData.set(DATA_STAGE, st.ordinal());
            if (st == LifeStage.ADULT) Chronicle.add(server, team, getName().getString() + " creció y ya es adulto");
        }
        if (ageTicks >= LifeStage.MAX_AGE) {
            agedOut = true;
            hurt(damageSources().genericKill(), 1000f);
            return;
        }

        // Hambre
        hunger = Math.max(0f, hunger - 1f / 600f);
        if (hunger <= 0f && tickCount % 80 == 0) hurt(damageSources().starve(), 1f);
        if (hunger >= 14f && tickCount % 100 == 0 && getHealth() < getMaxHealth()) heal(1f);
        if (hunger < 6f && tickCount % 60 == 0)   // señal visible de que tiene hambre
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.5, getZ(), 1, 0.05, 0.05, 0.05, 0.0);

        if (breedCooldown > 0) breedCooldown--;

        // El jugador puede soltar comida/troncos cerca del hogar: la colonia los absorbe.
        if (tickCount % 20 == 0 && colony.home() != null) {
            for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(colony.home()).inflate(5))) {
                Resource r = Resource.fromStack(item.getItem());
                if (r != null) {
                    colony.add(r, item.getItem().getCount());
                    item.discard();
                }
            }
        }

        if (tickCount % 10 == 0) scanForEnemy();
        if (tickCount % 40 == 0) tryBreed(server, colony);
        if (tickCount % 200 == 0) tryEquipSword(colony);
    }

    private void scanForEnemy() {
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || distanceToSqr(t) > 256.0)) { setTarget(null); t = null; }
        if (t != null || !canFight()) return;
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        SoldierEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (SoldierEntity other : level().getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(range),
                e -> e.team != team && e.isAlive() && e.stage() != LifeStage.BABY)) {
            double d = distanceToSqr(other);
            if (d < bestDist) { bestDist = d; best = other; }
        }
        if (best != null) setTarget(best);
    }

    private void tryEquipSword(Colony colony) {
        if (canFight() && getMainHandItem().isEmpty() && colony.get(Resource.WOOD) >= SWORD_WOOD_COST) {
            colony.take(Resource.WOOD, SWORD_WOOD_COST);
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
            setDropChance(EquipmentSlot.MAINHAND, 0f);
        }
    }

    private void tryBreed(ServerLevel server, Colony colony) {
        if (stage() != LifeStage.ADULT || breedCooldown > 0 || hunger < 12f) return;
        if (colony.home() == null || colony.get(Resource.FOOD) < BREED_FOOD_COST) return;

        List<SoldierEntity> mates = server.getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(4.0),
                e -> e != this && e.team == team && e.stage() == LifeStage.ADULT
                        && e.breedCooldown <= 0 && e.hunger >= 12f && e.isAlive()
                        && e.isFemale() != isFemale());
        if (mates.isEmpty()) return;
        if (colonyPopulation(server, colony) >= colony.capacity()) return;

        SoldierEntity mate = mates.get(0);
        SoldierEntity baby = (SoldierEntity) getType().create(server);
        if (baby == null) return;
        baby.moveTo(getX(), getY(), getZ(), getRandom().nextFloat() * 360f, 0f);
        baby.ageTicks = 0;
        baby.entityData.set(DATA_STAGE, LifeStage.BABY.ordinal());
        baby.breedCooldown = BREED_COOLDOWN;
        baby.genderSet = true;
        baby.entityData.set(DATA_FEMALE, getRandom().nextBoolean());
        baby.setCustomName(Component.literal(Names.random(getRandom(), baby.isFemale())));
        server.addFreshEntity(baby);

        colony.take(Resource.FOOD, BREED_FOOD_COST);
        this.breedCooldown = BREED_COOLDOWN;
        mate.breedCooldown = BREED_COOLDOWN;
        server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 5, 0.25, 0.2, 0.25, 0.0);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.3, getZ(), 6, 0.3, 0.2, 0.3, 0.0);
        server.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.9f, 1.3f);   // ¡nació un bebé!
        Chronicle.add(server, team, getName().getString() + " y " + mate.getName().getString()
                + " tuvieron un bebé: " + baby.getName().getString());
    }

    private int colonyPopulation(ServerLevel server, Colony colony) {
        BlockPos h = colony.home();
        return server.getEntitiesOfClass(SoldierEntity.class, new AABB(h).inflate(48.0),
                e -> e.team == team && e.isAlive()).size();
    }

    // ---------------------------------------------------------------- muerte e historial

    @Override
    public void die(DamageSource source) {
        if (!this.dead && !isRemoved() && level() instanceof ServerLevel server) {
            Chronicle.add(server, team, getName().getString() + " " + describeDeath(source) + " (" + stage().label() + ")");
        }
        super.die(source);
    }

    private String describeDeath(DamageSource s) {
        if (agedOut) return "murió de viejo";
        Entity killer = s.getEntity();
        if (killer instanceof SoldierEntity k)
            return "cayó en combate contra " + k.getName().getString() + " (" + k.team.label().toLowerCase() + ")";
        if (killer instanceof MiniCreeperEntity) return "voló por los aires por un mini creeper";
        if (killer instanceof Player) return "fue aplastado por un gigante";
        if (s.is(DamageTypes.STARVE)) return "murió de hambre";
        if (s.is(DamageTypes.FALL)) return "murió por una caída";
        if (s.is(DamageTypes.DROWN)) return "se ahogó";
        if (s.is(DamageTypes.IN_FIRE) || s.is(DamageTypes.ON_FIRE) || s.is(DamageTypes.LAVA)) return "murió quemado";
        return "murió de causas misteriosas";
    }

    /** Si te atacan, respondes (así no gana siempre el que pega primero). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide && source.getEntity() instanceof SoldierEntity a
                && a.team != team && getTarget() == null && canFight()) setTarget(a);
        return r;
    }

    /** Ajusta la animación de caminar a su tamaño diminuto: piernas más rápidas, sin "resbalar". */
    @Override
    protected void updateWalkAnimation(float distance) {
        float target = getPose() == Pose.STANDING ? Math.min(distance * 6.0f * ANIM_BOOST, 3.0f) : 0f;
        this.walkAnimation.update(target, 0.4f);
    }

    // ---------------------------------------------------------------- acceso para metas y renderer

    public Colony colony() { return Colony.get((ServerLevel) level(), team); }
    public Team team() { return team; }
    public float hunger() { return hunger; }
    public void eat(float amount) { hunger = Math.min(MAX_HUNGER, hunger + amount); }

    public LifeStage stage() { return LifeStage.values()[entityData.get(DATA_STAGE)]; }
    public boolean canFight() { return stage() == LifeStage.ADULT; }
    public boolean isFemale() { return entityData.get(DATA_FEMALE); }
    public boolean canWork() { return stage() != LifeStage.BABY; }

    // ---------------------------------------------------------------- zona, pareja y oficio

    /** Punto al que pertenece: los bebés, la casa terminada más cercana; los demás, el hogar. */
    @Nullable
    public BlockPos anchor() {
        Colony c = colony();
        BlockPos home = c.home();
        if (home == null) return null;
        if (stage() == LifeStage.BABY) {
            BlockPos best = null;
            double bestDist = 24.0 * 24.0;
            for (BlockPos p : c.houseSites()) {
                BlockState s = level().getBlockState(p);
                if (s.getBlock() instanceof HouseBlock && s.getValue(HouseBlock.STAGE) >= 3) {
                    double d = p.distSqr(blockPosition());
                    if (d < bestDist) { bestDist = d; best = p; }
                }
            }
            if (best != null) return best;
        }
        return home;
    }

    /** Hasta dónde puede alejarse de su punto de anclaje. */
    public int leashRadius() {
        return switch (stage()) { case BABY -> 5; case YOUTH -> 14; default -> 22; };
    }

    public boolean isReadyToBreed() {
        return stage() == LifeStage.ADULT && breedCooldown <= 0 && hunger >= 12f && getTarget() == null;
    }

    public boolean colonyHasRoom() {
        Colony c = colony();
        return c.home() != null && c.get(Resource.FOOD) >= BREED_FOOD_COST
                && colonyPopulation((ServerLevel) level(), c) < c.capacity();
    }

    @Nullable
    public SoldierEntity findMate(double range) {
        SoldierEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (SoldierEntity m : level().getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(range),
                o -> o != this && o.team == team && o.isFemale() != isFemale() && o.isReadyToBreed())) {
            double d = distanceToSqr(m);
            if (d < bestDist) { bestDist = d; best = m; }
        }
        return best;
    }

    public void tryBreedNow() { tryBreed((ServerLevel) level(), colony()); }

    @Nullable public Role role() { return role; }

    /** Hace con gusto las tareas de su oficio; las ajenas solo si el reino las necesita con urgencia. */
    public boolean roleAllows(Role needed, boolean urgent) { return role == null || role == needed || urgent; }

    /** Velocidad al trabajar: paso normal, pero apurado si tiene hambre. (multiplica la velocidad base) */
    public double workSpeed() { return hunger < 6f ? 1.4 : 1.0; }

    public boolean canCarry(Resource r) {
        return carriedAmount == 0 || (carried == r && carriedAmount < CARRY_CAPACITY);
    }
    public void carry(Resource r) { carried = r; carriedAmount++; }
    @Nullable public Resource carried() { return carried; }
    public int carriedAmount() { return carriedAmount; }
    public void clearCarried() { carried = null; carriedAmount = 0; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Hunger", hunger);
        tag.putInt("CarriedAmount", carriedAmount);
        tag.putInt("AgeTicks", ageTicks);
        tag.putInt("BreedCooldown", breedCooldown);
        tag.putBoolean("Female", isFemale());
        tag.putBoolean("GenderSet", genderSet);
        if (role != null) tag.putString("Role", role.name());
        if (carried != null) tag.putString("Carried", carried.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Hunger")) hunger = tag.getFloat("Hunger");
        carriedAmount = tag.getInt("CarriedAmount");
        carried = tag.contains("Carried") ? Resource.valueOf(tag.getString("Carried")) : null;
        if (tag.contains("AgeTicks")) ageTicks = tag.getInt("AgeTicks");
        if (tag.contains("BreedCooldown")) breedCooldown = tag.getInt("BreedCooldown");
        if (tag.contains("GenderSet")) { genderSet = tag.getBoolean("GenderSet"); entityData.set(DATA_FEMALE, tag.getBoolean("Female")); }
        entityData.set(DATA_STAGE, LifeStage.fromAge(ageTicks).ordinal());
        if (tag.contains("Role")) role = Role.valueOf(tag.getString("Role"));
    }
}
