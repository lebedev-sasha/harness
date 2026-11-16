package ru.sashalebedev.harness.tool;

import org.junit.jupiter.api.Test;
import ru.sashalebedev.harness.agent.RecordingEventPublisher;
import ru.sashalebedev.harness.agent.event.AgentEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    private final RecordingEventPublisher events = new RecordingEventPublisher();

    private ToolRegistry registry(Tool<?>... tools) {
        return new ToolRegistry(List.of(tools), events);
    }

    @Test
    void executesToolAndReportsStartAndFinish() {
        ToolRegistry registry = registry(StubTool.replying("Echo", ToolResult.ok("эхо")));

        ToolResult result = registry.execute("Echo", "{}");

        assertThat(result.error()).isFalse();
        assertThat(result.content()).isEqualTo("эхо");

        assertThat(events.events()).hasSize(2);
        assertThat(events.events().get(0))
                .isInstanceOfSatisfying(
                        AgentEvent.ToolCallStarted.class,
                        started -> {
                            assertThat(started.toolName()).isEqualTo("Echo");
                            assertThat(started.description()).isEqualTo("использован Echo");
                        });
        assertThat(events.events().get(1))
                .isInstanceOfSatisfying(
                        AgentEvent.ToolCallFinished.class,
                        finished -> {
                            assertThat(finished.toolName()).isEqualTo("Echo");
                            assertThat(finished.error()).isFalse();
                            assertThat(finished.preview()).isEqualTo("эхо");
                        });
    }

    @Test
    void unknownToolReturnsErrorListingAvailableTools() {
        ToolRegistry registry = registry(StubTool.replying("Echo", ToolResult.ok("эхо")));

        ToolResult result = registry.execute("НетТакого", "{}");

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("нет инструмента 'НетТакого'").contains("Echo");
        assertThat(events.events()).isEmpty();
    }

    @Test
    void malformedJsonBecomesToolError() {
        ToolRegistry registry = registry(StubTool.replying("Echo", ToolResult.ok("эхо")));

        ToolResult result = registry.execute("Echo", "{не json");

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("не разобрать");
    }

    @Test
    void failingToolReturnsErrorInsteadOfThrowing() {
        ToolRegistry registry = registry(StubTool.crashing("Echo"));

        ToolResult result = registry.execute("Echo", "{}");

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("упал");
    }

    @Test
    void duplicateToolNamesAreRejected() {
        assertThatThrownBy(() -> registry(
                StubTool.replying("Echo", ToolResult.ok("первый")),
                StubTool.replying("Echo", ToolResult.ok("второй"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("два инструмента с именем 'Echo'");
    }
}
