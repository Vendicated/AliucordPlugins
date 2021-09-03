/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.hastebin;

import android.content.Context;

import com.aliucord.Http;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;

import java.io.IOException;
import java.util.Arrays;

@AliucordPlugin
public class Hastebin extends Plugin {
    public Hastebin() {
        super();
        settingsTab = new SettingsTab(PluginSettings.class).withArgs(settings);
    }

    @Override
    public void start(Context context) {
        var arguments = Arrays.asList(
            new ApplicationCommandOption(ApplicationCommandType.STRING, "text", "The text to upload", null, true, true, null, null),
            new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "send", "Whether the message should be visible for everyone", null, false, true, null, null)
        );

        commands.registerCommand(
            "haste",
            "Create pastes on hastebin",
            arguments,
            ctx -> {
                var text = ctx.getRequiredString("text");
                var send = ctx.getBoolOrDefault("send", false);

                String result;
                String mirror = settings.getString("mirror", "https://haste.powercord.dev") + "/";

                try {
                    HasteResponse res = Http.simpleJsonPost(mirror + "documents", text, HasteResponse.class);
                    result = mirror + res.key;
                } catch (IOException ex) {
                    send = false;
                    result = String.format("Error while uploading to hastebin:\n```\n%s```Consider changing hastebin mirror if this keeps happening", ex.getMessage());
                }

                return new CommandsAPI.CommandResult(result, null, send);
            }
        );
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
