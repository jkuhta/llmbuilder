package io.github.jkuhta.llmbuilder.generator.structure;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Mass;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Roof;

/**
 * Horizontal articulation: a trim string course at every upper floor line (projecting as a slab
 * ledge at medium detail and above) and a corbelled cornice of upside-down stairs under the eaves.
 */
public final class Bands implements Component {
	@Override
	public void apply(BuildContext ctx) {
		boolean bands = ctx.spec.walls().bands();
		boolean cornice = ctx.spec.roof().cornice();
		for (FacadeFrame f : ctx.layout.facades()) {
			// Blank facades are party walls: plain masonry without courses or cornice.
			if (ctx.facadeSpec(f).blank()) {
				continue;
			}
			Mass m = f.mass();
			if (bands) {
				for (int floor = 1; floor < m.floors(); floor++) {
					course(ctx, f, m.floorBases().get(floor));
				}
			}
			for (int u = 1; u < f.length() - 1; u++) {
				int y = m.wallTop();
				if (!ctx.layout.exposed(f, u, y)) {
					continue;
				}
				Vec3 wall = f.at(u, y, 0);
				if (bands || cornice) {
					ctx.set(wall, ctx.palette.full(Role.TRIM, wall), Part.CORNICE);
				}
				Vec3 out = f.at(u, y, 1);
				boolean flatOverhang = ctx.spec.roof().type() == Roof.Type.FLAT && ctx.spec.roof().overhang() > 0;
				if (cornice && !flatOverhang) {
					ctx.setIfFree(out, ctx.palette.stairs(Role.TRIM, out, f.inward(), Half.TOP), Part.CORNICE);
				}
			}
		}
	}

	private static void course(BuildContext ctx, FacadeFrame f, int y) {
		for (int u = 1; u < f.length() - 1; u++) {
			if (!ctx.layout.exposed(f, u, y)) {
				continue;
			}
			Vec3 wall = f.at(u, y, 0);
			ctx.set(wall, ctx.palette.full(Role.TRIM, wall), Part.BAND);
			if (ctx.detail(Level.MEDIUM)) {
				Vec3 out = f.at(u, y, 1);
				ctx.setIfFree(out, ctx.palette.slab(Role.TRIM, out, Half.TOP), Part.BAND);
			}
		}
	}
}
