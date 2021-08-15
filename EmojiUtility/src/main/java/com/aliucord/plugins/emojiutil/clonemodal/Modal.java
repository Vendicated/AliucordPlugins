/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.emojiutil.clonemodal;

import android.content.Context;
import android.util.Base64;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.*;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.EmojiUtility;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.GuildEmojiWrapper;
import com.discord.api.permission.Permission;
import com.discord.models.guild.Guild;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreGuilds;
import com.discord.stores.StoreStream;
import com.discord.utilities.permissions.PermissionUtils;
import com.discord.utilities.rest.RestAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Modal extends SettingsPage {
    private static final Map<Integer, Integer> emojiLimits = new HashMap<>();
    static {
        emojiLimits.put(0, 50);
        emojiLimits.put(1, 100);
        emojiLimits.put(2, 150);
        emojiLimits.put(3, 250);
    }

    private final Map<Long, Long> guildPerms = StoreStream.getPermissions().getGuildPermissions();
    private final StoreGuilds guildStore = StoreStream.getGuilds();

    private final String name;
    private final String url;
    private final long id;
    private final boolean isAnimated;

    public Modal(String url, String name, long id, boolean isAnimated) {
        this.url = url;
        this.name = name;
        this.id = id;
        this.isAnimated = isAnimated;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onViewBound(View view) {
        super.onViewBound(view);

        setActionBarTitle("Clone Emoji");
        setActionBarSubtitle(name);

        var ctx = view.getContext();

        setPadding(0);

        var recycler = new RecyclerView(ctx);
        recycler.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.VERTICAL, false));
        var adapter = new Adapter(this, CollectionUtils.filter(guildStore.getGuilds().values(), this::isCandidate));
        recycler.setAdapter(adapter);

        addView(recycler);
    }

    private boolean isCandidate(Guild guild) {
        var perms = guildPerms.get(guild.getId());
        if (!PermissionUtils.can(Permission.MANAGE_EMOJIS_AND_STICKERS, perms)) return false;
        int usedSlots = 0;
        for (var emoji : guild.getEmojis()) {
            if (GuildEmojiWrapper.getId(emoji) == id) return false;
            if (GuildEmojiWrapper.isAnimated(emoji) == isAnimated) usedSlots++;
        }
        var slots = emojiLimits.get(guild.getPremiumTier());
        return slots != null && usedSlots < slots;
    }

    private String imageToDataUri() {
        try {
            var res = new Http.Request(url).execute();
            try (var baos = new ByteArrayOutputStream()) {
                res.pipe(baos);
                var b64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                return String.format("data:image/%s;base64,%s", isAnimated ? "gif" : "png", b64);
            }
        } catch (IOException ex) { EmojiUtility.logger.error(ex); return null; }
    }

    public void clone(Context ctx, Guild guild) {
        Utils.threadPool.execute(() -> {
            var api = RestAPI.getApi();
            var uri = imageToDataUri();
            if (uri == null) {
                EmojiUtility.logger.error(ctx, "Something went wrong while preparing the image");
                return;
            }
            var obs = api.postGuildEmoji(guild.getId(), new RestAPIParams.PostGuildEmoji(name, uri));
            var res = RxUtils.getResultBlocking(obs);
            if (res.second == null)
                EmojiUtility.logger.info(ctx, String.format("Successfully cloned %s to %s", name, guild.getName()));
            else
                EmojiUtility.logger.error(ctx, "Something went wrong while cloning this emoji", res.second);
        });
    }
}
