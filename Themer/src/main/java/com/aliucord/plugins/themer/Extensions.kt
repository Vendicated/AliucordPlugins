package com.aliucord.plugins.themer

import com.aliucord.api.SettingsAPI

var SettingsAPI.transparencyMode
        get() = TransparencyMode.from(getInt("transparencyMode", TransparencyMode.NONE.value))
        set(v) = setInt("transparencyMode", v.value)
