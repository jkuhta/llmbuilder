package io.github.jkuhta.llmbuilder.generator;

import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Part;
import io.github.jkuhta.llmbuilder.generator.core.Vec3;
import io.github.jkuhta.llmbuilder.generator.layout.BayLayout;
import io.github.jkuhta.llmbuilder.generator.layout.FacadeFrame;
import io.github.jkuhta.llmbuilder.generator.layout.Layout;
import io.github.jkuhta.llmbuilder.generator.material.BlockPalette;
import io.github.jkuhta.llmbuilder.generator.roof.RoofPlan;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Detail.Level;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Facade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared state for one generation run, passed to every component in order. */
public final class BuildContext {
	public final BuildingSpec spec;
	public final Layout layout;
	public final BlockPalette palette;
	public final BlockBuffer buffer = new BlockBuffer();
	private final List<String> warnings = new ArrayList<>();
	private final Map<FacadeFrame, BayLayout.Result> bays = new LinkedHashMap<>();
	private RoofPlan roofPlan;

	public BuildContext(BuildingSpec spec) {
		this.spec = spec;
		this.layout = Layout.of(spec);
		this.palette = new BlockPalette(spec.palette(), spec.seed());
	}

	public void set(Vec3 p, Block block, Part part) {
		buffer.set(p, block, part);
	}

	public boolean setIfFree(Vec3 p, Block block, Part part) {
		return buffer.setIfFree(p, block, part);
	}

	public boolean detail(Level atLeast) {
		return spec.detail().level().ordinal() >= atLeast.ordinal();
	}

	public void warn(String message) {
		if (!warnings.contains(message)) {
			warnings.add(message);
		}
	}

	public List<String> warnings() {
		return List.copyOf(warnings);
	}

	/** The spec facade governing a frame; wings reuse the spec of the world side they face. */
	public Facade facadeSpec(FacadeFrame frame) {
		return spec.facade(frame.side());
	}

	/**
	 * Bay positions for a facade, shared by every floor so windows line up vertically. Wings get
	 * a bay count scaled to their length.
	 */
	public BayLayout.Result bays(FacadeFrame frame) {
		return bays.computeIfAbsent(frame, f -> {
			Facade facade = facadeSpec(f);
			if (facade.blank()) {
				return BayLayout.compute(f.length(), 0, 1);
			}
			int count = facade.bays();
			if (!f.mass().isMain()) {
				int mainLength = FacadeFrame.of(layout.main(), f.side()).length();
				count = Math.max(1, Math.round(count * (float) f.length() / mainLength));
			}
			return BayLayout.compute(f.length(), count, preferredWindowWidth(f, facade, count));
		});
	}

	private static int preferredWindowWidth(FacadeFrame frame, Facade facade, int count) {
		if (facade.windows().width() > 0) {
			return facade.windows().width();
		}
		if (count == 0) {
			return 1;
		}
		int pitch = (frame.length() - 1) / count;
		return switch (facade.windows().type()) {
			case LANCET, ARCHED -> pitch >= 4 ? 2 : 1;
			case SLIT, ROUND -> 1;
			case RIBBON, SHOPFRONT -> Math.max(1, pitch - 1);
			default -> pitch >= 5 ? 3 : pitch >= 4 ? 2 : 1;
		};
	}

	public RoofPlan roofPlan() {
		return roofPlan;
	}

	public void setRoofPlan(RoofPlan roofPlan) {
		this.roofPlan = roofPlan;
		layout.setRoofTop(roofPlan::topAt);
	}
}
