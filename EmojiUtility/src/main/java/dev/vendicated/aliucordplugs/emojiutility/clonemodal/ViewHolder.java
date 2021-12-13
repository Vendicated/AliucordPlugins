/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.emojiutility.clonemodal;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.facebook.drawee.view.SimpleDraweeView;

public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private static final int iconId = Utils.getResId("user_profile_adapter_item_server_image", "id");
    private static final int iconTextId = Utils.getResId("user_profile_adapter_item_server_text", "id");
    private static final int serverNameId = Utils.getResId("user_profile_adapter_item_server_name", "id");
    private static final int identityBarrierId = Utils.getResId("guild_member_identity_barrier", "id");
    private static final int serverAvatarId = Utils.getResId("guild_member_avatar", "id");
    private static final int serverNickId = Utils.getResId("user_profile_adapter_item_user_display_name", "id");

    private final Adapter adapter;

    public final SimpleDraweeView icon;
    public final TextView iconText;
    public final TextView name;

    public ViewHolder(Adapter adapter, @NonNull ViewGroup layout) {
        super(layout);
        this.adapter = adapter;

        icon = layout.findViewById(iconId);
        iconText = layout.findViewById(iconTextId);
        name = layout.findViewById(serverNameId);

        // Hide server profile stuff
        layout.findViewById(identityBarrierId).setVisibility(View.GONE);
        layout.findViewById(serverAvatarId).setVisibility(View.GONE);
        layout.findViewById(serverNickId).setVisibility(View.GONE);

        layout.setOnClickListener(this);
    }

    @Override public void onClick(View view) {
        adapter.onClick(getAdapterPosition());
    }
}
