package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Al despertar de la cinematica la vista va de muy borrosa a nitida: el blur
 * de los menus de Minecraft lee su radio de los uniforms globales, y aqui se
 * cambia ese radio mientras dura.
 */
@Mixin(GameRenderer.class)
public abstract class CinematicaBlurMixin {
	@ModifyArg(method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlobalSettingsUniform;update(IIDJLnet/minecraft/client/DeltaTracker;ILnet/minecraft/client/Camera;Z)V"),
		index = 5)
	private int backrooms$radioBlur(int radio) {
		int r = CinematicaCliente.desenfoque();
		return r > 0 ? r : radio;
	}
}
