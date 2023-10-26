package com.osu.swi2.rabbitchatapp.service;

import com.osu.swi2.rabbitchatapp.model.ChatMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumerService {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    @RabbitListener(queues = "${rabbitmq.queue}")
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType("Chat");
        chatMessage.setContent(message);
        chatMessage.setSender("RabbitMQ");

        messagingTemplate.convertAndSend("/topic/rabbitmqchat", chatMessage);
    }
}