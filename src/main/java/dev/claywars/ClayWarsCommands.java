package dev.claywars;

import com.mojang.brigadier.context.CommandContext;
import dev.claywars.entity.SoldierEntity;
import dev.claywars.sim.Chronicle;
import dev.claywars.sim.Colony;
import dev.claywars.sim.LifeStage;
import dev.claywars.sim.Resource;
import dev.claywars.sim.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * /claywars            -> recursos y capacidad de cada reino
 * /claywars poblacion  -> cuántos bebés, jóvenes, adultos y viejos hay
 * /claywars historial  -> últimos 15 eventos (nacimientos, muertes...)
 */
@EventBusSubscriber(modid = ClayWars.MODID)
public class ClayWarsCommands {
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("claywars").requires(s -> s.hasPermission(2))
                .executes(ClayWarsCommands::stock)
                .then(Commands.literal("poblacion").executes(ClayWarsCommands::population))
                .then(Commands.literal("historial").executes(ClayWarsCommands::history)));
    }

    private static void say(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), false);
    }

    private static int stock(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        for (Team t : Team.values()) {
            Colony c = Colony.get(level, t);
            say(ctx.getSource(), t.label() + " -> madera: " + c.get(Resource.WOOD) + ", comida: " + c.get(Resource.FOOD)
                    + ", capacidad: " + c.capacity() + ", hogar: " + c.home());
        }
        return 1;
    }

    private static int population(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        for (Team t : Team.values()) {
            Colony c = Colony.get(level, t);
            if (c.home() == null) { say(ctx.getSource(), t.label() + " -> sin hogar todavía"); continue; }
            List<SoldierEntity> list = level.getEntitiesOfClass(SoldierEntity.class,
                    new AABB(c.home()).inflate(64), s -> s.team() == t && s.isAlive());
            int[] n = new int[LifeStage.values().length];
            for (SoldierEntity s : list) n[s.stage().ordinal()]++;
            say(ctx.getSource(), t.label() + " -> total " + list.size() + " | bebés " + n[0] + ", jóvenes " + n[1]
                    + ", adultos " + n[2] + ", viejos " + n[3]);
        }
        return 1;
    }

    private static int history(CommandContext<CommandSourceStack> ctx) {
        List<String> lines = Chronicle.get(ctx.getSource().getLevel()).last(15);
        if (lines.isEmpty()) say(ctx.getSource(), "Aún no hay historia que contar.");
        for (String l : lines) say(ctx.getSource(), l);
        return 1;
    }
}
