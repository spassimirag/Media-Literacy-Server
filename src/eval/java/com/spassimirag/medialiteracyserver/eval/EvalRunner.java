package com.spassimirag.medialiteracyserver.eval;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EvalRunner {

    private static final Path TEXTS_FILE = Path.of("eval/data/texts.csv");
    private static final Path RUBRIC_FILE = Path.of("docs/framing-rubric.md");

    public static void main(String[] args) throws IOException {
        List<EvalText> texts = loadTexts(TEXTS_FILE);
        System.out.println("Loaded " + texts.size() + " texts");

        RubricJudge judge = new RubricJudge(
                HttpClient.newHttpClient(),
                System.getenv("ANTHROPIC_API_KEY"),
                System.getenv().getOrDefault("JUDGE_MODEL", RubricJudge.DEFAULT_MODEL),
                Files.readString(RUBRIC_FILE));

        for (EvalText text : texts) {
            try {
                JudgeResult result = judge.judge(text.text());
                System.out.println(text.id()
                        + " charged_words=" + result.chargedWords().yes()
                        + " built_in_blame=" + result.builtInBlame().yes()
                        + " only_one_side=" + result.onlyOneSide().yes());
            } catch (JudgeException e) {
                // One failed text shouldn't stop the whole run.
                System.out.println(text.id() + " FAILED: " + e.getMessage());
            }
        }
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

    record EvalText(String id, String text) {
    }
}
