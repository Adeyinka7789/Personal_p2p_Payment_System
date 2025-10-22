package com.example.ppps.controller;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceResponse {
    private BigDecimal balance;
    private String currency;
}