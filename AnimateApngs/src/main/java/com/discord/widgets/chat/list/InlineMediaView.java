/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.chat.list;

import android.content.Context;

import androidx.cardview.widget.CardView;

import com.discord.api.message.embed.EmbedType;
import com.discord.databinding.InlineMediaViewBinding;
import com.discord.embed.RenderableEmbedMedia;

public final class InlineMediaView extends CardView {
    public InlineMediaView(Context context) { super(context); }

    public static InlineMediaViewBinding access$getBinding$p(InlineMediaView inlineMediaView) { return new InlineMediaViewBinding(); }

    private void updateUI(
            RenderableEmbedMedia renderableEmbedMedia,
            String progressiveMediaUri,
            EmbedType embedType,
            Integer width,
            Integer height,
            String featureTag
    ) { }
}
