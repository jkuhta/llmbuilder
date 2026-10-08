package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Walls.Corners;

/**
 * Wall planes of every mass: foundation courses up to the ground floor, wall material above,
 * and trim at the corners. Corner treatment is either quoins (alternating trim in the wall
 * plane) or pillars that stand one block proud of both facades.
 */
public final class WallShell implements Component {
	@Override
	public void apply(BuildContext ctx) {
		Corners corners = ctx.spec.walls().corners();
		int plinth = ctx.layout.plinth();
		for (FacadeFrame f : ctx.layout.facades()) {
			int top = f.mass().wallTop();
			for (int u = 0; u < f.length(); u++) {
				boolean corner = u == 0 || u == f.length() - 1;
				for (int y = 0; y <= top; y++) {
					Vec3 p = f.at(u, y, 0);
					Role role = y <= plinth ? Role.FOUNDATION : Role.WALL;
					if (corner && corners != Corners.NONE && y > plinth) {
						role = Role.TRIM;
					}
					if (corners == Corners.QUOINS && y > plinth && (y - plinth) % 2 == 1 && (u == 1 || u == f.length() - 2)) {
						role = Role.TRIM;
					}
					ctx.set(p, ctx.palette.full(role, p), Part.WALL);
				}
			}
			if (corners == Corners.PILLARS) {
				pillar(ctx, f, 0, f.left());
				pillar(ctx, f, f.length() - 1, f.right());
			}
		}
	}

	/** A 2x2 corner pillar: the corner block plus one block proud of each facade and the diagonal. */
	private static void pillar(BuildContext ctx, FacadeFrame f, int u, Dir side) {
		if (!isConvexCorner(ctx, f, u, side)) {
			return;
		}
		Mass m = f.mass();
		for (int y = 0; y <= m.wallTop(); y++) {
			Vec3 own = f.at(u, y, 1);
			Vec3 diagonal = own.offset(side);
			Role role = y <= ctx.layout.plinth() ? Role.FOUNDATION : Role.TRIM;
			ctx.set(own, ctx.palette.full(role, own), Part.PILLAR);
			ctx.set(diagonal, ctx.palette.full(role, diagonal), Part.PILLAR);
		}
		// Pillars end in a projecting cap under the eaves.
		Vec3 cap = f.at(u, m.wallTop(), 2);
		if (ctx.detail(io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level.HIGH)) {
			ctx.setIfFree(cap, ctx.palette.stairs(Role.TRIM, cap, f.inward(), Half.TOP), Part.PILLAR);
		}
	}

	static boolean isConvexCorner(BuildContext ctx, FacadeFrame f, int u, Dir side) {
		Vec3 c = f.at(u, 0, 0);
		Vec3 out = c.offset(f.outward());
		Vec3 aside = c.offset(side);
		Vec3 diag = out.offset(side);
		return !ctx.layout.inAnyMass(out.x(), out.z()) && !ctx.layout.inAnyMass(aside.x(), aside.z())
			&& !ctx.layout.inAnyMass(diag.x(), diag.z());
	}
}
