/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.themer;

import android.content.Context;
import android.graphics.Color;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.plugins.Themer;

import org.json.*;

import java.io.*;
import java.util.*;

public class ThemeManager {
    public static final class ThemeInfo {
        public final File file;
        public final String name;

        public ThemeInfo(File file) {
            this.file = file;
            this.name = file.getName().replace(".json", "");
        }

        public void enable() {
            for (var theme : themes) theme.disable();
            settings.setBool(getPrefKey(), true);
        }

        public boolean load() {
            return loadTheme(this, true);
        }

        public void disable() {
            if (isEnabled()) settings.setBool(getPrefKey(), false);
        }

        public boolean isEnabled() {
            return settings.getBool(getPrefKey(), false);
        }

        public final String getPrefKey() {
            return name + "-enabled";
        }
    }


    public static final List<ThemeInfo> themes = new ArrayList<>();
    public static SettingsAPI settings;

    public static Map<String, Integer> activeTheme = new HashMap<>();

    public static void init(Context ctx, SettingsAPI sets, boolean shouldRerender) {
        settings = sets;
        loadThemes(ctx, true, shouldRerender);
    }

    public static void loadThemes(Context ctx, boolean shouldLoad, boolean shouldRerender) {
        var files = new File(Constants.BASE_PATH, "themes").listFiles();
        if (files == null) return;
        for (var file : files) {
            if (file.getName().equals("default.json")) continue;
            var theme = new ThemeInfo(file);
            if (shouldLoad && theme.isEnabled()) {
                boolean success = loadTheme(theme, shouldRerender);
                Utils.showToast(ctx, (success ? "Successfully loaded theme " : "Failed to load theme ") + theme.name);
                if (!success) continue;
            }
            themes.add(theme);
        }
        themes.sort(Comparator.comparing(t -> t.name));
    }

    private static final int colorPrefixLength = "color_".length();
    private static final int drawableColorPrefixLength = "drawablecolor_".length();

    public static boolean loadTheme(ThemeInfo theme, boolean shouldRerender) {
        int size = Math.toIntExact(theme.file.length());
        var bytes = new byte[size];
        try (var fis = new FileInputStream(theme.file)) {
            fis.read(bytes);
        } catch (IOException ex) {
            Themer.logger.error("Failed to load theme " + theme, ex);
            return false;
        }
        var content = new String(bytes);

        try {
            var json = new JSONObject(new JSONTokener(content));
            var it = json.keys();

            // TODO: -- support active_channel_color: https://github.com/Aliucord/DiscordThemer/blob/7e194cbf9c24e85004287332a8813ce9e395ef82/app/src/main/java/com/aliucord/themer/Main.java#L134-L137
            // TODO: -- Support accent colours (patch attributes)
            // TODO: -- Background image ???
            // TODO: -- Add button to generate theme boilerplate json to make your own themes

            activeTheme = new HashMap<>();

            while (it.hasNext()) {
                var key = it.next();
                if (key.equals("name") || key.equals("author") || key.equalsIgnoreCase("copyright") || key.equalsIgnoreCase("license")) continue;

                var val = json.getInt(key);
                switch (key) {
                    case "simple_bg_color":
                        themeAll(Themer.BACKGROUND_NAMES, val);
                        continue;
                    case "simple_bg_secondary_color":
                        themeAll(Themer.BACKGROUND_SECONDARY_NAMES, val);
                        activeTheme.put("input_background_color", val);
                        activeTheme.put("statusbar_color", val);
                        // TODO: -- Tint active channels: https://github.com/Aliucord/DiscordThemer/blob/7e194cbf9c24e85004287332a8813ce9e395ef82/app/src/main/java/com/aliucord/themer/Main.java#L134-L137
                        continue;
                    case "simple_accent_color":
                        themeAll(Themer.ACCENT_NAMES, val);
                        // TODO: -- Fix nitro icon: https://github.com/Aliucord/DiscordThemer/blob/7e194cbf9c24e85004287332a8813ce9e395ef82/app/src/main/java/com/aliucord/themer/Main.java#L130
                        continue;
                    case "mention_highlight":
                        activeTheme.put("status_yellow_500", Color.parseColor("#ff" + Integer.toHexString(val).substring(2)));
                        continue;
                }
                if (key.startsWith("color_")) {
                    activeTheme.put(key.substring(colorPrefixLength), val);
                } else if (key.startsWith("drawablecolor_")) {
                    // TODO: -- support drawable_color: https://github.com/Aliucord/DiscordThemer/blob/7e194cbf9c24e85004287332a8813ce9e395ef82/app/src/main/java/com/aliucord/themer/Main.java#L139-L147
                    Themer.logger.warn("Drawable colors are not supported yet, skipped " + key);
                } else {
                    Themer.logger.warn("Unrecognized key " + key);
                }
            }
        } catch (JSONException ex) {
            Themer.logger.error("Failed to load theme " + theme.name, ex);
            return false;
        }

        if (shouldRerender) Utils.appActivity.recreate();
        return true;
    }

    public static Integer getColor(String key) {
        if (activeTheme == null) return null;
        return activeTheme.get(key);
    }

    private static void themeAll(String[] colors, int color) {
        for (var name : colors)
            activeTheme.put(name, color);
    }
}
