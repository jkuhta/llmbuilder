package io.github.jkuhta.llmbuilder.spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * The hand-written example buildings bundled with the mod. They double as regression tests and
 * as few-shot examples for the planner.
 */
public final class ExampleSpecs {
	public static final List<String> NAMES = List.of("canal_house", "medieval_tower", "modern_villa", "farmhouse", "gothic_chapel");

	private ExampleSpecs() {
	}

	public static Optional<String> json(String name) {
		if (!NAMES.contains(name)) {
			return Optional.empty();
		}
		try (InputStream in = ExampleSpecs.class.getResourceAsStream("/llmbuilder/examples/" + name + ".json")) {
			return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
