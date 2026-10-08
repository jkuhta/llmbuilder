package io.github.jkuhta.llmbuilder.generator.check;

import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Blocks;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Feature;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The checker must actually catch the problems it claims to, not just pass on good buildings. */
class RealismCheckerTest {
	@Test
	void detectsFloatingBlock() {
		BlockBuffer b = new BlockBuffer();
		b.set(new Vec3(0, 0, 0), Block.of("stone"), Part.WALL);
		b.set(new Vec3(0, 1, 0), Block.of("stone"), Part.WALL);
		b.set(new Vec3(5, 4, 5), Block.of("stone"), Part.WALL);
		List<RealismChecker.Violation> v = new ArrayList<>();
		RealismChecker.floating(b, v);
		assertEquals(1, v.size());
		assertTrue(v.get(0).message().startsWith("1 blocks are not connected"));
	}

	@Test
	void detectsWindowWithoutSill() {
		BlockBuffer b = new BlockBuffer();
		b.addFeature(new Feature.Opening(Feature.Opening.Kind.WINDOW, "front", new Vec3(2, 3, 0), Dir.SOUTH, Dir.EAST, 1, 2, true));
		List<RealismChecker.Violation> v = new ArrayList<>();
		RealismChecker.sills(b, v);
		assertEquals(RealismChecker.Rule.WINDOW_HAS_SILL, v.get(0).rule());

		b.set(new Vec3(2, 2, 1), Blocks.stairs("stone_brick_stairs", Dir.NORTH, Half.TOP), Part.SILL);
		v.clear();
		RealismChecker.sills(b, v);
		assertTrue(v.isEmpty());
	}

	@Test
	void detectsRoofStairFacingDownhill() {
		BlockBuffer b = new BlockBuffer();
		// A 1:1 slope rising towards +x, but the stairs face west (downhill).
		for (int x = 0; x < 4; x++) {
			b.set(new Vec3(x, x, 0), Blocks.stairs("oak_stairs", Dir.WEST, Half.BOTTOM), Part.ROOF);
		}
		List<RealismChecker.Violation> v = new ArrayList<>();
		RealismChecker.roofSlopes(b, v);
		assertEquals(RealismChecker.Rule.ROOF_SLOPES_OUTWARD, v.get(0).rule());
	}

	@Test
	void detectsIncompleteBlockState() {
		BlockBuffer b = new BlockBuffer();
		b.set(new Vec3(0, 0, 0), Block.of("oak_stairs").with("facing", Dir.NORTH), Part.ROOF);
		List<RealismChecker.Violation> v = new ArrayList<>();
		RealismChecker.blockStates(b, v);
		assertEquals(RealismChecker.Rule.BLOCK_STATE_COMPLETE, v.get(0).rule());
	}
}
