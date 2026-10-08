package io.github.jkuhta.llmbuilder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LlmBuilderMod implements ModInitializer {
	public static final String MOD_ID = "llmbuilder";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("LLM Builder initialised");
	}
}
