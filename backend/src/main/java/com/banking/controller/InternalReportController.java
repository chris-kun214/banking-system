package com.banking.controller;

import com.banking.service.MonthlyStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;

/**
 * 供 EventBridge 定时触发的月报 Lambda 调用，不走用户 JWT，
 * 由 {@link com.banking.config.InternalApiKeyFilter} 用共享密钥单独鉴权。
 */
@RestController
@RequestMapping("/api/internal/reports")
@RequiredArgsConstructor
public class InternalReportController {

    private final MonthlyStatementService monthlyStatementService;

    /**
     * 批量生成上个自然月（默认）或指定月份所有账户的流水 PDF。
     * 示例：POST /api/internal/reports/monthly-batch?month=2026-06
     */
    @PostMapping("/monthly-batch")
    public ResponseEntity<Map<String, Object>> generateMonthlyBatch(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth targetMonth = month == null ? YearMonth.now().minusMonths(1) : month;
        int generated = monthlyStatementService.generateMonthlyStatementsForAllAccounts(targetMonth);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "month", targetMonth.toString(),
                "generated", generated));
    }
}
