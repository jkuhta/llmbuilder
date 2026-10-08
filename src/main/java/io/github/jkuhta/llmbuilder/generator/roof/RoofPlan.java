package io.github.jkuhta.llmbuilder.generator.roof;

import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The roof as a set of columns, each holding the blocks of the roof surface there. Built before
 * walls so facades know where the roof is, then rendered by {@link RoofBuilder}.
 */
public final class RoofPlan {
	public enum Kind {
		STAIR,
		SLAB_BOTTOM,
		SLAB_TOP,
		FULL
	}

	/** One roof block. {@code facing} is uphill (towards the ridge) for stairs, null otherwise. */
	public record Cell(int y, Kind kind, Dir facing) {
	}

	/**
	 * Roof blocks over one column. {@code distance} is the number of steps from the eave, and
	 * {@code ridge} marks the apex line.
	 */
	public record Column(int x, int z, Mass mass, List<Cell> cells, int distance, boolean ridge) {
		public int top() {
			return cells.stream().mapToInt(Cell::y).max().orElseThrow();
		}

		public int bottom() {
			return cells.stream().mapToInt(Cell::y).min().orElseThrow();
		}
	}

	private final Map<Long, Column> columns = new HashMap<>();
	private final Map<Mass, Integer> flatTops = new HashMap<>();

	private static long key(int x, int z) {
		return ((long) x << 32) | (z & 0xffffffffL);
	}

	/** Adds a column, keeping whichever surface is higher where two masses' roofs overlap. */
	public void merge(Column column) {
		columns.merge(key(column.x(), column.z()), column, (a, b) -> b.top() > a.top() ? b : a);
	}

	public Column column(int x, int z) {
		return columns.get(key(x, z));
	}

	public Collection<Column> columns() {
		return columns.values();
	}

	public void setFlatTop(Mass mass, int y) {
		flatTops.put(mass, y);
	}

	/** Highest roof block over the column, or Integer.MIN_VALUE if there is none. */
	public int topAt(int x, int z) {
		Column c = column(x, z);
		int top = c == null ? Integer.MIN_VALUE : c.top();
		for (Map.Entry<Mass, Integer> e : flatTops.entrySet()) {
			if (e.getKey().contains(x, z)) {
				top = Math.max(top, e.getValue());
			}
		}
		return top;
	}

	public int ridgeY() {
		int max = Integer.MIN_VALUE;
		for (Column c : columns.values()) {
			max = Math.max(max, c.top());
		}
		for (int y : flatTops.values()) {
			max = Math.max(max, y);
		}
		return max;
	}
}
