package com.example.ppps.controller;

import lombok.Data;

@Data
public class AuthenticationResponse {
    private String token;
    private String userId;
}