package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;

import java.util.List;

public interface ChatService {

    void sendMessage(User user, ChatMessage message);
    List<String> getAllUserQueues(User user);
    List<ChatDTO> getAllUserChatRooms(User user);
    ChatRoom getChatByMessage(ChatMessage message);
    ChatRoom createChat(User creator, String chatName);
    void deleteChat(User requestingUser, Long chatId);
    ChatRoom addUserToChat(ChatRoom chat, User user);
    ChatRoom checkUserMessage(ChatMessage message, User user);


    void addNewQueue(String queueName,String exchangeName,String routingKey);
    void addQueueToListener(String listenerId,String queueName);
    void removeQueueFromListener(String listenerId,String queueName);
    Boolean checkQueueExistOnListener(String listenerId,String queueName);

    void createConsumer(String queue);
}
