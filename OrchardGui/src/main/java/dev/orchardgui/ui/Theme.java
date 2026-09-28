package dev.orchardgui.ui;

/** Colours sampled from the reference screenshot. */
public final class Theme {
	public static final int PANEL = 0xFF1B1D22;
	public static final int PANEL_BORDER = 0xFF303237;
	public static final int SIDEBAR = 0xFF15171C;
	public static final int SIDEBAR_HOVER = 0xFF1D1F25;
	public static final int ACCENT = 0xFF7B40A2;

	public static final int TITLE = 0xFF955BFF;
	public static final int VERSION = 0xFF9CA3BA;
	public static final int TEXT = 0xFFFFFFFF;

	public static final int FIELD = 0xFF2A2D35;
	public static final int FIELD_HOVER = 0xFF2F323B;
	public static final int FIELD_SELECTED = 0xFF353840;
	public static final int MUTED_PURPLE = 0xFF987DB7;

	public static final int GRADIENT_START = 0xFFE85DFF;
	public static final int GRADIENT_END = 0xFF955BFF;

	public static final int CARD = 0xFF202228;
	public static final int CARD_HOVER = 0xFF25272E;
	public static final int DESCRIPTION = 0xFF686D7D;

	public static final int BUTTON = 0xFF1D1E21;
	public static final int BUTTON_HOVER = 0xFF24262A;
	public static final int BUTTON_BORDER = 0xFF343842;
	public static final int BUTTON_MUTED = 0xFF676C7B;
	public static final int DANGER = 0xFFB91C1C;
	public static final int DANGER_HOVER = 0xFFC92424;
	public static final int DANGER_BORDER = 0xFFCC2222;
	public static final int DANGER_GLOW = 0xB0DC2626;

	public static final int HANDLE = 0xFF6240A4;

	private Theme() {
	}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int r = 0;

		for (int shift = 0; shift < 32; shift += 8) {
			int ca = a >>> shift & 0xFF;
			int cb = b >>> shift & 0xFF;
			r |= Math.round(ca + (cb - ca) * t) << shift;
		}

		return r;
	}
}
