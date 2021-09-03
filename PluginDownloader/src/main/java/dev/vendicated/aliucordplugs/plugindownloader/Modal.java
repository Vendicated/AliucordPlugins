/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.plugindownloader;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.*;

public final class Modal extends SettingsPage {
    private static final Type resType = TypeToken.getParameterized(Map.class, String.class, Plugin.Info.class).getType();

    private final String author;
    private final String repo;
    private Map<String, Plugin.Info> plugins;
    private Throwable ex;

    public Modal(String author, String repo) {
        super();
        this.author = author;
        this.repo = repo;
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewBound(View view) {
        super.onViewBound(view);

        setActionBarTitle("Plugin downloader");
        setActionBarSubtitle(String.format("https://github.com/%s/%s", author, repo));

        var ctx = requireContext();

        if (ex != null) {
            var exView = new TextView(ctx, null, 0, R.h.UiKit_Settings_Item_SubText);
            var sw = new StringWriter();
            var pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            exView.setText("An error occurred:\n\n" + sw);
            exView.setTextIsSelectable(true);
            addView(exView);
        } else if (plugins == null) {
            Utils.threadPool.execute(() -> {
                try {
                    plugins = Http.simpleJsonGet(String.format("https://raw.githubusercontent.com/%s/%s/builds/updater.json", author, repo), resType);
                } catch (Throwable e) {
                    ex = e;
                }
                Utils.mainThread.post(() -> onViewBound(view));
            });
        } else {
            var list = new ArrayList<Plugin.CardInfo>();
            for (var plugin : plugins.entrySet()) {
                String name = plugin.getKey();
                if (name.equals("default")) continue;
                boolean exists = PDUtil.isPluginInstalled(name);
                String title = String.format("%s %s v%s", exists ? "Uninstall" : "Install", name, plugin.getValue().version);
                list.add(new Plugin.CardInfo(name, title, exists));
            }
            list.sort(Comparator.comparing(a -> a.title));
            for (var plugin: list) {
                var button = plugin.exists ? new DangerButton(ctx) : new Button(ctx);
                button.setText(plugin.title);
                if (!plugin.exists)
                    button.setOnClickListener(e -> PDUtil.downloadPlugin(ctx, author, repo, plugin.name, this::reRender));
                else
                    button.setOnClickListener(e -> PDUtil.deletePlugin(ctx, plugin.name, this::reRender));
                addView(button);
            }
        }
    }

}
