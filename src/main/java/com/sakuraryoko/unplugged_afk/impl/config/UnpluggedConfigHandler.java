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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.sakuraryoko.unplugged_afk.impl.Log;
import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.config.data.UnpluggedConfigData;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.CommandOptions;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.MainOptions;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.MessageOptions;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.UnpluggedOptions;
import com.sakuraryoko.unplugged_afk.impl.time.TimeFormat;

@ApiStatus.Internal
public class UnpluggedConfigHandler implements IConfigDispatch
{
    private static final UnpluggedConfigHandler INSTANCE = new UnpluggedConfigHandler();

    public static UnpluggedConfigHandler getInstance() { return INSTANCE; }

    private UnpluggedConfigData CONFIG = newConfig();
    private final String CONFIG_NAME = Reference.MOD_ID;
    private boolean loaded = false;
    private boolean hideAllPlayers = false;
    private boolean unhideAllPlayers = false;
    private boolean fromReloadCmd = false;
    private boolean commandWarn = false;
    private @Nullable IConfigExecuteHandler executeHandler;

    public void setExecuteHandler(@Nullable IConfigExecuteHandler handler)
    {
        this.executeHandler = handler;
    }

    @Override
    public String getConfigName()
    {
        return this.CONFIG_NAME;
    }

    @Override
    public UnpluggedConfigData newConfig()
    {
        return new UnpluggedConfigData();
    }

    @Override
    public UnpluggedConfigData getConfig()
    {
        return CONFIG;
    }

    public MainOptions getMainOptions()
    {
        return CONFIG.MAIN;
    }

    public CommandOptions getCommandOptions()
    {
        return CONFIG.COMMANDS;
    }

    public UnpluggedOptions getUnpluggedOptions()
    {
        return CONFIG.UNPLUGGED;
    }

    public MessageOptions getMessageOptions()
    {
        return CONFIG.MESS;
    }

    public List<PlayerOptions> getPlayerOptions()
    {
        return CONFIG.PLAYERS;
    }

    @Override
    public boolean isLoaded()
    {
        return this.loaded;
    }

    @Override
    public void initConfig()
    {
        Log.debug("UnpluggedConfigHandler#initConfig()");
    }

    @Override
    public void onPreLoadConfig()
    {
        this.loaded = false;
    }

    @Override
    public void onPostLoadConfig()
    {
        this.loaded = true;
    }

    @Override
    public void onPreSaveConfig()
    {
        this.loaded = false;
    }

    @Override
    public void onPostSaveConfig()
    {
        this.loaded = true;
    }

    @Override
    public UnpluggedConfigData defaults()
    {
        UnpluggedConfigData config = this.newConfig();
        Log.debug("UnpluggedConfigHandler#defaults(): Setting default config.");

        config.config_date = TimeFormat.RFC1123.format(System.currentTimeMillis());
        config.MAIN = new MainOptions();
        config.COMMANDS = new CommandOptions();
        config.UNPLUGGED = new UnpluggedOptions();
        config.MESS = new MessageOptions();
        config.PLAYERS = new ArrayList<>();

        return config;
    }

    @Override
    public UnpluggedConfigData update(IConfigData newConfig)
    {
        UnpluggedConfigData newConf = (UnpluggedConfigData) newConfig;
        Log.debug("UnpluggedConfigHandler#update(): Refresh config.");

        // Tolerate a hand-edited file that dropped whole sections.
        if (newConf.MAIN == null) { newConf.MAIN = new MainOptions(); }
        if (newConf.COMMANDS == null) { newConf.COMMANDS = new CommandOptions(); }
        if (newConf.UNPLUGGED == null) { newConf.UNPLUGGED = new UnpluggedOptions(); }
        if (newConf.MESS == null) { newConf.MESS = new MessageOptions(); }
        if (newConf.PLAYERS == null) { newConf.PLAYERS = new ArrayList<>(); }

        CONFIG.comment = Reference.MOD_NAME + " Config";
        CONFIG.config_date = TimeFormat.RFC1123.format(System.currentTimeMillis());

        if (CONFIG.last_start == null || CONFIG.last_start < 1L)
        {
            CONFIG.last_start = System.currentTimeMillis();
        }

        CONFIG.last_stop = Objects.requireNonNullElse(newConf.last_stop, -1L);

        if (CONFIG.last_stop < 1L)
        {
            // last_stop should never be < 1L (Or else things break)
            CONFIG.last_stop = CONFIG.last_start - 60000L;     // 1 minute offset
        }

        if (CONFIG.UNPLUGGED.unpluggedHidePlayer && !newConf.UNPLUGGED.unpluggedHidePlayer)
        {
            this.unhideAllPlayers = true;
        }
        else if (!CONFIG.UNPLUGGED.unpluggedHidePlayer && newConf.UNPLUGGED.unpluggedHidePlayer)
        {
            this.hideAllPlayers = true;
        }
        if (CONFIG.UNPLUGGED.unpluggedHideFromOps && !newConf.UNPLUGGED.unpluggedHideFromOps)
        {
            this.unhideAllPlayers = true;
        }
        else if (!CONFIG.UNPLUGGED.unpluggedHideFromOps && newConf.UNPLUGGED.unpluggedHideFromOps)
        {
            this.hideAllPlayers = true;
        }

        if (CONFIG.COMMANDS.enableAfkCommand && !newConf.COMMANDS.enableAfkCommand)
        {
            this.commandWarn = true;
        }
        if (CONFIG.COMMANDS.enableUnplugCommand && !newConf.COMMANDS.enableUnplugCommand)
        {
            this.commandWarn = true;
        }

        // Copy Incoming Config
        CONFIG.MAIN.copy(newConf.MAIN);
        CONFIG.COMMANDS.copy(newConf.COMMANDS);
        CONFIG.UNPLUGGED.copy(newConf.UNPLUGGED);
        CONFIG.MESS.copy(newConf.MESS);

        // Copy Players Config (deep copy)
        CONFIG.PLAYERS.clear();
        newConf.PLAYERS.forEach(
                player ->
                {
                    PlayerOptions newEntry = new PlayerOptions(player);

                    if (!newEntry.pos.equals(player.pos))
                    {
                        newEntry.pos = player.pos;
                    }
                    if (!newEntry.game.equals(player.game))
                    {
                        newEntry.game = player.game;
                    }

                    CONFIG.PLAYERS.add(newEntry);
                }
        );

        Log.setDebugEnabled(CONFIG.MAIN.debugMode);

        return CONFIG;
    }

