package io.github.jkuhta.llmbuilder.testutil;

import io.github.jkuhta.llmbuilder.generator.core.Block;
import io.github.jkuhta.llmbuilder.generator.core.BlockBuffer;
import io.github.jkuhta.llmbuilder.generator.core.Dir;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a block buffer to an isometric PNG at quarter-block resolution, approximating stairs,
 * slabs, panes, fences, walls and trapdoors by their real shapes. Test-only; lets generator
 * output be reviewed without launching Minecraft.
 */
public final class IsoRenderer {
	private static final int RES = 4;

	public enum View {
		FRONT_RIGHT,
		BACK_LEFT
	}

	private record Voxel(int x, int y, int z, BlockColors.Rgba color) {
	}

	private IsoRenderer() {
	}

	public static File render(BlockBuffer buffer, String name, View view, int scale) throws IOException {
		String dir = System.getProperty("llmbuilder.renderDir", "build/renders");
		File out = new File(dir, name + "-" + view.name().toLowerCase() + ".png");
		out.getParentFile().mkdirs();
		ImageIO.write(render(buffer, view, scale), "png", out);
		return out;
	}

	/**
	 * Straight-on elevation of the front (south) face. Each pixel shows the nearest surface,
	 * darkened with depth, so recesses and projections stand out.
	 */
	public static File elevation(BlockBuffer buffer, String name, int scale) throws IOException {
		BlockBuffer.Box box = buffer.bounds();
		int sx = box.sizeX() * RES, sy = box.sizeY() * RES, sz = box.sizeZ() * RES;
		BufferedImage img = new BufferedImage(sx * scale + 20, sy * scale + 20, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(235, 240, 245));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		BlockColors.Rgba[] grid = voxelize(buffer, box, View.FRONT_RIGHT);
		for (int x = 0; x < sx; x++) {
			for (int y = 0; y < sy; y++) {
				double r = 0, gg = 0, b = 0, remaining = 1;
				int hitDepth = -1;
				for (int z = sz - 1; z >= 0 && remaining > 0.02; z--) {
					BlockColors.Rgba c = grid[(x * sy + y) * sz + z];
					if (c == null) {
						continue;
					}
					if (hitDepth < 0) {
						hitDepth = sz - 1 - z;
					}
					double shade = Math.max(0.35, 1.0 - (sz - 1 - z) / (double) RES * 0.18);
					double a = c.a() / 255.0;
					r += remaining * a * c.r() * shade;
					gg += remaining * a * c.g() * shade;
					b += remaining * a * c.b() * shade;
					remaining *= 1 - a;
				}
				if (hitDepth < 0) {
					continue;
				}
				g.setColor(new Color(clamp(r + remaining * 235), clamp(gg + remaining * 240), clamp(b + remaining * 245)));
				g.fillRect(10 + x * scale, 10 + (sy - 1 - y) * scale, scale, scale);
			}
		}
		g.dispose();
		File out = new File(System.getProperty("llmbuilder.renderDir", "build/renders"), name + "-elevation.png");
		out.getParentFile().mkdirs();
		ImageIO.write(img, "png", out);
		return out;
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, v));
	}

	private static BlockColors.Rgba[] voxelize(BlockBuffer buffer, BlockBuffer.Box box, View view) {
		int sx = box.sizeX() * RES, sy = box.sizeY() * RES, sz = box.sizeZ() * RES;
		BlockColors.Rgba[] grid = new BlockColors.Rgba[sx * sy * sz];
		for (BlockBuffer.Entry e : buffer.entries()) {
			if (e.block().isAir()) {
				continue;
			}
			BlockColors.Rgba color = BlockColors.of(e.block().path());
			boolean[] mask = shape(e.block(), buffer, e.pos().x(), e.pos().y(), e.pos().z());
			int bx = (e.pos().x() - box.minX()) * RES, by = (e.pos().y() - box.minY()) * RES, bz = (e.pos().z() - box.minZ()) * RES;
			for (int i = 0; i < RES; i++) {
				for (int j = 0; j < RES; j++) {
					for (int k = 0; k < RES; k++) {
						if (mask[(i * RES + j) * RES + k]) {
							int x = bx + i, y = by + j, z = bz + k;
							if (view == View.BACK_LEFT) {
								x = sx - 1 - x;
								z = sz - 1 - z;
							}
							grid[(x * sy + y) * sz + z] = color;
						}
					}
				}
			}
		}
		return grid;
	}

	public static BufferedImage render(BlockBuffer buffer, View view, int scale) {
		BlockBuffer.Box box = buffer.bounds();
		int sx = box.sizeX() * RES, sy = box.sizeY() * RES, sz = box.sizeZ() * RES;
		BlockColors.Rgba[] grid = voxelize(buffer, box, view);
		int s = scale;
		int width = (sx + sz) * s * 2 + 40;
		int height = (sx + sz) * s + sy * s * 2 + 40;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setColor(new Color(235, 240, 245));
		g.fillRect(0, 0, width, height);
		int ox = sz * s * 2 + 20;
		int oy = sy * s * 2 + 20;
		// Camera at +x +z looking towards -x -z: draw far to near, bottom to top.
		for (int sum = 0; sum <= sx + sz - 2; sum++) {
			for (int y = 0; y < sy; y++) {
				for (int x = Math.max(0, sum - sz + 1); x <= Math.min(sx - 1, sum); x++) {
					int z = sum - x;
					BlockColors.Rgba c = grid[(x * sy + y) * sz + z];
					if (c == null) {
						continue;
					}
					boolean top = y + 1 >= sy || grid[(x * sy + y + 1) * sz + z] == null;
					boolean south = z + 1 >= sz || grid[(x * sy + y) * sz + z + 1] == null;
					boolean east = x + 1 >= sx || grid[((x + 1) * sy + y) * sz + z] == null;
					if (!top && !south && !east && c.a() == 255) {
						continue;
					}
					int px = ox + (x - z) * s * 2;
					int py = oy + (x + z) * s - y * s * 2;
					if (c.a() < 255) {
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, c.a() / 255f));
					}
					if (top) {
						fill(g, c, 1.0, new int[] {px, px + 2 * s, px, px - 2 * s}, new int[] {py - 2 * s, py - s, py, py - s});
					}
					if (south) {
						fill(g, c, 0.78, new int[] {px - 2 * s, px, px, px - 2 * s}, new int[] {py - s, py, py + 2 * s, py + s});
					}
					if (east) {
						fill(g, c, 0.62, new int[] {px, px + 2 * s, px + 2 * s, px}, new int[] {py, py - s, py + s, py + 2 * s});
					}
					g.setComposite(AlphaComposite.SrcOver);
				}
			}
		}
		g.dispose();
		return img;
	}

	private static void fill(Graphics2D g, BlockColors.Rgba c, double shade, int[] xs, int[] ys) {
		g.setColor(new Color((int) (c.r() * shade), (int) (c.g() * shade), (int) (c.b() * shade)));
		g.fillPolygon(new Polygon(xs, ys, 4));
	}

	/** 4x4x4 occupancy mask indexed [x][y][z] within the block. */
	static boolean[] shape(Block b, BlockBuffer buffer, int bx, int by, int bz) {
		boolean[] m = new boolean[RES * RES * RES];
		String p = b.path();
		if (p.endsWith("_stairs")) {
			boolean top = "top".equals(b.prop("half"));
			Dir f = Dir.fromId(b.prop("facing"));
			String shape = b.prop("shape") == null ? "straight" : b.prop("shape");
			box(m, 0, top ? 2 : 0, 0, 4, top ? 4 : 2, 4);
			int y0 = top ? 0 : 2, y1 = top ? 2 : 4;
			List<int[]> quads = new ArrayList<>();
			switch (shape) {
				case "outer_left" -> quads.add(quad(f, f.ccw()));
				case "outer_right" -> quads.add(quad(f, f.cw()));
				case "inner_left" -> {
					quads.add(quad(f, f.ccw()));
					quads.add(quad(f, f.cw()));
					quads.add(quad(f.opposite(), f.ccw()));
				}
				case "inner_right" -> {
					quads.add(quad(f, f.ccw()));
					quads.add(quad(f, f.cw()));
					quads.add(quad(f.opposite(), f.cw()));
				}
				default -> {
					quads.add(quad(f, f.ccw()));
					quads.add(quad(f, f.cw()));
				}
			}
			for (int[] q : quads) {
				box(m, q[0], y0, q[1], q[0] + 2, y1, q[1] + 2);
			}
		} else if (p.endsWith("_slab")) {
			String type = b.prop("type");
			if ("double".equals(type)) {
				box(m, 0, 0, 0, 4, 4, 4);
			} else if ("top".equals(type)) {
				box(m, 0, 2, 0, 4, 4, 4);
			} else {
				box(m, 0, 0, 0, 4, 2, 4);
			}
		} else if (p.endsWith("_trapdoor")) {
			if ("true".equals(b.prop("open"))) {
				Dir f = Dir.fromId(b.prop("facing"));
				Dir panel = f.opposite();
				int x0 = panel == Dir.EAST ? 3 : 0, x1 = panel == Dir.WEST ? 1 : 4;
				int z0 = panel == Dir.SOUTH ? 3 : 0, z1 = panel == Dir.NORTH ? 1 : 4;
				box(m, x0, 0, z0, x1, 4, z1);
			} else if ("top".equals(b.prop("half"))) {
				box(m, 0, 3, 0, 4, 4, 4);
			} else {
				box(m, 0, 0, 0, 4, 1, 4);
			}
		} else if (p.endsWith("_door")) {
			Dir panel = Dir.fromId(b.prop("facing")).opposite();
			int x0 = panel == Dir.EAST ? 3 : 0, x1 = panel == Dir.WEST ? 1 : 4;
			int z0 = panel == Dir.SOUTH ? 3 : 0, z1 = panel == Dir.NORTH ? 1 : 4;
			box(m, x0, 0, z0, x1, 4, z1);
		} else if (p.endsWith("_pane") || p.equals("iron_bars")) {
			box(m, 1, 0, 1, 3, 4, 3);
			arms(m, b, 1, 3, 0, 4);
		} else if (p.endsWith("_fence")) {
			box(m, 1, 0, 1, 3, 4, 3);
			arms(m, b, 1, 3, 2, 3);
		} else if (p.endsWith("_wall")) {
			box(m, 1, 0, 1, 3, 4, 3);
			arms(m, b, 1, 3, 0, 3);
		} else if (p.contains("lantern")) {
			box(m, 1, 0, 1, 3, 2, 3);
		} else if (p.endsWith("_button") || p.contains("torch") || p.equals("flower_pot") || p.startsWith("potted_")) {
			box(m, 1, 0, 1, 3, 1, 3);
		} else {
			box(m, 0, 0, 0, 4, 4, 4);
		}
		return m;
	}

	private static void arms(boolean[] m, Block b, int lo, int hi, int y0, int y1) {
		if ("true".equals(b.prop("north")) || "low".equals(b.prop("north")) || "tall".equals(b.prop("north"))) {
			box(m, lo, y0, 0, hi, y1, 2);
		}
		if ("true".equals(b.prop("south")) || "low".equals(b.prop("south")) || "tall".equals(b.prop("south"))) {
			box(m, lo, y0, 2, hi, y1, 4);
		}
		if ("true".equals(b.prop("west")) || "low".equals(b.prop("west")) || "tall".equals(b.prop("west"))) {
			box(m, 0, y0, lo, 2, y1, hi);
		}
		if ("true".equals(b.prop("east")) || "low".equals(b.prop("east")) || "tall".equals(b.prop("east"))) {
			box(m, 2, y0, lo, 4, y1, hi);
		}
	}

	private static int[] quad(Dir a, Dir b) {
		int x = (a == Dir.EAST || b == Dir.EAST) ? 2 : 0;
		int z = (a == Dir.SOUTH || b == Dir.SOUTH) ? 2 : 0;
		return new int[] {x, z};
	}

	private static void box(boolean[] m, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				for (int z = z0; z < z1; z++) {
					m[(x * RES + y) * RES + z] = true;
				}
			}
		}
	}
}
