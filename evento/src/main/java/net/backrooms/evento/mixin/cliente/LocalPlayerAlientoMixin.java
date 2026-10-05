package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.SupervivenciaCliente;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sin aliento no se puede correr (ni empezar ni seguir) hasta recuperarse. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerAlientoMixin {
	@Inject(method = "isSprintingPossible", at = @At("RETURN"), cancellable = true)
	private void backrooms$sinAliento(boolean volando, CallbackInfoReturnable<Boolean> cir) {
		if (SupervivenciaCliente.agotado()) {
			cir.setReturnValue(false);
		}
	}
}
