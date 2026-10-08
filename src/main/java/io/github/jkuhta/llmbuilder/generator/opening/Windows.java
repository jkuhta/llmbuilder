package io.github.jkuhta.llmbuilder.generator.opening;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Feature;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.BayLayout;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Facade;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.WindowType;

import java.util.List;

/**
 * Windows with real depth. The opening is cut through the wall, the glass sits one block back in
 * a frame, a stair sill projects below and a trim lintel spans above. Optional hood mouldings,
 * arched heads and trapdoor shutters add further layers.
 */
public final class Windows implements Component {
	/** Opening rectangle in facade coordinates, inclusive. */
	record Rect(int u0, int u1, int b, int t) {
		int width() {
			return u1 - u0 + 1;
		}

		int height() {
			return t - b + 1;
		}
	}

	@Override
	public void apply(BuildContext ctx) {
		for (FacadeFrame f : ctx.layout.facades()) {
			Facade spec = ctx.facadeSpec(f);
			if (spec.blank()) {
				continue;
			}
			BayLayout.Result bays = ctx.bays(f);
			if (bays.bays().isEmpty()) {
				continue;
			}
			Mass m = f.mass();
			for (int floor = 0; floor < m.floors(); floor++) {
				int base = m.floorBases().get(floor);
				int h = floorHeight(m, floor);
				WindowType type = spec.windowType(floor);
				if (type == WindowType.NONE) {
					continue;
				}
				if (type == WindowType.RIBBON) {
					BayLayout.Bay first = bays.bays().get(0);
					BayLayout.Bay last = bays.bays().get(bays.bays().size() - 1);
					window(ctx, f, spec, type, new Rect(first.u0(), last.u1(), base + 2, base + h - 1));
					continue;
				}
				for (BayLayout.Bay bay : bays.bays()) {
					if (hasDoor(ctx, f, floor, bay)) {
						continue;
					}
					Rect r = rect(type, spec, bay, bays, base, h);
					if (r != null) {
						window(ctx, f, spec, type, r);
					}
				}
			}
		}
	}

	/** Doors take the place of the ground-floor window in their bay. */
	static boolean hasDoor(BuildContext ctx, FacadeFrame f, int floor, BayLayout.Bay bay) {
		return floor == 0 && f.mass().isMain() && ctx.facadeSpec(f).doors().stream().anyMatch(d -> d.bay() == bay.index());
	}

	static int floorHeight(Mass m, int floor) {
		List<Integer> bases = m.floorBases();
		int next = floor + 1 < bases.size() ? bases.get(floor + 1) : m.wallTop();
		return next - bases.get(floor);
	}

	private static Rect rect(WindowType type, Facade spec, BayLayout.Bay bay, BayLayout.Result bays, int base, int h) {
		int top = base + h - 1;
		int u0 = bay.u0();
		int u1 = bay.u1();
		int requested = spec.windows().height();
		return switch (type) {
			case ROUND -> {
				int c = (u0 + u1) / 2;
				int y = base + Math.max(2, h / 2);
				yield new Rect(c, c + (bay.width() % 2 == 0 ? 1 : 0), y, y);
			}
			case SLIT -> {
				int c = (u0 + u1) / 2;
				yield new Rect(c, c, base + 2, Math.min(top, base + 3));
			}
			case SHOPFRONT -> {
				// Shop windows widen into the piers, leaving at least one column of wall between them.
				int grow = 0;
				for (BayLayout.Gap g : bays.gaps()) {
					if (g.u0() + g.width() == u0 || g.u0() == u1 + 1) {
						grow = Math.max(grow, (g.width() - 1) / 2);
					}
				}
				yield new Rect(u0 - grow, u1 + grow, base + 1, top);
			}
			case CASEMENT -> {
				int height = requested > 0 ? requested : Math.min(2, Math.max(1, h - 2));
				yield new Rect(u0, u1, Math.max(base + 1, top - height + 1), top);
			}
			default -> {
				int height = requested > 0 ? requested : Math.max(1, h - 2);
				yield new Rect(u0, u1, Math.max(base + 1, top - height + 1), top);
			}
		};
	}

	private static void window(BuildContext ctx, FacadeFrame f, Facade spec, WindowType type, Rect r) {
		for (int u = r.u0() - 1; u <= r.u1() + 1; u++) {
			for (int y = r.b() - 1; y <= r.t() + 1; y++) {
				if (u < 1 || u > f.length() - 2 || !ctx.layout.exposed(f, u, y)) {
					return;
				}
			}
		}
		boolean sill = type != WindowType.SHOPFRONT && type != WindowType.ROUND;
		cutOpening(ctx, f, r, Part.WINDOW);
		Block pane = ctx.palette.glassPane();
		for (int u = r.u0(); u <= r.u1(); u++) {
			for (int y = r.b(); y <= r.t(); y++) {
				ctx.set(f.at(u, y, -1), pane, Part.WINDOW);
			}
		}
		innerFrame(ctx, f, r);
		sideFrames(ctx, f, r);
		lintel(ctx, f, r);
		if (sill) {
			for (int u = r.u0(); u <= r.u1(); u++) {
				Vec3 below = f.at(u, r.b() - 1, 0);
				ctx.set(below, ctx.palette.full(Role.TRIM, below), Part.SILL);
				Vec3 p = f.at(u, r.b() - 1, 1);
				ctx.set(p, ctx.palette.stairs(Role.TRIM, p, f.inward(), Half.TOP), Part.SILL);
			}
		}
		if ((type == WindowType.ARCHED || type == WindowType.LANCET) && r.width() >= 2 && r.height() >= 2) {
			archHead(ctx, f, r);
		} else if (type == WindowType.ARCHED && ctx.detail(Level.MEDIUM)) {
			Vec3 key = f.at((r.u0() + r.u1()) / 2, r.t() + 1, 0);
			ctx.set(key, ctx.palette.full(Role.ACCENT, key), Part.LINTEL);
		}
		if (spec.windows().hood() && ctx.detail(Level.MEDIUM)) {
			for (int u = r.u0() - 1; u <= r.u1() + 1; u++) {
				Vec3 p = f.at(u, r.t() + 1, 1);
				ctx.setIfFree(p, ctx.palette.slab(Role.TRIM, p, Half.BOTTOM), Part.LINTEL);
			}
		}
		if (spec.windows().shutters() && type != WindowType.SHOPFRONT && type != WindowType.RIBBON && roomForShutters(ctx, f)) {
			shutters(ctx, f, r);
		}
		ctx.buffer.addFeature(new Feature.Opening(Feature.Opening.Kind.WINDOW, f.name(), f.at(r.u0(), r.b(), 0),
			f.outward(), f.right(), r.width(), r.height(), sill));
	}

