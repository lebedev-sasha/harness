package ru.sashalebedev.harness.hud;

import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.sashalebedev.harness.agent.Agent;
import ru.sashalebedev.harness.agent.event.AgentEvent;
import ru.sashalebedev.harness.context.TokenEstimator;
import ru.sashalebedev.harness.tool.Tool;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static ru.sashalebedev.harness.hud.BodyPart.*;
import static ru.sashalebedev.harness.hud.LanguageUtils.plural;

@Component
public class Hud {

    private final MachineViewPrinter machinePrinter;

    private final Agent agent;

    private final List<Tool<?>> tools;

    public Hud(MachineViewPrinter machinePrinter, Agent agent, List<Tool<?>> tools) {
        this.machinePrinter = machinePrinter;
        this.agent = agent;
        this.tools = tools;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted(ApplicationStartedEvent event) {
        machinePrinter.showBootLog(collectBootLines(event.getApplicationContext().getBeanDefinitionCount()));
    }

    private List<BootLine> collectBootLines(int beanDefinitionCount) {
        Map<BodyPart, BootLine> lines = new EnumMap<>(BodyPart.class);

        lines.put(POWER_CELL, BootLine.of(POWER_CELL, BootStatus.ONLINE, "спринг контекст " + plural(beanDefinitionCount, "бин", "бина", "бинов")));
        lines.put(NEUROPROCESSOR, BootLine.of(NEUROPROCESSOR, BootStatus.ONLINE, agent.chatModel()));
        lines.put(MEMORY_BANK, BootLine.of(MEMORY_BANK, BootStatus.ONLINE, agent.conversation().getClass().getSimpleName()));
        lines.put(DIAGNOSTIC_BUS, BootLine.of(DIAGNOSTIC_BUS, BootStatus.ONLINE, "Agents events: " + AgentEvent.class.getPermittedSubclasses().length));

        for (Tool<?> tool : tools) {
            BodyPart part = BodyPart.forToolName(tool.name());
            BootLine bootLine = lines.computeIfAbsent(part, BootLine::empty);
            bootLine.components().add(tool.name());
        }

        if (!tools.isEmpty()) {
            lines.put(TOOL_BUS, BootLine.of(TOOL_BUS, BootStatus.ONLINE, plural(tools.size(), "инструмент", "инструмента", "инструментов")));
        }

        return List.copyOf(lines.values());
    }

    @EventListener
    public void onTurnStarted(AgentEvent.TurnStarted event) {
        String userInput = event.userMessage();
        machinePrinter.showOrder(userInput);
        List<Message> promptMessages = agent.conversation().toPromptMessages();
        machinePrinter.showContext(promptMessages, TokenEstimator.estimate(promptMessages));

    }

    @EventListener
    public void onTurnFinished(AgentEvent.TurnFinished event) {
        machinePrinter.showTurnFinished(event.duration(), event.iteration());
    }

    @EventListener
    public void onToolCallStarted(AgentEvent.ToolCallStarted event) {
        machinePrinter.showToolCallStarted(event.description());
    }

    @EventListener
    public void onToolCallFinished(AgentEvent.ToolCallFinished event) {
        machinePrinter.showToolCallFinished(
                event.error(), event.preview(), event.duration());
    }

    @EventListener
    public void onTurnLimitReached(AgentEvent.TurnLimitReached event) {
        machinePrinter.showTurnLimitReached(event.maxTurns());
    }


}
