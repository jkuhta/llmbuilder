package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;

/**
 * Marks the inside of every mass as explicit air, from the ground floor up to the roof, so
 * placement clears terrain, trees or old builds that would otherwise poke through.
 */
public final class ClearVolume implements Component {
	@Override
	public void apply(BuildContext ctx) {
		int floor = ctx.layout.plinth();
		for (Mass m : ctx.layout.masses()) {
			for (int x = m.x0(); x <= m.x1(); x++) {
				for (int z = m.z0(); z <= m.z1(); z++) {
					int top = Math.max(m.wallTop(), ctx.roofPlan().topAt(x, z));
					for (int y = floor; y <= top; y++) {
						ctx.set(new Vec3(x, y, z), Block.AIR, Part.CLEAR);
					}
				}
			}
		}
	}
}
