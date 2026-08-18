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

import org.jetbrains.annotations.ApiStatus;

/**
 * Local replacement for CoreLib's {@code IConfigDispatch}. The lifecycle is
 * driven by {@link JsonConfigManager}:
 * {@code onPreLoad -> defaults() -> update(new) -> onPostLoad -> execute(fromInit)}.
 */
@ApiStatus.Internal
public interface IConfigDispatch
{
    String getConfigName();

    IConfigData newConfig();

    IConfigData getConfig();

    boolean isLoaded();

    void initConfig();

    void onPreLoadConfig();

    void onPostLoadConfig();

    void onPreSaveConfig();

    void onPostSaveConfig();

    IConfigData defaults();

    IConfigData update(IConfigData newConfig);

    void execute(boolean fromInit);
}
