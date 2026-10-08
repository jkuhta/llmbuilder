package io.github.jkuhta.llmbuilder.generator.layout;

import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.spec.Side;

/**
 * Local coordinate system for one wall of a mass, as seen from outside: {@code u} runs left to
 * right along the wall (0 and length-1 are the corners), {@code y} is world height and {@code w}
 * is depth, 0 on the wall plane, positive outward. Components write facades in these
 * coordinates so the same code works for every side.
 */
public record FacadeFrame(Mass mass, Side side, Dir outward, Vec3 origin, int length) {
	public Dir right() {
		return outward.opposite().cw();
	}

	public Dir left() {
		return right().opposite();
	}

	public Dir inward() {
		return outward.opposite();
	}

	public Vec3 at(int u, int y, int w) {
		Dir r = right();
		return new Vec3(origin.x() + r.dx * u + outward.dx * w, y, origin.z() + r.dz * u + outward.dz * w);
	}

	public String name() {
		return side.id() + (mass.isMain() ? "" : "#" + mass.index());
	}

	public static Dir outwardOf(Side side) {
		return switch (side) {
			case FRONT -> Dir.SOUTH;
			case BACK -> Dir.NORTH;
			case LEFT -> Dir.WEST;
			case RIGHT -> Dir.EAST;
		};
	}

	public static FacadeFrame of(Mass m, Side side) {
		return switch (side) {
			case FRONT -> new FacadeFrame(m, side, Dir.SOUTH, new Vec3(m.x0(), 0, m.z1()), m.width());
			case BACK -> new FacadeFrame(m, side, Dir.NORTH, new Vec3(m.x1(), 0, m.z0()), m.width());
			case LEFT -> new FacadeFrame(m, side, Dir.WEST, new Vec3(m.x0(), 0, m.z0()), m.depth());
			case RIGHT -> new FacadeFrame(m, side, Dir.EAST, new Vec3(m.x1(), 0, m.z1()), m.depth());
		};
	}
}
