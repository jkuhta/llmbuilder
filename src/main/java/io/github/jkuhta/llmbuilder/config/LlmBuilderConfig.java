package io.github.jkuhta.llmbuilder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.jkuhta.llmbuilder.spec.SpecValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server settings in {@code config/llmbuilder.json}. Created with defaults on first start.
 * Missing fields keep their defaults and out-of-range values are clamped.
 */
public final class LlmBuilderConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** Largest allowed footprint width or depth, in blocks. */
	public int maxBuildingSize = 48;
	/** Largest allowed total height including the roof, in blocks. */
	public int maxBuildingHeight = 64;
	/** Blocks placed per server tick across all running builds. */
	public int blocksPerTick = 1500;
	/** Command permission level required for /build (0 = everyone, 2 = operators). */
	public int permissionLevel = 2;

	public SpecValidator.Limits limits() {
		return new SpecValidator.Limits(maxBuildingSize, maxBuildingHeight);
	}

	private void clamp() {
		maxBuildingSize = Math.max(8, Math.min(64, maxBuildingSize));
		maxBuildingHeight = Math.max(8, Math.min(320, maxBuildingHeight));
		blocksPerTick = Math.max(50, Math.min(50_000, blocksPerTick));
		permissionLevel = Math.max(0, Math.min(4, permissionLevel));
	}

	/** Loads the config, writing a default file if none exists or adding newly introduced fields. */
	public static LlmBuilderConfig load(Path file) throws IOException {
		LlmBuilderConfig config = new LlmBuilderConfig();
		if (Files.exists(file)) {
			try {
				LlmBuilderConfig read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), LlmBuilderConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (JsonParseException e) {
				throw new IOException("Invalid " + file.getFileName() + ": " + e.getMessage(), e);
			}
		}
		config.clamp();
		Files.createDirectories(file.getParent());
		Files.writeString(file, GSON.toJson(config), StandardCharsets.UTF_8);
		return config;
	}
}
