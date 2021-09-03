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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.api.*;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

public class Settings extends BottomSheet {
    private final SettingsAPI settings;
    private final PatcherAPI patcher;
    private final CommandsAPI commands;

    public Settings(SettingsAPI settings, PatcherAPI patcher, CommandsAPI commands) {
        this.settings = settings;
        this.patcher = patcher;
        this.commands = commands;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        var ctx = requireContext();
        addView(createCheckedSetting(ctx, "Add extra buttons to emoji widget", "copy url, save & clone", "extraButtons"));
        addView(createCheckedSetting(ctx, "Keep reaction emoji picker open", "long press the react button","keepOpen"));
        addView(createCheckedSetting(ctx, "Hide unusable emojis", null, "hideUnusable"));
        addView(createCheckedSetting(ctx, "Register download commands", null, "registerCommands"));
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        if (settings.getBool("registerCommands", true))
            Commands.registerAll(commands);
        else
            commands.unregisterAll();

        try {
            Patches.init(requireContext(), settings, patcher);
        } catch (Throwable th) {
            EmojiUtility.logger.error(requireContext(), "Something went wrong while initialising the patches :(", th);
        }
    }

    private CheckedSetting createCheckedSetting(Context ctx, String title, String subtitle, String setting) {
        var cs = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, title, subtitle);
        cs.setChecked(settings.getBool(setting, true));
        cs.setOnCheckedListener(checked -> settings.setBool(setting, checked));
        return cs;
    }
}
