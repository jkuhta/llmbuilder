package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.BayLayout;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Facade;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

/**
 * Vertical articulation between bays: flat pilasters one block proud of the wall, or Gothic
 * buttresses that step out a second block over their lower half, each offset capped with a
 * sloping stair weathering.
 */
public final class Piers implements Component {
	@Override
	public void apply(BuildContext ctx) {
		for (FacadeFrame f : ctx.layout.facades()) {
			Facade.Piers style = ctx.facadeSpec(f).piers();
			if (style == Facade.Piers.NONE) {
				continue;
			}
			for (BayLayout.Gap gap : ctx.bays(f).gaps()) {
				if (!gap.inner()) {
					continue;
				}
				int first = gap.u0() + (gap.width() - 1) / 2;
				int last = gap.u0() + gap.width() / 2;
				for (int u = first; u <= last; u++) {
					if (style == Facade.Piers.PILASTERS) {
						pilaster(ctx, f, u);
					} else {
						buttress(ctx, f, u);
					}
				}
			}
		}
	}

	private static void pilaster(BuildContext ctx, FacadeFrame f, int u) {
		int top = f.mass().wallTop() - 1;
		for (int y = 0; y <= top; y++) {
			if (!ctx.layout.exposed(f, u, y)) {
				continue;
			}
			Vec3 p = f.at(u, y, 1);
			Role role = y <= ctx.layout.plinth() ? Role.FOUNDATION : Role.TRIM;
			ctx.set(p, ctx.palette.full(role, p), Part.PILLAR);
		}
	}

	private static void buttress(BuildContext ctx, FacadeFrame f, int u) {
		int top = f.mass().wallTop() - 1;
		int lower = Math.max(ctx.layout.plinth() + 2, (top * 3) / 5);
		for (int y = 0; y <= top; y++) {
			if (!ctx.layout.exposed(f, u, y)) {
				continue;
			}
			Vec3 inner = f.at(u, y, 1);
			Role role = y <= ctx.layout.plinth() ? Role.FOUNDATION : Role.TRIM;
			if (y == top) {
				ctx.set(inner, ctx.palette.stairs(Role.TRIM, inner, f.inward(), Half.BOTTOM), Part.PILLAR);
			} else {
				ctx.set(inner, ctx.palette.full(role, inner), Part.PILLAR);
			}
			if (y < lower) {
				Vec3 outer = f.at(u, y, 2);
				ctx.set(outer, ctx.palette.full(role, outer), Part.PILLAR);
			} else if (y == lower) {
				Vec3 outer = f.at(u, y, 2);
				ctx.set(outer, ctx.palette.stairs(Role.TRIM, outer, f.inward(), Half.BOTTOM), Part.PILLAR);
			}
		}
	}
}
