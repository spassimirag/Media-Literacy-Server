package com.spassimirag.medialiteracyserver.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgreementTest {

  @Test
  void countsTextsWhereBothRatersGaveTheSameAnswer() {
    // Texts 1, 2 and 4 match; text 3 doesn't.
    List<Boolean> human = List.of(true, false, true, false);
    List<Boolean> judge = List.of(true, false, false, false);

    Agreement agreement = Agreement.of(human, judge);

    assertEquals(4, agreement.total());
    assertEquals(3, agreement.agreed());
    assertEquals(75.0, agreement.percent(), 1e-9);
  }

  @Test
  void identicalAnswersGiveFullAgreement() {
    List<Boolean> answers = List.of(true, false, true);

    assertEquals(100.0, Agreement.of(answers, answers).percent(), 1e-9);
  }

  @Test
  void oppositeAnswersGiveNoAgreement() {
    List<Boolean> human = List.of(true, false);
    List<Boolean> judge = List.of(false, true);

    assertEquals(0.0, Agreement.of(human, judge).percent(), 1e-9);
  }

  @Test
  void rejectsListsOfDifferentLengths() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Agreement.of(List.of(true, false), List.of(true)));
  }

  @Test
  void rejectsEmptyLists() {
    assertThrows(IllegalArgumentException.class, () -> Agreement.of(List.of(), List.of()));
  }
}
