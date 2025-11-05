// REVIEW: full code review requested

package com.example.ppps.controller;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BalanceResponse implements Serializable {
    private BigDecimal balance;
    private String currency;
}