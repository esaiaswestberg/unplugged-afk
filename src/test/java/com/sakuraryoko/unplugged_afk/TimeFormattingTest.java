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

package com.sakuraryoko.unplugged_afk;

import org.junit.jupiter.api.Test;

import com.sakuraryoko.unplugged_afk.impl.time.DurationFormat;
import com.sakuraryoko.unplugged_afk.impl.time.TimeFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeFormattingTest
{
    @Test
    void invalidDurationKeepsCoreLibSentinel()
    {
        assertEquals("Invalid Duration [0]", DurationFormat.PRETTY.format(0L));
        assertEquals("Invalid Duration [-5]", DurationFormat.REGULAR.format(-5L));
    }

    @Test
    void regularDurationIsHms()
    {
        // 1h 1m 1s
        assertEquals("01:01:01.000", DurationFormat.REGULAR.format(3_661_000L));
    }

    @Test
    void prettyDurationIsWords()
    {
        String out = DurationFormat.PRETTY.format(3_661_000L);
        assertTrue(out.contains("hour"), "expected words, got: " + out);
    }

    @Test
    void durationNamesRoundTrip()
    {
        for (DurationFormat format : DurationFormat.values())
        {
            assertEquals(format, DurationFormat.fromStringStatic(format.getName()));
            assertEquals(format, DurationFormat.fromStringStatic(format.name()));
        }
    }

    @Test
    void timeNamesRoundTrip()
    {
        for (TimeFormat format : TimeFormat.values())
        {
            assertEquals(format, TimeFormat.fromStringStatic(format.getName()));
            assertEquals(format, TimeFormat.fromStringStatic(format.name()));
        }
    }

    @Test
    void rfc1123IsParseableShape()
    {
        String out = TimeFormat.RFC1123.format(0L);
        assertTrue(out.contains("1970"), "expected epoch year, got: " + out);
    }
}
