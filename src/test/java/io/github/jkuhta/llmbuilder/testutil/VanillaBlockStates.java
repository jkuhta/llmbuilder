package io.github.jkuhta.llmbuilder.testutil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jkuhta.llmbuilder.generator.core.Block;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads vanilla blockstate JSON files from the client jar to learn which ids exist and which
 * property values each block accepts. Lets tests reject typos like {@code stone_bricks_stairs}.
 */
public final class VanillaBlockStates {
	/** Properties that exist on blocks but never appear in blockstate files. */
	private static final Set<String> LOGIC_PROPS = Set.of("waterlogged", "powered", "persistent", "distance", "lit", "signal_fire");

	private static final Map<String, Optional<Map<String, Set<String>>>> CACHE = new HashMap<>();

	private VanillaBlockStates() {
	}

	public static boolean exists(String path) {
		return props(path).isPresent();
	}

	/** Returns a reason if the block state is invalid, otherwise empty. */
	public static Optional<String> problem(Block block) {
		Optional<Map<String, Set<String>>> known = props(block.path());
		if (known.isEmpty()) {
			return Optional.of("unknown block id " + block.id());
		}
		for (Map.Entry<String, String> prop : block.props().entrySet()) {
			Set<String> values = known.get().get(prop.getKey());
			if (values == null) {
				if (!LOGIC_PROPS.contains(prop.getKey())) {
					return Optional.of(block + ": unknown property " + prop.getKey());
				}
			} else if (!values.contains(prop.getValue())) {
				return Optional.of(block + ": invalid value " + prop.getValue() + " for " + prop.getKey() + ", expected " + values);
			}
		}
		return Optional.empty();
	}

	private static synchronized Optional<Map<String, Set<String>>> props(String path) {
		return CACHE.computeIfAbsent(path, VanillaBlockStates::load);
	}

	private static Optional<Map<String, Set<String>>> load(String path) {
		String resource = "/assets/minecraft/blockstates/" + path + ".json";
		try (InputStream in = VanillaBlockStates.class.getResourceAsStream(resource)) {
			if (in == null) {
				return Optional.empty();
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, Set<String>> result = new HashMap<>();
			if (root.has("variants")) {
				for (String key : root.getAsJsonObject("variants").keySet()) {
					for (String pair : key.split(",")) {
						if (pair.isEmpty()) {
							continue;
						}
						String[] kv = pair.split("=");
						result.computeIfAbsent(kv[0], k -> new HashSet<>()).add(kv[1]);
					}
				}
			}
			if (root.has("multipart")) {
				for (JsonElement part : root.getAsJsonArray("multipart")) {
					JsonObject obj = part.getAsJsonObject();
					if (obj.has("when")) {
						collectWhen(obj.get("when"), result);
					}
				}
				// Multipart files only mention values that change the model; defaults are implied.
				result.forEach((k, v) -> {
					if (v.contains("true")) {
						v.add("false");
					}
					if (v.contains("low") || v.contains("tall")) {
						v.add("none");
					}
				});
			}
			return Optional.of(result);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void collectWhen(JsonElement when, Map<String, Set<String>> result) {
		if (when.isJsonArray()) {
			for (JsonElement e : (JsonArray) when) {
				collectWhen(e, result);
			}
			return;
		}
		JsonObject obj = when.getAsJsonObject();
		for (String key : obj.keySet()) {
			if (key.equals("OR") || key.equals("AND")) {
				collectWhen(obj.get(key), result);
				continue;
			}
			for (String value : obj.get(key).getAsString().split("\\|")) {
				result.computeIfAbsent(key, k -> new HashSet<>()).add(value);
			}
		}
	}
}
