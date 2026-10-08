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
			for (int u = 0; u < f.length(); u++) {
				if (!ctx.layout.exposed(f, u, 0)) {
					continue;
				}
				plinth(ctx, f, f.at(u, 0, 1), plinth);
				// Wrap around convex corners so the plinth runs continuously.
				if (u == 0 && WallShell.isConvexCorner(ctx, f, u, f.left())) {
					plinth(ctx, f, f.at(u, 0, 1).offset(f.left()), plinth);
				} else if (u == f.length() - 1 && WallShell.isConvexCorner(ctx, f, u, f.right())) {
					plinth(ctx, f, f.at(u, 0, 1).offset(f.right()), plinth);
				}
			}
		}
	}

	private static void plinth(BuildContext ctx, FacadeFrame f, Vec3 ground, int height) {
		for (int y = 0; y < height; y++) {
			Vec3 p = ground.above(y);
			ctx.setIfFree(p, ctx.palette.full(Role.FOUNDATION, p), Part.PLINTH);
		}
		Vec3 cap = ground.above(height);
		ctx.setIfFree(cap, ctx.palette.stairs(Role.FOUNDATION, cap, f.inward(), Half.BOTTOM), Part.PLINTH);
	}
}
