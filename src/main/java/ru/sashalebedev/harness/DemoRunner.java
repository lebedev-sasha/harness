package ru.sashalebedev.harness;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.sashalebedev.harness.agent.Agent;

/**
 * Сценарий текущего урока. Единственный файл, который меняется «до» урока —
 * всё остальное мы дописываем по ходу.
 *
 * <p>Урок 1: хотим просто поговорить с агентом. Пока не с чем: класса Agent нет.
 */
@Component
@Profile("!test")
public class DemoRunner implements CommandLineRunner {

    private final Agent agent;

    public DemoRunner(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println(agent.run("Какая версия java  этом проекте. Ответь одним предложением."));
    }
}
