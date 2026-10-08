package io.github.jkuhta.llmbuilder.generator.check;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.BlockShapes;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Feature;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The generator's realism rules as executable checks. Run after every generation (violations
 * become warnings) and asserted to be empty in tests for every example building.
 */
public final class RealismChecker {
	public enum Rule {
		NO_FLOATING_BLOCKS,
		WINDOW_HAS_SILL,
		ROOF_SLOPES_OUTWARD,
		FACADE_DEPTH,
		BLOCK_STATE_COMPLETE,
		PALETTE_VARIATION
	}

	public record Violation(Rule rule, String message) {
		@Override
		public String toString() {
			return rule + ": " + message;
		}
	}

	private static final Map<String, List<String>> REQUIRED_PROPS = Map.of(
		"_stairs", List.of("facing", "half", "shape"),
		"_slab", List.of("type"),
		"_trapdoor", List.of("facing", "half", "open"),
		"_door", List.of("facing", "half", "hinge", "open"),
		"_pane", List.of("north", "east", "south", "west"),
		"_fence", List.of("north", "east", "south", "west"),
		"_wall", List.of("north", "east", "south", "west", "up"),
		"_log", List.of("axis")
	);

	private RealismChecker() {
	}

	public static List<Violation> check(BuildContext ctx) {
		List<Violation> v = new ArrayList<>();
		floating(ctx.buffer, v);
		sills(ctx.buffer, v);
		roofSlopes(ctx.buffer, v);
		facadeDepth(ctx, v);
		blockStates(ctx.buffer, v);
		variation(ctx, v);
		return v;
	}

	/** Every block must connect to the ground (y <= 0) through face-adjacent blocks. */
	static void floating(BlockBuffer buffer, List<Violation> out) {
		Set<Vec3> solid = new HashSet<>();
		Deque<Vec3> queue = new ArrayDeque<>();
		for (BlockBuffer.Entry e : buffer.entries()) {
			if (e.block().isAir()) {
				continue;
			}
			solid.add(e.pos());
			if (e.pos().y() <= 0) {
				queue.add(e.pos());
			}
		}
		Set<Vec3> reached = new HashSet<>(queue);
		while (!queue.isEmpty()) {
			Vec3 p = queue.poll();
			for (Vec3 n : new Vec3[] {p.above(), p.below(), p.offset(Dir.NORTH), p.offset(Dir.SOUTH), p.offset(Dir.EAST), p.offset(Dir.WEST)}) {
				if (solid.contains(n) && reached.add(n)) {
					queue.add(n);
				}
			}
		}
		int floating = solid.size() - reached.size();
		if (floating > 0) {
			Vec3 example = solid.stream().filter(p -> !reached.contains(p)).findFirst().orElseThrow();
			out.add(new Violation(Rule.NO_FLOATING_BLOCKS, floating + " blocks are not connected to the ground, e.g. "
				+ buffer.get(example) + " at " + example));
		}
	}

	/** Each window with a sill has a stair or slab projecting under every column of its opening. */
	static void sills(BlockBuffer buffer, List<Violation> out) {
		for (Feature.Opening o : buffer.features(Feature.Opening.class)) {
			if (o.kind() != Feature.Opening.Kind.WINDOW || !o.sill()) {
				continue;
			}
			for (int u = 0; u < o.width(); u++) {
				Vec3 sill = o.cell(u, -1).offset(o.outward());
				Block b = buffer.get(sill);
				if (b == null || !(b.path().endsWith("_stairs") || b.path().endsWith("_slab"))) {
					out.add(new Violation(Rule.WINDOW_HAS_SILL, "window on " + o.facade() + " at " + o.origin() + " has no sill at " + sill));
					break;
				}
			}
		}
	}

