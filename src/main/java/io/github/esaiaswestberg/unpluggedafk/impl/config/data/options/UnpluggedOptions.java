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
public class UnpluggedOptions implements IConfigOption
{
    public int defaultUnpluggedTimeout;
    public boolean resetHealthUponDeath;
    public boolean unpluggedDisableDamage;
    public boolean unpluggedHidePlayer;
    public boolean unpluggedHideFromOps;

    public UnpluggedOptions()
    {
        this.defaults();
    }

    @Override
    public void defaults()
    {
        this.defaultUnpluggedTimeout = 129600;
        this.resetHealthUponDeath = false;
        this.unpluggedDisableDamage = false;
        this.unpluggedHidePlayer = false;
        this.unpluggedHideFromOps = false;
    }

    @Override
    public UnpluggedOptions copy(IConfigOption opt)
    {
        UnpluggedOptions opts = (UnpluggedOptions) opt;

        this.defaultUnpluggedTimeout = opts.defaultUnpluggedTimeout;
        this.resetHealthUponDeath = opts.resetHealthUponDeath;
        this.unpluggedDisableDamage = opts.unpluggedDisableDamage;
        this.unpluggedHidePlayer = opts.unpluggedHidePlayer;
        this.unpluggedHideFromOps = opts.unpluggedHideFromOps;

        return this;
    }
}
