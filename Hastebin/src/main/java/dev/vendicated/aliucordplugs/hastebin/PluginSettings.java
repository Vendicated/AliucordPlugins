/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.hastebin;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Button;
import com.aliucord.views.TextInput;

import java.util.regex.Pattern;


@SuppressLint("SetTextI18n")
public final class PluginSettings extends SettingsPage {
    private static final Pattern re = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}");

    private final SettingsAPI settings;

    public PluginSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);

        setActionBarTitle("Hastebin");

        var ctx = requireContext();

        var input = new TextInput(ctx);
        input.setHint("Hastebin Mirror");

        var editText = input.getEditText();
        assert editText != null;

        var button = new Button(ctx);
        button.setText("Save");
        button.setOnClickListener(v -> {
            settings.setString("mirror", editText.getText().toString().replaceFirst("/+$", ""));
            Utils.showToast(ctx, "Saved!");
            close();
        });

        editText.setMaxLines(1);
        editText.setText(settings.getString("mirror", "https://haste.powercord.dev"));
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable s) {
                if (!isValid(s.toString())) {
                    button.setAlpha(0.5f);
                    button.setClickable(false);
                } else {
                    button.setAlpha(1f);
                    button.setClickable(true);
                }
            }
        });

        addView(input);
        addView(button);
    }

    private boolean isValid(String s) {
        return re.matcher(s).matches();
    }
}
