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

import androidx.annotation.NonNull;

import com.aliucord.Logger;
import com.aliucord.entities.Plugin;
import com.aliucord.plugins.emojiutil.*;

@SuppressWarnings("unused")
public class EmojiUtility extends Plugin {
    public static final Logger logger = new Logger("EmojiUtility");

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Adds lots of utility for emojis";
        manifest.version = "1.0.1";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    public EmojiUtility() {
        settingsTab = new SettingsTab(Settings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings, patcher, commands);
    }

    @Override
    public void start(Context ctx) throws Throwable {
        Patches.init(ctx, settings, patcher);
        Commands.registerAll(commands);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
