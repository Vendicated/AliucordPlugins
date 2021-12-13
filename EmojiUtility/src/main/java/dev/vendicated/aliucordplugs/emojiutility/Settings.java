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
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.api.*;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.TextInput;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

import java.io.File;

import kotlin.jvm.functions.Function1;

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
        addInput(ctx, "Maximum Download Thread Count", "threadCount", "10", true, s -> {
            try {
                int i = Integer.parseInt(s);
                return i > 0 && i <= 100;
            } catch (NumberFormatException ignored) {}
            return false;
        });
        addInput(ctx, "Download Dir", "downloadDir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Emojis", false, s -> {
            var file = new File(s);
            return file.isDirectory() && file.canWrite();
        });
        addCheckedSetting(ctx, "Human readable file names", "Name downloads NAME.png instead of ID.png. Downloading twice will lead to duplicate downloads", "humanNames");
        addCheckedSetting(ctx, "Add extra buttons to emoji widget", "copy url, save & clone", "extraButtons");
        addCheckedSetting(ctx, "Keep reaction emoji picker open", "long press the react button","keepOpen");
        addCheckedSetting(ctx, "Hide unusable emojis", null, "hideUnusable");
        addCheckedSetting(ctx, "Register download commands", null, "registerCommands");
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        if (settings.getBool("registerCommands", true))
            Commands.registerAll(commands);
        else
            commands.unregisterAll();

        EmojiUtility.useHumanNames = settings.getBool("humanNames", true);
        try {
            Patches.init(settings, patcher);
        } catch (Throwable th) {
            EmojiUtility.logger.error("Something went wrong while initialising the patches :(", th);
        }
    }

    private void addCheckedSetting(Context ctx, String title, String subtitle, String setting) {
        var cs = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, title, subtitle);
        cs.setChecked(settings.getBool(setting, true));
        cs.setOnCheckedListener(checked -> settings.setBool(setting, checked));
        addView(cs);
    }

    private void addInput(Context ctx, String title, String setting, String def, boolean isInt, Function1<String, Boolean> validate) {
        var input = new TextInput(ctx);
        var params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(DimenUtils.getDefaultPadding(), DimenUtils.getDefaultPadding() / 2, DimenUtils.getDefaultPadding(), DimenUtils.getDefaultPadding() / 2);
        input.setLayoutParams(params);
        input.setHint(title);
        var editText = input.getEditText();
        editText.setText(isInt ? Integer.toString(settings.getInt(setting, Integer.parseInt(def))) : settings.getString(setting, def));
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable e) {
                var s = e.toString();
                if (!validate.invoke(s)) input.setHint(title + " [INVALID]");
                else {
                    if (isInt) {
                        settings.setInt(setting, Integer.parseInt(s));
                    } else {
                        settings.setString(setting, s);
                    }
                    input.setHint(title);
                }
            }
        });
        addView(input);
    }
}
