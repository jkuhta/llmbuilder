package io.github.jkuhta.llmbuilder.generator.core;

import java.util.Locale;

/** Horizontal direction in generator space. North is -Z, east is +X, matching Minecraft. */
public enum Dir {
	NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

	public final int dx;
	public final int dz;

	Dir(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public Dir opposite() {
		return values()[(ordinal() + 2) % 4];
	}

	/** Rotated 90 degrees clockwise when viewed from above. */
	public Dir cw() {
		return values()[(ordinal() + 1) % 4];
	}

	public Dir ccw() {
		return values()[(ordinal() + 3) % 4];
	}

	public boolean isXAxis() {
		return dx != 0;
	}

	/** Lowercase name as used by Minecraft block state properties. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Dir fromId(String id) {
		return valueOf(id.toUpperCase(Locale.ROOT));
	}
}
