package io.github.jkuhta.llmbuilder.generator.roof;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Feature;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlan.Cell;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlan.Column;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

/**
 * Renders the planned roof surface with correctly oriented stairs and slabs, closes the gable
 * triangles and knee walls underneath it, and adds stepped soffits under deep overhangs.
 */
public final class RoofBuilder implements Component {
	@Override
	public void apply(BuildContext ctx) {
		RoofPlan plan = ctx.roofPlan();
		for (Column column : plan.columns()) {
			for (Cell cell : column.cells()) {
				Vec3 p = new Vec3(column.x(), cell.y(), column.z());
				ctx.set(p, block(ctx, p, cell), column.ridge() ? Part.RIDGE : Part.ROOF);
			}
		}
		for (Mass m : ctx.layout.masses()) {
			for (int x = m.x0(); x <= m.x1(); x++) {
				for (int z = m.z0(); z <= m.z1(); z++) {
					if (m.isWallLine(x, z)) {
						fillUnder(ctx, m, plan.column(x, z));
					}
				}
			}
		}
		for (Column column : plan.columns()) {
			soffit(ctx, column);
		}
		if (!plan.columns().isEmpty()) {
			ctx.buffer.addFeature(new Feature.RoofInfo(ctx.layout.main().roofBase(), plan.ridgeY()));
		}
	}

	static Block block(BuildContext ctx, Vec3 p, Cell cell) {
		return switch (cell.kind()) {
			case STAIR -> ctx.palette.stairs(Role.ROOF, p, cell.facing(), Half.BOTTOM);
			case SLAB_BOTTOM -> ctx.palette.slab(Role.ROOF, p, Half.BOTTOM);
			case SLAB_TOP -> ctx.palette.slab(Role.ROOF, p, Half.TOP);
			case FULL -> ctx.palette.full(Role.ROOF, p);
		};
	}

	/** Gable triangles and knee walls: wall from the wall top up to the underside of the roof. */
	private static void fillUnder(BuildContext ctx, Mass m, Column column) {
		if (column == null) {
			return;
		}
		for (int y = m.roofBase(); y < column.bottom(); y++) {
			Vec3 p = new Vec3(column.x(), y, column.z());
			ctx.set(p, ctx.palette.full(Role.WALL, p), Part.GABLE);
		}
		// A top slab leaves a half-block hole in the gable face; a full roof block closes it.
		Cell lowest = column.cells().get(0);
		if (lowest.kind() == RoofPlan.Kind.SLAB_TOP) {
			Vec3 p = new Vec3(column.x(), lowest.y(), column.z());
			ctx.set(p, ctx.palette.full(Role.ROOF, p), Part.ROOF);
		}
	}

	/** Upside-down stairs under overhanging roof stairs so eaves and rakes read as thick, finished edges. */
	private static void soffit(BuildContext ctx, Column column) {
		if (ctx.layout.inAnyMass(column.x(), column.z()) || column.distance() < 1 || column.cells().size() != 1) {
			return;
		}
		Cell cell = column.cells().get(0);
		if (cell.kind() != RoofPlan.Kind.STAIR) {
			return;
		}
		Vec3 below = new Vec3(column.x(), cell.y() - 1, column.z());
		ctx.setIfFree(below, ctx.palette.stairs(Role.ROOF, below, cell.facing(), Half.TOP), Part.ROOF);
	}
}
