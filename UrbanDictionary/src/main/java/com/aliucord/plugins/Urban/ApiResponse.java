/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * These plugins are free software: you can redistribute them and/or modify
 * them under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * They are distributed in the hope that they will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.aliucord.plugins.Urban;

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

