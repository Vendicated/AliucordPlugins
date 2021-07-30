/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.emojiutil;

import android.content.Context;
import android.os.Environment;
import android.util.Pair;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.plugins.EmojiUtility;
import com.aliucord.wrappers.GuildEmojiWrapper;
import com.discord.api.emoji.GuildEmoji;
import com.discord.models.guild.Guild;
import com.discord.stores.StoreStream;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import kotlin.jvm.functions.Function4;

public class EmojiDownloader {
    private static final Pattern emotePattern = Pattern.compile("<?(a)?:?(\\w{2,32}):(\\d{17,19})>?");

    public static File getEmojiFolder() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Emojis");
    }

    public static Pair<String, String> getFilenameAndUrl(long id, boolean animated) {
        var fileName = String.format(Locale.ENGLISH, "%d.%s", id, animated ? "gif" : "png");
        var url = "https://cdn.discordapp.com/emojis/" + fileName;
        return new Pair<>(fileName, url);
    }

    private static final String[] messages = { "Done!", "Already downloaded", "Something went wrong, sorry :(" };

    public static void downloadSingle(Context ctx, long id, boolean animated, Runnable cb) {
        Utils.threadPool.execute(() -> {
            int res = downloadSingle(id, animated);
            Utils.showToast(ctx, messages[res]);
            cb.run();
        });
    }

    public static int downloadSingle(long id, boolean animated) {
        var nameAndUrl = getFilenameAndUrl(id, animated);
        var file = new File(getEmojiFolder(), nameAndUrl.first);
        return download(file, nameAndUrl.second);
    }

    public static int[] downloadFromGuild(Guild guild) {
        var emojis = guild.getEmojis();
        if (emojis.size() == 0) return null;
        return download(new File(getEmojiFolder(), guild.getName()), emojis);
    }

    public static int[] downloadFromAllGuilds(Function4<String, String, Integer, int[], Object> onNext) {
        var result = new int[3];
        var values = StoreStream.getGuilds().getGuilds().values();
        var it = values.iterator();
        int size = values.size();
        int i = 0;
        for ( ; it.hasNext(); i++) {
            var guild = it.next();
            onNext.invoke(guild.getName(), String.format("%s/%s", i + 1, size), guild.getEmojis().size(), result);
            var ret = downloadFromGuild(guild);
            if (ret == null) continue;
            result[0] += ret[0];
            result[1] += ret[1];
            result[2] += ret[2];
        }
        return result;
    }

    public static int[] downloadFromString(String string) {
        var emojis = new ArrayList<GuildEmoji>();
        var matcher = emotePattern.matcher(string);
        while (matcher.find()) {
            boolean animated = Objects.equals(matcher.group(1), "a");
            String name = matcher.group(2);
            long id = Long.parseLong(Objects.requireNonNull(matcher.group(3)));
            emojis.add(new GuildEmoji(id, name, Collections.emptyList(), true, false, animated, true));
        }
        return download(getEmojiFolder(), emojis);
    }

    /** @return int[success, skipped, failed] */
    private static int[] download(File outputFolder, List<GuildEmoji> emojis) {
        if (!outputFolder.exists() && !outputFolder.mkdirs())
            throw new RuntimeException("Could not create directory " + outputFolder.getAbsolutePath());
        var result = new int[3];
        for (var emoji : emojis) {
            var nameAndUrl = getFilenameAndUrl(GuildEmojiWrapper.getId(emoji), GuildEmojiWrapper.isAnimated(emoji));
            var outFile = new File(outputFolder, nameAndUrl.first);
            int res = download(outFile, nameAndUrl.second);
            result[res]++;
        }
        return result;
    }

    /** @return <ul>
     *  <li>0: Success</li>
     *  <li>1: Skipped</li>
     *  <li>2: Failed</li>
     *  </ul>
     */
    private static int download(File outFile, String url) {
        if (outFile.exists()) return 1;
        try {
            var req = new Http.Request(url).execute();
            try (var oStream = new FileOutputStream(outFile)) {
                req.pipe(oStream);
            }
            return 0;
        } catch (IOException th) {
            EmojiUtility.logger.error("Failed to download emoji " + outFile, th);
            return 2;
        }
    }
}
