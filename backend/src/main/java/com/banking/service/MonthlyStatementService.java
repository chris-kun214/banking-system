package com.banking.service;

import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyStatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${report.monthly.output-dir:./build/reports/monthly}")
    private String outputDir;

    public Path generateMonthlyStatementPdf(String accountNumber, YearMonth yearMonth) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("账户不存在: " + accountNumber));

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Transaction> transactions = transactionRepository
                .findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtAsc(
                        accountNumber, start, endExclusive);

        String fileName = String.format("statement-%s-%s.pdf", accountNumber, yearMonth);
        Path dirPath = Paths.get(outputDir);
        Path filePath = dirPath.resolve(fileName);

        try {
            Files.createDirectories(dirPath);
            writePdf(filePath, account, transactions, yearMonth);
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("生成月度流水 PDF 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为所有账户批量生成月度流水 PDF（供 EventBridge 定时触发的月报 Lambda 调用）。
     * 单个账户生成失败不应中断整批，记录日志后继续处理下一个账户。
     */
    public int generateMonthlyStatementsForAllAccounts(YearMonth yearMonth) {
        List<Account> accounts = accountRepository.findAll();
        int generated = 0;
        for (Account account : accounts) {
            try {
                generateMonthlyStatementPdf(account.getAccountNumber(), yearMonth);
                generated++;
            } catch (RuntimeException e) {
                log.error("生成月度流水失败，账号: {}, 月份: {}", account.getAccountNumber(), yearMonth, e);
            }
        }
        return generated;
    }

    private void writePdf(Path filePath, Account account, List<Transaction> transactions, YearMonth yearMonth) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50f;
            float y = 780f;
            float lineHeight = 14f;
            PDPageContentStream content = new PDPageContentStream(document, page);
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.newLineAtOffset(margin, y);
            content.showText("Bank Monthly Statement");

            content.setFont(PDType1Font.HELVETICA, 11);
            content.newLineAtOffset(0, -lineHeight * 2);
            y -= lineHeight * 2;
            content.showText("Account Number: " + account.getAccountNumber());
            content.newLineAtOffset(0, -lineHeight);
            y -= lineHeight;
            content.showText("Account Name: " + account.getAccountName());
            content.newLineAtOffset(0, -lineHeight);
            y -= lineHeight;
            content.showText("Month: " + yearMonth);
            content.newLineAtOffset(0, -lineHeight);
            y -= lineHeight;
            content.showText("Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            content.newLineAtOffset(0, -lineHeight * 2);
            y -= lineHeight * 2;
            content.setFont(PDType1Font.HELVETICA_BOLD, 11);
            content.showText("Date                Type            Amount          Balance Before   Balance After");
            content.setFont(PDType1Font.HELVETICA, 10);

            BigDecimal totalIn = BigDecimal.ZERO;
            BigDecimal totalOut = BigDecimal.ZERO;

            for (Transaction tx : transactions) {
                if (y < 80f) {
                    content.endText();
                    content.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = 780f;
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    content.newLineAtOffset(margin, y);
                    content.showText("Date                Type            Amount          Balance Before   Balance After");
                    content.setFont(PDType1Font.HELVETICA, 10);
                }

                content.newLineAtOffset(0, -lineHeight);
                y -= lineHeight;
                content.showText(formatLine(tx));

                if (isIncoming(tx)) {
                    totalIn = totalIn.add(tx.getAmount());
                } else {
                    totalOut = totalOut.add(tx.getAmount());
                }
            }

            BigDecimal openingBalance = transactions.isEmpty() ? account.getBalance() : transactions.get(0).getBalanceBefore();
            BigDecimal closingBalance = transactions.isEmpty() ? account.getBalance() : transactions.get(transactions.size() - 1).getBalanceAfter();

            if (y < 120f) {
                content.endText();
                content.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                content = new PDPageContentStream(document, page);
                y = 780f;
                content.beginText();
                content.newLineAtOffset(margin, y);
            } else {
                content.newLineAtOffset(0, -lineHeight * 2);
            }

            content.setFont(PDType1Font.HELVETICA_BOLD, 11);
            content.showText("Summary");
            content.setFont(PDType1Font.HELVETICA, 10);
            content.newLineAtOffset(0, -lineHeight);
            content.showText("Opening Balance: " + openingBalance);
            content.newLineAtOffset(0, -lineHeight);
            content.showText("Total In: " + totalIn);
            content.newLineAtOffset(0, -lineHeight);
            content.showText("Total Out: " + totalOut);
            content.newLineAtOffset(0, -lineHeight);
            content.showText("Closing Balance: " + closingBalance);
            content.endText();
            content.close();

            document.save(filePath.toFile());
        }
    }

    private String formatLine(Transaction tx) {
        String date = tx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String type = tx.getTransactionType().name();
        return String.format("%-18s %-14s %-15s %-15s %s",
                date,
                type,
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter());
    }

    private boolean isIncoming(Transaction tx) {
        return tx.getTransactionType() == Transaction.TransactionType.DEPOSIT
                || tx.getTransactionType() == Transaction.TransactionType.TRANSFER_IN;
    }
}
