package com.osu.swi2.rabbitchatapp.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatConsumer implements MessageListener {

    private final SimpMessagingTemplate template;

    @Override
    public void onMessage(Message message) {
        template.convertAndSend("/topic/message", new String(message.getBody()));
    }
}
