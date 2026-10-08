package io.github.jkuhta.llmbuilder.generator.resolve;

import io.github.jkuhta.llmbuilder.generator.BuildContext;
import io.github.jkuhta.llmbuilder.generator.Component;
import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.BlockShapes;
import io.github.jkuhta.llmbuilder.generator.core.Dir;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sets the side connections of panes, iron bars, fences and walls from their neighbours,
 * approximating the vanilla rules. Placement re-runs the game's own shape update afterwards, so
 * this mainly makes previews and tests faithful.
 */
public final class Connections implements Component {
	@Override
	public void apply(BuildContext ctx) {
		resolve(ctx.buffer);
	}

	public static void resolve(BlockBuffer buffer) {
		List<BlockBuffer.Entry> targets = buffer.entries(e ->
			BlockShapes.isPaneLike(e.block()) || BlockShapes.isFence(e.block()) || BlockShapes.isWall(e.block()));
		for (BlockBuffer.Entry e : targets) {
			Block b = e.block();
			Map<String, String> props = new HashMap<>();
			int connected = 0;
			boolean[] sides = new boolean[4];
			for (Dir d : Dir.values()) {
				Block n = buffer.get(e.pos().offset(d));
				boolean c = connects(b, n);
				sides[d.ordinal()] = c;
				if (c) {
					connected++;
				}
			}
			if (BlockShapes.isWall(b)) {
				Block above = buffer.get(e.pos().above());
				boolean tall = BlockShapes.isFullCube(above);
				for (Dir d : Dir.values()) {
					props.put(d.id(), sides[d.ordinal()] ? (tall ? "tall" : "low") : "none");
				}
				boolean straight = connected == 2 && (sides[0] && sides[2] || sides[1] && sides[3]);
				props.put("up", Boolean.toString(!straight || BlockShapes.isWall(above)));
			} else {
				for (Dir d : Dir.values()) {
					props.put(d.id(), Boolean.toString(sides[d.ordinal()]));
				}
			}
			buffer.set(e.pos(), b.with(props), e.part());
		}
	}

	private static boolean connects(Block self, Block other) {
		if (other == null || other.isAir()) {
			return false;
		}
		if (BlockShapes.isFullCube(other)) {
			return true;
		}
		if (BlockShapes.isPaneLike(self)) {
			return BlockShapes.isPaneLike(other) || BlockShapes.isWall(other);
		}
		if (BlockShapes.isFence(self)) {
			boolean selfNether = self.path().startsWith("nether_brick");
			return BlockShapes.isFence(other) && other.path().startsWith("nether_brick") == selfNether
				|| BlockShapes.isFenceGate(other);
		}
		return BlockShapes.isWall(other) || BlockShapes.isPaneLike(other) || BlockShapes.isFenceGate(other);
	}
}
