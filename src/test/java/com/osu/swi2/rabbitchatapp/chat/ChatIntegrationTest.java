package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.RabbitChatAppApplication;
import com.osu.swi2.rabbitchatapp.user.User;
import com.osu.swi2.rabbitchatapp.user.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = RabbitChatAppApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2, replace = AutoConfigureTestDatabase.Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChatIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQueueRepository userQueueRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MockBean
    private ChatServiceImpl chatService;

    @Test
    void createChat() {

        User owner = User.builder()
                .firstName("Jan")
                .lastName("Novák")
                .email("mail@mail.com")
                .password("password")
                .build();
        owner = userRepository.save(owner);

        ChatRoom chat = ChatRoom.builder()
                .chatName("chat")
                .exchange("exchange")
                .userQueues(new HashSet<>())
                .owner(owner)
                .build();
        chat = chatRoomRepository.save(chat);

        RabbitAdmin rabbitAdminMock = mock(RabbitAdmin.class);
        SimpMessagingTemplate simpTemplateMock = mock(SimpMessagingTemplate.class);

        when(rabbitAdminMock.declareQueue(Mockito.any(Queue.class))).thenReturn("queue");
        doNothing().when(rabbitAdminMock).declareBinding(Mockito.any(Binding.class));
        doNothing().when(simpTemplateMock).convertAndSend(Mockito.anyString(), Mockito.any(User.class));
        doNothing().when(simpTemplateMock).convertAndSend(Mockito.anyString(), Mockito.any(ChatDTO.class));

        try {
            ReflectionTestUtils.setField(chatService, "rabbitAdmin", rabbitAdminMock);
            ReflectionTestUtils.setField(chatService, "simpTemplate", simpTemplateMock);

            mvc.perform(post("/api/chat/add")
                            .requestAttr("jwtUser", owner)
                            .param("chatId", chat.getId().toString())
                            .param("email", owner.getEmail())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            fail();
        }
    }
}