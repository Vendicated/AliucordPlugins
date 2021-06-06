/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.utilities.textprocessing.node;

import java.io.Serializable;

public final class EmojiNode {
    public static abstract class EmojiIdAndType implements Serializable {
        public static final class Custom extends EmojiIdAndType {
            public final long getId() { return 0; }
            public final String getName() { return null; }
            public final boolean isAnimated() { return false; }
        }
    }
}
