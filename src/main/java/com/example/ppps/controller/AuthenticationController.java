// REVIEW: full code review requested

package com.example.ppps.controller;

import com.example.ppps.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody AuthenticationRequest request) { // Use AuthenticationRequest
        try {
            AuthenticationResponse response =
                    authenticationService.authenticateAdmin(request.getPhoneNumber(), request.getPin());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}