package com.banking.service;

import com.banking.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 给交易生成一句人类可读的描述 + 分类标签。未启用或未配置 API key 时走规则兜底，
 * 绝不阻塞或抛异常影响调用方（存取款/转账等资金操作永远不依赖这个服务）。
 */
@Slf4j
@Service
public class OpenAiTransactionDescriptionService {

    private static final String SYSTEM_PROMPT = """
            You are a banking assistant that writes short, human-friendly transaction descriptions.
            Given a transaction type, amount, and an optional user note, respond with ONLY a JSON object
            of the form {"description": "...", "category": "..."} — no markdown, no extra text.
            description: one short sentence (max ~12 words).
            category: one of Groceries, Dining, Transport, Rent, Utilities, Entertainment, Salary,
            Transfer, Shopping, Healthcare, Other.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean enabled;
    private final String apiKey;
    private final String model;

    public OpenAiTransactionDescriptionService(
            RestClient.Builder restClientBuilder,
            @Value("${openai.enabled:false}") boolean enabled,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
    }

    public record Suggestion(String description, String category) {
    }

    public Suggestion suggest(Transaction transaction, String userNote) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return fallback(transaction);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildUserPrompt(transaction, userNote))),
                    "temperature", 0.2);

            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseSuggestion(rawResponse, transaction);
        } catch (Exception e) {
            log.warn("OpenAI 交易描述推荐失败，使用规则兜底描述, transactionId={}: {}",
                    transaction.getTransactionId(), e.getMessage());
            return fallback(transaction);
        }
    }

    private String buildUserPrompt(Transaction transaction, String userNote) {
        StringBuilder prompt = new StringBuilder()
                .append("Transaction type: ").append(transaction.getTransactionType()).append('\n')
                .append("Amount: ").append(transaction.getAmount());
        if (userNote != null && !userNote.isBlank()) {
            prompt.append('\n').append("User note: ").append(userNote);
        }
        return prompt.toString();
    }

    private Suggestion parseSuggestion(String rawResponse, Transaction transaction) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode suggestionNode = objectMapper.readTree(content);
            String description = suggestionNode.path("description").asText(null);
            String category = suggestionNode.path("category").asText(null);
            if (description == null || description.isBlank()) {
                return fallback(transaction);
            }
            return new Suggestion(description, category != null && !category.isBlank() ? category : "Other");
        } catch (Exception e) {
            log.warn("解析 OpenAI 响应失败，使用规则兜底描述: {}", e.getMessage());
            return fallback(transaction);
        }
    }

    private Suggestion fallback(Transaction transaction) {
        String description = "%s of $%s".formatted(transaction.getTransactionType(), transaction.getAmount());
        return new Suggestion(description, "Other");
    }
}
