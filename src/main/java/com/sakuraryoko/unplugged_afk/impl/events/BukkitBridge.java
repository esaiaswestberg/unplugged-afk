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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.unplugged_afk.impl.Log;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;
import com.sakuraryoko.unplugged_afk.impl.player.PlayerManager;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedSpawner;

/**
 * Bukkit event listeners that stand in for several of the Fabric mixins.
 *
 * <p>Join and quit message suppression in particular is cleaner here than it
 * was on Fabric: Paper only broadcasts when the event's message is non-null, so
 * nulling it suppresses the broadcast outright, with none of the translation
 * key and name matching the {@code messageSuppress} mixins needed.
 */
@ApiStatus.Internal
public class BukkitBridge implements Listener
{
    public static void register(Plugin plugin)
    {
        Bukkit.getPluginManager().registerEvents(new BukkitBridge(), plugin);
    }

    /** Step two of the unplug handshake -- see {@link UnpluggedSpawner}. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Log.debug("BukkitBridge#onQuit(): '{}' quit (pending unplug: {})",
                  player.getName(), UnpluggedSpawner.getInstance().isPending(player.getUniqueId()));

        if (UnpluggedSpawner.getInstance().isPending(player.getUniqueId()))
        {
            // Both the player's own quit line and the shadow's follow-up join
            // line are governed by the same config option.
            if (ConfigWrap.mess().hideUnpluggedJoin)
            {
                event.quitMessage(null);
                PlayerEventsHandler.getInstance().addShouldHideJoin(player.getName());
            }

            Log.debug("BukkitBridge#onQuit(): '{}' quit while unplugging, handing to the spawner", player.getName());
            UnpluggedSpawner.getInstance().onQuit(player.getUniqueId());
        }
    }

    /** Suppresses the shadow's own join broadcast. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if (PlayerEventsHandler.getInstance().shouldHideJoin(player.getName()))
        {
            event.joinMessage(null);
            PlayerEventsHandler.getInstance().removeShouldHideJoin(player.getName());
            Log.debug("BukkitBridge#onJoin(): suppressed join broadcast for '{}'", player.getName());
        }

        PlayerManager.getInstance().syncProfile(Nms.handle(player).getGameProfile());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKick(PlayerKickEvent event)
    {
        if (PlayerEventsHandler.getInstance().shouldHideJoin(event.getPlayer().getName()))
        {
            event.leaveMessage(null);
        }
    }
}
