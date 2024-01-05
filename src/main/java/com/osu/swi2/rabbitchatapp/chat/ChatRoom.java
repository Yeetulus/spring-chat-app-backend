package com.osu.swi2.rabbitchatapp.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.osu.swi2.rabbitchatapp.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String exchange;
    @NotBlank
    private String chatName;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("queue")
    private Set<UserQueue> userQueues = new HashSet<>();
}
