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

package com.sakuraryoko.unplugged_afk.impl.player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import com.sakuraryoko.unplugged_afk.impl.config.JsonConfigManager;
import com.sakuraryoko.unplugged_afk.api.state.GameState;
import com.sakuraryoko.unplugged_afk.api.state.PosState;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedStatus;
import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.config.IConfigExecuteHandler;
import com.sakuraryoko.unplugged_afk.impl.config.data.UnpluggedConfigData;
import com.sakuraryoko.unplugged_afk.impl.events.ServerEventsHandler;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.events.ServerEventsHandler;
import com.sakuraryoko.unplugged_afk.impl.player.wrap.*;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.*;
import com.sakuraryoko.unplugged_afk.impl.Log;

@ApiStatus.Internal
public class PlayerManager implements IConfigExecuteHandler
{
    private static final PlayerManager INSTANCE = new PlayerManager();
    public static PlayerManager getInstance() { return INSTANCE; }

    private static final float TICK_RATE = 5.0f;
    private final ConcurrentHashMap<UUID, PlayerEntry> players;
    private long lastTick;

    @ApiStatus.Internal
    private PlayerManager()
    {
        this.players = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.lastTick = System.currentTimeMillis();
    }

    @ApiStatus.Internal
    public void syncProfile(GameProfile profile)
    {
        if (profile == null) { return; }
        List<PlayerOptions> config = ConfigWrap.players();
        UUID uuid = ProfileWrap.id(profile);

        Log.debug("syncProfile: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));

        for (PlayerOptions opt : config)
        {
            if (opt.uuid.equals(uuid))
            {
                this.addOrUpdateProfile(profile, opt.state);
                return;
            }
        }

        // Doesn't exist in config --> Add
        this.addConfig(profile);
        this.addOrUpdateProfile(profile, UnpluggedState.DEFAULT);
    }

    @ApiStatus.Internal
    public void resetFromConfig()
    {
        this.players.clear();
    }

    @ApiStatus.Internal
    public void syncFromConfig(@Nonnull PlayerOptions opt)
    {
        Log.debug("syncFromConfig: ['{}'/{}]", opt.name, opt.uuid.toString());
        this.addOrUpdateProfile(ProfileWrap.profile(opt.uuid, opt.name), opt.state, opt.pos, opt.game);
    }

    @ApiStatus.Internal
    private void addOrUpdateProfile(@Nonnull GameProfile profile, UnpluggedState state)
    {
        this.addOrUpdateProfile(profile, state, PosWrap.defaultPos(), GameWrap.defMode());
    }

    @ApiStatus.Internal
    private void addOrUpdateProfile(@Nonnull GameProfile profile, UnpluggedState state, PosState pos, GameState game)
    {
        UUID uuid = ProfileWrap.id(profile);
        String name = ProfileWrap.name(profile);
        PlayerEntry newEntry = new PlayerEntry(uuid, name, state, pos, game);

        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null && !entry.state().equals(state))
            {
                entry = this.players.remove(uuid);
                newEntry = entry.updateState(state);
                this.players.put(uuid, newEntry);
            }
        }
        else
        {
            this.players.put(uuid, newEntry);
        }

        Log.debug("addOrUpdateProfile: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
    }

