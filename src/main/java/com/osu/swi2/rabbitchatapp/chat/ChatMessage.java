package com.osu.swi2.rabbitchatapp.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String type;
    private Long chatId;
    private Long senderId;
    private String content;
}
