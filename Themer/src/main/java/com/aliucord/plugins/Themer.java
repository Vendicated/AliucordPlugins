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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.*;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.patcher.PinePrePatchFn;
import com.aliucord.plugins.themer.*;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.lytefast.flexinput.R$c;

import java.io.File;
import java.util.HashMap;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class Themer extends Plugin {
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
        manifest.description = "Adds support for custom themes & fonts";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    public final HashMap<Integer, String> colorToName = new HashMap<>();

    public void start(Context ctx) throws Throwable {
        var themeDir = new File(Constants.BASE_PATH, "themes");
        if (!themeDir.exists() && !themeDir.mkdir()) throw new RuntimeException("Failed to create theme folder.");

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

        patcher.patch("com.discord.widgets.tabs.WidgetTabsHost", "onViewBound", new Class<?>[] { View.class }, new PinePatchFn(callFrame -> {
            if (ThemeManager.customBackground != null) ((View) callFrame.args[0]).setBackground(ThemeManager.customBackground);
        }));

        var viewHook = new MethodHook() {
            @Override
            public void afterCall(Pine.CallFrame callFrame) {
                var view = (View) callFrame.thisObject;
                var background = view.getBackground();

                if (background instanceof ColorDrawable) {
                    var colorDrawable = (ColorDrawable) background;
                    int color = colorDrawable.getColor();
                    var colorName = colorToName.get(color);
                    if (colorName == null) return;

                    var customColor = ThemeManager.getColor(colorName);
                    if (customColor != null) colorDrawable.setColor(customColor);

                    if (ThemeManager.customBackground != null && ThemeManager.BACKGROUNDS.containsKey(colorName)) {
                        colorDrawable.setAlpha(ThemeManager.backgroundTransparency == -1 ? 150 : ThemeManager.backgroundTransparency);
                    }
                }

                if (ThemeManager.font != null && view instanceof TextView) {
                    ((TextView) view).setTypeface(ThemeManager.font);
                }
            }
        };

        MethodHook fontHook = new MethodHook() {
            @Override public void beforeCall(Pine.CallFrame callFrame) {
                if (ThemeManager.font != null) callFrame.setResult(ThemeManager.font);
            }
        };

         // None of these call each other and the underlying private method is not stable across Android versions so oh boy 3 patches here we go
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class), fontHook);
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class, ResourcesCompat.FontCallback.class, Handler.class), fontHook);
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class, TypedValue.class, int.class, ResourcesCompat.FontCallback.class), fontHook);

        patcher.patch(View.class.getDeclaredMethod("onFinishInflate"), viewHook);
        patcher.patch(View.class.getDeclaredMethod("setBackground", Drawable.class), viewHook);

        patcher.patch(ColorCompat.class.getDeclaredMethod("getThemedColor", Context.class, int.class), new PinePatchFn(callFrame -> {
            int ret = (int) callFrame.getResult();
            var colorName = colorToName.get(ret);
            if (colorName == null) return;
            // colorName = colorName.replace("brand_new", "brand");
            var customColor = ThemeManager.getColor(colorName);
            if (customColor != null || (customColor = AttributeManager.getAttr((int) callFrame.args[1])) != null) callFrame.setResult(customColor);
        }));

        patcher.patch(Resources.class.getDeclaredMethod("getDrawableForDensity", int.class, int.class, Resources.Theme.class), new PinePatchFn(callFrame -> {
            var drawable = (Drawable) callFrame.getResult();
            if (drawable != null) {
                var color = ThemeManager.getTint((int) callFrame.args[0]);
                if (color != null) drawable.setTint(color);
            }
        }));

        patcher.patch(Resources.Theme.class.getDeclaredMethod("resolveAttribute", int.class, TypedValue.class, boolean.class), new PinePatchFn(callFrame -> {
            int attr = (int) callFrame.args[0];
            var color = AttributeManager.getAttr(attr);
            if (color != null) ((TypedValue) callFrame.args[1]).data = color;
        }));

        patcher.patch(ColorCompat.class.getDeclaredMethod("setStatusBarColor", Window.class, int.class, boolean.class), new PinePrePatchFn(callFrame -> {
            var color = ThemeManager.getColor("statusbar_color");
            if (color != null) callFrame.args[1] = color;
        }));

        patcher.patch(TextInputLayout.class.getDeclaredMethod("calculateBoxBackgroundColor"), new PinePrePatchFn(callFrame -> {
            var color = ThemeManager.getColor("input_background_color");
            if (color != null) callFrame.setResult(color);
        }));
    }

    public void stop(Context context) {
        patcher.unpatchAll();
        colorToName.clear();
        ThemeManager.activeTheme = null;
        ThemeManager.font = null;
        ThemeManager.drawableTints = null;
        AttributeManager.activeTheme = null;
        Utils.appActivity.recreate();
    }
}