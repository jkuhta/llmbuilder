package io.github.jkuhta.llmbuilder.placement;

import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.BlockShapes;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Places a generated building over several ticks. Blocks are ordered so supports go down before
 * what hangs on them: clearing, then full blocks, then shaped blocks, then attachments such as
 * doors and lanterns. Every overwritten block is recorded so the build can be undone.
 */
public final class PlacementJob {
	/** Shape updates are suppressed while placing; the final pass recomputes connections. */
	static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
	/** How deep the foundation is extended down to uneven ground. */
	private static final int MAX_FOOTING = 12;

	record Step(BlockPos pos, BlockState state) {
	}

	/** A block as it was before placement, for undo. */
	public record Snapshot(BlockPos pos, BlockState state, CompoundTag blockEntity) {
	}

	private final UUID owner;
	private final String name;
	private final ServerLevel level;
	private final List<Step> steps;
	private final List<Snapshot> snapshots = new ArrayList<>();
	private final Set<BlockPos> touched = new HashSet<>();
	private int cursor;
	private boolean finished;

	private PlacementJob(UUID owner, String name, ServerLevel level, List<Step> steps) {
		this.owner = owner;
		this.name = name;
		this.level = level;
		this.steps = steps;
	}

	public static PlacementJob create(UUID owner, String name, ServerLevel level, BlockBuffer buffer, Transform transform) {
		List<Step> clears = new ArrayList<>();
		List<Step> solids = new ArrayList<>();
		List<Step> shaped = new ArrayList<>();
		List<Step> attached = new ArrayList<>();
		for (BlockBuffer.Entry e : buffer.entries()) {
			BlockState state = BlockMapper.toState(e.block()).orElse(null);
			if (state == null) {
				continue;
			}
			BlockPos pos = transform.toWorld(e.pos());
			Step step = new Step(pos, state.rotate(transform.rotation()));
			if (e.block().isAir()) {
				if (!level.getBlockState(pos).isAir()) {
					clears.add(step);
				}
			} else if (BlockShapes.isFullCube(e.block())) {
				solids.add(step);
				if (e.pos().y() == 0 && isGrounded(e.part())) {
					footing(level, pos, step.state(), solids);
				}
			} else if (isAttachment(e.block())) {
				attached.add(step);
			} else {
				shaped.add(step);
			}
		}
		Comparator<Step> bottomUp = Comparator.comparingInt(s -> s.pos().getY());
		clears.sort(bottomUp.reversed());
		solids.sort(bottomUp);
		shaped.sort(bottomUp);
		attached.sort(bottomUp);
		List<Step> all = new ArrayList<>(clears.size() + solids.size() + shaped.size() + attached.size());
		all.addAll(clears);
		all.addAll(solids);
		all.addAll(shaped);
		all.addAll(attached);
		return new PlacementJob(owner, name, level, all);
	}

	private static boolean isGrounded(Part part) {
		return part == Part.WALL || part == Part.FOUNDATION || part == Part.PLINTH || part == Part.PILLAR
			|| part == Part.STEP || part == Part.CHIMNEY;
	}

	/** Extends ground-level masonry down to solid terrain so the building never floats on slopes. */
	private static void footing(ServerLevel level, BlockPos pos, BlockState state, List<Step> out) {
		for (int i = 1; i <= MAX_FOOTING; i++) {
			BlockPos below = pos.below(i);
			BlockState existing = level.getBlockState(below);
			if (!existing.canBeReplaced() && existing.getFluidState().isEmpty()) {
				return;
			}
			out.add(new Step(below, state));
		}
	}

	private static boolean isAttachment(io.github.jkuhta.llmbuilder.generator.core.Block b) {
		String p = b.path();
		return p.endsWith("_door") || p.endsWith("_trapdoor") || p.contains("lantern") || p.endsWith("_button")
			|| p.contains("torch") || p.endsWith("_sign") || p.equals("ladder") || p.startsWith("potted_") || p.equals("flower_pot");
	}

	/** Places up to {@code budget} blocks and returns how many were placed. */
	public int tick(int budget) {
		int placed = 0;
		while (placed < budget && cursor < steps.size()) {
			Step step = steps.get(cursor++);
			if (touched.add(step.pos())) {
				BlockEntity be = level.getBlockEntity(step.pos());
				CompoundTag tag = be == null ? null : be.saveWithFullMetadata(level.registryAccess());
				snapshots.add(new Snapshot(step.pos(), level.getBlockState(step.pos()), tag));
			}
			level.setBlock(step.pos(), step.state(), FLAGS);
			placed++;
		}
		if (cursor >= steps.size() && !finished) {
			finishConnections();
			finished = true;
		}
		return placed;
	}

	/** Lets fences, panes and walls connect to the actual neighbours now that everything is placed. */
	private void finishConnections() {
		for (Step step : steps) {
			BlockState current = level.getBlockState(step.pos());
			if (!isConnecting(current)) {
				continue;
			}
			BlockState updated = Block.updateFromNeighbourShapes(current, level, step.pos());
			if (updated != current) {
				level.setBlock(step.pos(), updated, FLAGS);
			}
		}
	}

	private static boolean isConnecting(BlockState state) {
		return state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
			|| state.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock
			|| state.getBlock() instanceof net.minecraft.world.level.block.WallBlock;
	}

	public boolean isDone() {
		return finished;
	}

	public int total() {
		return steps.size();
	}

	public int progress() {
		return cursor;
	}

	public UUID owner() {
		return owner;
	}

	public String name() {
		return name;
	}

	/** Lowest corner of everything this job places. */
	public BlockPos min() {
		return steps.stream().map(Step::pos).reduce((a, b) -> new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()))).orElse(BlockPos.ZERO);
	}

	/** Highest corner of everything this job places. */
	public BlockPos max() {
		return steps.stream().map(Step::pos).reduce((a, b) -> new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()))).orElse(BlockPos.ZERO);
	}

	public ServerLevel level() {
		return level;
	}

	public List<Snapshot> snapshots() {
		return snapshots;
	}

}
