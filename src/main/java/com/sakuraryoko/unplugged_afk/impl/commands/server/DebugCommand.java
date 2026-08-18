/*
 * This file is part of the Unplugged-AFK project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Sakura-Ryoko and contributors
 *
 * Unplugged-AFK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unplugged-AFK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unplugged-AFK.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sakuraryoko.unplugged_afk.impl.commands.server;

import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.UUIDUtil;

import com.sakuraryoko.unplugged_afk.api.state.GameState;
import com.sakuraryoko.unplugged_afk.api.state.PosState;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedStatus;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;
import com.sakuraryoko.unplugged_afk.impl.nms.Text;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedEntryList;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;

/**
 * TEMPORARY harness for exercising the shadow spawn from the console, without
 * needing a connected client. Exercises the same {@code createFromConfig} path
 * the restart-respawn uses. Remove once the admin command lands.
 */
@ApiStatus.Internal
public class DebugCommand
{
    public static LiteralCommandNode<CommandSourceStack> build()
    {
        return Commands.literal("uadebug")
                .requires(src -> src.getSender().isOp() || src.getSender().getName().equals("CONSOLE"))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(DebugCommand::spawn))))))
                .then(Commands.literal("list").executes(DebugCommand::list))
                .build();
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx)
    {
        String name = StringArgumentType.getString(ctx, "name");
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");

        UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);

        PlayerOptions opts = new PlayerOptions();
        opts.uuid = uuid;
        opts.name = name;
        opts.state = new UnpluggedState(UnpluggedStatus.ACTIVE, 60, 60L * 60L * 1000L, System.currentTimeMillis(), "debug");
        opts.pos = new PosState("minecraft:overworld", x, y, z, 0f, 0f);
        opts.game = new GameState("survival", false);

        ctx.getSource().getSender().sendMessage(Text.adventure("§eSpawning shadow '" + name + "' at " + x + "," + y + "," + z + "§r"));
        UnpluggedServerPlayer.createFromConfig(Nms.server(), opts);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx)
    {
        var map = UnpluggedEntryList.getInstance().shadowMapCopy();
        ctx.getSource().getSender().sendMessage(Text.adventure("§eShadows: " + map.size() + "§r"));
        map.forEach((uuid, entry) ->
                ctx.getSource().getSender().sendMessage(Text.adventure(
                        " §7- §e" + entry.name().getString() + "§7 status=" + entry.status() + " timeout=" + entry.timeout())));
        return 1;
    }
}
