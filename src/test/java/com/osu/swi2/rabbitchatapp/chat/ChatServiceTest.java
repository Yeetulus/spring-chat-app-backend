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
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
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

        when(chatRoomRepository.findById(Mockito.any(Long.class))).thenReturn(Optional.of(chat));
        willThrow(new RuntimeException("Test"))
                .given(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), Mockito.anyString(), Mockito.any(ChatMessage.class));

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
        User user1 = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .id(1L)
                .build();

        User user2 = User.builder()
                .firstName("Pavel")
                .lastName("Novák")
                .email("pavel.novak@mail.com")
                .password("password")
                .id(2L)
                .build();

        List<UserQueue> queues = new ArrayList<>();
        queues.add(UserQueue.builder()
                .id(1L)
                .user(user1)
                .queue("q1")
                .build());
        queues.add(UserQueue.builder()
                .id(2L)
                .user(user1)
                .queue("q2")
                .build());
        queues.add(UserQueue.builder()
                .id(3L)
                .user(user2)
                .queue("q3")
                .build());

        when(userQueueRepository.findQueuesByUser(user1)).thenReturn(queues.stream()
                .filter(uq -> uq.getUser().getId().equals(user1.getId()))
                .map(UserQueue::getQueue)
                .collect(Collectors.toList()));
        List<String> queueStrings = userQueueRepository.findQueuesByUser(user1);
        Assertions.assertThat(queueStrings.size()).isEqualTo(2L);
    }

    @Test
    void getAllUserChatRooms() {
        User user1 = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .id(1L)
                .build();

        List<ChatRoom> chatRooms = new ArrayList<>();
        ChatRoom chatRoom1 = ChatRoom.builder()
                .chatName("c")
                .exchange("e1")
                .userQueues(Set.of(UserQueue.builder().user(user1).queue("q1").id(1L).build()))
                .id(1L)
                .owner(user1)
                .build();
        chatRooms.add(chatRoom1);

        ChatRoom chatRoom2 = ChatRoom.builder()
                .chatName("c")
                .exchange("e2")
                .userQueues(Set.of(UserQueue.builder().user(user1).queue("q2").id(2L).build()))
                .id(2L)
                .owner(user1)
                .build();
        chatRooms.add(chatRoom2);

        ChatRoom chatRoom3 = ChatRoom.builder()
                .chatName("c")
                .exchange("e3")
                .userQueues(Set.of(UserQueue.builder().user(user1).queue("q3").id(3L).build()))
                .id(3L)
                .owner(user1)
                .build();
        chatRooms.add(chatRoom3);

        when(chatRoomRepository.findAllByUserQueues_User(user1))
                .thenReturn(chatRooms.stream()
                .filter(c -> c.getUserQueues().stream()
                .anyMatch(uq -> uq.getUser().getId().equals(user1.getId())))
                .collect(Collectors.toList()));

        List<ChatRoom> result = chatRoomRepository.findAllByUserQueues_User(user1);

        Assertions.assertThat(result.size()).isEqualTo(3);
    }

    @Test
    void createChat() {
        User user1 = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .id(1L)
                .build();

        String chatName = "chat";

        UserQueue uq = UserQueue.builder()
                .id(1L)
                .user(user1)
                .queue("queue")
                .build();

        doNothing().when(rabbitAdmin).declareExchange(Mockito.any(Exchange.class));
        doNothing().when(rabbitAdmin).declareBinding(Mockito.any(Binding.class));
        doNothing().when(simpTemplate).convertAndSend(Mockito.anyString(), Mockito.any(User.class));
        doNothing().when(simpTemplate).convertAndSend(Mockito.anyString(), Mockito.any(ChatDTO.class));
        when(rabbitAdmin.declareQueue(Mockito.any(Queue.class))).thenReturn("queue");
        when(chatRoomRepository.save(Mockito.any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom savedChatRoom = invocation.getArgument(0);
            Set<UserQueue> modifiedUserQueues = new HashSet<>(savedChatRoom.getUserQueues());
            var userQueue = UserQueue.builder()
                    .id(1L)
                    .user(user1)
                    .queue("queue")
                    .build();
            modifiedUserQueues.add(userQueue);
            savedChatRoom.setUserQueues(modifiedUserQueues);
            return savedChatRoom;
        });

        when(userQueueRepository.save(Mockito.any(UserQueue.class))).thenReturn(uq);

        ChatDTO result = chatService.createChat(user1, chatName);
        Assertions.assertThat(result.getChat().getChatName()).isEqualTo("chat");

    }

    @Test
    void addUserToChat() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@mail.com")
                .password("password")
                .id(1L)
                .build();

        ChatRoom chat = ChatRoom.builder()
                .exchange("exchange")
                .chatName("chat")
                .owner(user)
                .userQueues(new HashSet<>())
                .build();

        when(rabbitAdmin.declareQueue(Mockito.any(Queue.class))).thenReturn("queueName");
        doNothing().when(rabbitAdmin).declareBinding(Mockito.any(Binding.class));
        doNothing().when(simpTemplate).convertAndSend(Mockito.anyString(), Mockito.any(Object.class));

        chat.setUserQueues(new HashSet<>());

        doAnswer(invocation -> {
            UserQueue userQueue = UserQueue.builder()
                    .queue("queueName")
                    .user(user)
                    .build();
            chat.getUserQueues().add(userQueue);

            return chat;
        }).when(chatRoomRepository).save(Mockito.any(ChatRoom.class));

        ChatRoom _chat = chatService.addUserToChat(chat, user);

        Assertions.assertThat(_chat.getChatName()).isEqualTo("chat");
    }

}