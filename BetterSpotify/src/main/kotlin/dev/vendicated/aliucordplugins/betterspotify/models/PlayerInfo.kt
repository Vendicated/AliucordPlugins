/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.betterspotify.models

class PlayerInfo(
    val progress_ms: Int,
    val is_playing: Boolean,
    val currently_playing_type: String,
    val shuffle_state: Boolean,
    val repeat_state: String,
    val device: Device,
    val item: Item
) {
    class Device(val name: String, val volume_percent: Int)
    class Item(val id: String, val name: String, val type: String, val uri: String)
}
