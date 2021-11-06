/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.emojiutility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.views.Button;
import com.aliucord.wrappers.GuildEmojiWrapper;
import com.discord.api.emoji.GuildEmoji;
import com.discord.models.guild.Guild;
import com.discord.stores.StoreStream;
import com.lytefast.flexinput.R;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import kotlin.jvm.functions.Function4;

public class EmojiDownloader {
    private static final Pattern emotePattern = Pattern.compile("<?(a)?:?(\\w{2,32}):(\\d{17,19})>?");

    public static File getEmojiFolder() {
        var downloadFolder = EmojiUtility.mSettings.getString("downloadDir", "");
        File dir;
        if (downloadFolder.equals("") || !(dir = new File(downloadFolder)).exists()) {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Emojis");
        }
        if (!dir.exists() && !dir.mkdirs()) throw new RuntimeException("Failed to create emoji folder " + dir.getAbsolutePath());
        return dir;
    }

    public static String getUrl(long id, boolean animated) {
        var fileName = String.format("%s.%s", id, animated ? "gif" : "png");
        return "https://cdn.discordapp.com/emojis/" + fileName;
    }

    @SuppressLint("SetTextI18n")
    public static void configureSaveButton(Context ctx, Button btn, String name, long id, boolean animated) {
        Runnable callSelf = () -> configureSaveButton(ctx, btn, name, id, animated);
        var file = new File(EmojiDownloader.getEmojiFolder(), String.format("%s.%s", name, animated ? "gif" : "png"));
        var downloaded = file.exists();
        if (downloaded) {
            btn.setText("Remove saved Emoji");
            btn.setBackgroundColor(ctx.getColor(R.c.uikit_btn_bg_color_selector_red));
            btn.setOnClickListener(v -> {
                if (file.delete())
                    callSelf.run();
                else
                    Utils.showToast("Failed to delete emoji :(");
            });
        } else {
            btn.setText("Save Emoji");
            btn.setBackgroundColor(ctx.getColor(R.c.uikit_btn_bg_color_selector_brand));
            btn.setOnClickListener(v -> downloadSingle(id, name, animated, callSelf));
        }
    }

    private static final String[] messages = { "Done!", "Already downloaded", "Something went wrong, sorry :(" };

    public static void downloadSingle(long id, String name, boolean animated, Runnable cb) {
        Utils.threadPool.execute(() -> {
            int res = downloadSingle(id, name, animated);
            if (res != 0) Utils.showToast(messages[res]);
            cb.run();
        });
    }

    public static int downloadSingle(long id, String name, boolean animated) {
        var file = getFileForEmoji(getEmojiFolder(), id, name, animated);
        return download(file, getUrl(id, animated));
    }

    public static int[] downloadFromGuild(Guild guild, ExecutorService executor) {
        var emojis = guild.getEmojis();
        if (emojis.size() == 0) return null;
        if (executor == null) executor = makeExecutor(emojis.size());
        return download(new File(getEmojiFolder(), guild.getName()), emojis, executor);
    }

    public static int[] downloadFromAllGuilds(Function4<String, String, Integer, int[], Object> onNext) {
        var result = new int[3];
        var values = StoreStream.getGuilds().getGuilds().values();
        var it = values.iterator();
        int size = values.size();
        var executor = Executors.newFixedThreadPool(EmojiUtility.mSettings.getInt("threadCount", 10));
        for (int i = 0; it.hasNext(); i++) {
            var guild = it.next();
            onNext.invoke(guild.getName(), String.format("%s/%s", i + 1, size), guild.getEmojis().size(), result);
            var ret = downloadFromGuild(guild, executor);
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
        return download(getEmojiFolder(), emojis, makeExecutor(emojis.size()));
    }

    /** @return int[success, skipped, failed] */
    private static int[] download(File outputFolder, List<GuildEmoji> emojis, ExecutorService executor) {
        if (!outputFolder.exists() && !outputFolder.mkdirs())
            throw new RuntimeException("Could not create directory " + outputFolder.getAbsolutePath());
        var result = new int[3];
        var tasks = new ArrayList<Callable<Integer>>(emojis.size());
        for (var emoji : emojis) {
            tasks.add(() -> {
                var id = GuildEmojiWrapper.getId(emoji);
                var animated = GuildEmojiWrapper.isAnimated(emoji);
                var outFile = getFileForEmoji(outputFolder, id, GuildEmojiWrapper.getName(emoji), animated);
                return download(outFile, getUrl(id, animated));
            });
        }

        try {
            var futures = executor.invokeAll(tasks);
            futures.forEach(f -> {
                try {
                    result[f.get()]++;
                } catch (Throwable th) {
                    EmojiUtility.logger.error(th);
                    result[2]++;
                }
            });
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
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
        if (!EmojiUtility.useHumanNames && outFile.exists()) return 1;
        try {
            var req = new Http.Request(url).execute();
            req.saveToFile(outFile);
            return 0;
        } catch (IOException th) {
            EmojiUtility.logger.error("Failed to download emoji " + outFile, th);
            return 2;
        }
    }

    private static File getFileForEmoji(File dir, long id, String name, boolean animated) {
        var ext = animated ? ".gif" : ".png";
        return EmojiUtility.useHumanNames
            ? createFileForEmoji(dir, name, ext)
            : new File(dir, id + ext);
    }

    private static File createFileForEmoji(File dir, String name, String ext) {
        var file = new File(dir, name + ext);
        try {
            if (file.createNewFile()) return file;
            int increment = 1;
            do {
                file = new File(dir, String.format("%s-%s%s", name, increment++, ext));
            } while (!file.createNewFile());
            return file;
        } catch (Throwable th) {
            EmojiUtility.logger.error(th);
            return file;
        }
    }

    private static ExecutorService makeExecutor(int emojiCount) {
        int maxThreadCount = EmojiUtility.mSettings.getInt("threadCount", 10);
        int size = emojiCount > 20 ? Math.min(maxThreadCount, emojiCount / 20) : 1;
        Utils.showToast(String.format("Downloading emojis with %s threads...", size));
        return Executors.newFixedThreadPool(size);
    }
}
