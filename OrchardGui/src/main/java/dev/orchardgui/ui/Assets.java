package dev.orchardgui.ui;

/** Every texture and font the GUI uses, plus the shape helpers built on them. */
public final class Assets {
	public static final String ROOT = "/assets/orchardgui/gui/";
	private static final int MAX_RADIUS = 16;

	public final GlyphFont title;
	public final GlyphFont version;
	public final GlyphFont category;
	public final GlyphFont search;
	public final GlyphFont tab;
	public final GlyphFont name;
	public final GlyphFont desc;
	public final GlyphFont button;
	public final GlyphFont buttonAlt;

	public final Texture iconCombat;
	public final Texture iconMovement;
	public final Texture iconPlayer;
	public final Texture iconRender;
	public final Texture iconHud;
	public final Texture iconConfigs;
	public final Texture iconBinds;
	public final Texture iconThemes;
	public final Texture iconSearch;
	public final Texture buttonGlow;

	private final Texture[] circles = new Texture[MAX_RADIUS + 1];

	public Assets(TextureSource textures) {
		title = GlyphFont.load("title", textures);
		version = GlyphFont.load("version", textures);
		category = GlyphFont.load("category", textures);
		search = GlyphFont.load("search", textures);
		tab = GlyphFont.load("tab", textures);
		name = GlyphFont.load("name", textures);
		desc = GlyphFont.load("desc", textures);
		button = GlyphFont.load("button", textures);
		buttonAlt = GlyphFont.load("button_alt", textures);

		iconCombat = textures.load("icon_combat");
		iconMovement = textures.load("icon_movement");
		iconPlayer = textures.load("icon_player");
		iconRender = textures.load("icon_render");
		iconHud = textures.load("icon_hud");
		iconConfigs = textures.load("icon_configs");
		iconBinds = textures.load("icon_binds");
		iconThemes = textures.load("icon_themes");
		iconSearch = textures.load("icon_search");
		buttonGlow = textures.load("glow_button");

		for (int r = 1; r <= MAX_RADIUS; r++) {
			circles[r] = textures.load("circle_" + r);
		}
	}

	public void icon(Canvas c, Texture icon, int x, int y, int color) {
		c.blit(icon, x, y, 0, 0, icon.width(), icon.height(), color);
	}

	/** Filled rectangle with anti-aliased rounded corners. */
	public void roundRect(Canvas c, int x, int y, int w, int h, int radius, int color) {
		int r = Math.max(0, Math.min(Math.min(radius, MAX_RADIUS), Math.min(w, h) / 2));

		if (r == 0) {
			c.fill(x, y, x + w, y + h, color);
			return;
		}

		Texture circle = circles[r];
		c.blit(circle, x, y, 0, 0, r, r, color);
		c.blit(circle, x + w - r, y, r, 0, r, r, color);
		c.blit(circle, x, y + h - r, 0, r, r, r, color);
		c.blit(circle, x + w - r, y + h - r, r, r, r, r, color);
		c.fill(x + r, y, x + w - r, y + r, color);
		c.fill(x, y + r, x + w, y + h - r, color);
		c.fill(x + r, y + h - r, x + w - r, y + h, color);
	}

	/** Rounded rectangle with a solid border (both colours should be opaque). */
	public void roundRectBordered(Canvas c, int x, int y, int w, int h, int radius, int border, int fill, int borderColor) {
		roundRect(c, x, y, w, h, radius, borderColor);
		roundRect(c, x + border, y + border, w - 2 * border, h - 2 * border, Math.max(0, radius - border), fill);
	}
}
