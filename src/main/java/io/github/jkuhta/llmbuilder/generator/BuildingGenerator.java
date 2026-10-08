package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.opening.Doors;
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

import java.util.List;

/**
 * Turns a {@link BuildingSpec} into blocks by running components in a fixed order. Pure Java with
 * no Minecraft dependency, so it runs identically in tests and in game. Deterministic for a given
 * spec and seed.
 */
public final class BuildingGenerator {
	public record Result(BlockBuffer buffer, List<String> warnings) {
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
		new StairShapes()
	);

	private BuildingGenerator() {
	}

	public static Result generate(BuildingSpec spec) {
		BuildContext ctx = new BuildContext(spec);
		for (Component component : PIPELINE) {
			component.apply(ctx);
		}
		return new Result(ctx.buffer, ctx.warnings());
	}
}
