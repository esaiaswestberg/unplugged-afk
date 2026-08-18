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

package com.sakuraryoko.unplugged_afk.impl.config.data.options;

import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.unplugged_afk.impl.config.IConfigOption;
import com.sakuraryoko.unplugged_afk.impl.time.DurationFormat;
import com.sakuraryoko.unplugged_afk.impl.time.DurationOption;
import com.sakuraryoko.unplugged_afk.impl.time.TimeDateOption;
import com.sakuraryoko.unplugged_afk.impl.time.TimeFormat;

@ApiStatus.Internal
public class MessageOptions implements IConfigOption
{
    public boolean broadcastMessages;
    public boolean hideUnpluggedJoin;
    public boolean displayDuration;
    public boolean displayReturnFeedback;
    public String defaultUnpluggedReason;
    public String unpluggedPlayerPrefix;
    public String unpluggedPlayerSuffix;
    public String unpluggedKickMessage;
    public String unpluggedExpiredReason;
    public String unpluggedStarted;
    public String unpluggedPunctuation;
    public String unpluggedReplaced;
    public String unpluggedTerminated;
    public String unpluggedUnsuccessful;
    public String unpluggedUnsuccessfulPrefix;
    public String unpluggedUnsuccessfulPunctuation;
    public String unpluggedSuccessful;
    public String unpluggedSuccessfulPrefix;
    public String unpluggedSuccessfulSuffix;
    public String unpluggedSuccessfulPunctuation;
    public String whenUnpluggedReturned;
    public String whenUnpluggedExpired;
    public String whenUnpluggedInterrupted;
    public String whenUnpluggedTerminated;
    public String whenUnpluggedDurationPrefix;
    public String whenUnpluggedDurationSuffix;
    public String whenReturnDurationPrefix;
    public String whenReturnDurationSuffix;
    public DurationOption duration;
    public TimeDateOption timeDate;

    public MessageOptions()
    {
        this.defaults();
    }

    @Override
    public void defaults()
    {
        this.broadcastMessages = false;
        this.hideUnpluggedJoin = false;
        this.displayDuration = false;
        this.displayReturnFeedback = false;
        this.defaultUnpluggedReason = "";
        this.unpluggedPlayerPrefix = "§e";
        this.unpluggedPlayerSuffix = "§r";
        this.unpluggedKickMessage = "§6Your player will be AFK§r";
        this.unpluggedExpiredReason = "§eTimeout expired§r";
        this.unpluggedStarted = " §ehas been unplugged§r";
        this.unpluggedPunctuation = "§e,§r ";
        this.unpluggedReplaced = "§6Replaced by player§r";
        this.unpluggedTerminated = "§cAFK session terminated§r";
        this.unpluggedUnsuccessful = "§eYour AFK session was interrupted§r";
        this.unpluggedUnsuccessfulPrefix = " §eafter:§a ";
        this.unpluggedUnsuccessfulPunctuation = "\n §7- For:§r ";
        this.unpluggedSuccessful = "§eYour Session was successful.§r";
        this.unpluggedSuccessfulPrefix = "§eYour §a";
        this.unpluggedSuccessfulSuffix = " §eSession was successful.§r";
        this.unpluggedSuccessfulPunctuation = "\n §7- For:§r ";
        this.whenUnpluggedReturned = " §ehas returned§r";
        this.whenUnpluggedExpired = " §eAFK session expired§r";
        this.whenUnpluggedInterrupted = " §eAFK session interrupted§r";
        this.whenUnpluggedTerminated = " §eAFK session terminated§r";
        this.whenUnpluggedDurationPrefix = " §6for: §a";
        this.whenUnpluggedDurationSuffix = "§7 minutes)";
        this.whenReturnDurationPrefix = " §7(Gone for: §a";
        this.whenReturnDurationSuffix = "§7)§r";
        this.duration = new DurationOption();
        this.duration.option = DurationFormat.PRETTY;
        this.timeDate = new TimeDateOption();
        this.timeDate.option = TimeFormat.RFC1123;
    }

