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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.*;

import com.aliucord.*;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Divider;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.settings.WidgetSettings;
import com.lytefast.flexinput.R;

@AliucordPlugin
public class DedicatedPluginSettings extends Plugin {
    private PluginsAdapter adapter;
    private TextView header;
    private View divider;
    private TextView customize;
    private TextView reset;
    private RecyclerView recycler;

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        if (recycler == null) {
            final var getBinding = WidgetSettings.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);

            patcher.patch(WidgetSettings.class.getDeclaredMethod("onViewBound", View.class), new Hook(param -> {
                widgetSettings = (WidgetSettings) param.thisObject;
                Utils.mainThread.postDelayed(() -> {
                    var layout = (ViewGroup) ((ViewGroup) ((ViewGroup) param.args[0]).getChildAt(1)).getChildAt(0);
                    var ctx = layout.getContext();

                    var devDivider = layout.findViewById(Utils.getResId("developer_options_divider", "id"));
                    int idx = layout.indexOfChild(devDivider);

                    var openDrawable = ContextCompat.getDrawable(ctx, R.e.ic_arrow_down_14dp).mutate();
                    openDrawable.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
                    var closedDrawable = new LayerDrawable(new Drawable[] { openDrawable }) {
                        @Override public void draw(Canvas canvas) {
                            var bounds = openDrawable.getBounds();
                            canvas.save();
                            canvas.rotate(270, bounds.width() / 2f, bounds.height() / 2f);
                            super.draw(canvas);
                            canvas.restore();
                        }
                    };

                    header = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header);
                    header.setText("Plugin Settings");
                    header.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
                    header.setCompoundDrawablePadding(DimenUtils.dpToPx(4));
                    header.setCompoundDrawablesRelativeWithIntrinsicBounds(openDrawable, null, null, null);
                    header.setOnClickListener(view -> {
                        TransitionManager.beginDelayedTransition(layout);
                        if (recycler.getVisibility() == View.VISIBLE) {
                            recycler.setVisibility(View.GONE);
                            customize.setVisibility(View.GONE);
                            reset.setVisibility(View.GONE);
                            header.setCompoundDrawablesRelativeWithIntrinsicBounds(closedDrawable, null, null, null);
                        } else {
                            recycler.setVisibility(View.VISIBLE);
                            customize.setVisibility(View.VISIBLE);
                            if (adapter.isEditing()) reset.setVisibility(View.VISIBLE);
                            header.setCompoundDrawablesRelativeWithIntrinsicBounds(openDrawable, null, null, null);
                        }
                    });

                    customize = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon);
                    customize.setText("Customize");
                    customize.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium));
                    var editDrawable = ContextCompat.getDrawable(ctx, R.e.ic_edit_24dp).mutate();
                    editDrawable.setTint(ctx.getColor(R.c.brand));
                    customize.setCompoundDrawablesRelativeWithIntrinsicBounds(editDrawable, null, null, null);
                    customize.setTextColor(ctx.getColor(R.c.brand));
                    customize.setOnClickListener(v -> {
                        if (customize.getText() == "Customize") {
                            customize.setText("Save");
                            customize.setTextColor(ctx.getColor(R.c.uikit_btn_bg_color_selector_green));
                            editDrawable.setTint(ctx.getColor(R.c.uikit_btn_bg_color_selector_green));
                            reset.setVisibility(View.VISIBLE);
                        } else {
                            customize.setText("Customize");
                            customize.setTextColor(ctx.getColor(R.c.brand));
                            editDrawable.setTint(ctx.getColor(R.c.brand));
                            reset.setVisibility(View.GONE);
                        }
                        adapter.toggleCustomize();
                    });

                    reset = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon);
                    reset.setVisibility(View.GONE);
                    reset.setText("Reset Settings");
                    reset.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium));
                    reset.setTextColor(ctx.getColor(R.c.uikit_btn_bg_color_selector_red));
                    var resetDrawable = ContextCompat.getDrawable(ctx, R.e.ic_delete_24dp).mutate();
                    resetDrawable.setTint(ctx.getColor(R.c.uikit_btn_bg_color_selector_red));
                    reset.setCompoundDrawablesRelativeWithIntrinsicBounds(resetDrawable, null, null, null);
                    reset.setOnClickListener(v -> {
                        var dialog = new ConfirmDialog();
                        dialog
                            .setTitle("Reset Settings")
                            .setDescription("This will reset your sort order and hidden plugins. Are you sure?")
                            .setOnOkListener(_v -> {
                                adapter.reset();
                                dialog.dismiss();
                            })
                            .show(widgetSettings.getParentFragmentManager(), "Reset Settings");
                    });

                    layout.addView((divider = new Divider(ctx)), idx);
                    layout.addView(header, ++idx);
                    layout.addView(customize, ++idx);
                    layout.addView(reset, ++idx);

                    recycler = new RecyclerView(ctx);

                    var touchCallback = new DragAndDropHelper();
                    new ItemTouchHelper(touchCallback).attachToRecyclerView(recycler);

                    recycler.setAdapter((adapter = new PluginsAdapter(touchCallback)));

                    recycler.setLayoutManager(new LinearLayoutManager(ctx));

                    layout.addView(recycler, ++idx);
                }, 2000);
            }));
        } else {
            header.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.VISIBLE);
            customize.setVisibility(View.VISIBLE);
            reset.setVisibility(View.GONE);
        }

        patcher.patch(PluginManager.class.getDeclaredMethod("startPlugin", String.class), new Hook(param -> {
            var name = (String) param.args[0];
            Plugin p;
            if (adapter != null && (p = PluginManager.plugins.get(name)) != null && p.settingsTab != null) {
                adapter.addPlugin(p);
            }
        }));

        patcher.patch(PluginManager.class.getDeclaredMethod("stopPlugin", String.class), new Hook(param -> {
            var name = (String) param.args[0];
            Plugin p;
            if (adapter != null && (p = PluginManager.plugins.get(name)) != null) {
                adapter.removePlugin(p);
            }
        }));
    }

    static WidgetSettings widgetSettings;

    @Override
    public void stop(Context context) {
        widgetSettings = null;
        patcher.unpatchAll();
        if (recycler != null) {
            header.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            recycler.setVisibility(View.GONE);
            customize.setVisibility(View.GONE);
            reset.setVisibility(View.GONE);
        }
    }
}
