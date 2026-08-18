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

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import com.sakuraryoko.unplugged_afk.impl.commands.CommandRegister;
import com.sakuraryoko.unplugged_afk.impl.config.JsonConfigManager;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;
import com.sakuraryoko.unplugged_afk.impl.events.BukkitBridge;
import com.sakuraryoko.unplugged_afk.impl.events.ServerEventsHandler;
import com.sakuraryoko.unplugged_afk.impl.events.TickDriver;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;
import com.sakuraryoko.unplugged_afk.impl.player.PlayerManager;

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

        // PlayerManager owns what the Fabric config handler called directly.
        UnpluggedConfigHandler.getInstance().setExecuteHandler(PlayerManager.getInstance());
        configs.loadEach(true);

        CommandRegister.register(this);
    }

    @Override
    public void onEnable()
    {
        UnpluggedConfigHandler.getInstance().setStartTime();
        ServerEventsHandler.getInstance().onStarting(Nms.server());

        TickDriver.register(this);
        BukkitBridge.register(this);

        Log.info("{} v{} enabled", Reference.MOD_NAME, this.getPluginMeta().getVersion());
    }

    @Override
    public void onDisable()
    {
        ServerEventsHandler.getInstance().onStopping(Nms.server());
        UnpluggedConfigHandler.getInstance().setStopTime();
        JsonConfigManager.getInstance().saveEach();

        Log.info("{} disabled", Reference.MOD_NAME);
        instance = null;
    }
}
