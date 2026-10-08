package io.github.jkuhta.llmbuilder.generator.material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of every material family the planner may use. Ids are plain vanilla 1.21.11 block ids;
 * a test checks each one against the game's blockstate files.
 */
public final class Materials {
	private static final Map<String, Material> REGISTRY = new LinkedHashMap<>();

	public static final String[] WOODS = {
		"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"
	};

	private static final String[] COLORS = {
		"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
		"lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"
	};

	static {
		// Stone families: name, full block id, prefix for stairs/slab/wall, has wall
		stone("stone", "stone", "stone", false, "cobblestone");
		stone("cobblestone", "cobblestone", "cobblestone", true, null);
		stone("mossy_cobblestone", "mossy_cobblestone", "mossy_cobblestone", true, "cobblestone");
		stone("stone_bricks", "stone_bricks", "stone_brick", true, null);
		stone("mossy_stone_bricks", "mossy_stone_bricks", "mossy_stone_brick", true, "stone_bricks");
		fullOnly("cracked_stone_bricks", "stone_bricks");
		fullOnly("chiseled_stone_bricks", "stone_bricks");
		slabOnly("smooth_stone", "smooth_stone", "stone");
		for (String s : new String[] {"granite", "diorite", "andesite"}) {
			stone(s, s, s, true, null);
			stone("polished_" + s, "polished_" + s, "polished_" + s, false, s);
		}
		stone("bricks", "bricks", "brick", true, null);
		stone("mud_bricks", "mud_bricks", "mud_brick", true, null);
		fullOnly("packed_mud", "mud_bricks");
		stone("sandstone", "sandstone", "sandstone", true, null);
		stone("smooth_sandstone", "smooth_sandstone", "smooth_sandstone", false, "sandstone");
		slabOnly("cut_sandstone", "cut_sandstone", "sandstone");
		fullOnly("chiseled_sandstone", "sandstone");
		stone("red_sandstone", "red_sandstone", "red_sandstone", true, null);
		stone("smooth_red_sandstone", "smooth_red_sandstone", "smooth_red_sandstone", false, "red_sandstone");
		slabOnly("cut_red_sandstone", "cut_red_sandstone", "red_sandstone");
		stone("cobbled_deepslate", "cobbled_deepslate", "cobbled_deepslate", true, null);
		stone("polished_deepslate", "polished_deepslate", "polished_deepslate", true, null);
		stone("deepslate_bricks", "deepslate_bricks", "deepslate_brick", true, null);
		stone("deepslate_tiles", "deepslate_tiles", "deepslate_tile", true, null);
		fullOnly("cracked_deepslate_bricks", "deepslate_bricks");
		fullOnly("cracked_deepslate_tiles", "deepslate_tiles");
		fullOnly("chiseled_deepslate", "deepslate_bricks");
		stone("tuff", "tuff", "tuff", true, null);
		stone("polished_tuff", "polished_tuff", "polished_tuff", true, null);
		stone("tuff_bricks", "tuff_bricks", "tuff_brick", true, null);
		fullOnly("chiseled_tuff", "tuff_bricks");
		stone("blackstone", "blackstone", "blackstone", true, null);
		stone("polished_blackstone", "polished_blackstone", "polished_blackstone", true, null);
		stone("polished_blackstone_bricks", "polished_blackstone_bricks", "polished_blackstone_brick", true, null);
		stone("quartz", "quartz_block", "quartz", false, "smooth_quartz");
		stone("smooth_quartz", "smooth_quartz", "smooth_quartz", false, "quartz");
		fullOnly("quartz_bricks", "smooth_quartz");
		fullOnly("chiseled_quartz_block", "smooth_quartz");
		stone("prismarine", "prismarine", "prismarine", true, null);
		stone("prismarine_bricks", "prismarine_bricks", "prismarine_brick", false, "prismarine");
		stone("dark_prismarine", "dark_prismarine", "dark_prismarine", false, "prismarine");
		stone("end_stone_bricks", "end_stone_bricks", "end_stone_brick", true, null);
		stone("nether_bricks", "nether_bricks", "nether_brick", true, null);
		stone("red_nether_bricks", "red_nether_bricks", "red_nether_brick", true, null);
		stone("resin_bricks", "resin_bricks", "resin_brick", true, null);
		fullOnly("calcite", "polished_diorite");
		fullOnly("dripstone_block", "andesite");
		fullOnly("smooth_basalt", "polished_blackstone");
		fullOnly("terracotta", null);
		fullOnly("moss_block", "mossy_cobblestone");
		for (String c : COLORS) {
			fullOnly(c + "_terracotta", null);
			fullOnly(c + "_concrete", null);
		}
		for (String prefix : new String[] {"", "exposed_", "weathered_", "oxidized_"}) {
			stone("waxed_" + prefix + "cut_copper", "waxed_" + prefix + "cut_copper", "waxed_" + prefix + "cut_copper", false, null);
			String block = prefix.isEmpty() ? "waxed_copper_block" : "waxed_" + prefix + "copper";
			fullOnly(block, "waxed_" + prefix + "cut_copper");
		}

		for (String wood : WOODS) {
			wood(wood);
		}

		// Glass: full block plus pane
		glass("glass", "glass", "glass_pane");
		fullOnly("tinted_glass", "glass");
		for (String c : COLORS) {
			glass(c + "_stained_glass", c + "_stained_glass", c + "_stained_glass_pane");
		}

		Map<Shape, String> iron = new EnumMap<>(Shape.class);
		iron.put(Shape.FULL, "iron_block");
		iron.put(Shape.PANE, "iron_bars");
		iron.put(Shape.TRAPDOOR, "iron_trapdoor");
		iron.put(Shape.DOOR, "iron_door");
		register(new Material("iron", Material.Kind.METAL, iron, null, false));
	}

