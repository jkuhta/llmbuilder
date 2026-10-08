package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.generator.check.RealismChecker;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.SpecValidator;
import io.github.jkuhta.llmbuilder.testutil.IsoRenderer;
import io.github.jkuhta.llmbuilder.testutil.VanillaBlockStates;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every example spec validates, generates without realism violations and uses only real block states. */
class ExampleBuildingsTest {
	@ParameterizedTest
	@ValueSource(strings = {"canal_house", "medieval_tower"})
	void generatesRealisticBuilding(String name) throws IOException {
		BuildingSpec spec = Examples.load(name);
		SpecValidator.Result validation = SpecValidator.validate(spec, SpecValidator.Limits.DEFAULT);
		assertTrue(validation.ok(), () -> String.join("\n", validation.errors()));

		BuildingGenerator.Result result = BuildingGenerator.generate(spec);
		IsoRenderer.render(result.buffer(), name, IsoRenderer.View.FRONT_RIGHT, 3);
		IsoRenderer.render(result.buffer(), name, IsoRenderer.View.BACK_LEFT, 3);
		IsoRenderer.elevation(result.buffer(), name, 4);

		assertEquals(List.of(), result.violations().stream().map(RealismChecker.Violation::toString).toList());

		List<String> invalid = new ArrayList<>();
		for (BlockBuffer.Entry e : result.buffer().entries()) {
			VanillaBlockStates.problem(e.block()).ifPresent(p -> invalid.add(p + " at " + e.pos()));
		}
		assertEquals(List.of(), invalid.stream().distinct().limit(10).toList());
	}

	@ParameterizedTest
	@ValueSource(strings = {"canal_house", "medieval_tower"})
	void isDeterministic(String name) throws IOException {
		BuildingSpec spec = Examples.load(name);
		BlockBuffer a = BuildingGenerator.generate(spec).buffer();
		BlockBuffer b = BuildingGenerator.generate(spec).buffer();
		assertEquals(a.entries(), b.entries());
	}
}
