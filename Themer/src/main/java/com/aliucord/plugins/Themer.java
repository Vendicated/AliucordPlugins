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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.aliucord.*;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.patcher.PinePrePatchFn;
import com.aliucord.plugins.themer.ThemeManager;
import com.aliucord.plugins.themer.ThemerSettings;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.lytefast.flexinput.R$c;

import java.io.File;
import java.util.HashMap;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class Themer extends Plugin {
    public static final String[] ACCENT_NAMES = new String[] {
            "link", "link_light", "brand", "brand_360", "brand_500", "brand_600", "brand_new", "brand_new_360", "brand_new_500", "brand_new_530", "brand_new_560", "brand_new_600"
    };
    public static final String[] BACKGROUND_NAMES = new String[] {
            "dark_grey_2", "primary_600", "primary_660", "primary_800", "primary_dark_600", "primary_dark_630", "primary_dark_800"
    };
    public static final String[] BACKGROUND_SECONDARY_NAMES = new String[] {
            "primary_500", "primary_630", "primary_700", "primary_dark_660", "primary_dark_700"
    };

    public static final Logger logger = new Logger("Themer");

    public Themer() {
        super();
        settingsTab = new SettingsTab(ThemerSettings.class);
    }

    @NonNull
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[]{
                new Manifest.Author("Vendicated", 343383572805058560L),
                new Manifest.Author("AAGaming", 373833473091436546L),
        };
        manifest.description = "Custom themes.";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    public final HashMap<Integer, String> colorToName = new HashMap<>();

    public void start(Context ctx) {
        var themeDir = new File(Constants.BASE_PATH, "themes");
        if (!themeDir.exists() && !themeDir.mkdir()) throw new RuntimeException("Failed to create theme folder.");

        try {
            var res = ctx.getResources();
            var theme = ctx.getTheme();

            for (var field : R$c.class.getDeclaredFields()) {
                String colorName = field.getName();
                int colorId = field.getInt(null);
                int color = res.getColor(colorId, theme);
                if (color == 0) continue;
                colorToName.put(color, colorName);
            }

            ThemeManager.init(ctx, settings, false);

            var viewHook = new MethodHook() {
                private boolean busy = false;
                @Override
                public void afterCall(Pine.CallFrame callFrame) {
                    if (busy) return;
                    var background = ((View) callFrame.thisObject).getBackground();

                    if (background instanceof ColorDrawable) {
                        int color = ((ColorDrawable) background).getColor();
                        var colorName = colorToName.get(color);
                        if (colorName == null) return;
                        colorName = colorName.replace("brand_new", "brand"); //this trash
                        var customColor = ThemeManager.getColor(colorName);
                        if (customColor == null) return;
                        busy = true;
                        ((View) callFrame.thisObject).setBackground(new ColorDrawable(customColor));
                        busy = false;
                    }
                }
            };

            patcher.patch(View.class.getDeclaredMethod("onFinishInflate"), viewHook);
            patcher.patch(View.class.getDeclaredMethod("setBackground", Drawable.class), viewHook);

            patcher.patch(ColorCompat.class.getDeclaredMethod("getThemedColor", Context.class, int.class), new PinePatchFn(callFrame -> {
                int ret = (int) callFrame.getResult();
                var colorName = colorToName.get(ret);
                if (colorName == null) return;
                colorName = colorName.replace("brand_new", "brand"); // this trash
                var customColor = ThemeManager.getColor(colorName);
                if (customColor != null) callFrame.setResult(customColor);
            }));

            patcher.patch(ColorCompat.class.getDeclaredMethod("setStatusBarColor", Window.class, int.class, boolean.class), new PinePrePatchFn(callFrame -> {
                var color = ThemeManager.getColor("statusbar_color");
                if (color != null) callFrame.args[1] = color;
            }));

            patcher.patch(TextInputLayout.class.getDeclaredMethod("calculateBoxBackgroundColor"), new PinePrePatchFn(callFrame -> {
                var color = ThemeManager.getColor("input_background_color");
                if (color != null) callFrame.setResult(color);
            }));
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public void stop(Context context) {
        patcher.unpatchAll();
        colorToName.clear();
        ThemeManager.activeTheme = null;
        Utils.appActivity.recreate();
    }
}