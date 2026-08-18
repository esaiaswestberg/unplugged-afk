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

package io.github.esaiaswestberg.unpluggedafk;

import org.junit.jupiter.api.Test;

import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedState;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnpluggedStateTest
{
    @Test
    void equalsComparesOnlyStatusAndTime()
    {
        UnpluggedState a = new UnpluggedState(UnpluggedStatus.ACTIVE, 10, 1000L, 5L, "a");
        UnpluggedState b = new UnpluggedState(UnpluggedStatus.ACTIVE, 10, 9999L, 77L, "b");
        UnpluggedState c = new UnpluggedState(UnpluggedStatus.EXPIRED, 10, 1000L, 5L, "a");

        assertEquals(a, b, "timeout/startTime/reason must not affect equality");
        assertNotEquals(a, c, "status must affect equality");
    }

    @Test
    void defaultStateIsEmpty()
    {
        assertTrue(UnpluggedState.DEFAULT.isEmpty());
    }

    @Test
    void ensureValidRepairsHandEditedActiveState()
    {
        UnpluggedState broken = new UnpluggedState(UnpluggedStatus.ACTIVE, 0, 0L, 0L, "");
        UnpluggedState fixed = broken.ensureValid();

        assertEquals(5, fixed.time());
        assertEquals(5L * 60L * 1000L, fixed.timeout());
        assertTrue(fixed.startTime() > 0L);
    }

    @Test
    void ensureValidLeavesInactiveStateAlone()
    {
        UnpluggedState inactive = new UnpluggedState(UnpluggedStatus.INACTIVE, 0, 0L, 0L, "");
        assertEquals(inactive, inactive.ensureValid());
    }
}
