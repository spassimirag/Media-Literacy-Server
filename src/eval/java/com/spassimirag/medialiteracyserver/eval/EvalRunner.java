package com.spassimirag.medialiteracyserver.eval;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvalRunner {

    private static final Path TEXTS_FILE = Path.of("eval/data/texts.csv");
    private static final Path RUBRIC_FILE = Path.of("docs/framing-rubric.md");
    static final Path RESULTS_FILE = Path.of("eval/results/judge_labels.csv");

    public static void main(String[] args) throws IOException {
        List<EvalText> texts = loadTexts(TEXTS_FILE);
        System.out.println("Loaded " + texts.size() + " texts");

        RubricJudge judge = new RubricJudge(
                HttpClient.newHttpClient(),
                System.getenv("ANTHROPIC_API_KEY"),
                System.getenv().getOrDefault("JUDGE_MODEL", RubricJudge.DEFAULT_MODEL),
                Files.readString(RUBRIC_FILE));
        Map<String, JudgeResult> results = judgeAll(judge, texts);
        writeResults(RESULTS_FILE, results);

        System.out.println("Wrote " + results.size() + " of " + texts.size() + " texts to " + RESULTS_FILE);

        AgreementReport.print();
    }

    static List<EvalText> loadTexts(Path file) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        List<EvalText> texts = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord row : parser) {
                texts.add(new EvalText(row.get("id"), row.get("text")));
            }
        }
        return texts;
    }

    // Returns text id -> judge result. Texts the judge failed on are logged and left out,
    // so one failure doesn't stop the whole run.
    private static Map<String, JudgeResult> judgeAll(RubricJudge judge, List<EvalText> texts) {
        Map<String, JudgeResult> results = new LinkedHashMap<>();  // keeps the texts' order
        for (EvalText text : texts) {
            try {
                results.put(text.id(), judge.judge(text.text()));
                System.out.println(text.id() + " done");
            } catch (JudgeException e) {
                System.out.println(text.id() + " FAILED: " + e.getMessage());
            }
        }
        return results;
    }

    // Same columns as human_labels.csv first, then run details and the judge's reasons.
    private static void writeResults(Path file, Map<String, JudgeResult> results) throws IOException {
        String runAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        Files.createDirectories(file.getParent());

        try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(file), CSVFormat.DEFAULT)) {
            csv.printRecord("id", "charged_words", "built_in_blame", "only_one_side", "model", "run_at",
                    "charged_words_reason", "built_in_blame_reason", "only_one_side_reason");

            for (Map.Entry<String, JudgeResult> entry : results.entrySet()) {
                JudgeResult result = entry.getValue();
                csv.printRecord(
                        entry.getKey(),
                        yesNo(result.chargedWords()),
                        yesNo(result.builtInBlame()),
                        yesNo(result.onlyOneSide()),
                        result.model(),
                        runAt,
                        result.chargedWords().reason(),
                        result.builtInBlame().reason(),
                        result.onlyOneSide().reason());
            }
        }
    }

    private static String yesNo(RubricAnswer answer) {
        return answer.yes() ? "yes" : "no";
    }

    record EvalText(String id, String text) {
    }
}
