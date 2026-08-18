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

package com.sakuraryoko.unplugged_afk.impl.player;

import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.api.state.GameState;
import com.sakuraryoko.unplugged_afk.api.state.PosState;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;
import com.sakuraryoko.unplugged_afk.impl.nms.Text;

@ApiStatus.Internal
public record PlayerEntry(UUID uuid, String name, UnpluggedState state, PosState pos, GameState game)
{
    public PlayerEntry updateState(UnpluggedState state)
    {
        return new PlayerEntry(this.uuid(), this.name(), state, this.pos(), this.game());
    }

    public PlayerEntry updatePosAndGame(PosState pos, GameState game)
    {
        return new PlayerEntry(this.uuid(), this.name(), this.state(), pos, game);
    }

    public PlayerEntry updatePlayerData(String name, PosState pos, GameState game)
    {
        return new PlayerEntry(this.uuid(), name, this.state(), pos, game);
    }

    public PlayerEntry updateAll(UnpluggedState state, PosState pos, GameState game)
    {
        return new PlayerEntry(this.uuid(), this.name(), state, pos, game);
    }

    public PlayerEntry updateAll(UnpluggedState state, String name, PosState pos, GameState game)
    {
        return new PlayerEntry(this.uuid(), name, state, pos, game);
    }

    @Override
    public @NonNull String toString()
    {
        return "PlayerEntry{name="+this.name()+",uuid="+this.uuid().toString()+",[state="+this.state().toString()+"],[pos="+this.pos().toString()+"],[game="+this.game().toString()+"]}";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        PlayerEntry playerEntry = (PlayerEntry) o;

        if (this.uuid.equals(playerEntry.uuid))
        {
            return  this.name().equals(playerEntry.name()) &&
                    this.game().equals(playerEntry.game()) &&
                    this.state().equals(playerEntry.state()) &&
                    this.pos().equals(playerEntry.pos());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        int result = this.uuid.hashCode();
        result = 31 * result + this.name().hashCode();
        result = 31 * result + this.state().hashCode();
        result = 31 * result + this.pos().hashCode();
        result = 31 * result + this.game().hashCode();
        return result;
    }

    public boolean matches(UUID uuid)
    {
        return this.uuid.equals(uuid);
    }

    public Component getDebugFormatted()
    {
        MutableComponent text = Component.literal("");

        if (!ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            text.append(
                    Text.of("\n - §7Name: ")
            ).append(
                    PlayerUtils.formatSuggestSpawnCommand(this.name())
            );
            text.append(
                    Text.of("\n - §7UUID: ")
            ).append(
                    Text.of(this.uuid().toString())
            ).append(
                    Text.of("\n - §7Game: ")
            ).append(
                    this.game().getDebugFormatted()
            );
            text.append(
                    Text.of("\n - §7Position: ")
            ).append(
                    this.pos().getDebugFormatted()
            );
            text.append(
                    Text.of("\n - §7State: ")
            ).append(
                    this.state().getDebugFormatted()
            );
        }

        return text;
    }
}
