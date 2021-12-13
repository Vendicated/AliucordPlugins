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

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.discord.models.guild.Guild;
import com.discord.utilities.extensions.SimpleDraweeViewExtensionsKt;
import com.lytefast.flexinput.R;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int layoutId = Utils.getResId("widget_user_profile_adapter_item_server", "layout");

    private final List<Guild> guilds;
    private final Modal modal;

    public Adapter(Modal modal, List<Guild> guilds) {
        this.guilds = guilds;
        this.modal = modal;
    }

    @Override
    public int getItemCount() {
        return guilds.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var layout = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(this, (ViewGroup) layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var guild = guilds.get(position);

        if (guild.getIcon() != null) {
            var color = holder.icon.getContext().getColor(R.c.primary_dark_600);
            SimpleDraweeViewExtensionsKt.setGuildIcon(holder.icon, false, guild, 0, null, color, null, null, true, null);
            holder.iconText.setVisibility(View.GONE);
        } else {
            holder.icon.setVisibility(View.GONE);
            holder.iconText.setText(guild.getShortName());
        }

        holder.name.setText(guild.getName());
    }

    public void onClick(int position) {
        var guild = guilds.get(position);
        modal.clone(guild);
    }
}