	private Materials() {
	}

	private static void register(Material material) {
		REGISTRY.put(material.name(), material);
	}

	private static void stone(String name, String full, String prefix, boolean wall, String fallback) {
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, full);
		shapes.put(Shape.STAIRS, prefix + "_stairs");
		shapes.put(Shape.SLAB, prefix + "_slab");
		if (wall) {
			shapes.put(Shape.WALL, prefix + "_wall");
		}
		if (name.equals("nether_bricks")) {
			shapes.put(Shape.FENCE, "nether_brick_fence");
		}
		register(new Material(name, Material.Kind.STONE, shapes, fallback, false));
	}

	private static void slabOnly(String name, String full, String fallback) {
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, full);
		shapes.put(Shape.SLAB, full + "_slab");
		register(new Material(name, Material.Kind.STONE, shapes, fallback, false));
	}

	private static void fullOnly(String name, String fallback) {
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, name);
		Material.Kind kind = name.contains("glass") ? Material.Kind.GLASS : Material.Kind.STONE;
		register(new Material(name, kind, shapes, fallback, false));
	}

	private static void glass(String name, String full, String pane) {
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, full);
		shapes.put(Shape.PANE, pane);
		register(new Material(name, Material.Kind.GLASS, shapes, null, false));
	}

	private static void wood(String wood) {
		boolean nether = wood.equals("crimson") || wood.equals("warped");
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, wood + "_planks");
		shapes.put(Shape.STAIRS, wood + "_stairs");
		shapes.put(Shape.SLAB, wood + "_slab");
		shapes.put(Shape.FENCE, wood + "_fence");
		shapes.put(Shape.FENCE_GATE, wood + "_fence_gate");
		shapes.put(Shape.TRAPDOOR, wood + "_trapdoor");
		shapes.put(Shape.DOOR, wood + "_door");
		shapes.put(Shape.BUTTON, wood + "_button");
		register(new Material(wood, Material.Kind.WOOD, shapes, null, false));

		if (wood.equals("bamboo")) {
			register(log("bamboo_block", "bamboo"));
			register(log("stripped_bamboo_block", "bamboo"));
			return;
		}
		String log = nether ? wood + "_stem" : wood + "_log";
		register(log(log, wood));
		register(log("stripped_" + log, wood));
	}

	private static Material log(String id, String wood) {
		Map<Shape, String> shapes = new EnumMap<>(Shape.class);
		shapes.put(Shape.FULL, id);
		return new Material(id, Material.Kind.LOG, shapes, wood, true);
	}

	public static Optional<Material> find(String name) {
		if (name == null) {
			return Optional.empty();
		}
		String key = name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
		return Optional.ofNullable(REGISTRY.get(key));
	}

	public static Material get(String name) {
		return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown material: " + name));
	}

	public static Map<String, Material> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	/**
	 * Block id for the given shape, following the fallback chain when the material lacks it.
	 * Returns empty if no family in the chain has the shape.
	 */
	public static Optional<String> resolve(String materialName, Shape shape) {
		Material material = find(materialName).orElse(null);
		for (int depth = 0; material != null && depth < 5; depth++) {
			Optional<String> id = material.id(shape);
			if (id.isPresent()) {
				return id;
			}
			material = find(material.fallback()).orElse(null);
		}
		return Optional.empty();
	}

	public static boolean supports(String materialName, Shape shape) {
		return resolve(materialName, shape).isPresent();
	}
}
