package io.github.jkuhta.llmbuilder.generator.roof;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;
import io.github.jkuhta.llmbuilder.spec.Side;

/**
 * Dutch/Flemish crow-stepped gable: the gable wall rises above the roof in steps, each capped with
 * a slab coping, with a raised apex and (at high detail) a timber hoist beam.
 */
public final class SteppedGable implements Component {
	@Override
	public void apply(BuildContext ctx) {
		if (ctx.spec.roof().type() != Roof.Type.STEPPED_GABLE) {
			return;
		}
		int step = ctx.spec.roof().pitch() == Roof.Pitch.STEEP ? 1 : 2;
		for (FacadeFrame f : ctx.layout.facades()) {
			boolean alongX = RoofPlanner.ridgeAlongX(ctx, f.mass());
			boolean gableSide = alongX ? (f.side() == Side.LEFT || f.side() == Side.RIGHT) : (f.side() == Side.FRONT || f.side() == Side.BACK);
			if (gableSide) {
				gable(ctx, f, step);
			}
		}
	}

	private static void gable(BuildContext ctx, FacadeFrame f, int step) {
		Mass m = f.mass();
		RoofPlan plan = ctx.roofPlan();
		int apexU = -1;
		int apexTop = Integer.MIN_VALUE;
		int[] tops = new int[f.length()];
		for (int u = 0; u < f.length(); u++) {
			RoofPlan.Column c = column(plan, f, u);
			if (c == null) {
				tops[u] = Integer.MIN_VALUE;
				continue;
			}
			// Step groups count from each eave; the wall top is just above the group's highest roof block.
			int group = c.distance() / step;
			int groupTop = c.top();
			for (int v = 0; v < f.length(); v++) {
				RoofPlan.Column other = column(plan, f, v);
				if (other != null && other.distance() / step == group) {
					groupTop = Math.max(groupTop, other.top());
				}
			}
			tops[u] = groupTop + 1;
			if (tops[u] > apexTop) {
				apexTop = tops[u];
				apexU = u;
			}
		}
		for (int u = 0; u < f.length(); u++) {
			if (tops[u] == Integer.MIN_VALUE || !ctx.layout.exposed(f, u, m.roofBase())) {
				continue;
			}
			for (int y = m.roofBase(); y <= tops[u]; y++) {
				Vec3 p = f.at(u, y, 0);
				ctx.set(p, ctx.palette.full(Role.WALL, p), Part.GABLE);
			}
			Vec3 coping = f.at(u, tops[u] + 1, 0);
			ctx.set(coping, ctx.palette.slab(Role.TRIM, coping, Half.BOTTOM), Part.GABLE);
		}
		if (apexU < 0) {
			return;
		}
		// Raised apex: the centre step gets a trim block under its coping.
		for (int u = 0; u < f.length(); u++) {
			if (tops[u] == apexTop) {
				Vec3 apex = f.at(u, apexTop + 1, 0);
				ctx.set(apex, ctx.palette.full(Role.TRIM, apex), Part.GABLE);
				Vec3 cap = apex.above();
				ctx.set(cap, ctx.palette.slab(Role.TRIM, cap, Half.BOTTOM), Part.GABLE);
			}
		}
		if (ctx.detail(Level.HIGH) && f.side() == Side.FRONT) {
			int center2 = f.length() - 1;
			int u = center2 / 2;
			Vec3 beam1 = f.at(u, apexTop - 2, 1);
			Vec3 beam2 = f.at(u, apexTop - 2, 2);
			ctx.setIfFree(beam1, ctx.palette.fence(Role.DOOR, beam1), Part.DETAIL);
			ctx.setIfFree(beam2, ctx.palette.fence(Role.DOOR, beam2), Part.DETAIL);
		}
	}

	private static RoofPlan.Column column(RoofPlan plan, FacadeFrame f, int u) {
		Vec3 p = f.at(u, 0, 0);
		return plan.column(p.x(), p.z());
	}
}
