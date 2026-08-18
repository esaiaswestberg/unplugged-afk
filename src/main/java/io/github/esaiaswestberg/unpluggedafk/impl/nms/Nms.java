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

package io.github.esaiaswestberg.unpluggedafk.impl.nms;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bridge between the Bukkit API and NMS.
 *
 * <p>Replaces the {@code IPlayerInvoker} mixin interface the Fabric build used
 * to reach {@code ServerPlayer}'s private {@code server} and {@code connection}
 * fields: on Paper {@code connection} is public and the server is reachable
 * from the Bukkit singleton.
 */
@ApiStatus.Internal
public class Nms
{
    public static MinecraftServer server()
    {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static ServerPlayer handle(Player player)
    {
        return ((CraftPlayer) player).getHandle();
    }

    public static @Nullable Player bukkit(@Nullable ServerPlayer player)
    {
        return player == null ? null : player.getBukkitEntity();
    }
}
