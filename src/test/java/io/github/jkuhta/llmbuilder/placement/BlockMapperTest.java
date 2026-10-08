package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.BuildingGenerator;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Blocks;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.spec.ExampleSpecs;
import io.github.jkuhta.llmbuilder.spec.SpecParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Runs against Minecraft's real block registry. */
class BlockMapperTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	static List<String> names() {
		return ExampleSpecs.NAMES;
	}

	@ParameterizedTest
	@MethodSource("names")
	void everyGeneratedBlockMapsExactly(String name) {
		var spec = SpecParser.parse(ExampleSpecs.json(name).orElseThrow()).spec();
		BlockBuffer buffer = BuildingGenerator.generate(spec).buffer();
		List<String> problems = new ArrayList<>();
		for (BlockBuffer.Entry e : buffer.entries()) {
			var state = BlockMapper.toState(e.block());
			if (state.isEmpty()) {
				problems.add("unknown block " + e.block());
				continue;
			}
			for (Map.Entry<String, String> prop : e.block().props().entrySet()) {
				String actual = valueName(state.get(), prop.getKey());
				if (!prop.getValue().equals(actual)) {
					problems.add(e.block() + ": " + prop.getKey() + " became " + actual);
				}
			}
		}
		assertEquals(List.of(), problems.stream().distinct().limit(10).toList());
	}

	@Test
	void rotationTurnsStairsWithTheBuilding() {
		Block stair = Blocks.stairs("oak_stairs", Dir.NORTH, Blocks.Half.BOTTOM);
		BlockState state = BlockMapper.toState(stair).orElseThrow().rotate(Rotation.CLOCKWISE_90);
		assertEquals(Direction.EAST, state.getValue(StairBlock.FACING));
	}

	private static String valueName(BlockState state, String key) {
		Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
		return property == null ? null : name(state, property);
	}

	private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}
}
