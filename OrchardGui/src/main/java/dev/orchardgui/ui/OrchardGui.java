package dev.orchardgui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Orchard click GUI, independent of Minecraft. Coordinates are "design pixels" that match
 * the reference screenshot 1:1 (panel top-left is at (-2, 3)); the screen maps them to the window.
 */
public final class OrchardGui {
	// Area occupied by the panel plus the buttons (and their glow) underneath, used for centering.
	public static final int BOUNDS_X1 = -2;
	public static final int BOUNDS_Y1 = 3;
	public static final int BOUNDS_X2 = 1236;
	public static final int BOUNDS_Y2 = 827;

	private static final int PANEL_X = -2, PANEL_Y = 3, PANEL_W = 1238, PANEL_H = 764, PANEL_R = 16, PANEL_BORDER = 3;
	private static final int SIDEBAR_X = 10, SIDEBAR_Y = 15, SIDEBAR_W = 199, SIDEBAR_H = 740, SIDEBAR_R = 12;

	private static final int ITEM_X = 18, ITEM_W = 184, ITEM_H = 48, ITEM_R = 8;
	private static final int FIRST_ITEM_Y = 119, BOTTOM_ITEM_Y = 531, ITEM_PITCH = 56;
	private static final int LABEL_X = 74, SELECTED_SHIFT = 4;

	private static final int CONTENT_X = 250, CONTENT_R = 1209;
	private static final int SEARCH_Y = 19, SEARCH_H = 44, SEARCH_R = 10;
	private static final int TAB_Y = 79, TAB_H = 44, TAB_R = 8, TAB_GAP = 12;
	private static final int LIST_Y = 139, LIST_BOTTOM = 764, CARD_H = 74, ROW_PITCH = 90, COLUMN_GAP = 12, CARD_R = 6;

	private static final int BUTTON_Y = 779, BUTTON_W = 200, BUTTON_H = 40, BUTTON_R = 10;
	private static final int CONTROL_X = 409, DESTRUCT_X = 625;

	private static final String[] TAB_LABELS = {"All", "Ghost", "Blatant"};

	// Icon placement per page: {x, y offset from item centre}. Sizes come from the textures.
	private static final int[][] ICON_POS = {
			{36, -11}, {36, -14}, {35, -14}, {35, -10}, {34, -11}, {34, -11}, {36, -10}, {35, -9}, {35, -12}
	};

	// Remembered between openings, like a real client.
	private static Page page = Page.COMBAT;
	private static int filter = 0;

	private final Assets assets;
	private final Runnable onClose;
	private final StringBuilder query = new StringBuilder();
	private boolean searchFocused;

	private float highlightY = Float.NaN;
	private final float[] shift = new float[Page.values().length];
	private float scroll;
	private float scrollTarget;
	private long lastFrame = System.nanoTime();
	private long caretStart = System.nanoTime();

	private int pressedButton = -1; // 0 = control panel, 1 = self destruct

	public OrchardGui(Assets assets, Runnable onClose) {
		this.assets = assets;
		this.onClose = onClose;
	}

	// ------------------------------------------------------------------ layout helpers

	private static int itemCenter(Page p) {
		int i = p.ordinal();
		return i < 5 ? FIRST_ITEM_Y + i * ITEM_PITCH : BOTTOM_ITEM_Y + (i - 5) * ITEM_PITCH;
	}

	private static int tabX(int i) {
		int w = tabWidth(0);
		return CONTENT_X + i * (w + TAB_GAP) + (i > 0 ? 1 : 0);
	}

	private static int tabWidth(int i) {
		return i == 0 ? 311 : 312;
	}

	private static int columnX(int col) {
		return col == 0 ? CONTENT_X : CONTENT_X + 473 + COLUMN_GAP;
	}

