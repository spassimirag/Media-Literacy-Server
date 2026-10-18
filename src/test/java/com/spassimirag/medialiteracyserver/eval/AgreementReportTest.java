package com.spassimirag.medialiteracyserver.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgreementReportTest {

  private static final String HEADER = "id,charged_words,built_in_blame,only_one_side\n";

  @TempDir private Path tempDir;

  private Map<String, CSVRecord> csv(String name, String rows) throws IOException {
    Path file = tempDir.resolve(name);
    Files.writeString(file, HEADER + rows);
    return AgreementReport.loadById(file);
  }

  @Test
  void comparesEachMarkerSeparately() throws IOException {
    Map<String, CSVRecord> human = csv("human.csv", "001,yes,no,yes\n002,no,no,no\n");
    Map<String, CSVRecord> judge = csv("judge.csv", "001,yes,yes,yes\n002,no,no,yes\n");

    Map<String, Agreement> agreements = AgreementReport.compare(human, judge);

    assertEquals(2, agreements.get("charged_words").agreed());
    assertEquals(1, agreements.get("built_in_blame").agreed());
    assertEquals(1, agreements.get("only_one_side").agreed());
  }

  @Test
  void leavesOutTextsTheJudgeFailedOn() throws IOException {
    Map<String, CSVRecord> human = csv("human.csv", "001,yes,no,yes\n002,no,no,no\n");
    Map<String, CSVRecord> judge = csv("judge.csv", "002,no,no,no\n");

    Agreement agreement = AgreementReport.compare(human, judge).get("charged_words");

    assertEquals(1, agreement.total());
    assertEquals(1, agreement.agreed());
  }

  @Test
  void rejectsJudgeTextWithNoHumanLabel() throws IOException {
    Map<String, CSVRecord> human = csv("human.csv", "001,yes,no,yes\n");
    Map<String, CSVRecord> judge = csv("judge.csv", "099,yes,no,yes\n");

    assertThrows(IllegalStateException.class, () -> AgreementReport.compare(human, judge));
  }

  @Test
  void rejectsAnswersOtherThanYesOrNo() throws IOException {
    Map<String, CSVRecord> human = csv("human.csv", "001,Yes,no,yes\n");
    Map<String, CSVRecord> judge = csv("judge.csv", "001,yes,no,yes\n");

    assertThrows(IllegalStateException.class, () -> AgreementReport.compare(human, judge));
  }
}
