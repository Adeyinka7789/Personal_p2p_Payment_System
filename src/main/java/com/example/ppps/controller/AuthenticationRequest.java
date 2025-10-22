package com.example.ppps.controller;

import lombok.Data;

@Data
public class AuthenticationRequest {
    private String userId;
    private String pin;
}