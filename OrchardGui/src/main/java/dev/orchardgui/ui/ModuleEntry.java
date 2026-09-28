package dev.orchardgui.ui;

/**
 * A row in the module list. This project only contains the GUI, so an entry is just
 * display data plus an on/off flag that the GUI toggles.
 */
public final class ModuleEntry {
	public enum Type {
		GHOST,
		BLATANT
	}

	public final Page category;
	public final String name;
	public final String description;
	public final Type type;
	public boolean enabled;

	public ModuleEntry(Page category, String name, String description, Type type, boolean enabled) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.type = type;
		this.enabled = enabled;
	}
}
