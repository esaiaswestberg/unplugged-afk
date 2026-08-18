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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedState;
import io.github.esaiaswestberg.unpluggedafk.api.state.UnpluggedStatus;
import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Text;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedPlayerUtils;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedServerPlayer;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;
import io.github.esaiaswestberg.unpluggedafk.impl.nms.Nms;
import io.github.esaiaswestberg.unpluggedafk.impl.player.PlayerManager;
import io.github.esaiaswestberg.unpluggedafk.impl.player.unplugged.UnpluggedSpawner;

/**
 * Bukkit event listeners that stand in for several of the Fabric mixins.
 *
 * <p>Join and quit message suppression in particular is cleaner here than it
 * was on Fabric: Paper only broadcasts when the event's message is non-null, so
 * nulling it suppresses the broadcast outright, with none of the translation
 * key and name matching the {@code messageSuppress} mixins needed.
 */
@ApiStatus.Internal
public class BukkitBridge implements Listener
{
    public static void register(Plugin plugin)
    {
        Bukkit.getPluginManager().registerEvents(new BukkitBridge(), plugin);
    }

    /** Step two of the unplug handshake -- see {@link UnpluggedSpawner}. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Log.debug("BukkitBridge#onQuit(): '{}' quit (pending unplug: {})",
                  player.getName(), UnpluggedSpawner.getInstance().isPending(player.getUniqueId()));

        if (UnpluggedSpawner.getInstance().isPending(player.getUniqueId()))
        {
            // Both the player's own quit line and the shadow's follow-up join
            // line are governed by the same config option.
            if (ConfigWrap.mess().hideUnpluggedJoin)
            {
                event.quitMessage(null);
                PlayerEventsHandler.getInstance().addShouldHideJoin(player.getName());
            }

            Log.debug("BukkitBridge#onQuit(): '{}' quit while unplugging, handing to the spawner", player.getName());
            UnpluggedSpawner.getInstance().onQuit(player.getUniqueId());
        }
    }

    /**
     * Suppresses the shadow's own join broadcast, hides existing shadows from
     * the arriving player, and replays why their last session ended.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        ServerPlayer handle = Nms.handle(player);

        if (PlayerEventsHandler.getInstance().shouldHideJoin(player.getName()))
        {
            event.joinMessage(null);
            PlayerEventsHandler.getInstance().removeShouldHideJoin(player.getName());
            Log.debug("BukkitBridge#onJoin(): suppressed join broadcast for '{}'", player.getName());
        }

        PlayerManager.getInstance().syncProfile(handle.getGameProfile());
        PlayerManager.getInstance().updatePlayerData(handle);

        if (handle instanceof UnpluggedServerPlayer)
        {
            return;
        }

        // Shadows already standing around must be hidden from the new arrival
        // too, not just from players who were online when the option was set.
        UnpluggedPlayerUtils.hideAllUnpluggedFromPlayer(Nms.server(), handle);

        // Tell them why their previous session ended, then clear it.
        UnpluggedState state = PlayerManager.getInstance().getState(player.getUniqueId());

        if (state.status() != UnpluggedStatus.INACTIVE && !state.reason().isEmpty())
        {
            if (ConfigWrap.mess().displayReturnFeedback)
            {
                Log.debug("BukkitBridge#onJoin(): informing '{}' of [{}] status", player.getName(), state.status().name());
                handle.sendSystemMessage(Text.of(state.reason()));
            }

            PlayerManager.getInstance().resetState(handle);
        }
    }

    /**
     * Evicts a shadow just before Paper's duplicate-login check.
     *
     * <p>This fires from {@code handleLoginResult}, immediately before
     * {@code disconnectAllPlayersWithProfile} -- the same point the Fabric
     * build's mixin wrapped. Without it the shadow is still killed as a
     * duplicate, but the session is never recorded as INTERRUPTED and the
     * returning player gets no explanation.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent event)
    {
        if (!(event.getConnection() instanceof PlayerLoginConnection login))
        {
            return;
        }

        PlayerProfile profile = login.getAuthenticatedProfile();
        UUID uuid = profile != null ? profile.getId() : null;

        if (uuid == null)
        {
            return;
        }

        final String name = profile.getName() != null ? profile.getName() : uuid.toString();
        final boolean[] evicted = { false };
        Runnable task = () -> evicted[0] = UnpluggedPlayerUtils.evictShadowForLogin(Nms.server(), uuid, name);

        // Login is not necessarily processed on the main thread, and the shadow
        // has to be gone before the duplicate check runs, so this blocks.
        if (Bukkit.isPrimaryThread())
        {
            task.run();
        }
        else
        {
            Nms.server().executeBlocking(task);
        }

        // The shadow may have been holding the last slot. Only a full-server
        // rejection is reversed here -- bans and whitelist use different keys
        // and must continue to apply.
        if (evicted[0] && !event.isAllowed() && isServerFull(event.getKickMessage()))
        {
            Log.debug("BukkitBridge#onValidateLogin(): freeing slot held by the shadow of '{}'", name);
            event.allow();
        }
    }

    /**
     * Preserves server-side knockback on a shadow.
     *
     * <p>{@code Player#causeExtraKnockback} normally reverts the target's
     * movement with {@code setDeltaMovement(oldMovement)} and sends a velocity
     * packet, because a real client applies knockback itself. A shadow has no
     * client, so the knockback would simply vanish -- which is why the Fabric
     * build patched that method.
     *
     * <p>Paper wraps exactly that block in a cancellable
     * {@code PlayerVelocityEvent}, and cancelling skips the revert. That makes
     * this an exact replacement for the mixin rather than an approximation.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onVelocity(PlayerVelocityEvent event)
    {
        if (Nms.handle(event.getPlayer()) instanceof UnpluggedServerPlayer)
        {
            event.setCancelled(true);
        }
    }

    private static boolean isServerFull(net.kyori.adventure.text.Component message)
    {
        return message instanceof TranslatableComponent translatable
               && translatable.key().equals("multiplayer.disconnect.server_full");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKick(PlayerKickEvent event)
    {
        if (PlayerEventsHandler.getInstance().shouldHideJoin(event.getPlayer().getName()))
        {
            event.leaveMessage(null);
        }
    }
}
