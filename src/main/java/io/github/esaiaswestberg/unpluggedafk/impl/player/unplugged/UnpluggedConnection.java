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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/**
 * A {@link Connection} with no socket behind it, so a shadow player can be put
 * through {@code PlayerList#placeNewPlayer} exactly like a real login.
 *
 * <p>The Fabric build needed a mixin {@code @Accessor} to install the channel,
 * because {@code Connection#channel} is private in vanilla. Paper widens both
 * {@code channel} and {@code address} to public, so this is a plain assignment
 * and no reflection is required.
 *
 * <p>Every outbound path is a no-op: there is no client to receive packets, and
 * ticking the connection would run the game listener's keep-alive and idle
 * timeout, which would disconnect the shadow.
 */
@ApiStatus.Internal
public class UnpluggedConnection extends Connection
{
    private final EmbeddedChannel embedded;

    public UnpluggedConnection(PacketFlow receiving)
    {
        super(receiving);

        this.embedded = new EmbeddedChannel();
        this.channel = this.embedded;
        this.address = new InetSocketAddress("127.0.0.1", 65535);
    }

    /**
     * The single outbound sink -- {@code send(packet)} and
     * {@code send(packet, listener)} both delegate here.
     */
    @Override
    public void send(@NonNull Packet<?> packet, @Nullable ChannelFutureListener futureListener, boolean flush)
    {
    }

    @Override
    public void setReadOnly()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }

    @Override
    public void setListenerForServerboundHandshake(@NonNull PacketListener packetListener)
    {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NonNull ProtocolInfo<T> protocolInfo, @NonNull T packetListener)
    {
    }

    /**
     * NO-OP. Ticking would run {@code ServerGamePacketListenerImpl#tick()},
     * whose idle-timeout check would kick the shadow.
     *
     * <p>The outbound buffer is drained rather than left to grow, in case
     * anything writes to the channel directly instead of going through
     * {@link #send}.
     */
    @Override
    public void tick()
    {
        if (!this.embedded.outboundMessages().isEmpty())
        {
            this.embedded.releaseOutbound();
        }
    }

    @Override
    public @NonNull SocketAddress getRemoteAddress()
    {
        return this.address;
    }

    public EmbeddedChannel embeddedChannel()
    {
        return this.embedded;
    }
}
