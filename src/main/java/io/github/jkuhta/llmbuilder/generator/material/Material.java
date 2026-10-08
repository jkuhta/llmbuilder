package io.github.jkuhta.llmbuilder.generator.material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * A material family such as {@code stone_bricks} or {@code dark_oak}: one name the planner can use,
 * mapped to every block id that shares its texture. Shapes the family lacks are borrowed from
 * {@link #fallback()}, e.g. {@code cracked_stone_bricks} borrows stairs from {@code stone_bricks}.
 */
public record Material(String name, Kind kind, Map<Shape, String> shapes, String fallback, boolean axis) {
	public enum Kind {
		STONE,
		WOOD,
		LOG,
		GLASS,
		METAL,
		OTHER
	}

	public Material {
		shapes = Collections.unmodifiableMap(new EnumMap<>(shapes));
	}

	public Optional<String> id(Shape shape) {
		return Optional.ofNullable(shapes.get(shape));
	}

	public boolean has(Shape shape) {
		return shapes.containsKey(shape);
	}
}
