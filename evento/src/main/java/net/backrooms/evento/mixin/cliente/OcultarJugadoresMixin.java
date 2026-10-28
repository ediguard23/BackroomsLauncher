package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.cinematica.ToboganCliente;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** En la cinematica del tobogan no se ve a ningun jugador (tampoco a uno mismo, que se queda en la puerta). */
@Mixin(EntityRenderDispatcher.class)
public abstract class OcultarJugadoresMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void backrooms$sinJugadores(E entidad, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (entidad instanceof Player && (ToboganCliente.activa() || net.backrooms.evento.cliente.cinematica.SalidaFiestaCliente.activa() || net.backrooms.evento.cliente.cinematica.SalidaEstacionCliente.activa())) {
			cir.setReturnValue(false);
		}
	}
}
