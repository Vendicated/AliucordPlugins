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
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;

import com.aliucord.*;
import com.aliucord.api.SettingsAPI;
import com.aliucord.plugins.Themer;
import com.aliucord.updater.Updater;

import org.json.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ThemeManager {
    public static final String[] ACCENT_NAMES = new String[] {
            "link",
            "link_light",
            "brand",
            "brand_360",
            "brand_500",
            "brand_600",
            "brand_new",
            "brand_new_360",
            "brand_new_500",
            "brand_new_530",
            "brand_new_560",
            "brand_new_600"
    };
    public static final String[] BACKGROUND_NAMES = new String[] {
            "dark_grey_2",
            "primary_600",
            "primary_660",
            "primary_800",
            "primary_dark_600",
            "primary_dark_630",
            "primary_dark_800"
    };
    public static final String[] BACKGROUND_SECONDARY_NAMES = new String[] {
            "primary_500",
            "primary_630",
            "primary_700",
            "primary_dark_660",
            "primary_dark_700"
    };

    public static final Map<String, Boolean> BACKGROUNDS = new HashMap<>() {{
        for (String backgroundName : BACKGROUND_NAMES) put(backgroundName, true);
        for (String backgroundSecondaryName : BACKGROUND_SECONDARY_NAMES) put(backgroundSecondaryName, true);
    }};

    public static String readFile(File file) throws IOException {
        int size = Math.toIntExact(file.length());
        var bytes = new byte[size];
        try (var fis = new FileInputStream(file)) {
            fis.read(bytes);
        }
        return new String(bytes);
    }

    public static final class ThemeInfo {
        public final File file;
        public String name;
        public String author = "Anonymous";
        public String version = "1.0.0";
        public String license = null;
        public String updaterUrl = null;

        public ThemeInfo(File file) {
            this.file = file;
            this.name = file.getName().replace(".json", "");
            try {
                var json = new JSONObject(new JSONTokener(readFile(file)));
                if (json.has("name")) name = json.getString("name");
                if (json.has("author")) author = json.getString("author");
                if (json.has("version")) version = json.getString("version");
                if (json.has("license")) license = json.getString("license");
                if (json.has("updater")) updaterUrl = json.getString("updater");
            } catch (Throwable ignored) {}
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

    public static Map<Integer, Typeface> fonts;
    public static final List<ThemeInfo> themes = new ArrayList<>();
    public static SettingsAPI settings;

    public static Map<String, Integer> activeTheme;
    public static Map<Integer, Integer> drawableTints;
    public static BitmapDrawable customBackground;
    public static int backgroundTransparency = -1;

    public static void init(Context ctx, SettingsAPI sets, boolean shouldRerender) {
        settings = sets;
        loadThemes(ctx, true, shouldRerender);
    }

    public static void loadThemes(Context ctx, boolean shouldLoad, boolean shouldRerender) {
        var files = new File(Constants.BASE_PATH, "themes").listFiles();
        if (files == null) return;
        for (var file : files) {
            if (file.getName().equals("default.json") || !file.getName().endsWith(".json")) continue;
            var theme = new ThemeInfo(file);
            if (shouldLoad && theme.isEnabled()) {
                boolean success = loadTheme(theme, shouldRerender);
                if (!success) continue;
            }
            themes.add(theme);
        }
        themes.sort(Comparator.comparing(t -> t.name));
    }

    private static final int colorPrefixLength = "color_".length();
    private static final int drawableColorPrefixLength = "drawablecolor_".length();

    public static boolean loadFont(int id, File file, boolean shouldRerender) {
        var font = Typeface.createFromFile(file);
        fonts.put(id, font);
        if (shouldRerender) Utils.appActivity.recreate();
        return true;
    }

    public static void loadFont(int id, String url, boolean shouldRerender) {
        Utils.threadPool.execute(() -> {
            try (var req = new Http.Request(url)) {
                var res = req.execute();
                var file = new File(Utils.appActivity.getCacheDir(), "font-" + id + ".ttf");
                try (var fos = new FileOutputStream(file)) {
                    res.pipe(fos);
                    Utils.mainThread.post(() -> loadFont(id, file, shouldRerender));
                }
            } catch (IOException ex) {
                Themer.logger.error("Failed to load font " + url, ex);
            }
        });
    }

    public static void loadBackground(String url) {
        Utils.threadPool.execute(() -> {
            try (var stream = new Http.Request(url).execute().stream()) {
                var bitmap = BitmapFactory.decodeStream(stream);
                customBackground = new BitmapDrawable(Utils.appActivity.getResources(), bitmap);
                if (Themer.appContainer != null)
                    Utils.mainThread.post(() -> {
                        Themer.appContainer.setBackground(customBackground);
                        Themer.appContainer = null;
                    });
            } catch (Throwable th) {
                Themer.logger.error("Failed to load background " + url, th);
                Themer.appContainer = null;
            }
        });
    }

    public static boolean loadTheme(ThemeInfo theme, boolean shouldRerender) {
        String content;
        try {
            content = readFile(theme.file);
        } catch (IOException ex) {
            Themer.logger.error("Failed to load theme " + theme, ex);
            return false;
        }

        try {
            var json = new JSONObject(new JSONTokener(content));
            var it = json.keys();

            fonts = new HashMap<>();
            activeTheme = new HashMap<>();
            drawableTints = new HashMap<>();
            customBackground = null;
            backgroundTransparency = -1;
            AttributeManager.activeTheme = new HashMap<>();

            while (it.hasNext()) {
                var key = it.next();

                switch (key) {
                    case "name":
                    case "author":
                    case "version":
                    case "license":
                    case "updater":
                        continue;
                    case "background_url":
                        loadBackground(json.getString(key));
                        if (backgroundTransparency == -1) backgroundTransparency = Themer.DEFAULT_ALPHA;
                        continue;
                }

                if (key.startsWith("font")) {
                    if (key.equals("font")) loadFont(-1, json.getString(key), shouldRerender);
                    else if (key.charAt(4) == '_'){
                        var fontName = key.substring(5);
                        try {
                            var font = Constants.Fonts.class.getField(fontName);
                            loadFont((int) Objects.requireNonNull(font.get(null)), json.getString(key), shouldRerender);
                        } catch (ReflectiveOperationException ex) {
                            Themer.logger.error("No such font: " + fontName, ex);
                        }
                    }
                    continue;
                }

                var val = json.getInt(key);

                if (AttributeManager.mappings.containsKey(key)) {
                    AttributeManager.loadAttr(key, val);
                }

                switch (key) {
                    case "background_transparency":
                        if (val < 0 || val > 255) throw new IndexOutOfBoundsException("background_transparency must be 0-255, was " + val);
                        backgroundTransparency = val;
                        continue;
                    case "simple_accent_color":
                        themeAll(ACCENT_NAMES, val);
                        AttributeManager.loadAll(AttributeManager.SIMPLE_ACCENT_ATTRS, val);
                        tintDrawable("ic_nitro_rep", val);
                        continue;
                    case "simple_bg_color":
                        themeAll(BACKGROUND_NAMES, val);
                        AttributeManager.loadAll(AttributeManager.SIMPLE_BACKGROUND_ATTRS, val);
                        continue;
                    case "simple_bg_secondary_color":
                        themeAll(BACKGROUND_SECONDARY_NAMES, val);
                        AttributeManager.loadAll(AttributeManager.SIMPLE_BACKGROUND_SECONDARY_ATTRS, val);
                        activeTheme.put("input_background_color", val);
                        activeTheme.put("statusbar_color", val);
                        tintDrawable("drawable_overlay_channels_active_dark", val);
                        tintDrawable("drawable_overlay_channels_active_light", val);
                        continue;
                    case "mention_highlight":
                        activeTheme.put("status_yellow_500", getColorWithAlpha("ff", val));
                        continue;
                    case "active_channel_color":
                        tintDrawable("drawable_overlay_channels_active_dark", val);
                        tintDrawable("drawable_overlay_channels_active_light", val);
                    case "statusbar_color":
                    case "input_background_color":
                        activeTheme.put(key, val);
                        continue;
                    case "color_brand_500":
                        tintDrawable("ic_nitro_rep", val);
                        break;
                }

                if (key.startsWith("color_")) {
                    put(key.substring(colorPrefixLength), val);
                } else if (key.startsWith("drawablecolor_")) {
                    boolean success = tintDrawable(key.substring(drawableColorPrefixLength), val);
                    if (!success) Themer.logger.warn("Failed to tint drawable " + key.substring(drawableColorPrefixLength));
                } else {
                    Themer.logger.warn("Unrecognised key " + key);
                }
            }
        } catch (JSONException ex) {
            Themer.logger.error("Failed to load theme " + theme.name, ex);
            return false;
        }

        if (shouldRerender) Utils.appActivity.recreate();

        if (theme.updaterUrl != null && theme.version != null) Utils.threadPool.execute(() -> {
            try (var req = new Http.Request(theme.updaterUrl)) {
                var res = req.execute().text();
                var json = new JSONObject(res);
                if (json.has("version") && Updater.isOutdated("Theme " + theme.name, theme.version, json.getString("version"))) {
                    try (var fos = new FileOutputStream(theme.file)) {
                        fos.write(res.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Throwable ex) {
                Themer.logger.error("Failed to update theme " + theme.name, ex);
            }
        });

        return true;
    }

    private static void put(String key, int value) {
        int id = Utils.getResId(key, "color");
        if (id != 0) {
            Themer.idToColor.put(id, value);
            activeTheme.put(key, value);
            /*try {
                int color = Utils.appActivity.getColor(id);
                Themer.colorReplacements.put(color, value);
            } catch (Throwable ignored) {}*/
        } else {
            Themer.logger.warn("Unrecognised colour " + key);
        }
    }

    public static Integer getColor(String key) {
        if (activeTheme == null) return null;
        return activeTheme.get(key);
    }

    public static Integer getTint(int key) {
        if (drawableTints == null) return null;
        return drawableTints.get(key);
    }

    public static Integer getColorWithAlpha(String alpha, int color) {
        return Color.parseColor("#" + alpha + Integer.toHexString(color).substring(2));
    }

    private static void themeAll(String[] colors, int color) {
        for (var name : colors)
            put(name, color);
    }

    private static boolean tintDrawable(String name, int color) {
        int id = Utils.getResId(name, "drawable");
        if (id == 0) return false;
        drawableTints.put(id, color);
        return true;
    }
}
