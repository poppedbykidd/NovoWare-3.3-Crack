package dev.orchardgui.screen;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import dev.orchardgui.ui.Assets;
import dev.orchardgui.ui.OrchardGui;

/**
 * Hosts {@link OrchardGui} inside Minecraft. The GUI is laid out in physical pixels so it looks
 * exactly like the reference at any GUI scale, and is shrunk only when the window is too small.
 */
public final class ClickGuiScreen extends Screen {
	public static final int OPEN_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

	private static final float DESIGN_W = OrchardGui.BOUNDS_X2 - OrchardGui.BOUNDS_X1;
	private static final float DESIGN_H = OrchardGui.BOUNDS_Y2 - OrchardGui.BOUNDS_Y1;

	private static Assets assets;
	private static float userScale = 1;

	private final McCanvas canvas = new McCanvas();
	private OrchardGui gui;

	// design pixel -> GUI unit mapping, recomputed every frame
	private float originX;
	private float originY;
	private float scale = 1;

	// Right Shift only closes the GUI after it was released once, so the press (or key repeat)
	// that opened the GUI can never close it straight away.
	private boolean openKeyReleased;
	private boolean leftDown = true;
	private boolean rightDown = true;
	private boolean resizing;
	private double resizeStartX;
	private double resizeStartY;
	private float resizeStartScale;

	public ClickGuiScreen() {
		super(Component.literal("Orchard"));
	}

	@Override
	protected void init() {
		if (assets == null) {
			assets = new Assets(McTextures::load);
		}

		if (gui == null) {
			gui = new OrchardGui(assets, this::onClose);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// The original draws straight over the world: no blur, no dimming.
	}

	private void updateTransform() {
		Window window = minecraft.getWindow();
		float guiPerPixel = (float) width / window.getWidth();
		float maxFit = Math.min(window.getWidth() * 0.98f / DESIGN_W, window.getHeight() * 0.98f / DESIGN_H);
		float pixels = Math.min(maxFit, Math.min(1, maxFit) * userScale); // physical px per design px

		// keep the panel on whole physical pixels so it stays crisp at 100%
		float left = Math.round((window.getWidth() - DESIGN_W * pixels) / 2f - OrchardGui.BOUNDS_X1 * pixels);
		float top = Math.round((window.getHeight() - DESIGN_H * pixels) / 2f - OrchardGui.BOUNDS_Y1 * pixels);

		scale = pixels * guiPerPixel;
		originX = left * guiPerPixel;
		originY = top * guiPerPixel;
	}

	private double toDesignX(double guiX) {
		return (guiX - originX) / scale;
	}

	private double toDesignY(double guiY) {
		return (guiY - originY) / scale;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		updateTransform();
		pollMouse(mouseX, mouseY);

		graphics.pose().pushMatrix();
		graphics.pose().translate(originX, originY);
		graphics.pose().scale(scale, scale);
		canvas.begin(graphics, originX, originY, scale);
		gui.render(canvas, toDesignX(mouseX), toDesignY(mouseY));
		graphics.pose().popMatrix();
	}

	/** Clicks are read straight from GLFW so this works across the 1.21.x input API changes. */
	private void pollMouse(int mouseX, int mouseY) {
		long handle = minecraft.getWindow().handle();

		if (GLFW.glfwGetKey(handle, OPEN_KEY) != GLFW.GLFW_PRESS) {
			openKeyReleased = true;
		}

		boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		double x = toDesignX(mouseX);
		double y = toDesignY(mouseY);

		if (left && !leftDown) {
			if (gui.isOverResizeHandle(x, y)) {
				resizing = true;
				resizeStartX = mouseX;
				resizeStartY = mouseY;
				resizeStartScale = userScale;
			} else {
				gui.mousePressed(x, y, 0);
			}
		} else if (!left && leftDown) {
			if (!resizing) {
				gui.mouseReleased(x, y, 0);
			}

			resizing = false;
		}

		if (right && !rightDown) {
			gui.mousePressed(x, y, 1);
		} else if (!right && rightDown) {
			gui.mouseReleased(x, y, 1);
		}

		if (resizing) {
			// drag the corner: scale so it follows the cursor, measured from the screen centre
			double startDist = (resizeStartX - width / 2.0) + (resizeStartY - height / 2.0);
			double dist = (mouseX - width / 2.0) + (mouseY - height / 2.0);

			if (startDist > 1) {
				userScale = (float) Math.max(0.5, Math.min(2, resizeStartScale * dist / startDist));
			}
		}

		leftDown = left;
		rightDown = right;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		gui.mouseScrolled(toDesignX(mouseX), toDesignY(mouseY), scrollY);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		long handle = minecraft.getWindow().handle();

		if (key == GLFW.GLFW_KEY_ESCAPE && gui.escape()) {
			return true;
		}

		if (!gui.isSearchFocused()) {
			if (key == OPEN_KEY) {
				if (openKeyReleased) {
					onClose();
				}

				return true;
			}

			return super.keyPressed(event);
		}

		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
					|| GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
			gui.backspace(ctrl);
			return true;
		}

		if (key == GLFW.GLFW_KEY_SPACE) {
			gui.typeCharacter(' ');
			return true;
		}

		// Printable keys, resolved through the user's keyboard layout.
		String name = key == GLFW.GLFW_KEY_UNKNOWN ? null : GLFW.glfwGetKeyName(key, 0);

		if (name != null && name.length() == 1) {
			boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
					|| GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
			char ch = name.charAt(0);
			gui.typeCharacter(shift ? Character.toUpperCase(ch) : ch);
			return true;
		}

		return super.keyPressed(event);
	}
}
