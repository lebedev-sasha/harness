package ru.sashalebedev.harness.tool;

/**
 * Инструмент-заглушка для тестов: либо отдаёт заранее заданный результат,
 * либо падает — если сценарий проверяет обработку ошибок.
 *
 * <p>Настоящие инструменты читают файлы и ходят по диску; чтобы протестировать
 * цикл агента и реестр, достаточно инструмента, поведение которого полностью
 * под нашим контролем.
 */
public final class StubTool implements Tool<StubTool.Input> {

    /** Аргументы любого вида: реестр всё равно не валидирует их сам. */
    public record Input() {}

    private final String name;
    private final ToolResult result;
    private final boolean crash;

    private StubTool(String name, ToolResult result, boolean crash) {
        this.name = name;
        this.result = result;
        this.crash = crash;
    }

    public static StubTool replying(String name, ToolResult result) {
        return new StubTool(name, result, false);
    }

    public static StubTool crashing(String name) {
        return new StubTool(name, ToolResult.ok("до этого результата дело не дойдёт"), true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "заглушка для тестов";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public String renderToolUseMessage(Input input) {
        return "использован " + name;
    }

    @Override
    public ToolResult call(Input input) {
        if (crash) {
            throw new IllegalStateException("сломался по сценарию теста");
        }
        return result;
    }
}
