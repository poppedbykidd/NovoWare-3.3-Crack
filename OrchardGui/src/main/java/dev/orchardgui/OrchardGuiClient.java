package dev.orchardgui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

public final class OrchardGuiClient implements ClientModInitializer {
	public static final String MOD_ID = "orchardgui";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// The GUI is opened from KeyboardHandlerMixin (Right Shift).
		LOGGER.info("Orchard GUI loaded - press Right Shift to open it");
	}
}
