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

package com.sakuraryoko.unplugged_afk.impl;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;

import com.sakuraryoko.unplugged_afk.impl.config.JsonConfigManager;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;

/**
 * Paper plugin entry point. Replaces the Fabric {@code ModInitializer} plus the
 * CoreLib {@code ModInitManager} dispatch that used to live in
 * {@code impl.modinit.UnpluggedInit}.
 */
public class UnpluggedAfkPlugin extends JavaPlugin
{
    private static UnpluggedAfkPlugin instance;

    public static UnpluggedAfkPlugin getInstance()
    {
        return instance;
    }

    @Override
    public void onLoad()
    {
        instance = this;
        Log.init(this.getLogger());

        JsonConfigManager configs = JsonConfigManager.getInstance();
        configs.setConfigDir(this.getDataFolder().toPath());
        configs.registerConfigDispatcher(UnpluggedConfigHandler.getInstance());

        // The Fabric mod kept unplugged_afk.json in the game root; adopt any such file once.
        configs.migrateFromLegacyLocation(UnpluggedConfigHandler.getInstance(), Path.of("."));
        configs.loadEach(true);
    }

    @Override
    public void onEnable()
    {
        UnpluggedConfigHandler.getInstance().setStartTime();
        Log.info("{} v{} enabled", Reference.MOD_NAME, this.getPluginMeta().getVersion());
    }

    @Override
    public void onDisable()
    {
        UnpluggedConfigHandler.getInstance().setStopTime();
        JsonConfigManager.getInstance().saveEach();
        Log.info("{} disabled", Reference.MOD_NAME);
        instance = null;
    }
}
