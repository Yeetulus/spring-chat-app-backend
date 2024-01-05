package com.osu.swi2.rabbitchatapp.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osu.swi2.rabbitchatapp.auth.AuthRequest;
import com.osu.swi2.rabbitchatapp.auth.AuthResponse;
import com.osu.swi2.rabbitchatapp.auth.RegistrationRequest;
import com.osu.swi2.rabbitchatapp.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public User getUserFromRequest(HttpServletRequest request) {
        User user = (User)request.getAttribute("jwtUser");
        if(user == null) throw new AuthorizationServiceException("Could not parse user from JWT token");
        return user;
    }

    public AuthResponse registerUser(RegistrationRequest request){
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if(existing.isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("User with email %s already exists", request.getEmail()));
        }

        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        User savedUser = userRepository.save(newUser);
        var token = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);
        jwtService.saveUserToken(savedUser, token);
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .build();

    }
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        jwtService.revokeAllUserTokens(user);
        jwtService.saveUserToken(user, jwtToken);

        LOGGER.info(String.format("Created access token: %s", jwtToken));
        LOGGER.info(String.format("Created refresh token: %s", refreshToken));

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                jwtService.revokeAllUserTokens(user);
                jwtService.saveUserToken(user, accessToken);
                var authResponse = AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Cannot find user with email: %s", email)));
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Cannot find user with id: %s", id)));
    }
}
