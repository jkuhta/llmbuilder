package io.github.jkuhta.llmbuilder.generator.layout;

import io.github.jkuhta.llmbuilder.spec.Side;

import java.util.List;

/**
 * One rectangular volume of the building: the main block or a wing. Rect bounds are inclusive
 * and include the walls. {@code floorBases.get(f)} is the y of floor f's floor row;
 * {@link #wallTop()} is the y of the topmost wall row (the attic floor and cornice row).
 */
public record Mass(int index, int x0, int z0, int x1, int z1, List<Integer> floorBases, int wallTop, Side attachedTo) {
	public boolean isMain() {
		return attachedTo == null;
	}

	public int width() {
		return x1 - x0 + 1;
	}

	public int depth() {
		return z1 - z0 + 1;
	}

	public int floors() {
		return floorBases.size();
	}

	/** First y of the roof, directly above the wall top. */
	public int roofBase() {
		return wallTop + 1;
	}

	public boolean contains(int x, int z) {
		return x >= x0 && x <= x1 && z >= z0 && z <= z1;
	}

	public boolean isInterior(int x, int z) {
		return x > x0 && x < x1 && z > z0 && z < z1;
	}

	public boolean isWallLine(int x, int z) {
		return contains(x, z) && !isInterior(x, z);
	}
}
