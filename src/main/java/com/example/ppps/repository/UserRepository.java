package com.example.ppps.repository;

import com.example.ppps.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findById(String userId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByEmail(String email); // Add email lookup
    boolean existsByEmail(String email); // Check if email exists
}