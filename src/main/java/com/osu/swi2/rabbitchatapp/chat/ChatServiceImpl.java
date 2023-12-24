package com.osu.swi2.rabbitchatapp.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osu.swi2.rabbitchatapp.user.User;
import com.rabbitmq.client.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.Charset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{

    private final RabbitTemplate template;
    private final RabbitAdmin admin;

    private final ChatRoomRepository chatRoomRepository;
    private final UserQueueRepository userQueueRepository;

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final SimpMessagingTemplate simpTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    public void sendMessage(User user, ChatMessage message) {
        var chatRoom = checkUserMessage(message, user);
        LOGGER.info(String.format("User %s sends a message to chat with ID %s", user.getEmail(), message.getChatId()));
        template.convertAndSend(chatRoom.getExchange(), chatRoom.getExchange(), message);
    }

    @Override
    public List<String> getAllUserQueues(User user) {
        return userQueueRepository.findQueuesByUser(user);
    }

    @Override
    public List<ChatDTO> getAllUserChatRooms(User user) {
        var chatRooms = chatRoomRepository.findAllByUserQueues_User(user);

        List<ChatDTO> chatDTOS = new ArrayList<>();
        chatRooms.forEach(c ->{
            var dto = ChatDTO.builder()
                    .chat(c)
                    .queues(c.getUserQueues().stream().filter(p -> p.getUser().getId().equals(user.getId()))
                            .map(UserQueue::getQueue).toList())
                    .build();
            chatDTOS.add(dto);
        });

        return chatDTOS;
    }

    @Override
    public ChatRoom getChatByMessage(ChatMessage message) {
        return chatRoomRepository.findById(message.getChatId()).orElseThrow(() ->
                new RuntimeException("Chat not found"));
    }

    @Override
    public ChatRoom checkUserMessage(ChatMessage message, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(message.getChatId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found"));

        if(chatRoom.getUserQueues().stream().noneMatch(uq -> uq.getUser().getId().equals(user.getId())))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not member of this chat");

        return chatRoom;
    }

    @Override
    public ChatRoom createChat(User creator, String chatName) {

        Exchange newExchange = ExchangeBuilder.fanoutExchange(UUID.randomUUID().toString()).durable(true).build();

        try {
            admin.declareExchange(newExchange);
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "New chat could not be created");
        }

        ChatRoom newChat = ChatRoom.builder()
                .exchange(newExchange.getName())
                .chatName(chatName)
                .owner(creator)
                .userQueues(new HashSet<>())
                .build();

        return addUserToChat(newChat, creator);
    }

    @Override
    public void deleteChat(User requestingUser, Long chatId) {
        var toDelete = chatRoomRepository.findById(chatId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Chat with id %s not found", chatId)));

        if(!toDelete.getOwner().getId().equals(requestingUser.getId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    String.format("Chat not owned by the user with ID %s", requestingUser.getId()));

        chatRoomRepository.delete(toDelete);
    }

    @Override
    public ChatRoom addUserToChat(ChatRoom chat, User user) {

        Queue queue = QueueBuilder.durable().build();
        String queueName = admin.declareQueue(queue);
        if(queueName == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create queue");

        Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, chat.getExchange(), chat.getExchange(), null);
        admin.declareBinding(binding);

        var userQueue = UserQueue.builder()
                .queue(queueName)
                .user(user)
                .build();
        chat.getUserQueues().add(userQueue);
        userQueueRepository.save(userQueue);

        return chatRoomRepository.save(chat);
    }

    @Override
    public void addNewQueue(String queueName, String exchangeName, String routingKey) {
        Queue queue = QueueBuilder.durable().build();
        Binding binding = new Binding(
                queueName,
                Binding.DestinationType.QUEUE,
                exchangeName,
                routingKey,
                null
        );
        admin.declareQueue(queue);
        admin.declareBinding(binding);
        addQueueToListener(exchangeName,queueName);
    }

    @Override
    public void addQueueToListener(String listenerId, String queueName) {
        LOGGER.info(String.format("adding queue : %s to listener with id : %s", queueName, listenerId));
        if (!checkQueueExistOnListener(listenerId,queueName)) {
            getMessageListenerContainerById(listenerId).addQueueNames(queueName);
            LOGGER.info("queue ");
        } else {
            LOGGER.info(String.format("given queue name : %s not exist on given listener id : %s", queueName, listenerId));
        }
    }

    @Override
    public void removeQueueFromListener(String listenerId, String queueName) {
        LOGGER.info(String.format("removing queue : %s from listener : %s", queueName, listenerId));
        if (checkQueueExistOnListener(listenerId,queueName)) {
            getMessageListenerContainerById(listenerId).removeQueueNames(queueName);
            LOGGER.info("deleting queue from rabbit management");
            admin.deleteQueue(queueName);
        } else {
            LOGGER.info(String.format("given queue name : %s not exist on given listener id : %s", queueName, listenerId));
        }
    }

    @Override
    public Boolean checkQueueExistOnListener(String listenerId, String queueName) {
        try {
            LOGGER.info("checking queueName : " + queueName + " exist on listener id : " + listenerId);
            LOGGER.info("getting queueNames");
            String[] queueNames = getMessageListenerContainerById(listenerId).getQueueNames();
            LOGGER.info("checking " + queueName + " exist on active queues");
            for (String name : queueNames) {
                LOGGER.info("name : " + name + " with checking name : " + queueName);
                if (name.equals(queueName)) {
                    LOGGER.info("queue name exist on listener, returning true");
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        } catch (Exception e) {
            LOGGER.error("Error on checking queue exist on listener");
            LOGGER.error("error message : " + e.getMessage());
            LOGGER.error("trace : " + Arrays.toString(e.getStackTrace()));
            return Boolean.FALSE;
        }
    }

    @Override
    public void createConsumer(String queue) {
        template.execute(channel -> {
            LOGGER.info(String.format("Creating consumer for queue: %s", queue));
            channel.basicConsume(queue, true, (consumerTag, delivery) -> {
                ObjectMapper om = new ObjectMapper();
                ChatMessage msg = om.readValue(delivery.getBody(), ChatMessage.class);
                if(msg != null){
                    LOGGER.info(String.format("Received message from sender with ID: %s", msg.getSenderId()));
                    simpTemplate.convertAndSend(String.format("/chat/%s", queue), msg);
                }
                else{
                    LOGGER.warn(String.format("Consumer tag: %s, Cannot parse message from delivery", consumerTag));
                }
            }, consumerTag -> {});
            return null;
        });
    }

    private AbstractMessageListenerContainer getMessageListenerContainerById(String listenerId) {
        LOGGER.info(String.format("getting message listener container by id : %s", listenerId));
        return ((AbstractMessageListenerContainer) rabbitListenerEndpointRegistry
                .getListenerContainer(listenerId));
    }

}
