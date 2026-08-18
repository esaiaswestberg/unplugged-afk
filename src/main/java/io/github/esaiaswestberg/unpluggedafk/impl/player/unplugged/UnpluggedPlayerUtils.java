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

import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.waypoints.ServerWaypointManager;

import io.github.esaiaswestberg.unpluggedafk.api.event.UnpluggedRespawnEvent;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;
import io.github.esaiaswestberg.unpluggedafk.impl.events.PlayerEventsHandler;
import io.github.esaiaswestberg.unpluggedafk.impl.player.PlayerManager;
import io.github.esaiaswestberg.unpluggedafk.impl.player.wrap.ProfileWrap;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedState;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedStatus;
import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Text;

@ApiStatus.Internal
public class UnpluggedPlayerUtils
{
    @ApiStatus.Internal
    public static boolean ensureSafeForUUID(@Nonnull MinecraftServer server, @Nonnull UUID uuid)
    {
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        boolean isSafe = true;

        for (ServerPlayer player : players)
        {
            if (player.getUUID().equals(uuid))
            {
                isSafe = false;
                break;
            }
        }

        return isSafe;
    }

    @ApiStatus.Internal
    public static ImmutableList<UnpluggedServerPlayer> getShadows(@Nonnull MinecraftServer server)
    {
        ImmutableList.Builder<UnpluggedServerPlayer> builder = ImmutableList.builder();
        PlayerList pl = server.getPlayerList();
        List<ServerPlayer> players = pl.getPlayers();

        for (ServerPlayer player : players)
        {
            if (player instanceof UnpluggedServerPlayer sp)
            {
                builder.add(sp);
            }
        }

        return builder.build();
    }

    @ApiStatus.Internal
    public static void hideAllUnpluggedFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
    {
        if (ConfigWrap.unplugged().unpluggedHidePlayer)
        {
            ImmutableList<UnpluggedServerPlayer> shadows = getShadows(server);
            boolean result = false;

            if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
            {
                result = true;
            }
            else if (!isOpWrap(player))
            {
                result = true;
            }

            if (result)
            {
                for (UnpluggedServerPlayer shadow : shadows)
                {
                    sendRemovePacketToPlayerWrap(shadow, player);
                }
            }
        }
    }

    @ApiStatus.Internal
    public static void unhideAllUnpluggedFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
    {
        if (!ConfigWrap.unplugged().unpluggedHidePlayer ||
            (!ConfigWrap.unplugged().unpluggedHideFromOps) && isOpWrap(player))
        {
            ImmutableList<UnpluggedServerPlayer> shadows = getShadows(server);

            for (UnpluggedServerPlayer shadow : shadows)
            {
                sendAddPacketToPlayerWrap(shadow, player);

                // Note, that the difference between hiding from
                // Ops vs all players; is indistinguishable for Waypoints

                if (!ConfigWrap.unplugged().unpluggedHidePlayer)
                {
                    player.level().getWaypointManager().addPlayer(shadow);
                }
            }
        }
    }

    @ApiStatus.Internal
    protected static void sendHidePlayerPacket(@Nonnull MinecraftServer server, @Nonnull UnpluggedServerPlayer sp)
    {
        if (ConfigWrap.unplugged().unpluggedHidePlayer)
        {
            PlayerList pl = server.getPlayerList();
            List<ServerPlayer> players = pl.getPlayers();

            for (ServerPlayer player : players)
            {
                boolean result = false;

                if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
                {
                    result = true;
                }
                else if (!isOpWrap(player))
                {
                    result = true;
                }

                if (result)
                {
                    sendRemovePacketToPlayerWrap(sp, player);
                }
            }
        }

        applyWaypointVisibility(sp);
    }

    /**
     * Keeps the shadow off (or on) the locator bar.
     *
     * <p>The Fabric build hooked {@code ServerWaypointManager#addPlayer} and
     * {@code updatePlayer} with a mixin. Paper fires no event there, but both
     * methods are public in 26.2, so visibility is simply reasserted -- at spawn
     * and on the shadow's tick cycle, since movement re-adds the waypoint.
     */
    @ApiStatus.Internal
    public static void applyWaypointVisibility(@Nonnull UnpluggedServerPlayer sp)
    {
        if (!sp.isValid())
        {
            return;
        }

        ServerWaypointManager manager = sp.level().getWaypointManager();

        if (manager == null)
        {
            return;
        }

        if (ConfigWrap.unplugged().unpluggedHidePlayer)
        {
            // Hiding from ops versus everyone is indistinguishable for waypoints,
            // so any hiding at all takes the shadow off the bar.
            manager.removePlayer(sp);
        }
        else
        {
            manager.addPlayer(sp);
        }
    }

