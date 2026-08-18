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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Replacement for CoreLib's {@code TimeFormat}. */
public enum TimeFormat
{
    RFC1123("rfc1123"),
    REGULAR("regular"),
    FORMATTED("formatted"),
    ISO_LOCAL("iso_local"),
    ISO_OFFSET("iso_offset"),
    TIME_ONLY("time_only"),
    DATE_ONLY("date_only"),
    ;

    private static final DateTimeFormatter REGULAR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final String name;

    TimeFormat(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public static TimeFormat fromStringStatic(String value)
    {
        if (value == null || value.isEmpty())
        {
            return RFC1123;
        }

        String lower = value.toLowerCase(Locale.ROOT);

        for (TimeFormat format : values())
        {
            if (format.name.equals(lower) || format.name().equalsIgnoreCase(value))
            {
                return format;
            }
        }

        return RFC1123;
    }

    public String format(long epochMillis)
    {
        return this.format(epochMillis, "");
    }

    public String format(long epochMillis, String customFormat)
    {
        ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());

        return switch (this)
        {
            case RFC1123 -> DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
            case REGULAR -> REGULAR_FORMAT.format(time);
            case FORMATTED -> (customFormat == null || customFormat.isEmpty()
                    ? REGULAR_FORMAT
                    : DateTimeFormatter.ofPattern(customFormat, Locale.ROOT)).format(time);
            case ISO_LOCAL -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time);
            case ISO_OFFSET -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time);
            case TIME_ONLY -> TIME_FORMAT.format(time);
            case DATE_ONLY -> DATE_FORMAT.format(time);
        };
    }
}
