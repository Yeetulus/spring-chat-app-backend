package com.osu.swi2.rabbitchatapp.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private String content;
    private String sender;

}
