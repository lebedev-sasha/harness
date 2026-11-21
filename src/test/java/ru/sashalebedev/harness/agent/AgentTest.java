package ru.sashalebedev.harness.agent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import ru.sashalebedev.harness.agent.event.AgentEvent;
import ru.sashalebedev.harness.tool.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTest {
    private final RecordingEventPublisher events = new RecordingEventPublisher();

    /** Агент с полным набором зависимостей: реестр настоящий, адаптер — заглушка. */
    private Agent agent(ScriptedChatModel model, List<Tool<?>> tools, AgentProperties properties) {
        SpringAiToolAdapter adapter = Mockito.mock(SpringAiToolAdapter.class);
        Mockito.when(adapter.toolCallbacks()).thenReturn(List.of());
        return new Agent(model, events, new ToolRegistry(tools, events), adapter, properties);
    }

    private Agent agent(ScriptedChatModel model) {
        return agent(model, List.of(), new AgentProperties(20, "."));
    }

    @Test
    void returnsModelAnswer() {
        Agent agent = agent(ScriptedChatModel.replyingWith("I am Harness."));

        assertThat(agent.run("Кто ты?")).isEqualTo("I am Harness.");
    }

    @Test
    void systemPromptIsAlwaysTheFirstMessage() {
        ScriptedChatModel model = ScriptedChatModel.replyingWith("ok");
        Agent agent = agent(model);

        agent.run("Привет");

        List<Message> sent = model.lastPrompt().getInstructions();
        assertThat(sent.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(sent.getFirst().getText()).contains("Harness");
    }

    @Test
    void remembersPreviousTurns() {
        ScriptedChatModel model = ScriptedChatModel.replyingWith("Привет!", "Тебя зовут Саша.");
        Agent agent = agent(model);

        agent.run("Меня зовут Саша");
        agent.run("Как меня зовут?");

        // Второй запрос содержит всю историю: у модели нет памяти между вызовами,
        // помнит за неё Conversation.
        List<String> secondCall = model.lastPrompt().getInstructions().stream().map(Message::getText).toList();
        assertThat(secondCall)
                .containsExactly(
                        secondCall.getFirst(), "Меня зовут Саша", "Привет!", "Как меня зовут?");
    }

    @Test
    void reportsAnswerExactlyOnce() {
        Agent agent = agent(ScriptedChatModel.replyingWith("готово"));

        agent.run("привет");

        assertThat(events.events())
                .filteredOn(AgentEvent.AgentSpoke.class::isInstance)
                .singleElement()
                .isInstanceOfSatisfying(
                        AgentEvent.AgentSpoke.class,
                        spoke -> assertThat(spoke.assistantMessage()).isEqualTo("готово"));
    }

    @Test
    void executesToolAndRunsSecondRound() {
        ScriptedChatModel model = ScriptedChatModel.replyingWith();
        model.then(toolCall("call-1", "Echo"));
        model.then(new AssistantMessage("Эхо получено"));
        Agent agent = agent(
                model,
                List.of(StubTool.replying("Echo", ToolResult.ok("эхо"))),
                new AgentProperties(20, "."));

        String answer = agent.run("скажи эхо");

        assertThat(answer).isEqualTo("Эхо получено");
        assertThat(model.callCount()).isEqualTo(2);

        // Второй промпт: системный промпт, вопрос, вызов инструмента, его результат.
        List<Message> secondPrompt = model.receivedPrompts().get(1).getInstructions();
        assertThat(secondPrompt)
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL);
        ToolResponseMessage toolResponse = (ToolResponseMessage) secondPrompt.getLast();
        assertThat(toolResponse.getResponses())
                .singleElement()
                .isInstanceOfSatisfying(
                        ToolResponseMessage.ToolResponse.class,
                        response -> {
                            assertThat(response.name()).isEqualTo("Echo");
                            assertThat(response.responseData()).isEqualTo("эхо");
                        });
    }

    @Test
    void stopsAtIterationLimit() {
        ScriptedChatModel model = ScriptedChatModel.replyingWith();
        for (int i = 0; i < 3; i++) {
            model.then(toolCall("call-" + i, "Echo"));
        }
        Agent agent = agent(
                model,
                List.of(StubTool.replying("Echo", ToolResult.ok("эхо"))),
                new AgentProperties(2, "."));

        String answer = agent.run("работай, не останавливаясь");

        assertThat(answer).isEqualTo("Достигнут предел в 2 итераций - задача не завершена.");
        assertThat(model.callCount()).isEqualTo(2);
        assertThat(events.events())
                .filteredOn(AgentEvent.TurnLimitReached.class::isInstance)
                .singleElement()
                .isInstanceOfSatisfying(
                        AgentEvent.TurnLimitReached.class,
                        limit -> assertThat(limit.maxTurns()).isEqualTo(2));
    }

    /** Ответ модели, требующий вызвать инструмент. */
    private static AssistantMessage toolCall(String id, String toolName) {
        return AssistantMessage.builder()
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", toolName, "{}")))
                .build();
    }
}
