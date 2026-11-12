package io.github.lelena1.flatfinder.agent;

import io.github.lelena1.flatfinder.domain.SearchCriteria;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Выбирает способ подключения Claude по настройке {@code app.agent.mode}:
 * {@code auto} (по умолчанию) — Claude API, если задан ключ, иначе Claude Code по подписке.
 */
@Service
public class AgentService {

    private final AgentProperties props;
    private final ClaudeApiAgent api;
    private final ClaudeCodeAgent claudeCode;

    public AgentService(AgentProperties props, ClaudeApiAgent api, ClaudeCodeAgent claudeCode) {
        this.props = props;
        this.api = api;
        this.claudeCode = claudeCode;
    }

    public Optional<AgentBackend> active() {
        return switch (props.mode()) {
            case "api" -> Optional.<AgentBackend>of(api).filter(AgentBackend::available);
            case "claude-code" -> Optional.<AgentBackend>of(claudeCode).filter(AgentBackend::available);
            default -> api.available() ? Optional.of(api)
                    : claudeCode.available() ? Optional.of(claudeCode)
                    : Optional.empty();
        };
    }

    public boolean available() {
        return active().isPresent();
    }

    public AgentReport run(SearchCriteria criteria, boolean webSearch) {
        return active()
                .map(backend -> backend.run(criteria, webSearch))
                .orElseGet(() -> AgentReport.failed("AI-агент не настроен: нужен Claude Code со входом по подписке "
                        + "или ключ ANTHROPIC_API_KEY. Подбор по правилам работает и без них."));
    }
}
