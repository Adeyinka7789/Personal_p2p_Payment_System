package com.example.ppps.controller;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionSearchRequest {
    private Instant startDate;
    private Instant endDate;
    private String status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer pageNumber;
    private Integer pageSize;
}