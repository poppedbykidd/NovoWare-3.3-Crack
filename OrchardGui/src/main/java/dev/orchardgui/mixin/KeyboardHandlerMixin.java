package dev.orchardgui.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import dev.orchardgui.screen.ClickGuiScreen;

/**
 * Opens the click GUI with Right Shift while no other screen is open.
 *
 * <p>The screen is opened after vanilla has finished handling the key press. Opening it at the
 * start would make vanilla forward that same press to the new screen, which closes it again.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Unique
	private boolean orchardgui$noScreenBeforePress;

	@Inject(method = "keyPress", at = @At("HEAD"))
	private void orchardgui$rememberScreen(long window, int action, KeyEvent event, CallbackInfo ci) {
		orchardgui$noScreenBeforePress = Minecraft.getInstance().screen == null;
	}

	@Inject(method = "keyPress", at = @At("RETURN"))
	private void orchardgui$openClickGui(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (!orchardgui$noScreenBeforePress || action != GLFW.GLFW_PRESS || event.key() != ClickGuiScreen.OPEN_KEY) {
			return;
		}

		orchardgui$noScreenBeforePress = false;
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.screen == null && window == minecraft.getWindow().handle()) {
			minecraft.setScreen(new ClickGuiScreen());
		}
	}
}
