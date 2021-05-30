/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.urban;

import java.util.List;

public class ApiResponse {
    public List<Definition> list;
    public static class Definition {
        public String definition;
        public String permalink;
        public String word;
        public int thumbs_down;
        public int thumbs_up;
    }
}

