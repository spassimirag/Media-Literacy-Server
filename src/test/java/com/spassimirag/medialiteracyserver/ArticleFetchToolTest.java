package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleFetchToolTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<InputStream> response;

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

    @Test
    void refusesPrivateAndInternalAddressesWithoutFetching() throws Exception {
        String[] urls = {
                "http://127.0.0.1/",
                "http://localhost/",
                "http://[::1]/",
                "http://10.0.0.5/",
                "http://192.168.1.1/",
                "http://169.254.169.254/latest/meta-data/",
                "http://[fd00::1]/"
        };
        for (String url : urls) {
            String result = tool.fetchArticle(url);
            assertTrue(result.startsWith("Error: refusing"), url + " -> " + result);
        }
        verifyNoInteractions(httpClient);
    }

    @Test
    void refusesRedirectToInternalAddress() throws Exception {
        apiReturns(302, Map.of("Location", List.of("http://169.254.169.254/latest/meta-data/")), "");

        String result = tool.fetchArticle("http://93.184.216.34/");

        assertTrue(result.startsWith("Error: refusing"), result);
    }

    @Test
    void givesUpAfterTooManyRedirects() throws Exception {
        apiReturns(302, Map.of("Location", List.of("http://93.184.216.35/article")), "");

        String result = tool.fetchArticle("http://93.184.216.34/");

        assertTrue(result.startsWith("Error: too many redirects"), result);
    }

    @Test
    void rejectsNonHtmlContentType() throws Exception {
        apiReturns(200, Map.of("Content-Type", List.of("application/pdf")), "%PDF-1.7");

        String result = tool.fetchArticle("http://93.184.216.34/file.pdf");

        assertTrue(result.startsWith("Error: unsupported content type"), result);
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        apiReturns(200, Map.of("Content-Type", List.of("text/html")), "a".repeat(2_000_001));

        String result = tool.fetchArticle("http://93.184.216.34/big");

        assertTrue(result.startsWith("Error: page is larger than"), result);
    }

    @Test
    void returnsTextForNormalHtmlPage() throws Exception {
        apiReturns(200, Map.of("Content-Type", List.of("text/html; charset=utf-8")),
                "<html><head><title>Hi</title></head><body><p>Hello there.</p></body></html>");

        String result = tool.fetchArticle("http://93.184.216.34/");

        assertTrue(result.contains("Hello there."), result);
    }

    private void apiReturns(int status, Map<String, List<String>> headers, String body) throws Exception {
        doReturn(response).when(httpClient).send(any(), any());
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes()));
    }
}
