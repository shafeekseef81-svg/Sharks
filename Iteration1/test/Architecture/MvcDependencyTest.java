package Architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the MVC package rules on the source files (NFR-08, NFR-20, Task 27).
 * Run from the {@code Iteration1} folder; {@code test.bat} and {@code test.sh} do this.
 *
 * <p>Owner: Rakan Ziyan (author of Task 27); reviewed by Shafeek Saif.
 */
class MvcDependencyTest {

    private static final Path SRC = Path.of("src");

    @Test
    @DisplayName("NFR-08: Model has no GUI code and does not depend on View or Controller")
    void modelIsIndependent() throws IOException {
        List<String> bad = linesMatching(SRC.resolve("Model"),
                "import javax.swing", "import java.awt", "import View.", "import Controller.");
        assertTrue(bad.isEmpty(), "Model imports GUI/View/Controller code: " + bad);
    }

    @Test
    @DisplayName("NFR-08: View does not depend on Controller")
    void viewDoesNotUseController() throws IOException {
        List<String> bad = linesMatching(SRC.resolve("View"), "import Controller.");
        assertTrue(bad.isEmpty(), "View imports Controller code: " + bad);
    }

    @Test
    @DisplayName("NFR-20: only the Model, View and Controller packages exist")
    void onlyMvcPackages() throws IOException {
        List<String> others = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(SRC)) {
            dirs.filter(Files::isDirectory)
                    .map(d -> d.getFileName().toString())
                    .filter(n -> !n.equals("Model") && !n.equals("View") && !n.equals("Controller"))
                    .forEach(others::add);
        }
        assertTrue(others.isEmpty(), "Unexpected top-level packages: " + others);
    }

    @Test
    @DisplayName("SDP 5.3.1: no prototype demo code in the product")
    void noPrototypeDemoCode() throws IOException {
        List<String> bad = linesMatching(SRC, "DemoData", "forceNextRegular", "finishNow", "PROTOTYPE DEMO",
                "SelfTest", "devtools.");
        assertTrue(bad.isEmpty(), "Prototype demo code found: " + bad);
    }

    /** Returns "file:line: text" for every line under {@code root} that contains one of the needles. */
    private static List<String> linesMatching(Path root, String... needles) throws IOException {
        List<String> hits = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    for (String needle : needles) {
                        if (lines.get(i).contains(needle)) {
                            hits.add(SRC.relativize(f) + ":" + (i + 1) + ": " + lines.get(i).trim());
                        }
                    }
                }
            }
        }
        return hits;
    }
}
