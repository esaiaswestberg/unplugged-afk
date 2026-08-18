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

package io.github.esaiaswestberg.unpluggedafk.impl.nms;

import org.jetbrains.annotations.ApiStatus;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Replaces CoreLib's {@code BuiltinTextHandler}, which was simply
 * {@code Component.literal(s)} -- the legacy section codes in the config are
 * interpreted client side, so no parsing is required.
 */
@ApiStatus.Internal
public class Text
{
    public static final char SECTION = '§';
    public static final char AMPERSAND = '&';

    /** Vanilla {@link Component} for a legacy section-code string. */
    public static MutableComponent of(String text)
    {
        return Component.literal(text == null ? "" : text);
    }

    public static MutableComponent empty()
    {
        return Component.literal("");
    }

    /** Adventure component, for anything crossing into the Bukkit API surface. */
    public static net.kyori.adventure.text.Component adventure(String text)
    {
        return LegacyComponentSerializer.legacySection().deserialize(text == null ? "" : text);
    }

    /**
     * Delivers a vanilla component to a command source.
     *
     * <p>Players get it as-is over NMS, which keeps the legacy colour codes and
     * the click/hover events intact and never involves Adventure.
     *
     * <p>The console is an Adventure sender, so handing it a component whose
     * content still holds raw section signs makes Adventure log
     * {@code LegacyFormattingDetected} (at construction, so it cannot be
     * repaired after the fact) and print them literally. For that path the codes
     * are parsed into real Adventure styles instead; click and hover mean
     * nothing on a console line, so nothing of value is lost.
     */
    public static void send(io.papermc.paper.command.brigadier.CommandSourceStack source, Component message)
    {
        if (source.getExecutor() instanceof org.bukkit.entity.Player player)
        {
            Nms.handle(player).sendSystemMessage(message);
            return;
        }

        source.getSender().sendMessage(adventure(message.getString()));
    }

    /** {@code &} -> {@code §}; used when writing config values via /unplugged-admin set. */
    public static String toSection(String text)
    {
        return text == null ? "" : text.replace(AMPERSAND, SECTION);
    }

    /** {@code §} -> {@code &}; used when suggesting config values. */
    public static String toAmpersand(String text)
    {
        return text == null ? "" : text.replace(SECTION, AMPERSAND);
    }

    public static String stripFormatting(String text)
    {
        if (text == null || text.isEmpty())
        {
            return "";
        }

        StringBuilder out = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);

            if (c == SECTION && i + 1 < text.length())
            {
                i++;
                continue;
            }

            out.append(c);
        }

        return out.toString();
    }
}
