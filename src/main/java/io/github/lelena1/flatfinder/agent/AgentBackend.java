package io.github.lelena1.flatfinder.agent;

import io.github.lelena1.flatfinder.domain.SearchCriteria;

/**
 * Способ подключения Claude к приложению.
 * <ul>
 *   <li>{@link ClaudeApiAgent} — Claude API по ключу (оплата за токены), агентный цикл с инструментами;</li>
 *   <li>{@link ClaudeCodeAgent} — локальный Claude Code CLI, работает по подписке Claude Pro/Max.</li>
 * </ul>
 */
public interface AgentBackend {

    String name();

    boolean available();

    AgentReport run(SearchCriteria criteria, boolean webSearch);
}
