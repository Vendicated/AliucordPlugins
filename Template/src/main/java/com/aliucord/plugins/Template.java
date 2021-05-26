/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * These plugins are free software: you can redistribute them and/or modify
 * them under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * They are distributed in the hope that they will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Template extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        Manifest manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context context) {
        List<ApplicationCommandOption> arguments = new ArrayList<>();
        arguments.add(new ApplicationCommandOption(ApplicationCommandType.STRING, "", "", null, true, true, null, null));

        commands.registerCommand(
                "",
                "",
                arguments,
                args -> {
                    return new CommandsAPI.CommandResult("", null, false);
                }
        );
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
