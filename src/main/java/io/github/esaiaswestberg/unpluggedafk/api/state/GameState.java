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

package io.github.esaiaswestberg.unpluggedafk.api.state;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import io.github.esaiaswestberg.unpluggedafk.impl.nms.Text;
import io.github.esaiaswestberg.unpluggedafk.impl.player.wrap.GameWrap;

/**
 * GameState - Wrapper around storing these Player values
 *
 * @param gameMode Game Mode
 * @param flying isFlying
 */
public record GameState(String gameMode, boolean flying)
{
    @Override
    public @NonNull String toString()
    {
        return "GameState{gameType="+this.gameMode+",flying="+this.flying+"}";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        GameState gameState = (GameState) o;
        return this.gameMode.equals(gameState.gameMode()) && this.flying == gameState.flying;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.gameMode, this.flying);
    }

    public boolean isEmpty()
    {
        return GameWrap.defMode().equals(this);
    }

    public Component getDebugFormatted()
    {
        MutableComponent text = Text.empty();

        text.append(Text.of("§r "))
            .append(Text.of(String.format("§b%s§r", this.gameMode())))
            .append(Text.of(" / F: "))
            .append(Text.of(String.format("§e%s§r", this.flying())));

        return text;
    }

    public GameState copy()
    {
        return new GameState(this.gameMode, this.flying);
    }
}
