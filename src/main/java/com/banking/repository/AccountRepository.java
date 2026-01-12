package com.banking.repository;

import com.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 根据账户名查找账户
     */
    Optional<Account> findByAccountName(String accountName);

    /**
     * 根据账号查找账户
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * 根据用户ID查找所有账户
     */
    List<Account> findByUserId(Long userId);

    /**
     * 根据用户名查找所有账户
     */
    List<Account> findByUserUsername(String username);

    /**
     * 检查账户名是否存在
     */
    boolean existsByAccountName(String accountName);

    /**
     * 检查账号是否存在
     */
    boolean existsByAccountNumber(String accountNumber);
}
