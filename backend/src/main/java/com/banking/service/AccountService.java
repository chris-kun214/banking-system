package com.banking.service;

import com.banking.dto.AccountDTO;
import com.banking.dto.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * 为当前登录用户创建账户
     */
    @Transactional
    public AccountDTO createAccountForCurrentUser(CreateAccountRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查账户名是否已存在
        if (accountRepository.existsByAccountName(request.getAccountName())) {
            throw new RuntimeException("账户名已存在: " + request.getAccountName());
        }
        
        // 检查账号是否已存在
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new RuntimeException("账号已存在: " + request.getAccountNumber());
        }

        // 创建账户
        Account account = new Account();
        account.setAccountName(request.getAccountName());
        account.setAccountNumber(request.getAccountNumber());
        account.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        account.setUser(user);

        Account savedAccount = accountRepository.save(account);
        return AccountDTO.fromEntity(savedAccount);
    }

    /**
     * 创建账户（原始方法，保留向后兼容）
     */
    @Transactional
    public Account createAccount(Account account) {
        // 检查账户名是否已存在
        if (accountRepository.existsByAccountName(account.getAccountName())) {
            throw new RuntimeException("账户名已存在: " + account.getAccountName());
        }
        // 检查账号是否已存在
        if (accountRepository.existsByAccountNumber(account.getAccountNumber())) {
            throw new RuntimeException("账号已存在: " + account.getAccountNumber());
        }
        return accountRepository.save(account);
    }

    /**
     * 根据ID查询账户
     */
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    /**
     * 根据ID查询账户DTO
     */
    public Optional<AccountDTO> getAccountDTOById(Long id) {
        return accountRepository.findById(id).map(AccountDTO::fromEntity);
    }

    /**
     * 根据账户名查询账户
     */
    public Optional<Account> getAccountByName(String accountName) {
        return accountRepository.findByAccountName(accountName);
    }

    /**
     * 根据账号查询账户
     */
    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    /**
     * 获取当前用户的所有账户
     */
    public List<AccountDTO> getCurrentUserAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        return accountRepository.findByUserUsername(username).stream()
                .map(AccountDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据用户ID查询所有账户
     */
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有账户
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * 查询所有账户DTO
     */
    public List<AccountDTO> getAllAccountDTOs() {
        return accountRepository.findAll().stream()
                .map(AccountDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 更新账户
     */
    @Transactional
    public Account updateAccount(Long id, Account accountDetails) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账户不存在，ID: " + id));

        // 如果要更新账户名，检查新账户名是否已被使用
        if (!account.getAccountName().equals(accountDetails.getAccountName())) {
            if (accountRepository.existsByAccountName(accountDetails.getAccountName())) {
                throw new RuntimeException("账户名已存在: " + accountDetails.getAccountName());
            }
            account.setAccountName(accountDetails.getAccountName());
        }

        // 如果要更新账号，检查新账号是否已被使用
        if (!account.getAccountNumber().equals(accountDetails.getAccountNumber())) {
            if (accountRepository.existsByAccountNumber(accountDetails.getAccountNumber())) {
                throw new RuntimeException("账号已存在: " + accountDetails.getAccountNumber());
            }
            account.setAccountNumber(accountDetails.getAccountNumber());
        }

        account.setBalance(accountDetails.getBalance());
        return accountRepository.save(account);
    }

    /**
     * 删除账户
     */
    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("账户不存在，ID: " + id);
        }
        accountRepository.deleteById(id);
    }

    /**
     * 检查账户是否存在
     */
    public boolean accountExists(Long id) {
        return accountRepository.existsById(id);
    }
}
