package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;

import java.util.ArrayList;
import java.util.List;

/** Floor plates at every storey plus the attic floor (or roof deck for flat roofs). */
public final class Floors implements Component {
	@Override
	public void apply(BuildContext ctx) {
		boolean flat = ctx.spec.roof().type() == Roof.Type.FLAT;
		for (Mass m : ctx.layout.masses()) {
			List<Integer> levels = new ArrayList<>(m.floorBases());
			levels.add(m.wallTop());
			for (int y : levels) {
				Role role = flat && y == m.wallTop() ? Role.ROOF : Role.FLOOR;
				for (int x = m.x0() + 1; x < m.x1(); x++) {
					for (int z = m.z0() + 1; z < m.z1(); z++) {
						if (!m.isInterior(x, z)) {
							continue;
						}
						Vec3 p = new Vec3(x, y, z);
						ctx.set(p, ctx.palette.full(role, p), Part.FLOOR);
					}
				}
			}
		}
	}
}
