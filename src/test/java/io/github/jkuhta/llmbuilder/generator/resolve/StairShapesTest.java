package io.github.jkuhta.llmbuilder.generator.resolve;

import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Blocks;
import io.github.jkuhta.llmbuilder.generator.core.Blocks.Half;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StairShapesTest {
	private static void stair(BlockBuffer b, int x, int z, Dir facing) {
		b.set(new Vec3(x, 0, z), Blocks.stairs("oak_stairs", facing, Half.BOTTOM), Part.ROOF);
	}

	@Test
	void hipCornerBecomesOuterCorner() {
		// North-west corner of a hip: north row faces south, west row faces east.
		BlockBuffer b = new BlockBuffer();
		stair(b, 0, 0, Dir.EAST);
		stair(b, 1, 0, Dir.SOUTH);
		stair(b, 2, 0, Dir.SOUTH);
		stair(b, 0, 1, Dir.EAST);
		stair(b, 0, 2, Dir.EAST);
		StairShapes.resolve(b);
		// Vanilla: facing east, front neighbour faces south (== east.cw), so outer_right.
		assertEquals("outer_right", b.get(0, 0, 0).prop("shape"));
		assertEquals("straight", b.get(1, 0, 0).prop("shape"));
		assertEquals("straight", b.get(0, 0, 1).prop("shape"));
	}

	@Test
	void valleyBecomesInnerCorner() {
		BlockBuffer b = new BlockBuffer();
		stair(b, 1, 1, Dir.NORTH);
		stair(b, 1, 2, Dir.EAST);
		StairShapes.resolve(b);
		// Facing north, back neighbour faces east (== north.cw), so inner_right.
		assertEquals("inner_right", b.get(1, 0, 1).prop("shape"));
	}

	@Test
	void differentHalvesDoNotConnect() {
		BlockBuffer b = new BlockBuffer();
		stair(b, 0, 0, Dir.EAST);
		b.set(new Vec3(1, 0, 0), Blocks.stairs("oak_stairs", Dir.SOUTH, Half.TOP), Part.ROOF);
		StairShapes.resolve(b);
		assertEquals("straight", b.get(0, 0, 0).prop("shape"));
	}
}
