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

import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.unplugged_afk.impl.Log;
import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;

/**
 * Per-player limit on how long an unplugged session may last, driven by
 * permission nodes.
 *
 * <p>{@code unplugged_afk.maxtime.<minutes>} grants a ceiling in minutes, and
 * {@code unplugged_afk.maxtime.unlimited} removes it. The highest granted node
 * wins, so a player inheriting several groups gets the most generous one.
 *
 * <p>With no node granted the limit is {@code unplugged.defaultUnpluggedTimeout}
 * from the config, which is what the Fabric mod enforced for everyone.
 */
@ApiStatus.Internal
public class TimeLimits
{
    public static final String PREFIX = Reference.MOD_ID + ".maxtime.";
    public static final String UNLIMITED = PREFIX + "unlimited";

    /** Ceiling in minutes; {@link Integer#MAX_VALUE} means unlimited. */
    public static int maxMinutes(CommandSender sender)
    {
        if (sender.hasPermission(UNLIMITED))
        {
            return Integer.MAX_VALUE;
        }

        int best = -1;

        for (PermissionAttachmentInfo info : sender.getEffectivePermissions())
        {
            if (!info.getValue())
            {
                continue;
            }

            String node = info.getPermission().toLowerCase(Locale.ROOT);

            if (!node.startsWith(PREFIX))
            {
                continue;
            }

            String suffix = node.substring(PREFIX.length());

            if (suffix.equals("unlimited"))
            {
                return Integer.MAX_VALUE;
            }

            try
            {
                best = Math.max(best, Integer.parseInt(suffix));
            }
            catch (NumberFormatException ignored)
            {
                Log.debug("TimeLimits: ignoring malformed node '{}'", node);
            }
        }

        if (best > 0)
        {
            return best;
        }

        int fallback = ConfigWrap.unplugged().defaultUnpluggedTimeout;
        return fallback > 0 ? fallback : 129600;
    }

    /**
     * Clamps a requested duration to the sender's ceiling.
     *
     * @return the duration to actually use, never above the ceiling
     */
    public static int clamp(CommandSender sender, int requestedMinutes)
    {
        int max = maxMinutes(sender);

        if (max == Integer.MAX_VALUE || requestedMinutes <= max)
        {
            return requestedMinutes;
        }

        return max;
    }

    public static boolean isUnlimited(CommandSender sender)
    {
        return maxMinutes(sender) == Integer.MAX_VALUE;
    }
}
