package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * Maps building-local coordinates to the world: rotate about the local origin, then translate.
 * Block states are rotated with the same {@link Rotation} so facings stay consistent.
 */
public record Transform(BlockPos origin, Rotation rotation) {
	public BlockPos toWorld(Vec3 local) {
		return new BlockPos(local.x(), local.y(), local.z()).rotate(rotation).offset(origin);
	}

	/**
	 * Places the given local anchor point at {@code worldAnchor}, rotated so the building's front
	 * (local south) faces {@code front}.
	 */
	public static Transform anchored(Vec3 localAnchor, BlockPos worldAnchor, Direction front) {
		Rotation rotation = rotationFromSouth(front);
		BlockPos rotatedAnchor = new BlockPos(localAnchor.x(), localAnchor.y(), localAnchor.z()).rotate(rotation);
		return new Transform(worldAnchor.subtract(rotatedAnchor), rotation);
	}

	static Rotation rotationFromSouth(Direction front) {
		return switch (front) {
			case WEST -> Rotation.CLOCKWISE_90;
			case NORTH -> Rotation.CLOCKWISE_180;
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public Transform rotatedClockwise(Vec3 localAnchor) {
		BlockPos worldAnchor = toWorld(localAnchor);
		Rotation next = rotation.getRotated(Rotation.CLOCKWISE_90);
		BlockPos rotatedAnchor = new BlockPos(localAnchor.x(), localAnchor.y(), localAnchor.z()).rotate(next);
		return new Transform(worldAnchor.subtract(rotatedAnchor), next);
	}
}
