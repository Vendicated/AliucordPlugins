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

import android.annotation.SuppressLint;
import android.content.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.aliucord.Utils;
import com.aliucord.api.PatcherAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.patcher.PinePrePatchFn;
import com.aliucord.plugins.emojiutil.clonemodal.Modal;
import com.aliucord.views.Button;
import com.discord.app.AppBottomSheet;
import com.discord.databinding.WidgetEmojiSheetBinding;
import com.discord.models.domain.emoji.Emoji;
import com.discord.models.guild.Guild;
import com.discord.utilities.textprocessing.node.EmojiNode;
import com.discord.widgets.chat.input.emoji.*;
import com.discord.widgets.chat.list.actions.*;
import com.discord.widgets.emoji.WidgetEmojiSheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.jvm.functions.Function1;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

@SuppressWarnings({"UnusedReturnValue", "JavaReflectionMemberAccess"})
public class Patches {
    private static Runnable extraButtonsUnhook;
    private static Runnable keepOpenUnhook;
    private static Runnable hideUnusableUnhook;

    public static void init(Context ctx, SettingsAPI settings, PatcherAPI patcher) throws Throwable {
        if (settings.getBool("extraButtons", true)) {
            if (extraButtonsUnhook == null) extraButtonsUnhook = emojiModalExtraButtons(ctx, patcher);
        } else if (extraButtonsUnhook != null) {
            extraButtonsUnhook.run();
            extraButtonsUnhook = null;
        }

        if (settings.getBool("keepOpen", true)) {
            if (keepOpenUnhook == null) keepOpenUnhook = keepEmojiPickerOpen(patcher);
        } else if (keepOpenUnhook != null) {
            keepOpenUnhook.run();
            keepOpenUnhook = null;
        }

        if (settings.getBool("hideUnusable", true)) {
            if (hideUnusableUnhook == null) hideUnusableUnhook = hideUnusableEmojis(patcher);
        } else if (hideUnusableUnhook != null) {
            hideUnusableUnhook.run();
            hideUnusableUnhook = null;
        }
    }

