package io.github.jkuhta.llmbuilder.generator.core;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * A Minecraft block state expressed without depending on Minecraft classes, e.g.
 * {@code minecraft:oak_stairs[facing=north,half=bottom]}. Properties that are not set keep the
 * block's default value when placed.
 */
public record Block(String id, SortedMap<String, String> props) {
	public static final Block AIR = of("minecraft:air");

	public Block {
		if (id.indexOf(':') < 0) {
			id = "minecraft:" + id;
		}
		props = Collections.unmodifiableSortedMap(new TreeMap<>(props));
	}

	public static Block of(String id) {
		return new Block(id, new TreeMap<>());
	}

	/** Parses the {@link #toString()} format. */
	public static Block parse(String text) {
		int bracket = text.indexOf('[');
		if (bracket < 0) {
			return of(text.trim());
		}
		SortedMap<String, String> props = new TreeMap<>();
		String inner = text.substring(bracket + 1, text.lastIndexOf(']'));
		for (String pair : inner.split(",")) {
			if (pair.isBlank()) {
				continue;
			}
			String[] kv = pair.split("=", 2);
			props.put(kv[0].trim(), kv[1].trim());
		}
		return new Block(text.substring(0, bracket).trim(), props);
	}

	public Block with(String key, String value) {
		SortedMap<String, String> copy = new TreeMap<>(props);
		copy.put(key, value);
		return new Block(id, copy);
	}

	public Block with(String key, Dir dir) {
		return with(key, dir.id());
	}

	public Block with(String key, boolean value) {
		return with(key, Boolean.toString(value));
	}

	public Block with(Map<String, String> more) {
		SortedMap<String, String> copy = new TreeMap<>(props);
		copy.putAll(more);
		return new Block(id, copy);
	}

	public String prop(String key) {
		return props.get(key);
	}

	public boolean hasProp(String key) {
		return props.containsKey(key);
	}

	/** Id without namespace, e.g. {@code oak_stairs}. */
	public String path() {
		return id.substring(id.indexOf(':') + 1);
	}

	public boolean isAir() {
		return id.equals("minecraft:air") || id.equals("minecraft:cave_air");
	}

	@Override
	public String toString() {
		if (props.isEmpty()) {
			return id;
		}
		StringJoiner joiner = new StringJoiner(",", id + "[", "]");
		props.forEach((k, v) -> joiner.add(k + "=" + v));
		return joiner.toString();
	}
}
