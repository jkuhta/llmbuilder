package io.github.jkuhta.llmbuilder.generator.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BayLayoutTest {
	private static void assertSymmetric(int length, BayLayout.Result r) {
		for (int i = 0; i < r.bays().size(); i++) {
			BayLayout.Bay a = r.bays().get(i);
			BayLayout.Bay b = r.bays().get(r.bays().size() - 1 - i);
			assertEquals(a.u0(), length - 1 - b.u1(), "bay " + i + " not mirrored in " + r);
		}
	}

	@Test
	void canalHouseThreeNarrowBays() {
		BayLayout.Result r = BayLayout.compute(9, 3, 1);
		assertEquals(List.of(new BayLayout.Bay(0, 2, 1), new BayLayout.Bay(1, 4, 1), new BayLayout.Bay(2, 6, 1)), r.bays());
		assertSymmetric(9, r);
	}

	@Test
	void layoutsAreAlwaysSymmetric() {
		for (int length = 5; length <= 30; length++) {
			for (int bays = 1; bays <= (length - 1) / 2; bays++) {
				for (int w = 1; w <= 3; w++) {
					BayLayout.Result r = BayLayout.compute(length, bays, w);
					assertSymmetric(length, r);
					int last = r.bays().get(r.bays().size() - 1).u1();
					assertTrue(last <= length - 2, "bays overrun corner: " + r);
				}
			}
		}
	}

	@Test
	void reducesWidthToKeepWallBetweenWindows() {
		BayLayout.Result r = BayLayout.compute(9, 3, 3);
		for (int i = 1; i < r.bays().size(); i++) {
			assertTrue(r.bays().get(i).u0() > r.bays().get(i - 1).u1(), "windows touch: " + r);
		}
	}

	@Test
	void keepsInnerPiersUniform() {
		// 17 m chapel nave, 5 lancets: piers 2 wide everywhere, 1 block margins at the ends.
		BayLayout.Result r = BayLayout.compute(17, 5, 1);
		List<Integer> widths = r.gaps().stream().map(BayLayout.Gap::width).toList();
		assertEquals(List.of(1, 2, 2, 2, 2, 1), widths);
	}

	@Test
	void prefersGenerousMarginsOverWidePiers() {
		BayLayout.Result r = BayLayout.compute(13, 3, 1);
		assertEquals(List.of(2, 2, 2, 2), r.gaps().stream().map(BayLayout.Gap::width).toList());
	}
}
