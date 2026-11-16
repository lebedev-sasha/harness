package ru.sashalebedev.harness.agent;

import org.springframework.context.ApplicationEventPublisher;
import ru.sashalebedev.harness.agent.event.AgentEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Ловит события агента, чтобы проверить, что ядро сообщает о фактах.
 * Заодно доказывает, что подписчиком может быть кто угодно — HUD тут не при чём.
 */
public class RecordingEventPublisher implements ApplicationEventPublisher {

    private final List<AgentEvent> events = new ArrayList<>();

    @Override
    public void publishEvent(Object event) {
        if (event instanceof AgentEvent agentEvent) {
            events.add(agentEvent);
        }
    }

    public List<AgentEvent> events() {
        return List.copyOf(events);
    }
}
