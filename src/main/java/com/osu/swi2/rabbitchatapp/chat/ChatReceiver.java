package com.osu.swi2.rabbitchatapp.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Getter
@Setter
public class ChatReceiver implements MessageListener {

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatReceiver.class);

    public void receiveMessage(Message message) {

        LOGGER.info("Received Rabbit MQ message: " + message.toString());
        try {
            var chatMessage = objectMapper.readValue(message.getBody(), ChatMessage.class);
            var chatRoom = chatService.getChatByMessage(chatMessage);

            Optional<UserQueue> optionalUQ = chatRoom.getUserQueues().stream()
                    .filter(userQueue -> chatMessage.getSenderId().equals(userQueue.getUser().getId()))
                    .findFirst();
            if(optionalUQ.isEmpty()) throw new RuntimeException("User queue not found");

            var uq = optionalUQ.get();
            var destination = String.format("/%s/%s", chatRoom.getExchange(), uq.getQueue());
            LOGGER.info(String.format("Sending message to: %s", destination));
            template.convertAndSend(destination, message.getBody());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessage(Message message) {
        receiveMessage(message);
    }
}