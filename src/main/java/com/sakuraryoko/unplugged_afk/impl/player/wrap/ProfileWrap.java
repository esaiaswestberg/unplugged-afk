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

package com.sakuraryoko.unplugged_afk.impl.player.wrap;

import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.NameAndId;

@ApiStatus.Internal
public class ProfileWrap
{
    public static UUID id(GameProfile profile)
    {
        return profile.id();
    }

    public static String name(GameProfile profile)
    {
        return profile.name();
    }

    public static GameProfile profile(NameAndId nameAndId)
    {
        return new GameProfile(nameAndId.id(), nameAndId.name());
    }

    public static NameAndId nameAndId(GameProfile profile)
    {
        return new NameAndId(profile.id(), profile.name());
    }

    public static GameProfile profile(UUID id, String name)
    {
        return new GameProfile(id, name);
    }
}
