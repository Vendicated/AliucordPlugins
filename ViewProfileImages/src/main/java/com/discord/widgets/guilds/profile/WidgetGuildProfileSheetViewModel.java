/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.guilds.profile;

public class WidgetGuildProfileSheetViewModel {
    public static abstract class ViewState {
        public static final class Loaded extends ViewState {
            public final Banner getBanner() { return getBanner(); }
            public final String getGuildIcon() { return null; }
            public final long getGuildId() { return 0; }
            public final String getGuildName() { return ""; }
        }
    }
    public static final class Banner {
        public final String getHash() { return null; }
    }
}
