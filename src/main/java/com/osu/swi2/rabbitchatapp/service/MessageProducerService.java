package com.osu.swi2.rabbitchatapp.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageProducerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(routingKey, message);
    }
}