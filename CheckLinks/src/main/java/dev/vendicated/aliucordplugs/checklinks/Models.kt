/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.checklinks

class Entry(val result: String)

class CachedUrlInfo(val data: List<Data>) {
    class Data(val attributes: Attributes) {
        class Attributes(val last_analysis_results: Map<String, Entry>)
    }
}

class NewUrlInfo(val data: Data) {
    class Data(val attributes: Attributes) {
        class Attributes(val results: Map<String, Entry>)
    }
}

class UrlIdInfo(val data: Data) {
    class Data(val id: String)
}
