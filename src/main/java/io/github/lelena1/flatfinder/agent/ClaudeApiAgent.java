package io.github.lelena1.flatfinder.agent;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.models.messages.WebSearchTool20260209;
import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Агент подбора квартиры на Claude API.
 *
 * <p>Классический агентный цикл: модель получает задачу и инструменты, сама решает, какие объявления
 * смотреть подробно, и, если разрешено, ищет свежие объявления в интернете (серверный инструмент
 * web search, ограниченный сайтами недвижимости). Цикл продолжается, пока модель вызывает инструменты.
 */
@Service
public class ClaudeApiAgent implements AgentBackend {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiAgent.class);

    private final AgentProperties props;
    private final ListingTools tools;
    private volatile AnthropicClient client;

    public ClaudeApiAgent(AgentProperties props, ListingTools tools) {
        this.props = props;
        this.tools = tools;
    }

    @Override
    public String name() {
        return "Claude API";
    }

    /** Агент доступен, если в окружении есть ключ Claude API. */
    @Override
    public boolean available() {
        return notBlank(System.getenv("ANTHROPIC_API_KEY")) || notBlank(System.getenv("ANTHROPIC_AUTH_TOKEN"));
    }

    @Override
    public AgentReport run(SearchCriteria criteria, boolean webSearch) {
        if (!available()) {
            return AgentReport.failed("Не задан ключ Claude API (переменная окружения ANTHROPIC_API_KEY). "
                    + "Подбор по правилам работает и без него.");
        }
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(props.model())
                .maxTokens(16000L)
                .system(AgentPrompts.system(criteria, props.searchDomains()))
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
                .addUserMessage(AgentPrompts.task(criteria, webSearch));
        tools.specs().values().forEach(spec -> params.addTool(toSdkTool(spec)));
        if (webSearch) {
            params.addTool(WebSearchTool20260209.builder()
                    .maxUses(5L)
                    .allowedDomains(props.searchDomains())
                    .build());
        }
        if (props.fallbacks()) {
            // Если модель откажется отвечать, сервер повторит запрос на подходящей модели-замене.
            params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01");
            params.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }

        int toolCalls = 0;
        int webSearches = 0;
        try {
            for (int turn = 0; turn < props.maxTurns(); turn++) {
                Message response = client().messages().create(params.build());
                params.addMessage(response);

                webSearches += (int) response.content().stream().filter(ContentBlock::isServerToolUse).count();
                StopReason stop = response.stopReason().orElse(StopReason.END_TURN);

                if (StopReason.REFUSAL.equals(stop)) {
                    return AgentReport.failed("Модель отказалась выполнять запрос.");
                }
                if (StopReason.PAUSE_TURN.equals(stop)) {
                    continue; // долгий серверный поиск — продолжаем тот же ход
                }
                if (StopReason.TOOL_USE.equals(stop)) {
                    List<ContentBlockParam> results = new ArrayList<>();
                    for (ContentBlock block : response.content()) {
                        if (block.isToolUse()) {
                            toolCalls++;
                            results.add(execute(block.asToolUse(), criteria));
                        }
                    }
                    params.addUserMessageOfContentBlockParams(results);
                    continue;
                }
                String text = response.content().stream()
                        .flatMap(b -> b.text().stream())
                        .map(TextBlock::text)
                        .collect(Collectors.joining("\n"));
                if (StopReason.MAX_TOKENS.equals(stop)) {
                    text += "\n\n_(ответ обрезан по лимиту длины)_";
                }
                return AgentReport.ok(text, toolCalls, webSearches);
            }
            return AgentReport.failed("Агент не уложился в " + props.maxTurns() + " шагов.");
        } catch (UnauthorizedException e) {
            return AgentReport.failed("Ключ Claude API не подошёл: " + e.getMessage());
        } catch (RateLimitException e) {
            return AgentReport.failed("Превышен лимит запросов к Claude API, попробуйте через минуту.");
        } catch (AnthropicServiceException e) {
            log.warn("Ошибка Claude API", e);
            return AgentReport.failed("Ошибка Claude API (" + e.statusCode() + "): " + e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Агент упал", e);
            return AgentReport.failed("Не удалось связаться с Claude API: " + e.getMessage());
        }
    }

    private ContentBlockParam execute(ToolUseBlock call, SearchCriteria criteria) {
        String result;
        boolean error = false;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> input = call._input().convert(Map.class);
            result = switch (call.name()) {
                case ListingTools.SEARCH -> tools.search(criteria, Boolean.TRUE.equals(input.get("include_rejected")));
                case ListingTools.DETAILS -> {
                    Object id = input.get("id");
                    if (!(id instanceof String s) || s.isBlank()) {
                        throw new IllegalArgumentException("нужен параметр id");
                    }
                    yield tools.details(criteria, s);
                }
                default -> throw new IllegalArgumentException("неизвестный инструмент " + call.name());
            };
        } catch (RuntimeException e) {
            result = "Ошибка: " + e.getMessage();
            error = true;
        }
        return ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(call.id())
                .content(result)
                .isError(error)
                .build());
    }

    private static Tool toSdkTool(ListingTools.ToolSpec spec) {
        Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
        spec.properties().forEach((name, schema) -> properties.putAdditionalProperty(name, JsonValue.from(schema)));
        return Tool.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(Tool.InputSchema.builder()
                        .properties(properties.build())
                        .required(spec.required())
                        .build())
                .build();
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.fromEnv();
                }
            }
        }
        return client;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
