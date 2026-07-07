package com.banking.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_account_trans"))
    private Account account;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 用户可选填的原始备注
    @Column(name = "description", length = 255)
    private String description;

    // LLM（OpenAI）生成的交易描述推荐，见 OpenAiTransactionDescriptionService
    @Column(name = "ai_description", length = 255)
    private String aiDescription;

    @Column(name = "ai_category", length = 50)
    private String aiCategory;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (account != null && accountNumber == null) {
            accountNumber = account.getAccountNumber();
        }
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    }
}
