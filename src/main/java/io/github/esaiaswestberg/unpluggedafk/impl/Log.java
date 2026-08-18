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

package io.github.esaiaswestberg.unpluggedafk.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.ApiStatus;

/**
 * Thin logging facade. Replaces the Fabric build's Log4J logger plus
 * {@code UnpluggedAfk.debugLog}, which was gated on {@code main.debugMode}.
 */
@ApiStatus.Internal
public class Log
{
    private static Logger logger = Logger.getLogger(Reference.MOD_NAME);
    private static boolean debugEnabled = Reference.DEBUG;

    public static void init(Logger pluginLogger)
    {
        logger = pluginLogger;
    }

    public static void setDebugEnabled(boolean enabled)
    {
        debugEnabled = enabled || Reference.DEBUG;
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static void info(String msg, Object... args)
    {
        logger.info(format(msg, args));
    }

    public static void warn(String msg, Object... args)
    {
        logger.warning(format(msg, args));
    }

    public static void error(String msg, Object... args)
    {
        logger.severe(format(msg, args));
    }

    public static void error(String msg, Throwable t)
    {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void debug(String msg, Object... args)
    {
        if (debugEnabled)
        {
            logger.info("[DEBUG] " + format(msg, args));
        }
    }

    /**
     * Formats an SLF4J-style {@code {}} template, so ported call sites keep their
     * original message strings.
     */
    private static String format(String msg, Object... args)
    {
        if (args == null || args.length == 0)
        {
            return msg;
        }

        StringBuilder out = new StringBuilder(msg.length() + 16 * args.length);
        int argIndex = 0;
        int i = 0;

        while (i < msg.length())
        {
            int next = msg.indexOf("{}", i);

            if (next < 0 || argIndex >= args.length)
            {
                out.append(msg, i, msg.length());
                break;
            }

            out.append(msg, i, next).append(stringify(args[argIndex++]));
            i = next + 2;
        }

        return out.toString();
    }

    private static String stringify(Object arg)
    {
        if (arg == null)
        {
            return "null";
        }

        try
        {
            return String.valueOf(arg);
        }
        catch (Exception e)
        {
            return "<" + arg.getClass().getSimpleName() + " toString() failed>";
        }
    }
}