    @ApiStatus.Internal
    private void addConfig(@Nonnull GameProfile profile)
    {
        List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
        UUID uuid = ProfileWrap.id(profile);
        boolean exists = false;

        for (PlayerOptions entry : config)
        {
            if (entry.uuid.equals(uuid))
            {
                exists = true;
                break;
            }
        }

        if (!exists)
        {
            PlayerOptions opt = PlayerOptions.fromProfile(profile, UnpluggedState.DEFAULT);

            if (this.players.containsKey(uuid))
            {
                PlayerEntry entry = this.players.get(uuid);

                if (entry != null)
                {
                    opt.pos = entry.pos();
                    opt.game = entry.game();
                }
            }

            ConfigWrap.players().add(opt);
        }

        Log.debug("addConfig: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
    }

    @ApiStatus.Internal
    private void setConfig(@Nonnull GameProfile profile, UnpluggedState state)
    {
        List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
        UUID uuid = ProfileWrap.id(profile);
        boolean dirty = false;

        for (PlayerOptions entry : config)
        {
            if (entry.uuid.equals(uuid))
            {
                entry.state = state;
                entry.name = ProfileWrap.name(profile);

                if (this.players.containsKey(uuid))
                {
                    PlayerEntry playerEntry = this.players.get(uuid);

                    if (playerEntry != null)
                    {
                        entry.pos = playerEntry.pos();
                        entry.game = playerEntry.game();
                    }

                    break;
                }

                dirty = true;
            }
        }

        if (dirty)
        {
            ConfigWrap.players().clear();

            for (PlayerOptions entry : config)
            {
                PlayerOptions opt = new PlayerOptions(entry);

                if (this.players.containsKey(uuid))
                {
                    PlayerEntry playerEntry = this.players.get(uuid);

                    if (playerEntry != null)
                    {
                        entry.pos = playerEntry.pos();
                        entry.game = playerEntry.game();
                    }

                    break;
                }

                ConfigWrap.players().add(opt);
            }
        }

        Log.debug("setConfig: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
    }

    public UnpluggedState getState(@Nonnull GameProfile profile)
    {
        UUID uuid = ProfileWrap.id(profile);

        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null)
            {
                return entry.state();
            }
        }

        Log.debug("getState: player: ['{}'/{}] failure; adding new entry", ProfileWrap.name(profile), ProfileWrap.id(profile));
        this.addOrUpdateProfile(profile, UnpluggedState.DEFAULT);
        this.addConfig(profile);
        return UnpluggedState.DEFAULT;
    }

    @ApiStatus.Internal
    public UnpluggedState getState(@Nonnull UUID uuid)
    {
        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null)
            {
                return entry.state();
            }
        }

        Log.debug("getState: UUID: [{}] failure; returning default state", uuid.toString());
        return UnpluggedState.DEFAULT;
    }

    @ApiStatus.Internal
    public void setState(@Nonnull GameProfile profile, UnpluggedState state)
    {
        this.addOrUpdateProfile(profile, state);
        this.setConfig(profile, state);
        Log.debug("setState: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
    }

    public void resetState(@Nonnull ServerPlayer player)
    {
        this.resetState(player.getGameProfile());
    }

    public void resetState(@Nonnull GameProfile profile)
    {
        this.setState(profile, UnpluggedState.DEFAULT);
    }

    public void remove(@Nonnull UUID uuid, boolean silent, UnpluggedStatus reason)
    {
        UnpluggedEntryList.getInstance().remove(uuid, silent, reason);
        this.players.remove(uuid);
        ConfigWrap.players().removeIf(opt -> opt.uuid.equals(uuid));
        Log.debug("remove: UUID: [{}], silent: {}, reason: {}", uuid.toString(), silent, reason.toString());
    }

    public PosState getPos(@Nonnull UUID uuid)
    {
        List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null)
            {
                return entry.pos();
            }
        }

