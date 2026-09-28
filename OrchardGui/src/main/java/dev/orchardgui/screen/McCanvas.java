package dev.orchardgui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import dev.orchardgui.ui.Canvas;
import dev.orchardgui.ui.Texture;

/**
 * {@link Canvas} backed by GuiGraphics. Draw calls happen inside a pose that maps design
 * pixels to GUI units; scissor rectangles are converted to GUI units by hand.
 */
final class McCanvas implements Canvas {
	private GuiGraphics graphics;
	private float originX;
	private float originY;
	private float scale;

	void begin(GuiGraphics graphics, float originX, float originY, float scale) {
		this.graphics = graphics;
		this.originX = originX;
		this.originY = originY;
		this.scale = scale;
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		if (x2 > x1 && y2 > y1) {
			graphics.fill(x1, y1, x2, y2, argb);
		}
	}

	@Override
	public void blit(Texture texture, int x, int y, int u, int v, int w, int h, int argb) {
		McTextures.McTexture tex = (McTextures.McTexture) texture;
		graphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(), x, y, u, v, w, h, tex.width(), tex.height(), argb);
	}

	@Override
	public void pushClip(int x1, int y1, int x2, int y2) {
		int gx1 = (int) Math.floor(originX + x1 * scale);
		int gy1 = (int) Math.floor(originY + y1 * scale);
		int gx2 = (int) Math.ceil(originX + x2 * scale);
		int gy2 = (int) Math.ceil(originY + y2 * scale);

		// Enable the scissor with an identity pose so the rectangle is taken as plain GUI units,
		// whether or not this version transforms scissor rectangles by the current pose.
		graphics.pose().pushMatrix();
		graphics.pose().identity();
		graphics.enableScissor(gx1, gy1, gx2, gy2);
		graphics.pose().popMatrix();
	}

	@Override
	public void popClip() {
		graphics.disableScissor();
	}
}