	/**
	 * Roof stairs climb towards the ridge: the column downhill of a stair (opposite its facing)
	 * must not carry roof higher than the stair itself.
	 */
	static void roofSlopes(BlockBuffer buffer, List<Violation> out) {
		Map<Long, Integer> roofTop = new HashMap<>();
		List<BlockBuffer.Entry> stairs = new ArrayList<>();
		for (BlockBuffer.Entry e : buffer.entries()) {
			if (e.part() != Part.ROOF && e.part() != Part.RIDGE) {
				continue;
			}
			roofTop.merge(column(e.pos()), e.pos().y(), Math::max);
			if (BlockShapes.isStairs(e.block()) && "bottom".equals(e.block().prop("half"))) {
				stairs.add(e);
			}
		}
		int wrong = 0;
		Vec3 example = null;
		for (BlockBuffer.Entry e : stairs) {
			Dir downhill = Dir.fromId(e.block().prop("facing")).opposite();
			Integer top = roofTop.get(column(e.pos().offset(downhill)));
			if (top != null && top > e.pos().y()) {
				wrong++;
				example = e.pos();
			}
		}
		if (wrong > 0) {
			out.add(new Violation(Rule.ROOF_SLOPES_OUTWARD, wrong + " roof stairs face downhill, e.g. at " + example));
		}
	}

	private static long column(Vec3 p) {
		return ((long) p.x() << 32) | (p.z() & 0xffffffffL);
	}

	/** Facades are never flat: each must use at least two depth layers with real coverage. */
	static void facadeDepth(BuildContext ctx, List<Violation> out) {
		for (FacadeFrame f : ctx.layout.facades()) {
			Map<Integer, Integer> perLayer = new HashMap<>();
			int exposedCells = 0;
			for (int u = 0; u < f.length(); u++) {
				for (int y = 0; y <= f.mass().wallTop(); y++) {
					if (!ctx.layout.exposed(f, u, y)) {
						continue;
					}
					exposedCells++;
					for (int w = -1; w <= 2; w++) {
						if (ctx.buffer.isSolidAt(f.at(u, y, w)) && !(w == -1 && ctx.buffer.part(f.at(u, y, w)) == Part.FLOOR)) {
							perLayer.merge(w, 1, Integer::sum);
						}
					}
				}
			}
			if (exposedCells < 10) {
				continue;
			}
			int threshold = Math.max(3, exposedCells / 50);
			long layers = perLayer.values().stream().filter(c -> c >= threshold).count();
			if (layers < 2) {
				out.add(new Violation(Rule.FACADE_DEPTH, "facade " + f.name() + " is flat (" + perLayer + ")"));
			}
		}
	}

	/** Directional and connecting blocks must carry every property that defines their shape. */
	static void blockStates(BlockBuffer buffer, List<Violation> out) {
		for (BlockBuffer.Entry e : buffer.entries()) {
			String path = e.block().path();
			for (Map.Entry<String, List<String>> req : REQUIRED_PROPS.entrySet()) {
				if (!path.endsWith(req.getKey()) || (req.getKey().equals("_wall") && !BlockShapes.isWall(e.block()))) {
					continue;
				}
				for (String prop : req.getValue()) {
					if (!e.block().hasProp(prop)) {
						out.add(new Violation(Rule.BLOCK_STATE_COMPLETE, e.block() + " at " + e.pos() + " lacks " + prop));
						return;
					}
				}
			}
		}
	}

	/** Large wall surfaces must actually show the palette's variants. */
	static void variation(BuildContext ctx, List<Violation> out) {
		if (ctx.spec.palette().variants(Role.WALL).size() < 2) {
			return;
		}
		Set<String> used = new HashSet<>();
		int walls = 0;
		for (BlockBuffer.Entry e : ctx.buffer.entries()) {
			if (e.part() == Part.WALL) {
				used.add(e.block().id());
				walls++;
			}
		}
		if (walls >= 50 && used.size() < 2) {
			out.add(new Violation(Rule.PALETTE_VARIATION, walls + " wall blocks use a single material " + used));
		}
	}
}
