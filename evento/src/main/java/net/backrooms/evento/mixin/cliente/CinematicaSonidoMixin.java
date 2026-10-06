package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Durante la cinematica solo se oye su banda sonora: el resto del mundo esta
 * callado y vuelve poco a poco al despertar (como quien recupera el oido).
 */
@Mixin(SoundEngine.class)
public abstract class CinematicaSonidoMixin {
	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
	private void backrooms$silenciarMundo(SoundInstance sonido, CallbackInfoReturnable<Float> cir) {
		if (CinematicaCliente.activa() && !CinematicaCliente.esBanda(sonido.getIdentifier())) {
			cir.setReturnValue(cir.getReturnValueF() * CinematicaCliente.volumenMundo());
		}
	}
}
