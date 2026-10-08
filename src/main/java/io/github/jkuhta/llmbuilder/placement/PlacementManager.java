package io.github.jkuhta.llmbuilder.placement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Runs placement jobs on the server thread, sharing a per-tick block budget between players so
 * one large build cannot stall the server. Keeps each player's finished builds for undo.
 */
public final class PlacementManager {
	private static final int HISTORY = 5;

	private record Active(PlacementJob job, Consumer<PlacementJob> onDone) {
	}

	private final Map<UUID, Active> active = new LinkedHashMap<>();
	private final Map<UUID, Deque<PlacementJob>> history = new HashMap<>();
	private final IntSupplier budget;

	public PlacementManager(IntSupplier budget) {
		this.budget = budget;
	}

	/** Starts a job; returns false if the player already has one running. */
	public boolean start(PlacementJob job, Consumer<PlacementJob> onDone) {
		if (active.containsKey(job.owner())) {
			return false;
		}
		active.put(job.owner(), new Active(job, onDone));
		return true;
	}

	public boolean isBusy(UUID owner) {
		return active.containsKey(owner);
	}

	public void tick() {
		if (active.isEmpty()) {
			return;
		}
		int share = Math.max(1, budget.getAsInt() / active.size());
		List<UUID> done = new ArrayList<>();
		for (Map.Entry<UUID, Active> e : active.entrySet()) {
			PlacementJob job = e.getValue().job();
			job.tick(share);
			if (job.isDone()) {
				done.add(e.getKey());
			}
		}
		for (UUID owner : done) {
			Active a = active.remove(owner);
			Deque<PlacementJob> past = history.computeIfAbsent(owner, k -> new ArrayDeque<>());
			past.push(a.job());
			while (past.size() > HISTORY) {
				past.removeLast();
			}
			a.onDone().accept(a.job());
		}
	}

	/** Removes and returns the player's most recent finished build. */
	public Optional<PlacementJob> popHistory(UUID owner) {
		Deque<PlacementJob> past = history.get(owner);
		return past == null || past.isEmpty() ? Optional.empty() : Optional.of(past.pop());
	}
}
