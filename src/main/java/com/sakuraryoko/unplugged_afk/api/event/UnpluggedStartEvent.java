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

package com.sakuraryoko.unplugged_afk.api.event;

import java.util.UUID;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;

/** Fired when an unplugged session begins. */
public class UnpluggedStartEvent extends UnpluggedEvent
{
    private static final HandlerList HANDLERS = new HandlerList();

    public UnpluggedStartEvent(@Nullable UUID player, UnpluggedState state)
    {
        super(player, state);
    }

    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS;
    }
}
