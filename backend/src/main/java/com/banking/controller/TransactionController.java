package com.banking.controller;

import com.banking.dto.ApiResponse;
import com.banking.dto.TransactionDTO;
import com.banking.dto.TransactionRequest;
import com.banking.entity.Transaction;
import com.banking.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    /**
     * 存款
     * POST /api/transactions/deposit
     */
    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TransactionDTO>> deposit(@Valid @RequestBody TransactionRequest request) {
        try {
            logger.info("存款请求: 账号={}, 金额={}", request.getAccountNumber(), request.getAmount());
            TransactionDTO transaction = transactionService.deposit(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("存款成功", transaction));
        } catch (RuntimeException e) {
            logger.error("存款失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 取款
     * POST /api/transactions/withdraw
     */
    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TransactionDTO>> withdraw(@Valid @RequestBody TransactionRequest request) {
        try {
            logger.info("取款请求: 账号={}, 金额={}", request.getAccountNumber(), request.getAmount());
            TransactionDTO transaction = transactionService.withdraw(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("取款成功", transaction));
        } catch (RuntimeException e) {
            logger.error("取款失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 转账
     * POST /api/transactions/transfer
     */
    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TransactionDTO>> transfer(@Valid @RequestBody TransactionRequest request) {
        try {
            logger.info("转账请求: 源账号={}, 目标账号={}, 金额={}", 
                    request.getAccountNumber(), request.getTargetAccountNumber(), request.getAmount());
            TransactionDTO transaction = transactionService.transfer(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("转账成功", transaction));
        } catch (RuntimeException e) {
            logger.error("转账失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 创建交易记录（管理员）
     * POST /api/transactions
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Transaction>> createTransaction(@Valid @RequestBody Transaction transaction) {
        try {
            Transaction createdTransaction = transactionService.createTransaction(transaction);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("交易记录创建成功", createdTransaction));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 根据ID查询交易记录
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionDTOById(id)
                .map(transaction -> ResponseEntity.ok(ApiResponse.success("查询成功", transaction)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("交易记录不存在，ID: " + id)));
    }

    /**
     * 根据交易ID查询交易记录
     * GET /api/transactions/transaction-id/{transactionId}
     */
    @GetMapping("/transaction-id/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionByTransactionId(@PathVariable String transactionId) {
        return transactionService.getTransactionByTransactionId(transactionId)
                .map(transaction -> ResponseEntity.ok(ApiResponse.success("查询成功", transaction)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("交易ID不存在: " + transactionId)));
    }

    /**
     * 根据账户ID查询所有交易记录
     * GET /api/transactions/account-id/{accountId}
     */
    @GetMapping("/account-id/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByAccountId(
            @PathVariable Long accountId) {
        try {
            List<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(accountId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", transactions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 根据账号查询所有交易记录（管理员）
     * GET /api/transactions/account/{accountNumber}
     */
    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByAccountNumber(
            @PathVariable String accountNumber) {
        List<TransactionDTO> transactions = transactionService.getTransactionDTOsByAccountNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("查询成功", transactions));
    }

    /**
     * 根据交易类型查询交易记录（管理员）
     * GET /api/transactions/type/{transactionType}
     */
    @GetMapping("/type/{transactionType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionsByType(
            @PathVariable Transaction.TransactionType transactionType) {
        List<Transaction> transactions = transactionService.getTransactionsByType(transactionType);
        return ResponseEntity.ok(ApiResponse.success("查询成功", transactions));
    }

    /**
     * 根据账号和交易类型查询交易记录（管理员）
     * GET /api/transactions/account/{accountNumber}/type/{transactionType}
     */
    @GetMapping("/account/{accountNumber}/type/{transactionType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionsByAccountNumberAndType(
            @PathVariable String accountNumber,
            @PathVariable Transaction.TransactionType transactionType) {
        List<Transaction> transactions = transactionService
                .getTransactionsByAccountNumberAndType(accountNumber, transactionType);
        return ResponseEntity.ok(ApiResponse.success("查询成功", transactions));
    }

    /**
     * 查询所有交易记录（管理员）
     * GET /api/transactions/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAllTransactions() {
        List<TransactionDTO> transactions = transactionService.getAllTransactionDTOs();
        return ResponseEntity.ok(ApiResponse.success("查询成功", transactions));
    }

    /**
     * 更新交易记录（管理员）
     * PUT /api/transactions/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Transaction>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody Transaction transactionDetails) {
        try {
            Transaction updatedTransaction = transactionService.updateTransaction(id, transactionDetails);
            return ResponseEntity.ok(ApiResponse.success("交易记录更新成功", updatedTransaction));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除交易记录（管理员）
     * DELETE /api/transactions/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        try {
            transactionService.deleteTransaction(id);
            return ResponseEntity.ok(ApiResponse.success("交易记录删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 检查交易记录是否存在
     * HEAD /api/transactions/{id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> checkTransactionExists(@PathVariable Long id) {
        if (transactionService.transactionExists(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
