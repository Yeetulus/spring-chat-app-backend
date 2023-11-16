package com.osu.swi2.rabbitchatapp.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{

    private final AmqpTemplate template;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Override
    public void sendMessage(ChatMessage message) {
        template.convertAndSend(exchange, routingKey, message.getContent());
    }
}
