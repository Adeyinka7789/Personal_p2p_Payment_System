package com.example.ppps.service;

import com.example.ppps.controller.AuthenticationResponse;
import com.example.ppps.controller.RegistrationRequest;
import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Register a new user with phone number, optional email, and PIN
     */
    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        // Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already registered");
        }

        // Check if email already exists (if provided)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already registered");
            }
        }

        // Create user first
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail() != null && !request.getEmail().trim().isEmpty() ?
                request.getEmail().trim() : null);
        user.setHashedPin(passwordEncoder.encode(request.getPin()));

        // Save user first to generate userId
        User savedUser = userRepository.save(user);

        // Create wallet for user with the userId
        Wallet wallet = new Wallet();
        // DON'T set ID manually - let Hibernate generate it
        // wallet.setId(UUID.randomUUID()); // REMOVE THIS LINE
        wallet.setUserId(UUID.fromString(savedUser.getUserId()));
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("NGN");
        // createdAt and updatedAt are automatically handled by @CreationTimestamp and @UpdateTimestamp

        // Save wallet
        Wallet savedWallet = walletRepository.save(wallet);

        // Update user with wallet reference
        savedUser.setWallet(savedWallet);
        // The wallet reference will be persisted automatically since savedUser is managed

        // Generate JWT token
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(savedUser.getUserId())
                .claim("phoneNumber", savedUser.getPhoneNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();

        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        response.setUserId(savedUser.getUserId());
        return response;
    }
    /**
     * Authenticates a user using their phone number and PIN, and generates a JWT.
     */
    public AuthenticationResponse authenticate(String phoneNumber, String pin) {
        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);

        User user = userOptional.map(u -> (User) u)
                .orElseThrow(() -> new SecurityException("User not found: " + phoneNumber));

        // PIN Verification
        if (!passwordEncoder.matches(pin, user.getHashedPin())) {
            throw new SecurityException("Invalid PIN for user: " + phoneNumber);
        }

        // Generate JWT
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(user.getUserId())
                .claim("phoneNumber", user.getPhoneNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();

        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        response.setUserId(user.getUserId());
        return response;
    }
}