package net.backrooms.menu.mixin;

import net.backrooms.menu.Tema;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** El panorama de los menus (opciones, conectando, desconectado...) es el pasillo del nivel del evento. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void backrooms$piscinas(GuiGraphics g, float parcial, CallbackInfo ci) {
		Tema.actual().dibujarFondo(g, this.width, this.height);
		ci.cancel();
	}
}
