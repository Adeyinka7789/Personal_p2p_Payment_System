package com.example.ppps.service;

import com.example.ppps.controller.AuthenticationResponse;
import com.example.ppps.entity.User;
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
import java.util.Optional; // Added import for Optional

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

    /**
     * Authenticates a user using their phone number and PIN, and generates a JWT.
     * * @param phoneNumber The user's registered phone number.
     * @param pin The user's secure PIN.
     * @return AuthenticationResponse containing the JWT.
     */
    // FIX 1: Change method signature to accept phoneNumber instead of userId
    public AuthenticationResponse authenticate(String phoneNumber, String pin) {

        // FIX 2: Look up the User by phoneNumber (using the correct method from the repository)
        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);

        User user = userOptional.map(u -> (User) u) // Cast Object back to User, required due to the repository signature
                .orElseThrow(() -> new SecurityException("User not found: " + phoneNumber));

        // PIN Verification
        if (!passwordEncoder.matches(pin, user.getHashedPin())) {
            throw new SecurityException("Invalid PIN for user: " + phoneNumber);
        }

        // --- JWT Signing ---
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                // FIX 3: Set the JWT subject to the actual unique identifier (userId)
                .setSubject(user.getUserId())
                .claim("phoneNumber", user.getPhoneNumber()) // Adding phone number as a useful claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
        // -------------------

        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        return response;
    }
}