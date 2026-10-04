package dev.claywars.entity.ai;

import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Colony;
import dev.claywars.sim.ReserveData;
import dev.claywars.sim.Resource;
import dev.claywars.sim.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Busca y extrae un recurso natural alcanzable cerca del hogar (troncos o cultivos maduros), con animación de trabajo. */
public class GatherGoal extends Goal {
    private static final int RADIUS = 16, ACTION_TICKS = 20, LOG_RESERVE = 4, MAX_RUN_TICKS = 600;
    private static final double WORK_DIST_SQR = 2.4;

    private final SoldierEntity mob;
    private final Resource resource;
    @Nullable private BlockPos target;
    private int timer, repath, fails, nextScan, runTicks;
    private boolean pathedOnce;
    private final Map<Long, Integer> blacklist = new HashMap<>();

    public GatherGoal(SoldierEntity mob, Resource resource) {
        this.mob = mob;
        this.resource = resource;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (!mob.canWork() || !mob.canCarry(resource)) return false;
        Colony colony = mob.colony();
        if (colony.get(resource) >= resource.targetStock) return false;
        Role needed = resource == Resource.FOOD ? Role.FARMER : Role.LUMBERJACK;
        if (!mob.roleAllows(needed, colony.get(resource) < resource.targetStock / 3)) return false;
        if (mob.tickCount < nextScan) return false;
        target = findTarget();
        if (target == null) { nextScan = mob.tickCount + 40; return false; }
        return true;
    }

    @Override public boolean canContinueToUse() {
        return target != null && fails < 4 && runTicks < MAX_RUN_TICKS && mob.canCarry(resource)
                && isValid(mob.level().getBlockState(target));
    }

    @Override public void start() { timer = 0; repath = 0; fails = 0; runTicks = 0; pathedOnce = false; }

    @Override public void stop() {
        if (target != null && (fails >= 4 || runTicks >= MAX_RUN_TICKS))
            blacklist.put(target.asLong(), mob.tickCount + 1200);
        target = null;
        mob.getNavigation().stop();
        nextScan = mob.tickCount;
    }

    @Override public void tick() {
        if (target == null) return;
        runTicks++;
        Vec3 c = Vec3.atCenterOf(target);
        if (mob.position().distanceToSqr(c) > WORK_DIST_SQR) {
            if (--repath <= 0) {
                repath = 10;
                if (pathedOnce && mob.getNavigation().isDone()) fails++;
                pathedOnce = true;
                if (!mob.getNavigation().moveTo(c.x, target.getY(), c.z, mob.workSpeed())) fails++;
            }
        } else {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(c);
            timer++;
            if (timer % 6 == 0) mob.swing(InteractionHand.MAIN_HAND);
            if (timer % 10 == 0) hitEffects();
            if (timer >= ACTION_TICKS) { timer = 0; harvest(); }
        }
    }

    private void hitEffects() {
        ServerLevel level = (ServerLevel) mob.level();
        BlockState s = level.getBlockState(target);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s),
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, target, s.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.4f, 1.4f);
    }

    private void harvest() {
        ServerLevel level = (ServerLevel) mob.level();
        BlockState state = level.getBlockState(target);
        mob.swing(InteractionHand.MAIN_HAND);
        if (resource == Resource.WOOD) {
            int left = ReserveData.get(level).extract(target, LOG_RESERVE);
            if (left == 0) fellTree(level, target);
        } else if (state.getBlock() instanceof CropBlock crop) {
            level.setBlock(target, crop.getStateForAge(0), 3);                 // se replanta solo
            if (state.is(Blocks.WHEAT) && mob.getRandom().nextBoolean()) mob.colony().add(Resource.SEEDS, 1);
        } else {
            level.setBlock(target, state.setValue(SweetBerryBushBlock.AGE, 1), 3);
        }
        if (resource == Resource.FOOD && mob.hunger() < 8f) {                   // con hambre, come lo que cosecha
            mob.eat(5f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + 0.4, mob.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
        } else {
            mob.carry(resource);
        }
    }

    /** Al agotar el tronco, tala el árbol completo para no dejar troncos flotando. */
    private void fellTree(ServerLevel level, BlockPos origin) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        List<BlockPos> logs = new ArrayList<>();
        boolean tree = false;
        queue.add(origin);
        seen.add(origin.asLong());
        while (!queue.isEmpty() && logs.size() < 48) {
            BlockPos p = queue.poll();
            logs.add(p);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos n = p.offset(dx, dy, dz);
                if (Math.abs(n.getX() - origin.getX()) > 6 || Math.abs(n.getZ() - origin.getZ()) > 6 || n.getY() < origin.getY()) continue;
                BlockState s = level.getBlockState(n);
                if (s.is(BlockTags.LEAVES)) tree = true;
                if (s.is(BlockTags.LOGS) && seen.add(n.asLong())) queue.add(n);
            }
        }
        Colony colony = mob.colony();
        for (BlockPos p : logs) {
            if (!tree && !p.equals(origin)) continue;      // si no es un árbol (p. ej. una cabaña), solo se quita el tronco tocado
            level.destroyBlock(p, false);
            if (!p.equals(origin)) colony.add(Resource.WOOD, 2);
        }
    }

    private boolean isValid(BlockState s) {
        return switch (resource) {
            case WOOD -> s.is(BlockTags.LOGS);
            case FOOD -> (s.getBlock() instanceof CropBlock c && c.isMaxAge(s))
                    || (s.is(Blocks.SWEET_BERRY_BUSH) && s.getValue(SweetBerryBushBlock.AGE) == 3);
            default -> false;
        };
    }

    @Nullable private BlockPos findTarget() {
        blacklist.values().removeIf(expire -> expire <= mob.tickCount);
        BlockPos home = mob.colony().home();
        BlockPos center = home != null ? new BlockPos(home.getX(), mob.blockPosition().getY(), home.getZ()) : mob.blockPosition();
        int up = resource == Resource.WOOD ? 1 : 2;
        int down = 2;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-RADIUS, -down, -RADIUS), center.offset(RADIUS, up, RADIUS))) {
            if (blacklist.containsKey(p.asLong())) continue;
            if (!isValid(mob.level().getBlockState(p))) continue;
            double d = p.distSqr(mob.blockPosition());
            if (d < bestDist) { bestDist = d; best = p.immutable(); }
        }
        return best;
    }
}
