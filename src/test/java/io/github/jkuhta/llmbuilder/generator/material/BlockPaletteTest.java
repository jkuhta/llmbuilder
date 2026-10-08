package io.github.jkuhta.llmbuilder.generator.material;

import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Variant;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPaletteTest {
	private static BlockPalette palette(List<Variant> wall) {
		return new BlockPalette(new Palette(Map.of(Role.WALL, wall), "glass"), 42);
	}

	@Test
	void followsWeightsRoughly() {
		BlockPalette p = palette(List.of(new Variant("bricks", 7), new Variant("mud_bricks", 2), new Variant("granite", 1)));
		Map<String, Integer> counts = new HashMap<>();
		for (int x = 0; x < 40; x++) {
			for (int y = 0; y < 40; y++) {
				counts.merge(p.material(Role.WALL, new Vec3(x, y, 0)), 1, Integer::sum);
			}
		}
		double bricks = counts.get("bricks") / 1600.0;
		double granite = counts.get("granite") / 1600.0;
		assertTrue(bricks > 0.6 && bricks < 0.8, "bricks share " + bricks);
		assertTrue(granite > 0.05 && granite < 0.16, "granite share " + granite);
	}

	@Test
	void isDeterministic() {
		BlockPalette a = palette(List.of(new Variant("bricks", 1), new Variant("granite", 1)));
		BlockPalette b = palette(List.of(new Variant("bricks", 1), new Variant("granite", 1)));
		for (int i = 0; i < 100; i++) {
			Vec3 pos = new Vec3(i, i * 3, -i);
			assertEquals(a.material(Role.WALL, pos), b.material(Role.WALL, pos));
		}
	}

	@Test
	void fallsBackWhenVariantLacksShape() {
		BlockPalette p = palette(List.of(new Variant("cracked_stone_bricks", 1)));
		assertEquals("stone_brick_stairs", p.id(Role.WALL, new Vec3(0, 0, 0), Shape.STAIRS));
	}
}
