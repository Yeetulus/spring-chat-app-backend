package com.osu.swi2.rabbitchatapp.chat;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {

    SimpMessagingTemplate template;
    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessage message) {
        chatService.sendMessage(message);
        //template.convertAndSend("/topic/message", message);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @MessageMapping("/sendMessage")
    public void receiveMessage(@Payload ChatMessage message) {
        chatService.sendMessage(message);
    }
    @SendTo("/topic/message")
    public ChatMessage broadcastMessage(@Payload ChatMessage message) {
        return message;
    }
}
