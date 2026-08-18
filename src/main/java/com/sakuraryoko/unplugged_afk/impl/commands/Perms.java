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

package com.sakuraryoko.unplugged_afk.impl.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;

import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;

/**
 * Replaces the Fabric permissions API shim. An explicitly-set Bukkit node wins
 * (so LuckPerms and friends are authoritative); otherwise it falls back to the
 * vanilla operator level the config asks for, matching the Fabric behaviour.
 */
@ApiStatus.Internal
public class Perms
{
    public static boolean check(CommandSender sender, String node, int level)
    {
        if (sender.isPermissionSet(node))
        {
            return sender.hasPermission(node);
        }

        if (sender instanceof Player player)
        {
            Permission required = permissionFromInt(level);
            return required == null || Nms.handle(player).permissions().hasPermission(required);
        }

        // Console and command blocks
        return true;
    }

    /** As {@link #check}, but gated behind {@code main.advancedAdminOptions}. */
    public static boolean checkAdv(CommandSender sender, String node, int level)
    {
        if (!ConfigWrap.mainOpt().advancedAdminOptions)
        {
            return false;
        }

        return check(sender, node, level);
    }

    /**
     * Maps the config's vanilla operator level onto 26.2's named permissions.
     * Level 0 means "no permission required", so it maps to null.
     */
    public static Permission permissionFromInt(int level)
    {
        return switch (Math.max(0, Math.min(level, 4)))
        {
            case 0 -> null;
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            default -> Permissions.COMMANDS_OWNER;
        };
    }
}
