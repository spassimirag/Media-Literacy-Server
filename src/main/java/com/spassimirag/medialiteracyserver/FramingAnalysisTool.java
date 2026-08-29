package com.spassimirag.medialiteracyserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class FramingAnalysisTool {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_KEY = System.getenv("ANTHROPIC_API_KEY");
    private static final String MODEL = "claude-sonnet-5";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @McpTool(description = "Analyzes a piece of text for framing patterns — loaded language, "
            + "one-sided phrasing, emotionally charged wording — without judging whether the "
            + "content itself is true or false.")
    public String analyzeFraming(
            @McpToolParam(description = "The text to analyze, e.g. an article excerpt or a social media post.", required = true)
            String text) throws Exception {

        if (API_KEY == null || API_KEY.isBlank()) {
            return "Error: ANTHROPIC_API_KEY environment variable is not set.";
        }

        String prompt = """
                You are a media literacy assistant. Analyze the following text for framing \
                patterns only — loaded language, one-sided phrasing, emotionally charged \
                wording, or missing context. Do NOT judge whether the content is true, false, \
                or misleading. List 2-4 specific observations, each tied to a phrase from the \
                text. If the text reads as neutral in tone, say so plainly.
 
                TEXT:
                %s
                """.formatted(text);

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 500);
        requestBody.putArray("messages").add(message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("x-api-key", API_KEY)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "Error calling Anthropic API: HTTP " + response.statusCode() + " — " + response.body();
        }

        JsonNode root = mapper.readTree(response.body());
        return root.at("/content/0/text").asText("(no text found in response)");
    }
}

