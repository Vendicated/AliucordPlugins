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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.aliucord.*;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.patcher.PinePrePatchFn;
import com.aliucord.plugins.themer.*;
import com.discord.app.AppFragment;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.lytefast.flexinput.R$c;

import java.io.File;
import java.util.HashMap;

import rx.functions.Action1;
import top.canyie.pine.Pine;

public class Themer extends Plugin {
    public static final int DEFAULT_ALPHA = 150;
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

    public static View appContainer;

    private Integer getReplacement(Object color) {
        var name = colorToName.get((int) color);
        return name == null ? null : ThemeManager.getColor(name);
    }

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

        final boolean enableTransparency = settings.getBool("enableTransparency", false);
        if (enableTransparency) {
            int containerId = Utils.getResId("widget_tabs_host_container", "id");
            patcher.patch(AppFragment.class.getDeclaredMethod("onViewBound", View.class), new PinePatchFn(callFrame -> {
                var clazz = callFrame.thisObject.getClass();
                var className = clazz.getSimpleName();
                var view = (View) callFrame.args[0];
                if (className.equals("WidgetChatList")) {
                    while (view.getId() != containerId) view = (View) view.getParent();
                    if (ThemeManager.customBackground != null)
                        view.setBackground(ThemeManager.customBackground);
                    else
                        appContainer = view;
                } else if (className.toLowerCase().contains("settings") || SettingsPage.class.isAssignableFrom(clazz)) {
                    Integer tint = ThemeManager.getColor("primary_dark_600");
                    var bg = (Drawable) ThemeManager.customBackground;
                    if (tint != null || (tint = view.getContext().getColor(R$c.primary_dark_600)) != 0) {
                        bg = ThemeManager.customBackground.mutate();
                        bg.setColorFilter(tint, PorterDuff.Mode.DARKEN);
                    }
                    view.setBackground(bg);
                }
            }));
        }

        var fontHook = new PinePrePatchFn(callFrame -> {
            if (ThemeManager.font != null) callFrame.setResult(ThemeManager.font);
        });

         // None of these call each other and the underlying private method is not stable across Android versions so oh boy 3 patches here we go
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class), fontHook);
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class, ResourcesCompat.FontCallback.class, Handler.class), fontHook);
        patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class, TypedValue.class, int.class, ResourcesCompat.FontCallback.class), fontHook);

        Action1<Pine.CallFrame> setColorHook = enableTransparency
                ? callFrame -> {
                    var name = colorToName.get((int) callFrame.args[0]);
                    if (name != null) {
                        var replacement = ThemeManager.getColor(name);
                        if (replacement != null) callFrame.args[0] = replacement;
                        if (ThemeManager.BACKGROUNDS.containsKey(name))
                            callFrame.args[0] = ColorUtils.setAlphaComponent(
                                    (int) callFrame.args[0],
                                    ThemeManager.backgroundTransparency == -1
                                            ? DEFAULT_ALPHA
                                            : ThemeManager.backgroundTransparency
                            );
                    }
                }
                : callFrame -> {
                    var replacement = getReplacement(callFrame.args[0]);
                    if (replacement != null) callFrame.args[0] = replacement;
                };

        patcher.patch(ColorDrawable.class.getDeclaredMethod("setColor", int.class), new PinePrePatchFn(setColorHook));

        var replaceResult = new PinePatchFn(callFrame -> {
            var replacement = getReplacement(callFrame.getResult());
            if (replacement != null) callFrame.setResult(replacement);
        });

        patcher.patch(ColorCompat.class.getDeclaredMethod("getThemedColor", Context.class, int.class), replaceResult);
        patcher.patch(ColorStateList.class.getDeclaredMethod("getColorForState", int[].class, int.class), replaceResult);

        patcher.patch(Resources.class.getDeclaredMethod("getDrawableForDensity", int.class, int.class, Resources.Theme.class), new PinePatchFn(callFrame -> {
            var color = ThemeManager.getTint((int) callFrame.args[0]);
            if (color != null) {
                var drawable = (Drawable) callFrame.getResult();
                if (drawable != null) drawable.setTint(color);
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
        ThemeManager.customBackground = null;
        AttributeManager.activeTheme = null;
        Utils.appActivity.recreate();
    }
}