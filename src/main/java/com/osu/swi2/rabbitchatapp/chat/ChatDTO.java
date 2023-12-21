package com.osu.swi2.rabbitchatapp.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatDTO {

    private ChatRoom chat;
    private List<String> queues;
}
