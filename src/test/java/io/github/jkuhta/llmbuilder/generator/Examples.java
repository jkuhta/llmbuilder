package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.SpecParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads example specs from resources for tests. */
final class Examples {
	static BuildingSpec load(String name) throws IOException {
		try (InputStream in = Examples.class.getResourceAsStream("/llmbuilder/examples/" + name + ".json")) {
			assertNotNull(in, "missing example " + name);
			SpecParser.Result r = SpecParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			assertTrue(r.ok(), () -> String.join("\n", r.errors()));
			return r.spec();
		}
	}
}
