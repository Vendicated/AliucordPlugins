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
import android.os.Environment;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.*;
import com.aliucord.fragments.InputDialog;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.Themer;
import com.aliucord.views.*;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;
import com.lytefast.flexinput.R;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import kotlin.jvm.functions.Function1;

public class ThemerSettings extends SettingsPage {
    private static final String THEME = "theme";
    private static final String FONT = "font";

    private ActivityResultLauncher<Intent> launcher;
    private String intentType;

    private File importComponent(Uri uri, String name, String ext) throws Throwable {
        if (!name.endsWith(ext)) name += ext;
        var file = new File(Constants.BASE_PATH, "themes/" + name);
        try (var is = requireActivity().getContentResolver().openInputStream(uri);
             var os = new FileOutputStream(file)
        ) {
            int n;
            byte[] buf = new byte[16384]; // 16 KB
            while ((n = is.read(buf)) > -1) {
                os.write(buf, 0, n);
            }
            os.flush();
        }
        return file;
    }

    private void handleImport(String ext, String kind) {
        var type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        var intent = new Intent(Intent.ACTION_GET_CONTENT).setType(type);
        intent = Intent.createChooser(intent, "Choose a file");
        intentType = kind;
        launcher.launch(intent);
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
                    if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null) {
                        var data = res.getData();
                        var contentUri = data.getData();

                        var isFont = intentType.equals(FONT);
                        var ext = isFont ? ".ttf" : ".json";

                        Function1<String, Object> doImport = name -> {
                            try {
                                var file = importComponent(contentUri, name, ext);
                                Utils.showToast(ctx, "Successfully imported " + intentType);
                                if (!isFont)         {
                                    ThemeManager.themes.clear();
                                    ThemeManager.loadThemes(ctx, false, false);
                                } else {
                                    ThemeManager.settings.setString("font", file.getAbsolutePath());
                                    ThemeManager.loadFont(-1, file, true);
                                }
                                reRender();
                            } catch (Throwable th) {
                                Themer.logger.error(ctx, "Failed to import " + intentType, th);
                            }
                            return null;
                        };

                        if (contentUri.toString().endsWith(ext)) doImport.invoke(new File(contentUri.getPath()).getName());
                        else {
                            try {
                                var json = new JSONObject(new JSONTokener(new String(Utils.readBytes(act.getContentResolver().openInputStream(contentUri)))));
                                if (json.has("name")) {
                                    doImport.invoke(json.getString("name"));
                                    return;
                                }
                            } catch (Throwable ignored) { }

                            var dialog = new InputDialog()
                                    .setPlaceholderText("Filename")
                                    .setTitle("Filename")
                                    .setDescription("Please specify a name for this " + (isFont ? "Font" : "Theme"));
                            dialog.setOnOkListener(e -> {
                                var text = dialog.getInput();
                                if (text.length() > 0) {
                                    doImport.invoke(text);
                                    dialog.dismiss();
                                }
                            });
                            dialog.show(getParentFragmentManager(), "Themer");
                        }
                    }
                }
        );

        setActionBarTitle("Themer");

        var exportBtn = new ToolbarButton(ctx);
        exportBtn.setOnClickListener(v -> {
            var f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "nameToColor.json");
            try (var fos = new FileOutputStream(f)) {
                var json = new JSONObject();
                var res = v.getResources();
                var theme = v.getContext().getTheme();
                for (var field : R.c.class.getDeclaredFields()) {
                    String colorName = field.getName();
                    int colorId = field.getInt(null);
                    int color = res.getColor(colorId, theme);
                    if (color == 0) continue;
                    json.put(colorName, color);
                }
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                Utils.showToast(v.getContext(), "Successfully created nameToColor.json");
            } catch (Throwable ignored) { }
        });
        exportBtn.setImageDrawable(ContextCompat.getDrawable(ctx, R.d.ic_theme_24dp));
        exportBtn.getDrawable().mutate().setAlpha(0);
        addHeaderButton(exportBtn);

        var fontBtn = new Button(ctx);
        fontBtn.setText("Choose font");
        fontBtn.setOnClickListener(e -> handleImport("ttf", FONT));

        var importBtn = new Button(ctx);
        importBtn.setText("Import theme");
        importBtn.setOnClickListener(e -> handleImport("json", THEME));

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

        var header = new TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header);
        header.setText("Active Theme");
        header.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));

        addView(header);

        if (ThemeManager.themes.size() == 0) {
            var text = new TextView(ctx, null, 0, R.h.UiKit_TextView);
            text.setText("Hmm... No themes found.");
            addView(text);
        } else {
            var transparencySwitch = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, "Transparency", "Enables transparency for themes with custom background. Requires restart");
            transparencySwitch.setChecked(ThemeManager.settings.getBool("enableTransparency", false));
            transparencySwitch.setOnCheckedListener(c -> ThemeManager.settings.setBool("enableTransparency", c));
            addView(transparencySwitch);

            var items = CollectionUtils.map(ThemeManager.themes, t -> Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, String.format("%s v%s by %s", t.name, t.version, t.author), null));
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
                        if (theme.load()) {
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
