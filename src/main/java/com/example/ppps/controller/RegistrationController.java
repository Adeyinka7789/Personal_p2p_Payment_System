package com.example.ppps.controller;

import com.example.ppps.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/reset-pin/{userId}")
    public ResponseEntity<Void> resetPin(@PathVariable String userId, @Valid @RequestBody PinResetRequest request) {
        registrationService.resetPin(userId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}