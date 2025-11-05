// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.controller.PinResetRequest;
import com.example.ppps.controller.RegistrationRequest;
import com.example.ppps.controller.RegistrationResponse;
import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RegistrationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public RegistrationResponse registerUser(@Valid RegistrationRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalStateException("Phone number already registered");
        }
        String hashedPin = passwordEncoder.encode(request.getPin());

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setHashedPin(hashedPin);

        Wallet wallet = new Wallet();
        wallet.setUserId(UUID.fromString(user.getUserId()));
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("NGN");
        wallet = walletRepository.save(wallet);

        user.setWallet(wallet);
        userRepository.save(user);

        RegistrationResponse response = new RegistrationResponse();
        response.setUserId(user.getUserId());
        response.setWalletId(wallet.getId());
        return response;
    }

    @Transactional
    public void resetPin(String userId, @Valid PinResetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(request.getOldPin(), user.getHashedPin())) {
            throw new SecurityException("Invalid old PIN");
        }

        String newHashedPin = passwordEncoder.encode(request.getNewPin());
        user.setHashedPin(newHashedPin);
        userRepository.save(user);
    }
}