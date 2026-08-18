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

package io.github.esaiaswestberg.unpluggedafk.impl.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.ApiStatus;

import io.github.esaiaswestberg.unpluggedafk.impl.Log;

/**
 * Replacement for CoreLib's {@code ConfigManager}. Owns the JSON file on disk
 * and drives the {@link IConfigDispatch} lifecycle.
 *
 * <p>{@code disableHtmlEscaping} is required -- the message strings are full of
 * legacy section signs that must survive a save/load round trip.
 */
@ApiStatus.Internal
public class JsonConfigManager
{
    private static final JsonConfigManager INSTANCE = new JsonConfigManager();

    public static JsonConfigManager getInstance() { return INSTANCE; }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private final List<IConfigDispatch> dispatchers = new ArrayList<>();
    private Path configDir;

    public void setConfigDir(Path dir)
    {
        this.configDir = dir;
    }

    public Path getConfigDir()
    {
        return this.configDir;
    }

    public void registerConfigDispatcher(IConfigDispatch dispatch)
    {
        if (!this.dispatchers.contains(dispatch))
        {
            this.dispatchers.add(dispatch);
            dispatch.initConfig();
        }
    }

    public Path fileFor(IConfigDispatch dispatch)
    {
        return this.configDir.resolve(dispatch.getConfigName() + ".json");
    }

    /**
     * Adopts a config written under an older name or location, once.
     *
     * <p>Two predecessors exist: the Fabric mod kept
     * {@code unplugged_afk.json} in the server root, and earlier builds of this
     * plugin kept it in {@code plugins/UnpluggedAFK/}. Candidates are tried in
     * order and the first hit is moved to the current path.
     */
    public void migrateLegacyConfig(IConfigDispatch dispatch, List<Path> candidates)
    {
        Path target = this.fileFor(dispatch);

        if (Files.exists(target))
        {
            return;
        }

        for (Path legacy : candidates)
        {
            if (!Files.exists(legacy))
            {
                continue;
            }

            try
            {
                Files.createDirectories(target.getParent());
                Files.move(legacy, target, StandardCopyOption.REPLACE_EXISTING);
                Log.warn("Adopted existing config from '{}' -> '{}'", legacy.toAbsolutePath(), target.toAbsolutePath());
            }
            catch (IOException e)
            {
                Log.error("Failed to adopt config from '" + legacy.toAbsolutePath() + "'", e);
            }

            return;
        }
    }

    public void loadEach(boolean fromInit)
    {
        this.dispatchers.forEach(dispatch -> this.load(dispatch, fromInit));
    }

    public void reloadEach()
    {
        this.dispatchers.forEach(dispatch -> this.load(dispatch, false));
    }

    public void saveEach()
    {
        this.dispatchers.forEach(this::save);
    }

    public void load(IConfigDispatch dispatch, boolean fromInit)
    {
        Path file = this.fileFor(dispatch);
        dispatch.onPreLoadConfig();

        IConfigData incoming = null;

        if (Files.exists(file))
        {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
            {
                incoming = GSON.fromJson(reader, dispatch.newConfig().getClass());
            }
            catch (IOException | JsonSyntaxException e)
            {
                Log.error("Failed to read config '" + file.toAbsolutePath() + "', falling back to defaults", e);
            }
        }

        if (incoming == null)
        {
            incoming = dispatch.defaults();
        }

        dispatch.update(incoming);
        dispatch.onPostLoadConfig();
        dispatch.execute(fromInit);

        // Write back immediately so a fresh or partially-populated file is completed on disk.
        this.save(dispatch);
    }

    public void save(IConfigDispatch dispatch)
    {
        Path file = this.fileFor(dispatch);
        dispatch.onPreSaveConfig();

        try
        {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8))
            {
                GSON.toJson(dispatch.getConfig(), writer);
            }

            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            Log.error("Failed to write config '" + file.toAbsolutePath() + "'", e);
        }

        dispatch.onPostSaveConfig();
    }

    public static Gson gson()
    {
        return GSON;
    }
}
