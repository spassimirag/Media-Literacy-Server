package com.spassimirag.medialiteracyserver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class ArticleFetchTool {

    private static final int MAX_CHARS = 8000;
    private static final int MAX_BYTES = 2_000_000;
    private static final int MAX_REDIRECTS = 5;

    private final HttpClient httpClient;

    public ArticleFetchTool(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

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

        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            String problem = checkUri(uri);
            if (problem != null) {
                return problem;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "media-literacy-mcp-server/0.1 (article text extraction)")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null) {
                        return "Error: redirect without a Location header.";
                    }
                    uri = uri.resolve(location);
                    continue;
                }
                if (status != 200) {
                    return "Error fetching article: HTTP " + status + " from " + uri.getHost();
                }

                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (!isTextContent(contentType)) {
                    return "Error: unsupported content type '" + contentType + "'. Only HTML and plain text are supported.";
                }

                byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) {
                    return "Error: page is larger than " + MAX_BYTES + " bytes.";
                }
                return extractReadableText(new String(bytes, StandardCharsets.UTF_8));
            }
        }
        return "Error: too many redirects.";
    }

    private static boolean isTextContent(String contentType) {
        String type = contentType.toLowerCase();
        return type.startsWith("text/html")
                || type.startsWith("application/xhtml+xml")
                || type.startsWith("text/plain");
    }

    String checkUri(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return "Error: only http and https URLs are supported.";
        }
        if (uri.getHost() == null) {
            return "Error: URL has no host.";
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            return "Error: could not resolve host " + uri.getHost() + ".";
        }
        for (InetAddress address : addresses) {
            if (isInternal(address)) {
                return "Error: refusing to fetch private or internal address.";
            }
        }
        return null;
    }

    private static boolean isInternal(InetAddress address) {
        boolean uniqueLocalIpv6 = address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc;
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || uniqueLocalIpv6;
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