    @SuppressLint("SetTextI18n")
    public static Runnable emojiModalExtraButtons(Context ctx, PatcherAPI patcher) throws Throwable {
        final int layoutId = View.generateViewId();
        var clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

        var getEmojiIdAndType = WidgetEmojiSheet.class.getDeclaredMethod("getEmojiIdAndType");
        getEmojiIdAndType.setAccessible(true);
        var getBinding = WidgetEmojiSheet.class.getDeclaredMethod("getBinding");
        getBinding.setAccessible(true);

        return patcher.patch(WidgetEmojiSheet.class.getDeclaredMethod("configureButtons", boolean.class, boolean.class, Guild.class), new MethodHook() {
            @Override
            public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                super.afterCall(callFrame);

                var args = callFrame.args;
                var _this = callFrame.thisObject;

                var emoji = (EmojiNode.EmojiIdAndType.Custom) getEmojiIdAndType.invoke(_this);
                if (emoji == null) return;

                long id = emoji.getId();
                boolean animated = emoji.isAnimated();
                var nameAndUrl = EmojiDownloader.getFilenameAndUrl(emoji.getId(), emoji.isAnimated());
                var fileName = nameAndUrl.first;
                var url = nameAndUrl.second;

                var binding = (WidgetEmojiSheetBinding) getBinding.invoke(_this);
                if (binding == null) return;
                var root = (ViewGroup) binding.getRoot();
                if (root == null) return;
                var rootLayout = (LinearLayout) root.getChildAt(0);

                if (rootLayout.findViewById(layoutId) != null) return;

                var ctx = rootLayout.getContext();
                if (ctx == null) return;

                int marginDpFour = Utils.dpToPx(4);
                int marginDpEight = marginDpFour * 2;
                int marginDpSixteen = marginDpEight * 2;

                var copyLinkButton = new Button(ctx);
                copyLinkButton.setText("Copy Link");
                copyLinkButton.setOnClickListener(v -> {
                    var clip = ClipData.newPlainText("Copy emoji link", url);
                    clipboard.setPrimaryClip(clip);
                    Utils.showToast(ctx, "Copied to clipboard");
                });

                var saveButton = new Button(ctx);
                EmojiDownloader.configureSaveButton(ctx, saveButton, fileName, id, animated);

                var cloneButton = new Button(ctx);
                cloneButton.setText("Clone to other server");
                cloneButton.setOnClickListener(v -> Utils.openPageWithProxy(ctx, new Modal(url, emoji.getName(), id, animated)));

                var buttonParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(0, 0, 0, 0);
                copyLinkButton.setLayoutParams(buttonParams);
                saveButton.setLayoutParams(buttonParams);
                cloneButton.setLayoutParams(buttonParams);

                var pluginButtonLayout = new com.aliucord.widgets.LinearLayout(ctx);
                pluginButtonLayout.setId(layoutId);
                pluginButtonLayout.addView(copyLinkButton);
                pluginButtonLayout.addView(saveButton);
                pluginButtonLayout.addView(cloneButton);

                int idx = 2;
                if (
                        (args[0].equals(false) /* need nitro */ ||
                                args[1].equals(false) /* not on server */
                        ) && args[2] != null
                ) {
                    // Nitro or Join Button visible
                    pluginButtonLayout.setPadding(marginDpSixteen, marginDpFour, marginDpSixteen, marginDpEight);

                    // Adjust nitro and join button
                    var params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, 0);
                    binding.q.setLayoutParams(params); // Nitro
                    binding.o.setLayoutParams(params); // Join

                    // Adjust nitro/join container
                    var joinContainer = (FrameLayout) binding.o.getParent();
                    var containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    containerParams.setMargins(marginDpSixteen, marginDpEight, marginDpSixteen, 0);
                    joinContainer.setLayoutParams(containerParams);
                } else if (args[2] != null) {
                    // Favourite buttons
                    idx = 3;
                    pluginButtonLayout.setPadding(marginDpSixteen, marginDpFour, marginDpSixteen, marginDpEight);

                    // Adjust  fav button margins
                    var params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, marginDpEight, 0, 0);
                    binding.f.setLayoutParams(params); // Fav
                    binding.h.setLayoutParams(params); // Unfav

                    // Adjust favs container
                    var favsContainer = (FrameLayout) binding.f.getParent();
                    var containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    containerParams.setMargins(marginDpSixteen, marginDpEight, marginDpSixteen, 0);
                    favsContainer.setLayoutParams(containerParams);
                } else {
                    // No buttons
                    pluginButtonLayout.setPadding(marginDpSixteen, marginDpEight, marginDpSixteen, marginDpEight);
                }

                rootLayout.addView(pluginButtonLayout, idx);
            }
        });
    }

    public static Runnable keepEmojiPickerOpen(PatcherAPI patcher) throws Throwable {
        var isLongPress = new AtomicReference<>(false);

        var unhook1 = patcher.patch(MoreEmojisViewHolder.class.getDeclaredMethod("onConfigure", int.class, EmojiItem.class), new PinePatchFn(callFrame -> {
            var _this = (MoreEmojisViewHolder) callFrame.thisObject;
            _this.itemView.setOnLongClickListener(v -> {
                isLongPress.set(true);
                var actions = ((WidgetChatListActions$onViewCreated$2) MoreEmojisViewHolder.access$getAdapter$p(_this).getOnClickMoreEmojis()).this$0;

                EmojiPickerNavigator.launchBottomSheet(
                        actions.getParentFragmentManager(),
                        e -> WidgetChatListActions.access$addReaction(actions, e),
                        EmojiPickerContextType.CHAT,
                        () -> { isLongPress.set(false); actions.dismiss(); return null; }
                );

                return true;
            });
        }));

        var unhook2 = patcher.patch(AppBottomSheet.class.getDeclaredMethod("dismiss"), new PinePrePatchFn(callFrame -> {
            if (isLongPress.get() && callFrame.thisObject instanceof WidgetChatListActions) callFrame.setResult(null);
        }));

        var clazz = Class.forName("com.discord.widgets.chat.input.emoji.WidgetEmojiPickerSheet");
        final var onEmojiPickedField = clazz.getDeclaredField("emojiPickerListenerDelegate");
        onEmojiPickedField.setAccessible(true);

        var unhook3 = patcher.patch(clazz, "onEmojiPicked", new Class<?>[]{Emoji.class}, new PinePrePatchFn(callFrame -> {
            if (isLongPress.get()) try {
                var listener = (EmojiPickerListener) onEmojiPickedField.get(callFrame.thisObject);
                if (listener != null) listener.onEmojiPicked((Emoji) callFrame.args[0]);
                callFrame.setResult(null);
            } catch (Throwable ignored) { }
        }));

        return () -> { unhook1.run(); unhook2.run(); unhook3.run(); };
    }

    // Credit: https://github.com/Juby210/Aliucord-plugins/blob/d8f6da1ad387c0f94796b8efce6915163aeebb5b/HideDisabledEmojis/src/main/java/com/aliucord/plugins/HideDisabledEmojis.java
    public static Runnable hideUnusableEmojis(PatcherAPI patcher) {
        return patcher.patch(
                "com.discord.widgets.chat.input.emoji.EmojiPickerViewModel$Companion", "buildEmojiListItems",
                new Class<?>[]{ Collection.class, Function1.class, String.class, boolean.class, boolean.class, boolean.class },
                new PinePrePatchFn(callFrame -> {
                    var emojis = (Collection<? extends Emoji>) callFrame.args[0];
                    if (!(emojis instanceof ArrayList)) {
                        emojis = new ArrayList<>(emojis);
                        callFrame.args[0] = emojis;
                    }
                    emojis.removeIf(e -> !e.isUsable());
                })
        );
    }
}