	private static int columnWidth(int col) {
		return col == 0 ? 473 : CONTENT_R - columnX(1);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private boolean showsModules() {
		return page.category || query.length() > 0;
	}

	private List<ModuleEntry> visibleEntries() {
		String q = query.toString().toLowerCase(Locale.ROOT).trim();
		List<ModuleEntry> out = new ArrayList<>();

		for (ModuleEntry e : ModuleRegistry.all()) {
			if (q.isEmpty() ? e.category != page : !e.name.toLowerCase(Locale.ROOT).contains(q)) {
				continue;
			}

			if (filter == 1 && e.type != ModuleEntry.Type.GHOST || filter == 2 && e.type != ModuleEntry.Type.BLATANT) {
				continue;
			}

			out.add(e);
		}

		return out;
	}

	private float maxScroll(int count) {
		int rows = (count + 1) / 2;
		int bottom = LIST_Y + (rows - 1) * ROW_PITCH + CARD_H + 10;
		return Math.max(0, bottom - LIST_BOTTOM);
	}

	public boolean isOverResizeHandle(double mx, double my) {
		return inside(mx, my, 1204, 734, 28, 28);
	}

	// ------------------------------------------------------------------ rendering

	public void render(Canvas c, double mx, double my) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		animate(dt);

		assets.roundRectBordered(c, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, PANEL_R, PANEL_BORDER, Theme.PANEL, Theme.PANEL_BORDER);

		renderSidebar(c, mx, my);
		renderSearch(c, mx, my);

		if (showsModules()) {
			renderTabs(c, mx, my);
			renderList(c, mx, my);
		} else {
			renderPlaceholder(c, page.label, "Nothing here yet");
		}

		renderResizeHandle(c);
		renderButtons(c, mx, my);
	}

	private void animate(float dt) {
		float target = itemCenter(page) - ITEM_H / 2f;
		float k = 1 - (float) Math.exp(-dt * 20);
		highlightY = Float.isNaN(highlightY) ? target : highlightY + (target - highlightY) * k;

		for (Page p : Page.values()) {
			float t = p == page ? SELECTED_SHIFT : 0;
			shift[p.ordinal()] += (t - shift[p.ordinal()]) * k;
		}

		scroll += (scrollTarget - scroll) * (1 - (float) Math.exp(-dt * 16));
	}

	private void renderSidebar(Canvas c, double mx, double my) {
		assets.roundRect(c, SIDEBAR_X, SIDEBAR_Y, SIDEBAR_W, SIDEBAR_H, SIDEBAR_R, Theme.SIDEBAR);

		float titleEnd = assets.title.draw(c, "Orchard", 25, 66, Theme.TITLE);
		assets.version.draw(c, "v1.7.5", titleEnd + 3, 67, Theme.VERSION);

		for (Page p : Page.values()) {
			int cy = itemCenter(p);

			if (p != page && inside(mx, my, ITEM_X, cy - ITEM_H / 2, ITEM_W, ITEM_H)) {
				assets.roundRect(c, ITEM_X, cy - ITEM_H / 2, ITEM_W, ITEM_H, ITEM_R, Theme.SIDEBAR_HOVER);
			}
		}

		assets.roundRect(c, ITEM_X, Math.round(highlightY), ITEM_W, ITEM_H, ITEM_R, Theme.ACCENT);

		for (Page p : Page.values()) {
			int cy = itemCenter(p);
			int dx = Math.round(shift[p.ordinal()]);
			int[] pos = ICON_POS[p.ordinal()];
			assets.icon(c, icon(p), pos[0] + dx, cy + pos[1], Theme.TEXT);
			assets.category.draw(c, p.label, LABEL_X + dx, cy + 6, Theme.TEXT);
		}
	}

	private Texture icon(Page p) {
		return switch (p) {
			case COMBAT -> assets.iconCombat;
			case MOVEMENT -> assets.iconMovement;
			case PLAYER -> assets.iconPlayer;
			case RENDER -> assets.iconRender;
			case HUD, EDITOR -> assets.iconHud;
			case CONFIGS -> assets.iconConfigs;
			case BINDS -> assets.iconBinds;
			case THEMES -> assets.iconThemes;
		};
	}

