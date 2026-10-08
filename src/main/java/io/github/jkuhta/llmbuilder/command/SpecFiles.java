package io.github.jkuhta.llmbuilder.command;

import io.github.jkuhta.llmbuilder.spec.ExampleSpecs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Resolves spec names for {@code /build fromjson}: files in {@code config/llmbuilder/specs}
 * first, then the bundled examples. Names are restricted to a safe character set so they can
 * never escape the specs directory.
 */
public final class SpecFiles {
	private static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9_\\-.]{1,64}");

	private final Path dir;

	public SpecFiles(Path dir) {
		this.dir = dir;
	}

	public Path dir() {
		return dir;
	}

	public Optional<String> read(String name) throws IOException {
		String base = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
		if (!SAFE_NAME.matcher(base).matches() || base.contains("..")) {
			return Optional.empty();
		}
		Path file = dir.resolve(base + ".json").normalize();
		if (file.startsWith(dir) && Files.isRegularFile(file)) {
			return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
		}
		return ExampleSpecs.json(base);
	}

	public List<String> names() {
		List<String> names = new ArrayList<>();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> files = Files.list(dir)) {
				files.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".json"))
					.map(n -> n.substring(0, n.length() - 5))
					.filter(n -> SAFE_NAME.matcher(n).matches())
					.sorted()
					.forEach(names::add);
			} catch (IOException ignored) {
				// Suggestions are best effort.
			}
		}
		for (String example : ExampleSpecs.NAMES) {
			if (!names.contains(example)) {
				names.add(example);
			}
		}
		return names;
	}
}
