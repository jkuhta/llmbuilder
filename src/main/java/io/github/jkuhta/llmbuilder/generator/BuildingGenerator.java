package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.generator.check.RealismChecker;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.feature.Chimneys;
import io.github.jkuhta.llmbuilder.generator.opening.Doors;
import io.github.jkuhta.llmbuilder.generator.resolve.Connections;
import io.github.jkuhta.llmbuilder.generator.resolve.StairShapes;
import io.github.jkuhta.llmbuilder.generator.opening.Windows;
import io.github.jkuhta.llmbuilder.generator.roof.FlatRoof;
import io.github.jkuhta.llmbuilder.generator.roof.RoofBuilder;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlanner;
import io.github.jkuhta.llmbuilder.generator.roof.SteppedGable;
import io.github.jkuhta.llmbuilder.generator.structure.Bands;
import io.github.jkuhta.llmbuilder.generator.structure.ClearVolume;
import io.github.jkuhta.llmbuilder.generator.structure.Floors;
import io.github.jkuhta.llmbuilder.generator.structure.Foundation;
import io.github.jkuhta.llmbuilder.generator.structure.WallShell;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link BuildingSpec} into blocks by running components in a fixed order. Pure Java with
 * no Minecraft dependency, so it runs identically in tests and in game. Deterministic for a given
 * spec and seed.
 */
public final class BuildingGenerator {
	public record Result(BlockBuffer buffer, List<String> warnings, List<RealismChecker.Violation> violations) {
	}

	private static final List<Component> PIPELINE = List.of(
		new RoofPlanner(),
		new ClearVolume(),
		new WallShell(),
		new Foundation(),
		new Bands(),
		new Floors(),
		new Windows(),
		new Doors(),
		new RoofBuilder(),
		new FlatRoof(),
		new SteppedGable(),
		new Chimneys(),
		new StairShapes(),
		new Connections()
	);

	private BuildingGenerator() {
	}

	public static Result generate(BuildingSpec spec) {
		BuildContext ctx = new BuildContext(spec);
		for (Component component : PIPELINE) {
			component.apply(ctx);
		}
		List<RealismChecker.Violation> violations = RealismChecker.check(ctx);
		List<String> warnings = new ArrayList<>(ctx.warnings());
		violations.forEach(v -> warnings.add(v.toString()));
		return new Result(ctx.buffer, List.copyOf(warnings), violations);
	}
}
