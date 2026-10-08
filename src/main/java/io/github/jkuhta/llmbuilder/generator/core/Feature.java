package io.github.jkuhta.llmbuilder.generator.core;

/**
 * Semantic record of something the generator built, kept next to the blocks so checks, tests and
 * the detailer can reason about windows or doors without re-deriving them from raw blocks.
 */
public interface Feature {
	/**
	 * A window or door opening in a facade. Coordinates are the opening's lowest-left cell on the
	 * wall plane, its width and height, and the facade's outward direction and rightward direction.
	 * {@code sill} is false for opening types that have none, such as shopfronts and doors.
	 */
	record Opening(Kind kind, String facade, Vec3 origin, Dir outward, Dir right, int width, int height, boolean sill) implements Feature {
		public enum Kind {
			WINDOW,
			DOOR
		}

		/** Cell on the wall plane at opening-local column {@code u} and row {@code v}. */
		public Vec3 cell(int u, int v) {
			return origin.offset(right, u).above(v);
		}
	}

	/** Roof surface metadata: the highest point of the roof, used by chimneys and checks. */
	record RoofInfo(int baseY, int ridgeY) implements Feature {
	}
}
