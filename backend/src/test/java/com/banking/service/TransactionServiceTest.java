package com.banking.service;

import com.banking.dto.TransactionDTO;
import com.banking.dto.TransactionRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 单元测试")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountCreditEventPublisher accountCreditEventPublisher;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Mock
    private OpenAiTransactionDescriptionService openAiTransactionDescriptionService;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Account targetAccount;
    private Transaction testTransaction;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountName("TestAccount");
        testAccount.setAccountNumber("ACC123456");
        testAccount.setBalance(BigDecimal.valueOf(1000));
        testAccount.setUser(testUser);

        targetAccount = new Account();
        targetAccount.setId(2L);
        targetAccount.setAccountName("TargetAccount");
        targetAccount.setAccountNumber("ACC789012");
        targetAccount.setBalance(BigDecimal.valueOf(500));
        targetAccount.setUser(testUser);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionId("TXN-123");
        testTransaction.setAccount(testAccount);
        testTransaction.setAccountNumber("ACC123456");
        testTransaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        testTransaction.setAmount(BigDecimal.valueOf(100));
        testTransaction.setBalanceBefore(BigDecimal.valueOf(1000));
        testTransaction.setBalanceAfter(BigDecimal.valueOf(1100));
    }

    @Test
    @DisplayName("存款 - 成功")
    void deposit_Success() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setAmount(BigDecimal.valueOf(500));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionDTO result = transactionService.deposit(request);

        // Assert
        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountCreditEventPublisher).publishDepositEvent(any(Transaction.class));
    }

    @Test
    @DisplayName("存款 - 账户不存在")
    void deposit_AccountNotFound_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC999999");
        request.setAmount(BigDecimal.valueOf(500));

        when(accountRepository.findByAccountNumber("ACC999999"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.deposit(request);
        });
        assertTrue(exception.getMessage().contains("账户不存在"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("取款 - 成功")
    void withdraw_Success() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setAmount(BigDecimal.valueOf(200));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionDTO result = transactionService.withdraw(request);

        // Assert
        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("取款 - 余额不足")
    void withdraw_InsufficientBalance_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setAmount(BigDecimal.valueOf(2000));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.withdraw(request);
        });
        assertTrue(exception.getMessage().contains("余额不足"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("取款 - 账户不存在")
    void withdraw_AccountNotFound_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC999999");
        request.setAmount(BigDecimal.valueOf(200));

        when(accountRepository.findByAccountNumber("ACC999999"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.withdraw(request);
        });
        assertTrue(exception.getMessage().contains("账户不存在"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("转账 - 成功")
    void transfer_Success() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setTargetAccountNumber("ACC789012");
        request.setAmount(BigDecimal.valueOf(300));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC789012"))
                .thenReturn(Optional.of(targetAccount));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(testAccount)
                .thenReturn(targetAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionDTO result = transactionService.transfer(request);

        // Assert
        assertNotNull(result);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("转账 - 余额不足")
    void transfer_InsufficientBalance_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setTargetAccountNumber("ACC789012");
        request.setAmount(BigDecimal.valueOf(2000));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC789012"))
                .thenReturn(Optional.of(targetAccount));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(request);
        });
        assertTrue(exception.getMessage().contains("余额不足"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("转账 - 源账户不存在")
    void transfer_SourceAccountNotFound_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC999999");
        request.setTargetAccountNumber("ACC789012");
        request.setAmount(BigDecimal.valueOf(300));

        when(accountRepository.findByAccountNumber("ACC999999"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(request);
        });
        assertTrue(exception.getMessage().contains("源账户不存在"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("转账 - 目标账户不存在")
    void transfer_TargetAccountNotFound_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setTargetAccountNumber("ACC999999");
        request.setAmount(BigDecimal.valueOf(300));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC999999"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(request);
        });
        assertTrue(exception.getMessage().contains("目标账户不存在"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("转账 - 不能转账到同一账户")
    void transfer_SameAccount_ThrowsException() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("ACC123456");
        request.setTargetAccountNumber("ACC123456");
        request.setAmount(BigDecimal.valueOf(300));

        when(accountRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(request);
        });
        assertTrue(exception.getMessage().contains("不能转账到同一账户"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("根据ID查询交易记录 - 成功")
    void getTransactionById_Success() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        Optional<Transaction> result = transactionService.getTransactionById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testTransaction.getId(), result.get().getId());
    }

    @Test
    @DisplayName("根据账户ID查询所有交易记录")
    void getTransactionsByAccountId_Success() {
        // Arrange
        when(transactionRepository.findByAccountId(1L))
                .thenReturn(Arrays.asList(testTransaction));

        // Act
        List<TransactionDTO> results = transactionService.getTransactionsByAccountId(1L);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("根据账号查询所有交易记录")
    void getTransactionsByAccountNumber_Success() {
        // Arrange
        when(transactionRepository.findByAccountNumber("ACC123456"))
                .thenReturn(Arrays.asList(testTransaction));

        // Act
        List<Transaction> results = transactionService.getTransactionsByAccountNumber("ACC123456");

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("删除交易记录 - 成功")
    void deleteTransaction_Success() {
        // Arrange
        when(transactionRepository.existsById(1L)).thenReturn(true);

        // Act
        transactionService.deleteTransaction(1L);

        // Assert
        verify(transactionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除交易记录 - 交易记录不存在")
    void deleteTransaction_TransactionNotFound_ThrowsException() {
        // Arrange
        when(transactionRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.deleteTransaction(1L);
        });
        assertTrue(exception.getMessage().contains("交易记录不存在"));
        verify(transactionRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("交易描述推荐 - 成功写回 AI 描述和分类")
    void suggestDescription_Success() {
        // Arrange
        testTransaction.setDescription("cash from Westpac ATM");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(openAiTransactionDescriptionService.suggest(testTransaction, "cash from Westpac ATM"))
                .thenReturn(new OpenAiTransactionDescriptionService.Suggestion("ATM cash withdrawal", "Other"));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionDTO result = transactionService.suggestDescription(1L);

        // Assert
        assertEquals("ATM cash withdrawal", result.getAiDescription());
        assertEquals("Other", result.getAiCategory());
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    @DisplayName("交易描述推荐 - 交易记录不存在")
    void suggestDescription_TransactionNotFound_ThrowsException() {
        // Arrange
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.suggestDescription(99L);
        });
        assertTrue(exception.getMessage().contains("交易记录不存在"));
        verify(openAiTransactionDescriptionService, never()).suggest(any(), any());
    }
}
