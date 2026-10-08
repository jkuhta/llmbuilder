package io.github.jkuhta.llmbuilder.generator.core;

/** Factories for directional block states, so components never hand-type property names. */
public final class Blocks {
	private Blocks() {
	}

	public enum Half {
		BOTTOM,
		TOP;

		public String id() {
			return name().toLowerCase();
		}
	}

	/** {@code facing} is the side with the tall back of the stair, i.e. the direction it climbs. */
	public static Block stairs(String id, Dir facing, Half half) {
		return Block.of(id).with("facing", facing).with("half", half.id()).with("shape", "straight");
	}

	public static Block slab(String id, Half half) {
		return Block.of(id).with("type", half.id());
	}

	public static Block doubleSlab(String id) {
		return Block.of(id).with("type", "double");
	}

	/**
	 * An open trapdoor's panel lies on the side opposite {@code facing}; a closed one lies flat on
	 * the {@code half} side.
	 */
	public static Block trapdoor(String id, Dir facing, Half half, boolean open) {
		return Block.of(id).with("facing", facing).with("half", half.id()).with("open", open);
	}

	/**
	 * Lower or upper half of a closed door. A closed door's panel lies on the side opposite
	 * {@code facing}; facing is the direction a player looks when walking in.
	 */
	public static Block door(String id, Dir facing, boolean upper, boolean rightHinge) {
		return Block.of(id)
			.with("facing", facing)
			.with("half", upper ? "upper" : "lower")
			.with("hinge", rightHinge ? "right" : "left")
			.with("open", false);
	}

	public static Block log(String id, Axis axis) {
		return Block.of(id).with("axis", axis.name().toLowerCase());
	}

	public static Block lantern(String id, boolean hanging) {
		return Block.of(id).with("hanging", hanging);
	}

	public enum Axis {
		X,
		Y,
		Z;

		public static Axis horizontal(Dir dir) {
			return dir.isXAxis() ? X : Z;
		}
	}
}
