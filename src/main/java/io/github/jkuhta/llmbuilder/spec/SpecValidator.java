package io.github.jkuhta.llmbuilder.spec;

import io.github.jkuhta.llmbuilder.generator.material.Material;
import io.github.jkuhta.llmbuilder.generator.material.Materials;
import io.github.jkuhta.llmbuilder.generator.material.Shape;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Facade;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Footprint;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Variant;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Wing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semantic checks that JSON structure alone cannot express: things fit, materials exist and
 * support the shapes their role needs, and sizes stay within server limits.
 */
public final class SpecValidator {
	/** Server-configurable size limits. */
	public record Limits(int maxSize, int maxHeight) {
		public static final Limits DEFAULT = new Limits(48, 64);
	}

	public record Result(List<String> errors, List<String> warnings) {
		public boolean ok() {
			return errors.isEmpty();
		}
	}

	/** Detailer modules the generator knows; unknown names are ignored with a warning. */
	public static final Set<String> KNOWN_MODULES = Set.of(
		"flower_boxes", "weathering_moss_on_foundation", "wall_lanterns_at_doors", "ivy_on_walls",
		"cracked_bricks", "hanging_signs", "window_plants", "roof_vents"
	);

	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	private SpecValidator() {
	}

	public static Result validate(BuildingSpec spec, Limits limits) {
		SpecValidator v = new SpecValidator();
		v.check(spec, limits);
		return new Result(List.copyOf(v.errors), List.copyOf(v.warnings));
	}

	private void check(BuildingSpec spec, Limits limits) {
		Footprint fp = spec.footprint();
		if (fp.width() > limits.maxSize()) {
			errors.add("footprint.width: " + fp.width() + " exceeds the server limit of " + limits.maxSize());
		}
		if (fp.depth() > limits.maxSize()) {
			errors.add("footprint.depth: " + fp.depth() + " exceeds the server limit of " + limits.maxSize());
		}
		checkWings(spec);
		checkFloors(spec, limits);
		checkRoof(spec);
		checkPalette(spec);
		for (Map.Entry<Side, Facade> e : spec.facades().entrySet()) {
			checkFacade(spec, e.getKey(), e.getValue());
		}
		for (int i = 0; i < spec.chimneys().size(); i++) {
			if (spec.chimneys().get(i).side() == Side.FRONT && spec.roof().type() != Roof.Type.FLAT) {
				warnings.add("chimneys[" + i + "]: chimneys on the front are unusual; consider left, right or back");
			}
		}
		for (String module : spec.detail().modules()) {
			if (!KNOWN_MODULES.contains(module)) {
				warnings.add("detail.modules: unknown module \"" + module + "\" will be ignored");
			}
		}
	}

	private void checkWings(BuildingSpec spec) {
		Footprint fp = spec.footprint();
		List<Wing> wings = fp.wings();
		switch (fp.shape()) {
			case RECT -> {
				if (!wings.isEmpty()) {
					errors.add("footprint.wings: a rect footprint has no wings; use shape l, u or composite");
				}
			}
			case L -> {
				if (wings.size() != 1) {
					errors.add("footprint.wings: an L footprint needs exactly 1 wing (got " + wings.size() + ")");
				} else if (wings.get(0).align() == Wing.Align.CENTER) {
					errors.add("footprint.wings[0].align: an L wing must be at start or end, not center");
				}
			}
			case U -> {
				if (wings.size() != 2) {
					errors.add("footprint.wings: a U footprint needs exactly 2 wings (got " + wings.size() + ")");
				} else if (wings.get(0).side() != wings.get(1).side()
					|| Set.of(wings.get(0).align(), wings.get(1).align()).size() != 2
					|| wings.stream().anyMatch(w -> w.align() == Wing.Align.CENTER)) {
					errors.add("footprint.wings: a U footprint needs 2 wings on the same side, one at start and one at end");
				}
			}
			case COMPOSITE -> {
				if (wings.isEmpty()) {
					errors.add("footprint.wings: a composite footprint needs at least 1 wing");
				}
			}
		}
		for (int i = 0; i < wings.size(); i++) {
			Wing w = wings.get(i);
			String p = "footprint.wings[" + i + "]";
			int sideLength = (w.side() == Side.FRONT || w.side() == Side.BACK) ? fp.width() : fp.depth();
			if (w.width() > sideLength) {
				errors.add(p + ".width: " + w.width() + " is wider than the " + w.side().id() + " side it attaches to (" + sideLength + ")");
			}
			if (w.floors() > spec.floors().count()) {
				errors.add(p + ".floors: a wing cannot have more floors (" + w.floors() + ") than the main block (" + spec.floors().count() + ")");
			}
		}
	}

	private void checkFloors(BuildingSpec spec, Limits limits) {
		var floors = spec.floors();
		if (floors.heights().size() != floors.count()) {
			errors.add("floors.heights: has " + floors.heights().size() + " entries but floors.count is " + floors.count()
				+ "; give one height per floor or a single height for all");
		}
		for (int i = 0; i < floors.heights().size(); i++) {
			int h = floors.heights().get(i);
			if (h > 6 && floors.count() > 1) {
				warnings.add("floors.heights[" + i + "]: " + h + " m is very tall for a storey; 3-5 is typical");
			}
		}
		int roofAllowance = spec.roof().type() == Roof.Type.FLAT ? 2 : Math.min(spec.footprint().width(), spec.footprint().depth());
		int estimated = floors.plinth() + floors.totalHeight() + roofAllowance;
		if (estimated > limits.maxHeight()) {
			errors.add("floors: estimated height " + estimated + " exceeds the server limit of " + limits.maxHeight());
		}
	}

