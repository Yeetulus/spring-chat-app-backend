package com.osu.swi2.rabbitchatapp.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SubscriptionController {

    private final ChatService chatService;
    @SubscribeMapping("/queue/{queue}")
    public void createConsumer(@DestinationVariable("queue") String queue){
        chatService.createConsumer(queue);
    }



}
