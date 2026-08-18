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

package io.github.esaiaswestberg.unpluggedafk.impl.commands;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import io.github.esaiaswestberg.unpluggedafk.impl.Log;
import io.github.esaiaswestberg.unpluggedafk.impl.Reference;
import io.github.esaiaswestberg.unpluggedafk.impl.commands.server.DebugCommand;
import io.github.esaiaswestberg.unpluggedafk.impl.commands.server.UnpluggedAdminCommand;
import io.github.esaiaswestberg.unpluggedafk.impl.commands.server.UnplugCommand;
import io.github.esaiaswestberg.unpluggedafk.impl.config.ConfigWrap;

/**
 * Registers the brigadier trees through Paper's command lifecycle event.
 *
 * <p>As on Fabric, whether a command exists is decided once at registration
 * time, which is why toggling {@code enableUnplugCommand} logs a "restart the
 * server" warning rather than taking effect live.
 */
@ApiStatus.Internal
public class CommandRegister
{
    public static void register(JavaPlugin plugin)
    {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
        {
            var registrar = event.registrar();

            // TEMPORARY test harness
            registrar.register(DebugCommand.build(), Reference.MOD_NAME + " debug harness");

            // The admin command stays available even when the feature is off,
            // so an operator can turn it back on with /unplugged-admin set.
            registrar.register(UnpluggedAdminCommand.build(), "Manage " + Reference.MOD_NAME);

            if (!ConfigWrap.mainOpt().unpluggedAfkEnabled)
            {
                return;
            }

            boolean afkConflict = ConfigWrap.cmdOpt().enableAfkCommand && hasAfkConflict();

            if (afkConflict)
            {
                Log.error("The /afk command is provided by another plugin, but your config has it enabled; so it has been disabled.");
                ConfigWrap.cmdOpt().enableAfkCommand = false;

                if (!ConfigWrap.cmdOpt().enableUnplugCommand)
                {
                    Log.warn("Re-Enabling the disabled '/unplug' command so that users can use this plugin.");
                    ConfigWrap.cmdOpt().enableUnplugCommand = true;
                }
            }

            if (ConfigWrap.cmdOpt().enableUnplugCommand)
            {
                registrar.register(UnplugCommand.build(Reference.UNPLUG_COMMAND), "Go unplugged (AFK)");
            }

            if (ConfigWrap.cmdOpt().enableAfkCommand)
            {
                registrar.register(UnplugCommand.build(Reference.AFK_COMMAND), "Go unplugged (AFK)");
            }
        });
    }

    /**
     * On Fabric this scanned loaded mod ids. The equivalent conflict on Bukkit
     * is another plugin already owning the /afk command.
     */
    private static boolean hasAfkConflict()
    {
        for (String name : new String[]{"Essentials", "EssentialsX", "CMI", "AFKPlus", "AntiLogout"})
        {
            if (Bukkit.getPluginManager().getPlugin(name) != null)
            {
                return true;
            }
        }

        return Bukkit.getCommandMap().getKnownCommands().containsKey(Reference.AFK_COMMAND);
    }
}
