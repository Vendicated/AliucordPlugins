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

public class Plugin {
    public static class Info {
        public String version;
        public int minimumDiscordVersion;
    }
    public static class CardInfo {
        public final String name;
        public final String title;
        public final boolean exists;
        public CardInfo(String name, String title, boolean exists) {
            this.name = name;
            this.title = title;
            this.exists = exists;
        }
    }
}
