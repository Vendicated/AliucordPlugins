/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer

import com.aliucord.api.SettingsAPI

var SettingsAPI.transparencyMode
        get() = TransparencyMode.from(getInt("transparencyMode", TransparencyMode.NONE.value))
        set(v) = setInt("transparencyMode", v.value)
