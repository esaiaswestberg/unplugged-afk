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

import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;

/**
 * Play-phase listener for a shadow player.
 *
 * <p>The Fabric build substituted this for the vanilla listener with a mixin on
 * {@code PlayerList#placeNewPlayer}. On Paper it is constructed immediately
 * after {@code placeNewPlayer} returns instead: the
 * {@link ServerGamePacketListenerImpl} constructor assigns
 * {@code player.connection = this}, so it self-installs.
 *
 * <p>Paper routes every synchronous kick through
 * {@link #disconnect(DisconnectionDetails)} (which is where it fires
 * {@code PlayerKickEvent}) and every asynchronous one through
 * {@link #disconnectAsync(DisconnectionDetails)}, so both are intercepted --
 * overriding only {@code disconnect(Component)} as the Fabric build did would
 * miss most call sites.
 */
@ApiStatus.Internal
public class UnpluggedGamePacketListener extends ServerGamePacketListenerImpl
{
    public UnpluggedGamePacketListener(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie)
    {
        super(server, connection, player, cookie);
    }

    @Override
    public void disconnect(@NonNull Component message)
    {
        this.handleDisconnect(message);
    }

    @Override
    public void disconnect(@NonNull DisconnectionDetails details)
    {
        this.handleDisconnect(details.reason());
    }

    @Override
    public void disconnectAsync(@NonNull DisconnectionDetails details)
    {
        this.handleDisconnect(details.reason());
    }

    private void handleDisconnect(Component message)
    {
        Log.debug("UnpluggedGamePacketListener#disconnect(): message: {}", message.getString());

        if (!(this.player instanceof UnpluggedServerPlayer sp) || !sp.isValid())
        {
            return;
        }

        if (message.getContents() instanceof TranslatableContents text &&
            (text.getKey().equals("multiplayer.disconnect.idling") ||
             text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            sp.kill(message);
            return;
        }

        if (!ConfigWrap.unplugged().resetHealthUponDeath)
        {
            sp.kill(message);
        }
    }

    /**
     * There is no client to acknowledge a teleport, so the server-side chunk
     * tracker is resynced by hand.
     */
    @Override
    public void teleport(@NonNull PositionMoveRotation position, @NonNull Set<Relative> relativeSet)
    {
        super.teleport(position, relativeSet);

        if (this.player.level().getPlayerByUUID(this.player.getUUID()) != null)
        {
            this.resetPosition();
            this.player.level().getChunkSource().move(this.player);
        }
    }
}
