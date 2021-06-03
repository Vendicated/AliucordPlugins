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
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Main;
import com.aliucord.entities.Plugin;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.lytefast.flexinput.R$b;
import com.lytefast.flexinput.R$h;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@SuppressLint("SetTextI18n")
public class PluginDownloader extends Plugin {
    private final Pattern pluginTitle = Pattern.compile("\\*{2}(\\w+)\\*{2}");
    private final Pattern repoPattern = Pattern.compile("https?://github.com/([A-Za-z0-9\\-_.]+)/([A-Za-z0-9\\-_.]+)");
    private final Pattern zipPattern = Pattern.compile("https?://github.com/([A-Za-z0-9\\-_.]+)/([A-Za-z0-9\\-_.]+)/(raw|blob)/\\w+/(\\w+).zip");

    public PluginDownloader() {
        super();
        needsResources = true;
    }

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Adds message context menu items to quick download plugins";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private static final String className = "com.discord.widgets.chat.list.actions.WidgetChatListActions";
    public static Map<String, List<String>> getClassesToPatch() {
        var map = new HashMap<String, List<String>>();
        map.put(className, Arrays.asList("configureUI", "onViewCreated"));
        return map;
    }

    @Override
    public void start(Context context) {
        var icon = ResourcesCompat.getDrawable(
                resources,
                resources.getIdentifier(
                        "ic_download_plugin",
                        "drawable",
                        "com.aliucord.plugins"),
                null);
        assert icon != null;
        icon.setTint(ColorCompat.getThemedColor(context, R$b.colorInteractiveNormal));
        AtomicReference<LinearLayout> layoutRef = new AtomicReference<>();

        try {
            Method getBinding = WidgetChatListActions.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);

            patcher.patch(className, "configureUI", (_this, args, ret) -> {
                try {
                    var layout = layoutRef.get();
                    if (layout == null) return ret;
                    var ctx = layout.getContext();
                    var msg = ((WidgetChatListActions.Model) args.get(0)).getMessage();
                    if (msg == null) return ret;
                    String content = msg.getContent();
                    long channelId = msg.getChannelId();

                    if (channelId == 845784407846813696L /* Plugin-Links-Updates */) {
                        var matcher = zipPattern.matcher(content);
                        while (matcher.find()) {
                            String author = matcher.group(1);
                            String repo = matcher.group(2);
                            String name = matcher.group(4);
                            var view = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_Icon);
                            view.setText("Download " + name);
                            view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                            view.setOnClickListener(e -> downloadPlugin(ctx, author, repo, name));
                            layout.addView(view);
                        }
                    } else if (channelId == 811275162715553823L /* Plugin-Links */) {
                        var repoMatcher = repoPattern.matcher(content);
                        if (!repoMatcher.find()) return ret; // zzzzzzz don't post junk
                        String author = repoMatcher.group(1);
                        String repo = repoMatcher.group(2);

                        var pluginMatcher = pluginTitle.matcher(content);
                        while (pluginMatcher.find()) {
                            String name = pluginMatcher.group(1);
                            var view = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_Icon);
                            view.setText("Download " + name);
                            view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                            view.setOnClickListener(e -> downloadPlugin(ctx, author, repo, name));
                            layout.addView(view);
                        }
                    }
                    return ret;
                } catch (Throwable ignored) {}
                return ret;
            });
        } catch (Throwable e) { Main.logger.error(e); }

        patcher.patch(className, "onViewCreated", (_this, args, ret) -> {
            layoutRef.set((LinearLayout) ((NestedScrollView) args.get(0)).getChildAt(0));
            return ret;
        });
    }

    private void downloadPlugin(Context ctx, String author, String repo, String name) {
        new Thread(() -> {
            var url = String.format("https://github.com/%s/%s/raw/builds/%s.zip", author, repo, name);
            var fileName = String.format("%s/plugins/%s.zip", Constants.BASE_PATH, name);

            var file = new File(fileName);
            if (file.exists()) {
                showToast(ctx, String.format("Plugin %s already installed", name));
                return;
            }
            try {
                var res = new Http.Request(url).execute();
                try (var out = new FileOutputStream(file)) {
                    res.pipe(out);
                    showToast(ctx, String.format("Plugin %s successfully downloaded", name));
                }
            } catch (IOException ex) {
                showToast(ctx, String.format("Something went wrong while downloading plugin %s, sorry: %s", name, ex.getMessage()));
            };
        }).start();
    }

    private void showToast(Context ctx, String text) {
        var handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
