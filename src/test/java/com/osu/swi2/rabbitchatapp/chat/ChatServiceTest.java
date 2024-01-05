package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;
import com.osu.swi2.rabbitchatapp.user.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private UserService userService;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private UserQueueRepository userQueueRepository;

    @Mock
    private SimpMessagingTemplate simpTemplate;

    @Test
    void sendMessage() {
        ChatMessage message = ChatMessage.builder()
                .chatId(1L)
                .senderId(1L)
                .content("Hello")
                .type("Text")
                .build();

        User user = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .id(1L)
                .build();

        HashSet<UserQueue> queues = new HashSet<>();
        queues.add(UserQueue.builder()
                .user(user)
                .queue("queue")
                .id(1L)
                .build());

        ChatRoom chat = ChatRoom.builder()
                .id(1L)
                .exchange("exchange")
                .chatName("chat")
                .userQueues(queues)
                .owner(user)
                .build();

        when(chatService.checkUserMessage(Mockito.any(ChatMessage.class), Mockito.any(User.class))).thenReturn(chat);
        when(chatRoomRepository.findById(Mockito.any(Long.class))).thenReturn(Optional.of(chat));
        doThrow(new RuntimeException("Test")).when(rabbitTemplate).convertAndSend(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(Objects.class));
        
        try {
            chatService.sendMessage(user, message);
        } catch (RuntimeException e){
            Assertions.assertThat(e.getMessage()).isEqualTo("Test");
        } catch (Exception e){
            fail();
        }
    }

    @Test
    void getAllUserQueues() {
    }

    @Test
    void getAllUserChatRooms() {
    }

    @Test
    void createChat() {
    }

    @Test
    void addUserToChat() {
    }

    @Test
    void removeUserFromChat() {
    }

    @Test
    void addNewUser() {
    }

    @Test
    void checkUserMessage() {
    }

    @Test
    void checkChatOwnership() {
    }

    @Test
    void createConsumer() {
    }

    @Test
    void getChat() {
    }
}