package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{

    private final RabbitTemplate template;
    private final RabbitAdmin admin;
    private final ChatRoomRepository chatRoomRepository;
    private final UserQueueRepository userQueueRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    public void sendMessage(User user, ChatMessage message) {
        var chatRoom = checkUserMessage(message, user);
        LOGGER.info(String.format("User %s sends a message to chat with ID %s", user.getEmail(), message.getChatId()));
        template.convertAndSend(chatRoom.getExchange(), chatRoom.getExchange(), message.getContent());
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
}