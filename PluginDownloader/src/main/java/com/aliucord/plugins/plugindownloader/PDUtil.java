/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.plugindownloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.PluginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class PDUtil {
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static File openPluginFile(String plugin) {
        return new File(String.format("%s/plugins/%s.zip", Constants.BASE_PATH, plugin));
    }
    public static void downloadPlugin(Context ctx, String author, String repo, String name, Runnable callback) {
        new Thread(() -> {
            var url = String.format("https://github.com/%s/%s/raw/builds/%s.zip", author, repo, name);
            var file = openPluginFile(name);
            if (file.exists()) {
                showToast(ctx, String.format("Plugin %s already installed", name));
                return;
            }
            try {
                var res = new Http.Request(url).execute();
                try (var out = new FileOutputStream(file)) {
                    res.pipe(out);
                    PluginManager.loadPlugin(ctx, file);
                    showToast(ctx, String.format("Plugin %s successfully downloaded", name));
                    handler.post(callback);
                }
            } catch (IOException ex) {
                showToast(ctx, String.format("Something went wrong while downloading plugin %s, sorry: %s", name, ex.getMessage()));
            };
        }).start();
    }

    public static void showToast(Context ctx, String text) {
        handler.post(() -> Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show());
    }

    public static boolean isPluginInstalled(String plugin) {
        return openPluginFile(plugin).exists();
    }

    public static void deletePlugin(Context ctx, String plugin, Runnable callback) {
        boolean success = openPluginFile(plugin).delete();
        showToast(ctx, String.format("%s uninstalled plugin %s", success ? "Successfully" : "Failed to" ,plugin));
        if (success) handler.post(callback);
    }
}
