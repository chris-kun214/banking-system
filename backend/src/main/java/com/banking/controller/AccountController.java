package com.banking.controller;

import com.banking.dto.AccountDTO;
import com.banking.dto.ApiResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 为当前用户创建账户
     * POST /api/accounts
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AccountDTO>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            AccountDTO createdAccount = accountService.createAccountForCurrentUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("账户创建成功", createdAccount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取当前用户的所有账户
     * GET /api/accounts/my
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getMyAccounts() {
        try {
            List<AccountDTO> accounts = accountService.getCurrentUserAccounts();
            return ResponseEntity.ok(ApiResponse.success("查询成功", accounts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 根据ID查询账户（管理员）
     * GET /api/accounts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable Long id) {
        return accountService.getAccountDTOById(id)
                .map(account -> ResponseEntity.ok(ApiResponse.success("查询成功", account)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("账户不存在，ID: " + id)));
    }

    /**
     * 根据账户名查询账户（管理员）
     * GET /api/accounts/name/{accountName}
     */
    @GetMapping("/name/{accountName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Account>> getAccountByName(@PathVariable String accountName) {
        return accountService.getAccountByName(accountName)
                .map(account -> ResponseEntity.ok(ApiResponse.success("查询成功", account)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("账户名不存在: " + accountName)));
    }

    /**
     * 根据账号查询账户（管理员）
     * GET /api/accounts/number/{accountNumber}
     */
    @GetMapping("/number/{accountNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Account>> getAccountByNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber)
                .map(account -> ResponseEntity.ok(ApiResponse.success("查询成功", account)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("账号不存在: " + accountNumber)));
    }

    /**
     * 根据用户ID查询账户（管理员）
     * GET /api/accounts/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByUserId(@PathVariable Long userId) {
        try {
            List<AccountDTO> accounts = accountService.getAccountsByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", accounts));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 查询所有账户（管理员）
     * GET /api/accounts/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts() {
        List<AccountDTO> accounts = accountService.getAllAccountDTOs();
        return ResponseEntity.ok(ApiResponse.success("查询成功", accounts));
    }

    /**
     * 更新账户（管理员）
     * PUT /api/accounts/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody Account accountDetails) {
        try {
            Account updatedAccount = accountService.updateAccount(id, accountDetails);
            return ResponseEntity.ok(ApiResponse.success("账户更新成功", updatedAccount));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除账户（管理员）
     * DELETE /api/accounts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok(ApiResponse.success("账户删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 检查账户是否存在
     * HEAD /api/accounts/{id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> checkAccountExists(@PathVariable Long id) {
        if (accountService.accountExists(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
