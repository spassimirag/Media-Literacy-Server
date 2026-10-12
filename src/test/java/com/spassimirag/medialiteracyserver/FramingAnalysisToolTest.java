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

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    @Test
    void returnsClaudeTextOnSuccess() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key");
        apiReturns(200, """
                {"content": [{"type": "text", "text": "Loaded phrase: 'crisis spirals'"}]}
                """);

        String result = tool.analyzeFraming("The crisis spirals out of control.");

        assertEquals("Loaded phrase: 'crisis spirals'", result);
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

        assertTrue(result.startsWith("Error calling Anthropic API: HTTP 401"), result);
    }

    @Test
    void emptyResponseReturnsFallbackMessage() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "test-key");
        apiReturns(200, """
                {"content": []}
                """);

        String result = tool.analyzeFraming("some text");

        assertEquals("(no text found in response)", result);
    }

    private void apiReturns(int status, String body) throws Exception {
        doReturn(response).when(httpClient).send(any(), any());
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
    }
}
