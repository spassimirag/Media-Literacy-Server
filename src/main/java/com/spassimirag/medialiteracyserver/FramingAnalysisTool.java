package com.spassimirag.medialiteracyserver;

import com.spassimirag.medialiteracyserver.judge.JudgeException;
import com.spassimirag.medialiteracyserver.judge.JudgeResult;
import com.spassimirag.medialiteracyserver.judge.RubricAnswer;
import com.spassimirag.medialiteracyserver.judge.RubricJudge;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

@Service
public class FramingAnalysisTool {

    // Copied from docs/framing-rubric.md at build time (see processResources in build.gradle).
    private static final String RUBRIC_RESOURCE = "framing-rubric.md";

    // Null when there's no API key, so the tool can explain the problem instead of failing at startup.
    private final RubricJudge judge;

    public FramingAnalysisTool(HttpClient httpClient,
                               @Value("${ANTHROPIC_API_KEY:}") String apiKey) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            this.judge = null;
        } else {
            String rubric = new ClassPathResource(RUBRIC_RESOURCE).getContentAsString(StandardCharsets.UTF_8);
            this.judge = new RubricJudge(httpClient, apiKey, RubricJudge.DEFAULT_MODEL, rubric);
        }
    }

    @McpTool(description = "Checks a text against a three-question framing checklist: charged words, "
            + "built-in blame, and only one side. Returns yes or no for each question, with a short reason "
            + "quoting the text. Does not judge whether the content itself is true or false.")
    public String analyzeFraming(
            @McpToolParam(description = "The text to analyze, e.g. an article excerpt or a social media post.", required = true)
            String text) {

        if (judge == null) {
            return "Error: ANTHROPIC_API_KEY environment variable is not set.";
        }

        JudgeResult result;
        try {
            result = judge.judge(text);
        } catch (JudgeException e) {
            return "Error: " + e.getMessage();
        }

        return "Framing checklist results:\n\n"
                + formatAnswer(1, "Charged words", result.chargedWords())
                + formatAnswer(2, "Built-in blame", result.builtInBlame())
                + formatAnswer(3, "Only one side", result.onlyOneSide());
    }

    private static String formatAnswer(int number, String question, RubricAnswer answer) {
        String yesNo;
        if (answer.yes()) {
            yesNo = "yes";
        } else {
            yesNo = "no";
        }
        return number + ". " + question + ": " + yesNo + "\n   " + answer.reason() + "\n\n";
    }
}
