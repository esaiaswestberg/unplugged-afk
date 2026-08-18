/*
 * This file is part of Unplugged AFK: Paper Edition, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Sakura-Ryoko and contributors
 *
 * Unplugged AFK: Paper Edition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unplugged AFK: Paper Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unplugged AFK: Paper Edition.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.esaiaswestberg.unpluggedafk.impl;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class Reference
{
    /**
     * Permission namespace and internal identifier. Deliberately still
     * {@code unplugged_afk} rather than the new slug, so permission nodes set
     * up against the Fabric mod or an earlier build keep working.
     */
    public static final String MOD_ID = "unplugged_afk";

    /** Plugin name as Bukkit knows it; also the data folder name. */
    public static final String PLUGIN_SLUG = "unplugged-afk-paper-edition";

    /** Human-readable name for logs and command output. */
    public static final String MOD_NAME = "Unplugged AFK: Paper Edition";

    public static final String CONFIG_NAME = PLUGIN_SLUG;
    public static final String CONFIG_FILE = CONFIG_NAME + ".json";

    /** Config names this plugin has previously used, newest first. */
    public static final String[] LEGACY_CONFIG_NAMES = { "unplugged_afk" };

    /** Plugin data folders this plugin has previously used. */
    public static final String[] LEGACY_DATA_FOLDERS = { "UnpluggedAFK" };

    public static final String UNPLUG_COMMAND = "unplug";
    public static final String AFK_COMMAND = "afk";
    public static final String ADMIN_COMMAND = "unplugged-admin";

    /** Compile-time debug flag, OR'd with the {@code main.debugMode} config option. */
    public static final boolean DEBUG = false;
}
