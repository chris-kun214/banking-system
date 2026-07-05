package com.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreditEvent {
    private String eventId;
    private String transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime occurredAt;
}
