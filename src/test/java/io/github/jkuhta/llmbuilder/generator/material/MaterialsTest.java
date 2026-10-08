package io.github.jkuhta.llmbuilder.generator.material;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialsTest {
	@Test
	void resolvesShapesDirectly() {
		assertEquals("stone_brick_stairs", Materials.resolve("stone_bricks", Shape.STAIRS).orElseThrow());
		assertEquals("brick_wall", Materials.resolve("bricks", Shape.WALL).orElseThrow());
		assertEquals("dark_oak_trapdoor", Materials.resolve("dark_oak", Shape.TRAPDOOR).orElseThrow());
	}

	@Test
	void followsFallbackChainForMissingShapes() {
		assertEquals("stone_brick_stairs", Materials.resolve("cracked_stone_bricks", Shape.STAIRS).orElseThrow());
		assertEquals("spruce_stairs", Materials.resolve("stripped_spruce_log", Shape.STAIRS).orElseThrow());
		assertEquals("cobblestone_wall", Materials.resolve("stone", Shape.WALL).orElseThrow());
	}

	@Test
	void reportsUnsupportedShapes() {
		assertFalse(Materials.supports("glass", Shape.STAIRS));
		assertTrue(Materials.supports("glass", Shape.PANE));
		assertFalse(Materials.find("unobtainium").isPresent());
	}
}
