package dev.orchardgui.screen;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import dev.orchardgui.OrchardGuiClient;
import dev.orchardgui.ui.Assets;
import dev.orchardgui.ui.Texture;

/**
 * Uploads the GUI's PNGs straight from the mod jar, so the mod needs no Fabric API
 * resource loading. Must be called on the render thread.
 */
final class McTextures {
	record McTexture(Identifier id, int width, int height) implements Texture {
	}

	private McTextures() {
	}

	static Texture load(String name) {
		String path = Assets.ROOT + name + ".png";
		Identifier id = Identifier.fromNamespaceAndPath(OrchardGuiClient.MOD_ID, "gui/" + name);

		try (InputStream in = McTextures.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + path);
			}

			NativeImage image = NativeImage.read(in);
			int width = image.getWidth();
			int height = image.getHeight();
			Minecraft.getInstance().getTextureManager().register(id, createTexture(id, image));
			return new McTexture(id, width, height);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load " + path, e);
		}
	}

	/**
	 * DynamicTexture's constructor has changed shape a few times between 1.21.x releases
	 * (label as Supplier or String), so pick whichever one this game version has.
	 */
	private static AbstractTexture createTexture(Identifier id, NativeImage image) {
		try {
			for (Constructor<?> constructor : DynamicTexture.class.getConstructors()) {
				Class<?>[] params = constructor.getParameterTypes();

				if (params.length == 2 && params[1] == NativeImage.class) {
					if (params[0] == Supplier.class) {
						Supplier<String> label = id::toString;
						return (AbstractTexture) constructor.newInstance(label, image);
					}

					if (params[0] == String.class) {
						return (AbstractTexture) constructor.newInstance(id.toString(), image);
					}
				}

				if (params.length == 1 && params[0] == NativeImage.class) {
					return (AbstractTexture) constructor.newInstance(image);
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not create texture " + id, e);
		}

		throw new IllegalStateException("No usable DynamicTexture constructor for " + id);
	}
}
