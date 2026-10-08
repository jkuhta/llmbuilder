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

	private Anchor() {
	}

	public static Transform at(ServerPlayer player, BlockBuffer buffer) {
		Direction facing = player.getDirection();
		BlockPos target;
		HitResult hit = player.pick(REACH, 1.0f, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			target = blockHit.getBlockPos();
		} else {
			target = player.blockPosition().relative(facing, FALLBACK_DISTANCE);
		}
		int ground = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
		BlockPos worldAnchor = new BlockPos(target.getX(), ground, target.getZ());
		return Transform.anchored(localAnchor(buffer), worldAnchor, facing.getOpposite());
	}

	/** Front-centre of the building at ground level, in local coordinates. */
	public static Vec3 localAnchor(BlockBuffer buffer) {
		BlockBuffer.Box box = buffer.bounds();
		return new Vec3((box.minX() + box.maxX()) / 2, 0, box.maxZ());
	}
}
