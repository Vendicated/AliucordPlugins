/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.emojiutility;

import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;

@AliucordPlugin
public class EmojiUtility extends Plugin {
    public static boolean useHumanNames;
    public static final Logger logger = new Logger("EmojiUtility");
    public static SettingsAPI mSettings;

    public EmojiUtility() {
        settingsTab = new SettingsTab(Settings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings, patcher, commands);
    }

    @Override
    public void start(Context ctx) throws Throwable {
        mSettings = settings;
        useHumanNames = settings.getBool("humanNames", true);
        Patches.init(settings, patcher);
        Commands.registerAll(commands);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