	private void checkRoof(BuildingSpec spec) {
		Roof roof = spec.roof();
		if (roof.type() == Roof.Type.DOME && Math.abs(spec.footprint().width() - spec.footprint().depth()) > 2) {
			errors.add("roof.type: a dome needs a near-square footprint (width and depth within 2)");
		}
		if (roof.type() == Roof.Type.STEPPED_GABLE && roof.overhang() > 0) {
			warnings.add("roof.overhang: stepped gables have no overhang at the gable ends; it only applies to the eaves");
		}
		if (roof.parapet() != Roof.Parapet.NONE && roof.type() != Roof.Type.FLAT) {
			warnings.add("roof.parapet: parapets only apply to flat roofs and will be ignored");
		}
	}

	private void checkPalette(BuildingSpec spec) {
		var palette = spec.palette();
		for (Map.Entry<Role, List<Variant>> e : palette.roles().entrySet()) {
			Role role = e.getKey();
			for (int i = 0; i < e.getValue().size(); i++) {
				String name = e.getValue().get(i).material();
				String p = "palette." + camel(role) + "[" + i + "]";
				if (Materials.find(name).isEmpty()) {
					errors.add(p + ": unknown material \"" + name + "\"; " + suggest(name));
					continue;
				}
				for (Shape needed : requiredShapes(role, spec)) {
					if (!Materials.supports(name, needed)) {
						errors.add(p + ": material \"" + name + "\" has no " + needed.name().toLowerCase() + " form, which the " + role.id() + " role needs");
					}
				}
			}
		}
		Material glass = Materials.find(palette.glass()).orElse(null);
		if (glass == null || !glass.has(Shape.PANE)) {
			errors.add("palette.glass: \"" + palette.glass() + "\" is not a glass type with panes (e.g. glass, white_stained_glass)");
		}
	}

	private static List<Shape> requiredShapes(Role role, BuildingSpec spec) {
		return switch (role) {
			case ROOF -> spec.roof().type() == Roof.Type.FLAT ? List.of(Shape.SLAB) : List.of(Shape.STAIRS, Shape.SLAB);
			case TRIM -> List.of(Shape.STAIRS, Shape.SLAB);
			case DOOR -> List.of(Shape.DOOR, Shape.TRAPDOOR);
			case FLOOR -> List.of(Shape.SLAB);
			default -> List.of();
		};
	}

	private void checkFacade(BuildingSpec spec, Side side, Facade facade) {
		String p = "facades." + side.id();
		int length = (side == Side.FRONT || side == Side.BACK) ? spec.footprint().width() : spec.footprint().depth();
		int span = length - 2;
		int maxBays = (span + 1) / 2;
		if (facade.bays() > maxBays) {
			errors.add(p + ".bays: " + facade.bays() + " bays do not fit a " + length + " m facade (max " + maxBays + ")");
		}
		for (int i = 0; i < facade.doors().size(); i++) {
			var door = facade.doors().get(i);
			if (door.bay() >= facade.bays()) {
				errors.add(p + ".doors[" + i + "].bay: bay " + door.bay() + " does not exist (bays are 0.." + (facade.bays() - 1) + ")");
			}
		}
		for (Integer floor : facade.floorWindows().keySet()) {
			if (floor < 0 || floor >= spec.floors().count()) {
				errors.add(p + ".floorWindows." + floor + ": floor index must be 0.." + (spec.floors().count() - 1));
			}
		}
		for (int i = 0; i < facade.balconies().size(); i++) {
			var balcony = facade.balconies().get(i);
			if (balcony.floor() >= spec.floors().count()) {
				errors.add(p + ".balconies[" + i + "].floor: floor " + balcony.floor() + " does not exist");
			}
			for (int bay : balcony.bays()) {
				if (bay >= facade.bays()) {
					errors.add(p + ".balconies[" + i + "].bays: bay " + bay + " does not exist");
				}
			}
		}
		if (facade.dormers() > 0 && spec.roof().type() == Roof.Type.FLAT) {
			warnings.add(p + ".dormers: flat roofs cannot have dormers; they will be ignored");
		}
	}

	private static String camel(Role role) {
		String id = role.id();
		int i = id.indexOf('_');
		return i < 0 ? id : id.substring(0, i) + Character.toUpperCase(id.charAt(i + 1)) + id.substring(i + 2);
	}

	private static String suggest(String name) {
		String best = null;
		int bestScore = Integer.MAX_VALUE;
		for (String candidate : Materials.all().keySet()) {
			int d = distance(name, candidate);
			if (d < bestScore) {
				bestScore = d;
				best = candidate;
			}
		}
		return bestScore <= 4 ? "did you mean \"" + best + "\"?" : "see the list of allowed materials";
	}

	private static int distance(String a, String b) {
		int[] prev = new int[b.length() + 1];
		int[] cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			prev[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] t = prev;
			prev = cur;
			cur = t;
		}
		return prev[b.length()];
	}
}
