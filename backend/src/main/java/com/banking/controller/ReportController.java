package com.banking.controller;

import com.banking.service.MonthlyStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final MonthlyStatementService monthlyStatementService;

    /**
     * 生成并下载账户月度流水 PDF。
     * 示例：GET /api/reports/monthly?accountNumber=123456&month=2026-02
     */
    @GetMapping("/monthly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> generateMonthlyReport(
            @RequestParam String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        Path pdfPath = monthlyStatementService.generateMonthlyStatementPdf(accountNumber, targetMonth);
        FileSystemResource resource = new FileSystemResource(pdfPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(pdfPath.getFileName().toString())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
