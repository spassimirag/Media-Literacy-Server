package com.spassimirag.medialiteracyserver.eval;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Compares the judge's saved answers with the human labels and prints how often they agree. Reads
 * only the two CSV files, so it's free to run as often as you like (no API calls).
 */
public class AgreementReport {

  static final Path HUMAN_LABELS_FILE = Path.of("eval/data/human_labels.csv");

  // The checklist questions, using the column names from the CSV files.
  static final List<String> MARKERS = List.of("charged_words", "built_in_blame", "only_one_side");

  public static void main(String[] args) throws IOException {
    print();
  }

  static void print() throws IOException {
    Map<String, CSVRecord> humanRows = loadById(AgreementReport.HUMAN_LABELS_FILE);
    Map<String, CSVRecord> judgeRows = loadById(EvalRunner.RESULTS_FILE);

    if (judgeRows.isEmpty()) {
      System.out.println("No judge labels in " + EvalRunner.RESULTS_FILE + " to compare.");
      return;
    }

    Map<String, Agreement> agreements = compare(humanRows, judgeRows);

    System.out.println();
    System.out.println(
        "Agreement with human labels ("
            + judgeRows.size()
            + " of "
            + humanRows.size()
            + " texts judged)");
    // %-16s pads the marker name to 16 characters so the columns line up.
    System.out.printf("%-16s %-8s %s%n", "Marker", "Agreed", "Percent");
    for (Map.Entry<String, Agreement> entry : agreements.entrySet()) {
      Agreement agreement = entry.getValue();
      String agreed = agreement.agreed() + "/" + agreement.total();
      System.out.printf("%-16s %-8s %.1f%%%n", entry.getKey(), agreed, agreement.percent());
    }
  }

  /**
   * Returns marker -> agreement. Only texts in the judge's file are compared, because texts the
   * judge failed on are left out of it.
   */
  static Map<String, Agreement> compare(
      Map<String, CSVRecord> humanRows, Map<String, CSVRecord> judgeRows) {
    Map<String, Agreement> agreements = new LinkedHashMap<>();

    for (String marker : MARKERS) {
      List<Boolean> humanAnswers = new ArrayList<>();
      List<Boolean> judgeAnswers = new ArrayList<>();

      for (String id : judgeRows.keySet()) {
        CSVRecord humanRow = humanRows.get(id);
        if (humanRow == null) {
          throw new IllegalStateException(
              "The judge labeled text " + id + ", which has no human label");
        }
        humanAnswers.add(isYes(humanRow.get(marker), id));
        judgeAnswers.add(isYes(judgeRows.get(id).get(marker), id));
      }

      agreements.put(marker, Agreement.of(humanAnswers, judgeAnswers));
    }
    return agreements;
  }

  static Map<String, CSVRecord> loadById(Path file) throws IOException {
    CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();

    Map<String, CSVRecord> rows = new LinkedHashMap<>();
    try (Reader reader = Files.newBufferedReader(file);
        CSVParser parser = format.parse(reader)) {
      for (CSVRecord row : parser) {
        rows.put(row.get("id"), row);
      }
    }
    return rows;
  }

  private static boolean isYes(String value, String id) {
    if (value.equals("yes")) {
      return true;
    }
    if (value.equals("no")) {
      return false;
    }
    throw new IllegalStateException(
        "Text " + id + " has \"" + value + "\" where yes or no was expected");
  }
}
