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

package io.github.esaiaswestberg.unpluggedafk.impl.config.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;

import io.github.esaiaswestberg.unpluggedafk.impl.config.IConfigData;
import io.github.esaiaswestberg.unpluggedafk.impl.config.data.options.CommandOptions;
import io.github.esaiaswestberg.unpluggedafk.impl.config.data.options.MainOptions;
import io.github.esaiaswestberg.unpluggedafk.impl.config.data.options.MessageOptions;
import io.github.esaiaswestberg.unpluggedafk.impl.config.data.options.PlayerOptions;
import io.github.esaiaswestberg.unpluggedafk.impl.config.data.options.UnpluggedOptions;

@ApiStatus.Internal
public class UnpluggedConfigData implements IConfigData
{
    @SerializedName("___comment")
    public String comment = "Unplugged AFK Config";

    @SerializedName("config_date")
    public String config_date;

    @SerializedName("last_start")
    public Long last_start;

    @SerializedName("last_stop")
    public Long last_stop;

    @SerializedName("main")
    public MainOptions MAIN = new MainOptions();

    @SerializedName("commands")
    public CommandOptions COMMANDS = new CommandOptions();

    @SerializedName("unplugged")
    public UnpluggedOptions UNPLUGGED = new UnpluggedOptions();

    @SerializedName("messages")
    public MessageOptions MESS = new MessageOptions();

    @SerializedName("players")
    public List<PlayerOptions> PLAYERS = new ArrayList<>();
}
