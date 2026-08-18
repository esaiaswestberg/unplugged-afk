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

package com.sakuraryoko.unplugged_afk.impl.commands.server;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import com.sakuraryoko.unplugged_afk.impl.config.JsonConfigManager;
import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.commands.Perms;
import com.sakuraryoko.unplugged_afk.impl.config.FieldTarget;
import com.sakuraryoko.unplugged_afk.impl.nms.Nms;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.events.PlayerEventsHandler;
import com.sakuraryoko.unplugged_afk.impl.events.ServerEventsHandler;
import com.sakuraryoko.unplugged_afk.impl.player.*;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedStatus;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.*;
import com.sakuraryoko.unplugged_afk.impl.player.wrap.ProfileWrap;
import com.sakuraryoko.unplugged_afk.api.state.UnpluggedState;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import com.sakuraryoko.unplugged_afk.impl.Log;
import com.sakuraryoko.unplugged_afk.impl.nms.Text;

@ApiStatus.Internal
public class UnpluggedAdminCommand
{
    public static final String COMMAND = "unplugged-admin";

    private static final String NEWLINE = "\n";
    public static final String NODE = Reference.MOD_ID + "." + COMMAND;

    private static final UnpluggedAdminCommand INSTANCE = new UnpluggedAdminCommand();

    public static LiteralCommandNode<CommandSourceStack> build()
    {
        return INSTANCE.tree();
    }

