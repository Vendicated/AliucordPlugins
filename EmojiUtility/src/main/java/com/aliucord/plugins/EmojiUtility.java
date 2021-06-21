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
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.plugins.emojiutil.CloneModal;
import com.aliucord.views.Button;
import com.discord.databinding.WidgetEmojiSheetBinding;
import com.discord.models.guild.Guild;
import com.discord.utilities.textprocessing.node.EmojiNode;
import com.discord.widgets.emoji.WidgetEmojiSheet;
import com.lytefast.flexinput.R$c;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

@SuppressWarnings("unused")
public class EmojiUtility extends Plugin {
    public static final Logger logger = new Logger("EmojiUtility");

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
        int id = View.generateViewId();
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
                        String fileName = String.format(Locale.ENGLISH, "%d.%s", emoji.getId(), emoji.isAnimated() ? "gif" : "png");
                        String url = "https://cdn.discordapp.com/emojis/" + fileName;

                        var binding = (WidgetEmojiSheetBinding) getBinding.invoke(_this);
                        if (binding == null) return;
                        var root = (ViewGroup) binding.getRoot();
                        if (root == null) return;
                        var rootLayout = (LinearLayout) root.getChildAt(0);

                        if (rootLayout.findViewById(id) != null) return;

                        var ctx = rootLayout.getContext();
                        if (ctx == null) return;

                        int marginDpFour = Utils.dpToPx(4);
                        int marginDpEight = marginDpFour * 2;
                        int marginDpSixteen = marginDpEight * 2;

                        var copyLinkButton = new Button(ctx);
                        copyLinkButton.setText("Copy Link");
                        copyLinkButton.setOnClickListener(v -> {
                            var clip = ClipData.newPlainText("Copy emoji link", url);
                            clipboard.setPrimaryClip(clip);
                            Utils.showToast(ctx, "Copied to clipboard");
                        });

                        var saveButton = new Button(ctx);
                        Runnable initSaveButton = () -> {
                            if (new File(getEmojiFolder(), fileName).exists()) {
                                saveButton.setText("Emoji saved");
                                saveButton.setBackgroundColor(ctx.getColor(R$c.uikit_btn_bg_color_selector_red));
                            } else {
                                saveButton.setText("Save Emoji");
                            }
                        };
                        initSaveButton.run();
                        saveButton.setOnClickListener(v -> downloadEmoji(ctx, url, fileName, initSaveButton));

                        var cloneButton = new Button(ctx);
                        cloneButton.setText("Clone to other server");
                        cloneButton.setOnClickListener(v -> Utils.openPageWithProxy(ctx, new CloneModal(url, emoji.getName(), emoji.getId(), emoji.isAnimated())));

                        var buttonParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        buttonParams.setMargins(0, 0, 0, 0);
                        copyLinkButton.setLayoutParams(buttonParams);
                        saveButton.setLayoutParams(buttonParams);
                        cloneButton.setLayoutParams(buttonParams);

                        var pluginButtonLayout = new com.aliucord.widgets.LinearLayout(ctx);
                        pluginButtonLayout.setId(id);
                        pluginButtonLayout.addView(copyLinkButton);
                        pluginButtonLayout.addView(saveButton);
                        pluginButtonLayout.addView(cloneButton);

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

                        rootLayout.addView(pluginButtonLayout, idx);
                    } catch (IllegalAccessException | InvocationTargetException ignored) { }
                }
            });
        } catch (NoSuchMethodException ignored) { }
    }

    private File getEmojiFolder() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Emojis");
    }

    private void downloadEmoji(Context ctx, String url, String fileName, Runnable callback) {
        Utils.threadPool.execute(() -> {
            var downloadFolder = getEmojiFolder();
            if (!(downloadFolder.exists() || downloadFolder.mkdir())) {
                logger.info(ctx, "Failed to create output folder Downloads/Emojis :(");
                return;
            }

            var outFile = new File(downloadFolder, fileName);
            if (outFile.exists()) {
                logger.info(ctx, "Already downloaded that emoji!");
                return;
            }

            try {
                var req = new Http.Request(url).execute();
                try (var oStream = new FileOutputStream(outFile)) {
                    req.pipe(oStream);
                    logger.info(ctx, "Done!");
                    callback.run();
                }
            } catch (IOException ex) {
                logger.error(ctx, "Sorry, something went wrong while saving that emoji :(", ex);
            }
        });
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
        patcher.unpatchAll();
    }
}
