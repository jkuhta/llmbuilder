package io.github.jkuhta.llmbuilder.spec;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Spec enums serialise as lowercase snake_case, e.g. {@code stepped_gable}. */
public interface SpecEnum {
	String name();

	default String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	static <E extends Enum<E> & SpecEnum> Optional<E> parse(Class<E> type, String id) {
		return Arrays.stream(type.getEnumConstants()).filter(e -> e.id().equals(id)).findFirst();
	}

	static <E extends Enum<E> & SpecEnum> String ids(Class<E> type) {
		return String.join(", ", Arrays.stream(type.getEnumConstants()).map(SpecEnum::id).toList());
	}
}