	private void renderSearch(Canvas c, double mx, double my) {
		int w = CONTENT_R - CONTENT_X;
		boolean hover = inside(mx, my, CONTENT_X, SEARCH_Y, w, SEARCH_H);
		assets.roundRect(c, CONTENT_X, SEARCH_Y, w, SEARCH_H, SEARCH_R, hover || searchFocused ? Theme.FIELD_HOVER : Theme.FIELD);
		assets.icon(c, assets.iconSearch, 265, 29, Theme.MUTED_PURPLE);

		float end = 298;

		if (query.length() == 0) {
			if (!searchFocused) {
				assets.search.draw(c, "Search", 298, 48, Theme.MUTED_PURPLE);
			}
		} else {
			c.pushClip(298, SEARCH_Y, CONTENT_R - 16, SEARCH_Y + SEARCH_H);
			String text = query.toString();
			float overflow = Math.max(0, assets.search.width(text) - (CONTENT_R - 16 - 300));
			end = assets.search.draw(c, text, 298 - overflow, 48, Theme.TEXT);
			c.popClip();
		}

		if (searchFocused && (System.nanoTime() - caretStart) / 530_000_000L % 2 == 0) {
			int x = Math.round(end) + 1;
			c.fill(x, 31, x + 2, 52, Theme.MUTED_PURPLE);
		}
	}

