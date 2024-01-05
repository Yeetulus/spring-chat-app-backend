package com.osu.swi2.rabbitchatapp.jwt;

import com.osu.swi2.rabbitchatapp.user.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;

public interface JwtService {

    String extractUsername(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    String generateToken(UserDetails userDetails);
    String generateToken(Map<String, Object> extraClaims,
                         UserDetails userDetails);
    String generateRefreshToken(UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails userDetails);
    boolean isTokenValid(String token);
    void saveUserToken(User user, String jwtToken);
    void revokeAllUserTokens(User user);
    String getTokenFromRequest(HttpServletRequest request);

}
