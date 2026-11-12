package io.github.lelena1.flatfinder.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Настройки Claude-агента.
 *
 * @param model          модель Claude
 * @param maxTurns       предел шагов агентного цикла (защита от зацикливания)
 * @param fallbacks      включить серверный фолбэк на другую модель, если основная откажется отвечать
 * @param searchDomains  сайты, по которым агенту разрешено искать в интернете
 */
@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(
        String model,
        Integer maxTurns,
        Boolean fallbacks,
        List<String> searchDomains
) {
    public AgentProperties {
        model = model == null || model.isBlank() ? "claude-opus-5-5" : model;
        maxTurns = maxTurns == null ? 12 : maxTurns;
        fallbacks = fallbacks == null || fallbacks;
        searchDomains = searchDomains == null || searchDomains.isEmpty()
                ? List.of("cian.ru", "avito.ru", "realty.yandex.ru", "domclick.ru")
                : List.copyOf(searchDomains);
    }
}
