package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.objetos.Objetos;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con la linterna o la camara en la mano, la Q no hace nada (ni se manda al servidor). */
@Mixin(LocalPlayer.class)
public abstract class SoltarObjetoClienteMixin {
	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void backrooms$noSeTira(boolean todo, CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer j = (LocalPlayer) (Object) this;
		if (Objetos.noSeTira(j, j.getInventory().getSelectedItem())) {
			cir.setReturnValue(false);
		}
	}
}
