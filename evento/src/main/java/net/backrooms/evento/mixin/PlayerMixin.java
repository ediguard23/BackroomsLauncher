package net.backrooms.evento.mixin;

import net.backrooms.evento.entidad.Bacteria;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Agarrado por la Bacteria: el jugador va "montado" en ella mientras le levanta y
 * le come (Bacteria#positionRider). Mayus no le suelta (Minecraft baja de la
 * montura con Mayus). En cliente y servidor a la vez.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void backrooms$agarrado(CallbackInfoReturnable<Boolean> cir) {
		if (((Player) (Object) this).getVehicle() instanceof Bacteria) {
			cir.setReturnValue(false);
		}
	}
}
