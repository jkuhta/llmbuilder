package io.github.jkuhta.llmbuilder.generator.layout;

import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Wing;
import io.github.jkuhta.llmbuilder.spec.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Resolves the spec's footprint into masses in building-local coordinates. The main block spans
 * x 0..width-1 and z 0..depth-1 with the front facing south (+z), so "left" is west (x = 0).
 * Wings share the main block's wall line on the side they attach to.
 */
public final class Layout {
	private final List<Mass> masses;
	private final int plinth;
	/** Highest roof y per column, filled in by the roof planner; used for exposure tests. */
	private IntBinaryOperator roofTop = (x, z) -> Integer.MIN_VALUE;

	private Layout(List<Mass> masses, int plinth) {
		this.masses = masses;
		this.plinth = plinth;
	}

	public static Layout of(BuildingSpec spec) {
		var floors = spec.floors();
		int plinth = floors.plinth();
		List<Integer> bases = new ArrayList<>();
		int y = plinth;
		for (int h : floors.heights()) {
			bases.add(y);
			y += h;
		}
		int width = spec.footprint().width();
		int depth = spec.footprint().depth();
		List<Mass> masses = new ArrayList<>();
		Mass main = new Mass(0, 0, 0, width - 1, depth - 1, List.copyOf(bases), y, null);
		masses.add(main);
		for (Wing wing : spec.footprint().wings()) {
			int count = wing.floors() > 0 ? Math.min(wing.floors(), bases.size()) : bases.size();
			List<Integer> wingBases = List.copyOf(bases.subList(0, count));
			int wingTop = count < bases.size() ? bases.get(count) : y;
			FacadeFrame side = FacadeFrame.of(main, wing.side());
			int ww = Math.min(wing.width(), side.length());
			int u0 = switch (wing.align()) {
				case START -> 0;
				case END -> side.length() - ww;
				case CENTER -> (side.length() - ww) / 2;
			};
			Vec3 a = side.at(u0, 0, 0);
			Vec3 b = side.at(u0 + ww - 1, 0, wing.depth());
			masses.add(new Mass(masses.size(), Math.min(a.x(), b.x()), Math.min(a.z(), b.z()),
				Math.max(a.x(), b.x()), Math.max(a.z(), b.z()), wingBases, wingTop, wing.side()));
		}
		return new Layout(List.copyOf(masses), plinth);
	}

	public List<Mass> masses() {
		return masses;
	}

	public Mass main() {
		return masses.get(0);
	}

	public int plinth() {
		return plinth;
	}

	/** Frames for every side of every mass, except a wing's side that faces into the main block. */
	public List<FacadeFrame> facades() {
		List<FacadeFrame> result = new ArrayList<>();
		for (Mass m : masses) {
			for (Side side : Side.values()) {
				if (!m.isMain() && FacadeFrame.outwardOf(side) == FacadeFrame.outwardOf(m.attachedTo()).opposite()) {
					continue;
				}
				result.add(FacadeFrame.of(m, side));
			}
		}
		return result;
	}

	public boolean inAnyMass(int x, int z) {
		return masses.stream().anyMatch(m -> m.contains(x, z));
	}

	/** True if the column lies strictly inside the building, i.e. not on any outer wall line. */
	public boolean isInteriorColumn(int x, int z) {
		if (!inAnyMass(x, z)) {
			return false;
		}
		for (Dir d : Dir.values()) {
			if (!inAnyMass(x + d.dx, z + d.dz)) {
				return false;
			}
		}
		return inAnyMass(x + 1, z + 1) && inAnyMass(x - 1, z - 1) && inAnyMass(x + 1, z - 1) && inAnyMass(x - 1, z + 1);
	}

	public void setRoofTop(IntBinaryOperator roofTop) {
		this.roofTop = roofTop;
	}

	/**
	 * True if position p is enclosed by some mass other than {@code self}: inside its rect and at
	 * or below its walls or roof.
	 */
	public boolean enclosedByOther(Mass self, Vec3 p) {
		for (Mass m : masses) {
			if (m == self || !m.contains(p.x(), p.z())) {
				continue;
			}
			if (p.y() <= m.wallTop() || p.y() <= roofTop.applyAsInt(p.x(), p.z())) {
				return true;
			}
		}
		return false;
	}

	/** A wall cell is exposed if the cell just outside it is open air rather than another mass. */
	public boolean exposed(FacadeFrame frame, int u, int y) {
		Vec3 outside = frame.at(u, y, 1);
		return !enclosedByOther(frame.mass(), outside) && !enclosedByOther(frame.mass(), frame.at(u, y, 0));
	}
}
