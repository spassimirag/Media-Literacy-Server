package com.spassimirag.medialiteracyserver.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RubricJudgeTest {

  private static final String SAMPLE_TEXT =
      "The bus driver caused the collision on Route 4 on Friday morning.";

  private static final String VALID_ANSWERS =
      """
            {"charged_words": {"reason": "Neutral wording.", "answer": "no"}, \
            "built_in_blame": {"reason": "'caused the collision' with no source.", "answer": "yes"}, \
            "only_one_side": {"reason": "Not a disputed issue.", "answer": "no"}}""";

  @Mock private HttpClient httpClient;

  @Mock private HttpResponse<String> response;

  private static String apiBody(String stopReason, String contentBlocks) {
    return """
                {"model": "claude-sonnet-5-5", "stop_reason": "%s", "content": [%s]}"""
        .formatted(stopReason, contentBlocks);
  }

  // The judge's JSON arrives as a string inside the API's JSON, so its quotes must be escaped.
  private static String textBlock(String text) {
    return """
                {"type": "text", "text": "%s"}"""
        .formatted(text.replace("\"", "\\\""));
  }

  @Test
  void parsesAnswersOnSuccess() throws Exception {
    apiReturns(200, apiBody("end_turn", textBlock(VALID_ANSWERS)));

    JudgeResult result = judge().judge(SAMPLE_TEXT);

    assertEquals("claude-sonnet-5-5", result.model());
    assertFalse(result.chargedWords().yes());
    assertTrue(result.builtInBlame().yes());
    assertEquals("'caused the collision' with no source.", result.builtInBlame().reason());
    assertFalse(result.onlyOneSide().yes());
  }

  @Test
  void skipsThinkingBlockBeforeText() throws Exception {
    String thinking =
        """
                {"type": "thinking", "thinking": "", "signature": "abc"}""";
    apiReturns(200, apiBody("end_turn", thinking + ", " + textBlock(VALID_ANSWERS)));

    JudgeResult result = judge().judge(SAMPLE_TEXT);

    assertTrue(result.builtInBlame().yes());
  }

  @Test
  void missingApiKeyFailsWithoutCallingApi() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RubricJudge(httpClient, "", RubricJudge.DEFAULT_MODEL, "rubric"));
    verifyNoInteractions(httpClient);
  }

  @Test
  void errorStatusThrows() throws Exception {
    apiReturns(
        401,
        """
                {"error": {"message": "invalid x-api-key"}}""");

    JudgeException e = assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));

    assertTrue(e.getMessage().contains("HTTP 401"), e.getMessage());
  }

  @Test
  void networkFailureThrows() throws Exception {
    doThrow(new IOException("connection reset")).when(httpClient).send(any(), any());

    JudgeException e = assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));

    assertTrue(e.getMessage().contains("connection reset"), e.getMessage());
  }

  @Test
  void refusalThrows() throws Exception {
    apiReturns(
        200,
        """
                {"model": "claude-sonnet-5-5", "stop_reason": "refusal", \
                "stop_details": {"type": "refusal", "explanation": "declined"}, "content": []}""");

    JudgeException e = assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));

    assertTrue(e.getMessage().contains("refused"), e.getMessage());
  }

  @Test
  void truncatedReplyThrows() throws Exception {
    apiReturns(200, apiBody("max_tokens", textBlock("{\"charged_words\": {")));

    assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));
  }

  @Test
  void malformedJudgeJsonThrows() throws Exception {
    apiReturns(200, apiBody("end_turn", textBlock("Sure! Here are my answers: yes, yes, no")));

    JudgeException e = assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));

    assertTrue(e.getMessage().contains("not valid JSON"), e.getMessage());
  }

  @Test
  void invalidAnswerValueThrows() throws Exception {
    String answers = VALID_ANSWERS.replace("\"answer\": \"yes\"", "\"answer\": \"maybe\"");
    apiReturns(200, apiBody("end_turn", textBlock(answers)));

    JudgeException e = assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));

    assertTrue(e.getMessage().contains("built_in_blame"), e.getMessage());
  }

  @Test
  void missingQuestionThrows() throws Exception {
    String answers =
        """
                {"charged_words": {"reason": "Neutral.", "answer": "no"}}""";
    apiReturns(200, apiBody("end_turn", textBlock(answers)));

    assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));
  }

  @Test
  void noTextBlockThrows() throws Exception {
    apiReturns(200, apiBody("end_turn", ""));

    assertThrows(JudgeException.class, () -> judge().judge(SAMPLE_TEXT));
  }

  private RubricJudge judge() {
    return new RubricJudge(httpClient, "test-key", RubricJudge.DEFAULT_MODEL, "rubric");
  }

  private void apiReturns(int status, String body) throws Exception {
    doReturn(response).when(httpClient).send(any(), any());
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
  }
}
