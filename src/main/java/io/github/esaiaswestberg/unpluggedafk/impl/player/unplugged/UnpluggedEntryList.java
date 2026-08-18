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

package io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import net.minecraft.network.chat.Component;

import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedState;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedStatus;
import io.github.esaiaswestberg.unpluggedafk.impl.Log;

@ApiStatus.Internal
public class UnpluggedEntryList
{
    private static final UnpluggedEntryList INSTANCE = new UnpluggedEntryList();
    public static UnpluggedEntryList getInstance() { return INSTANCE; }
    private final ConcurrentHashMap<UUID, UnpluggedEntry> map;

    private UnpluggedEntryList()
    {
        this.map = new ConcurrentHashMap<>(16, 0.9f, 1);
    }

    public @Nullable UnpluggedEntry get(@Nonnull UnpluggedServerPlayer player)
    {
        return this.get(player.getUUID());
    }

    public @Nullable UnpluggedEntry get(UUID uuid)
    {
        if (this.map.containsKey(uuid))
        {
            return this.map.get(uuid);
        }

        return null;
    }

    public @Nullable UnpluggedEntry add(@Nonnull UnpluggedServerPlayer player, UnpluggedState state)
    {
        if (this.get(player) == null)
        {
            UnpluggedEntry entry = UnpluggedEntry.create(player);

            if (state.status() == UnpluggedStatus.ACTIVE)
            {
                entry.updateState(state);
            }

            this.map.put(player.getUUID(), entry);
            Log.debug("UnpluggedEntryList(): add({}) --> ADD", entry.name().getString());
            return entry;
        }

        return this.get(player);
    }

    public boolean contains(UUID uuid)
    {
        return this.map.containsKey(uuid);
    }

    public void updateFromUnplugged(@Nonnull UnpluggedServerPlayer player)
    {
        UUID uuid = player.getUUID();

        if (this.map.containsKey(uuid))
        {
            UnpluggedEntry entry = this.map.get(uuid);

            if (entry != null)
            {
                entry.setPlayer(player);
            }
        }
    }

    public void syncEntry(@Nonnull UnpluggedServerPlayer player, UnpluggedEntry entry)
    {
        UUID uuid = player.getUUID();

        if (entry.matches(uuid))
        {
            this.map.remove(uuid);
            entry.setPlayer(player);
            this.map.put(uuid, entry);
        }
    }

    public void remove(@Nonnull UUID uuid, boolean silent, UnpluggedStatus reason)
    {
        UnpluggedEntry entry = this.map.remove(uuid);

        if (entry != null)
        {
            this.map.remove(uuid);
            Log.debug("UnpluggedEntryList(): remove({}) --> REMOVE", entry.name().getString());
            entry.handler().unregisterUnpluggedAfk(silent, reason);
        }
    }

    public void remove(@Nonnull UnpluggedServerPlayer player, boolean silent, UnpluggedStatus reason)
    {
        this.remove(player.getUUID(), silent, reason);
    }

    @VisibleForTesting
    public ImmutableMap<UUID, UnpluggedEntry> shadowMapCopy()
    {
        ImmutableMap.Builder<UUID, UnpluggedEntry> builder = ImmutableMap.builder();
        this.map.forEach(builder::put);
        return builder.build();
    }

    @VisibleForTesting
    public Component getDebugFormatted(UUID uuid)
    {
        if (this.map.containsKey(uuid))
        {
            UnpluggedEntry entry = this.map.get(uuid);

            if (entry != null)
            {
                return entry.debugFormatted();
            }
        }

        return Component.literal("§cUnplugged Player not found§r");
    }
}
