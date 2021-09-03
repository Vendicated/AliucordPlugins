/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.taptap;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;

import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.lytefast.flexinput.R;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.TextInput;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.view.text.TextWatcher;
import com.discord.views.CheckedSetting;
import com.google.android.material.card.MaterialCardView;

public class TapTapSettings extends SettingsPage {
    private final TapTap plugin;
    public TapTapSettings(TapTap plugin) {
        this.plugin = plugin;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        setActionBarTitle("TapTap");

        var ctx = requireContext();

        var input = new TextInput(ctx);
        input.setHint("Double Tap Window (in ms)");

        var editText = input.getEditText();
        assert editText != null;

        editText.setMaxLines(1);
        editText.setText(String.valueOf(plugin.settings.getInt("doubleTapWindow", TapTap.defaultDelay)));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable s) {
                var str = s.toString();
                int i;
                try { i = Integer.parseInt(str);
                } catch (NumberFormatException ignored) { i = TapTap.defaultDelay; }
                plugin.settings.setInt("doubleTapWindow", i);
            }
        });

        var card = new MaterialCardView(ctx);
        card.setRadius(DimenUtils.getDefaultCardRadius());
        card.setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary));
        var params = new MaterialCardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, DimenUtils.getDefaultPadding(), 0, 0);
        card.setLayoutParams(params);


        var checkbox = Utils.createCheckedSetting(
                ctx,
                CheckedSetting.ViewType.SWITCH,
                "Hide Buttons",
                "Hides reply & edit buttons in the message actions menu. The reply button will still be shown on your own messages."
        );
        checkbox.setChecked(plugin.settings.getBool("hideButtons", false));
        checkbox.setOnCheckedListener(checked -> {
            plugin.settings.setBool("hideButtons", checked);
            plugin.togglePatch.run();
        });

        card.addView(checkbox);

        addView(input);
        addView(card);
    }
}
