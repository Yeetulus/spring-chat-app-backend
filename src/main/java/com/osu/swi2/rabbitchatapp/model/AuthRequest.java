package com.osu.swi2.rabbitchatapp.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AuthRequest {

    @NotNull
    private String email;
    @NotNull
    private String password;
}
