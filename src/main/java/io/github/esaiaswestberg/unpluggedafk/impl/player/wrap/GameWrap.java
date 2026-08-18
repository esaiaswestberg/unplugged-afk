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

package io.github.esaiaswestberg.unpluggedafk.impl.player.wrap;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import io.github.esaiaswestberg.unpluggedafk.api.state.GameState;

@ApiStatus.Internal
public class GameWrap
{
    public static GameState defMode()
    {
        return new GameState(GameType.DEFAULT_MODE.getName(), false);
    }

    public static GameState of(@Nonnull ServerPlayer player)
    {
        return new GameState(player.gameMode.getGameModeForPlayer().getName(), player.getAbilities().flying);
    }
}
