package com.example.ppps.service;

import com.example.ppps.controller.AuthenticationResponse;
import com.example.ppps.entity.User; // Using the entity from your latest definition
import com.example.ppps.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID; // Not strictly needed for user ID now, but kept for context

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthenticationResponse authenticate(String userId, String pin) {
        // FIX 1: User ID is now treated as a String, matching the new User entity definition.
        // We use findById directly with the String userId.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        // FIX 2: Corrected getter from getPinHash() to getHashedPin()
        if (!passwordEncoder.matches(pin, user.getHashedPin())) {
            throw new SecurityException("Invalid PIN");
        }

        // --- JWT Signing (Kept the fix for Base64 URL issue) ---
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key) // Use the SecretKey object (modern JJWT API)
                .compact();
        // --------------------------------------------------------

        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        return response;
    }
}