package io.github.jkuhta.llmbuilder.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecFilesTest {
	@Test
	void prefersUserFilesThenExamples(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("my_house.json"), "{\"name\":\"mine\"}");
		SpecFiles files = new SpecFiles(dir);
		assertEquals("{\"name\":\"mine\"}", files.read("my_house").orElseThrow());
		assertEquals("{\"name\":\"mine\"}", files.read("my_house.json").orElseThrow());
		assertTrue(files.read("canal_house").orElseThrow().contains("Herengracht"));
		assertEquals("my_house", files.names().get(0));
		assertTrue(files.names().contains("gothic_chapel"));
	}

	@Test
	void rejectsPathsOutsideTheSpecsDirectory(@TempDir Path dir) throws Exception {
		Path specs = Files.createDirectories(dir.resolve("specs"));
		Files.writeString(dir.resolve("secret.json"), "{}");
		SpecFiles files = new SpecFiles(specs);
		assertTrue(files.read("../secret").isEmpty());
		assertTrue(files.read("..").isEmpty());
		assertTrue(files.read("/etc/passwd").isEmpty());
	}
}
