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

package com.sakuraryoko.unplugged_afk.impl.player.unplugged;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.MinecraftServer;

import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.events.ServerEventsHandler;

@ApiStatus.Internal
public class UnpluggedPendingSpawns
{
    public static final UnpluggedPendingSpawns INSTANCE = new UnpluggedPendingSpawns();
    private static final float TICK_RATE = 3.0f;
    private final List<PlayerOptions> pendingSpawnsList;
    private long lastTick;

    private UnpluggedPendingSpawns()
    {
        this.pendingSpawnsList = new ArrayList<>();
        this.lastTick = System.currentTimeMillis();
    }

    public void scheduleSpawn(PlayerOptions opts)
    {
        UUID uuid = opts.uuid;

        // Deduplicate
        for (PlayerOptions spawn : this.pendingSpawnsList)
        {
            if (uuid.equals(spawn.uuid))
            {
                return;
            }
        }

        this.pendingSpawnsList.add(opts);
    }

    private boolean shouldTick()
    {
        return !this.pendingSpawnsList.isEmpty();
    }

    private void executeOneSpawn(@Nonnull MinecraftServer server)
    {
        if (!this.pendingSpawnsList.isEmpty())
        {
            PlayerOptions opts = this.pendingSpawnsList.removeFirst();
            opts.state = opts.state.ensureValid();

            UnpluggedServerPlayer.createFromConfig(server, opts);

            if (this.pendingSpawnsList.isEmpty())
            {
                if (!ServerEventsHandler.getInstance().isSpawnSafe())
                {
                    ServerEventsHandler.getInstance().toggleSpawnSafe(true);
                }
            }
        }
    }

    private long tickRate()
    {
        return (long) (TICK_RATE * 1000L);
    }

    public void tick(@Nonnull MinecraftServer server)
    {
        if (this.shouldTick())
        {
            final long now = System.currentTimeMillis();

            if ((now - this.lastTick) > this.tickRate())
            {
                this.executeOneSpawn(server);
                this.lastTick = now;
            }
        }
    }
}
