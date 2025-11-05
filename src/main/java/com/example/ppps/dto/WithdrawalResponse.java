// REVIEW: full code review requested

package com.example.ppps.dto;

import com.example.ppps.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {
    private String transactionRef;
    private BigDecimal amount;
    private String accountNumber;
    private String bankName;
    private TransactionStatus status;
    private Instant createdAt;
    private String message;
}