    private LiteralCommandNode<CommandSourceStack> tree()
    {
        return (
                literal(COMMAND)
                        .requires(src -> Perms.check(src.getSender(), NODE, ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                        .executes(this::about)
                        .then(literal("save")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".save", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .executes(this::save)
                        )
                        .then(literal("reload")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".reload", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .executes(this::reload)
                        )
                        .then(literal("list")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".list", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .executes(this::listUnpluggedMap)
                                      .then(literal("players")
                                                    .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".list.players", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .executes(this::listPlayerMap)
                                      )
                                      .then(literal("unplugged")
                                                    .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".list.unplugged", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .executes(this::listUnpluggedMap)
                                      )
                                      .then(literal("all")
                                                    .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".list.all", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .executes(this::listAll)
                                      )
                        )
                        .then(literal("info")
                                      .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".info", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .executes(this::infoPlayer)
                                      .then(argument("player", ArgumentTypes.player())
                                                    .executes(ctx ->
                                                                      this.infoPlayer(ctx, resolvePlayer(ctx))
                                                    )
                                      )
                        )
                        .then(literal("purge")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".purge", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .executes(this::purgePlayers)
                        )
                        .then(literal("spawn")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".spawn", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .then(argument("player", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    suggestProfiles(PlayerManager.getInstance().getSpawnCommandSuggestions(ctx), builder)
                                                    )
                                                    .requires(src -> Perms.check(src.getSender(), NODE + ".spawn", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "player");
                                                                  return this.createUnplugged(ctx, result, -1, "");
                                                              }
                                                    )
                                                    .then(argument("minutes", IntegerArgumentType.integer(1))
                                                                  .requires(src -> Perms.check(src.getSender(), NODE + ".spawn", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                                  .executes(ctx ->
                                                                            {
                                                                                String result = StringArgumentType.getString(ctx, "player");
                                                                                return this.createUnplugged(ctx, result, IntegerArgumentType.getInteger(ctx, "minutes"), "");
                                                                            }
                                                                  )
                                                                  .then(argument("reason", StringArgumentType.greedyString())
                                                                                .requires(src -> Perms.check(src.getSender(), NODE + ".spawn", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                                                .executes(ctx ->
                                                                                          {
                                                                                              String result = StringArgumentType.getString(ctx, "player");
                                                                                              return this.createUnplugged(ctx, result, IntegerArgumentType.getInteger(ctx, "minutes"), StringArgumentType.getString(ctx, "reason"));
                                                                                          }
                                                                                )
                                                                  )
                                                    )
                                      )
                        )
                        .then(literal("kick")
                                      .requires(src -> Perms.check(src.getSender(), NODE + ".kick", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .then(argument("target", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    suggestProfiles(PlayerManager.getInstance().getKickCommandSuggestions(ctx), builder)
                                                    )
                                                    .requires(src -> Perms.check(src.getSender(), NODE + ".kick", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "target");
                                                                  return this.kickUnplugged(ctx, result);
                                                              }
                                                    )
                                      )
                        )
                        .then(literal("set")
                                      .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".set", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                      .then(argument("config", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    suggestStrings(UnpluggedConfigHandler.getInstance().configSuggestions(), builder)
                                                    )
                                                    .requires(src -> Perms.checkAdv(src.getSender(), NODE + ".set", ConfigWrap.cmdOpt().unpluggedAdminCommandPermissions))
                                                    .then(argument("value", StringArgumentType.greedyString())
                                                                  .suggests((ctx, builder) ->
                                                                            {
                                                                                String configName = StringArgumentType.getString(ctx, "config");
                                                                                FieldTarget targetData = UnpluggedConfigHandler.getInstance().getConfigInstanceByField(configName);

                                                                                if (targetData != null)
                                                                                {
                                                                                    Field targetField = targetData.field();
                                                                                    Object targetInstance = targetData.instance();

                                                                                    try
                                                                                    {
                                                                                        Class<?> type = targetField.getType();
                                                                                        Object currentValue = targetField.get(targetInstance);

                                                                                        if (currentValue != null)
                                                                                        {
                                                                                            // Illegal Character prevention.
                                                                                            if (type == String.class)
                                                                                            {
                                                                                                builder.suggest(currentValue.toString().replace('§', '&'));
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                builder.suggest(currentValue.toString());
                                                                                            }
                                                                                        }

                                                                                        if (type == boolean.class || type == Boolean.class)
                                                                                        {
                                                                                            builder.suggest("true");
                                                                                            builder.suggest("false");
                                                                                        }
                                                                                        else if (type.isEnum())
                                                                                        {
                                                                                            for (Object enumConstant : type.getEnumConstants())
                                                                                            {
                                                                                                builder.suggest(((Enum<?>) enumConstant).name());
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    catch (Exception ignored) {}
                                                                                }

                                                                                return builder.buildFuture();
                                                                            })
                                                                  .executes(ctx ->
                                                                              {
                                                                                  String result = StringArgumentType.getString(ctx, "config");
                                                                                  return this.setConfig(ctx, result, StringArgumentType.getString(ctx, "value"));
                                                                              }
                                                                  )
                                                    )
                                      )
                        )
        ).build();
    }

    private int about(CommandContext<CommandSourceStack> ctx)
    {
        MutableComponent text = Component.literal("")
                .append(Text.of("§6" + Reference.MOD_NAME + "§r v" + pluginVersion()))
                .append(Component.literal(NEWLINE))
                .append(Text.of("§7Unplug players as AFK§r"));

        Text.send(ctx.getSource(), text);

        return 1;
    }

    private static String pluginVersion()
    {
        var plugin = com.sakuraryoko.unplugged_afk.impl.UnpluggedAfkPlugin.getInstance();
        return plugin != null ? plugin.getPluginMeta().getVersion() : "?";
    }

    /** The NMS player behind the command sender, or null for console. */
    private static ServerPlayer senderPlayer(CommandContext<CommandSourceStack> ctx)
    {
        return ctx.getSource().getExecutor() instanceof org.bukkit.entity.Player p ? Nms.handle(p) : null;
    }

    private static ServerPlayer resolvePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        return Nms.handle(resolver.resolve(ctx.getSource()).getFirst());
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestStrings(
            Iterable<String> options, com.mojang.brigadier.suggestion.SuggestionsBuilder builder)
    {
        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);

        for (String option : options)
        {
            if (option.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
            {
                builder.suggest(option);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Replaces {@code SharedSuggestionProvider.suggest}, which Paper's command
     * API does not expose. Tooltips keep the same entity-hover formatting.
     */
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestProfiles(
            Iterable<GameProfile> profiles, com.mojang.brigadier.suggestion.SuggestionsBuilder builder)
    {
        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);

        for (GameProfile profile : profiles)
        {
            String name = ProfileWrap.name(profile);

            if (name != null && name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
            {
                // A suggestion tooltip is a flat Message, so the vanilla
                // entity-hover component has nothing to attach to here.
                builder.suggest(name, io.papermc.paper.command.brigadier.MessageComponentSerializer.message()
                        .serialize(net.kyori.adventure.text.Component.text(
                                ProfileWrap.id(profile) != null ? ProfileWrap.id(profile).toString() : name)));
            }
        }

        return builder.buildFuture();
    }

    private int save(CommandContext<CommandSourceStack> ctx)
    {
        PlayerManager.getInstance().flushToConfig(Nms.server());
        JsonConfigManager.getInstance().save(UnpluggedConfigHandler.getInstance());
        String user = ctx.getSource().getSender().getName();

        Text.send(ctx.getSource(), Text.of("Saving config!"));

        Log.info("{} has saved the configuration.", user);

        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> ctx)
    {
        UnpluggedConfigHandler.getInstance().toggleFromReloadCmd(true);
        JsonConfigManager.getInstance().load(UnpluggedConfigHandler.getInstance(), false);
        String user = ctx.getSource().getSender().getName();

        Text.send(ctx.getSource(), Text.of("Reloaded config!"));

        Log.info("{} has reloaded the configuration.", user);

        return 1;
    }

    private int listAll(CommandContext<CommandSourceStack> ctx)
    {
        if (!ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            this.listPlayerMap(ctx);
        }

        this.listUnpluggedMap(ctx);

        return 1;
    }

    private int listPlayerMap(CommandContext<CommandSourceStack> ctx)
    {
        if (ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            final Component result = Text.of("§dReduced debug info enabled; player listing disabled§r");

            Text.send(ctx.getSource(), result);

            return 0;
        }

        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                Text.of("§dPlayer Map:")
        );

        for (UUID key : playerMap.keySet())
        {
            PlayerEntry entry = playerMap.get(key);

            if (entry != null)
            {
                text.append(
                        Text.of(
                                String.format("\n§9[Entry: %02d]", count)
                        )
                ).append(
                        entry.getDebugFormatted()
                );
            }

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        ).append("\n");     // prefix for unplugged list

        Text.send(ctx.getSource(), text);

        if (senderPlayer(ctx) != null)
        {
            GameProfile profile = senderPlayer(ctx).getGameProfile();
            Log.debug("listPlayerMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            Log.debug("listPlayerMap: by: [console/unknown]");
        }

        return 1;
    }

    private int listUnpluggedMap(CommandContext<CommandSourceStack> ctx)
    {
        ImmutableMap<UUID, UnpluggedEntry> map = UnpluggedEntryList.getInstance().shadowMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                Text.of("§dUnplugged Map:")
        );

        for (UnpluggedEntry entry : map.values())
        {
            text.append(
                    Text.of(
                            String.format("\n§9[Entry: %02d]", count)
                    )
            ).append(
                    entry.debugFormatted()
            );

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        );

        Text.send(ctx.getSource(), text);

        if (senderPlayer(ctx) != null)
        {
            GameProfile profile = senderPlayer(ctx).getGameProfile();
            Log.debug("listUnpluggedMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            Log.debug("listUnpluggedMap: by: [console/unknown]");
        }

        return 1;
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx)
    {
        ServerPlayer self = senderPlayer(ctx);

        if (self == null)
        {
            Text.send(ctx.getSource(), Text.of("§cThe console must name a player.§r"));
            return 0;
        }

        return this.infoPlayer(ctx, self);
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
    {
        MutableComponent text = Component.literal("");
        boolean sent = false;

        if (!ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            text.append(
                    Text.of("§9Player Info: ")
            ).append(
                    PlayerManager.getInstance().getDebugFormatted(player.getUUID())
            ).append("\n");

            sent = true;
        }

        if (UnpluggedEntryList.getInstance().contains(player.getUUID()))
        {
            text.append(
                    Text.of("§9Unplugged Info: ")
            ).append(
                    UnpluggedEntryList.getInstance().getDebugFormatted(player.getUUID())
            );

            sent = true;
        }

        if (!sent)
        {
            text.append(
                    Text.of("§6Player: '§r"+player.getName().getString()+"§6' is not unplugged")
            );
        }

        Text.send(ctx.getSource(), text);

        GameProfile profile = player.getGameProfile();

        if (senderPlayer(ctx) != null)
        {
            GameProfile ctxProfile = senderPlayer(ctx).getGameProfile();
            Log.debug("infoPlayer: by: ['{}'/{}] for player: ['{}'/{}]",
                                  ProfileWrap.name(ctxProfile), ProfileWrap.id(ctxProfile),
                                  ProfileWrap.name(profile), ProfileWrap.id(profile)
            );
        }
        else
        {
            Log.debug("infoPlayer: by: [console/unknown] for player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }

        return 1;
    }

    private int purgePlayers(CommandContext<CommandSourceStack> ctx)
    {
        ServerPlayer player = senderPlayer(ctx);
        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        ImmutableMap<UUID, UnpluggedEntry> shadowMap = UnpluggedEntryList.getInstance().shadowMapCopy();
        int count = 0;

//        PlayerManager.getInstance().flushToConfig(Nms.server());

        for (UUID uuid : playerMap.keySet())
        {
            if (player != null)
            {
                if (!uuid.equals(player.getUUID()))
                {
                    PlayerManager.getInstance().remove(uuid, true, UnpluggedStatus.INTERRUPTED);
                    count++;
                }
            }
            else
            {
                // Via console command, probably.
                PlayerManager.getInstance().remove(uuid, true, UnpluggedStatus.INTERRUPTED);
                count++;
            }
        }

        // Resync
        PlayerManager.getInstance().onServerResync(Nms.server(), playerMap, shadowMap);
        playerMap = PlayerManager.getInstance().playerMapCopy();
        shadowMap = UnpluggedEntryList.getInstance().shadowMapCopy();
        String result = String.format("§ePurged: §c%d §eplayers, and then resynced §a%d §ecurrent players, with §6%d shadows§r", count, playerMap.size(), shadowMap.size());

        Text.send(ctx.getSource(), Text.of(result));

        if (senderPlayer(ctx) != null)
        {
            GameProfile profile = senderPlayer(ctx).getGameProfile();
            Log.debug("purgePlayers: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            Log.debug("purgePlayers: by: [console/unknown]");
        }

        return 1;
    }

    @ApiStatus.Internal
    private int createUnplugged(CommandContext<CommandSourceStack> ctx, String result, int time, String reason)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getSpawnCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        if (time < 0)
        {
            time = ConfigWrap.unplugged().defaultUnpluggedTimeout;

            if (time < 0)
            {
                time = 129600;
            }
        }
        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultUnpluggedReason;

            if (reason == null || reason.isEmpty())
            {
                reason = "§rnone";
            }
        }

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    PlayerOptions opts = ConfigWrap.players().stream()
                            .filter(opt -> opt.uuid.equals(ProfileWrap.id(entry)))
                                                   .findFirst()
                                                   .orElseThrow();

                    Log.debug("createUnplugged: Scheduling Unplugged player: ['{}'/{}]", opts.name, opts.uuid.toString());
                    reply = "§eScheduling unplugged spawn for: §7"+ result + "§r";
                    opts.state = new UnpluggedState(UnpluggedStatus.ACTIVE, time, (time * 60L) * 1000L, System.currentTimeMillis(), reason);
                    PlayerManager.getInstance().setState(entry, opts.state);
                    PlayerManager.getInstance().flushToConfig(Nms.server());
                    ServerEventsHandler.getInstance().toggleSpawnSafe(false);
                    UnpluggedPendingSpawns.INSTANCE.scheduleSpawn(opts);
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            reply = "§cNo matching player found§r";
        }

        final String finalReply = reply;

        Text.send(ctx.getSource(), Text.of(finalReply));

        if (senderPlayer(ctx) != null)
        {
            GameProfile profile = senderPlayer(ctx).getGameProfile();
            Log.debug("createUnplugged: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            Log.debug("createUnplugged: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }

    @ApiStatus.Internal
    private int kickUnplugged(CommandContext<CommandSourceStack> ctx, String result)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getKickCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    MinecraftServer server = Nms.server();
                    PlayerList playerList = server.getPlayerList();
                    List<ServerPlayer> players = playerList.getPlayers();

                    for (ServerPlayer player : players)
                    {
                        if (player.getUUID().equals(ProfileWrap.id(entry)) && player instanceof UnpluggedServerPlayer sp)
                        {
                            Log.debug("kickUnplugged: Kicking unplugged player: ['{}'/{}]", ProfileWrap.name(entry), ProfileWrap.id(entry).toString());
                            reply = " §7- Kicking unplugged player: §e"+ ProfileWrap.name(entry) + "§r";

                            if (ConfigWrap.mess().hideUnpluggedJoin)
                            {
                                PlayerEventsHandler.getInstance().addShouldHideJoin(result);
                            }

                            Component name = sp.getName();
                            Component message = Component.literal("Killed");
                            // kill() performs the removal itself; a second
                            // PlayerList#remove re-retires the entity scheduler.
                            sp.kill(message);

                            if (ConfigWrap.mess().hideUnpluggedJoin)
                            {
                                PlayerEventsHandler.getInstance().removeShouldHideJoin(name.getString());
                            }

                            break;
                        }
                    }
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            reply = "§cNo matching unplugged player found§r";
        }

        final String finalReply = reply;

        Text.send(ctx.getSource(), Text.of(finalReply));

        if (senderPlayer(ctx) != null)
        {
            GameProfile profile = senderPlayer(ctx).getGameProfile();
            Log.debug("kickUnplugged: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            Log.debug("kickUnplugged: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }

    @ApiStatus.Internal
    private int setConfig(CommandContext<CommandSourceStack> ctx, String config, String value)
    {
        FieldTarget target = UnpluggedConfigHandler.getInstance().getConfigInstanceByField(config);
        String reply;

        if (target == null)
        {
            reply = "§cUnknown config: "+config+"§r";
            String finalReply = reply;

            Text.send(ctx.getSource(), Text.of(finalReply));

            return 0;
        }

        Field targetField = target.field();
        Object targetInstance = target.instance();

        try
        {
            Class<?> fieldType = targetField.getType();
            Object parsedValue = null;

            if (fieldType == int.class || fieldType == Integer.class)
            {
                parsedValue = Integer.parseInt(value);
            }
            else if (fieldType == boolean.class || fieldType == Boolean.class)
            {
                if (value.equalsIgnoreCase("true"))
                {
                    parsedValue = true;
                }
                else if (value.equalsIgnoreCase("false"))
                {
                    parsedValue = false;
                }
                else
                {
                    reply = "§cInvalid boolean! Value must be 'true' or 'false'.§r";
                    String finalReply = reply;

                    Text.send(ctx.getSource(), Text.of(finalReply));

                    return 0;
                }
            }
            else if (fieldType == long.class || fieldType == Long.class)
            {
                parsedValue = Long.parseLong(value);
            }
            else if (fieldType == float.class || fieldType == Float.class)
            {
                parsedValue = Float.parseFloat(value);
            }
            else if (fieldType == double.class || fieldType == Double.class)
            {
                parsedValue = Double.parseDouble(value);
            }
            else if (fieldType == String.class)
            {
                parsedValue = value.replace('&', '§');
            }
            else if (fieldType.isEnum())
            {
                boolean found = false;

                for (Object enumConst : fieldType.getEnumConstants())
                {
                    if (((Enum<?>) enumConst).name().equalsIgnoreCase(value))
                    {
                        parsedValue = enumConst;
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    reply = "§cInvalid option! Valid options are: " + Arrays.toString(fieldType.getEnumConstants()) + "§r";
                    String finalReply = reply;

                    Text.send(ctx.getSource(), Text.of(finalReply));
                    return 0;
                }
            }

            if (parsedValue != null)
            {
                targetField.set(targetInstance, parsedValue);
                reply = "§aConfig: '"+config+"' updated to "+value+".§r\n§cNOTE: Some settings may require a server restart.";
                String finalReply = reply;

                Text.send(ctx.getSource(), Text.of(finalReply));

                this.save(ctx);
                this.reload(ctx);

                return 1;
            }
            else
            {
                reply = "§cUnsupported type for config: "+config+"§r";
                String finalReply = reply;

                Text.send(ctx.getSource(), Text.of(finalReply));

                return 0;
            }
        }
        catch (NumberFormatException e)
        {
            reply = "§cInvalid number format for config: "+config+"§r";
            String finalReply = reply;

            Text.send(ctx.getSource(), Text.of(finalReply));

            return 0;
        }
        catch (Exception e)
        {
            reply = "§cAn error occurred setting the config. Check logs.§r";
            Log.error("setConfig: Exception setting config '{}' to '{}'; {}", config, value, e.getLocalizedMessage());
            String finalReply = reply;

            Text.send(ctx.getSource(), Text.of(finalReply));

            return 0;
        }
    }
}
