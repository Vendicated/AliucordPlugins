/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.dps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.aliucord.*;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.views.Divider;
import com.discord.app.AppBottomSheet;
import com.discord.databinding.WidgetSettingsBinding;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.settings.WidgetSettings;
import com.lytefast.flexinput.R;

import java.util.*;

@AliucordPlugin
public class DedicatedPluginSettings extends Plugin {
    private static final Map<String, Integer> drawableIds = new HashMap<>() {{
        put("fallback", R.d.ic_slash_command_24dp);

        try {
            // Ven
            put("TapTap", R.d.ic_raised_hand_action_24dp);
            put("Themer", R.d.ic_theme_24dp);
            put("Hastebin", R.d.ic_link_white_24dp);
            put("ImageUploader", R.d.ic_uploads_image_dark);
            put("EmojiUtility", R.d.ic_emoji_24dp);

            // Juby
            put("UserDetails", R.d.ic_my_account_24dp);
            put("PronounDB", R.d.ic_accessibility_24dp);
            put("CustomTimestamps", R.d.ic_clock_black_24dp);
            put("CustomNicknameFormat", R.d.ic_account_circle_white_24dp);
            put("RemoveZoomLimit", R.d.ic_search_white_24dp);

            // Moth
            put("RotatedChat", com.yalantis.ucrop.R.c.ucrop_rotate); // This is from https://github.com/Yalantis/uCrop lmao

            // Xinto
            put("HideBloat", R.d.design_ic_visibility_off);
        } catch (Throwable th) { logger.error("Failed to retrieve some drawables", th); }
    }};

    private static final Logger logger = new Logger("DedicatedPluginSettings");
    private final int id = View.generateViewId();

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        final var drawables = new HashMap<String, Drawable>(){{
            for (var entry : drawableIds.entrySet()) {
                try {
                    final var drawable = Objects.requireNonNull(
                            ContextCompat.getDrawable(context, entry.getValue()),
                            "No such drawable: " + entry.getValue()
                    );
                    put(entry.getKey(), drawable);
                } catch (Throwable th) {
                    logger.error(String.format("Failed to retrieve drawable %s of plugin %s", entry.getValue(), entry.getKey()), th);
                }
            }
        }};

        final var getBinding = WidgetSettings.class.getDeclaredMethod("getBinding");
        getBinding.setAccessible(true);

        patcher.patch(WidgetSettings.class.getDeclaredMethod("configureUI", WidgetSettings.Model.class), new PinePatchFn(callFrame -> {
            var widgetSettings = (WidgetSettings) callFrame.thisObject;
            WidgetSettingsBinding binding;
            try {
                binding = (WidgetSettingsBinding) getBinding.invoke(widgetSettings);
                if (binding == null) return;
            } catch (Throwable th) { return; }

            var ctx = widgetSettings.requireContext();
            var layout = (LinearLayoutCompat) ((NestedScrollView) ((CoordinatorLayout) binding.getRoot()).getChildAt(1)).getChildAt(0);

            int baseIndex = layout.indexOfChild(layout.findViewById(Utils.getResId("developer_options_divider", "id")));

            if (PluginManager.plugins.size() == 0) return;

            var existingHeader = layout.findViewById(id);
            if (existingHeader == null) {
                var header = new TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header);
                header.setId(id);
                header.setText("Plugin Settings");
                header.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));

                layout.addView(new Divider(ctx), baseIndex);
                layout.addView(header, baseIndex + 1);
            } else baseIndex = layout.indexOfChild(existingHeader) - 1;

            var font = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium);

            int i = 2;
            var plugins = PluginManager.plugins.values();
            for (var p : PluginManager.plugins.values()) if (p.settingsTab != null && PluginManager.isPluginEnabled(p.getName())) {
                int hashcode = p.getName().hashCode();
                if (layout.findViewById(hashcode) == null) {
                    var view = new TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Icon);
                    view.setId(hashcode);
                    view.setText(p.getName());
                    view.setTypeface(font);

                    var icon = drawables.get(p.getName());
                    if (icon == null) {
                        try {
                            var iconField = p.getClass().getDeclaredField("pluginIcon");
                            iconField.setAccessible(true);
                            icon = (Drawable) iconField.get(p);
                        } catch (Throwable ignored) { }
                        if (icon == null) icon = Objects.requireNonNull(drawables.get("fallback"), "Fallback icon was somehow null");
                    }
                    icon = icon.mutate();
                    icon.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);

                    view.setOnClickListener(e -> handleEntryClicked(e.getContext(), p, widgetSettings.getParentFragmentManager()));
                    layout.addView(view, baseIndex + i++);
                }
            }
        }));
    }

    private void handleEntryClicked(Context ctx, Plugin p, FragmentManager manager) {
        try {
            if (p.settingsTab.type == Plugin.SettingsTab.Type.PAGE && p.settingsTab.page != null) {
                Fragment page = p.settingsTab.args != null
                        ? ReflectUtils.invokeConstructorWithArgs(p.settingsTab.page, p.settingsTab.args)
                        : p.settingsTab.page.newInstance();
                Utils.openPageWithProxy(ctx, page);
            } else if (p.settingsTab.type == Plugin.SettingsTab.Type.BOTTOM_SHEET && p.settingsTab.bottomSheet != null) {
                AppBottomSheet sheet = p.settingsTab.args != null
                        ? ReflectUtils.invokeConstructorWithArgs(p.settingsTab.bottomSheet, p.settingsTab.args)
                        : p.settingsTab.bottomSheet.newInstance();

                sheet.show(manager, p.getName() + "Settings");
            }
        } catch (Throwable th){
            PluginManager.logger.error(ctx, "Failed to launch plugin settings", th);
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
