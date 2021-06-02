/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.hastebin;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Button;
import com.aliucord.views.TextInput;
import com.lytefast.flexinput.R$c;

import java.util.regex.Pattern;


@SuppressLint("SetTextI18n")
public final class PluginSettings extends SettingsPage {
    private static final String plugin = "Hastebin";
    private static final Pattern re = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}");

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setActionBarTitle(plugin);
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);

        var plug = PluginManager.plugins.get(plugin);
        assert(plug != null);
        var settings = plug.sets;

        var ctx = view.getContext();
        var res = ctx.getResources();
        var layout = (LinearLayout) ((NestedScrollView) ((CoordinatorLayout) view).getChildAt(1)).getChildAt(0);
        int p = Utils.getDefaultPadding();
        layout.setPadding(p, p, p, p);

        var input = new TextInput(ctx);
        input.setHint("Hastebin Mirror");

        var editText = input.getEditText();
        assert(editText != null);

        var button = new Button(ctx, false);
        button.setText("Save");
        button.setOnClickListener(v -> {
            String text = editText.getText().toString();
            while (text.endsWith("/")) text = text.substring(0, text.length() -1);
            if (isValid(text)) settings.setString("mirror", text);
        });

        editText.setMaxLines(1);
        editText.setText(settings.getString("mirror", "https://haste.powercord.dev"));
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable s) {
                if (!isValid(s.toString())) button.setBackgroundColor(res.getColor(R$c.uikit_btn_bg_color_selector_red));
                else button.setBackgroundColor(res.getColor(R$c.brand));
            }
        });

        layout.addView(input);
        layout.addView(button);
    }

    private boolean isValid(String s) {
        return re.matcher(s).matches();
    }
}