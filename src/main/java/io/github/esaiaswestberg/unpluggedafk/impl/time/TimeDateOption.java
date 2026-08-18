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

package io.github.esaiaswestberg.unpluggedafk.impl.time;

import io.github.esaiaswestberg.unpluggedafk.impl.config.IConfigOption;

/** Serialises as {@code {"option": "RFC1123", "customFormat": ""}}. */
public class TimeDateOption implements IConfigOption
{
    public TimeFormat option;
    public String customFormat;

    public TimeDateOption()
    {
        this.defaults();
    }

    @Override
    public void defaults()
    {
        this.option = TimeFormat.RFC1123;
        this.customFormat = "";
    }

    @Override
    public TimeDateOption copy(IConfigOption other)
    {
        if (other instanceof TimeDateOption opts)
        {
            this.option = opts.option != null ? opts.option : TimeFormat.RFC1123;
            this.customFormat = opts.customFormat != null ? opts.customFormat : "";
        }

        return this;
    }

    public String format(long epochMillis)
    {
        return (this.option != null ? this.option : TimeFormat.RFC1123).format(epochMillis, this.customFormat);
    }

    public String now()
    {
        return this.format(System.currentTimeMillis());
    }
}
