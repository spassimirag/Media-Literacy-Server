package com.spassimirag.medialiteracyserver.judge;

/**
 * The judge's answer to one checklist question.
 *
 * @param yes    true for "yes", false for "no" — same meaning as the human labels
 * @param reason the judge's short justification, kept for the error analysis
 */
public record RubricAnswer(boolean yes, String reason) {
}
