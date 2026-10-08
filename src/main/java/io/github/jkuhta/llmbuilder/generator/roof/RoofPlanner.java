package io.github.jkuhta.llmbuilder.generator.roof;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlan.Cell;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlan.Kind;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof.Pitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans every pitched roof as a distance field: each column's distance from the nearest eave
 * edge sets its height and the edge's inward normal sets the uphill (stair facing) direction.
 * Gable roofs measure distance to the two eave edges only, hips and mansards to all four. Where
 * wings overlap the main roof the higher surface wins, which forms valleys naturally.
 */
public final class RoofPlanner implements Component {
	/** An eave edge: columns are measured from {@code coord} along {@code inward}. */
	private record Edge(Dir inward, int coord) {
		int distance(int x, int z) {
			return switch (inward) {
				case EAST -> x - coord;
				case WEST -> coord - x;
				case SOUTH -> z - coord;
				case NORTH -> coord - z;
			};
		}
	}

	@Override
	public void apply(BuildContext ctx) {
		RoofPlan plan = new RoofPlan();
		Roof roof = ctx.spec.roof();
		for (Mass mass : ctx.layout.masses()) {
			Roof.Type type = roof.type();
			if (type == Roof.Type.DOME) {
				ctx.warn("dome roofs are not supported yet; using a hip roof");
				type = Roof.Type.HIP;
			}
			if (type == Roof.Type.FLAT) {
				plan.setFlatTop(mass, mass.wallTop() + parapetHeight(roof));
				continue;
			}
			planMass(ctx, plan, mass, type, roof);
		}
		ctx.setRoofPlan(plan);
	}

	public static int parapetHeight(Roof roof) {
		return switch (roof.parapet()) {
			case NONE -> 0;
			case PLAIN -> 2;
			case CRENELLATED -> 2;
		};
	}

	/** True if the ridge of this mass runs along the x axis (eaves on its front and back). */
	public static boolean ridgeAlongX(BuildContext ctx, Mass mass) {
		Roof.Ridge ridge = ctx.spec.roof().ridge();
		if (!mass.isMain()) {
			// Wings run their ridge away from the main block unless told otherwise.
			return ridge == Roof.Ridge.AUTO ? FacadeFrame.outwardOf(mass.attachedTo()).isXAxis() : ridgeAlongXFor(ridge, mass);
		}
		return ridgeAlongXFor(ridge, mass);
	}

	private static boolean ridgeAlongXFor(Roof.Ridge ridge, Mass mass) {
		return switch (ridge) {
			case WIDTH -> true;
			case DEPTH -> false;
			case AUTO -> mass.width() >= mass.depth();
		};
	}

	private void planMass(BuildContext ctx, RoofPlan plan, Mass mass, Roof.Type type, Roof roof) {
		int o = roof.overhang();
		boolean gable = type == Roof.Type.GABLE || type == Roof.Type.STEPPED_GABLE;
		int gableOverhang = type == Roof.Type.STEPPED_GABLE ? 0 : o;
		boolean alongX = ridgeAlongX(ctx, mass);

		int rx0 = mass.x0() - (alongX && gable ? gableOverhang : o);
		int rx1 = mass.x1() + (alongX && gable ? gableOverhang : o);
		int rz0 = mass.z0() - (!alongX && gable ? gableOverhang : o);
		int rz1 = mass.z1() + (!alongX && gable ? gableOverhang : o);

		// Wings extend their roof into the main block up to its ridge so the two roofs meet in a valley.
		if (!mass.isMain()) {
			Mass main = ctx.layout.main();
			switch (mass.attachedTo()) {
				case FRONT -> rz0 = Math.min(rz0, main.z0() + main.depth() / 2);
				case BACK -> rz1 = Math.max(rz1, main.z1() - main.depth() / 2);
				case LEFT -> rx1 = Math.max(rx1, main.x1() - main.width() / 2);
				case RIGHT -> rx0 = Math.min(rx0, main.x0() + main.width() / 2);
			}
		}

		List<Edge> edges = new ArrayList<>();
		if (gable) {
			if (alongX) {
				edges.add(new Edge(Dir.SOUTH, rz0));
				edges.add(new Edge(Dir.NORTH, rz1));
			} else {
				edges.add(new Edge(Dir.EAST, rx0));
				edges.add(new Edge(Dir.WEST, rx1));
			}
		} else {
			edges.add(new Edge(Dir.SOUTH, rz0));
			edges.add(new Edge(Dir.NORTH, rz1));
			edges.add(new Edge(Dir.EAST, rx0));
			edges.add(new Edge(Dir.WEST, rx1));
		}

		int base = mass.roofBase();
		for (int x = rx0; x <= rx1; x++) {
			for (int z = rz0; z <= rz1; z++) {
				int d = Integer.MAX_VALUE;
				List<Edge> nearest = new ArrayList<>();
				for (Edge e : edges) {
					// A wing's edge inside the main block is not an eave; only its free edges slope.
					if (!mass.isMain() && isAttachedEdge(mass, e)) {
						continue;
					}
					int dist = e.distance(x, z);
					if (dist < d) {
						d = dist;
						nearest.clear();
						nearest.add(e);
					} else if (dist == d) {
						nearest.add(e);
					}
				}
				boolean ridge = nearest.size() > 1 && nearest.stream().anyMatch(a -> nearest.stream().anyMatch(b -> b.inward() == a.inward().opposite()));
				Dir facing = nearest.get(0).inward();
				List<Cell> cells = profile(type, roof.pitch(), base, d, ridge, facing);
				plan.merge(new RoofPlan.Column(x, z, mass, cells, d, ridge));
			}
		}
	}

	private static boolean isAttachedEdge(Mass wing, Edge e) {
		return e.inward() == FacadeFrame.outwardOf(wing.attachedTo());
	}

	/** Roof blocks for one column at distance d from the eave, from the given base y. */
	static List<Cell> profile(Roof.Type type, Pitch pitch, int base, int d, boolean ridge, Dir facing) {
		if (type == Roof.Type.MANSARD) {
			int steep = 2;
			if (d < steep) {
				return profile(Roof.Type.GABLE, Pitch.STEEP, base, d, false, facing);
			}
			return profile(Roof.Type.GABLE, Pitch.LOW, base + 2 * steep, d - steep, ridge, facing);
		}
		return switch (pitch) {
			case MEDIUM -> ridge
				? List.of(new Cell(base + d, Kind.SLAB_BOTTOM, null))
				: List.of(new Cell(base + d, Kind.STAIR, facing));
			case STEEP -> ridge
				? List.of(new Cell(base + 2 * d, Kind.SLAB_BOTTOM, null))
				: List.of(new Cell(base + 2 * d, Kind.STAIR, facing), new Cell(base + 2 * d + 1, Kind.STAIR, facing));
			case LOW -> d % 2 == 0
				? List.of(new Cell(base + d / 2, Kind.SLAB_BOTTOM, null))
				: List.of(new Cell(base + (d - 1) / 2, Kind.SLAB_TOP, null));
		};
	}
}
