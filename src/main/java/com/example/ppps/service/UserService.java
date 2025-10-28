package com.example.ppps.service;

import com.example.ppps.entity.User;
import com.example.ppps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
}