	static void cutOpening(BuildContext ctx, FacadeFrame f, Rect r, Part part) {
		for (int u = r.u0(); u <= r.u1(); u++) {
			for (int y = r.b(); y <= r.t(); y++) {
				ctx.set(f.at(u, y, 0), Block.AIR, part);
				// Anything projecting in front of an opening (bands, plinth) would block it.
				Vec3 front = f.at(u, y, 1);
				Part existing = ctx.buffer.part(front);
				if (existing == Part.BAND || existing == Part.CORNICE) {
					ctx.buffer.remove(front);
				}
			}
		}
	}

	/** Frame ring one block behind the wall face, holding the glass. */
	static void innerFrame(BuildContext ctx, FacadeFrame f, Rect r) {
		for (int u = r.u0() - 1; u <= r.u1() + 1; u++) {
			for (int y = r.b() - 1; y <= r.t() + 1; y++) {
				boolean ring = u == r.u0() - 1 || u == r.u1() + 1 || y == r.b() - 1 || y == r.t() + 1;
				Vec3 p = f.at(u, y, -1);
				// Floor plates and roof decks stay intact; the frame only fills the wall around them.
				if (ring && ctx.buffer.part(p) != Part.FLOOR) {
					ctx.set(p, ctx.palette.full(Role.WINDOW_FRAME, p), Part.FRAME);
				}
			}
		}
	}

	/**
	 * Frame-coloured jambs in the wall plane, only where the pier is wide enough to keep at least
	 * one column of wall between neighbouring jambs; otherwise the facade would read as all frame.
	 */
	static void sideFrames(BuildContext ctx, FacadeFrame f, Rect r) {
		for (int u : new int[] {r.u0() - 1, r.u1() + 1}) {
			BayLayout.Gap gap = gapAt(ctx, f, u);
			if (gap == null || gap.width() < (gap.inner() ? 3 : 2)) {
				continue;
			}
			for (int y = r.b(); y <= r.t(); y++) {
				Vec3 p = f.at(u, y, 0);
				ctx.set(p, ctx.palette.full(Role.WINDOW_FRAME, p), Part.FRAME);
			}
		}
	}

	private static BayLayout.Gap gapAt(BuildContext ctx, FacadeFrame f, int u) {
		for (BayLayout.Gap gap : ctx.bays(f).gaps()) {
			if (u >= gap.u0() && u < gap.u0() + gap.width()) {
				return gap;
			}
		}
		return null;
	}

	static void lintel(BuildContext ctx, FacadeFrame f, Rect r) {
		for (int u = r.u0() - 1; u <= r.u1() + 1; u++) {
			Vec3 p = f.at(u, r.t() + 1, 0);
			ctx.set(p, ctx.palette.full(Role.TRIM, p), Part.LINTEL);
		}
	}

	/** Rounds or points the head of the opening with upside-down stairs in its top corners. */
	static void archHead(BuildContext ctx, FacadeFrame f, Rect r) {
		Vec3 left = f.at(r.u0(), r.t(), 0);
		Vec3 right = f.at(r.u1(), r.t(), 0);
		ctx.set(left, ctx.palette.stairs(Role.TRIM, left, f.left(), Half.TOP), Part.LINTEL);
		ctx.set(right, ctx.palette.stairs(Role.TRIM, right, f.right(), Half.TOP), Part.LINTEL);
	}

	/**
	 * Shutters need a free wall column on each side of every window; with narrower piers no
	 * window gets them, so the facade stays symmetric.
	 */
	private static boolean roomForShutters(BuildContext ctx, FacadeFrame f) {
		for (BayLayout.Gap gap : ctx.bays(f).gaps()) {
			if (gap.width() < (gap.inner() ? 2 : 1)) {
				ctx.warn("shutters skipped on " + f.name() + ": piers between windows are too narrow");
				return false;
			}
		}
		return true;
	}

	private static void shutters(BuildContext ctx, FacadeFrame f, Rect r) {
		int left = r.u0() - 1;
		int right = r.u1() + 1;
		for (int y = r.b(); y <= r.t(); y++) {
			if (ctx.buffer.isSolidAt(f.at(left, y, 1)) || ctx.buffer.isSolidAt(f.at(right, y, 1))) {
				return;
			}
		}
		for (int y = r.b(); y <= r.t(); y++) {
			for (int u : new int[] {left, right}) {
				Vec3 p = f.at(u, y, 1);
				ctx.set(p, ctx.palette.trapdoor(Role.DOOR, p, f.outward(), Half.TOP, true), Part.SHUTTER);
			}
		}
	}
}