//      List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

        for (PlayerOptions entry : config)
        {
            if (entry.uuid.equals(uuid))
            {
                return entry.pos;
            }
        }

        return PosWrap.defaultPos();
    }

    public GameState getGameMode(@Nonnull UUID uuid)
    {
        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null)
            {
                return entry.game();
            }
        }

        List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

        for (PlayerOptions entry : config)
        {
            if (entry.uuid.equals(uuid))
            {
                return entry.game;
            }
        }

        return GameWrap.defMode();
    }

    @ApiStatus.Internal
    public void updatePlayerData(@Nonnull ServerPlayer player)
    {
        PosState pos = PosWrap.of(player);
        GameState game = GameWrap.of(player);
        UUID uuid = player.getUUID();
        GameProfile profile = player.getGameProfile();

        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.remove(uuid);

            if (entry != null)
            {
                if (entry.state().status() == UnpluggedStatus.ACTIVE)
                {
                    UnpluggedEntry unplugged = UnpluggedEntryList.getInstance().get(uuid);

                    if (unplugged != null)
                    {
                        entry = entry.updateAll(unplugged.toState(), player.getName().getString(), pos, game);
                    }
                    else
                    {
                        entry = entry.updatePlayerData(player.getName().getString(), pos, game);
                    }
                }
                else
                {
                    entry = entry.updatePlayerData(player.getName().getString(), pos, game);
                }

                if (Reference.DEBUG)
                {
                    Log.debug("updatePlayerData: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
                }

                this.players.put(uuid, entry);
            }
        }
    }

    @ApiStatus.Internal
    public ImmutableList<GameProfile> getSpawnCommandSuggestions(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        ImmutableList.Builder<GameProfile> builder = ImmutableList.builder();
        MinecraftServer server = ctx.getSource().getServer();
        PlayerList playerList = server.getPlayerList();
        final List<ServerPlayer> players = playerList.getPlayers();

        this.players.forEach(
                (uuid, entry) ->
                {
                    boolean found = false;

                    for (ServerPlayer player : players)
                    {
                        if (player.getUUID().equals(uuid))
                        {
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        builder.add(ProfileWrap.profile(entry.uuid(), entry.name()));
                    }
                }
        );

        return builder.build();
    }

    @ApiStatus.Internal
    public ImmutableList<GameProfile> getKickCommandSuggestions(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        ImmutableList.Builder<GameProfile> builder = ImmutableList.builder();
        MinecraftServer server = ctx.getSource().getServer();
        PlayerList playerList = server.getPlayerList();
        final List<ServerPlayer> players = playerList.getPlayers();

        this.players.forEach(
                (uuid, entry) ->
                {
                    boolean found = false;

                    if (entry.state().status() == UnpluggedStatus.ACTIVE)
                    {
                        for (ServerPlayer player : players)
                        {
                            if (player.getUUID().equals(uuid))
                            {
                                found = true;
                                break;
                            }
                        }
                    }
                    else
                    {
                        for (ServerPlayer player : players)
                        {
                            if (player.getUUID().equals(uuid) && player instanceof UnpluggedServerPlayer sp)
                            {
                                // Fix desynced player
                                UnpluggedState newState = new UnpluggedState(UnpluggedStatus.ACTIVE, sp.getTimer(), sp.getTimeout(), sp.getStartTime(), sp.getReason());
                                this.setState(player.getGameProfile(), newState);
                                found = true;
                                break;
                            }
                        }
                    }

                    if (found)
                    {
                        builder.add(ProfileWrap.profile(entry.uuid(), entry.name()));
                    }
                }
        );

        return builder.build();
    }

    @VisibleForTesting
    public ImmutableMap<UUID, PlayerEntry> playerMapCopy()
    {
        ImmutableMap.Builder<UUID, PlayerEntry> map = ImmutableMap.builder();
        this.players.forEach(map::put);
        return map.build();
    }

    @VisibleForTesting
    public Component getDebugFormatted(UUID uuid)
    {
        if (this.players.containsKey(uuid))
        {
            PlayerEntry entry = this.players.get(uuid);

            if (entry != null)
            {
                return entry.getDebugFormatted();
            }
        }

        return Component.literal("§cPlayer not found§r");
    }

    @ApiStatus.Internal
    private boolean syncConfig(@Nonnull MinecraftServer server, boolean stop)
    {
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        boolean dirty = false;

        for (ServerPlayer player : players)
        {
            if (this.syncConfigEach(player))
            {
                dirty = true;
            }
        }

        if (stop) { return dirty; }

        // Spawn shadow configured players
        ImmutableList<PlayerOptions> config = ImmutableList.copyOf(ConfigWrap.players());
        final long serverDelta = PlayerUtils.getServerStartDelta();

        for (PlayerOptions entry : config)
        {
            boolean found = false;

            for (ServerPlayer player : players)
            {
                if (entry.uuid.equals(player.getUUID()))
                {
                    found = true;
                    break;
                }
            }

            if (!found && entry.state.status() == UnpluggedStatus.ACTIVE)
            {
                boolean schedule = true;

                // Calculate Server Start / Stop timeout offset.
                if (serverDelta > 0L)
                {
                    UnpluggedState oldState = entry.state;
                    UnpluggedState newState;
                    final long oldTimeout = oldState.timeout();
                    final long newTimeout = oldTimeout - serverDelta;

                    if (newTimeout < 1L)
                    {
                        String mess = ConfigWrap.mess().unpluggedExpiredReason;

                        if (mess == null || mess.isEmpty())
                        {
                            mess = "§eTimeout Expired§r";
                        }

                        final long delta = UnpluggedPlayerUtils.getStartTimeDelta(entry.state.startTime());
                        final String reason = (ConfigWrap.mess().displayDuration
                                               ? ConfigWrap.mess().unpluggedSuccessfulPrefix
                                                 + ConfigWrap.mess().duration.option.format(delta)
                                                 + ConfigWrap.mess().unpluggedSuccessfulSuffix
                                               : ConfigWrap.mess().unpluggedSuccessful)
                                + mess;
                        newState = new UnpluggedState(UnpluggedStatus.EXPIRED, -1, -1L, -1L, reason);
                        Log.debug("syncConfig: player: ['{}'/{}], serverDelta expired session [diff: {}]", entry.name, entry.uuid.toString(), (oldTimeout - newTimeout));
                        schedule = false;
                    }
                    else
                    {
                        newState = new UnpluggedState(oldState.status(), oldState.time(), newTimeout, oldState.startTime(), oldState.reason());
                        Log.debug("syncConfig: player: ['{}'/{}], add serverDelta to timeout [{} -> {}], diff: [{}]", entry.name, entry.uuid.toString(), oldTimeout, newTimeout, (oldTimeout - newTimeout));
                    }

                    this.setState(ProfileWrap.profile(entry.uuid, entry.name), newState);
                    UnpluggedEntryList.getInstance().remove(entry.uuid, true, UnpluggedStatus.EXPIRED);
                    dirty = true;
                }

                if (schedule)
                {
                    ServerEventsHandler.getInstance().toggleSpawnSafe(false);
                    Log.debug("syncConfig: Scheduling unplugged player: ['{}'/{}], state: [{}]", entry.name, entry.uuid.toString(), entry.state.toString());
                    UnpluggedPendingSpawns.INSTANCE.scheduleSpawn(entry);
                }
            }
        }

        return dirty;
    }

    @ApiStatus.Internal
    private boolean syncConfigEach(ServerPlayer player)
    {
        ImmutableList<PlayerOptions> oldConfig = ImmutableList.copyOf(ConfigWrap.players());
        List<PlayerOptions> newConfig = new ArrayList<>();
        String name = player.getName().getString();
        UUID uuid = player.getUUID();
        PosState pos = PosWrap.of(player);
        GameState game = GameWrap.of(player);
        UnpluggedState state = this.getState(uuid).ensureValid();

        if (player instanceof UnpluggedServerPlayer sp)
        {
            UnpluggedEntry entry = UnpluggedEntryList.getInstance().get(sp);

            if (entry == null)
            {
                if (state.isEmpty())
                {
                    state = sp.toState();
                }

                entry = UnpluggedEntryList.getInstance().add(sp, state);
            }

            if (entry != null)
            {
                state = new UnpluggedState(entry.status(), entry.timer(), sp.getTimeout(), sp.getStartTime(), entry.reason());
            }
            else
            {
                state = new UnpluggedState(sp.isValid() ? UnpluggedStatus.ACTIVE : UnpluggedStatus.INTERRUPTED, sp.getTimer(), sp.getTimeout(), sp.getStartTime(), sp.getReason());
            }
        }

        boolean found = false;
        boolean dirty = false;

        Log.debug("syncConfigEach(): Player ['{}'/{}]; [CALULATED] state: [{}], pos: [{}], game: [{}]", name, uuid.toString(), state.toString(), pos.toString(), game.toString());

        for (PlayerOptions entry : oldConfig)
        {
            PlayerOptions newEntry = new PlayerOptions(entry);

            if (newEntry.uuid.equals(uuid))
            {
                if (newEntry.state.isEmpty() || !newEntry.state.equals(state))
                {
                    newEntry.state = state;
                    dirty = true;
                }
                if (newEntry.pos.isEmpty() || !newEntry.pos.equals(pos))
                {
                    newEntry.pos = pos;
                    dirty = true;
                }
                if (newEntry.game.isEmpty() || !newEntry.game.equals(game))
                {
                    newEntry.game = game;
                    dirty = true;
                }
                if (newEntry.name.isEmpty() || newEntry.name.equals(uuid.toString()) || !newEntry.name.equals(name))
                {
                    newEntry.name = name;
                    dirty = true;
                }

                found = true;
            }

//          Log.debug("syncConfigEach(): Player ['{}'/{}]; [NEW-ENTRY] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
            newConfig.add(newEntry);
        }

        if (!found)
        {
            PlayerOptions newEntry = PlayerOptions.fromProfile(player.getGameProfile(), state);
            newEntry.state = state;
            newEntry.pos = pos;
            newEntry.game = game;
            newConfig.add(newEntry);
//          Log.debug("syncConfigEach(): Player ['{}'/{}]; [NOT-FOUND] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
            dirty = true;
        }

        if (dirty)
        {
            ConfigWrap.players().clear();

            for (PlayerOptions entry : newConfig)
            {
                PlayerOptions newEntry = new PlayerOptions(entry);

                // Double Verify
                if (newEntry.uuid.equals(uuid))
                {
                    if (!newEntry.state.equals(state))
                    {
                        newEntry.state = state;
                    }
                    if (!newEntry.pos.equals(pos))
                    {
                        newEntry.pos = pos;
                    }
                    if (!newEntry.game.equals(game))
                    {
                        newEntry.game = game;
                    }
                }

//              Log.debug("syncConfigEach(): Player ['{}'/{}]; [DIRTY] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
                ConfigWrap.players().add(newEntry);
            }

            return true;
        }

        return false;
    }

    @ApiStatus.Internal
    public void flushToConfig(@Nonnull MinecraftServer server)
    {
        List<PlayerOptions> newConfig = new ArrayList<>();
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();

        this.players.forEach(
                (uuid, playerEntry) ->
                {
                    PlayerOptions newEntry = new PlayerOptions();
                    newEntry.uuid = uuid;
                    newEntry.name = playerEntry.name();
                    newEntry.state = playerEntry.state();
                    newEntry.pos = playerEntry.pos();
                    newEntry.game = playerEntry.game();

                    // Copy state from an active Unplugged player
                    for (ServerPlayer player : players)
                    {
                        if (player.getUUID().equals(uuid))
                        {
                            if (player instanceof UnpluggedServerPlayer sp)
                            {
                                newEntry.state = sp.toState();
                            }

                            break;
                        }
                    }

                    newConfig.add(newEntry);
                }
        );

        ConfigWrap.players().clear();
        for (PlayerOptions entry : newConfig)
        {
            ConfigWrap.players().add(entry);
        }

        Log.debug("flushToConfig(): Done");
    }

    @ApiStatus.Internal
    public void onServerStop(@Nonnull MinecraftServer server)
    {
        Log.debug("onServerStop --> syncConfig()");
        UnpluggedConfigHandler.getInstance().setStopTime();

        this.flushToConfig(server);
//      Log.debug("onServerStop(): flushing config changes ...");
        JsonConfigManager.getInstance().save(UnpluggedConfigHandler.getInstance());
    }

    @ApiStatus.Internal
    public void onServerStarted(@Nonnull MinecraftServer server)
    {
        Log.debug("onServerStarted --> syncConfig()");
        UnpluggedConfigHandler.getInstance().setStartTime();

        if (this.syncConfig(server, false))
        {
//          Log.debug("onServerStarted(): flushing config changes ...");
            JsonConfigManager.getInstance().save(UnpluggedConfigHandler.getInstance());
        }
    }

    @ApiStatus.Internal
    public void onServerResync(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, UnpluggedEntry> shadowMap)
    {
        boolean dirty = false;
        // Same as stop, really; just with a different message
        Log.debug("onServerResync --> syncConfig()");

        if (this.syncConfig(server, true))
        {
            dirty = true;
        }

        if (this.syncEntries(server, playerMap, shadowMap))
        {
            dirty = true;
        }

        if (dirty)
        {
//          Log.debug("onServerResync(): flushing config changes ...");
            JsonConfigManager.getInstance().save(UnpluggedConfigHandler.getInstance());
        }
    }

    private boolean syncEntries(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, UnpluggedEntry> shadowMap)
    {
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        boolean dirty = false;
        boolean updated = false;

        Log.debug("syncEntries --> count: {}", shadowMap.size());

        for (ServerPlayer player : players)
        {
            UUID uuid = player.getUUID();

            if (player instanceof UnpluggedServerPlayer sp)
            {
                if (shadowMap.containsKey(uuid))
                {
                    UnpluggedEntry entry = shadowMap.get(uuid);

                    if (entry != null)
                    {
                        UnpluggedState newState = null;

                        if (playerMap.containsKey(uuid))
                        {
                            PlayerEntry playerEntry = playerMap.get(uuid);

                            if (playerEntry != null)
                            {
                                newState = playerEntry.state();
                            }
                        }

                        if (newState == null)
                        {
                            newState = sp.toState();
                        }

//                      Log.debug("syncEntries --> sync: ['{}'/{}], state: [{}]", sp.getName().getString(), uuid.toString(), newState.toString());
                        this.setState(sp.getGameProfile(), newState);
                        entry.updateState(newState);
                        UnpluggedEntryList.getInstance().syncEntry(sp, entry);
                        dirty = true;
                    }
                }
            }

            PosState pos = PlayerManager.getInstance().getPos(uuid);

            if (!pos.matches(player))
            {
                this.updatePlayerData(player);
                updated = true;
            }
        }

        if (updated)
        {
            this.flushToConfig(server);
        }

        return dirty;
    }

    @ApiStatus.Internal
    public void onTick(@Nonnull MinecraftServer server, boolean spawnSafe)
    {
        final long now = System.currentTimeMillis();

        if ((now - this.lastTick) > this.onTickTimeout())
        {
            this.onTickCycle(server, spawnSafe);
            this.lastTick = now;
        }
    }

    private long onTickTimeout()
    {
        return (long) (TICK_RATE * 1000L);
    }

    private void onTickCycle(@Nonnull MinecraftServer server, boolean spawnSafe)
    {
        PlayerList playerList = server.getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        boolean dirty = false;

        for (ServerPlayer player : players)
        {
            if (this.onTickEach(player))
            {
                dirty = true;
            }
        }

        // Only spawn if marked as "safe to spawn";
        // which means there are pending spawns already scheduled, or
        // the Server is currently still starting up.
        if (spawnSafe)
        {
            ImmutableMap<UUID, PlayerEntry> playerMap = this.playerMapCopy();

            for (UUID uuid : playerMap.keySet())
            {
                PlayerEntry playerEntry = playerMap.get(uuid);

                if (playerEntry == null)
                {
                    continue;
                }

                if (playerEntry.state().status() == UnpluggedStatus.ACTIVE)
                {
                    boolean found = false;

                    for (ServerPlayer entry : players)
                    {
                        if (entry.getUUID().equals(uuid))
                        {
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        ImmutableList<PlayerOptions> config = ImmutableList.copyOf(ConfigWrap.players());
                        ServerEventsHandler.getInstance().toggleSpawnSafe(false);

                        for (PlayerOptions opt : config)
                        {
                            if (opt.uuid.equals(uuid))
                            {
                                if (Log.isDebugEnabled())
                                {
                                    Log.debug("onTickCycle: Scheduling unplugged player: ['{}'/{}], state: [{}]", opt.name, opt.uuid.toString(), opt.state.toString());
                                }
                                else
                                {
                                    Log.info("PlayerManager#onTickCycle: Scheduling unplugged player: ['{}'/{}]", opt.name, opt.uuid.toString());
                                }

                                UnpluggedPendingSpawns.INSTANCE.scheduleSpawn(opt);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (dirty)
        {
//          Log.debug("onServerResync(): flushing config changes ...");
            JsonConfigManager.getInstance().save(UnpluggedConfigHandler.getInstance());
        }
    }

    private boolean onTickEach(ServerPlayer player)
    {
        UUID uuid = player.getUUID();

        if (!this.players.containsKey(uuid))
        {
            UnpluggedState newState = UnpluggedState.DEFAULT;

            if (player instanceof UnpluggedServerPlayer sp)
            {
                newState = sp.toState();
                this.addOrUpdateProfile(player.getGameProfile(), newState, PosWrap.of(player), GameWrap.of(player));

                if (!UnpluggedEntryList.getInstance().contains(uuid))
                {
                    UnpluggedEntryList.getInstance().add(sp, newState);
                }
            }
            else
            {
                this.addOrUpdateProfile(player.getGameProfile(), newState, PosWrap.of(player), GameWrap.of(player));
            }

            Log.debug("onTickEach() sync: ['{}'/{}] --> added missing player", player.getName().getString(), uuid.toString());
            return true;
        }

        return false;
    }

    /**
     * Invoked at the end of the config lifecycle. The Fabric build ran this
     * inline inside {@code UnpluggedConfigHandler#execute}; on Paper the
     * dependency is inverted so the config can load before the server exists.
     */
    @Override
    public void onConfigExecute(UnpluggedConfigData config, boolean fromInit, boolean hideAll, boolean unhideAll, boolean fromReloadCmd)
    {
        this.resetFromConfig();
        config.PLAYERS.forEach(this::syncFromConfig);

        if (unhideAll && !fromInit && fromReloadCmd)
        {
            ServerEventsHandler.getInstance().toggleUnhideAllPlayers(true);
        }
        if (hideAll && !fromInit && fromReloadCmd)
        {
            ServerEventsHandler.getInstance().toggleHideAllPlayers(true);
        }
    }
}
