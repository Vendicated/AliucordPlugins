/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.themer;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.*;
import com.aliucord.fragments.InputDialog;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.Themer;
import com.aliucord.views.Button;
import com.aliucord.views.Divider;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;
import com.lytefast.flexinput.R$h;

import java.io.File;
import java.io.FileOutputStream;

public class ThemerSettings extends SettingsPage {
    private ActivityResultLauncher<Intent> launcher;

    public void importTheme(Uri uri, String name) throws Throwable {
        if (!name.endsWith(".json")) name += ".json";
        try (var is = requireActivity().getContentResolver().openInputStream(uri);
             var os = new FileOutputStream(new File(Constants.BASE_PATH, "themes/" + name))
        ) {
            int n;
            byte[] buf = new byte[16384]; // 16 KB
            while ((n = is.read(buf)) > -1) {
                os.write(buf, 0, n);
            }
            os.flush();
        }
        var ctx = requireContext();
        Utils.showToast(ctx, "Successfully imported theme " + name);
        ThemeManager.themes.clear();
        ThemeManager.loadThemes(ctx, false, false);
        reRender();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);

        var ctx = requireContext();
        var act = requireActivity();
        if (launcher == null)
            launcher = act.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == Activity.RESULT_OK) {
                        var data = res.getData();
                        if (data == null) return;
                        var contentUri = data.getData();
                        try {
                            if (contentUri.toString().endsWith(".json")) importTheme(contentUri, new File(contentUri.getPath()).getName());
                            else {
                                var dialog = new InputDialog()
                                        .setPlaceholderText("Filename")
                                        .setTitle("Filename")
                                        .setDescription("Please specify a name for this theme");
                                dialog.setOnOkListener(e -> {
                                    var text = dialog.getInput();
                                    if (text.length() > 0) {
                                        try {
                                            importTheme(contentUri, text);
                                        } catch (Throwable ex) {
                                            Themer.logger.error(requireContext(), "Failed to import theme.", ex);
                                        }
                                        dialog.dismiss();
                                    }
                                });
                                dialog.show(getParentFragmentManager(), "Themer");
                            }
                        } catch (Throwable ex) {
                            Themer.logger.error(requireContext(), "Failed to import theme.", ex);
                        }
                    }
                }
        );

        setActionBarTitle("Themer");

        var importBtn = new Button(ctx);
        importBtn.setText("Import theme");
        importBtn.setOnClickListener(e -> {
            var type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("json");
            var intent = new Intent(Intent.ACTION_GET_CONTENT).setType(type);
            intent = Intent.createChooser(intent, "Choose a json file");
            launcher.launch(intent);
        });

        var refreshBtn = new Button(ctx);
        refreshBtn.setText("Load missing themes");
        refreshBtn.setOnClickListener(e -> {
            ThemeManager.themes.clear();
            ThemeManager.loadThemes(e.getContext(), false, false);
            reRender();
        });

        addView(importBtn);
        addView(refreshBtn);
        addView(new Divider(ctx));

        var header = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_Header);
        header.setText("Active Theme");
        header.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));

        addView(header);

        if (ThemeManager.themes.size() == 0) {
            var text = new TextView(ctx, null, 0, R$h.UiKit_TextView);
            text.setText("Hmm... No themes found.");
            addView(text);
        } else {
            var items = CollectionUtils.map(ThemeManager.themes, t -> Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, t.name, null));
            var noTheme = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "None", null);

            items.add(0, noTheme);

            var manager = new RadioManager(items);
            manager.a(noTheme);
            noTheme.e(e -> {
                ThemeManager.activeTheme = null;
                int idx = manager.b();
                if (idx != 0) {
                    ThemeManager.themes.get(idx - 1).disable();
                    manager.a(noTheme);
                    Utils.appActivity.recreate();
                    requireActivity().onBackPressed();
                    Utils.openPageWithProxy(ctx, new ThemerSettings());
                }
            });

            addView(noTheme);

            for (int i = 1; i < items.size(); i++) {
                var theme = ThemeManager.themes.get(i - 1);
                var item = items.get(i);
                final int idx = i;
                item.e(e -> {
                    if (manager.b() != idx) {
                        if (theme.load(ctx)) {
                            theme.enable();
                            manager.a(item);
                            requireActivity().onBackPressed();
                            Utils.openPageWithProxy(ctx, new ThemerSettings());
                        } else {
                            Utils.showToast(ctx, "Something went wrong while loading that theme :(");
                        }
                    }
                });
                addView(item);
                if (theme.isEnabled()) manager.a(item);
            }
        }
    }
}
