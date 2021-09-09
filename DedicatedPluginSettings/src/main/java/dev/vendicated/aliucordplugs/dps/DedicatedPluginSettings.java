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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.*;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.views.Divider;
import com.discord.widgets.settings.WidgetSettings;
import com.lytefast.flexinput.R;

import java.util.Comparator;

@AliucordPlugin
public class DedicatedPluginSettings extends Plugin {
    private PluginsAdapter adapter;
    private TextView header;
    private View divider;
    private RecyclerView recycler;

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        if (recycler == null) {
            final var getBinding = WidgetSettings.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);

            patcher.patch(WidgetSettings.class.getDeclaredMethod("onViewBound", View.class), new PinePatchFn(callFrame -> {
                Utils.mainThread.postDelayed(() -> {
                    var widgetSettings = (WidgetSettings) callFrame.thisObject;

                    var layout = (ViewGroup) ((ViewGroup) ((ViewGroup) callFrame.args[0]).getChildAt(1)).getChildAt(0);
                    var ctx = layout.getContext();

                    int idx = layout.indexOfChild(layout.findViewById(Utils.getResId("developer_options_divider", "id")));

                    header = new TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header);
                    header.setText("Plugin Settings");
                    header.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));

                    layout.addView((divider = new Divider(ctx)), idx);
                    layout.addView(header, ++idx);

                    recycler = new RecyclerView(ctx);
                    recycler.setAdapter((adapter = new PluginsAdapter(widgetSettings.getParentFragmentManager())));
                    recycler.setLayoutManager(new LinearLayoutManager(ctx));
                    layout.addView(recycler, ++idx);
                }, 2000);
            }));
        } else {
            header.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.VISIBLE);
        }

        patcher.patch(PluginManager.class.getDeclaredMethod("startPlugin", String.class), new PinePatchFn(callFrame -> {
            var name = (String) callFrame.args[0];
            Plugin p;
            if (adapter != null && (p = PluginManager.plugins.get(name)) != null && p.settingsTab != null) {
                var data = adapter.getData();
                if (data.contains(p)) return;
                data.add(p);
                data.sort(Comparator.comparing(Plugin::getName));
                adapter.notifyItemInserted(data.indexOf(p));
            }
        }));

        patcher.patch(PluginManager.class.getDeclaredMethod("stopPlugin", String.class), new PinePatchFn(callFrame -> {
            var name = (String) callFrame.args[0];
            Plugin p;
            if (adapter != null && (p = PluginManager.plugins.get(name)) != null) {
                var data = adapter.getData();
                var idx = data.indexOf(p);
                if (idx != -1) {
                    data.remove(idx);
                    adapter.notifyItemRemoved(idx);
                }
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        if (recycler != null) {
            header.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            recycler.setVisibility(View.GONE);
        }
    }
}
