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
import com.discord.utilities.textprocessing.node.EmojiNode;
import com.discord.widgets.emoji.WidgetEmojiSheet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class EmojiUtility extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private static final String className = "com.discord.widgets.emoji.WidgetEmojiSheet";
    public static Map<String, List<String>> getClassesToPatch() {
        var map = new HashMap<String, List<String>>();
        map.put(className, Arrays.asList("configureButtons"));
        return map;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        var clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        try {
            var getEmojiIdAndType = WidgetEmojiSheet.class.getDeclaredMethod("getEmojiIdAndType");
            getEmojiIdAndType.setAccessible(true);
            var getBinding = WidgetEmojiSheet.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);
            patcher.patch(className, "configureButtons", (_this, args, ret) -> {
                try {
                    var emoji = (EmojiNode.EmojiIdAndType.Custom) getEmojiIdAndType.invoke(_this);
                    if (emoji == null) return ret;
                    String url = String.format(
                            Locale.ENGLISH,
                            "https://cdn.discordapp.com/emojis/%d.%s",
                            emoji.getId(),
                            emoji.isAnimated() ? "gif" : "png");

                    var binding = (WidgetEmojiSheetBinding) getBinding.invoke(_this);
                    var rootLinearLayout = (LinearLayout) ((ViewGroup) binding.getRoot()).getChildAt(0);

                    var ctx = ((WidgetEmojiSheet) _this).getContext();

                    assert ctx != null;
                    var marginDpSixteen = dpToPx(ctx, 16);
                    var marginDpEight = dpToPx(ctx, 8);

                    //Params for plugin's buttons
                    var pluginButtonLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    pluginButtonLayoutParams.setMargins(0, marginDpEight, 0, 0);

                    //Adjust favorite button margins, otherwise new buttons look too far away
                    var favoriteButtonLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    favoriteButtonLayoutParams.setMargins(0, marginDpSixteen, 0, 0);

                    binding.f.setLayoutParams(favoriteButtonLayoutParams);
                    binding.h.setLayoutParams(favoriteButtonLayoutParams);

                    //Adjust guild buttons' parent LinearLayout for the same reason
                    var guildLinearLayoutParams = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    guildLinearLayoutParams.setMargins(marginDpSixteen, marginDpSixteen, marginDpSixteen, 0);
                    binding.k.setLayoutParams(guildLinearLayoutParams);

                    //LinearLayout for plugin's buttons
                    var pluginButtonLinearLayout = new com.aliucord.widgets.LinearLayout(ctx);
                    pluginButtonLinearLayout.setPadding(marginDpSixteen, 0, marginDpSixteen, marginDpSixteen);

                    var button = new Button(ctx, false);
                    button.setLayoutParams(pluginButtonLayoutParams);
                    button.setText("Copy Link");
                    button.setOnClickListener(v -> {
                        var clip = ClipData.newPlainText("Copied to clipboard", url);
                        clipboard.setPrimaryClip(clip);
                        Utils.showToast(ctx, "Copied to clipboard");
                    });

                    pluginButtonLinearLayout.addView(button);
                    rootLinearLayout.addView(pluginButtonLinearLayout);
                } catch (Throwable ignored) { }
                return ret;
            });
        } catch (Throwable ignored) { }
    }

    //Converting DP values to their respective PX values,
    //as LayoutParams only accept PX values.
    private int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
        patcher.unpatchAll();
    }
}
