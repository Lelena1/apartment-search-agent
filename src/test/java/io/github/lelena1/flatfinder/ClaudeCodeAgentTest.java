package io.github.lelena1.flatfinder;

import io.github.lelena1.flatfinder.agent.AgentProperties;
import io.github.lelena1.flatfinder.agent.AgentReport;
import io.github.lelena1.flatfinder.agent.ClaudeCodeAgentAccess;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeCodeAgentTest {

    @Test
    void parsesSuccessfulAnswer() {
        // формат --output-format stream-json: по одному JSON-событию на строку
        AgentReport r = ClaudeCodeAgentAccess.parse("""
                {"type":"system","subtype":"init"}
                {"type":"assistant","message":{"content":[{"type":"tool_use","name":"WebSearch","input":{}}]}}
                {"type":"assistant","message":{"content":[{"type":"tool_use","name":"WebSearch","input":{}},{"type":"text","text":"..."}]}}
                {"type":"assistant","message":{"content":[{"type":"tool_use","name":"Read","input":{}}]}}
                {"type":"result","is_error":false,"result":"## Лучшие варианты\\n..."}
                """);
        assertThat(r.success()).isTrue();
        assertThat(r.markdown()).startsWith("## Лучшие варианты");
        assertThat(r.toolCalls()).isEqualTo(3);
        assertThat(r.webSearches()).isEqualTo(2);
    }

    @Test
    void explainsMissingLogin() {
        AgentReport r = ClaudeCodeAgentAccess.parse("""
                {"is_error":true,"result":"Not logged in · Please run /login"}""");
        assertThat(r.success()).isFalse();
        assertThat(r.error()).contains("/login");
    }

    @Test
    void missingCliIsReportedNotThrown() {
        AgentProperties props = new AgentProperties("claude-code", null, null, null, null,
                "Z:/нет/такого/claude.exe", null);
        assertThat(ClaudeCodeAgentAccess.available(props)).isFalse();
    }
}
