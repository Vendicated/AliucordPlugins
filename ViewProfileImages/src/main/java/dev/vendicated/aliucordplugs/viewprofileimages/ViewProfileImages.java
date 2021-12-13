/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.viewprofileimages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import androidx.core.widget.NestedScrollView;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.databinding.WidgetGuildProfileSheetBinding;
import com.discord.models.member.GuildMember;
import com.discord.models.user.User;
import com.discord.utilities.SnowflakeUtils;
import com.discord.utilities.icon.IconUtils;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel;
import com.discord.widgets.media.WidgetMedia;
import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;

import java.lang.reflect.Field;

@AliucordPlugin
public class ViewProfileImages extends Plugin {
    private static final Logger logger = new Logger("ViewProfileImages");
    private static Field fileNameField;
    private static Field idField;
    private static Field urlField;
    private static Field proxyUrlField;

    @SuppressWarnings("AccessStaticViaInstance")
    private void openAttachment(Context ctx, String url, String name) {
        var attachment = ReflectUtils.allocateInstance(MessageAttachment.class);
        try {
            fileNameField.set(attachment, String.format("%s.%s", name, getExtFromUrl(url)));
            idField.set(attachment, SnowflakeUtils.fromTimestamp(System.currentTimeMillis()));
            urlField.set(attachment, url);
            proxyUrlField.set(attachment, url);
        } catch (Throwable th) {
            logger.errorToast("Failed to build fake attachment D:", th);
            return;
        }

        WidgetChatListAdapterItemAttachment.Companion.access$navigateToAttachment(WidgetChatListAdapterItemAttachment.Companion, ctx, attachment);
    }

    private static String getForGuildMemberOrUser(User user, GuildMember guildMember) {
        return guildMember == null || !guildMember.hasAvatar() ?
            IconUtils.getForUser(user, true, 2048) :
            IconUtils.INSTANCE.getForGuildMember(guildMember, 2048, true);
    }

    private static String getExtFromUrl(String url) {
        var sub = url.substring(url.lastIndexOf('.') + 1);
        return sub.contains("?") ? sub.substring(0, sub.indexOf('?')) : sub;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) throws Throwable {
        var c = MessageAttachment.class;
        fileNameField = c.getDeclaredField("filename");
        fileNameField.setAccessible(true);
        idField = c.getDeclaredField("id");
        idField.setAccessible(true);
        urlField = c.getDeclaredField("url");
        urlField.setAccessible(true);
        proxyUrlField = c.getDeclaredField("proxyUrl");
        proxyUrlField.setAccessible(true);

        final var getBindingMethod = WidgetGuildProfileSheet.class.getDeclaredMethod("getBinding");
        getBindingMethod.setAccessible(true);

        final int avatarResId = Utils.getResId("avatar", "id");
        final int bannerResId = Utils.getResId("banner", "id");
        final int secondaryNameResId = Utils.getResId("user_profile_header_secondary_name", "id");
        final int guildIconResId = Utils.getResId("guild_profile_sheet_icon", "id");
        final int guildBannerResId = Utils.getResId("guild_profile_sheet_banner", "id");

        // This patch is needed to allow size query parameter. This function naively adds ?width=x&height=x to the URI
        // so avatar urls become https://cdn.discord.com/.../...?size=2048?width=... which breaks the size parameter
        // so we end up with the 128x128 avatar. Enjoy your pixels ;)
        patcher.patch(WidgetMedia.class.getDeclaredMethod("getFormattedUrl", Context.class, Uri.class), new Hook(param -> {
            var uri = (param.args[1]).toString();
            if (uri.contains("?size=")) param.setResult(uri);
        }));

        patcher.patch(UserProfileHeaderView.class.getDeclaredMethod("configureBanner", UserProfileHeaderViewModel.ViewState.Loaded.class), new Hook(param -> {
            var binding = UserProfileHeaderView.access$getBinding$p((UserProfileHeaderView) param.thisObject);
            var root = binding.getRoot();
            var data = (UserProfileHeaderViewModel.ViewState.Loaded) param.args[0];
            if (data.getEditable()) return;
            var user = data.getUser();

            final var bannerHash = data.getBanner();
            final var username = user.getUsername();
            final var userId = user.getId();

            var avatarView = root.findViewById(avatarResId);
            if (avatarView != null) {
                avatarView.setOnClickListener(e -> openAttachment(e.getContext(), getForGuildMemberOrUser(user, data.getGuildMember()), username));
            }

            if (data.getShowSmallAvatar()) {
                var secondaryNameView = root.findViewById(secondaryNameResId);
                if (secondaryNameView != null) {
                    secondaryNameView.setOnClickListener(e -> openAttachment(e.getContext(), IconUtils.getForUser(user, true, 2048), username));
                }
            }

            var bannerView = root.findViewById(bannerResId);
            if (bannerView != null && bannerHash != null) {
                bannerView.setOnClickListener(e -> {
                    var banner = IconUtils.INSTANCE.getForUserBanner(userId, bannerHash, 2048, true);
                    openAttachment(e.getContext(), banner, username);
                });
            }
        }));

        patcher.patch(WidgetGuildProfileSheet.class.getDeclaredMethod("configureUI", WidgetGuildProfileSheetViewModel.ViewState.Loaded.class), new Hook(param -> {
            try {
                var data = (WidgetGuildProfileSheetViewModel.ViewState.Loaded) param.args[0];
                final var guildId = data.getGuildId();
                final var guildName = data.getGuildName();
                final var iconHash = data.getGuildIcon();
                final var bannerHash = data.getBanner().getHash();

                var binding = (WidgetGuildProfileSheetBinding) getBindingMethod.invoke(param.thisObject);
                if (binding == null) return;
                var root = (NestedScrollView) binding.getRoot();

                var iconView = root.findViewById(guildIconResId);
                if (iconView != null && iconHash != null) {
                    iconView.setOnClickListener(e -> {
                        var icon = IconUtils.getForGuild(guildId, iconHash, "", true, 2048);
                        openAttachment(e.getContext(), icon, guildName);
                    });
                }

                var bannerView = root.findViewById(guildBannerResId);
                if (bannerView != null && bannerHash != null) {
                    bannerView.setOnClickListener(e -> {
                        var banner = IconUtils.INSTANCE.getBannerForGuild(guildId, bannerHash, 2048, true);
                        openAttachment(e.getContext(), banner, guildName);
                    });
                }
            } catch (Throwable ignored) {}
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
