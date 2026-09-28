# Orchard GUI (Fabric 1.21.11)

A 1:1 recreation of the Orchard click GUI as a Fabric mod for Minecraft **1.21.11**.
It is **GUI only**: there are no modules/cheat features, only the menu itself.

- Open / close in game with **Right Shift** (Esc also closes).
- Sidebar categories and pages (animated selection), search box, All / Ghost / Blatant filters,
  two-column module cards with hover, click a card to toggle its enabled (purple gradient) state,
  smooth scrolling, drag the purple corner handle to resize, "Control Panel" and "Self Destruct"
  buttons (Self Destruct just closes the GUI).
- Drawn in real screen pixels, so it matches the reference exactly at any Minecraft GUI scale.
  It only shrinks when the window is smaller than the panel (about 1240x830); play fullscreen
  or in a large window for the pixel-exact look.

## Build (Windows)

1. Install a **Java 21** JDK (for example Temurin 21) and make sure `java` is on PATH or `JAVA_HOME` is set.
2. Double-click **`build.bat`**.
3. The mod ends up in `out\orchard-gui-1.0.0.jar`. Put it in `.minecraft\mods` with
   Fabric Loader 0.17.3+ for 1.21.11. Fabric API is **not** required.

On macOS/Linux run `./gradlew build`; the jar is in `build/libs/`.

## Adding your own entries

The list shows display entries from `ModuleRegistry`. Add more with:

```java
ModuleRegistry.add(new ModuleEntry(Page.MOVEMENT, "Sprint", "Always sprint.", ModuleEntry.Type.GHOST, false));
```

## Project layout

| Path | What |
| --- | --- |
| `src/main/java/dev/orchardgui/ui` | The GUI itself (layout, drawing, input). Plain Java, no Minecraft classes. |
| `src/main/java/dev/orchardgui/screen` | Minecraft `Screen` that hosts it, `GuiGraphics` canvas, texture upload. |
| `src/main/java/dev/orchardgui/mixin` | Right Shift keybind. |
| `src/main/resources/assets/orchardgui/gui` | Generated glyph atlases, icons, corner and glow textures. |
| `tools/generate_assets.py` | Regenerates those textures (fonts, icon sizes, colours). |

Layout numbers in `OrchardGui.java` are in the reference screenshot's pixel coordinates, so you
can measure any element in the screenshot and use the number directly.

## Credits

- Font: [Red Hat Display](https://github.com/RedHatOfficial/RedHatFont), SIL Open Font License 1.1.
- Icons: [Material Icons / Material Symbols](https://github.com/google/material-design-icons), Apache License 2.0.
