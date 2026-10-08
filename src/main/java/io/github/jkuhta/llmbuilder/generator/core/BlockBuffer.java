package io.github.jkuhta.llmbuilder.generator.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Sparse in-memory 3D map of blocks in building-local coordinates. Air entries are meaningful:
 * they tell placement to clear whatever terrain is there (e.g. inside the building).
 */
public final class BlockBuffer {
	public record Cell(Block block, Part part) {
	}

	public record Entry(Vec3 pos, Block block, Part part) {
	}

	public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		public int sizeX() {
			return maxX - minX + 1;
		}

		public int sizeY() {
			return maxY - minY + 1;
		}

		public int sizeZ() {
			return maxZ - minZ + 1;
		}

		public boolean contains(Vec3 p) {
			return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY && p.z() >= minZ && p.z() <= maxZ;
		}
	}

	private static final int BITS = 21;
	private static final long MASK = (1L << BITS) - 1;
	private static final int OFFSET = 1 << (BITS - 1);

	private final Map<Long, Cell> cells = new HashMap<>();
	private final List<Feature> features = new ArrayList<>();

	static long key(int x, int y, int z) {
		return ((long) (x + OFFSET) & MASK) << (2 * BITS) | ((long) (y + OFFSET) & MASK) << BITS | ((long) (z + OFFSET) & MASK);
	}

	static Vec3 unkey(long key) {
		int x = (int) ((key >>> (2 * BITS)) & MASK) - OFFSET;
		int y = (int) ((key >>> BITS) & MASK) - OFFSET;
		int z = (int) (key & MASK) - OFFSET;
		return new Vec3(x, y, z);
	}

	public void set(int x, int y, int z, Block block, Part part) {
		cells.put(key(x, y, z), new Cell(block, part));
	}

	public void set(Vec3 p, Block block, Part part) {
		set(p.x(), p.y(), p.z(), block, part);
	}

	/** Sets the block only if the position is empty or explicitly cleared to air. */
	public boolean setIfFree(Vec3 p, Block block, Part part) {
		Cell existing = cells.get(key(p.x(), p.y(), p.z()));
		if (existing != null && !existing.block().isAir()) {
			return false;
		}
		set(p, block, part);
		return true;
	}

	public void remove(Vec3 p) {
		cells.remove(key(p.x(), p.y(), p.z()));
	}

	/** Returns the block at the position, or {@code null} if nothing was written there. */
	public Block get(int x, int y, int z) {
		Cell cell = cells.get(key(x, y, z));
		return cell == null ? null : cell.block();
	}

	public Block get(Vec3 p) {
		return get(p.x(), p.y(), p.z());
	}

	public Part part(Vec3 p) {
		Cell cell = cells.get(key(p.x(), p.y(), p.z()));
		return cell == null ? null : cell.part();
	}

	/** True if a non-air block was written at the position. */
	public boolean isSolidAt(Vec3 p) {
		Block block = get(p);
		return block != null && !block.isAir();
	}

	public int size() {
		return cells.size();
	}

	public List<Entry> entries() {
		List<Entry> result = new ArrayList<>(cells.size());
		cells.forEach((k, cell) -> result.add(new Entry(unkey(k), cell.block(), cell.part())));
		result.sort((a, b) -> {
			int c = Integer.compare(a.pos().y(), b.pos().y());
			if (c != 0) {
				return c;
			}
			c = Integer.compare(a.pos().x(), b.pos().x());
			return c != 0 ? c : Integer.compare(a.pos().z(), b.pos().z());
		});
		return result;
	}

	public List<Entry> entries(Predicate<Entry> filter) {
		return entries().stream().filter(filter).toList();
	}

	/** Bounding box of all non-air blocks, or {@code null} if there are none. */
	public Box bounds() {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		boolean any = false;
		for (Map.Entry<Long, Cell> e : cells.entrySet()) {
			if (e.getValue().block().isAir()) {
				continue;
			}
			Vec3 p = unkey(e.getKey());
			any = true;
			minX = Math.min(minX, p.x());
			minY = Math.min(minY, p.y());
			minZ = Math.min(minZ, p.z());
			maxX = Math.max(maxX, p.x());
			maxY = Math.max(maxY, p.y());
			maxZ = Math.max(maxZ, p.z());
		}
		return any ? new Box(minX, minY, minZ, maxX, maxY, maxZ) : null;
	}

	public void addFeature(Feature feature) {
		features.add(feature);
	}

	public List<Feature> features() {
		return Collections.unmodifiableList(features);
	}

	public <T extends Feature> List<T> features(Class<T> type) {
		return features.stream().filter(type::isInstance).map(type::cast).toList();
	}
}
