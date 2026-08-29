package com.spassimirag.medialiteracyserver;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReadabilityScoreTool {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[.!?]+");
    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");
    private static final Pattern VOWEL_GROUPS = Pattern.compile("[aeiouyAEIOUY]+");

    @McpTool(description = "Scores a piece of text for reading difficulty using the Flesch "
            + "Reading Ease and Flesch-Kincaid Grade Level formulas. Pure text statistics — "
            + "no external calls, no judgment on content, only on how hard it is to read.")
    public String scoreReadability(
            @McpToolParam(description = "The text to score, e.g. an article excerpt.", required = true)
            String text) {

        if (text == null || text.isBlank()) {
            return "Error: no text provided.";
        }

        int sentenceCount = countSentences(text);
        String[] words = WORD_SPLIT.split(text.trim());
        int wordCount = words.length;

        int syllableCount = 0;
        for (String word : words) {
            syllableCount += countSyllables(word);
        }

        double wordsPerSentence = (double) wordCount / sentenceCount;
        double syllablesPerWord = (double) syllableCount / wordCount;

        double readingEase = 206.835 - 1.015 * wordsPerSentence - 84.6 * syllablesPerWord;
        double gradeLevel = 0.39 * wordsPerSentence + 11.8 * syllablesPerWord - 15.59;

        return """
                Flesch Reading Ease: %.1f (%s)
                Flesch-Kincaid Grade Level: %.1f
                Sentences: %d, Words: %d, Syllables: %d
                """.formatted(readingEase, describeEase(readingEase), gradeLevel,
                sentenceCount, wordCount, syllableCount);
    }

    private int countSentences(String text) {
        String[] parts = SENTENCE_SPLIT.split(text.trim());
        int count = 0;
        for (String part : parts) {
            if (!part.isBlank()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    private int countSyllables(String rawWord) {
        String word = rawWord.replaceAll("[^a-zA-Z]", "").toLowerCase();
        if (word.isEmpty()) {
            return 0;
        }

        Matcher matcher = VOWEL_GROUPS.matcher(word);
        int syllables = 0;
        while (matcher.find()) {
            syllables++;
        }

        if (word.endsWith("e") && syllables > 1) {
            syllables--;
        }

        return Math.max(syllables, 1);
    }

    private String describeEase(double score) {
        if (score >= 90) return "very easy";
        if (score >= 70) return "fairly easy";
        if (score >= 50) return "standard";
        if (score >= 30) return "fairly difficult";
        return "very difficult";
    }
}