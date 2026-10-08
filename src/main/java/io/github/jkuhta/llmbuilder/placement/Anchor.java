package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Where a building goes: its front centre at the player's crosshair, front facing the player. */
public final class Anchor {
	private static final double REACH = 96;
	private static final int FALLBACK_DISTANCE = 10;

	/** Ground position under the crosshair and the direction the building's front should face. */
	public record Target(BlockPos ground, Direction front) {
		public Transform transformFor(BlockBuffer buffer) {
			return Transform.anchored(localAnchor(buffer), ground, front);
		}
	}

	private Anchor() {
	}

	/** Captures the target when the command runs, so moving while the design is generated does not matter. */
	public static Target target(ServerPlayer player) {
		Direction facing = player.getDirection();
		BlockPos target;
		HitResult hit = player.pick(REACH, 1.0f, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			target = blockHit.getBlockPos();
		} else {
			target = player.blockPosition().relative(facing, FALLBACK_DISTANCE);
		}
		int ground = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
		return new Target(new BlockPos(target.getX(), ground, target.getZ()), facing.getOpposite());
	}

	/** Front-centre of the building at ground level, in local coordinates. */
	public static Vec3 localAnchor(BlockBuffer buffer) {
		BlockBuffer.Box box = buffer.bounds();
		return new Vec3((box.minX() + box.maxX()) / 2, 0, box.maxZ());
	}
}
