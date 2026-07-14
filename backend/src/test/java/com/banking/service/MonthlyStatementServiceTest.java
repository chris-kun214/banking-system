package com.banking.service;

import com.banking.entity.Account;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyStatementService 单元测试")
class MonthlyStatementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private MonthlyStatementService monthlyStatementService;

    @TempDir
    Path tempDir;

    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monthlyStatementService, "outputDir", tempDir.toString());

        accountA = new Account();
        accountA.setAccountNumber("ACC-A");
        accountA.setAccountName("Account A");
        accountA.setBalance(BigDecimal.valueOf(100));

        accountB = new Account();
        accountB.setAccountNumber("ACC-B");
        accountB.setAccountName("Account B");
        accountB.setBalance(BigDecimal.valueOf(200));

        when(transactionRepository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtAsc(
                anyString(), any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("批量生成 - 全部账户成功")
    void generateMonthlyStatementsForAllAccounts_allSucceed() {
        when(accountRepository.findAll()).thenReturn(Arrays.asList(accountA, accountB));
        when(accountRepository.findByAccountNumber("ACC-A")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACC-B")).thenReturn(Optional.of(accountB));

        int generated = monthlyStatementService.generateMonthlyStatementsForAllAccounts(YearMonth.of(2026, 6));

        assertEquals(2, generated);
    }

    @Test
    @DisplayName("批量生成 - 单个账户失败不影响其余账户继续生成")
    void generateMonthlyStatementsForAllAccounts_oneAccountFails_continuesOthers() {
        Account ghost = new Account();
        ghost.setAccountNumber("ACC-GHOST");

        when(accountRepository.findAll()).thenReturn(Arrays.asList(accountA, ghost));
        when(accountRepository.findByAccountNumber("ACC-A")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACC-GHOST")).thenReturn(Optional.empty());

        int generated = monthlyStatementService.generateMonthlyStatementsForAllAccounts(YearMonth.of(2026, 6));

        assertEquals(1, generated);
    }

    @Test
    @DisplayName("生成 PDF - 非 ASCII 账户名（如中文）不应导致 PDFBox 渲染失败")
    void generateMonthlyStatementPdf_nonAsciiAccountName_doesNotThrow() {
        Account chineseNamedAccount = new Account();
        chineseNamedAccount.setAccountNumber("ACC-CN");
        chineseNamedAccount.setAccountName("日常账户");
        chineseNamedAccount.setBalance(BigDecimal.valueOf(1000));

        when(accountRepository.findByAccountNumber("ACC-CN")).thenReturn(Optional.of(chineseNamedAccount));

        Path result = assertDoesNotThrow(() ->
                monthlyStatementService.generateMonthlyStatementPdf("ACC-CN", YearMonth.of(2026, 6)));

        assertTrue(Files.exists(result));
    }
}
