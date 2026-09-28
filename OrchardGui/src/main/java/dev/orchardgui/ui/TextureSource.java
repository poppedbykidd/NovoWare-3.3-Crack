package dev.orchardgui.ui;

/** Turns a PNG from {@code assets/orchardgui/gui/<name>.png} into a drawable texture. */
@FunctionalInterface
public interface TextureSource {
	Texture load(String name);
}
