package io.github.jkuhta.llmbuilder.testutil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Average colour of a block's vanilla texture, guessed from its id. Test-only, for preview renders. */
public final class BlockColors {
	public record Rgba(int r, int g, int b, int a) {
	}

	private static final Map<String, Rgba> CACHE = new HashMap<>();
	private static final Rgba UNKNOWN = new Rgba(255, 0, 255, 255);

	private BlockColors() {
	}

	public static synchronized Rgba of(String path) {
		return CACHE.computeIfAbsent(path, BlockColors::load);
	}

	private static Rgba load(String path) {
		for (String candidate : candidates(path)) {
			Rgba c = average("/assets/minecraft/textures/block/" + candidate + ".png");
			if (c != null) {
				if (path.contains("glass")) {
					return new Rgba(c.r(), c.g(), c.b(), 110);
				}
				if (path.contains("leaves")) {
					return new Rgba(70, 120, 45, 255);
				}
				return c;
			}
		}
		return UNKNOWN;
	}

	private static final Map<String, String> ALIASES = Map.of(
		"smooth_quartz", "quartz_block_bottom",
		"quartz", "quartz_block_side",
		"quartz_block", "quartz_block_side",
		"smooth_sandstone", "sandstone_top",
		"smooth_red_sandstone", "red_sandstone_top",
		"smooth_stone", "smooth_stone"
	);

	private static List<String> candidates(String path) {
		List<String> bases = new ArrayList<>();
		for (Map.Entry<String, String> alias : ALIASES.entrySet()) {
			if (path.equals(alias.getKey()) || path.startsWith(alias.getKey() + "_")) {
				bases.add(alias.getValue());
			}
		}
		bases.add(path);
		for (String suffix : new String[] {"_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pane", "_button"}) {
			if (path.endsWith(suffix)) {
				bases.add(path.substring(0, path.length() - suffix.length()));
			}
		}
		List<String> result = new ArrayList<>();
		for (String b : bases) {
			result.add(b);
			result.add(b + "s");
			result.add(b + "_planks");
			result.add(b + "_block");
			result.add(b + "_side");
			result.add(b + "_block_side");
			result.add(b + "_bottom");
			result.add(b.replace("waxed_", ""));
			result.add(b.replace("smooth_", "") + "_top");
			if (b.startsWith("smooth_")) {
				result.add(b.substring(7) + "_top");
			}
		}
		return result;
	}

	private static Rgba average(String resource) {
		try (InputStream in = BlockColors.class.getResourceAsStream(resource)) {
			if (in == null) {
				return null;
			}
			BufferedImage img = ImageIO.read(in);
			long r = 0, g = 0, b = 0, n = 0;
			int h = Math.min(img.getHeight(), img.getWidth());
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < img.getWidth(); x++) {
					int argb = img.getRGB(x, y);
					int a = (argb >>> 24) & 0xff;
					if (a < 128) {
						continue;
					}
					r += (argb >> 16) & 0xff;
					g += (argb >> 8) & 0xff;
					b += argb & 0xff;
					n++;
				}
			}
			if (n == 0) {
				return new Rgba(200, 220, 230, 80);
			}
			return new Rgba((int) (r / n), (int) (g / n), (int) (b / n), 255);
		} catch (IOException e) {
			return null;
		}
	}
}
