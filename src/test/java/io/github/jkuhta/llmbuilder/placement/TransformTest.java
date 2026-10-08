package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformTest {
	private static final BlockPos ANCHOR = new BlockPos(100, 64, -40);
	private static final Vec3 LOCAL_ANCHOR = new Vec3(4, 0, 9);

	@ParameterizedTest
	@EnumSource(value = Direction.class, names = {"NORTH", "EAST", "SOUTH", "WEST"})
	void frontFacesRequestedDirection(Direction front) {
		Transform t = Transform.anchored(LOCAL_ANCHOR, ANCHOR, front);
		// Local south is the building's front; block state rotation must agree with position rotation.
		assertEquals(front, t.rotation().rotate(Direction.SOUTH));
		assertEquals(ANCHOR, t.toWorld(LOCAL_ANCHOR));
		// One block outward from the front lands in front of the building, towards the viewer.
		assertEquals(ANCHOR.relative(front), t.toWorld(new Vec3(4, 0, 10)));
		// Depth runs away from the viewer.
		assertEquals(ANCHOR.relative(front.getOpposite(), 5), t.toWorld(new Vec3(4, 0, 4)));
	}

	@ParameterizedTest
	@EnumSource(value = Direction.class, names = {"NORTH", "EAST", "SOUTH", "WEST"})
	void rotatingKeepsAnchorAndTurnsClockwise(Direction front) {
		Transform t = Transform.anchored(LOCAL_ANCHOR, ANCHOR, front);
		Transform r = t.rotatedClockwise(LOCAL_ANCHOR);
		assertEquals(ANCHOR, r.toWorld(LOCAL_ANCHOR));
		assertEquals(front.getClockWise(), r.rotation().rotate(Direction.SOUTH));
	}
}
