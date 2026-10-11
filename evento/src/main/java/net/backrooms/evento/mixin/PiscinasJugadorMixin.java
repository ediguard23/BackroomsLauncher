package net.backrooms.evento.mixin;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.piscinas.Piscinas;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Los pasos en las Piscinas: con los pies en el agua no suena el azulejo de debajo (ni la
 * brazada de Minecraft), sino el agua que salpica a cada paso; y al nadar, las brazadas de
 * tools/sonidos/nivel37.js. Los pasos en seco ya los pone el azulejo (BloquesPiscinas).
 * Fuera de las Piscinas no cambia nada.
 */
@Mixin(Player.class)
public abstract class PiscinasJugadorMixin {
	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void backrooms$pasoEnElAgua(BlockPos pos, BlockState estado, CallbackInfo ci) {
		Player j = (Player) (Object) this;
		if (j.isInWater() && Piscinas.es(j.level())) {
			j.playSound(Sonidos.PISCINAS_PASO_AGUA, 0.35F, 0.9F + j.getRandom().nextFloat() * 0.2F);
			ci.cancel();
		}
	}

	@Inject(method = "getSwimSound", at = @At("HEAD"), cancellable = true)
	private void backrooms$brazada(CallbackInfoReturnable<SoundEvent> cir) {
		if (Piscinas.es(((Player) (Object) this).level())) {
			cir.setReturnValue(Sonidos.PISCINAS_NADO);
		}
	}
}
