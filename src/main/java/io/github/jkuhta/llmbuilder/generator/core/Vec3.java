package io.github.jkuhta.llmbuilder.generator.core;

/** Immutable integer position in building-local space. */
public record Vec3(int x, int y, int z) {
	public Vec3 offset(Dir dir, int n) {
		return new Vec3(x + dir.dx * n, y, z + dir.dz * n);
	}

	public Vec3 offset(Dir dir) {
		return offset(dir, 1);
	}

	public Vec3 above(int n) {
		return new Vec3(x, y + n, z);
	}

	public Vec3 above() {
		return above(1);
	}

	public Vec3 below() {
		return above(-1);
	}

	public Vec3 add(int dx, int dy, int dz) {
		return new Vec3(x + dx, y + dy, z + dz);
	}
}
