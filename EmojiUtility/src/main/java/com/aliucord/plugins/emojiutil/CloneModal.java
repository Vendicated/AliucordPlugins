/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.emojiutil;

import android.content.Context;
import android.util.Base64;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.appcompat.widget.TooltipCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.aliucord.CollectionUtils;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.EmojiUtility;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.GuildEmojiWrapper;
import com.discord.api.permission.Permission;
import com.discord.models.guild.Guild;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreGuilds;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.extensions.SimpleDraweeViewExtensionsKt;
import com.discord.utilities.permissions.PermissionUtils;
import com.discord.utilities.rest.RestAPI;
import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R$b;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CloneModal extends SettingsPage {
    private static final int guildIconRadiusIs = Utils.getResId("guild_icon_radius", "dimen");

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

    public CloneModal(String url, String name, long id, boolean isAnimated) {
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
        var layout = (LinearLayout) ((NestedScrollView) ((CoordinatorLayout) view).getChildAt(1)).getChildAt(0);
        int p = Utils.getDefaultPadding();
        layout.setPadding(p, p, p, p);

        var grid = new GridLayout(ctx);

        grid.setColumnCount(3);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        int m = Utils.dpToPx(8); // margin
        var color = Integer.valueOf(ColorCompat.getThemedColor(ctx, R$b.colorBackgroundPrimary));
        var dimension = ctx.getResources().getDimension(guildIconRadiusIs);

        var guilds = getGuilds();
        for (int i = 0; i < guilds.size(); i++) {
            var guild = guilds.get(i);

            // https://stackoverflow.com/a/53948318
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int currentCol = i % 3;
            int currentRow = i / 3;
            params.columnSpec = GridLayout.spec(currentCol, 1, 1);
            params.rowSpec = GridLayout.spec(currentRow, 1, 1);
            params.setMargins(m, m, m, m);

            var img = new SimpleDraweeView(ctx);
            img.setAdjustViewBounds(true);

            img.setOnClickListener(e -> clone(ctx, guild));

            TooltipCompat.setTooltipText(img, guild.getName());
            SimpleDraweeViewExtensionsKt.setGuildIcon$default(img, true, guild, dimension, null, color, null, null, true, null, 360, null);

            grid.addView(img, params);
        }

        layout.addView(grid);
    }

    private List<Guild> getGuilds() {
        return CollectionUtils.filter(guildStore.getGuilds().values(), this::isCandidate);
    }

    private boolean isCandidate(Guild guild) {
        var perms = guildPerms.get(guild.getId());
        if (!PermissionUtils.can(Permission.MANAGE_EMOJIS, perms)) return false;
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

    private void clone(Context ctx, Guild guild) {
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
