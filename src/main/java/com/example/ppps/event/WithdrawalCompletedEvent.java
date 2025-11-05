// REVIEW: full code review requested

package com.example.ppps.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalCompletedEvent {
    private UUID transactionId;
    private UUID senderWalletId;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String status;
    private Instant completedAt;
}