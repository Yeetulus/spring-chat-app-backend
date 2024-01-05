package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;
import com.osu.swi2.rabbitchatapp.user.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ChatRoomRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserQueueRepository userQueueRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Test
    void findAllByUserQueues_User() {
        User user1 = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .build();
        user1 = userRepository.save(user1);

        User user2 = User.builder()
                .firstName("Pavel")
                .lastName("Novák")
                .email("pavel.novak@mail.com")
                .password("password")
                .build();
        user2 = userRepository.save(user2);

        ChatRoom chat1 = ChatRoom.builder()
            .chatName("chat1")
            .owner(user1)
            .exchange("exchange1")
            .build();
        chat1 = chatRoomRepository.save(chat1);

        HashSet<UserQueue> queues1 = new HashSet<>();
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                .queue("queue1")
                .user(user1).build()));
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                .queue("queue2")
                .user(user1).build()));
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                .queue("queue3")
                .user(user2).build()));
        chat1.setUserQueues(queues1);
        chat1 = chatRoomRepository.save(chat1);

        ChatRoom chat2 = ChatRoom.builder()
                .chatName("chat2")
                .owner(user2)
                .exchange("exchange2")
                .build();
        chat2 = chatRoomRepository.save(chat2);

        HashSet<UserQueue> queues2 = new HashSet<>();
        queues2.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue4")
                        .user(user1).build()));
        queues2.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue5")
                        .user(user2).build()));
        queues2.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue6")
                        .user(user2).build()));
        chat2.setUserQueues(queues2);
        chat2 = chatRoomRepository.save(chat2);

        ChatRoom chat3 = ChatRoom.builder()
                .chatName("chat3")
                .owner(user2)
                .exchange("exchange3")
                .build();
        chat3 = chatRoomRepository.save(chat3);

        HashSet<UserQueue> queues3 = new HashSet<>();
        queues3.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue7")
                        .user(user2).build()));
        queues3.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue8")
                        .user(user2).build()));
        queues3.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue9")
                        .user(user2).build()));
        chat3.setUserQueues(queues3);
        chat3 = chatRoomRepository.save(chat3);

        var chats1 = chatRoomRepository.findAllByUserQueues_User(user1);
        var chats2 = chatRoomRepository.findAllByUserQueues_User(user2);

        Assertions.assertThat(chats1).size().isEqualTo(2);
        Assertions.assertThat(chats2).size().isEqualTo(3);
    }

    @Test
    void findById() {
        String expected = "exchange1";
        User user1 = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("jan.novak@mail.com")
                .password("password")
                .build();
        user1 = userRepository.save(user1);

        ChatRoom chat1 = ChatRoom.builder()
                .chatName("chat1")
                .owner(user1)
                .exchange("exchange1")
                .build();
        chat1 = chatRoomRepository.save(chat1);

        HashSet<UserQueue> queues1 = new HashSet<>();
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue1")
                        .user(user1).build()));
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue2")
                        .user(user1).build()));
        queues1.add(userQueueRepository.save(
                UserQueue.builder()
                        .queue("queue3")
                        .user(user1).build()));
        chat1.setUserQueues(queues1);
        chat1 = chatRoomRepository.save(chat1);

        var foundChat = chatRoomRepository.findById(chat1.getId());
        if(foundChat.isEmpty()) fail();

        Assertions.assertThat(foundChat.get().getExchange()).isEqualTo(expected);
        Assertions.assertThat(foundChat.get().getId()).isEqualTo(chat1.getId());
    }
}