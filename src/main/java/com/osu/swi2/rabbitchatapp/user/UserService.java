package com.osu.swi2.rabbitchatapp.user;

import com.osu.swi2.rabbitchatapp.auth.AuthRequest;
import com.osu.swi2.rabbitchatapp.auth.AuthResponse;
import com.osu.swi2.rabbitchatapp.auth.RegistrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface UserService {

    User getByEmail(String email);

    AuthResponse registerUser(RegistrationRequest request);
    AuthResponse authenticate(AuthRequest request);
    void refreshToken(HttpServletRequest request,
                      HttpServletResponse response) throws IOException;
}
