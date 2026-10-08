package io.github.jkuhta.llmbuilder.generator.feature;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.BayLayout;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Chimney;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

/**
 * External chimney stacks: a breast standing one block proud of a side wall from the ground,
 * joined by a flue in the wall plane once above the eaves, rising two blocks clear of whatever
 * roof or gable is next to it and finished with a trim cap.
 */
public final class Chimneys implements Component {
	@Override
	public void apply(BuildContext ctx) {
		for (Chimney chimney : ctx.spec.chimneys()) {
			FacadeFrame f = FacadeFrame.of(ctx.layout.main(), chimney.side());
			int u = column(ctx, f, chimney.position());
			if (u < 0) {
				interior(ctx, f, chimney.position());
			} else {
				build(ctx, f, u);
			}
		}
	}

	/** Nearest wall column to the requested position that is not part of a window or door bay. */
	private static int column(BuildContext ctx, FacadeFrame f, double position) {
		int target = (int) Math.round(position * (f.length() - 1));
		int best = -1;
		for (int u = 1; u < f.length() - 1; u++) {
			if (inBay(ctx, f, u) || !ctx.layout.exposed(f, u, 1)) {
				continue;
			}
			if (best < 0 || Math.abs(u - target) < Math.abs(best - target)) {
				best = u;
			}
		}
		return best;
	}

	private static boolean inBay(BuildContext ctx, FacadeFrame f, int u) {
		for (BayLayout.Bay bay : ctx.bays(f).bays()) {
			if (u >= bay.u0() - 1 && u <= bay.u1() + 1) {
				return true;
			}
		}
		return false;
	}

	private static void build(BuildContext ctx, FacadeFrame f, int u) {
		int wallTop = f.mass().wallTop();
		int highest = Math.max(highestSolid(ctx, f.at(u, 0, 0)), highestSolid(ctx, f.at(u, 0, 1)));
		int top = Math.max(highest + 2, wallTop + 3);
		for (int y = 0; y <= top; y++) {
			Vec3 breast = f.at(u, y, 1);
			ctx.set(breast, y <= ctx.layout.plinth() ? ctx.palette.full(Role.FOUNDATION, breast) : ctx.palette.masonry(breast), Part.CHIMNEY);
			if (y > wallTop) {
				Vec3 flue = f.at(u, y, 0);
				ctx.set(flue, ctx.palette.masonry(flue), Part.CHIMNEY);
			}
		}
		for (int w = 0; w <= 1; w++) {
			Vec3 band = f.at(u, top, w);
			ctx.set(band, ctx.palette.full(Role.TRIM, band), Part.CHIMNEY);
			Vec3 cap = band.above();
			ctx.set(cap, ctx.palette.slab(Role.TRIM, cap, Half.BOTTOM), Part.CHIMNEY);
		}
	}

	/**
	 * When windows fill the whole wall, the stack rises inside instead: from the attic floor just
	 * behind the wall, up through the roof.
	 */
	private static void interior(BuildContext ctx, FacadeFrame f, double position) {
		int u = Math.max(2, Math.min(f.length() - 3, (int) Math.round(position * (f.length() - 1))));
		Vec3 base = f.at(u, 0, -2);
		if (!f.mass().isInterior(base.x(), base.z())) {
			ctx.warn("no room for the chimney on the " + f.side().id() + " side");
			return;
		}
		int top = highestSolid(ctx, base) + 2;
		for (int y = f.mass().wallTop() + 1; y <= top; y++) {
			Vec3 p = new Vec3(base.x(), y, base.z());
			ctx.set(p, ctx.palette.masonry(p), Part.CHIMNEY);
		}
		Vec3 band = new Vec3(base.x(), top, base.z());
		ctx.set(band, ctx.palette.full(Role.TRIM, band), Part.CHIMNEY);
		ctx.set(band.above(), ctx.palette.slab(Role.TRIM, band.above(), Half.BOTTOM), Part.CHIMNEY);
	}

	private static int highestSolid(BuildContext ctx, Vec3 column) {
		int top = 0;
		for (int y = 0; y < 256; y++) {
			if (ctx.buffer.isSolidAt(new Vec3(column.x(), y, column.z()))) {
				top = y;
			}
		}
		return top;
	}
}
