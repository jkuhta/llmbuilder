package io.github.jkuhta.llmbuilder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmBuilderConfigTest {
	@Test
	void writesDefaultsWhenMissing(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config/llmbuilder.json");
		LlmBuilderConfig config = LlmBuilderConfig.load(file);
		assertEquals(48, config.maxBuildingSize);
		assertTrue(Files.readString(file).contains("\"blocksPerTick\": 1500"));
	}

	@Test
	void keepsDefaultsForMissingFieldsAndClamps(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("llmbuilder.json");
		Files.writeString(file, "{ \"maxBuildingSize\": 500 }");
		LlmBuilderConfig config = LlmBuilderConfig.load(file);
		assertEquals(64, config.maxBuildingSize);
		assertEquals(1500, config.blocksPerTick);
	}
}
