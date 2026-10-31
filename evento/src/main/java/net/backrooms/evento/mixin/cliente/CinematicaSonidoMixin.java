package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.Cinematicas;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Durante una cinematica solo se oye su banda sonora: el resto del mundo (el nivel, la megafonia, los
 * pasos de los demas) esta callado y vuelve poco a poco al despertar, como quien recupera el oido
 * (Cinematicas.volumenMundo). Las voces del chat de voz se callan aparte (EcoVoz).
 */
@Mixin(SoundEngine.class)
public abstract class CinematicaSonidoMixin {
	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
	private void backrooms$silenciarMundo(SoundInstance sonido, CallbackInfoReturnable<Float> cir) {
		if (Cinematicas.alguna() && !Cinematicas.esBanda(sonido.getIdentifier())) {
			cir.setReturnValue(cir.getReturnValueF() * Cinematicas.volumenMundo());
		}
	}
}
