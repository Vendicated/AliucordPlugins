/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.MessageEmbed;
import com.aliucord.entities.Plugin;

import com.aliucord.plugins.urban.ApiResponse;
import com.aliucord.plugins.urban.ApiResponse.Definition;

import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class UrbanDictionary extends Plugin {
    private final String baseUrl = "https://api.urbandictionary.com/v0/define";
    private final String thumbsUp = "\uD83D\uDC4D";
    private final String thumbsDown = "\uD83D\uDC4E";

    @NonNull
    @Override
    public Manifest getManifest() {
        Manifest manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Get definitions from urbandictionary.com";
        manifest.version = "1.0.2";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context context) {
        List<ApplicationCommandOption> arguments = new ArrayList<>();
        arguments.add(new ApplicationCommandOption(ApplicationCommandType.STRING, "search", "The word to search for", null, true, true, null, null));
        arguments.add(new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "send", "Whether the result should be visible for everyone", null, false, true, null, null));

        commands.registerCommand(
                "urban",
                "Get a definition from urbandictionary.com",
                arguments,
                args -> {
                    String search = (String) args.get("search");
                    Object _send = args.get("send");
                    boolean send = _send != null && (boolean) _send;

                    if (search == null) return new CommandsAPI.CommandResult("You did not specify a search term", null, false);

                    String url = new Http.QueryBuilder(baseUrl).append("term", search).toString();

                    List<com.discord.api.message.embed.MessageEmbed> embed = null;
                    String result;
                    try {
                        ApiResponse res = Http.simpleJsonGet(url, ApiResponse.class);
                        if (res.list.size() == 0) {
                            result = "No definition found for `" + search + "`";
                            send = false;
                        } else {
                            Definition data = res.list.get(0);
                            String votes = String.format(Locale.ENGLISH, "%s %d | %s %d", thumbsUp, data.thumbs_up, thumbsDown, data.thumbs_down);
                            if (send) {
                                result = String.format(Locale.ENGLISH,"**__'%s' on urban dictionary:__**\n>>> %s\n\n<%s>\n\n%s",
                                        data.word,
                                        trimLong(data.definition.replaceAll("\\[", "").replaceAll("]", ""), 1000),
                                        data.permalink,
                                        votes
                                );
                            } else {
                                result = "I found the following:";
                                embed = Collections.singletonList(
                                            new MessageEmbed()
                                                .setTitle(data.word)
                                                .setUrl(data.permalink)
                                                .setDescription(formatUrls(data.definition))
                                                .setFooter(votes, null)
                                                .embed
                                        );
                            }
                        }
                    } catch (IOException t) {
                        result = "Something went wrong: " + t.getMessage();
                        send = false;
                    }

                    return new CommandsAPI.CommandResult(result, embed, send);
                }
        );
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }

    private String trimLong(String str, int max) {
        return str.length() < max ? str : str.substring(0, max -3) + "...";
    }

    private String formatUrls(String raw) {
        StringBuilder sb = new StringBuilder();
        StringBuilder wb = new StringBuilder();
        boolean resolvingWord = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '[') resolvingWord = true;
            else if (c == ']') {
                String word = wb.toString();
                wb.setLength(0);
                resolvingWord = false;
                sb.append(String.format("[%s](https://www.urbandictionary.com/define.php?term=%s)", word, encodeUri(word)));
            } else if (resolvingWord) wb.append(c);
            else sb.append(c);
        }
        return sb.toString();
    }

    private String encodeUri(String raw) {
        try {
            return URLEncoder.encode(raw, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            throw new AssertionError("UTF-8 is not supported somehow");
        }
    }
}
