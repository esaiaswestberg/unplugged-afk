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

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;

/**
 * Base for the three unplugged lifecycle events. These replace the Fabric
 * {@code UnpluggedAfkEvents} callbacks and, like them, are informational rather
 * than cancellable.
 */
public abstract class UnpluggedEvent extends Event
{
    private final @Nullable UUID player;
    private final UnpluggedState state;

    protected UnpluggedEvent(@Nullable UUID player, UnpluggedState state)
    {
        this.player = player;
        this.state = state;
    }

    /** May be null if the shadow was already gone when the event fired. */
    public @Nullable UUID getPlayerId()
    {
        return this.player;
    }

    public UnpluggedState getState()
    {
        return this.state;
    }
}
