package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;
import com.osu.swi2.rabbitchatapp.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(HttpServletRequest request,
            @RequestBody ChatMessage message) {
        var user = userService.getUserFromRequest(request);
        chatService.sendMessage(user, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create")
    public ResponseEntity<ChatRoom> createChat(HttpServletRequest request,
                                            @RequestParam String chatName) {
        var user = userService.getUserFromRequest(request);
        var chatRoom = chatService.createChat(user, chatName);
        return new ResponseEntity<>(chatRoom, HttpStatus.CREATED);
    }

    @GetMapping("/queues")
    public ResponseEntity<List<String>> getQueues(HttpServletRequest request) {
        var user = userService.getUserFromRequest(request);
        return ResponseEntity.ok(chatService.getAllUserQueues(user));
    }
    @GetMapping("/exchanges")
    public ResponseEntity<List<ChatDTO>> getExchanges(HttpServletRequest request) {
        var user = userService.getUserFromRequest(request);
        return ResponseEntity.ok(chatService.getAllUserChatRooms(user));
    }

    @PostMapping("/add")
    public ResponseEntity<User> addUser(HttpServletRequest request, @RequestParam Long chatId, @RequestParam String email){
        var user = userService.getUserFromRequest(request);
        return ResponseEntity.ok(chatService.addNewUser(user, chatId, email));
    }
}
