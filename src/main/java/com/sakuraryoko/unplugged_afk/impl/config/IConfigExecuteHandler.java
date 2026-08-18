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

package com.sakuraryoko.unplugged_afk.impl.config;

import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.unplugged_afk.impl.config.data.UnpluggedConfigData;

/**
 * Hook invoked at the end of {@link UnpluggedConfigHandler#execute(boolean)}.
 *
 * <p>The Fabric build called {@code PlayerManager} and {@code ServerEventsHandler}
 * directly from the config handler. On Paper those subsystems are only live once
 * the plugin has enabled and the server is ticking, so the dependency is inverted
 * here: they register a handler instead of being called into unconditionally.
 */
@ApiStatus.Internal
public interface IConfigExecuteHandler
{
    /**
     * @param config the freshly-updated config
     * @param fromInit true on the initial load, false on a reload
     * @param hideAll the hide-player options were switched on
     * @param unhideAll the hide-player options were switched off
     * @param fromReloadCmd this load came from {@code /unplugged-admin reload}
     */
    void onConfigExecute(UnpluggedConfigData config, boolean fromInit, boolean hideAll, boolean unhideAll, boolean fromReloadCmd);
}
