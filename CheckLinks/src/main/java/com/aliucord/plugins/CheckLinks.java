/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CheckLinks extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Checks links via the VirusTotal api";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context ctx) throws Throwable {
        var dialogTextId = Utils.getResId("masked_links_body_text", "id");
        patcher.patch(c.a.a.g.a.class.getMethod("onViewBound", View.class), new PinePatchFn(callFrame -> {
            var dialog = (c.a.a.g.a) callFrame.thisObject;
            var bundle = dialog.getArguments();
            if (bundle == null) return;
            var url = bundle.getString("WIDGET_SPOOPY_LINKS_DIALOG_URL");
            var binding = dialog.g();
            var text = (TextView) binding.getRoot().findViewById(dialogTextId);
            text.setText(String.format("Checking URL %s...", url));
            Utils.threadPool.execute(() -> {
                String content;
                try {
                    var data = checkLink(url);
                    if (data == null) {
                        content = "No info on URL %s. Proceed at your own risk.";
                    } else {
                        var counts = new int[4];
                        for (var entry : data.values()) {
                            var res = entry.result;
                            switch (res) {
                                case "clean":
                                    counts[0]++;
                                    break;
                                case "phishing":
                                    counts[1]++;
                                    break;
                                case "malicious":
                                    counts[2]++;
                                    break;
                                default:
                                    counts[3]++;
                                    break;
                            }
                        }
                        content = String.format(
                                "URL %%s is %s\nClean: %s\nUnrated: %s\nMalicious: %s\nPhishing: %s",
                                counts[1] > 2 || counts[2] > 2
                                        ? "likely malicious!"
                                        : (counts[1] > 0 || counts[2] > 0)
                                            ? "possibly malicious."
                                            : "clean or too new to be flagged.",
                                counts[0],
                                counts[3],
                                counts[2],
                                counts[1]
                        );
                    }
                } catch (Throwable th) {
                    Log.e("[CheckLinks]", "Oops", th);
                    content = "Failed to check URL %s. Proceed at your own risk.";
                }
                var finalContent = String.format(content, url);
                Utils.mainThread.post(() -> text.setText(finalContent));
            });
        }));
    }

    public static class UrlInfo {
        public List<Data> data;
        public static class Data {
            public Attributes attributes;
            public static class Attributes {
                public Map<String, Entry> last_analysis_results;
                public static class Entry {
                    public String result;
                }
            }
        }
    }

    private Map<String, UrlInfo.Data.Attributes.Entry> checkLink(String url) throws IOException {
        var qb = new Http.QueryBuilder("https://www.virustotal.com/ui/search")
                .append("limit", "20")
                .append("relationships[comment]", "author,item")
                .append("query", url);

        var req = new Http.Request(qb.toString())
                .setHeader("Content-Type", "application/json")
                .setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:83.0) Firefox")
                .setHeader("X-Tool", "vt-ui-main")
                // Can be anything for some reason
                .setHeader("X-VT-Anti-Abuse-Header", "uwu")
                // yes upper case i lol
                .setHeader("Accept-Ianguage", "en-US,en;q=0.9,es;q=0.8");

        UrlInfo res = req.execute().json(UrlInfo.class);
        return res.data.size() > 0 ? res.data.get(0).attributes.last_analysis_results : null;
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
