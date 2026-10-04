package dev.claywars.entity.ai;

import dev.claywars.block.HouseBlock;
import dev.claywars.entity.SoldierEntity;
import dev.claywars.registry.ModBlocks;
import dev.claywars.sim.Chronicle;
import dev.claywars.sim.Colony;
import dev.claywars.sim.Resource;
import dev.claywars.sim.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** La colonia marca un solar y un soldado lo construye por etapas, pagando madera del almacén. */
public class BuildGoal extends Goal {
    /** Madera para pasar de etapa: 0->1 cimientos, 1->2 paredes, 2->3 techo. */
    private static final int[] COSTS = {4, 8, 8};
    private static final int WORK_TICKS = 80, MAX_HOUSES = 6, MAX_RUN_TICKS = 700;
    private static final double WORK_DIST_SQR = 4.5;

    private final SoldierEntity mob;
    @Nullable private BlockPos target;
    private int timer, repath, runTicks, nextCheck;

    public BuildGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (!mob.canWork() || mob.tickCount < nextCheck) return false;
        nextCheck = mob.tickCount + 60 + mob.getRandom().nextInt(40);

        ServerLevel level = (ServerLevel) mob.level();
        Colony c = mob.colony();
        if (c.home() == null || c.get(Resource.FOOD) < 4) return false;   // primero comer
        if (level.getGameTime() < c.busyUntil()) return false;             // ya hay alguien construyendo

        BlockPos site = c.site();
        if (site != null && !(level.getBlockState(site).getBlock() instanceof HouseBlock)) {
            c.clearSite();                                                  // el solar desapareció
            site = null;
        }
        if (site != null && level.getBlockState(site).getValue(HouseBlock.STAGE) >= COSTS.length) {
            c.clearSite();
            return false;
        }
        if (site == null) {
            if (!wantsNewHouse(level, c)) return false;
            site = chooseSite(level, c);
            if (site == null) return false;
            level.setBlock(site, ModBlocks.HOUSE.get().defaultBlockState()
                    .setValue(HouseBlock.BLUE, mob.team() == Team.BLUE), 3);
            c.setSite(site);
            Chronicle.add(level, mob.team(), "marcaron el terreno para una casa nueva");
        }
        int stage = level.getBlockState(site).getValue(HouseBlock.STAGE);
        if (c.get(Resource.WOOD) < COSTS[stage]) return false;
        target = site;
        return true;
    }

    @Override public boolean canContinueToUse() {
        if (target == null || runTicks >= MAX_RUN_TICKS) return false;
        BlockState s = mob.level().getBlockState(target);
        return s.getBlock() instanceof HouseBlock && s.getValue(HouseBlock.STAGE) < COSTS.length;
    }

    @Override public void start() { timer = 0; repath = 0; runTicks = 0; }
    @Override public void stop() { target = null; mob.getNavigation().stop(); }

    @Override public void tick() {
        if (target == null) return;
        runTicks++;
        ServerLevel level = (ServerLevel) mob.level();
        Colony c = mob.colony();
        c.setBusy(level.getGameTime() + 100);

        Vec3 center = Vec3.atCenterOf(target);
        if (mob.position().distanceToSqr(center) > WORK_DIST_SQR) {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(center.x, target.getY(), center.z, mob.workSpeed());
            }
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(center);
        timer++;
        if (timer % 6 == 0) mob.swing(InteractionHand.MAIN_HAND);
        if (timer % 10 == 0) {
            BlockState s = level.getBlockState(target);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s),
                    center.x, center.y, center.z, 4, 0.25, 0.2, 0.25, 0.05);
            level.playSound(null, target, s.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5f, 1.2f);
        }
        if (timer >= WORK_TICKS) { timer = 0; advance(level, c); }
    }

    private void advance(ServerLevel level, Colony c) {
        BlockState s = level.getBlockState(target);
        int stage = s.getValue(HouseBlock.STAGE);
        if (stage >= COSTS.length || !c.take(Resource.WOOD, COSTS[stage])) return;
        level.setBlock(target, s.setValue(HouseBlock.STAGE, stage + 1), 3);
        level.playSound(null, target, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
        if (stage + 1 >= 3) {
            c.addHouse();
            c.clearSite();
            Chronicle.add(level, mob.team(), "terminaron una casa (capacidad ahora " + c.capacity() + ")");
        }
    }

    private boolean wantsNewHouse(ServerLevel level, Colony c) {
        if (c.houses() >= MAX_HOUSES || c.get(Resource.WOOD) < COSTS[0]) return false;
        if (c.houses() == 0 || c.get(Resource.WOOD) >= 24) return true;      // primera casa o madera de sobra
        int pop = level.getEntitiesOfClass(SoldierEntity.class, new AABB(c.home()).inflate(48.0),
                e -> e.team() == mob.team() && e.isAlive()).size();
        return pop >= c.capacity() - 3;                                        // casi sin espacio
    }

    @Nullable
    private BlockPos chooseSite(ServerLevel level, Colony c) {
        BlockPos home = c.home();
        for (int i = 0; i < 30; i++) {
            int dx = mob.getRandom().nextInt(21) - 10, dz = mob.getRandom().nextInt(21) - 10;
            int d2 = dx * dx + dz * dz;
            if (d2 < 25 || d2 > 100) continue;                                 // ni pegado al hogar ni muy lejos
            int x = home.getX() + dx, z = home.getZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, home.getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - home.getY()) > 2) continue;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState here = level.getBlockState(pos);
            if (!(here.isAir() || here.is(BlockTags.REPLACEABLE)) || !level.getFluidState(pos).isEmpty()) continue;
            if (!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos.below()).isEmpty()) continue;
            boolean flat = true;
            for (Direction dir : Direction.Plane.HORIZONTAL)
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dir.getStepX(), z + dir.getStepZ()) != y) flat = false;
            if (!flat) continue;
            boolean tooClose = false;
            for (BlockPos other : c.houseSites()) if (other.distSqr(pos) < 25) tooClose = true;
            if (tooClose) continue;
            return pos;
        }
        return null;
    }
}
