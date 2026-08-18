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

package com.sakuraryoko.unplugged_afk.impl.time;

import java.util.Locale;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Replacement for CoreLib's {@code DurationFormat}, which delegated to
 * commons-lang3. Enum constants serialise by {@link #name()}, so config values
 * such as {@code "PRETTY"} round-trip unchanged.
 */
public enum DurationFormat
{
    PRETTY("pretty"),
    REGULAR("regular"),
    ISO_EXTENDED("iso_extended"),
    FORMATTED("formatted"),
    ;

    private final String name;

    DurationFormat(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public static DurationFormat fromStringStatic(String value)
    {
        if (value == null || value.isEmpty())
        {
            return PRETTY;
        }

        String lower = value.toLowerCase(Locale.ROOT);

        for (DurationFormat format : values())
        {
            if (format.name.equals(lower) || format.name().equalsIgnoreCase(value))
            {
                return format;
            }
        }

        return PRETTY;
    }

    public String format(long duration)
    {
        return this.format(duration, "");
    }

    public String format(long duration, String customFormat)
    {
        // Preserved from CoreLib -- callers and tests depend on this sentinel.
        if (duration < 1)
        {
            return "Invalid Duration [" + duration + "]";
        }

        return switch (this)
        {
            case PRETTY -> DurationFormatUtils.formatDurationWords(duration, true, true);
            case REGULAR -> DurationFormatUtils.formatDurationHMS(duration);
            case ISO_EXTENDED, FORMATTED -> DurationFormatUtils.formatDuration(
                    duration,
                    customFormat == null || customFormat.isEmpty()
                            ? DurationFormatUtils.ISO_EXTENDED_FORMAT_PATTERN
                            : customFormat,
                    true
            );
        };
    }
}
