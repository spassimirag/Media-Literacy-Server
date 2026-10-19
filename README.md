# Media Literacy MCP Server

An [MCP](https://modelcontextprotocol.io) server that gives an AI assistant a set of *lenses* for examining how a piece of text is constructed — how hard it is to read, how it is framed, what language it leans on. It deliberately never judges whether content is **true**; it makes the construction visible and leaves the judgment with the reader.

Built with Spring Boot and Spring AI's MCP server support, speaking MCP over Streamable HTTP.

## Tools

| Tool | Type | What it does |
|------|------|--------------|
| `fetchArticle` | Deterministic | Fetches a URL and extracts readable article text (jsoup), stripped of scripts, navigation, and boilerplate. |
| `scoreReadability` | Deterministic | Scores text with the Flesch Reading Ease and Flesch–Kincaid Grade Level formulas — pure text statistics, no external calls. |
| `analyzeFraming` | LLM-backed | Checks a text against the [framing checklist](docs/framing-rubric.md) (charged words, built-in blame, only one side) using the same LLM judge the [eval](eval/README.md) measures. Returns yes/no with a reason for each question. Does not rule on truth. |

The tools are independent classes with no references to each other. Composition happens in the MCP client: given a URL, the assistant chains `fetchArticle` → `scoreReadability` → `analyzeFraming` on its own.

## Why readability formulas?

Flesch Reading Ease (1948) and Flesch–Kincaid Grade Level (1975) are regression models from reading research: two cheap-to-measure predictors — words per sentence (syntactic load) and syllables per word (vocabulary difficulty) — fitted against tested reader comprehension. Knowing that an article is pitched at a 5th-grade reading level versus a postgraduate one says something real about who it is written for and how it works on its audience. It is the deterministic counterweight to the LLM lens: cheap, reproducible, and unit-testable.

## Requirements

- Java 21+
- An [Anthropic API key](https://console.anthropic.com/) (only needed for `analyzeFraming`; the other tools work without one)

## Build

```bash
./gradlew build
```

This runs the tests and produces `build/libs/media-literacy-server-0.0.1-SNAPSHOT.jar`.

## Run and register with Claude Code

Start the server (it listens on port 8080, MCP endpoint `/mcp`):

```bash
export ANTHROPIC_API_KEY=sk-ant-...
java -jar build/libs/media-literacy-server-0.0.1-SNAPSHOT.jar
```

In another terminal, register it:

```bash
claude mcp add --transport http media-literacy http://localhost:8080/mcp
```

Then in a Claude Code session, `/mcp` shows the connection and its tool list. Try:

> fetch https://en.wikipedia.org/wiki/Media_literacy and score its readability

After changing the code, run `./gradlew build` again and restart the server.

## How a tool call flows

The server is a normal web application: Claude Code sends JSON-RPC messages as HTTP POSTs to `/mcp`. Spring AI routes each incoming `tools/call` to the matching `@McpTool` method, the same way Spring MVC routes an HTTP request to a `@GetMapping` method.

```mermaid
sequenceDiagram
    participant U as You
    participant CC as Claude Code (MCP client)
    participant JVM as media-literacy server (HTTP, port 8080)
    participant API as Anthropic API

    Note over CC,JVM: session start
    CC->>JVM: POST /mcp - initialize, then tools/list
    JVM-->>CC: fetchArticle, scoreReadability, analyzeFraming

    Note over U,JVM: "score readability on this text"
    U->>CC: prompt
    CC->>JVM: POST /mcp - tools/call scoreReadability {"text": "The cat sat..."}
    JVM->>JVM: @McpTool method runs (pure math, never leaves the JVM)
    JVM-->>CC: result "Flesch Reading Ease: 116.8 ..."
    CC-->>U: answer grounded in the tool result

    Note over JVM,API: only analyzeFraming makes an outbound call
    CC->>JVM: POST /mcp - tools/call analyzeFraming
    JVM->>API: HTTPS POST /v1/messages
    API-->>JVM: checklist answers with reasons
    JVM-->>CC: result
```

## Tests

```bash
./gradlew test
```

Deterministic tools are tested against hand-verified ground truth (known sentence/word/syllable counts and formula results). The LLM-backed tool is tested with a mocked `HttpClient` — no network, no API key.

## Project layout

```
src/main/java/.../MediaLiteracyServerApplication.java   Spring Boot entry point
src/main/java/.../ArticleFetchTool.java                 fetchArticle
src/main/java/.../ReadabilityScoreTool.java             scoreReadability
src/main/java/.../FramingAnalysisTool.java              analyzeFraming
src/main/java/.../HttpClientConfig.java                 shared HttpClient bean
src/main/resources/application.yml                      MCP server config
```

## Roadmap

- A deterministic loaded-language counter backed by a lexicon
- An `@McpPrompt` guided "analyze this article" workflow and an MCP resource exposing the lexicon
- Hardening `fetchArticle` (response size limits, content-type checks)
- Deploying the server remotely

## Status

A learning project for building MCP servers in Java — APIs and tool behavior may change freely.
