package com.spassimirag.medialiteracyserver.eval;

/**
 * The judge's answers for one text, one per checklist question in docs/framing-rubric.md.
 *
 * @param model the model that actually produced the answers, as reported by the API
 */
public record JudgeResult(String model, RubricAnswer chargedWords, RubricAnswer builtInBlame, RubricAnswer onlyOneSide) {
}
