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

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.sakuraryoko.unplugged_afk.impl.nms.Nms;

/**
 * Feeds {@link ServerEventsHandler#onTick} from {@code ServerTickEndEvent},
 * which fires at the same point as the Fabric build's {@code tickServer} TAIL
 * mixin. A {@code BukkitScheduler} repeating task would run mid-tick instead.
 */
@ApiStatus.Internal
public class TickDriver implements Listener
{
    private boolean started = false;

    public static void register(Plugin plugin)
    {
        Bukkit.getPluginManager().registerEvents(new TickDriver(), plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerTickEnd(ServerTickEndEvent event)
    {
        // onEnable can run before the worlds are usable, so the "server started"
        // hook is deferred to the first real tick.
        if (!this.started)
        {
            this.started = true;
            ServerEventsHandler.getInstance().onStarted(Nms.server());
        }

        ServerEventsHandler.getInstance().onTick(Nms.server());
    }
}
