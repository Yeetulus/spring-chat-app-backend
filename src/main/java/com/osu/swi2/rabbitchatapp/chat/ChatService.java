package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;

import java.util.List;

public interface ChatService {

    void sendMessage(User user, ChatMessage message);
    List<String> getAllUserQueues(User user);
    List<ChatDTO> getAllUserChatRooms(User user);
    ChatDTO createChat(User creator, String chatName);
    ChatRoom addUserToChat(ChatRoom chat, User user);
    void removeUserFromChat(User requestingUser, Long chatId, Long userId);
    User addNewUser(User owner, Long chatId, String email);
    ChatRoom checkUserMessage(ChatMessage message, User user);
    void checkChatOwnership(User user, ChatRoom chat);
    void createConsumer(String queue);
    ChatRoom getChat(Long chatId);
}
