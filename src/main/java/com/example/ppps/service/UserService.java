package com.example.ppps.service;

import com.example.ppps.entity.User;
import com.example.ppps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find a user by ID
     */
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Find a user by phone number
     */
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Find a user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Save or update user
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Delete user by ID
     */
    public void deleteById(String userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Reset user PIN with validation
     */
    @Transactional
    public void resetPin(String userId, String currentPin, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current PIN
        if (!passwordEncoder.matches(currentPin, user.getHashedPin())) {
            throw new RuntimeException("Current PIN is incorrect");
        }

        // Hash and set new PIN
        user.setHashedPin(passwordEncoder.encode(newPin));
        userRepository.save(user);
    }
}