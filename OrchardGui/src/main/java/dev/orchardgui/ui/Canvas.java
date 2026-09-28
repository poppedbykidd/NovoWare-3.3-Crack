package dev.orchardgui.ui;

/**
 * Minimal drawing surface the GUI renders onto. All coordinates are in design pixels
 * (1 design pixel = 1 physical screen pixel when the GUI is shown at 100% size).
 */
public interface Canvas {
	/** Fills the rectangle [x1, x2) x [y1, y2) with an ARGB colour. */
	void fill(int x1, int y1, int x2, int y2, int argb);

	/** Draws the region (u, v, w, h) of a white alpha-mask texture at (x, y), tinted with an ARGB colour. */
	void blit(Texture texture, int x, int y, int u, int v, int w, int h, int argb);

	/** Restricts drawing to [x1, x2) x [y1, y2) until the matching {@link #popClip()}. */
	void pushClip(int x1, int y1, int x2, int y2);

	void popClip();
}
