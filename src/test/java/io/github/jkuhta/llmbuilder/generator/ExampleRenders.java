package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.SpecParser;
import io.github.jkuhta.llmbuilder.testutil.IsoRenderer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Writes isometric previews of the example buildings to build/renders for visual review. */
class ExampleRenders {
	static BuildingSpec load(String name) throws IOException {
		try (InputStream in = ExampleRenders.class.getResourceAsStream("/llmbuilder/examples/" + name + ".json")) {
			assertNotNull(in, "missing example " + name);
			SpecParser.Result r = SpecParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			assertTrue(r.ok(), () -> String.join("\n", r.errors()));
			return r.spec();
		}
	}

	@Test
	void renderCanalHouse() throws IOException {
		BuildingGenerator.Result result = BuildingGenerator.generate(load("canal_house"));
		IsoRenderer.render(result.buffer(), "canal_house", IsoRenderer.View.FRONT_RIGHT, 3);
		IsoRenderer.render(result.buffer(), "canal_house", IsoRenderer.View.BACK_LEFT, 3);
		IsoRenderer.elevation(result.buffer(), "canal_house", 4);
	}
}
