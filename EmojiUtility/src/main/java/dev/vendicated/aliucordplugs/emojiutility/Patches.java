/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.vendicated.aliucordplugs.emojiutility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.viewbinding.ViewBinding;

import com.aliucord.Utils;
import com.aliucord.api.PatcherAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.patcher.*;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.views.Button;
import com.discord.app.AppBottomSheet;
import com.discord.databinding.WidgetEmojiSheetBinding;
import com.discord.models.domain.emoji.Emoji;
import com.discord.models.guild.Guild;
import com.discord.utilities.textprocessing.node.EmojiNode;
import com.discord.widgets.chat.input.emoji.*;
import com.discord.widgets.chat.list.actions.*;
import com.discord.widgets.chat.managereactions.ManageReactionsEmojisAdapter;
import com.discord.widgets.emoji.WidgetEmojiSheet;
import com.discord.widgets.user.profile.UserProfileHeaderView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import dev.vendicated.aliucordplugs.emojiutility.clonemodal.Modal;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@SuppressWarnings({"UnusedReturnValue"})
public class Patches {
    private static Runnable extraButtonsUnhook;
    private static Runnable keepOpenUnhook;
    private static Runnable hideUnusableUnhook;

    public static void init(SettingsAPI settings, PatcherAPI patcher) throws Throwable {
        clickableStatusEmote(patcher);
        betterReactionSheet(patcher);

        if (settings.getBool("extraButtons", true)) {
            if (extraButtonsUnhook == null) extraButtonsUnhook = emojiModalExtraButtons(patcher);
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
    public static Runnable emojiModalExtraButtons(PatcherAPI patcher) throws Throwable {
        final int layoutId = View.generateViewId();

        var getEmojiIdAndType = WidgetEmojiSheet.class.getDeclaredMethod("getEmojiIdAndType");
        getEmojiIdAndType.setAccessible(true);
        var getBinding = WidgetEmojiSheet.class.getDeclaredMethod("getBinding");
        getBinding.setAccessible(true);

        return patcher.patch(WidgetEmojiSheet.class.getDeclaredMethod("configureButtons", boolean.class, boolean.class, Guild.class), new Hook(param -> {
            try {
                var args = param.args;
                var _this = param.thisObject;

                var emoji = (EmojiNode.EmojiIdAndType.Custom) getEmojiIdAndType.invoke(_this);
                if (emoji == null) return;

                long id = emoji.getId();
                boolean animated = emoji.isAnimated();
                var name = emoji.getName();
                var url = EmojiDownloader.getUrl(id, animated);

                var binding = (WidgetEmojiSheetBinding) getBinding.invoke(_this);
                if (binding == null) return;
                var root = (ViewGroup) binding.getRoot();
                var rootLayout = (LinearLayout) root.getChildAt(0);

                // Make Emoji Image open emoji in media viewer on click
                binding.d.setOnClickListener(v -> {
                    Utils.openMediaViewer(url, name + (animated ? ".gif" : ".png"));
                });

                if (rootLayout.findViewById(layoutId) != null) return;

                var ctx = rootLayout.getContext();
                if (ctx == null) return;

                int marginDpFour = DimenUtils.dpToPx(4);
                int marginDpEight = marginDpFour * 2;
                int marginDpSixteen = marginDpEight * 2;

                var copyLinkButton = new Button(ctx);
                copyLinkButton.setText("Copy Link");
                copyLinkButton.setOnClickListener(v -> {
                    Utils.setClipboard("copy url", url);
                    Utils.showToast("Copied to clipboard");
                });

                var copyCodeButton = new Button(ctx);
                copyCodeButton.setText("Copy Emoji Code");
                copyCodeButton.setOnClickListener(v -> {
                    Utils.setClipboard(
                            "copy emoji code",
                            String.format("<%s:%s:%s>", emoji.isAnimated() ? "a" : "", emoji.getName(), emoji.getId())
                    );
                    Utils.showToast("Copied to clipboard");
                });

                var saveButton = new Button(ctx);
                EmojiDownloader.configureSaveButton(ctx, saveButton, name, id, animated);

                var cloneButton = new Button(ctx);
                cloneButton.setText("Clone to other server");
                cloneButton.setOnClickListener(v -> Utils.openPageWithProxy(ctx, new Modal(url, emoji.getName(), id, animated)));

                var buttonParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(0, 0, 0, 0);
                copyLinkButton.setLayoutParams(buttonParams);
                copyCodeButton.setLayoutParams(buttonParams);
                saveButton.setLayoutParams(buttonParams);
                cloneButton.setLayoutParams(buttonParams);

                var pluginButtonLayout = new com.aliucord.widgets.LinearLayout(ctx);
                pluginButtonLayout.setId(layoutId);
                pluginButtonLayout.addView(copyLinkButton);
                pluginButtonLayout.addView(copyCodeButton);
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
            } catch (Throwable ignored) {
            }
        }));
    }

    public static Runnable keepEmojiPickerOpen(PatcherAPI patcher) throws Throwable {
        var isLongPress = new AtomicReference<>(false);

        var unhook1 = patcher.patch(MoreEmojisViewHolder.class.getDeclaredMethod("onConfigure", int.class, EmojiItem.class), new Hook(param -> {
            var _this = (MoreEmojisViewHolder) param.thisObject;
            _this.itemView.setOnLongClickListener(v -> {
                isLongPress.set(true);
                var actions = ((WidgetChatListActions$onViewCreated$2) MoreEmojisViewHolder.access$getAdapter$p(_this).getOnClickMoreEmojis()).this$0;

                EmojiPickerNavigator.launchBottomSheet(
                        actions.getParentFragmentManager(),
                        e -> WidgetChatListActions.access$addReaction(actions, e),
                        EmojiPickerContextType.Chat.INSTANCE,
                        () -> {
                            isLongPress.set(false);
                            actions.dismiss();
                            return null;
                        }
                );

                return true;
            });
        }));

        var unhook2 = patcher.patch(AppBottomSheet.class.getDeclaredMethod("dismiss"), new PreHook(param -> {
            if (isLongPress.get() && param.thisObject instanceof WidgetChatListActions) param.setResult(null);
        }));

        final var onEmojiPickedField = WidgetEmojiPickerSheet.class.getDeclaredField("emojiPickerListenerDelegate");
        onEmojiPickedField.setAccessible(true);


        var unhook3 = patcher.patch(WidgetEmojiPickerSheet.class, "onEmojiPicked", new Class<?>[]{Emoji.class}, new PreHook(callFrame -> {
            if (isLongPress.get()) try {
                var listener = (EmojiPickerListener) onEmojiPickedField.get(callFrame.thisObject);
                if (listener != null) listener.onEmojiPicked((Emoji) callFrame.args[0]);
                callFrame.setResult(null);
            } catch (Throwable ignored) {
            }
        }));

        return () -> {
            unhook1.run();
            unhook2.run();
            unhook3.run();
        };
    }

    public static Runnable hideUnusableEmojis(PatcherAPI patcher) throws Throwable {
        return patcher.patch(
                EmojiPickerViewModel.Companion.class.getDeclaredMethod("buildEmojiListItems", Collection.class, Function1.class, String.class, boolean.class, boolean.class, boolean.class),
                new PreHook(param -> {
                    var list = (Collection<Emoji>) param.args[0];
                    try {
                        list.removeIf(e -> !e.isUsable());
                    } catch (UnsupportedOperationException ignored) {
                        list = new ArrayList<>(list);
                        param.args[0] = list;
                        list.removeIf(e -> !e.isUsable());
                    }
                })
        );
    }

    public static void clickableStatusEmote(PatcherAPI patcher) throws Throwable {
        patcher.patch(EmojiNode.RenderContext.DefaultImpls.class.getDeclaredMethod("onEmojiClicked", EmojiNode.RenderContext.class, EmojiNode.EmojiIdAndType.class),
                new InsteadHook(param -> {
                    WidgetEmojiSheet.Companion.enqueueNotice((EmojiNode.EmojiIdAndType) param.args[1]);
                    return Unit.a;
                }));

        patcher.patch(
                UserProfileHeaderView.class.getDeclaredConstructor(Context.class, AttributeSet.class),
                new Hook(param -> {
                    try {
                        var view = (UserProfileHeaderView) param.thisObject;
                        var binding = (ViewBinding) ReflectUtils.getField(view, "binding");
                        var status = (TextView) binding.getRoot().findViewById(Utils.getResId("user_profile_header_custom_status", "id"));
                        status.setMovementMethod(LinkMovementMethod.getInstance());
                    } catch (Throwable ignored) {
                    }
                }));
    }

    public static void betterReactionSheet(PatcherAPI patcher) throws Throwable {
        patcher.patch(ManageReactionsEmojisAdapter.ReactionEmojiViewHolder.class.getDeclaredMethod("onConfigure", int.class, ManageReactionsEmojisAdapter.ReactionEmojiItem.class), new Hook(param -> {
            try {
                var binding = (ViewBinding) ReflectUtils.getField(param.thisObject, "binding");
                assert binding != null;
                var reactionItem = (ManageReactionsEmojisAdapter.ReactionEmojiItem) param.args[1];
                binding.getRoot().setOnLongClickListener(v -> {
                    var emoji = reactionItem.getReaction().b();
                    var idAndType = EmojiNode.Companion.generateEmojiIdAndType(emoji);
                    WidgetEmojiSheet.Companion.show(Utils.appActivity.getSupportFragmentManager(), idAndType);
                    return true;
                });
            } catch (Throwable ignored) {
            }
        }));
    }
}
