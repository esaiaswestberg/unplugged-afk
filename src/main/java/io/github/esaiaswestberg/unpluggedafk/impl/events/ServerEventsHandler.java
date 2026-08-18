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

package io.github.esaiaswestberg.unpluggedafk.impl.events;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.player.PlayerManager;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedPendingSpawns;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedPlayerUtils;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedServerPlayer;

/**
 * Server lifecycle and the timed cycles that used to hang off the
 * {@code MixinMinecraftServer} {@code tickServer} TAIL injection. On Paper
 * {@link TickDriver} feeds {@link #onTick} from {@code ServerTickEndEvent},
 * which is the same position in the tick.
 */
@ApiStatus.Internal
public class ServerEventsHandler
{
    private static final ServerEventsHandler INSTANCE = new ServerEventsHandler();

    public static ServerEventsHandler getInstance() { return INSTANCE; }

    private static final float TICK_RATE = 30.0f;
    private boolean tickingLock;
    private boolean spawnSafe;
    private boolean hideAllPlayers;
    private boolean unhideAllPlayers;
    private boolean serverStopping;
    private long startupTime;
    private long lastTick;

    private ServerEventsHandler()
    {
        this.init();
        this.startupTime = System.currentTimeMillis();
        this.lastTick = this.startupTime;
    }

    private void init()
    {
        this.tickingLock = true;
        this.spawnSafe = false;
        this.unhideAllPlayers = false;
        this.hideAllPlayers = false;
        this.serverStopping = false;
    }

    public void onStarting(MinecraftServer server)
    {
        this.init();
        this.startupTime = System.currentTimeMillis();
        this.lastTick = this.startupTime;
    }

    public void onStarted(MinecraftServer server)
    {
        this.tickingLock = true;
        PlayerManager.getInstance().onServerStarted(server);
    }

    private long tickRate()
    {
        return (long) (TICK_RATE * 1000L);
    }

    public void onTick(MinecraftServer server)
    {
        // Every Tick -->
        UnpluggedPendingSpawns.INSTANCE.tick(server);
        final long now = System.currentTimeMillis();

        // Hold additional tick tasks until server has been running for at least 1 tick cycle.
        if (this.tickingLock)
        {
            if ((now - this.startupTime) > this.tickRate())
            {
                this.tickingLock = false;
            }

            PlayerManager.getInstance().onTick(server, false);
            return;
        }

        PlayerManager.getInstance().onTick(server, this.isSpawnSafe());

        if ((now - this.lastTick) > this.tickRate())
        {
            if (this.hideAllPlayers || this.unhideAllPlayers)
            {
                this.processAllHideOrUnhide(server);
            }

            this.lastTick = now;
        }
    }

    @ApiStatus.Internal
    private void processAllHideOrUnhide(MinecraftServer server)
    {
        PlayerList pl = server.getPlayerList();
        List<ServerPlayer> players = pl.getPlayers();

        Log.debug("processAllHideOrUnhide()");

        // From changing the config options
        for (ServerPlayer player : players)
        {
            if (this.hideAllPlayers && !(player instanceof UnpluggedServerPlayer))
            {
                UnpluggedPlayerUtils.hideAllUnpluggedFromPlayer(server, player);
            }
            else if (this.unhideAllPlayers && !(player instanceof UnpluggedServerPlayer))
            {
                UnpluggedPlayerUtils.unhideAllUnpluggedFromPlayer(server, player);
            }
        }

        this.hideAllPlayers = false;
        this.unhideAllPlayers = false;
    }

    public void onStopping(MinecraftServer server)
    {
        this.tickingLock = true;
        this.serverStopping = true;
        this.toggleSpawnSafe(false);
        this.toggleHideAllPlayers(false);
        this.toggleUnhideAllPlayers(false);

        PlayerManager.getInstance().onServerStop(server);
    }

    @ApiStatus.Internal
    public boolean isSpawnSafe()
    {
        return this.spawnSafe;
    }

    @ApiStatus.Internal
    public void toggleSpawnSafe(boolean toggle)
    {
        this.spawnSafe = toggle;
    }

    @ApiStatus.Internal
    public void toggleHideAllPlayers(boolean toggle)
    {
        this.hideAllPlayers = toggle;
    }

    @ApiStatus.Internal
    public void toggleUnhideAllPlayers(boolean toggle)
    {
        this.unhideAllPlayers = toggle;
    }

    @ApiStatus.Internal
    public boolean isServerStopping()
    {
        return this.serverStopping;
    }
}
