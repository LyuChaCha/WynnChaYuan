package com.wynnchayuan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigRoundTripTest {

    private static final Path FIXTURES = Path.of("src/test/resources/config-golden");
    private static final String IN = ".in.json";
    private static final String OUT = ".out.json";

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        boolean bless = args.length > 0 && args[0].equals("--bless");
        List<Path> inputs;
        try (Stream<Path> files = Files.list(FIXTURES)) {
            inputs = files.filter(p -> p.getFileName().toString().endsWith(IN)).sorted().toList();
        }
        check("fixtures present", !inputs.isEmpty());
        for (Path input : inputs) {
            String name = input.getFileName().toString();
            Path expected = input.resolveSibling(name.substring(0, name.length() - IN.length()) + OUT);
            String written = roundTrip(input);
            if (bless) {
                Files.writeString(expected, written);
            }
            check(name, written.equals(Files.readString(expected).replace("\r\n", "\n")));
        }
        System.out.println(failures == 0 ? "ConfigRoundTrip: passed" : "ConfigRoundTrip: " + failures + " failed");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static String roundTrip(Path input) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-config");
        Path file = dir.resolve("config.json");
        Files.copy(input, file);
        CollectorConfig config = new CollectorConfig(file);
        config.toggleDebugDumps();
        config.toggleDebugDumps();
        return Files.readString(file);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
