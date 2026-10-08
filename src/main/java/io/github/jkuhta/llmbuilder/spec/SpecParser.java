package io.github.jkuhta.llmbuilder.spec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Balcony;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Chimney;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Door;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Facade;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Floors;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Footprint;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Variant;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Walls;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.WindowType;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Windows;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Wing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses BuildingSpec JSON. Collects every problem with its JSON path (e.g.
 * {@code facades.front.bays: must be between 0 and 16}) instead of stopping at the first one, so
 * the full list can be sent back to the planner LLM in a single retry.
 */
public final class SpecParser {
	public record Result(BuildingSpec spec, List<String> errors) {
		public boolean ok() {
			return errors.isEmpty();
		}
	}

	private final List<String> errors = new ArrayList<>();

	private SpecParser() {
	}

	public static Result parse(String json) {
		SpecParser parser = new SpecParser();
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			return new Result(null, List.of("invalid JSON: " + e.getMessage()));
		}
		if (!root.isJsonObject()) {
			return new Result(null, List.of("root must be a JSON object"));
		}
		BuildingSpec spec = parser.spec(root.getAsJsonObject());
		return parser.errors.isEmpty() ? new Result(spec, List.of()) : new Result(null, List.copyOf(parser.errors));
	}

	public static Result parse(JsonObject root) {
		return parse(root.toString());
	}

	private BuildingSpec spec(JsonObject o) {
		known(o, "", "name", "style", "era", "footprint", "floors", "roof", "walls", "palette", "facades", "chimneys", "detail", "seed");
		String name = string(o, "name", "", "Building", false);
		String style = string(o, "style", "", "generic", false);
		String era = string(o, "era", "", "", false);
		Footprint footprint = footprint(object(o, "footprint", "", true));
		Floors floors = floors(object(o, "floors", "", true));
		Roof roof = roof(object(o, "roof", "", true));
		Walls walls = walls(object(o, "walls", "", false));
		Palette palette = palette(object(o, "palette", "", true));
		Map<Side, Facade> facades = facades(object(o, "facades", "", true));
		List<Chimney> chimneys = new ArrayList<>();
		JsonArray chimneyArray = array(o, "chimneys", "", false);
		for (int i = 0; i < chimneyArray.size(); i++) {
			String cp = "chimneys[" + i + "]";
			chimneys.add(chimney(asObject(chimneyArray.get(i), cp), cp));
		}
		Detail detail = detail(object(o, "detail", "", false));
		long seed = o.has("seed") && o.get("seed").isJsonPrimitive() ? o.get("seed").getAsLong() : name.hashCode();
		return new BuildingSpec(name, style, era, footprint, floors, roof, walls, palette, facades, List.copyOf(chimneys), detail, seed);
	}

	private Footprint footprint(JsonObject o) {
		String p = "footprint";
		known(o, p, "shape", "width", "depth", "wings");
		Footprint.Shape shape = enumValue(o, "shape", p, Footprint.Shape.class, Footprint.Shape.RECT, false);
		int width = integer(o, "width", p, 0, 5, 64, true);
		int depth = integer(o, "depth", p, 0, 5, 64, true);
		List<Wing> wings = new ArrayList<>();
		JsonArray array = array(o, "wings", p, false);
		for (int i = 0; i < array.size(); i++) {
			String wp = p + ".wings[" + i + "]";
			JsonObject w = asObject(array.get(i), wp);
			known(w, wp, "side", "align", "width", "depth", "floors");
			wings.add(new Wing(
				enumValue(w, "side", wp, Side.class, Side.FRONT, true),
				enumValue(w, "align", wp, Wing.Align.class, Wing.Align.CENTER, false),
				integer(w, "width", wp, 0, 3, 64, true),
				integer(w, "depth", wp, 0, 3, 64, true),
				integer(w, "floors", wp, 0, 0, 12, false)
			));
		}
		return new Footprint(shape, width, depth, List.copyOf(wings));
	}

	private Floors floors(JsonObject o) {
		String p = "floors";
		known(o, p, "count", "heights", "groundFloor", "plinth");
		int count = integer(o, "count", p, 1, 1, 12, true);
		Floors.GroundFloor ground = enumValue(o, "groundFloor", p, Floors.GroundFloor.class, Floors.GroundFloor.STANDARD, false);
		int defaultPlinth = ground == Floors.GroundFloor.RAISED_BASEMENT ? 2 : 0;
		int plinth = integer(o, "plinth", p, defaultPlinth, 0, 4, false);
		List<Integer> heights = new ArrayList<>();
		JsonArray array = array(o, "heights", p, false);
		for (int i = 0; i < array.size(); i++) {
			heights.add(intValue(array.get(i), p + ".heights[" + i + "]", 3, 12));
		}
		if (heights.isEmpty()) {
			heights.add(4);
		}
		// A single height applies to every floor; otherwise the validator checks the count.
		while (heights.size() == 1 && count > 1 && heights.size() < count) {
			heights.add(heights.get(0));
		}
		return new Floors(count, List.copyOf(heights), ground, plinth);
	}

	private Roof roof(JsonObject o) {
		String p = "roof";
		known(o, p, "type", "pitch", "overhang", "ridge", "parapet", "cornice");
		Roof.Type type = enumValue(o, "type", p, Roof.Type.class, Roof.Type.GABLE, true);
		Roof.Pitch pitch = enumValue(o, "pitch", p, Roof.Pitch.class, Roof.Pitch.MEDIUM, false);
		int overhang = integer(o, "overhang", p, type == Roof.Type.STEPPED_GABLE ? 0 : 1, 0, 3, false);
		Roof.Ridge ridge = enumValue(o, "ridge", p, Roof.Ridge.class, Roof.Ridge.AUTO, false);
		Roof.Parapet parapet = enumValue(o, "parapet", p, Roof.Parapet.class,
			type == Roof.Type.FLAT ? Roof.Parapet.PLAIN : Roof.Parapet.NONE, false);
		boolean cornice = bool(o, "cornice", p, true);
		return new Roof(type, pitch, overhang, ridge, parapet, cornice);
	}

	private Walls walls(JsonObject o) {
		String p = "walls";
		known(o, p, "corners", "bands", "framing");
		return new Walls(
			enumValue(o, "corners", p, Walls.Corners.class, Walls.Corners.PILLARS, false),
			bool(o, "bands", p, true),
			enumValue(o, "framing", p, Walls.Framing.class, Walls.Framing.NONE, false)
		);
	}

	private Palette palette(JsonObject o) {
		String p = "palette";
		Map<Role, List<Variant>> roles = new EnumMap<>(Role.class);
		Set<String> allowed = new java.util.HashSet<>();
		allowed.add("glass");
		for (Role role : Role.values()) {
			String key = camel(role.id());
			allowed.add(key);
			if (!o.has(key)) {
				continue;
			}
			String rp = p + "." + key;
			List<Variant> variants = new ArrayList<>();
			JsonElement element = o.get(key);
			if (element.isJsonPrimitive()) {
				variants.add(new Variant(element.getAsString(), 1));
			} else if (element.isJsonArray()) {
				JsonArray array = element.getAsJsonArray();
				for (int i = 0; i < array.size(); i++) {
					String vp = rp + "[" + i + "]";
					JsonElement v = array.get(i);
					if (v.isJsonPrimitive()) {
						variants.add(new Variant(v.getAsString(), 1));
						continue;
					}
					JsonObject vo = asObject(v, vp);
					known(vo, vp, "material", "weight");
					String material = string(vo, "material", vp, null, true);
					double weight = number(vo, "weight", vp, 1, 0.001, 1000);
					if (material != null) {
						variants.add(new Variant(material, weight));
					}
				}
			} else {
				errors.add(rp + ": must be a material name or a list of {material, weight}");
			}
			if (variants.isEmpty()) {
				errors.add(rp + ": needs at least one material");
			} else {
				roles.put(role, List.copyOf(variants));
			}
		}
		for (String key : o.keySet()) {
			if (!allowed.contains(key)) {
				errors.add(p + "." + key + ": unknown field");
			}
		}
		for (Role required : List.of(Role.WALL, Role.ROOF)) {
			if (!roles.containsKey(required)) {
				errors.add(p + "." + camel(required.id()) + ": missing required field");
			}
		}
		// Optional roles default to sensible relatives of the required ones.
		roles.putIfAbsent(Role.TRIM, roles.getOrDefault(Role.WALL, List.of(new Variant("stone_bricks", 1))));
		roles.putIfAbsent(Role.FOUNDATION, List.of(new Variant("cobblestone", 1)));
		roles.putIfAbsent(Role.ACCENT, roles.get(Role.TRIM));
		roles.putIfAbsent(Role.WINDOW_FRAME, roles.get(Role.TRIM));
		roles.putIfAbsent(Role.DOOR, List.of(new Variant("spruce", 1)));
		roles.putIfAbsent(Role.FLOOR, List.of(new Variant("oak", 1)));
		String glass = string(o, "glass", p, "glass", false);
		return new Palette(Collections.unmodifiableMap(roles), glass);
	}

	private Map<Side, Facade> facades(JsonObject o) {
		String p = "facades";
		known(o, p, "front", "back", "left", "right");
		Map<Side, Facade> result = new EnumMap<>(Side.class);
		for (Side side : Side.values()) {
			if (o.has(side.id())) {
				String fp = p + "." + side.id();
				result.put(side, facade(asObject(o.get(side.id()), fp), fp));
			}
		}
		if (!result.containsKey(Side.FRONT)) {
			errors.add(p + ".front: missing required field");
			return result;
		}
		// Unspecified sides get a plain version of the front: same windows, no doors or balconies.
		Facade front = result.get(Side.FRONT);
		Facade plain = new Facade(front.bays(), front.windows(), Map.of(), List.of(), List.of(), 0, front.piers(), false);
		for (Side side : Side.values()) {
			result.putIfAbsent(side, side == Side.BACK ? plain : new Facade(Math.max(1, front.bays() - 1), front.windows(), Map.of(), List.of(), List.of(), 0, front.piers(), false));
		}
		return Collections.unmodifiableMap(result);
	}

	private Facade facade(JsonObject o, String p) {
		known(o, p, "bays", "windows", "floorWindows", "doors", "balconies", "dormers", "piers", "blank");
		int bays = integer(o, "bays", p, 3, 0, 16, true);
		Windows windows = windows(object(o, "windows", p, false), p + ".windows");
		Map<Integer, WindowType> floorWindows = new LinkedHashMap<>();
		JsonObject fw = object(o, "floorWindows", p, false);
		for (String key : fw.keySet()) {
			String fp = p + ".floorWindows." + key;
			try {
				int floor = Integer.parseInt(key);
				WindowType type = enumOf(fw.get(key), fp, WindowType.class);
				if (type != null) {
					floorWindows.put(floor, type);
				}
			} catch (NumberFormatException e) {
				errors.add(fp + ": key must be a floor index such as \"0\"");
			}
		}
		List<Door> doors = new ArrayList<>();
		JsonArray doorArray = array(o, "doors", p, false);
		for (int i = 0; i < doorArray.size(); i++) {
			String dp = p + ".doors[" + i + "]";
			JsonObject d = asObject(doorArray.get(i), dp);
			known(d, dp, "bay", "type", "steps", "awning");
			doors.add(new Door(
				integer(d, "bay", dp, 0, 0, 15, true),
				enumValue(d, "type", dp, Door.Type.class, Door.Type.SINGLE, false),
				bool(d, "steps", dp, true),
				bool(d, "awning", dp, false)
			));
		}
		List<Balcony> balconies = new ArrayList<>();
		JsonArray balconyArray = array(o, "balconies", p, false);
		for (int i = 0; i < balconyArray.size(); i++) {
			String bp = p + ".balconies[" + i + "]";
			JsonObject b = asObject(balconyArray.get(i), bp);
			known(b, bp, "floor", "bays", "type");
			List<Integer> bays2 = new ArrayList<>();
			JsonArray ba = array(b, "bays", bp, true);
			for (int j = 0; j < ba.size(); j++) {
				bays2.add(intValue(ba.get(j), bp + ".bays[" + j + "]", 0, 15));
			}
			balconies.add(new Balcony(
				integer(b, "floor", bp, 1, 1, 11, true),
				List.copyOf(bays2),
				enumValue(b, "type", bp, Balcony.Type.class, Balcony.Type.SINGLE, false)
			));
		}
		return new Facade(
			bays,
			windows,
			Collections.unmodifiableMap(floorWindows),
			List.copyOf(doors),
			List.copyOf(balconies),
			integer(o, "dormers", p, 0, 0, 8, false),
			enumValue(o, "piers", p, Facade.Piers.class, Facade.Piers.NONE, false),
			bool(o, "blank", p, false)
		);
	}

	private Windows windows(JsonObject o, String p) {
		known(o, p, "type", "width", "height", "shutters", "hood");
		return new Windows(
			enumValue(o, "type", p, WindowType.class, WindowType.SASH, false),
			integer(o, "width", p, 0, 0, 8, false),
			integer(o, "height", p, 0, 0, 10, false),
			bool(o, "shutters", p, false),
			bool(o, "hood", p, false)
		);
	}

	private Chimney chimney(JsonObject o, String p) {
		known(o, p, "side", "position");
		return new Chimney(
			enumValue(o, "side", p, Side.class, Side.LEFT, true),
			number(o, "position", p, 0.5, 0, 1)
		);
	}

	private Detail detail(JsonObject o) {
		String p = "detail";
		known(o, p, "level", "interior", "landscaping", "lighting", "modules");
		List<String> modules = new ArrayList<>();
		JsonArray array = array(o, "modules", p, false);
		for (int i = 0; i < array.size(); i++) {
			JsonElement e = array.get(i);
			if (e.isJsonPrimitive()) {
				modules.add(e.getAsString());
			} else {
				errors.add(p + ".modules[" + i + "]: must be a string");
			}
		}
		return new Detail(
			enumValue(o, "level", p, Detail.Level.class, Detail.Level.MEDIUM, false),
			bool(o, "interior", p, false),
			bool(o, "landscaping", p, false),
			bool(o, "lighting", p, true),
			List.copyOf(modules)
		);
	}

	// ---- low-level readers ----

	private static String camel(String snake) {
		StringBuilder sb = new StringBuilder();
		boolean upper = false;
		for (char c : snake.toCharArray()) {
			if (c == '_') {
				upper = true;
			} else {
				sb.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			}
		}
		return sb.toString();
	}

	private static String path(String parent, String key) {
		return parent.isEmpty() ? key : parent + "." + key;
	}

	private void known(JsonObject o, String p, String... keys) {
		Set<String> allowed = Set.of(keys);
		for (String key : o.keySet()) {
			if (!allowed.contains(key)) {
				errors.add(path(p, key) + ": unknown field (allowed: " + String.join(", ", keys) + ")");
			}
		}
	}

	private JsonObject object(JsonObject o, String key, String p, boolean required) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			if (required) {
				errors.add(path(p, key) + ": missing required field");
			}
			return new JsonObject();
		}
		return asObject(o.get(key), path(p, key));
	}

	private JsonObject asObject(JsonElement e, String p) {
		if (!e.isJsonObject()) {
			errors.add(p + ": must be an object");
			return new JsonObject();
		}
		return e.getAsJsonObject();
	}

	private JsonArray array(JsonObject o, String key, String p, boolean required) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			if (required) {
				errors.add(path(p, key) + ": missing required field");
			}
			return new JsonArray();
		}
		if (!o.get(key).isJsonArray()) {
			errors.add(path(p, key) + ": must be an array");
			return new JsonArray();
		}
		return o.getAsJsonArray(key);
	}

	private int integer(JsonObject o, String key, String p, int def, int min, int max, boolean required) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			if (required) {
				errors.add(path(p, key) + ": missing required field");
			}
			return def;
		}
		return intValue(o.get(key), path(p, key), min, max);
	}

	private int intValue(JsonElement e, String p, int min, int max) {
		if (!(e instanceof JsonPrimitive prim) || !prim.isNumber() || prim.getAsDouble() != Math.rint(prim.getAsDouble())) {
			errors.add(p + ": must be an integer");
			return min;
		}
		int value = prim.getAsInt();
		if (value < min || value > max) {
			errors.add(p + ": must be between " + min + " and " + max + " (got " + value + ")");
			return Math.max(min, Math.min(max, value));
		}
		return value;
	}

	private double number(JsonObject o, String key, String p, double def, double min, double max) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			return def;
		}
		JsonElement e = o.get(key);
		if (!(e instanceof JsonPrimitive prim) || !prim.isNumber()) {
			errors.add(path(p, key) + ": must be a number");
			return def;
		}
		double value = prim.getAsDouble();
		if (value < min || value > max) {
			errors.add(path(p, key) + ": must be between " + min + " and " + max + " (got " + value + ")");
			return Math.max(min, Math.min(max, value));
		}
		return value;
	}

	private boolean bool(JsonObject o, String key, String p, boolean def) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			return def;
		}
		JsonElement e = o.get(key);
		if (!(e instanceof JsonPrimitive prim) || !prim.isBoolean()) {
			errors.add(path(p, key) + ": must be true or false");
			return def;
		}
		return prim.getAsBoolean();
	}

	private String string(JsonObject o, String key, String p, String def, boolean required) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			if (required) {
				errors.add(path(p, key) + ": missing required field");
			}
			return def;
		}
		JsonElement e = o.get(key);
		if (!(e instanceof JsonPrimitive prim) || !prim.isString()) {
			errors.add(path(p, key) + ": must be a string");
			return def;
		}
		return prim.getAsString();
	}

	private <E extends Enum<E> & SpecEnum> E enumValue(JsonObject o, String key, String p, Class<E> type, E def, boolean required) {
		if (!o.has(key) || o.get(key).isJsonNull()) {
			if (required) {
				errors.add(path(p, key) + ": missing required field");
			}
			return def;
		}
		E value = enumOf(o.get(key), path(p, key), type);
		return value == null ? def : value;
	}

	private <E extends Enum<E> & SpecEnum> E enumOf(JsonElement e, String p, Class<E> type) {
		if (!(e instanceof JsonPrimitive prim) || !prim.isString()) {
			errors.add(p + ": must be one of " + SpecEnum.ids(type));
			return null;
		}
		return SpecEnum.parse(type, prim.getAsString()).orElseGet(() -> {
			errors.add(p + ": must be one of " + SpecEnum.ids(type) + " (got \"" + prim.getAsString() + "\")");
			return null;
		});
	}
}
