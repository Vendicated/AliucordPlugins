package com.aliucord.plugins.checklinks

class Entry(val result: String)

class CachedUrlInfo(val data: List<Data>) {
    class Data(val attributes: Attributes) {
        class Attributes(val last_analysis_results: Map<String, Entry>) {
        }
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
