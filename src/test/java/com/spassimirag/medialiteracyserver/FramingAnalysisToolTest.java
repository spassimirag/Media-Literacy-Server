package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(
                "{\"content\":[{\"type\":\"text\",\"text\":\"Loaded phrase: 'crisis spirals'\"}]}");

        String result = tool.analyzeFraming("The crisis spirals out of control.");

        assertEquals("Loaded phrase: 'crisis spirals'", result);
    }

    @Test
    void missingApiKeyReturnsErrorWithoutCallingApi() throws Exception {
        FramingAnalysisTool tool = new FramingAnalysisTool(httpClient, "");

        String result = tool.analyzeFraming("some text");

        assertTrue(result.startsWith("Error"), result);
    }
}