    @Override
    public void execute(boolean fromInit)
    {
        Log.debug("UnpluggedConfigHandler#execute(): Execute config.");

        IConfigExecuteHandler handler = this.executeHandler;

        if (handler != null)
        {
            handler.onConfigExecute(CONFIG, fromInit, this.hideAllPlayers, this.unhideAllPlayers, this.fromReloadCmd);
        }

        this.hideAllPlayers = false;
        this.unhideAllPlayers = false;
        this.toggleFromReloadCmd(false);

        if (this.commandWarn)
        {
            Log.warn("UnpluggedConfigHandler#execute(): You need to restart the server to enable or disable commands.");
            this.commandWarn = false;
        }
    }

    public void toggleFromReloadCmd(boolean toggle)
    {
        this.fromReloadCmd = toggle;
    }

    public void setStartTime()
    {
        this.CONFIG.last_start = System.currentTimeMillis();
    }

    public void setStopTime()
    {
        this.CONFIG.last_stop = System.currentTimeMillis();
    }

    public long getLastStart()
    {
        return this.CONFIG.last_start;
    }

    public long getLastStop()
    {
        return this.CONFIG.last_stop;
    }

    public ImmutableList<String> configSuggestions()
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();

        Field[] mainFields = MainOptions.class.getDeclaredFields();
        Field[] cmdFields = CommandOptions.class.getDeclaredFields();
        Field[] msgFields = MessageOptions.class.getDeclaredFields();
        Field[] unpluggedFields = UnpluggedOptions.class.getDeclaredFields();

        for (Field field : mainFields)
        {
            builder.add(field.getName());
        }

        for (Field field : cmdFields)
        {
            builder.add(field.getName());
        }

        for (Field field : msgFields)
        {
            if (field.getType().getSimpleName().equals("DurationOption") ||
                field.getType().getSimpleName().equals("TimeDateOption"))
            {
                builder.add(field.getName() + ".option");
                builder.add(field.getName() + ".customFormat");
            }
            else
            {
                builder.add(field.getName());
            }
        }

        for (Field field : unpluggedFields)
        {
            builder.add(field.getName());
        }

        return builder.build();
    }

    public @Nullable FieldTarget getConfigInstanceByField(String fieldName)
    {
        String parentName = fieldName;
        String childName = null;

        // Check if the user is trying to access a nested field (e.g., duration.customFormat)
        if (fieldName.contains("."))
        {
            String[] parts = fieldName.split("\\.", 2);
            parentName = parts[0];
            childName = parts[1];
        }

        FieldTarget parentData = lookup(MainOptions.class, parentName, this.CONFIG.MAIN);

        if (parentData == null)
        {
            parentData = lookup(CommandOptions.class, parentName, this.CONFIG.COMMANDS);
        }
        if (parentData == null)
        {
            parentData = lookup(UnpluggedOptions.class, parentName, this.CONFIG.UNPLUGGED);
        }
        if (parentData == null)
        {
            parentData = lookup(MessageOptions.class, parentName, this.CONFIG.MESS);
        }

        if (parentData != null && childName != null)
        {
            try
            {
                Field parentField = parentData.field();
                Object wrapperInstance = parentField.get(parentData.instance());
                Field childField = parentField.getType().getDeclaredField(childName);

                return new FieldTarget(childField, wrapperInstance);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        return parentData;
    }

    private static @Nullable FieldTarget lookup(Class<?> owner, String fieldName, Object instance)
    {
        try
        {
            return new FieldTarget(owner.getDeclaredField(fieldName), instance);
        }
        catch (NoSuchFieldException ignored)
        {
            return null;
        }
    }
}
