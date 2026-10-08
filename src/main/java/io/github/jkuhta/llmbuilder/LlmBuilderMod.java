package io.github.jkuhta.llmbuilder;

import io.github.jkuhta.llmbuilder.command.BuildCommand;
import io.github.jkuhta.llmbuilder.command.SpecFiles;
import io.github.jkuhta.llmbuilder.config.LlmBuilderConfig;
import io.github.jkuhta.llmbuilder.placement.PlacementManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LlmBuilderMod implements ModInitializer {
	public static final String MOD_ID = "llmbuilder";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Background thread for generation and network calls; never block the server thread. */
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "llmbuilder-worker");
		t.setDaemon(true);
		return t;
	});

	private LlmBuilderConfig config = new LlmBuilderConfig();

	@Override
	public void onInitialize() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		try {
			config = LlmBuilderConfig.load(configDir.resolve("llmbuilder.json"));
		} catch (IOException e) {
			LOGGER.error("Could not load config, using defaults: {}", e.getMessage());
		}
		Path specs = configDir.resolve("llmbuilder").resolve("specs");
		try {
			Files.createDirectories(specs);
		} catch (IOException e) {
			LOGGER.warn("Could not create {}: {}", specs, e.getMessage());
		}

		PlacementManager placements = new PlacementManager(() -> config.blocksPerTick);
		ServerTickEvents.END_SERVER_TICK.register(server -> placements.tick());
		BuildCommand command = new BuildCommand(() -> config, placements, new SpecFiles(specs), WORKER);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command.register(dispatcher));
		LOGGER.info("LLM Builder ready (specs in {})", specs);
	}
}
