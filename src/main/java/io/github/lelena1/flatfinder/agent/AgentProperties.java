package io.github.lelena1.flatfinder.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Настройки Claude-агента.
 *
 * @param mode                 auto — Claude API, если задан ключ, иначе Claude Code; api; claude-code
 * @param model                модель Claude для режима API
 * @param maxTurns             предел шагов агентного цикла (защита от зацикливания)
 * @param fallbacks            серверный фолбэк на другую модель, если основная откажется отвечать (режим API)
 * @param searchDomains        сайты, по которым агенту разрешено искать в интернете
 * @param claudeCodeCommand    путь к Claude Code CLI; пусто — искать {@code claude} в PATH
 * @param claudeCodeTimeoutSec сколько ждать ответа Claude Code
 */
@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(
        String mode,
        String model,
        Integer maxTurns,
        Boolean fallbacks,
        List<String> searchDomains,
        String claudeCodeCommand,
        Integer claudeCodeTimeoutSec
) {
    public AgentProperties {
        mode = mode == null || mode.isBlank() ? "auto" : mode.trim().toLowerCase();
        model = model == null || model.isBlank() ? "claude-opus-5-5" : model;
        maxTurns = maxTurns == null ? 12 : maxTurns;
        fallbacks = fallbacks == null || fallbacks;
        searchDomains = searchDomains == null || searchDomains.isEmpty()
                ? List.of("cian.ru", "avito.ru", "realty.yandex.ru", "domclick.ru")
                : List.copyOf(searchDomains);
        claudeCodeCommand = claudeCodeCommand == null ? "" : claudeCodeCommand.trim();
        claudeCodeTimeoutSec = claudeCodeTimeoutSec == null ? 300 : claudeCodeTimeoutSec;
    }
}
