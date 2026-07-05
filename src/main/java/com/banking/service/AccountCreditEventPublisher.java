package com.banking.service;

import com.banking.dto.AccountCreditEvent;
import com.banking.entity.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountCreditEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AccountCreditEventPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.enabled:false}")
    private boolean sqsEnabled;

    @Value("${aws.sqs.fail-on-error:false}")
    private boolean failOnError;

    @Value("${aws.sqs.credit-queue-url:}")
    private String creditQueueUrl;

    @Value("${aws.sqs.message-group-id:banking-credit-events}")
    private String messageGroupId;

    public void publishDepositEvent(Transaction transaction) {
        if (!sqsEnabled) {
            logger.debug("SQS disabled, skip publishing deposit event. txnId={}", transaction.getTransactionId());
            return;
        }

        if (creditQueueUrl == null || creditQueueUrl.isBlank()) {
            throw new RuntimeException("SQS 队列地址未配置: aws.sqs.credit-queue-url");
        }

        AccountCreditEvent event = AccountCreditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transaction.getTransactionId())
                .accountNumber(transaction.getAccountNumber())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .occurredAt(transaction.getCreatedAt())
                .build();

        try {
            String body = objectMapper.writeValueAsString(event);
            SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                    .queueUrl(creditQueueUrl)
                    .messageBody(body);

            if (creditQueueUrl.endsWith(".fifo")) {
                requestBuilder.messageGroupId(messageGroupId);
                requestBuilder.messageDeduplicationId(event.getEventId());
            }

            SendMessageRequest request = requestBuilder.build();

            sqsClient.sendMessage(request);
            logger.info("Deposit event sent to SQS successfully, txnId={}", transaction.getTransactionId());
        } catch (JsonProcessingException | SqsException e) {
            logger.error("Failed to send deposit event to SQS, txnId={}, error={}",
                    transaction.getTransactionId(), e.getMessage(), e);
            if (failOnError) {
                throw new RuntimeException("发送 SQS 消息失败: " + e.getMessage(), e);
            }
        }
    }
}
