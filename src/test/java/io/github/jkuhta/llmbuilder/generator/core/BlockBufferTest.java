package io.github.jkuhta.llmbuilder.generator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBufferTest {
	@Test
	void storesNegativeAndPositiveCoordinates() {
		BlockBuffer buffer = new BlockBuffer();
		buffer.set(new Vec3(-5, 0, 7), Block.of("stone"), Part.WALL);
		buffer.set(new Vec3(3, 40, -2), Block.of("bricks"), Part.WALL);
		assertEquals("minecraft:stone", buffer.get(-5, 0, 7).id());
		assertEquals("minecraft:bricks", buffer.get(3, 40, -2).id());
		assertNull(buffer.get(0, 0, 0));
		BlockBuffer.Box box = buffer.bounds();
		assertEquals(new BlockBuffer.Box(-5, 0, -2, 3, 40, 7), box);
	}

	@Test
	void setIfFreeOverwritesOnlyAir() {
		BlockBuffer buffer = new BlockBuffer();
		Vec3 p = new Vec3(1, 1, 1);
		buffer.set(p, Block.AIR, Part.CLEAR);
		assertTrue(buffer.setIfFree(p, Block.of("stone"), Part.WALL));
		assertFalse(buffer.setIfFree(p, Block.of("dirt"), Part.WALL));
		assertEquals("minecraft:stone", buffer.get(p).id());
	}

	@Test
	void blockRoundTripsThroughString() {
		Block stairs = Block.of("oak_stairs").with("facing", Dir.NORTH).with("half", "top");
		assertEquals("minecraft:oak_stairs[facing=north,half=top]", stairs.toString());
		assertEquals(stairs, Block.parse(stairs.toString()));
	}

	@Test
	void directionsRotate() {
		assertEquals(Dir.EAST, Dir.NORTH.cw());
		assertEquals(Dir.WEST, Dir.NORTH.ccw());
		assertEquals(Dir.SOUTH, Dir.NORTH.opposite());
	}
}
