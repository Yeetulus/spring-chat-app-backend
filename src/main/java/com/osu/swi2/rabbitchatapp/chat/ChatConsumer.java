package com.osu.swi2.rabbitchatapp.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatConsumer {

    private final SimpMessagingTemplate template;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatConsumer.class);

    @RabbitListener()
    public void receiveMessage(Message message) {
        LOGGER.info("Received Rabbit MQ message: " + message.toString());
        try {
            var destination = String.format("/%s/%s", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getConsumerQueue());
            LOGGER.info(String.format("Sending message to: %s", destination));
            template.convertAndSend(destination, message.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
