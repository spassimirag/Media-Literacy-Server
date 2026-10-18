package com.spassimirag.medialiteracyserver.judge;

/**
 * The judge could not produce a usable answer for a text (API error, refusal, malformed output).
 * Checked so callers have to decide what to do: the eval runner skips the text, the MCP tool
 * returns an error message.
 */
public class JudgeException extends Exception {

  public JudgeException(String message) {
    super(message);
  }

  public JudgeException(String message, Throwable cause) {
    super(message, cause);
  }
}
