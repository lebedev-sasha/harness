package ru.sashalebedev.harness.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.sashalebedev.harness.agent.AgentProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileReadToolTest {

    @TempDir
    Path root;

    private FileReadTool tool() {
        return new FileReadTool(new WorkspaceGuard(new AgentProperties(20, root.toString())));
    }

    private ToolResult read(String path, Integer offset, Integer limit) {
        return tool().call(new FileReadTool.Input(path, offset, limit));
    }

    @Test
    void readsFileInCatNFormat() throws IOException {
        Files.writeString(root.resolve("notes.txt"), "первая\nвторая\n");

        ToolResult result = read("notes.txt", null, null);

        assertThat(result.error()).isFalse();
        assertThat(result.content()).contains("1\tпервая").contains("2\tвторая");
    }

    @Test
    void offsetAndLimitBoundRangeAndHintContinuation() throws IOException {
        Files.writeString(root.resolve("big.txt"), "один\nдва\nтри\nчетыре\nпять\n");

        ToolResult result = read("big.txt", 2, 2);

        assertThat(result.error()).isFalse();
        assertThat(result.content())
                .contains("2\tдва")
                .contains("3\tтри")
                .doesNotContain("1\tодин")
                .doesNotContain("4\tчетыре")
                .contains("показаны строки 2-3 из 5");
    }

    @Test
    void offsetBeyondFileSizeIsError() throws IOException {
        Files.writeString(root.resolve("small.txt"), "всего одна\n");

        ToolResult result = read("small.txt", 5, null);

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("в файле всего 1 строк").contains("5");
    }

    @Test
    void missingFileIsReportedWithoutException() {
        ToolResult result = read("нет-такого.txt", null, null);

        // Не ошибка, а факт: файла ещё нет, но агент не должен падать из-за этого.
        assertThat(result.error()).isFalse();
        assertThat(result.content()).contains("не найден");
    }

    @Test
    void emptyFileIsClearlyMarked() throws IOException {
        Files.writeString(root.resolve("empty.txt"), "");

        ToolResult result = read("empty.txt", null, null);

        assertThat(result.error()).isFalse();
        assertThat(result.content()).contains("пустой");
    }

    @Test
    void directoryIsNotAFile() throws IOException {
        Files.createDirectory(root.resolve("папка"));

        ToolResult result = read("папка", null, null);

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("папка");
    }

    @Test
    void binaryFileIsNotReadAsText() throws IOException {
        Files.write(root.resolve("image.png"), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A});

        ToolResult result = read("image.png", null, null);

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("не текстовый");
    }

    @Test
    void pathOutsideWorkspaceIsRejected() {
        ToolResult result = read("../снаружи.txt", null, null);

        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("вне рабочей папки");
    }

    @Test
    void veryLongLinesAreTruncated() throws IOException {
        Files.writeString(root.resolve("long.txt"), "x".repeat(2500) + "\n");

        ToolResult result = read("long.txt", null, null);

        assertThat(result.error()).isFalse();
        assertThat(result.content()).contains("строка обрезана").hasSizeLessThan(2300);
    }
}
