package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FramingAnalysisToolTest {

    // An API reply whose text block holds the judge's JSON answers (quotes escaped, since it's a string inside JSON).
    private static final String JUDGE_REPLY = """
            {"model": "claude-sonnet-5-5", "stop_reason": "end_turn", "content": [{"type": "text", "text": \
            "{\\"charged_words\\": {\\"reason\\": \\"'crisis spirals' is charged.\\", \\"answer\\": \\"yes\\"}, \
            \\"built_in_blame\\": {\\"reason\\": \\"No one is blamed.\\", \\"answer\\": \\"no\\"}, \
            \\"only_one_side\\": {\\"reason\\": \\"Not a disputed issue.\\", \\"answer\\": \\"no\\"}}"}]}
            """;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    @Test
    void returnsTheThreeChecklistAnswersWithReasons() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key");
        apiReturns(200, JUDGE_REPLY);

        String result = tool.analyzeFraming("The crisis spirals out of control.");

        assertEquals("""
                Framing checklist results:

                1. Charged words: yes
                   'crisis spirals' is charged.

                2. Built-in blame: no
                   No one is blamed.

                3. Only one side: no
                   Not a disputed issue.

                """, result);
    }

    @Test
    void missingApiKeyReturnsErrorWithoutCallingApi() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "");

        String result = tool.analyzeFraming("some text");

        assertTrue(result.startsWith("Error"), result);
        verifyNoInteractions(httpClient);
    }

    @Test
    void errorStatusReturnsErrorMessage() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key");
        apiReturns(401, """
                {"error": {"message": "invalid x-api-key"}}
                """);

        String result = tool.analyzeFraming("some text");

        assertTrue(result.startsWith("Error: Anthropic API returned HTTP 401"), result);
    }

    private void apiReturns(int status, String body) throws Exception {
        doReturn(response).when(httpClient).send(any(), any());
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
    }
}
