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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;

import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.commands.Perms;
import com.sakuraryoko.unplugged_afk.impl.commands.TimeLimits;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;
import com.sakuraryoko.unplugged_afk.impl.nms.Text;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;

/** {@code /unplug [<minutes>] [<reason>]} */
@ApiStatus.Internal
public class UnplugCommand
{
    public static final String NODE = Reference.MOD_ID + "." + Reference.UNPLUG_COMMAND;

    public static LiteralCommandNode<CommandSourceStack> build(String label)
    {
        return Commands.literal(label)
                .requires(src -> Perms.check(src.getSender(), NODE, ConfigWrap.cmdOpt().unplugCommandPermissions))
                .executes(ctx -> unplug(ctx, -1, ""))
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                        .executes(ctx -> unplug(ctx, IntegerArgumentType.getInteger(ctx, "minutes"), ""))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> unplug(ctx,
                                        IntegerArgumentType.getInteger(ctx, "minutes"),
                                        StringArgumentType.getString(ctx, "reason")))
                        )
                )
                .build();
    }

    private static int unplug(CommandContext<CommandSourceStack> ctx, int time, String reason)
    {
        if (!ConfigWrap.mainOpt().unpluggedAfkEnabled)
        {
            ctx.getSource().getSender().sendMessage(Text.adventure("§cUnplugged-AFK is disabled§r"));
            return 0;
        }

        if (!(ctx.getSource().getExecutor() instanceof Player bukkitPlayer))
        {
            ctx.getSource().getSender().sendMessage(Text.adventure("§cThis command can only be used by a player§r"));
            return 0;
        }

        ServerPlayer player = Nms.handle(bukkitPlayer);

        boolean explicit = time > 0;

        if (!explicit)
        {
            time = ConfigWrap.unplugged().defaultUnpluggedTimeout;
        }
        if (time <= 0)
        {
            time = 129600;
        }
        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultUnpluggedReason;
        }

        // Permission ceiling (unplugged_afk.maxtime.<minutes>) overrides both an
        // explicit argument and the config default.
        int allowed = TimeLimits.clamp(ctx.getSource().getSender(), time);

        if (allowed < time)
        {
            if (explicit)
            {
                ctx.getSource().getSender().sendMessage(Text.adventure(
                        "§eYou may stay unplugged for at most §a" + allowed + "§e minutes; using that instead.§r"));
            }

            time = allowed;
        }

        if (!UnpluggedServerPlayer.createFromPlayer(Nms.server(), player, time, reason))
        {
            ctx.getSource().getSender().sendMessage(Text.adventure("§cCould not start an AFK session; check the server log.§r"));
            return 0;
        }

        return 1;
    }
}
