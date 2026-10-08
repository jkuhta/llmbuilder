package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.core.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Converts generator {@link Block} values into real block states, caching the result. */
public final class BlockMapper {
	private static final Map<Block, Optional<BlockState>> CACHE = new ConcurrentHashMap<>();

	private BlockMapper() {
	}

	/** The block state, or empty if the id is unknown. Unknown or invalid properties are skipped. */
	public static Optional<BlockState> toState(Block block) {
		return CACHE.computeIfAbsent(block, BlockMapper::convert);
	}

	private static Optional<BlockState> convert(Block block) {
		Identifier id = Identifier.tryParse(block.id());
		if (id == null) {
			return Optional.empty();
		}
		Optional<net.minecraft.world.level.block.Block> mc = BuiltInRegistries.BLOCK.getOptional(id);
		if (mc.isEmpty()) {
			return Optional.empty();
		}
		BlockState state = mc.get().defaultBlockState();
		for (Map.Entry<String, String> prop : block.props().entrySet()) {
			Property<?> property = state.getBlock().getStateDefinition().getProperty(prop.getKey());
			if (property != null) {
				state = withValue(state, property, prop.getValue());
			}
		}
		return Optional.of(state);
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}
}
