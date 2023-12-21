package com.osu.swi2.rabbitchatapp.chat;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private Long chatId;
    private Long senderId;
    private String content;

}
