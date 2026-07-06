package com.banking.service;

import com.banking.dto.TransactionDTO;
import com.banking.dto.TransactionRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountCreditEventPublisher accountCreditEventPublisher;

    /**
     * 存款
     */
    @Transactional
    public TransactionDTO deposit(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("账户不存在: " + request.getAccountNumber()));

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());

        // 更新账户余额
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        // 创建交易记录
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setAccount(account);
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountCreditEventPublisher.publishDepositEvent(savedTransaction);
        return TransactionDTO.fromEntity(savedTransaction);
    }

    /**
     * 取款
     */
    @Transactional
    public TransactionDTO withdraw(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("账户不存在: " + request.getAccountNumber()));

        BigDecimal balanceBefore = account.getBalance();
        
        // 检查余额是否充足
        if (balanceBefore.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("余额不足，当前余额: " + balanceBefore);
        }

        BigDecimal balanceAfter = balanceBefore.subtract(request.getAmount());

        // 更新账户余额
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        // 创建交易记录
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setAccount(account);
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAW);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionDTO.fromEntity(savedTransaction);
    }

    /**
     * 转账
     */
    @Transactional
    public TransactionDTO transfer(TransactionRequest request) {
        if (request.getTargetAccountNumber() == null || request.getTargetAccountNumber().isEmpty()) {
            throw new RuntimeException("目标账号不能为空");
        }

        // 查找源账户和目标账户
        Account sourceAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("源账户不存在: " + request.getAccountNumber()));
        
        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new RuntimeException("目标账户不存在: " + request.getTargetAccountNumber()));

        // 检查是否是同一账户
        if (sourceAccount.getId() == targetAccount.getId()) {
            throw new RuntimeException("不能转账到同一账户");
        }

        BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
        
        // 检查余额是否充足
        if (sourceBalanceBefore.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("余额不足，当前余额: " + sourceBalanceBefore);
        }

        BigDecimal sourceBalanceAfter = sourceBalanceBefore.subtract(request.getAmount());
        BigDecimal targetBalanceBefore = targetAccount.getBalance();
        BigDecimal targetBalanceAfter = targetBalanceBefore.add(request.getAmount());

        // 更新源账户余额
        sourceAccount.setBalance(sourceBalanceAfter);
        accountRepository.save(sourceAccount);

        // 更新目标账户余额
        targetAccount.setBalance(targetBalanceAfter);
        accountRepository.save(targetAccount);

        // 创建转出交易记录
        Transaction transferOutTransaction = new Transaction();
        transferOutTransaction.setTransactionId(generateTransactionId());
        transferOutTransaction.setAccount(sourceAccount);
        transferOutTransaction.setAccountNumber(sourceAccount.getAccountNumber());
        transferOutTransaction.setTransactionType(Transaction.TransactionType.TRANSFER_OUT);
        transferOutTransaction.setAmount(request.getAmount());
        transferOutTransaction.setBalanceBefore(sourceBalanceBefore);
        transferOutTransaction.setBalanceAfter(sourceBalanceAfter);
        transferOutTransaction.setCreatedAt(LocalDateTime.now());
        
        Transaction savedTransferOut = transactionRepository.save(transferOutTransaction);

        // 创建转入交易记录
        Transaction transferInTransaction = new Transaction();
        transferInTransaction.setTransactionId(generateTransactionId());
        transferInTransaction.setAccount(targetAccount);
        transferInTransaction.setAccountNumber(targetAccount.getAccountNumber());
        transferInTransaction.setTransactionType(Transaction.TransactionType.TRANSFER_IN);
        transferInTransaction.setAmount(request.getAmount());
        transferInTransaction.setBalanceBefore(targetBalanceBefore);
        transferInTransaction.setBalanceAfter(targetBalanceAfter);
        transferInTransaction.setCreatedAt(LocalDateTime.now());
        
        transactionRepository.save(transferInTransaction);

        return TransactionDTO.fromEntity(savedTransferOut);
    }

    /**
     * 创建交易记录（原始方法，保留向后兼容）
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * 根据ID查询交易记录
     */
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * 根据ID查询交易记录DTO
     */
    public Optional<TransactionDTO> getTransactionDTOById(Long id) {
        return transactionRepository.findById(id).map(TransactionDTO::fromEntity);
    }

    /**
     * 根据交易ID查询交易记录
     */
    public Optional<Transaction> getTransactionByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    /**
     * 根据账户ID查询所有交易记录
     */
    public List<TransactionDTO> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据账号查询所有交易记录
     */
    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    /**
     * 根据账号查询所有交易记录DTO
     */
    public List<TransactionDTO> getTransactionDTOsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据交易类型查询交易记录
     */
    public List<Transaction> getTransactionsByType(Transaction.TransactionType transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }

    /**
     * 根据账号和交易类型查询交易记录
     */
    public List<Transaction> getTransactionsByAccountNumberAndType(
            String accountNumber, Transaction.TransactionType transactionType) {
        return transactionRepository.findByAccountNumberAndTransactionType(accountNumber, transactionType);
    }

    /**
     * 查询所有交易记录
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * 查询所有交易记录DTO
     */
    public List<TransactionDTO> getAllTransactionDTOs() {
        return transactionRepository.findAll().stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 更新交易记录
     */
    @Transactional
    public Transaction updateTransaction(Long id, Transaction transactionDetails) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("交易记录不存在，ID: " + id));

        transaction.setTransactionId(transactionDetails.getTransactionId());
        transaction.setAccountNumber(transactionDetails.getAccountNumber());
        transaction.setTransactionType(transactionDetails.getTransactionType());
        transaction.setAmount(transactionDetails.getAmount());
        transaction.setBalanceBefore(transactionDetails.getBalanceBefore());
        transaction.setBalanceAfter(transactionDetails.getBalanceAfter());

        return transactionRepository.save(transaction);
    }

    /**
     * 删除交易记录
     */
    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("交易记录不存在，ID: " + id);
        }
        transactionRepository.deleteById(id);
    }

    /**
     * 检查交易记录是否存在
     */
    public boolean transactionExists(Long id) {
        return transactionRepository.existsById(id);
    }

    /**
     * 生成唯一的交易ID
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
