package dev.orchardgui.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Display entries shown in the list. Only labels are provided (no module logic);
 * hook your own modules in by calling {@link #add}.
 */
public final class ModuleRegistry {
	private static final List<ModuleEntry> ENTRIES = new ArrayList<>();

	static {
		combat("Aim Assist", "Gently moves your crosshair toward the opponent you are fighting.", ModuleEntry.Type.GHOST, false);
		combat("Aim Optimizer", "Slows mouse movement near targets and releases speed on the hitbox", ModuleEntry.Type.GHOST, false);
		combat("AirAnchor", "Attempts to place another respawn anchor in the exploded anchor location", ModuleEntry.Type.BLATANT, false);
		combat("AntiBot", "Detects and ignores anti-cheat bots instantly across servers.", ModuleEntry.Type.GHOST, true);
		combat("Auto Crystal", "Automatically places & breaks crystals", ModuleEntry.Type.BLATANT, false);
		combat("Auto Inventory Totem", "Briefly opens inventory and moves a totem into your offhand", ModuleEntry.Type.GHOST, false);
		combat("Auto Totem", "Refills your offhand with a totem after it is missing or pops", ModuleEntry.Type.BLATANT, false);
		combat("AutoCart", "Automatically places a rail and minecart at a flaming arrow impact point", ModuleEntry.Type.BLATANT, false);
		combat("AutoCrossy", "Shoots weak opponents with a charged crossbow.", ModuleEntry.Type.BLATANT, true);
		combat("AutoDhand", "Moves a totem to the main hand after your offhand totem pops.", ModuleEntry.Type.GHOST, false);
		combat("AutoHitCrystal", "Places an obsidian & a crystal when hitting the ground", ModuleEntry.Type.GHOST, false);
		combat("AutoMace", "Aims and lands a mace smash on nearby opponents while falling.", ModuleEntry.Type.BLATANT, false);
		combat("AutoSafeAnchor", "Places glowstone on the floor beside a newly placed anchor, toward the enemy", ModuleEntry.Type.GHOST, false);
		combat("Autoclicker", "Clicks for you while you hold the selected mouse button.", ModuleEntry.Type.GHOST, false);
	}

	private ModuleRegistry() {
	}

	private static void combat(String name, String description, ModuleEntry.Type type, boolean enabled) {
		add(new ModuleEntry(Page.COMBAT, name, description, type, enabled));
	}

	public static void add(ModuleEntry entry) {
		ENTRIES.add(entry);
	}

	public static List<ModuleEntry> all() {
		return Collections.unmodifiableList(ENTRIES);
	}
}
