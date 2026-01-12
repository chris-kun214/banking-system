package com.banking.dto;

import com.banking.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    private Long id;
    private String accountName;
    private String accountNumber;
    private BigDecimal balance;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Account 实体转换为 DTO
     */
    public static AccountDTO fromEntity(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getAccountName(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getUser() != null ? account.getUser().getId() : null,
                account.getUser() != null ? account.getUser().getUsername() : null,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

