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
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.discord.api.message.embed.EmbedType;
import com.discord.embed.RenderableEmbedMedia;
import com.discord.widgets.chat.list.InlineMediaView;
import com.discord.widgets.media.WidgetMedia;

import java.util.regex.Pattern;

public class AnimateApngs extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Makes apngs embeds animate properly";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private void initApng(ImageView view, String mediaUrl, Integer w, Integer h) {
        // media server serves them as regular pngs, only cdn serves actual apngs
        final var url = mediaUrl.replace("media.discordapp.net", "cdn.discordapp.com");

        Utils.threadPool.execute(() -> {
            try (var is = new Http.Request(url).execute().stream()){
                // You cannot consume the stream twice so just yolo it and don't use Apng.Companion.isApng first, eh whatever
                var drawable = c.l.a.a.a(is, w, h);
                if (view != null)
                    Utils.mainThread.post(() -> {
                        view.setImageDrawable(drawable);
                        drawable.start();
                    });
            } catch (Throwable ignored) { }
        });
    }

    @Override
    public void start(Context ctx) throws Throwable {
        int previewResId = Utils.getResId("inline_media_image_preview", "id");
        int mediaResId = Utils.getResId("media_image", "id");

        var updateUI = InlineMediaView.class.getDeclaredMethod("updateUI", RenderableEmbedMedia.class, String.class, EmbedType.class, Integer.class, Integer.class, String.class);
        patcher.patch(updateUI, new PinePatchFn(callFrame -> {
            var media = (RenderableEmbedMedia) callFrame.args[0];
            if (media == null || !media.a.endsWith(".png")) return;

            // media server serves them as regular pngs, only cdn serves actual apngs
            var url = media.a.replace("media.discordapp.net", "cdn.discordapp.com");
            var binding = InlineMediaView.access$getBinding$p((InlineMediaView) callFrame.thisObject);
            var view = (ImageView) binding.getRoot().findViewById(previewResId);
            initApng(view, url, media.b, media.c);
        }));


        var pattern = Pattern.compile("\\.png(\\?width=(\\d+)&height=(\\d+))?");

        var uriField = WidgetMedia.class.getDeclaredField("imageUri");
        uriField.setAccessible(true);
        var getFormattedUrl = WidgetMedia.class.getDeclaredMethod("getFormattedUrl", Context.class, Uri.class);
        getFormattedUrl.setAccessible(true);

        patcher.patch(WidgetMedia.class.getDeclaredMethod("configureMediaImage"), new PinePatchFn(callFrame -> {
            try {
                var widgetMedia = (WidgetMedia) callFrame.thisObject;
                var url = (String) getFormattedUrl.invoke(widgetMedia, widgetMedia.requireContext(), uriField.get(widgetMedia));
                if (url == null) return;
                var match = pattern.matcher(url);
                if (match.find()) {
                    String w = match.group(1);
                    String h = match.group(2);
                    var view = (ImageView) WidgetMedia.access$getBinding$p(widgetMedia).getRoot().findViewById(mediaResId);
                    initApng(view, url, w != null ? Integer.parseInt(w) : null, h != null ? Integer.parseInt(h) : null);
                }
            } catch (Throwable ignored) { }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
