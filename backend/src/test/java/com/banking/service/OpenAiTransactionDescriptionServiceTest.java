package com.banking.service;

import com.banking.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OpenAiTransactionDescriptionService 单元测试")
class OpenAiTransactionDescriptionServiceTest {

    private static final String BASE_URL = "https://api.openai.test/v1";

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setTransactionId("TXN-1");
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAW);
        transaction.setAmount(new BigDecimal("42.50"));
    }

    private OpenAiTransactionDescriptionService buildService(boolean enabled, String apiKey, MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        serverOut[0] = server;
        return new OpenAiTransactionDescriptionService(builder, enabled, apiKey, "gpt-4o-mini", BASE_URL);
    }

    @Test
    @DisplayName("未启用时直接走规则兜底，不发起 HTTP 请求")
    void disabled_usesFallback_noHttpCall() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        OpenAiTransactionDescriptionService service = buildService(false, "some-key", serverOut);

        OpenAiTransactionDescriptionService.Suggestion suggestion = service.suggest(transaction, null);

        assertEquals("WITHDRAW of $42.50", suggestion.description());
        assertEquals("Other", suggestion.category());
        serverOut[0].verify(); // no expectations set, so this just confirms nothing unexpected happened
    }

    @Test
    @DisplayName("启用但未配置 key 时走规则兜底")
    void enabledWithoutKey_usesFallback() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        OpenAiTransactionDescriptionService service = buildService(true, "", serverOut);

        OpenAiTransactionDescriptionService.Suggestion suggestion = service.suggest(transaction, null);

        assertEquals("WITHDRAW of $42.50", suggestion.description());
    }

    @Test
    @DisplayName("成功调用 OpenAI 时解析出描述和分类")
    void enabledWithKey_parsesSuccessfulResponse() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        OpenAiTransactionDescriptionService service = buildService(true, "test-key", serverOut);

        String openAiResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"description\\":\\"ATM cash withdrawal\\",\\"category\\":\\"Other\\"}"
                      }
                    }
                  ]
                }
                """;

        serverOut[0].expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(openAiResponse, MediaType.APPLICATION_JSON));

        OpenAiTransactionDescriptionService.Suggestion suggestion = service.suggest(transaction, "cash from Westpac ATM");

        assertEquals("ATM cash withdrawal", suggestion.description());
        assertEquals("Other", suggestion.category());
        serverOut[0].verify();
    }

    @Test
    @DisplayName("OpenAI 调用失败时不抛异常，走规则兜底")
    void httpError_fallsBackGracefully() {
        MockRestServiceServer[] serverOut = new MockRestServiceServer[1];
        OpenAiTransactionDescriptionService service = buildService(true, "test-key", serverOut);

        serverOut[0].expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withServerError());

        OpenAiTransactionDescriptionService.Suggestion suggestion = service.suggest(transaction, null);

        assertEquals("WITHDRAW of $42.50", suggestion.description());
        assertEquals("Other", suggestion.category());
    }
}
