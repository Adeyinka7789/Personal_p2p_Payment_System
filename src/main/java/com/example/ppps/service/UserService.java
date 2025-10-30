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

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void deleteById(String userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public void resetPin(String userId, String currentPin, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kindly verify current PIN
        if (!passwordEncoder.matches(currentPin, user.getHashedPin())) {
            throw new RuntimeException("Current PIN is incorrect");
        }

        // hey! Hash and set new PIN
        user.setHashedPin(passwordEncoder.encode(newPin));
        userRepository.save(user);
    }
}