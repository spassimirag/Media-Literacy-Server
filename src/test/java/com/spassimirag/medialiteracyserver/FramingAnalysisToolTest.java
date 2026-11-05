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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FramingAnalysisToolTest {

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
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key", 100, 10_000);
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
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "", 100, 10_000);

        String result = tool.analyzeFraming("some text");

        assertTrue(result.startsWith("Error"), result);
        verifyNoInteractions(httpClient);
    }

    @Test
    void errorStatusReturnsErrorMessage() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key", 100, 10_000);
        apiReturns(401, """
                {"error": {"message": "invalid x-api-key"}}
                """);

        String result = tool.analyzeFraming("some text");

        assertTrue(result.startsWith("Error: Anthropic API returned HTTP 401"), result);
    }

    @Test
    void textOverLengthLimitReturnsErrorWithoutCallingApi() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key", 100, 20);

        String result = tool.analyzeFraming("This sentence is longer than twenty characters.");

        assertTrue(result.startsWith("Error: text is 47 characters long; the limit is 20"), result);
        verifyNoInteractions(httpClient);
    }

    @Test
    void blankTextReturnsErrorWithoutCallingApi() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key", 100, 10_000);

        String result = tool.analyzeFraming("   ");

        assertEquals("Error: no text provided.", result);
        verifyNoInteractions(httpClient);
    }

    @Test
    void callsOverDailyLimitReturnErrorWithoutCallingApi() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key", 2, 10_000);
        apiReturns(200, JUDGE_REPLY);

        tool.analyzeFraming("First text.");
        tool.analyzeFraming("Second text.");
        String result = tool.analyzeFraming("Third text.");

        assertTrue(result.startsWith("Error: the daily limit for framing analyses has been reached"), result);
        verify(httpClient, times(2)).send(any(), any());
    }

    private void apiReturns(int status, String body) throws Exception {
        doReturn(response).when(httpClient).send(any(), any());
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
    }
}
