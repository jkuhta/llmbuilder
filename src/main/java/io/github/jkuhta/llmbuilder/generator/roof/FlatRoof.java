package io.github.jkuhta.llmbuilder.generator.roof;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;

/**
 * Flat roofs: a parapet with slab coping or crenellations on the wall line, or, with an
 * overhang, a thin cantilevered slab eave as on modernist houses.
 */
public final class FlatRoof implements Component {
	@Override
	public void apply(BuildContext ctx) {
		Roof roof = ctx.spec.roof();
		if (roof.type() != Roof.Type.FLAT) {
			return;
		}
		for (FacadeFrame f : ctx.layout.facades()) {
			Mass m = f.mass();
			int top = m.wallTop();
			for (int u = 0; u < f.length(); u++) {
				if (!ctx.layout.exposed(f, u, top + 1)) {
					continue;
				}
				switch (roof.parapet()) {
					case PLAIN -> {
						Vec3 wall = f.at(u, top + 1, 0);
						ctx.set(wall, ctx.palette.full(Role.WALL, wall), Part.PARAPET);
						Vec3 coping = f.at(u, top + 2, 0);
						ctx.set(coping, ctx.palette.slab(Role.TRIM, coping, Half.BOTTOM), Part.PARAPET);
					}
					case CRENELLATED -> {
						Vec3 wall = f.at(u, top + 1, 0);
						ctx.set(wall, ctx.palette.full(Role.WALL, wall), Part.PARAPET);
						Vec3 merlon = f.at(u, top + 2, 0);
						boolean solid = u % 2 == 0 || u == f.length() - 1;
						ctx.set(merlon, solid ? ctx.palette.full(Role.WALL, merlon) : ctx.palette.slab(Role.TRIM, merlon, Half.BOTTOM), Part.PARAPET);
					}
					case NONE -> {
					}
				}
				for (int w = 1; w <= roof.overhang(); w++) {
					eave(ctx, f, u, top, w);
				}
			}
		}
	}

	private static void eave(BuildContext ctx, FacadeFrame f, int u, int y, int w) {
		Vec3 p = f.at(u, y, w);
		if (ctx.layout.inAnyMass(p.x(), p.z())) {
			return;
		}
		ctx.set(p, ctx.palette.slab(Role.ROOF, p, Half.TOP), Part.ROOF);
		// Wrap around convex corners so the eave reads as one continuous slab.
		if (u == 0 || u == f.length() - 1) {
			var side = u == 0 ? f.left() : f.right();
			for (int s = 1; s <= ctx.spec.roof().overhang(); s++) {
				Vec3 c = p.offset(side, s);
				if (!ctx.layout.inAnyMass(c.x(), c.z())) {
					ctx.set(c, ctx.palette.slab(Role.ROOF, c, Half.TOP), Part.ROOF);
				}
			}
		}
	}
}
