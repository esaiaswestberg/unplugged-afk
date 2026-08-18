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

package io.github.esaiaswestberg.unpluggedafk.impl.player;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import io.github.esaiaswestberg.unpluggedafk.impl.Reference;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;
import io.github.esaiaswestberg.unpluggedafk.impl.player.wrap.ProfileWrap;

@ApiStatus.Internal
public class PlayerUtils
{
    public static Component formatEntityTooltip(GameProfile profile)
    {
        MutableComponent result = Component.literal(ProfileWrap.name(profile));
        HoverEvent hoverEvent = new HoverEvent.ShowEntity(
                                               new HoverEvent.EntityTooltipInfo(getEntityTypeWrap(),
                                                                                ProfileWrap.id(profile),
                                                                                Component.literal(ProfileWrap.name(profile))
                                               )
        );
        result.withStyle(style -> style.withHoverEvent(hoverEvent));
        return result;
    }

    public static EntityType<?> getEntityTypeWrap()
    {
        return EntityTypes.PLAYER;
    }

    public static Component formatSuggestSpawnCommand(final String name)
    {
        MutableComponent result = Component.literal(name);
        ClickEvent clickEvent;
        HoverEvent hoverEvent;

        clickEvent = new ClickEvent.SuggestCommand(
                                    "/"+Reference.ADMIN_COMMAND+ " spawn " + name);
        hoverEvent = new HoverEvent.ShowText(
                                    Component.literal("Spawn"));

        result.withStyle(style ->
                                 style.withClickEvent(clickEvent)
                                      .withHoverEvent(hoverEvent)
        );

        return result;
    }

    public static Component formatSuggestKickCommand(final Component name)
    {
        MutableComponent result = name.copy();
        ClickEvent clickEvent;
        HoverEvent hoverEvent;

        clickEvent = new ClickEvent.SuggestCommand(
                                    "/"+Reference.ADMIN_COMMAND+ " kick " + name.getString());
        hoverEvent = new HoverEvent.ShowText(
                                    Component.literal("Kick"));

        result.withStyle(style ->
                                 style.withClickEvent(clickEvent)
                                      .withHoverEvent(hoverEvent)
        );

        return result;
    }

    public static long getServerStartDelta()
    {
        if (ConfigWrap.lastStart() > 0L && ConfigWrap.lastStop() > 0L)
        {
            return ConfigWrap.lastStart() - ConfigWrap.lastStop();
        }

        // Indeterminate.
        return 0L;
    }
}
