package io.github.jkuhta.llmbuilder.generator.material;

import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.Blocks;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Variant;

import java.util.List;

/**
 * Turns the spec's weighted material palette into concrete blocks. Variant choice is a
 * deterministic function of position and seed, mixing per-block noise with 2x2x2 patches so large
 * surfaces get natural, clustered texture variation rather than salt-and-pepper speckle.
 */
public final class BlockPalette {
	private final Palette palette;
	private final long seed;

	public BlockPalette(Palette palette, long seed) {
		this.palette = palette;
		this.seed = seed;
	}

	public String primary(Role role) {
		return palette.primary(role);
	}

	/** Material family chosen for this role at this position. */
	public String material(Role role, Vec3 pos) {
		List<Variant> variants = palette.variants(role);
		if (variants.size() == 1) {
			return variants.get(0).material();
		}
		double total = variants.stream().mapToDouble(Variant::weight).sum();
		double r = hash(seed + role.ordinal(), pos.x(), pos.y(), pos.z()) < 0.5
			? hash(seed + role.ordinal() * 31L, Math.floorDiv(pos.x(), 2), Math.floorDiv(pos.y(), 2), Math.floorDiv(pos.z(), 2))
			: hash(seed + role.ordinal() * 17L + 1, pos.x(), pos.y(), pos.z());
		double acc = 0;
		for (Variant v : variants) {
			acc += v.weight() / total;
			if (r < acc) {
				return v.material();
			}
		}
		return variants.get(variants.size() - 1).material();
	}

	/** Block id of the given shape for this role, falling back across variants and related families. */
	public String id(Role role, Vec3 pos, Shape shape) {
		String chosen = material(role, pos);
		var direct = Materials.resolve(chosen, shape);
		if (direct.isPresent()) {
			return direct.get();
		}
		for (Variant v : palette.variants(role)) {
			var alt = Materials.resolve(v.material(), shape);
			if (alt.isPresent()) {
				return alt.get();
			}
		}
		return defaultId(shape);
	}

	private static String defaultId(Shape shape) {
		return switch (shape) {
			case FULL -> "stone_bricks";
			case STAIRS -> "stone_brick_stairs";
			case SLAB -> "stone_brick_slab";
			case WALL -> "stone_brick_wall";
			case FENCE -> "oak_fence";
			case FENCE_GATE -> "oak_fence_gate";
			case TRAPDOOR -> "spruce_trapdoor";
			case DOOR -> "spruce_door";
			case PANE -> "glass_pane";
			case BUTTON -> "stone_button";
		};
	}

	public Block full(Role role, Vec3 pos) {
		String material = material(role, pos);
		Material m = Materials.find(material).orElse(null);
		Block block = Block.of(id(role, pos, Shape.FULL));
		if (m != null && m.axis()) {
			block = block.with("axis", "y");
		}
		return block;
	}

	/** Full block of a log-like material laid horizontally along {@code axis}; plain blocks are unaffected. */
	public Block fullAlong(Role role, Vec3 pos, Blocks.Axis axis) {
		Block block = full(role, pos);
		return block.hasProp("axis") ? block.with("axis", axis.name().toLowerCase()) : block;
	}

	public Block stairs(Role role, Vec3 pos, Dir facing, Half half) {
		return Blocks.stairs(id(role, pos, Shape.STAIRS), facing, half);
	}

	public Block slab(Role role, Vec3 pos, Half half) {
		return Blocks.slab(id(role, pos, Shape.SLAB), half);
	}

	public Block wall(Role role, Vec3 pos) {
		return Block.of(id(role, pos, Shape.WALL));
	}

	public Block fence(Role role, Vec3 pos) {
		return Block.of(id(role, pos, Shape.FENCE));
	}

	public Block trapdoor(Role role, Vec3 pos, Dir facing, Half half, boolean open) {
		return Blocks.trapdoor(id(role, pos, Shape.TRAPDOOR), facing, half, open);
	}

	public String doorId() {
		return id(Role.DOOR, new Vec3(0, 0, 0), Shape.DOOR);
	}

	public Block glassPane() {
		return Block.of(Materials.resolve(palette.glass(), Shape.PANE).orElse("glass_pane"));
	}

	public boolean supports(Role role, Shape shape) {
		return Materials.supports(primary(role), shape);
	}

	/** Uniform hash in [0, 1) of the seed and a lattice position (SplitMix64 finaliser). */
	public static double hash(long seed, int x, int y, int z) {
		long h = seed;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft(h, 27) ^ (y * 0xC2B2AE3D27D4EB4FL);
		h = Long.rotateLeft(h, 31) ^ (z * 0x165667B19E3779F9L);
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
		h = h ^ (h >>> 31);
		return (h >>> 11) * 0x1.0p-53;
	}
}
