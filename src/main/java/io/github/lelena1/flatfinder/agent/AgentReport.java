package io.github.lelena1.flatfinder.agent;

/**
 * Ответ агента.
 *
 * @param markdown    итоговый текст рекомендации в Markdown
 * @param toolCalls   сколько раз агент обращался к инструментам приложения
 * @param webSearches сколько веб-поисков он выполнил
 */
public record AgentReport(boolean success, String markdown, String error, int toolCalls, int webSearches) {

    public static AgentReport ok(String markdown, int toolCalls, int webSearches) {
        return new AgentReport(true, markdown, null, toolCalls, webSearches);
    }

    public static AgentReport failed(String error) {
        return new AgentReport(false, null, error, 0, 0);
    }
}
