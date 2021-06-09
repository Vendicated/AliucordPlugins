package com.discord.models.domain;

import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.embed.MessageEmbed;
import com.discord.api.user.User;

import java.util.ArrayList;
import java.util.List;

public class ModelMessage {

    public List<MessageAttachment> getAttachments() {
        return null;
    }
    public User getAuthor() { return null; }
    public String getContent() { return null; }
    public List<MessageEmbed> getEmbeds() { return new ArrayList<>(); }

}
