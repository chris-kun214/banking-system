package com.banking.dto;

import com.banking.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private Long id;
    private String transactionId;
    private Long accountId;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    /**
     * 从 Transaction 实体转换为 DTO
     */
    public static TransactionDTO fromEntity(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getAccount() != null ? transaction.getAccount().getId() : null,
                transaction.getAccountNumber(),
                transaction.getTransactionType().name(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}

