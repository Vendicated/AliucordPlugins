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

import com.aliucord.Utils;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.MessageEmbed;
import com.aliucord.entities.Plugin;

import com.aliucord.plugins.Urban.ApiResponse;
import com.aliucord.plugins.Urban.ApiResponse.Definition;

import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.models.domain.ModelMessageEmbed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class UrbanDictionary extends Plugin {
    private final String baseUrl = "https://api.urbandictionary.com/v0/define?term=";
    private final String thumbsUp = "\uD83D\uDC4D";
    private final String thumbsDown = "\uD83D\uDC4E";

    @NonNull
    @Override
    public Manifest getManifest() {
        Manifest manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Get definitions from urbandictionary.com";
        manifest.version = "1.0.0";
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
                    String url = baseUrl + encodeUri(search);

                    MessageEmbed embed = null;
                    String result = null;
                    try {
                        String raw = fetch(url);
                        ApiResponse res = parse(raw);
                        if (res.list.size() == 0) {
                            result = "No definition found for `" + search + "`";
                            send = false;
                        } else {
                            Definition data = res.list.get(0);
                            String votes = String.format(Locale.ENGLISH, "%s %d | %s %d", thumbsUp, data.thumbs_up, thumbsDown, data.thumbs_down);
                            if (send) {
                                result = String.format(Locale.ENGLISH,
                                        "**__'%s' on urban dictionary:__**\n>>> %s\n\n<%s>\n\n%s",
                                        data.word,
                                        trimLong(data.definition.replaceAll("\\[", "").replaceAll("]", ""), 1000),
                                        data.permalink,
                                        votes
                                );
                            } else {
                                embed = new MessageEmbed();
                                embed.setTitle(data.word);
                                embed.setUrl(data.permalink);
                                embed.setDescription(formatUrls(data.definition));
                                // FIXME: Switch to actual class if this gets added to DiscordStubs
                                try {
                                    ModelMessageEmbed.Item footer = new ModelMessageEmbed.Item();
                                    Utils.setPrivateField(footer.getClass(), footer, "text", votes);
                                    embed.setFooter(footer);
                                } catch (Throwable ignored) { }
                            }
                        }
                    } catch (Throwable t) {
                        result = "Something went wrong: " + t.getMessage();
                        send = false;
                    }

                    List<ModelMessageEmbed> embeds = null;
                    if (embed != null) {
                        result = "I found the following:";
                        embeds = Collections.singletonList(embed);
                    }
                    return new CommandsAPI.CommandResult(result, embeds, send);
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
                sb.append(String.format(Locale.ENGLISH, "[%s](https://www.urbandictionary.com/define.php?term=%s)", word, encodeUri(word)));
            } else if (resolvingWord) wb.append(c);
            else sb.append(c);
        }
        return sb.toString();
    }

    private String encodeUri(String raw) {
        try {
            return URLEncoder.encode(raw, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return raw;
        }
    }

    private ApiResponse parse(String raw) {
        return Utils.fromJson(raw, ApiResponse.class);
    }

    private String fetch(String url) throws Throwable {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("User-Agent", "Aliucord");

        String line;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        return sb.toString().trim();
    }
}
