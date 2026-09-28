package dev.orchardgui.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import dev.orchardgui.screen.ClickGuiScreen;

/** Opens the click GUI with Right Shift while no other screen is open. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void orchardgui$openClickGui(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (action != GLFW.GLFW_PRESS || event.key() != ClickGuiScreen.OPEN_KEY) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.screen == null && window == minecraft.getWindow().handle()) {
			minecraft.setScreen(new ClickGuiScreen());
		}
	}
}
