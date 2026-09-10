package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ArticleFetchToolTest {

    @Mock
    private HttpClient httpClient;

    private ArticleFetchTool tool;

    @BeforeEach
    void setUp() {
        tool = new ArticleFetchTool(httpClient);
    }

    @Test
    void extractsArticleTextAndDropsChrome() {
        String html = """
                <html><head><title>Local Cat Refuses To Move</title>
                <script>trackEverything();</script></head>
                <body>
                <nav>Home | News | Cats</nav>
                <article><p>The cat sat on the mat.</p><p>It was a hot day.</p></article>
                <footer>Copyright 2026 Cat News</footer>
                </body></html>
                """;

        String result = tool.extractReadableText(html);

        assertTrue(result.contains("Title: Local Cat Refuses To Move"), result);
        assertTrue(result.contains("The cat sat on the mat. It was a hot day."), result);
        assertFalse(result.contains("trackEverything"), result);
        assertFalse(result.contains("Home | News | Cats"), result);
        assertFalse(result.contains("Copyright"), result);
    }

    @Test
    void fallsBackToBodyWhenNoArticleElement() {
        String html = "<html><head><title>Plain Page</title></head>"
                + "<body><p>Just a paragraph.</p></body></html>";

        String result = tool.extractReadableText(html);

        assertTrue(result.contains("Just a paragraph."), result);
    }

    @Test
    void reportsErrorWhenPageHasNoText() {
        String result = tool.extractReadableText("<html><body><script>x()</script></body></html>");

        assertTrue(result.startsWith("Error"), result);
    }

    @Test
    void rejectsNonHttpUrls() throws Exception {
        assertTrue(tool.fetchArticle("ftp://example.com/file").startsWith("Error"));
        assertTrue(tool.fetchArticle("  ").startsWith("Error"));
    }
}
