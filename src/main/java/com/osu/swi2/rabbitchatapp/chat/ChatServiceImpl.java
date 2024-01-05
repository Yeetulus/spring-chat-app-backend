package com.osu.swi2.rabbitchatapp.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osu.swi2.rabbitchatapp.user.User;
import com.osu.swi2.rabbitchatapp.user.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserQueueRepository userQueueRepository;

    private final SimpMessagingTemplate simpTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    public void sendMessage(User user, ChatMessage message) {
        message.setSenderId(user.getId());
        var chatRoom = checkUserMessage(message, user);
        LOGGER.info(String.format("User %s sends a message to chat with ID %s", user.getEmail(), message.getChatId()));
        rabbitTemplate.convertAndSend(chatRoom.getExchange(), chatRoom.getExchange(), message);
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
    public ChatRoom checkUserMessage(ChatMessage message, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(message.getChatId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found"));

        if(chatRoom.getUserQueues().stream().noneMatch(uq -> uq.getUser().getId().equals(user.getId())))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not member of this chat");

        return chatRoom;
    }

    @Override
    public void checkChatOwnership(User user, ChatRoom chat) {
        if(!chat.getOwner().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    String.format("User %s does not own chat with ID %s", user.getEmail(), chat.getId()));
    }

    @Override
    public ChatDTO createChat(User creator, String chatName) {

        Exchange newExchange = ExchangeBuilder.fanoutExchange(UUID.randomUUID().toString()).durable(true).build();

        try {
            rabbitAdmin.declareExchange(newExchange);
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "New chat could not be created");
        }

        ChatRoom newChat = ChatRoom.builder()
                .exchange(newExchange.getName())
                .chatName(chatName)
                .owner(creator)
                .userQueues(new HashSet<>())
                .build();
        newChat = chatRoomRepository.save(newChat);

        newChat = addUserToChat(newChat, creator);
        return createChatDTO(newChat, creator);
    }

    @Override
    public ChatRoom addUserToChat(ChatRoom chat, User user) {

        Queue queue = QueueBuilder.durable().build();
        String queueName = rabbitAdmin.declareQueue(queue);
        if(queueName == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create queue");

        Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, chat.getExchange(), chat.getExchange(), null);
        rabbitAdmin.declareBinding(binding);

        var userQueue = UserQueue.builder()
                .queue(queueName)
                .user(user)
                .build();
        chat.getUserQueues().add(userQueue);
        userQueueRepository.save(userQueue);

        var dto = createChatDTO(chat, user);
        simpTemplate.convertAndSend(String.format("/chat/exchange/%s", chat.getExchange()), user);
        simpTemplate.convertAndSend(String.format("/chat/user/%s", userQueue.getUser().getId()), dto);

        return chatRoomRepository.save(chat);
    }

    private static ChatDTO createChatDTO(ChatRoom chat, User user) {
        return ChatDTO.builder()
                .chat(chat)
                .queues(chat.getUserQueues().stream().filter(p -> p.getUser().getId().equals(user.getId()))
                        .map(UserQueue::getQueue).toList())
                .build();
    }

    @Override
    public void removeUserFromChat(User requestingUser, Long chatId, Long userId) {
        User userToRemove = userService.getById(userId);
        ChatRoom chat = getChat(chatId);
        checkChatOwnership(requestingUser, chat);

        UserQueue userQueue = chat.getUserQueues().stream()
                .filter(uq -> uq.getUser().getId().equals(userToRemove.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User not found in chat (ID): %s", chatId)));

        LOGGER.info(String.format("Deleting queue %s", userQueue.getQueue()));
        rabbitAdmin.deleteQueue(userQueue.getQueue());

        chat.getUserQueues().remove(userQueue);
        userQueueRepository.delete(userQueue);

        if(userToRemove.getId().equals(requestingUser.getId())) setNewOwner(chat);
        handleUserRemoval(chat);
        notifyRemovedUser(userToRemove,chat);
    }

    private void notifyRemovedUser(User userToRemove, ChatRoom chat) {
        Long destinationId = userToRemove.getId();
        userToRemove.setId(-1L);
        chat.setId(-1L);
        ChatDTO dto = ChatDTO.builder().chat(chat).build();
        simpTemplate.convertAndSend(String.format("/chat/user/%s", destinationId), dto);
        simpTemplate.convertAndSend(String.format("/chat/exchange/%s", chat.getExchange()), userToRemove);
    }
    private void setNewOwner(ChatRoom chat) {
        var newOwner = chat.getUserQueues().stream().findAny();
        newOwner.ifPresent(userQueue -> chat.setOwner(userQueue.getUser()));
    }
    private void handleUserRemoval(ChatRoom chat){
        if(chat.getUserQueues().isEmpty()){
            LOGGER.info(String.format("Last user from exchange %s removed, deleting exchange", chat.getExchange()));
            rabbitAdmin.deleteExchange(chat.getExchange());
            chatRoomRepository.delete(chat);
        }
        else{
            chatRoomRepository.save(chat);
        }
    }

    @Override
    public User addNewUser(User owner, Long chatId, String email) {
        var chat = getChat(chatId);
        checkChatOwnership(owner, chat);

        var toAdd = userService.getByEmail(email);
        addUserToChat(chat, toAdd);
        return toAdd;
    }

    @Override
    public void createConsumer(String queue) {
        rabbitTemplate.execute(channel -> {
            LOGGER.info(String.format("Creating consumer for queue: %s", queue));
            channel.basicConsume(queue, true, (consumerTag, delivery) -> {
                ObjectMapper om = new ObjectMapper();
                ChatMessage msg = om.readValue(delivery.getBody(), ChatMessage.class);
                if(msg != null){
                    LOGGER.info(String.format("Received message from sender with ID: %s", msg.getSenderId()));
                    simpTemplate.convertAndSend(String.format("/chat/queue/%s", queue), msg);
                }
                else{
                    LOGGER.warn(String.format("Consumer tag: %s, Cannot parse message from delivery", consumerTag));
                }
            }, consumerTag -> {});

            return null;
        });
    }

    @Override
    public ChatRoom getChat(Long chatId) {
        return chatRoomRepository.findById(chatId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Chat with ID %s was not found", chatId)));
    }

}
