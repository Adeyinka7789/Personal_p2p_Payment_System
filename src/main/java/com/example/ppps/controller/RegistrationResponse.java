package com.example.ppps.controller;

import lombok.Data;
import java.util.UUID;

@Data
public class RegistrationResponse {
    private String userId;
    private UUID walletId;
}