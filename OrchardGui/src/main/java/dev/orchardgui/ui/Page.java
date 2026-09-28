package dev.orchardgui.ui;

/** Sidebar entries. The first five are module categories, the rest are utility pages. */
public enum Page {
	COMBAT("Combat", true),
	MOVEMENT("Movement", true),
	PLAYER("Player", true),
	RENDER("Render", true),
	HUD("Hud", true),
	EDITOR("Editor", false),
	CONFIGS("Configs", false),
	BINDS("Binds", false),
	THEMES("Themes", false);

	public final String label;
	public final boolean category;

	Page(String label, boolean category) {
		this.label = label;
		this.category = category;
	}
}
