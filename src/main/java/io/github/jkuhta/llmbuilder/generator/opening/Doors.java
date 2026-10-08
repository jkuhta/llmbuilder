package io.github.jkuhta.llmbuilder.generator.opening;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Feature;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.BayLayout;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Door;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

import java.util.Optional;

/**
 * Entrance doors set one block back in a framed reveal, with a glazed transom on tall floors,
 * stair steps up to a raised ground floor, an optional awning and flanking lanterns.
 */
public final class Doors implements Component {
	@Override
	public void apply(BuildContext ctx) {
		for (FacadeFrame f : ctx.layout.facades()) {
			if (!f.mass().isMain()) {
				continue;
			}
			for (Door door : ctx.facadeSpec(f).doors()) {
				BayLayout.Bay bay = bay(ctx, f, door.bay()).orElse(null);
				if (bay == null) {
					ctx.warn("door on " + f.name() + " refers to missing bay " + door.bay());
					continue;
				}
				build(ctx, f, door, bay);
			}
		}
	}

	private static Optional<BayLayout.Bay> bay(BuildContext ctx, FacadeFrame f, int index) {
		return ctx.bays(f).bays().stream().filter(b -> b.index() == index).findFirst();
	}

	private static void build(BuildContext ctx, FacadeFrame f, Door door, BayLayout.Bay bay) {
		Mass m = f.mass();
		int base = m.floorBases().get(0);
		int h = Windows.floorHeight(m, 0);
		int width = door.type() == Door.Type.DOUBLE ? 2 : 1;
		if ((bay.width() - width) % 2 != 0) {
			// Keep the door centred in its bay: a single door in an even bay becomes a double door.
			width = width == 1 ? 2 : 1;
		}
		width = Math.min(width, bay.width());
		int u0 = bay.u0() + (bay.width() - width) / 2;
		boolean transom = h - 1 >= 4;
		Windows.Rect r = new Windows.Rect(u0, u0 + width - 1, base + 1, base + (transom ? 3 : 2));

		Windows.cutOpening(ctx, f, r, Part.DOOR);
		String doorId = ctx.palette.doorId();
		for (int i = 0; i < width; i++) {
			int u = r.u0() + i;
			boolean rightHinge = width == 2 && i == 1;
			ctx.set(f.at(u, base + 1, -1), Blocks.door(doorId, f.inward(), false, rightHinge), Part.DOOR);
			ctx.set(f.at(u, base + 2, -1), Blocks.door(doorId, f.inward(), true, rightHinge), Part.DOOR);
			if (transom) {
				ctx.set(f.at(u, base + 3, -1), ctx.palette.glassPane(), Part.WINDOW);
			}
		}
		Windows.innerFrame(ctx, f, r);
		// The floor in front of the door must stay walkable, so the frame ring has no bottom here.
		for (int u = r.u0(); u <= r.u1(); u++) {
			Vec3 threshold = f.at(u, base, -1);
			ctx.set(threshold, ctx.palette.full(Role.FLOOR, threshold), Part.FLOOR);
		}
		Windows.sideFrames(ctx, f, r);
		Windows.lintel(ctx, f, r);
		if (door.type() == Door.Type.ARCHED && width >= 2) {
			Windows.archHead(ctx, f, r);
		} else if (ctx.detail(Level.MEDIUM)) {
			Vec3 key = f.at((r.u0() + r.u1()) / 2, r.t() + 1, 0);
			ctx.set(key, ctx.palette.full(Role.ACCENT, key), Part.LINTEL);
		}
		if (door.steps() || base > 0) {
			steps(ctx, f, r, base);
		}
		if (door.awning()) {
			for (int u = r.u0() - 1; u <= r.u1() + 1; u++) {
				for (int w = 1; w <= 2; w++) {
					Vec3 p = f.at(u, r.t() + 1, w);
					ctx.set(p, ctx.palette.trapdoor(Role.DOOR, p, f.outward(), Half.TOP, false), Part.DETAIL);
				}
			}
		}
		if (ctx.spec.detail().lighting()) {
			lanterns(ctx, f, r, base);
		}
		ctx.buffer.addFeature(new Feature.Opening(Feature.Opening.Kind.DOOR, f.name(), f.at(r.u0(), r.b(), 0),
			f.outward(), f.right(), r.width(), 2, false));
	}

	/** Stairs climbing from the ground to the door threshold, on a solid foundation. */
	private static void steps(BuildContext ctx, FacadeFrame f, Windows.Rect r, int base) {
		for (int k = 0; k <= base; k++) {
			int w = 1 + k;
			int y = base - k;
			for (int u = r.u0(); u <= r.u1(); u++) {
				Vec3 p = f.at(u, y, w);
				ctx.set(p, ctx.palette.stairs(Role.FOUNDATION, p, f.inward(), Half.BOTTOM), Part.STEP);
				for (int below = 0; below < y; below++) {
					Vec3 q = f.at(u, below, w);
					ctx.set(q, ctx.palette.full(Role.FOUNDATION, q), Part.STEP);
				}
			}
		}
	}

	/** A lantern on a corbel bracket either side of the door. */
	private static void lanterns(BuildContext ctx, FacadeFrame f, Windows.Rect r, int base) {
		for (int u : new int[] {r.u0() - 1, r.u1() + 1}) {
			Vec3 bracket = f.at(u, base + 2, 1);
			Vec3 lamp = bracket.above();
			if (u < 1 || u > f.length() - 2 || ctx.buffer.isSolidAt(bracket) || ctx.buffer.isSolidAt(lamp)) {
				continue;
			}
			ctx.set(bracket, ctx.palette.stairs(Role.TRIM, bracket, f.inward(), Half.TOP), Part.LIGHT);
			ctx.set(lamp, Blocks.lantern("lantern", false), Part.LIGHT);
		}
	}
}
