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

package io.github.esaiaswestberg.unpluggedafk.impl.config.data.options;

import org.jetbrains.annotations.ApiStatus;

import io.github.esaiaswestberg.unpluggedafk.impl.config.IConfigOption;

@ApiStatus.Internal
public class MainOptions implements IConfigOption
{
    public boolean unpluggedAfkEnabled;
    public boolean debugMode;
    public boolean reducedListDebugInfo;
    public boolean advancedAdminOptions;

    public MainOptions()
    {
        this.defaults();
    }

    @Override
    public void defaults()
    {
        this.unpluggedAfkEnabled = true;
        this.debugMode = false;
        this.reducedListDebugInfo = true;
        this.advancedAdminOptions = false;
    }

    @Override
    public MainOptions copy(IConfigOption opt)
    {
        MainOptions opts = (MainOptions) opt;

        this.unpluggedAfkEnabled = opts.unpluggedAfkEnabled;
        this.debugMode = opts.debugMode;
        this.reducedListDebugInfo = opts.reducedListDebugInfo;
        this.advancedAdminOptions = opts.advancedAdminOptions;

        return this;
    }
}
