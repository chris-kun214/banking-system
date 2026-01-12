package com.banking.repository;

import com.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 根据交易ID查找交易
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * 根据账户ID查找所有交易记录
     */
    List<Transaction> findByAccountId(Long accountId);

    /**
     * 根据账号查找所有交易记录
     */
    List<Transaction> findByAccountNumber(String accountNumber);

    /**
     * 根据交易类型查找交易记录
     */
    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);

    /**
     * 根据账号和交易类型查找交易记录
     */
    List<Transaction> findByAccountNumberAndTransactionType(String accountNumber,
            Transaction.TransactionType transactionType);

    /**
     * 根据账户ID和交易类型查找交易记录
     */
    List<Transaction> findByAccountIdAndTransactionType(Long accountId,
            Transaction.TransactionType transactionType);
}
