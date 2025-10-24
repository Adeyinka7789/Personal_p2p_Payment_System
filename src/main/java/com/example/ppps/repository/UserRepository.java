package com.example.ppps.repository;

import com.example.ppps.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findById(String userId);

    // FIX APPLIED HERE: Changed Optional<Object> to Optional<User>
    Optional<User> findByPhoneNumber(String phoneNumber);
}
