package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.ExampleSpecs;
import io.github.jkuhta.llmbuilder.spec.SpecParser;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads example specs from resources for tests. */
final class Examples {
	static BuildingSpec load(String name) {
		String json = ExampleSpecs.json(name).orElseThrow(() -> new AssertionError("missing example " + name));
		SpecParser.Result r = SpecParser.parse(json);
		assertTrue(r.ok(), () -> String.join("\n", r.errors()));
		return r.spec();
	}
}
