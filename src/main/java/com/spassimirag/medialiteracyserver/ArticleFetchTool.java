package com.spassimirag.medialiteracyserver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ArticleFetchTool {

    private static final int MAX_CHARS = 8000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @McpTool(description = "Fetches a web page by URL and returns its readable article text, "
            + "stripped of HTML, scripts, and navigation. Use this to obtain the text that the "
            + "readability and framing tools analyze.")
    public String fetchArticle(
            @McpToolParam(description = "The full http(s) URL of the article to fetch.", required = true)
            String url) throws Exception {

        if (url == null || url.isBlank()) {
            return "Error: no URL provided.";
        }

        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return "Error: only http and https URLs are supported.";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "media-literacy-mcp-server/0.1 (article text extraction)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "Error fetching article: HTTP " + response.statusCode() + " from " + uri.getHost();
        }

        return extractReadableText(response.body());
    }

    String extractReadableText(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("script, style, noscript, nav, header, footer, aside, form").remove();

        String title = doc.title();

        Element article = doc.selectFirst("article");
        String body = (article != null ? article : doc.body()).text();

        if (body.isBlank()) {
            return "Error: no readable text found on the page.";
        }

        boolean truncated = body.length() > MAX_CHARS;
        if (truncated) {
            body = body.substring(0, MAX_CHARS);
        }

        return "Title: %s%n%n%s%s".formatted(
                title.isBlank() ? "(untitled)" : title,
                body,
                truncated ? "%n%n[truncated at %d characters]".formatted(MAX_CHARS) : "");
    }
}
