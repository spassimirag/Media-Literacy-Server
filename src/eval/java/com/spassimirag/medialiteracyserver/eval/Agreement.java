package com.spassimirag.medialiteracyserver.eval;

import java.util.List;

/**
 * How often two raters gave the same yes/no answer to one checklist question.
 *
 * @param total number of texts both raters answered
 * @param agreed number of texts where they gave the same answer
 */
public record Agreement(int total, int agreed) {

  /**
   * Builds an Agreement from two raters' raw answers by counting the texts where they match.
   * "static" means you call it on the class itself: {@code Agreement.of(humanAnswers,
   * judgeAnswers)}.
   *
   * <p>{@code rater1.get(i)} and {@code rater2.get(i)} must be answers for the same text.
   */
  public static Agreement of(List<Boolean> rater1, List<Boolean> rater2) {
    if (rater1.size() != rater2.size()) {
      throw new IllegalArgumentException(
          "Raters answered different numbers of texts: " + rater1.size() + " vs " + rater2.size());
    }
    if (rater1.isEmpty()) {
      throw new IllegalArgumentException("No answers to compare");
    }

    int total = rater1.size();
    int agreed = 0;
    for (int i = 0; i < total; i++) {
      boolean rater1Answer = rater1.get(i);
      boolean rater2Answer = rater2.get(i);

      if (rater1Answer == rater2Answer) {
        agreed++;
      }
    }

    return new Agreement(total, agreed);
  }

  public double percent() {
    return 100.0 * agreed / total;
  }
}
