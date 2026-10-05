package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.TablaJugadores;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** La lista del TAB es la del evento (TablaJugadores) en vez de la de Minecraft. */
@Mixin(PlayerTabOverlay.class)
public abstract class TablaJugadoresMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void backrooms$tablaDelEvento(GuiGraphics g, int ancho, Scoreboard marcador, @Nullable Objective objetivo, CallbackInfo ci) {
		TablaJugadores.dibujar(g, ancho);
		ci.cancel();
	}
}
