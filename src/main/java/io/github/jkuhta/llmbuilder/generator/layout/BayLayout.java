package io.github.jkuhta.llmbuilder.generator.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Divides a facade into window bays. The result is always mirror-symmetric, as in real
 * architecture: leftover space goes to symmetric pairs of gaps, and a final odd block widens the
 * centre gap or the centre bay.
 */
public final class BayLayout {
	/** A bay's opening columns, in facade {@code u} coordinates. */
	public record Bay(int index, int u0, int width) {
		public int u1() {
			return u0 + width - 1;
		}

		public int center2() {
			return u0 + u1();
		}
	}

	/** Wall columns between bays (or between a bay and a corner). */
	public record Gap(int u0, int width, boolean inner) {
	}

	public record Result(List<Bay> bays, List<Gap> gaps) {
	}

	private BayLayout() {
	}

	/**
	 * @param length facade length including the two corner columns
	 * @param bays number of bays
	 * @param preferredWidth desired opening width; reduced if the bays do not fit
	 */
	public static Result compute(int length, int bays, int preferredWidth) {
		if (bays <= 0) {
			return new Result(List.of(), List.of(new Gap(1, Math.max(0, length - 2), false)));
		}
		int span = length - 2;
		for (boolean needEdges : new boolean[] {true, false}) {
			for (int w = Math.max(1, preferredWidth); w >= 1; w--) {
				int gaps = span - bays * w;
				int minGaps = needEdges ? bays + 1 : bays - 1;
				if (gaps < minGaps) {
					continue;
				}
				return build(bays, w, gaps, needEdges);
			}
		}
		// Does not fit even at width 1: squeeze as many as possible.
		int fit = Math.max(1, (span + 1) / 2);
		return compute(length, fit, 1);
	}

	private static Result build(int bays, int width, int gapTotal, boolean edges) {
		int[] g = new int[bays + 1];
		int remaining = gapTotal;
		for (int i = 0; i <= bays; i++) {
			boolean inner = i > 0 && i < bays;
			if (inner || edges) {
				g[i] = 1;
				remaining--;
			}
		}
		int[] widths = new int[bays];
		java.util.Arrays.fill(widths, width);
		boolean progress = true;
		while (remaining > 0 && progress) {
			progress = false;
			for (int i = 0; i <= bays / 2; i++) {
				int j = bays - i;
				int need = i == j ? 1 : 2;
				if (remaining >= need) {
					g[i]++;
					if (i != j) {
						g[j]++;
					}
					remaining -= need;
					progress = true;
				}
			}
		}
		if (remaining == 1) {
			// Odd block left: even bay count has a centre gap, odd bay count has a centre bay.
			if (bays % 2 == 0) {
				g[bays / 2]++;
			} else {
				widths[bays / 2]++;
			}
		}
		List<Bay> result = new ArrayList<>();
		List<Gap> gapList = new ArrayList<>();
		int u = 1;
		for (int i = 0; i < bays; i++) {
			gapList.add(new Gap(u, g[i], i > 0));
			u += g[i];
			result.add(new Bay(i, u, widths[i]));
			u += widths[i];
		}
		gapList.add(new Gap(u, g[bays], false));
		return new Result(List.copyOf(result), List.copyOf(gapList));
	}
}
