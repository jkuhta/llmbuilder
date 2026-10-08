package io.github.jkuhta.llmbuilder.spec;

import java.util.List;
import java.util.Map;

/**
 * High-level architectural description of a building, produced by the planner LLM or written by
 * hand. Contains no block coordinates; the generator derives all geometry from it.
 * 1 block = 1 metre.
 */
public record BuildingSpec(
	String name,
	String style,
	String era,
	Footprint footprint,
	Floors floors,
	Roof roof,
	Walls walls,
	Palette palette,
	Map<Side, Facade> facades,
	List<Chimney> chimneys,
	Detail detail,
	long seed
) {
	public Facade facade(Side side) {
		return facades.get(side);
	}

	public record Footprint(Shape shape, int width, int depth, List<Wing> wings) {
		public enum Shape implements SpecEnum {
			RECT,
			L,
			U,
			COMPOSITE
		}
	}

	/** A rectangular mass attached to one side of the main block. */
	public record Wing(Side side, Align align, int width, int depth, int floors) {
		/** Where the wing sits along its side; start is the viewer's left when facing that side. */
		public enum Align implements SpecEnum {
			START,
			CENTER,
			END
		}
	}

	public record Floors(int count, List<Integer> heights, GroundFloor groundFloor, int plinth) {
		public int totalHeight() {
			return heights.stream().mapToInt(Integer::intValue).sum();
		}

		public enum GroundFloor implements SpecEnum {
			STANDARD,
			SHOPFRONT,
			RAISED_BASEMENT
		}
	}

	public record Roof(Type type, Pitch pitch, int overhang, Ridge ridge, Parapet parapet, boolean cornice) {
		public enum Type implements SpecEnum {
			GABLE,
			HIP,
			MANSARD,
			FLAT,
			STEPPED_GABLE,
			DOME
		}

		/** low = 1:2 (slabs), medium = 1:1 (stairs), steep = 2:1 (stacked stairs). */
		public enum Pitch implements SpecEnum {
			LOW,
			MEDIUM,
			STEEP
		}

		/** Direction the ridge runs: width = left to right (gables on the sides), depth = front to back. */
		public enum Ridge implements SpecEnum {
			AUTO,
			WIDTH,
			DEPTH
		}

		public enum Parapet implements SpecEnum {
			NONE,
			PLAIN,
			CRENELLATED
		}
	}

	public record Walls(Corners corners, boolean bands, Framing framing) {
		public enum Corners implements SpecEnum {
			NONE,
			PILLARS,
			QUOINS
		}

		public enum Framing implements SpecEnum {
			NONE,
			TIMBER
		}
	}

	public record Palette(java.util.Map<Role, List<Variant>> roles, String glass) {
		public List<Variant> variants(Role role) {
			return roles.get(role);
		}

		public String primary(Role role) {
			return roles.get(role).get(0).material();
		}

		public enum Role implements SpecEnum {
			WALL,
			TRIM,
			ACCENT,
			ROOF,
			FOUNDATION,
			WINDOW_FRAME,
			DOOR,
			FLOOR
		}

		public record Variant(String material, double weight) {
		}
	}

	public record Facade(
		int bays,
		Windows windows,
		Map<Integer, WindowType> floorWindows,
		List<Door> doors,
		List<Balcony> balconies,
		int dormers,
		Piers piers,
		boolean blank
	) {
		/** Window type for a floor, honouring per-floor overrides. */
		public WindowType windowType(int floor) {
			return floorWindows.getOrDefault(floor, windows.type());
		}

		public enum Piers implements SpecEnum {
			NONE,
			PILASTERS,
			BUTTRESSES
		}
	}

	public record Windows(WindowType type, int width, int height, boolean shutters, boolean hood) {
	}

	public enum WindowType implements SpecEnum {
		SASH,
		CASEMENT,
		ARCHED,
		LANCET,
		ROUND,
		SHOPFRONT,
		RIBBON,
		SLIT,
		NONE
	}

	public record Door(int bay, Type type, boolean steps, boolean awning) {
		public enum Type implements SpecEnum {
			SINGLE,
			DOUBLE,
			ARCHED
		}
	}

	public record Balcony(int floor, List<Integer> bays, Type type) {
		public enum Type implements SpecEnum {
			SINGLE,
			CONTINUOUS
		}
	}

	/** A chimney on a side wall; position runs 0..1 along that side from the viewer's left. */
	public record Chimney(Side side, double position) {
	}

	public record Detail(Level level, boolean interior, boolean landscaping, boolean lighting, List<String> modules) {
		public enum Level implements SpecEnum {
			LOW,
			MEDIUM,
			HIGH
		}
	}
}
