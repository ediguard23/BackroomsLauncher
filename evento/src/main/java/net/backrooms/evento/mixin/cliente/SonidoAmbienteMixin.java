package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.cliente.AmbienteCliente;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * En el apagon el zumbido de los tubos se corta de golpe (con el golpe del
 * rele) y vuelve a subir cuando vuelve la luz. El bucle del bioma se sigue
 * reproduciendo: solo cambia su volumen, que el motor recalcula cada tick.
 */
@Mixin(SoundEngine.class)
public abstract class SonidoAmbienteMixin {
	@Unique
	private static final Identifier ZUMBIDO = BackroomsEvento.id("nivel0.zumbido");

	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
	private void backrooms$cortarZumbido(SoundInstance sonido, CallbackInfoReturnable<Float> cir) {
		if (ZUMBIDO.equals(sonido.getIdentifier())) {
			cir.setReturnValue(cir.getReturnValueF() * AmbienteCliente.zumbido());
		}
	}
}
