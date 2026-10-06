package net.backrooms.evento.mixin;

import net.backrooms.evento.entidad.Bacteria;
import net.backrooms.evento.escondite.Arrastre;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dos cosas del jugador, en cliente y servidor a la vez:
 *  - Arrastrarse: la postura que quiere es la de nadar (tumbado, 0,6 de alto),
 *    como cuando una trampilla te obliga a gatear. Ver Arrastre.
 *  - Agarrado por la Bacteria: "montado" en ella mientras le levanta y le come;
 *    Mayus no le suelta (Minecraft baja de la montura con Mayus).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getDesiredPose", at = @At("HEAD"), cancellable = true)
	private void backrooms$arrastrarse(CallbackInfoReturnable<Pose> cir) {
		Player yo = (Player) (Object) this;
		if (Arrastre.arrastrandose(yo) && !yo.isSpectator() && !yo.isPassenger() && !yo.getAbilities().flying && !yo.isSleeping()) {
			cir.setReturnValue(Pose.SWIMMING);
		}
	}

	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void backrooms$agarrado(CallbackInfoReturnable<Boolean> cir) {
		if (((Player) (Object) this).getVehicle() instanceof Bacteria) {
			cir.setReturnValue(false);
		}
	}
}
