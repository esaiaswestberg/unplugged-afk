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

package io.github.esaiaswestberg.unpluggedafk.api;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedState;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedStatus;
import io.github.esaiaswestberg.unpluggedafk.impl.player.PlayerManager;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedEntry;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedEntryList;

/**
 * Read-only view of unplugged sessions, for other plugins.
 *
 * <p>Lifecycle notifications are Bukkit events rather than callbacks: see
 * {@link io.github.esaiaswestberg.unpluggedafk.api.event.UnpluggedStartEvent},
 * {@link io.github.esaiaswestberg.unpluggedafk.api.event.UnpluggedRespawnEvent}
 * and {@link io.github.esaiaswestberg.unpluggedafk.api.event.UnpluggedEndEvent}.
 *
 * <p>Call these from the main server thread; the underlying state is not
 * synchronised for concurrent readers.
 */
public interface UnpluggedAfkAPI
{
    /**
     * Whether a player currently has a live unplugged session.
     *
     * @param uuid the player's UUID
     * @return true only while the session is {@link UnpluggedStatus#ACTIVE}
     */
    static boolean isUnplugged(@Nonnull UUID uuid)
    {
        Optional<UnpluggedStatus> opt = getUnpluggedStatus(uuid);
        return opt.map(s -> s.equals(UnpluggedStatus.ACTIVE)).orElse(false);
    }

    /**
     * The status of a player's most recent session, which may describe how it
     * ended rather than a live one.
     *
     * @param uuid the player's UUID
     * @return the status, or empty if the player is not tracked
     */
    static Optional<UnpluggedStatus> getUnpluggedStatus(@Nonnull UUID uuid)
    {
        Optional<UnpluggedState> opt = getUnpluggedState(uuid);
        return opt.map(UnpluggedState::status);
    }

    /**
     * The full state of a player's session: status, configured duration,
     * remaining timeout, start time and the reason it ended.
     *
     * <p>A live shadow is preferred as the source; otherwise the persisted
     * state is returned.
     *
     * @param uuid the player's UUID
     * @return the state, or empty if the player is not tracked
     */
    static Optional<UnpluggedState> getUnpluggedState(@Nonnull UUID uuid)
    {
        Optional<UnpluggedEntry> opt = Optional.ofNullable(UnpluggedEntryList.getInstance().get(uuid));
        return opt.map(UnpluggedEntry::toState)
                  .or(() -> Optional.ofNullable(PlayerManager.getInstance().getState(uuid)));
    }
}