    @ApiStatus.Internal
    protected static boolean isOpWrap(@Nonnull ServerPlayer player)
    {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @ApiStatus.Internal
    protected static void sendAddPacketToPlayerWrap(@Nonnull UnpluggedServerPlayer sp, @Nonnull ServerPlayer player)
    {
        player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(sp)));
        player.connection.send(ClientboundPlayerInfoUpdatePacket.updateListed(sp.getUUID(), true));
    }

    @ApiStatus.Internal
    protected static void sendRemovePacketToPlayerWrap(@Nonnull UnpluggedServerPlayer sp, @Nonnull ServerPlayer player)
    {
        player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sp.getUUID())));
    }

    @ApiStatus.Internal
    public static void onAddOrUpdateWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
    {
        if (ConfigWrap.unplugged().unpluggedHidePlayer && player instanceof UnpluggedServerPlayer sp)
        {
            if (sp.isValid())
            {
                boolean result = false;

                if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
                {
                    result = true;
                }
                else if (!isOpWrap(player))
                {
                    result = true;
                }

                if (result)
                {
                    manager.removePlayer(player);
                }
            }
        }
    }

    @ApiStatus.Internal
    public static void onUnhideWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
    {
        if (!ConfigWrap.unplugged().unpluggedHidePlayer && player instanceof UnpluggedServerPlayer sp)
        {
            if (sp.isValid())
            {
                manager.addPlayer(player);
            }
        }
    }

    /**
     * Evicts a shadow standing in for a player who is logging back in.
     *
     * <p>The Fabric build reached this from a mixin wrapping
     * {@code PlayerList#canPlayerLogin}. On Paper the caller is a
     * {@code PlayerConnectionValidateLoginEvent} listener, which fires at the
     * same point -- immediately before {@code disconnectAllPlayersWithProfile}.
     *
     * @return true if a shadow was found and evicted
     */
    @ApiStatus.Internal
    public static boolean evictShadowForLogin(@Nonnull MinecraftServer server, @Nonnull UUID uuid, String name)
    {
        PlayerList playerList = server.getPlayerList();
        ServerPlayer existing = playerList.getPlayer(uuid);

        if (!(existing instanceof UnpluggedServerPlayer))
        {
            return false;
        }

        Log.debug("evictShadowForLogin(): evicting shadow for ['{}'/{}]", name, uuid);
        checkForUnpluggedAtPreLogin(playerList, ProfileWrap.profile(uuid, name), existing);

        return true;
    }

    @ApiStatus.Internal
    public static void checkForUnpluggedAtPreLogin(PlayerList playerList, GameProfile profile, ServerPlayer player)
    {
        if (player instanceof UnpluggedServerPlayer sp)
        {
            if (sp.isValid())
            {
                UnpluggedEntry entry = UnpluggedEntryList.getInstance().get(sp);

                if (entry != null)
                {
                    UnpluggedEntryList.getInstance().remove(sp, false, UnpluggedStatus.INTERRUPTED);
                }

                final long delta = getStartTimeDelta(sp.getStartTime());
                final String reason = ConfigWrap.mess().unpluggedUnsuccessful
                        + (ConfigWrap.mess().displayDuration
                           ? ConfigWrap.mess().unpluggedUnsuccessfulPrefix
                             + ConfigWrap.mess().duration.option.format(delta)
                        : "")
                        + ConfigWrap.mess().unpluggedUnsuccessfulPunctuation
                        + ConfigWrap.mess().unpluggedReplaced;

                UnpluggedState newState = new UnpluggedState(UnpluggedStatus.INTERRUPTED, -1, -1, -1L, reason);
                PlayerManager.getInstance().setState(profile, newState);
            }

            if (player.isInvulnerable() && player.gameMode.isSurvival())
            {
                player.setInvulnerable(false);
            }

            final String name = ProfileWrap.name(profile);

            if (ConfigWrap.mess().hideUnpluggedJoin)
            {
                PlayerEventsHandler.getInstance().addShouldHideJoin(name);
            }

            // Immediate: the returning player's login cannot wait a tick for the
            // shadow to go away. kill() performs the removal itself.
            String str = ConfigWrap.mess().unpluggedReplaced;
            sp.kill(Text.of(str), true);
        }
    }

    @ApiStatus.Internal
    public static void respawnUnpluggedAfk(GameProfile profile, UnpluggedServerPlayer oldSp, UnpluggedServerPlayer newSp)
    {
        newSp.updateTimeOut(oldSp.getTimeout());
        UnpluggedEntryList.getInstance().updateFromUnplugged(newSp);
        UnpluggedEntry entry = UnpluggedEntryList.getInstance().get(newSp);
        UnpluggedState state = PlayerManager.getInstance().getState(profile);
        UnpluggedState oldState = oldSp.toState();
        UnpluggedState newState;
//      final long now = System.currentTimeMillis();
        boolean dirty = false;

        if (state.status() == UnpluggedStatus.ACTIVE && oldState.status() != UnpluggedStatus.ACTIVE)
        {
            newState = state;
            dirty = true;
        }
        else if (oldState.status() == UnpluggedStatus.ACTIVE && state.status() != UnpluggedStatus.ACTIVE)
        {
            newState = oldState;
            dirty = true;
        }
//      else if (state.status() != UnpluggedStatus.ACTIVE)
//      {
//          newState = new UnpluggedState(UnpluggedStatus.ACTIVE, state.time(), oldSp.getTimeout(), now, state.reason());
//          dirty = true;
//      }
        else
        {
            newState = oldState;
        }

        if (dirty)
        {
            PlayerManager.getInstance().setState(profile, newState);

            if (entry != null)
            {
                entry.updateState(newState);
            }
        }

        newSp.fromState(newState);
        new UnpluggedRespawnEvent(ProfileWrap.id(profile), newState.copy()).callEvent();
    }

    @ApiStatus.Internal
    public static long getStartTimeDelta(final long startTime)
    {
        return (System.currentTimeMillis() - startTime);
    }

    @ApiStatus.Internal
    public static boolean matchesJoinPattern(Component message)
    {
//      Log.debug("matchesJoinPattern(): {}", message.getString());
        if (message.getContents() instanceof TranslatableContents text)
        {
            String key = text.getKey();
            return (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.joined.renamed") || key.equals("multiplayer.player.left"));
        }

        return false;
    }

    @ApiStatus.Internal
    public static void sendJoinMessage(MinecraftServer server, Component name)
    {
        if (!ConfigWrap.mess().hideUnpluggedJoin)
        {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    @ApiStatus.Internal
    public static void sendLeaveMessage(MinecraftServer server, Component name)
    {
        if (!ConfigWrap.mess().hideUnpluggedJoin)
        {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW), false);
        }
    }
}
