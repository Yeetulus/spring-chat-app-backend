package com.osu.swi2.rabbitchatapp.controller;

import com.osu.swi2.rabbitchatapp.model.ChatMessage;
import com.osu.swi2.rabbitchatapp.service.MessageProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {
    @Autowired
    private MessageProducerService messageProducerService;

    @MessageMapping("/chat/sendMessage")
    @SendTo("/topic/rabbitmqchat")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        messageProducerService.sendMessage(chatMessage.getContent());
        return chatMessage;
    }
    @MessageMapping("/chat/newUser")
    @SendTo("/topic/rabbitmqchat")
    public ChatMessage newUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }
}