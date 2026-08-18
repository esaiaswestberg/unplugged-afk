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

import java.nio.file.Path;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.esaiaswestberg.unpluggedafk.impl.commands.CommandRegister;
import io.github.esaiaswestberg.unpluggedafk.impl.config.JsonConfigManager;
import io.github.esaiaswestberg.unpluggedafk.impl.config.UnpluggedConfigHandler;
import io.github.esaiaswestberg.unpluggedafk.impl.events.BukkitBridge;
import io.github.esaiaswestberg.unpluggedafk.impl.events.ServerEventsHandler;
import io.github.esaiaswestberg.unpluggedafk.impl.events.TickDriver;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Nms;
import io.github.esaiaswestberg.unpluggedafk.impl.player.PlayerManager;

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

        configs.migrateLegacyConfig(UnpluggedConfigHandler.getInstance(), legacyConfigPaths());

        // PlayerManager owns what the Fabric config handler called directly.
        UnpluggedConfigHandler.getInstance().setExecuteHandler(PlayerManager.getInstance());
        configs.loadEach(true);

        CommandRegister.register(this);
    }

    /**
     * Where a config from an older name or location might still be sitting,
     * newest first: this plugin's previous data folders, then the server root
     * the Fabric mod used.
     */
    private java.util.List<Path> legacyConfigPaths()
    {
        java.util.List<Path> paths = new java.util.ArrayList<>();
        Path pluginsDir = this.getDataFolder().toPath().getParent();

        for (String folder : Reference.LEGACY_DATA_FOLDERS)
        {
            for (String name : Reference.LEGACY_CONFIG_NAMES)
            {
                paths.add(pluginsDir.resolve(folder).resolve(name + ".json"));
            }

            paths.add(pluginsDir.resolve(folder).resolve(Reference.CONFIG_FILE));
        }

        for (String name : Reference.LEGACY_CONFIG_NAMES)
        {
            paths.add(Path.of(".").resolve(name + ".json"));
        }

        return paths;
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
