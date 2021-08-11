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
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.lytefast.flexinput.R$h;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class CheckLinks extends Plugin {
    public static class MoreInfoModal extends SettingsPage {
        private final Map<String, CachedUrlInfo.Data.Attributes.Entry> data;
        public MoreInfoModal(Map<String, CachedUrlInfo.Data.Attributes.Entry> data) {
            this.data = data;
        }

        public void onViewBound(View _view) {
            super.onViewBound(_view);

            //noinspection ResultOfMethodCallIgnored
            setActionBarTitle("URL info");

            var ctx = requireContext();
            var table = new TableLayout(ctx);
            int p = Utils.getDefaultPadding();
            int p2 = p / 2;

            var entries = data
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing((Map.Entry<String, CachedUrlInfo.Data.Attributes.Entry> e) -> e.getValue().result).reversed())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (var entry : entries) {
                var row = new TableRow(ctx);
                var header = new TextView(ctx, null, 0, R$h.UiKit_TextView);
                header.setText(entry.getKey());
                header.setPadding(p, p2, p, p2);

                var body = new TextView(ctx, null, 0, R$h.UiKit_TextView);
                body.setText(entry.getValue().result);
                body.setPadding(p, p2, p, p2);

                row.addView(header);
                row.addView(body);

                table.addView(row);
            }

            addView(table);
        }
    }

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Checks links via the VirusTotal api";
        manifest.version = "1.0.3";
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
                Map<String, CheckLinks.CachedUrlInfo.Data.Attributes.Entry> data = null;
                try {
                    data = checkLink(url);
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
                        int malicious = counts[1] + counts[2];
                        if (malicious > 2) content = String.format("URL %%s is likely malicious. %s engines flagged it.", malicious);
                        else if (malicious > 0) content = String.format("URL %%s is possibly malicious. %s engines flagged it.", malicious);
                        else content = "URL %s is either safe or too new to be flagged.";
                    }
                } catch (Throwable th) {
                    Log.e("[CheckLinks]", "Oops", th);
                    content = "Failed to check URL %s. Proceed at your own risk.";
                }

                content = String.format(content, url);
                if (data != null) content += "\n\nMore Info";
                SpannableString spannableContent = new SpannableString(content);

                int urlIdx = content.indexOf(url);
                spannableContent.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        var intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        dialog.startActivity(intent);
                    }
                }, urlIdx, urlIdx + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (data != null) {
                    var finalData = data;
                    spannableContent.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            var page = new MoreInfoModal(finalData);
                            Utils.openPageWithProxy(view.getContext(), page);
                        }
                    }, content.length() - 9, content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                Utils.mainThread.post(() -> {
                    text.setMovementMethod(LinkMovementMethod.getInstance());
                    text.setText(spannableContent);
                });
            });
        }));
    }

    public static class CachedUrlInfo {
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

    public static class NewUrlInfo {
        public Data data;
        public static class Data {
            public Attributes attributes;
            public static class Attributes {
                public Map<String, CachedUrlInfo.Data.Attributes.Entry> results;
            }
        }
    }

    public static class UrlIdInfo {
        public Data data;
        public static class Data {
            public String id;
        }
    }

    private Http.Request makeReq(String url, String method, String contentType) throws IOException {
        return new Http.Request(url, method)
            .setHeader("Content-Type", contentType)
            .setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:83.0) Firefox")
            .setHeader("X-Tool", "vt-ui-main")
            // Can be anything for some reason
            .setHeader("X-VT-Anti-Abuse-Header", "uwu")
            // yes upper case i lol
            .setHeader("Accept-Ianguage", "en-US,en;q=0.9,es;q=0.8");
    }

    private Map<String, CachedUrlInfo.Data.Attributes.Entry> checkLink(String url) throws IOException {
        var qb = new Http.QueryBuilder("https://www.virustotal.com/ui/search")
                .append("limit", "20")
                .append("relationships[comment]", "author,item")
                .append("query", url);

        CachedUrlInfo cached = makeReq(qb.toString(), "GET", "application/json").execute().json(CachedUrlInfo.class);
        if (cached.data.size() > 0) return cached.data.get(0).attributes.last_analysis_results;

        // no cached data, make full request

        var postData = "url=" + URLEncoder.encode(url, "UTF-8");
        UrlIdInfo idInfo = makeReq("https://www.virustotal.com/ui/urls", "POST", "application/x-www-form-urlencoded")
                .setHeader("Content-Length", Integer.toString(postData.length()))
                .executeWithBody(postData)
                .json(UrlIdInfo.class);

        NewUrlInfo newUrlInfo = makeReq("https://www.virustotal.com/ui/analyses/" + idInfo.data.id, "GET", "application/json")
                .execute()
                .json(NewUrlInfo.class);

        return newUrlInfo.data.attributes.results;
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
