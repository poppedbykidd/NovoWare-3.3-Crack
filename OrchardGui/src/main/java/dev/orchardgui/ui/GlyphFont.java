package dev.orchardgui.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Anti-aliased bitmap font baked from Red Hat Display by tools/generate_assets.py.
 * Glyph metrics are in design pixels relative to the pen position on the baseline.
 */
public final class GlyphFont {
	private static final int FIRST = 32;
	private static final int LAST = 126;

	private final Texture atlas;
	private int subpixels = 1;
	private int[][][] glyphs = new int[LAST - FIRST + 1][1][];
	private final float[] advances = new float[LAST - FIRST + 1];
	private final Map<Integer, Float> kerning = new HashMap<>();

	private GlyphFont(Texture atlas) {
		this.atlas = atlas;
	}

	public static GlyphFont load(String name, TextureSource textures) {
		GlyphFont font = new GlyphFont(textures.load("font_" + name));
		String path = Assets.ROOT + "font_" + name + ".txt";

		try (InputStream in = GlyphFont.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + path);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] p = line.split(" ");

				if (p[0].equals("subpixels")) {
					font.subpixels = Integer.parseInt(p[1]);
					font.glyphs = new int[LAST - FIRST + 1][font.subpixels][];
				} else if (p[0].equals("g")) {
					int index = Integer.parseInt(p[1]) - FIRST;
					int w = Integer.parseInt(p[5]);

					if (w > 0) {
						font.glyphs[index][Integer.parseInt(p[2])] = new int[] {
								Integer.parseInt(p[3]), Integer.parseInt(p[4]), w,
								Integer.parseInt(p[6]), Integer.parseInt(p[7]), Integer.parseInt(p[8])
						};
					}

					font.advances[index] = Float.parseFloat(p[9]);
				} else if (p[0].equals("k")) {
					font.kerning.put(Integer.parseInt(p[1]) << 16 | Integer.parseInt(p[2]), Float.parseFloat(p[3]));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + path, e);
		}

		return font;
	}

	private static int index(char c) {
		return c >= FIRST && c <= LAST ? c - FIRST : '?' - FIRST;
	}

	private float kern(char a, char b) {
		Float k = kerning.get(a << 16 | b);
		return k == null ? 0 : k;
	}

	public float width(String text) {
		float w = 0;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (i > 0) {
				w += kern(text.charAt(i - 1), c);
			}

			w += advances[index(c)];
		}

		return w;
	}

	/** Draws text with its baseline at {@code baseline}; returns the pen x after the last glyph. */
	public float draw(Canvas canvas, String text, float x, int baseline, int color) {
		return draw(canvas, text, x, baseline, color, color);
	}

	/** Draws text with a left-to-right colour gradient. */
	public float draw(Canvas canvas, String text, float x, int baseline, int colorStart, int colorEnd) {
		float total = colorStart == colorEnd ? 1 : Math.max(1, width(text));
		float pen = x;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (i > 0) {
				pen += kern(text.charAt(i - 1), c);
			}

			int idx = index(c);
			// pick the variant baked closest to the pen's sub-pixel position
			float px = (float) Math.floor(pen);
			int sub = Math.round((pen - px) * subpixels);

			if (sub == subpixels) {
				px++;
				sub = 0;
			}

			int[] g = glyphs[idx][sub];

			if (g != null) {
				int color = colorStart == colorEnd ? colorStart
						: Theme.lerp(colorStart, colorEnd, (pen - x + advances[idx] / 2) / total);
				canvas.blit(atlas, (int) px + g[4], baseline + g[5], g[0], g[1], g[2], g[3], color);
			}

			pen += advances[idx];
		}

		return pen;
	}

	/**
	 * Shortens descriptions the way the original client does: anything longer than
	 * {@code maxChars} is cut at the last word boundary and gets a trailing "...".
	 * Falls back to a pixel-width cut if the result still does not fit.
	 */
	public String truncate(String text, int maxChars, float maxWidth) {
		if (text.length() > maxChars) {
			int cut = text.lastIndexOf(' ', maxChars);
			text = (cut > 0 ? text.substring(0, cut) : text.substring(0, maxChars)).stripTrailing() + "...";
		}

		return ellipsize(text, maxWidth);
	}

	/** Cuts {@code text} down with a trailing "..." so it fits in {@code maxWidth}. */
	public String ellipsize(String text, float maxWidth) {
		if (width(text) <= maxWidth) {
			return text;
		}

		float budget = maxWidth - width("...");
		int end = text.length();

		while (end > 0 && width(text.substring(0, end)) > budget) {
			end--;
		}

		return text.substring(0, end).stripTrailing() + "...";
	}
}