	private void renderTabs(Canvas c, double mx, double my) {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			int x = tabX(i);
			int w = tabWidth(i);
			boolean selected = filter == i;
			boolean hover = inside(mx, my, x, TAB_Y, w, TAB_H);
			assets.roundRect(c, x, TAB_Y, w, TAB_H, TAB_R, selected ? Theme.FIELD_SELECTED : hover ? Theme.FIELD_HOVER : Theme.FIELD);

			float tw = assets.tab.width(TAB_LABELS[i]);
			float tx = x + (w - tw) / 2f - 1.5f;

			if (selected) {
				assets.tab.draw(c, TAB_LABELS[i], tx, 108, Theme.GRADIENT_START, Theme.GRADIENT_END);
			} else {
				assets.tab.draw(c, TAB_LABELS[i], tx, 108, Theme.MUTED_PURPLE);
			}
		}
	}

	private void renderList(Canvas c, double mx, double my) {
		List<ModuleEntry> entries = visibleEntries();
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll(entries.size())));

		if (entries.isEmpty()) {
			renderPlaceholder(c, null, query.length() > 0 ? "No modules match \"" + query + "\"" : "No modules in this category");
			return;
		}

		int offset = Math.round(scroll);
		boolean mouseInList = inside(mx, my, CONTENT_X, LIST_Y, CONTENT_R - CONTENT_X, LIST_BOTTOM - LIST_Y);
		c.pushClip(CONTENT_X, LIST_Y, CONTENT_R, LIST_BOTTOM);

		for (int i = 0; i < entries.size(); i++) {
			ModuleEntry e = entries.get(i);
			int x = columnX(i % 2);
			int w = columnWidth(i % 2);
			int y = LIST_Y + i / 2 * ROW_PITCH - offset;

			if (y + CARD_H < LIST_Y || y > LIST_BOTTOM) {
				continue;
			}

			boolean hover = mouseInList && inside(mx, my, x, y, w, CARD_H);
			assets.roundRect(c, x, y, w, CARD_H, CARD_R, hover ? Theme.CARD_HOVER : Theme.CARD);

			if (e.enabled) {
				assets.name.draw(c, e.name, x + 19, y + 27, Theme.GRADIENT_START, Theme.GRADIENT_END);
			} else {
				assets.name.draw(c, e.name, x + 19, y + 27, Theme.TEXT);
			}

			assets.desc.draw(c, assets.desc.truncate(e.description, 66, w - 36), x + 20, y + 54, Theme.DESCRIPTION);
		}

		c.popClip();
	}

	private void renderPlaceholder(Canvas c, String title, String subtitle) {
		int cx = (CONTENT_X + CONTENT_R) / 2;
		int cy = title == null ? 440 : 400;

		if (title != null) {
			assets.name.draw(c, title, cx - assets.name.width(title) / 2f, cy, Theme.TEXT);
		}

		assets.desc.draw(c, subtitle, cx - assets.desc.width(subtitle) / 2f, cy + (title == null ? 0 : 26), Theme.DESCRIPTION);
	}

	private void renderResizeHandle(Canvas c) {
		for (int t = 0; t <= 15; t++) {
			c.fill(1210 + t, 754 - t, 1212 + t, 756 - t, Theme.HANDLE);
		}
	}

	private void renderButtons(Canvas c, double mx, double my) {
		boolean hoverControl = inside(mx, my, CONTROL_X, BUTTON_Y, BUTTON_W, BUTTON_H);
		assets.roundRectBordered(c, CONTROL_X, BUTTON_Y, BUTTON_W, BUTTON_H, BUTTON_R, 2,
				hoverControl ? Theme.BUTTON_HOVER : Theme.BUTTON, Theme.BUTTON_BORDER);
		float wControl = assets.button.width("Control") + 6;
		float wPanel = assets.buttonAlt.width("Panel");
		float x = CONTROL_X + (BUTTON_W - wControl - wPanel) / 2f;
		assets.button.draw(c, "Control", x, 804, Theme.TEXT);
		assets.buttonAlt.draw(c, "Panel", x + wControl, 805, Theme.BUTTON_MUTED);

		boolean hoverDestruct = inside(mx, my, DESTRUCT_X, BUTTON_Y, BUTTON_W, BUTTON_H);
		Texture glow = assets.buttonGlow;
		int margin = (glow.width() - BUTTON_W) / 2;
		assets.icon(c, glow, DESTRUCT_X - margin, BUTTON_Y - margin, Theme.DANGER_GLOW);
		assets.roundRectBordered(c, DESTRUCT_X, BUTTON_Y, BUTTON_W, BUTTON_H, BUTTON_R, 1,
				hoverDestruct ? Theme.DANGER_HOVER : Theme.DANGER, Theme.DANGER_BORDER);
		String label = "Self Destruct";
		assets.button.draw(c, label, DESTRUCT_X + (BUTTON_W - assets.button.width(label)) / 2f - 1, 804, Theme.TEXT);
	}

	// ------------------------------------------------------------------ input

	public void mousePressed(double mx, double my, int button) {
		searchFocused = inside(mx, my, CONTENT_X, SEARCH_Y, CONTENT_R - CONTENT_X, SEARCH_H) && button == 0;
		caretStart = System.nanoTime();

		for (Page p : Page.values()) {
			if (inside(mx, my, ITEM_X, itemCenter(p) - ITEM_H / 2, ITEM_W, ITEM_H)) {
				if (page != p) {
					page = p;
					scroll = scrollTarget = 0;
				}

				query.setLength(0);
				return;
			}
		}

		if (showsModules()) {
			for (int i = 0; i < TAB_LABELS.length; i++) {
				if (inside(mx, my, tabX(i), TAB_Y, tabWidth(i), TAB_H)) {
					filter = i;
					scroll = scrollTarget = 0;
					return;
				}
			}

			if (inside(mx, my, CONTENT_X, LIST_Y, CONTENT_R - CONTENT_X, LIST_BOTTOM - LIST_Y)) {
				List<ModuleEntry> entries = visibleEntries();
				int offset = Math.round(scroll);

				for (int i = 0; i < entries.size(); i++) {
					int y = LIST_Y + i / 2 * ROW_PITCH - offset;

					if (inside(mx, my, columnX(i % 2), y, columnWidth(i % 2), CARD_H)) {
						if (button == 0) {
							entries.get(i).enabled = !entries.get(i).enabled;
						}

						return;
					}
				}
			}
		}

		if (inside(mx, my, CONTROL_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
			pressedButton = 0;
		} else if (inside(mx, my, DESTRUCT_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
			pressedButton = 1;
		}
	}

	public void mouseReleased(double mx, double my, int button) {
		if (pressedButton == 1 && inside(mx, my, DESTRUCT_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
			// GUI-only project: "Self Destruct" simply closes the GUI.
			onClose.run();
		}

		pressedButton = -1;
	}

	public void mouseScrolled(double mx, double my, double amount) {
		if (showsModules()) {
			scrollTarget = (float) Math.max(0, Math.min(maxScroll(visibleEntries().size()), scrollTarget - amount * 45));
		}
	}

	public boolean isSearchFocused() {
		return searchFocused;
	}

	public void typeCharacter(char ch) {
		if (searchFocused && ch >= 32 && ch <= 126 && query.length() < 48) {
			query.append(ch);
			scroll = scrollTarget = 0;
			caretStart = System.nanoTime();
		}
	}

	public void backspace(boolean word) {
		if (!searchFocused || query.length() == 0) {
			return;
		}

		if (word) {
			query.setLength(0);
		} else {
			query.setLength(query.length() - 1);
		}

		caretStart = System.nanoTime();
	}

	/** @return true if the key was consumed (escape while typing unfocuses the search box). */
	public boolean escape() {
		if (searchFocused) {
			searchFocused = false;
			return true;
		}

		return false;
	}
}
