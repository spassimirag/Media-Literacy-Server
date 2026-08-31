package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadabilityScoreToolTest {

    private final ReadabilityScoreTool tool = new ReadabilityScoreTool();

    @Test
    void easyParagraphMatchesHandVerifiedGroundTruth() {
        String easy = "The cat sat on the mat. It was a hot day. "
                + "The cat did not want to move. A dog ran past. The cat did not care.";

        String result = tool.scoreReadability(easy);

        assertTrue(result.contains("Sentences: 5, Words: 27, Syllables: 27"), result);
        assertTrue(result.contains("Flesch Reading Ease: 116.8 (very easy)"), result);
        assertTrue(result.contains("Flesch-Kincaid Grade Level: -1.7"), result);
    }

    @Test
    void blankTextReturnsError() {
        assertTrue(tool.scoreReadability("   ").startsWith("Error"));
    }
}
