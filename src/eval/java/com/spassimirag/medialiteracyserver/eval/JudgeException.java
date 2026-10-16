package com.spassimirag.medialiteracyserver.eval;

/**
 * The judge could not produce a usable answer for a text (API error, refusal, malformed output).
 * Checked so the eval runner has to decide what to do per text instead of aborting the run.
 */
public class JudgeException extends Exception {

  public JudgeException(String message) {
    super(message);
  }

  public JudgeException(String message, Throwable cause) {
    super(message, cause);
  }
}
