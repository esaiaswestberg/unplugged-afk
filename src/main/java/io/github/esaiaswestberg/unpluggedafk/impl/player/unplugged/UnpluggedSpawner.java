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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.UnpluggedAfkPlugin;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Nms;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Text;

/**
 * Drives the two-step unplug handshake that Paper forces.
 *
 * <p>On Fabric the mod could do {@code PlayerList#remove} then
 * {@code connection.disconnect} back-to-back and place the shadow immediately.
 * Paper's {@code disconnect0} instead sets
 * {@code Connection#handleConnectionDisconnectOnNextTick}, so the real removal
 * happens a tick later. Placing the shadow first would let that deferred
 * removal fire a second {@code PlayerQuitEvent} and broadcast a player-info
 * remove for the shadow's own UUID.
 *
 * <p>So: capture state, kick, wait for {@code PlayerQuitEvent}, then spawn on
 * the following tick -- by which point {@code PlayerList#remove} has also
 * written the player NBT that {@code loadPlayerNbt} reads back.
 */
@ApiStatus.Internal
public class UnpluggedSpawner
{
    private static final UnpluggedSpawner INSTANCE = new UnpluggedSpawner();

    public static UnpluggedSpawner getInstance() { return INSTANCE; }

    private final Map<UUID, Capture> pending = new ConcurrentHashMap<>();

    private UnpluggedSpawner() {}

    /**
     * Everything about the departing player that the shadow needs, captured
     * before the disconnect tears the player down.
     */
    public record Capture(GameProfile profile,
                          ServerLevel level,
                          ClientInformation clientInformation,
                          @Nullable RemoteChatSession chatSession,
                          double x, double y, double z,
                          float yaw, float pitch,
                          float health,
                          GameType gameType,
                          boolean flying,
                          byte skinLayers,
                          int time,
                          long timeout,
                          String reason)
    {
    }

    /** Step one: capture, then kick. */
    public void scheduleFromPlayer(ServerPlayer player, int time, long timeout, String reason, Component kickMsg)
    {
        Capture capture = new Capture(
                player.getGameProfile(),
                player.level(),
                player.clientInformation(),
                player.getChatSession(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.getHealth(),
                player.gameMode.getGameModeForPlayer(),
                player.getAbilities().flying,
                player.getEntityData().get(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION),
                time, timeout, reason
        );

        this.pending.put(player.getUUID(), capture);
        Log.debug("UnpluggedSpawner#scheduleFromPlayer(): captured '{}'", capture.profile().name());

        Player bukkit = player.getBukkitEntity();
        bukkit.kick(Text.adventure(kickMsg.getString()), PlayerKickEvent.Cause.PLUGIN);
    }

    public boolean isPending(UUID uuid)
    {
        return this.pending.containsKey(uuid);
    }

    /**
     * Step two, called from the {@code PlayerQuitEvent} listener. The spawn is
     * deferred one tick so that {@code PlayerList#remove} has finished saving
     * the player NBT.
     */
    public void onQuit(UUID uuid)
    {
        Capture capture = this.pending.remove(uuid);

        if (capture == null)
        {
            return;
        }

        UnpluggedAfkPlugin plugin = UnpluggedAfkPlugin.getInstance();

        if (plugin == null)
        {
            return;
        }

        Log.debug("UnpluggedSpawner#onQuit(): scheduling spawn for '{}'", capture.profile().name());

        Bukkit.getScheduler().runTask(plugin, () ->
        {
            try
            {
                UnpluggedServerPlayer shadow = UnpluggedServerPlayer.spawnFromCapture(Nms.server(), capture);

                if (shadow == null)
                {
                    Log.warn("UnpluggedSpawner: failed to spawn a shadow for '{}'", capture.profile().name());
                }
                else
                {
                    Log.debug("UnpluggedSpawner: spawned shadow for '{}'", capture.profile().name());
                }
            }
            catch (Exception e)
            {
                Log.error("UnpluggedSpawner: error spawning a shadow for '" + capture.profile().name() + "'", e);
            }
        });
    }

    /** Dropped without spawning, e.g. the plugin is shutting down. */
    public void cancel(UUID uuid)
    {
        this.pending.remove(uuid);
    }

    public boolean hideJoinFor(UUID uuid)
    {
        return ConfigWrap.mess().hideUnpluggedJoin && this.pending.containsKey(uuid);
    }
}
