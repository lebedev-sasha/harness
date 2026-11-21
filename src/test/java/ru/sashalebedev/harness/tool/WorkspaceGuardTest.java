package ru.sashalebedev.harness.tool;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.sashalebedev.harness.agent.AgentProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceGuardTest {

    @TempDir
    Path root;

    private WorkspaceGuard guard() {
        return new WorkspaceGuard(new AgentProperties(20, root.toString()));
    }

    @Test
    void relativizePathResolvesInsideRoot() {
        assertThat(guard().resolve("src/Main.java"))
                .isEqualTo(root.resolve("src").resolve("Main.java"));
    }

    @Test
    void absolutePathInsideRootIsAllowed() {
        Path file = root.resolve("notes.txt");

        assertThat(guard().resolve(file.toString())).isEqualTo(file.normalize());
    }

    @Test
    void dotDotEscapeIsRejected() {
        assertThatThrownBy(() -> guard().resolve("../снаружи.txt"))
                .isInstanceOf(WorkspaceGuard.WorkspaceViolationException.class)
                .hasMessageContaining("вне рабочей папки");
    }

    @Test
    void absolutePathOutsideRootIsRejected() throws IOException {
        Path outside = Files.createTempDirectory("harness-outside");

        assertThatThrownBy(() -> guard().resolve(outside.resolve("файл.txt").toString()))
                .isInstanceOf(WorkspaceGuard.WorkspaceViolationException.class)
                .hasMessageContaining("вне рабочей папки");
    }

    @Test
    void emptyPathIsRejected() {
        assertThatThrownBy(() -> guard().resolve("   "))
                .isInstanceOf(WorkspaceGuard.WorkspaceViolationException.class)
                .hasMessageContaining("путь не указан");
    }

    @Test
    void nonexistentPathIsAllowedToolCreatesIt() {
        assertThat(guard().resolve("новый-файл.txt"))
                .isEqualTo(root.resolve("новый-файл.txt"));
    }

    @Test
    void symlinkLeadingOutsideIsRejected() throws IOException {
        Path outside = Files.createTempDirectory("harness-outside");
        // Целевой файл должен существовать: реальный путь проверяется через toRealPath,
        // а для несуществующего файла проверка просто не срабатывает.
        Files.writeString(outside.resolve("файл.txt"), "снаружи");
        Path link;
        try {
            link = Files.createSymbolicLink(root.resolve("link"), outside);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("симлинки недоступны в этом окружении: " + e.getMessage());
            return;
        }

        assertThatThrownBy(() -> guard().resolve(link.resolve("файл.txt").toString()))
                .isInstanceOf(WorkspaceGuard.WorkspaceViolationException.class)
                .hasMessageContaining("симлинк");
    }

    @Test
    void relativizeReturnsPathWithForwardSlashes() {
        assertThat(guard().relativize(root.resolve("src").resolve("Main.java")))
                .isEqualTo("src/Main.java");
    }
}
