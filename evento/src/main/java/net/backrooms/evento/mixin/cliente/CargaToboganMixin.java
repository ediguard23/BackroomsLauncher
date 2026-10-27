package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.cinematica.ToboganCliente;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mientras se baja por el tobogan, la pantalla de cargar el mundo nuevo es negra (sigue la cinematica). */
@Mixin(LevelLoadingScreen.class)
public abstract class CargaToboganMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void backrooms$negro(GuiGraphics g, int x, int y, float parcial, CallbackInfo ci) {
		if ((ToboganCliente.activa() || net.backrooms.evento.cliente.cinematica.SalidaFiestaCliente.activa())) {
			g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF000000);
			ci.cancel();
		}
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void backrooms$sinFondo(GuiGraphics g, int x, int y, float parcial, CallbackInfo ci) {
		if ((ToboganCliente.activa() || net.backrooms.evento.cliente.cinematica.SalidaFiestaCliente.activa())) {
			g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF000000);
			ci.cancel();
		}
	}
}
