package io.github.lelena1.flatfinder.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Агент через локальный Claude Code CLI в неинтерактивном режиме ({@code claude -p}).
 *
 * <p>Claude Code работает по подписке Claude Pro/Max, поэтому этот режим не требует платного API-ключа.
 * Подходит для личного использования на своём компьютере: один раз установить Claude Code и войти
 * командой {@code claude} → {@code /login}. Приложение передаёт в запросе уже оценённые объявления,
 * а Claude Code может дополнительно искать в интернете своим инструментом WebSearch.
 */
@Service
public class ClaudeCodeAgent implements AgentBackend {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeAgent.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private final AgentProperties props;
    private final ListingTools tools;

    public ClaudeCodeAgent(AgentProperties props, ListingTools tools) {
        this.props = props;
        this.tools = tools;
    }

    @Override
    public String name() {
        return "Claude Code (подписка)";
    }

    @Override
    public boolean available() {
        return command().isPresent();
    }

    @Override
    public AgentReport run(SearchCriteria criteria, boolean webSearch) {
        Optional<Path> cli = command();
        if (cli.isEmpty()) {
            return AgentReport.failed("Claude Code не найден. Установите его и войдите под своей подпиской — "
                    + "инструкция в README, раздел «AI-агент по подписке».");
        }
        String prompt = AgentPrompts.withData(criteria, props.searchDomains(),
                AgentPrompts.task(criteria, webSearch), tools.snapshot(criteria));

        List<String> cmd = new ArrayList<>(List.of(cli.get().toString(),
                // stream-json: построчный журнал шагов — по нему считаем, сколько раз агент искал в интернете
                "-p", "--output-format", "stream-json", "--verbose", "--max-turns", String.valueOf(props.maxTurns()),
                // изоляция от личных настроек пользователя: без его скиллов, MCP-серверов и истории сессий,
                // чтобы ответ зависел только от запроса из формы
                "--disable-slash-commands", "--strict-mcp-config", "--no-session-persistence"));
        if (webSearch) {
            cmd.addAll(List.of("--allowedTools", "WebSearch"));
        } else {
            cmd.addAll(List.of("--disallowedTools", "WebSearch", "WebFetch"));
        }

        Path workDir = null;
        try {
            // пустая временная папка: агенту нечего читать на диске, кроме того, что мы передали
            workDir = Files.createTempDirectory("flatfinder-agent");
            ProcessBuilder pb = new ProcessBuilder(cmd).directory(workDir.toFile());
            cleanEnvironment(pb.environment());
            Process process = pb.start();

            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());
            try (OutputStream in = process.getOutputStream()) {
                in.write(prompt.getBytes(StandardCharsets.UTF_8)); // промпт через stdin: без лимита длины командной строки
            }
            if (!process.waitFor(props.claudeCodeTimeoutSec(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return AgentReport.failed("Claude Code не ответил за " + props.claudeCodeTimeoutSec() + " с.");
            }
            String out = stdout.get(10, TimeUnit.SECONDS);
            if (out.isBlank()) {
                return AgentReport.failed("Claude Code завершился без ответа: " + stderr.get(5, TimeUnit.SECONDS).strip());
            }
            return parseResult(out);
        } catch (IOException e) {
            return AgentReport.failed("Не удалось запустить Claude Code: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Ошибка при работе с Claude Code", e);
            return AgentReport.failed("Ошибка при работе с Claude Code: " + e.getMessage());
        } finally {
            deleteQuietly(workDir);
        }
    }

    /**
     * Разбирает вывод {@code claude -p --output-format stream-json}: по строке JSON на событие.
     * Из сообщений ассистента считаем вызовы инструментов, итог берём из последнего события {@code result}.
     */
    static AgentReport parseResult(String output) {
        JsonNode result = null;
        int toolCalls = 0;
        int webSearches = 0;
        for (String line : output.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode event;
            try {
                event = MAPPER.readTree(line);
            } catch (IOException e) {
                continue; // посторонняя строка в выводе — пропускаем
            }
            if ("assistant".equals(event.path("type").asText())) {
                for (JsonNode block : event.path("message").path("content")) {
                    if ("tool_use".equals(block.path("type").asText())) {
                        toolCalls++;
                        String name = block.path("name").asText();
                        if (name.equals("WebSearch") || name.equals("WebFetch")) {
                            webSearches++;
                        }
                    }
                }
            } else if (event.has("result") || "result".equals(event.path("type").asText())) {
                result = event;
            }
        }
        if (result == null) {
            return AgentReport.failed("Не удалось разобрать ответ Claude Code");
        }
        String text = result.path("result").asText("");
        if (result.path("is_error").asBoolean(false)) {
            if (text.toLowerCase().contains("not logged in") || text.contains("/login")) {
                return AgentReport.failed("Claude Code установлен, но вход не выполнен: откройте терминал, "
                        + "запустите claude и выполните /login под своей подпиской.");
            }
            return AgentReport.failed("Claude Code вернул ошибку: " + text);
        }
        return AgentReport.ok(text, toolCalls, webSearches);
    }

    /** Путь к CLI: из настройки или поиск {@code claude} в PATH. */
    Optional<Path> command() {
        if (!props.claudeCodeCommand().isBlank()) {
            Path p = Path.of(props.claudeCodeCommand());
            return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
        }
        List<String> dirs = new ArrayList<>();
        String path = System.getenv("PATH");
        if (path != null) {
            dirs.addAll(List.of(path.split(File.pathSeparator)));
        }
        // стандартное место установки Claude Code — на случай, если PATH ещё не обновился
        dirs.add(Path.of(System.getProperty("user.home"), ".local", "bin").toString());
        List<String> names = WINDOWS ? List.of("claude.exe", "claude.cmd") : List.of("claude");
        for (String dir : dirs) {
            for (String name : names) {
                try {
                    Path candidate = Path.of(dir.trim(), name);
                    if (Files.isRegularFile(candidate)) {
                        return Optional.of(candidate);
                    }
                } catch (RuntimeException ignored) {
                    // некорректный элемент PATH — пропускаем
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Если приложение само запущено из сессии Claude Code, дочерний процесс унаследует её служебные
     * переменные и поведёт себя как вложенная сессия. Убираем их, чтобы CLI работал со своим входом.
     */
    private static void cleanEnvironment(Map<String, String> env) {
        boolean nested = env.containsKey("CLAUDECODE");
        env.keySet().removeIf(k -> k.equals("CLAUDECODE") || k.startsWith("CLAUDE_CODE_") || k.startsWith("CLAUDE_AGENT_SDK"));
        if (nested) {
            env.remove("ANTHROPIC_BASE_URL");
        }
    }

    private static CompletableFuture<String> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // временная папка — не критично
        }
    }
}
