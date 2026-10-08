package io.github.jkuhta.llmbuilder.generator.core;

import java.util.List;

/** Shape classification of block ids, mirroring how vanilla decides connections and support. */
public final class BlockShapes {
	private static final List<String> PARTIAL_SUFFIXES = List.of(
		"_stairs", "_slab", "_wall", "_fence", "_fence_gate", "_pane", "_trapdoor", "_door", "_button",
		"_pressure_plate", "_carpet", "_sign", "_leaves", "_torch", "_banner"
	);

	private BlockShapes() {
	}

	public static boolean isStairs(Block b) {
		return b != null && b.path().endsWith("_stairs");
	}

	public static boolean isPaneLike(Block b) {
		return b != null && (b.path().endsWith("_pane") || b.path().equals("iron_bars"));
	}

	public static boolean isFence(Block b) {
		return b != null && b.path().endsWith("_fence");
	}

	public static boolean isFenceGate(Block b) {
		return b != null && b.path().endsWith("_fence_gate");
	}

	public static boolean isWall(Block b) {
		return b != null && b.path().endsWith("_wall") && !b.path().endsWith("_sign_wall");
	}

	/** Full opaque-faced cube: supports attachments and makes panes, fences and walls connect. */
	public static boolean isFullCube(Block b) {
		if (b == null || b.isAir()) {
			return false;
		}
		String p = b.path();
		if (p.endsWith("_slab")) {
			return "double".equals(b.prop("type"));
		}
		for (String suffix : PARTIAL_SUFFIXES) {
			if (p.endsWith(suffix)) {
				return false;
			}
		}
		return !p.equals("iron_bars") && !p.contains("lantern") && !p.equals("torch") && !p.equals("chain")
			&& !p.equals("flower_pot") && !p.startsWith("potted_") && !p.equals("ladder") && !p.contains("campfire")
			&& !p.endsWith("_bed") && !p.equals("bell");
	}
}
