package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Colony;
import dev.claywars.sim.Resource;
import dev.claywars.sim.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** Agricultura: prepara tierra y siembra trigo con semillas del almacén, y cuida las parcelas para que crezcan. */
public class FarmGoal extends Goal {
    private enum Job { PLANT, TEND }

    private static final int WORK_TICKS = 40, MAX_RUN_TICKS = 500, BASE_PLOTS = 4, MAX_PLOTS = 14;
    private static final double WORK_DIST_SQR = 3.0;

    private final SoldierEntity mob;
    @Nullable private BlockPos target;     // celda del cultivo (encima de la tierra)
    private Job job = Job.PLANT;
    private int timer, repath, runTicks, nextCheck;

    public FarmGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (!mob.canWork() || mob.tickCount < nextCheck) return false;
        nextCheck = mob.tickCount + 40 + mob.getRandom().nextInt(40);

        ServerLevel level = (ServerLevel) mob.level();
        Colony c = mob.colony();
        if (c.home() == null) return false;
        if (!mob.roleAllows(Role.FARMER, c.get(Resource.FOOD) < 6)) return false;

        c.farmPlots().removeIf(p -> !(level.getBlockState(p).getBlock() instanceof CropBlock));   // parcelas perdidas
        c.setDirty();

        int desired = Math.min(MAX_PLOTS, BASE_PLOTS + 2 * c.houses());
        if (c.get(Resource.SEEDS) >= 1 && c.farmPlots().size() < desired) {
            BlockPos site = chooseSite(level, c);
            if (site != null) { target = site; job = Job.PLANT; return true; }
        }
        BlockPos plot = nearestImmature(level, c);
        if (plot != null) { target = plot; job = Job.TEND; return true; }
        return false;
    }

    @Override public boolean canContinueToUse() {
        if (target == null || runTicks >= MAX_RUN_TICKS) return false;
        BlockState s = mob.level().getBlockState(target);
        return job == Job.PLANT ? (s.isAir() || s.is(BlockTags.REPLACEABLE))
                                : (s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) < 7);
    }

    @Override public void start() { timer = 0; repath = 0; runTicks = 0; }
    @Override public void stop() { target = null; mob.getNavigation().stop(); }

    @Override public void tick() {
        if (target == null) return;
        runTicks++;
        ServerLevel level = (ServerLevel) mob.level();
        Vec3 c = Vec3.atCenterOf(target);
        if (mob.position().distanceToSqr(c) > WORK_DIST_SQR) {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(c.x, target.getY(), c.z, mob.workSpeed());
            }
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(c);
        timer++;
        if (timer % 6 == 0) mob.swing(InteractionHand.MAIN_HAND);
        if (timer % 10 == 0) {
            level.sendParticles(job == Job.PLANT ? ParticleTypes.COMPOSTER : ParticleTypes.HAPPY_VILLAGER,
                    c.x, c.y, c.z, 3, 0.2, 0.1, 0.2, 0.0);
            if (job == Job.PLANT) level.playSound(null, target, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
        if (timer >= WORK_TICKS) { timer = 0; perform(level); }
    }

    private void perform(ServerLevel level) {
        Colony c = mob.colony();
        if (job == Job.PLANT) {
            if (!c.take(Resource.SEEDS, 1)) { target = null; return; }
            level.setBlock(target.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            level.setBlock(target, Blocks.WHEAT.defaultBlockState(), 3);
            c.addPlot(target);
        } else {
            BlockState s = level.getBlockState(target);
            if (s.is(Blocks.WHEAT)) {
                level.setBlock(target, s.setValue(CropBlock.AGE, Math.min(7, s.getValue(CropBlock.AGE) + 2)), 2);
                BlockState below = level.getBlockState(target.below());
                if (below.is(Blocks.FARMLAND)) level.setBlock(target.below(), below.setValue(FarmBlock.MOISTURE, 7), 2);
            }
        }
        target = null;   // trabajo terminado: la meta se cierra
    }

    @Nullable private BlockPos nearestImmature(ServerLevel level, Colony c) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : c.farmPlots()) {
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.WHEAT) || s.getValue(CropBlock.AGE) >= 7) continue;
            double d = p.distSqr(mob.blockPosition());
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    @Nullable private BlockPos chooseSite(ServerLevel level, Colony c) {
        BlockPos home = c.home();
        for (int i = 0; i < 30; i++) {
            int x, z;
            if (!c.farmPlots().isEmpty() && mob.getRandom().nextInt(3) != 0) {      // agrupar parcelas
                BlockPos base = c.farmPlots().get(mob.getRandom().nextInt(c.farmPlots().size()));
                x = base.getX() + mob.getRandom().nextInt(5) - 2;
                z = base.getZ() + mob.getRandom().nextInt(5) - 2;
            } else {
                int dx = mob.getRandom().nextInt(21) - 10, dz = mob.getRandom().nextInt(21) - 10;
                int d2 = dx * dx + dz * dz;
                if (d2 < 9 || d2 > 100) continue;
                x = home.getX() + dx;
                z = home.getZ() + dz;
            }
            if (!level.hasChunkAt(new BlockPos(x, home.getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - home.getY()) > 2) continue;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState here = level.getBlockState(pos);
            if (!(here.isAir() || here.is(BlockTags.REPLACEABLE)) || !level.getFluidState(pos).isEmpty()) continue;
            if (!level.getBlockState(pos.below()).is(BlockTags.DIRT)) continue;
            if (c.farmPlots().contains(pos) || pos.distSqr(home) < 6) continue;
            boolean nearHouse = false;
            for (BlockPos h : c.houseSites()) if (h.distSqr(pos) < 6) nearHouse = true;
            if (nearHouse) continue;
            return pos;
        }
        return null;
    }
}
