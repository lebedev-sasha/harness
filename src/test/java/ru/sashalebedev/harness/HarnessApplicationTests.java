package ru.sashalebedev.harness;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Проверяет, что контекст собирается: бины агента, инструментов и HUD находят друг друга.
 * Профиль "test" отключает {@code DemoRunner}, который разговаривает с живой моделью.
 */
@SpringBootTest
@ActiveProfiles("test")
class HarnessApplicationTests {

	@Test
	void contextLoads() {
	}

}
