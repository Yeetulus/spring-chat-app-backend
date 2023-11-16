package com.osu.swi2.rabbitchatapp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.osu.swi2.rabbitchatapp.chat.ChatRoom;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;

    @NotBlank
    @JsonIgnore
    private String password;

    @ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
    private Set<ChatRoom> chats;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }
    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