    @Override
    public MessageOptions copy(IConfigOption opt)
    {
        MessageOptions opts = (MessageOptions) opt;

        this.broadcastMessages = opts.broadcastMessages;
        this.hideUnpluggedJoin = opts.hideUnpluggedJoin;
        this.displayDuration = opts.displayDuration;
        this.displayReturnFeedback = opts.displayReturnFeedback;
        this.defaultUnpluggedReason = orDefault(opts.defaultUnpluggedReason, "");
        this.unpluggedPlayerPrefix = orDefault(opts.unpluggedPlayerPrefix, "§e");
        this.unpluggedPlayerSuffix = orDefault(opts.unpluggedPlayerSuffix, "§r");
        this.unpluggedKickMessage = orDefault(opts.unpluggedKickMessage, "§6Your player will be AFK§r");
        this.unpluggedExpiredReason = orDefault(opts.unpluggedExpiredReason, "§eTimeout expired§r");
        this.unpluggedStarted = orDefault(opts.unpluggedStarted, " §ehas been unplugged§r");
        this.unpluggedPunctuation = orDefault(opts.unpluggedPunctuation, "§e,§r ");
        this.unpluggedReplaced = orDefault(opts.unpluggedReplaced, "§6Replaced by player§r");
        this.unpluggedTerminated = orDefault(opts.unpluggedTerminated, "§cAFK session terminated§r");
        this.unpluggedUnsuccessful = orDefault(opts.unpluggedUnsuccessful, "§eYour AFK session was interrupted§r");
        this.unpluggedUnsuccessfulPrefix = orDefault(opts.unpluggedUnsuccessfulPrefix, " §eafter:§a ");
        this.unpluggedUnsuccessfulPunctuation = orDefault(opts.unpluggedUnsuccessfulPunctuation, "\n §7- For:§r ");
        this.unpluggedSuccessful = orDefault(opts.unpluggedSuccessful, "§eYour Session was successful.§r");
        this.unpluggedSuccessfulPrefix = orDefault(opts.unpluggedSuccessfulPrefix, "§eYour §a");
        this.unpluggedSuccessfulSuffix = orDefault(opts.unpluggedSuccessfulSuffix, " §eSession was successful.§r");
        this.unpluggedSuccessfulPunctuation = orDefault(opts.unpluggedSuccessfulPunctuation, "\n §7- For:§r ");
        this.whenUnpluggedReturned = orDefault(opts.whenUnpluggedReturned, " §ehas returned§r");
        this.whenUnpluggedExpired = orDefault(opts.whenUnpluggedExpired, " §eAFK session expired§r");
        this.whenUnpluggedInterrupted = orDefault(opts.whenUnpluggedInterrupted, " §eAFK session interrupted§r");
        this.whenUnpluggedTerminated = orDefault(opts.whenUnpluggedTerminated, " §eAFK session terminated§r");
        this.whenUnpluggedDurationPrefix = orDefault(opts.whenUnpluggedDurationPrefix, " §6for: §a");
        this.whenUnpluggedDurationSuffix = orDefault(opts.whenUnpluggedDurationSuffix, "§7 minutes)");
        this.whenReturnDurationPrefix = orDefault(opts.whenReturnDurationPrefix, " §7(Gone for: §a");
        this.whenReturnDurationSuffix = orDefault(opts.whenReturnDurationSuffix, "§7)§r");

        if (this.duration == null) { this.duration = new DurationOption(); }
        if (this.timeDate == null) { this.timeDate = new TimeDateOption(); }
        if (opts.duration != null) { this.duration.copy(opts.duration); }
        if (opts.timeDate != null) { this.timeDate.copy(opts.timeDate); }

        return this;
    }

    /**
     * The Fabric build applied this fallback to the later message strings only,
     * and NPE'd on a hand-edited config that omitted the earlier ones. Applying
     * it uniformly keeps the same defaults while tolerating missing keys.
     */
    private static String orDefault(String value, String fallback)
    {
        return value != null && !value.isEmpty() ? value : fallback;
    }
}
