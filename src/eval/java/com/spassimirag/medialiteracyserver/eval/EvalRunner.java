package com.spassimirag.medialiteracyserver.eval;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EvalRunner {

    private static final Path TEXTS_FILE = Path.of("eval/data/texts.csv");

    public static void main(String[] args) throws IOException {
        List<EvalText> texts = loadTexts(TEXTS_FILE);
        System.out.println("Loaded " + texts.size() + " texts");
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
