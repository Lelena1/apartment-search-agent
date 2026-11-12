package io.github.lelena1.flatfinder.agent;

/** Доступ тестов к package-private методам {@link ClaudeCodeAgent}. */
public final class ClaudeCodeAgentAccess {

    private ClaudeCodeAgentAccess() {
    }

    public static AgentReport parse(String json) {
        return ClaudeCodeAgent.parseResult(json);
    }

    public static boolean available(AgentProperties props) {
        return new ClaudeCodeAgent(props, null).available();
    }
}
