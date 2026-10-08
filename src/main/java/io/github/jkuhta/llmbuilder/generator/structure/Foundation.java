package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

/**
 * Plinth that steps out one block from the wall at ground level and is capped by a sloping
 * stair (a water table), giving every facade a second depth layer at its base.
 */
public final class Foundation implements Component {
	@Override
	public void apply(BuildContext ctx) {
		int plinth = ctx.layout.plinth();
		for (FacadeFrame f : ctx.layout.facades()) {
			for (int u = 1; u < f.length() - 1; u++) {
				if (!ctx.layout.exposed(f, u, 0)) {
					continue;
				}
				for (int y = 0; y < plinth; y++) {
					Vec3 p = f.at(u, y, 1);
					ctx.setIfFree(p, ctx.palette.full(Role.FOUNDATION, p), Part.PLINTH);
				}
				Vec3 cap = f.at(u, plinth, 1);
				ctx.setIfFree(cap, ctx.palette.stairs(Role.FOUNDATION, cap, f.inward(), Half.BOTTOM), Part.PLINTH);
			}
		}
	}
}
