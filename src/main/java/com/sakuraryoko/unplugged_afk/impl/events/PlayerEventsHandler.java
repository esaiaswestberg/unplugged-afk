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

package com.sakuraryoko.unplugged_afk.impl.events;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;

import com.sakuraryoko.unplugged_afk.impl.Log;
import com.sakuraryoko.unplugged_afk.impl.player.PlayerManager;

/**
 * Per-player bookkeeping.
 *
 * <p>The Fabric build suppressed join/leave broadcasts by matching translation
 * keys inside a {@code sendSystemMessage} mixin. Paper only broadcasts when the
 * join/quit event's message is non-null, so {@link BukkitBridge} nulls it for
 * the names registered here instead -- no string matching involved.
 */
@ApiStatus.Internal
public class PlayerEventsHandler
{
    private static final PlayerEventsHandler INSTANCE = new PlayerEventsHandler();

    public static PlayerEventsHandler getInstance() { return INSTANCE; }

    private final Set<String> hideJoin = ConcurrentHashMap.newKeySet();

    private PlayerEventsHandler() {}

    public void addShouldHideJoin(String name)
    {
        if (name != null && !name.isEmpty())
        {
            this.hideJoin.add(name);
        }
    }

    public void removeShouldHideJoin(String name)
    {
        if (name != null)
        {
            this.hideJoin.remove(name);
        }
    }

    public boolean shouldHideJoin(String name)
    {
        return name != null && this.hideJoin.contains(name);
    }

    public void onTick(ServerPlayer player)
    {
        PlayerManager.getInstance().updatePlayerData(player);
    }

    public void clear()
    {
        this.hideJoin.clear();
    }
}
