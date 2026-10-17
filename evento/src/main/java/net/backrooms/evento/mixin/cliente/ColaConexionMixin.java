package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.ColaCliente;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mientras se espera en la cola de entrada, la pantalla de conexion es la sala de espera. */
@Mixin(ConnectScreen.class)
public abstract class ColaConexionMixin extends Screen {
	protected ColaConexionMixin(Component titulo) {
		super(titulo);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void backrooms$cola(GuiGraphics g, int x, int y, float parcial, CallbackInfo ci) {
		ColaCliente.dibujar(g, this.width, this.height);
	}
}
