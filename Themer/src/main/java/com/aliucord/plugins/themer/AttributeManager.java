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

import com.aliucord.Utils;
import com.aliucord.plugins.Themer;

import java.util.HashMap;
import java.util.Map;

public class AttributeManager {
    public static final String[] simpleAccentAttrs = new String[] {
            "color_brand",
            "colorControlBrandForeground",
            "colorControlActivated",
            "colorTextLink",
            "__alpha_10_theme_chat_mention_background",
            "theme_chat_mention_foreground"
    };
    public static final String[] simpleBackgroundAttrs = new String[] {
            "colorSurface",
            "colorBackgroundFloating",
            "colorTabsBackground",
            "theme_chat_spoiler_inapp_bg",
            "primary_600",
            "primary_660",
            "primary_800"
    };
    public static final String[] simpleBackgroundSecondaryAttrs = new String[] {
            "colorBackgroundTertiary",
            "colorBackgroundSecondary",
            "primary_700",
            "theme_chat_spoiler_bg"
    };

    public static final Map<String, String[]> mappings = new HashMap<>() {{
            put("color_brand_new", new String[] { "color_brand" });
            put("color_brand_360", new String[] { "colorControlBrandForeground" });
            put("color_primary_dark_800", new String[] { "colorSurface", "colorBackgroundFloating", "colorTabsBackground" });
            put("color_brand_500", new String[] { "colorControlActivated" });
            put("color_primary_660", new String[] { "primary_660" });
            put("color_primary_600", new String[] { "primary_600", "theme_chat_spoiler_inapp_bg" });
            put("color_primary_700", new String[] { "primary_700", "colorBackgroundTertiary", "colorBackgroundSecondary", "theme_chat_spoiler_bg" });
            put("color_primary_800", new String[] { "primary_800" });
            put("color_primary_900", new String[] { "primary_900" });
            put("color_link", new String[] { "colorTextLink" });
            put("color_brand_500_alpha_20", new String[] { "theme_chat_mention_background" });
            put("color_brand_new_530", new String[] { "theme_chat_mention_foreground" });
            put("mention_highlight", new String[] { "theme_chat_mentioned_me" });
    }};

    public static Map<Integer, Integer> activeTheme;

    public static void loadAll(String[] attrs, int color) {
        for (var attr : attrs) {
            if (attr.startsWith("__alpha_10_")) {
                final int len = "__alpha_10_".length();
                setAttr(attr.substring(len), ThemeManager.getColorWithAlpha("1a", color));
            } else
                setAttr(attr, color);
        }
    }

    public static void loadAttr(String name, int color) {
        var names = mappings.get(name);
        if (names == null) throw new RuntimeException(String.format("Attr %s somehow has no mapping.", name));
        for (var attr : names)
            setAttr(attr, color);
    }
    public static void setAttr(String attr, int color) {
        int id = Utils.getResId(attr, "attr");
        if (id == 0)
            Themer.logger.warn("No such attribute: " + attr);
        else
            activeTheme.put(id, color);
    }

    public static Integer getAttr(int attr) {
        if (activeTheme == null) return null;
        return activeTheme.get(attr);
    }
}
