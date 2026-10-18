package com.spassimirag.medialiteracyserver.judge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LLM judge that answers the three framing checklist questions for a text. Used both by the
 * analyzeFraming MCP tool and by the eval, which compares its answers with the human labels.
 */
public class RubricJudge {

    public static final String DEFAULT_MODEL = "claude-sonnet-5-5";

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    // The JSON shape the API must reply with (structured outputs). Question keys match the
    // columns in eval/data/human_labels.csv.
    private static final String ANSWER_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "charged_words": {
                  "type": "object",
                  "properties": {
                    "reason": {"type": "string"},
                    "answer": {"type": "string", "enum": ["yes", "no"]}
                  },
                  "required": ["reason", "answer"],
                  "additionalProperties": false
                },
                "built_in_blame": {
                  "type": "object",
                  "properties": {
                    "reason": {"type": "string"},
                    "answer": {"type": "string", "enum": ["yes", "no"]}
                  },
                  "required": ["reason", "answer"],
                  "additionalProperties": false
                },
                "only_one_side": {
                  "type": "object",
                  "properties": {
                    "reason": {"type": "string"},
                    "answer": {"type": "string", "enum": ["yes", "no"]}
                  },
                  "required": ["reason", "answer"],
                  "additionalProperties": false
                }
              },
              "required": ["charged_words", "built_in_blame", "only_one_side"],
              "additionalProperties": false
            }
            """;

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final ObjectMapper mapper = new ObjectMapper();

    public RubricJudge(HttpClient httpClient, String apiKey, String model, String rubric) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("ANTHROPIC_API_KEY is not set");
        }
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = """
                You are labeling short news-style texts against a framing checklist. Apply the \
                checklist below exactly as written, including its rules for borderline cases. \
                Judge only how the text is framed, never whether it is true.

                For each question, first give a one- or two-sentence reason that quotes the \
                relevant words from the text, then answer "yes" or "no".

                <checklist>
                %s
                </checklist>
                """.formatted(rubric);
    }

    public JudgeResult judge(String text) throws JudgeException {
        HttpResponse<String> response;
        try {
            response = httpClient.send(buildRequest(text), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new JudgeException("Request to Anthropic API failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Interrupted while calling Anthropic API", e);
        }

        if (response.statusCode() != 200) {
            throw new JudgeException("Anthropic API returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return parseResponse(response.body());
    }

    private HttpRequest buildRequest(String text) throws JudgeException {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", "<text>\n" + text + "\n</text>");

        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 16000);
        requestBody.put("system", systemPrompt);
        requestBody.putArray("messages").add(message);
        ObjectNode format = requestBody.putObject("output_config").putObject("format");
        format.put("type", "json_schema");
        format.set("schema", readJson(ANSWER_SCHEMA, "Answer schema is not valid JSON"));

        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();
        } catch (JsonProcessingException e) {
            throw new JudgeException("Could not serialize request", e);
        }
    }

    JudgeResult parseResponse(String body) throws JudgeException {
        JsonNode root = readJson(body, "API response is not valid JSON");
        checkStopReason(root);
        String answerJson = findTextBlock(root);
        JsonNode answers = readJson(answerJson, "Judge output is not valid JSON: " + answerJson);

        return new JudgeResult(
                root.path("model").asText(model),
                readAnswer(answers, "charged_words"),
                readAnswer(answers, "built_in_blame"),
                readAnswer(answers, "only_one_side"));
    }

    private JsonNode readJson(String json, String errorMessage) throws JudgeException {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new JudgeException(errorMessage, e);
        }
    }

    // A refusal or truncated reply may not match the schema, so check before parsing.
    private void checkStopReason(JsonNode root) throws JudgeException {
        String stopReason = root.path("stop_reason").asText();
        if (stopReason.equals("refusal")) {
            throw new JudgeException("Model refused: " + root.path("stop_details").path("explanation").asText("no details"));
        }
        if (stopReason.equals("max_tokens")) {
            throw new JudgeException("Reply was cut off at max_tokens");
        }
    }

    // With thinking on, thinking blocks come before the text block, so search for it.
    private String findTextBlock(JsonNode root) throws JudgeException {
        for (JsonNode block : root.path("content")) {
            if (block.path("type").asText().equals("text")) {
                return block.path("text").asText();
            }
        }
        throw new JudgeException("No text block in API response");
    }

    private RubricAnswer readAnswer(JsonNode answers, String question) throws JudgeException {
        JsonNode node = answers.path(question);
        String answer = node.path("answer").asText();
        if (!answer.equals("yes") && !answer.equals("no")) {
            throw new JudgeException("Missing or invalid answer for " + question + ": " + node);
        }
        return new RubricAnswer(answer.equals("yes"), node.path("reason").asText());
    }
}
