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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.views.Button;
import com.discord.databinding.WidgetEmojiSheetBinding;
import com.discord.models.guild.Guild;
import com.discord.utilities.textprocessing.node.EmojiNode;
import com.discord.widgets.emoji.WidgetEmojiSheet;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

@SuppressWarnings("unused")
public class EmojiUtility extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Adds lots of utility for emojis";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private static final String className = "com.discord.widgets.emoji.WidgetEmojiSheet";

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        var clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        try {
            var getEmojiIdAndType = WidgetEmojiSheet.class.getDeclaredMethod("getEmojiIdAndType");
            getEmojiIdAndType.setAccessible(true);
            var getBinding = WidgetEmojiSheet.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);


            patcher.patch(className, "configureButtons", new Class<?>[] {boolean.class, boolean.class, Guild.class}, new MethodHook() {
                @Override
                public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                    super.afterCall(callFrame);

                    var args = callFrame.args;
                    var _this = callFrame.thisObject;

                    try {
                        var emoji = (EmojiNode.EmojiIdAndType.Custom) getEmojiIdAndType.invoke(_this);
                        if (emoji == null) return;
                        String url = String.format(
                                Locale.ENGLISH,
                                "https://cdn.discordapp.com/emojis/%d.%s",
                                emoji.getId(),
                                emoji.isAnimated() ? "gif" : "png");

                        var binding = (WidgetEmojiSheetBinding) getBinding.invoke(_this);
                        if (binding == null) return;
                        var root = (ViewGroup) binding.getRoot();
                        if (root == null) return;
                        var rootLayout = (LinearLayout) root.getChildAt(0);

                        var ctx = rootLayout.getContext();
                        if (ctx == null) return;

                        int marginDpSixteen = dpToPx(ctx, 16);
                        int marginDpEight = dpToPx(ctx, 8);
                        int marginDpFour = dpToPx(ctx, 4);

                        var button = new Button(ctx);
                        button.setText("Copy Link");
                        button.setOnClickListener(v -> {
                            var clip = ClipData.newPlainText("Copy emoji link", url);
                            clipboard.setPrimaryClip(clip);
                            Utils.showToast(ctx, "Copied to clipboard");
                        });

                        var buttonParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        buttonParams.setMargins(0, 0, 0, 0);
                        button.setLayoutParams(buttonParams);

                        var pluginButtonLayout = new com.aliucord.widgets.LinearLayout(ctx);

                        int idx = 2;
                        if (
                                (args[0].equals(false) /* need nitro */ ||
                                args[1].equals(false) /* not on server */
                                ) && args[2] != null
                        ) {
                            // Nitro or Join Button visible
                            pluginButtonLayout.setPadding(marginDpSixteen, marginDpFour, marginDpSixteen, marginDpEight);

                            // Adjust nitro and join button
                            var params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 0, 0);
                            binding.q.setLayoutParams(params); // Nitro
                            binding.o.setLayoutParams(params); // Join

                            // Adjust nitro/join container
                            var joinContainer = (FrameLayout) binding.o.getParent();
                            var containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            containerParams.setMargins(marginDpSixteen, marginDpEight, marginDpSixteen, 0);
                            joinContainer.setLayoutParams(containerParams);
                        } else if (args[2] != null) {
                            // Favourite buttons
                            idx = 3;
                            pluginButtonLayout.setPadding(marginDpSixteen, marginDpFour, marginDpSixteen, marginDpEight);

                            // Adjust  fav button margins
                            var params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, marginDpEight, 0, 0);
                            binding.f.setLayoutParams(params); // Fav
                            binding.h.setLayoutParams(params); // Unfav

                            // Adjust favs container
                            var favsContainer = (FrameLayout) binding.f.getParent();
                            var containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            containerParams.setMargins(marginDpSixteen, marginDpEight, marginDpSixteen, 0);
                            favsContainer.setLayoutParams(containerParams);
                        } else {
                            // No buttons
                            pluginButtonLayout.setPadding(marginDpSixteen, marginDpEight, marginDpSixteen, marginDpEight);
                        }

                        pluginButtonLayout.addView(button);
                        rootLayout.addView(pluginButtonLayout, idx);
                    } catch (IllegalAccessException | InvocationTargetException ignored) { }
                }
            });
        } catch (NoSuchMethodException ignored) { }
    }

    // Convert DP values to their respective PX values, as LayoutParams only accept PX values.
    private int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
        patcher.unpatchAll();
    }
}
