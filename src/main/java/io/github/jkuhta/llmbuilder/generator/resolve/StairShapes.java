package io.github.jkuhta.llmbuilder.generator.resolve;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.BlockShapes;
import io.github.jkuhta.llmbuilder.generator.core.Dir;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes every stair's {@code shape} from its neighbours with the same rules as vanilla
 * {@code StairBlock.getStairsShape}, so hip corners and roof valleys get proper inner and outer
 * corner pieces and the preview matches what the game would show.
 */
public final class StairShapes implements Component {
	@Override
	public void apply(BuildContext ctx) {
		resolve(ctx.buffer);
	}

	public static void resolve(BlockBuffer buffer) {
		List<BlockBuffer.Entry> stairs = buffer.entries(e -> BlockShapes.isStairs(e.block()));
		// Shapes depend only on facing and half, which do not change, so one pass is enough.
		List<BlockBuffer.Entry> updated = new ArrayList<>();
		for (BlockBuffer.Entry e : stairs) {
			String shape = shape(buffer, e.pos(), e.block());
			updated.add(new BlockBuffer.Entry(e.pos(), e.block().with("shape", shape), e.part()));
		}
		for (BlockBuffer.Entry e : updated) {
			buffer.set(e.pos(), e.block(), e.part());
		}
	}

	static String shape(BlockBuffer buffer, Vec3 pos, Block state) {
		Dir facing = Dir.fromId(state.prop("facing"));
		String half = state.prop("half");
		Block front = buffer.get(pos.offset(facing));
		if (BlockShapes.isStairs(front) && half.equals(front.prop("half"))) {
			Dir frontFacing = Dir.fromId(front.prop("facing"));
			if (frontFacing.isXAxis() != facing.isXAxis() && canTakeShape(buffer, pos, state, frontFacing.opposite())) {
				return frontFacing == facing.ccw() ? "outer_left" : "outer_right";
			}
		}
		Block back = buffer.get(pos.offset(facing.opposite()));
		if (BlockShapes.isStairs(back) && half.equals(back.prop("half"))) {
			Dir backFacing = Dir.fromId(back.prop("facing"));
			if (backFacing.isXAxis() != facing.isXAxis() && canTakeShape(buffer, pos, state, backFacing)) {
				return backFacing == facing.ccw() ? "inner_left" : "inner_right";
			}
		}
		return "straight";
	}

	private static boolean canTakeShape(BlockBuffer buffer, Vec3 pos, Block state, Dir dir) {
		Block other = buffer.get(pos.offset(dir));
		return !BlockShapes.isStairs(other)
			|| !other.prop("facing").equals(state.prop("facing"))
			|| !other.prop("half").equals(state.prop("half"));
	}
}
