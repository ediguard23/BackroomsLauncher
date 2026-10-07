package net.backrooms.evento.mixin.cliente;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.backrooms.evento.cliente.HerramientasCliente;
import net.backrooms.evento.cliente.efectos.EfectosMundo;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Engancha los efectos del Nivel 0 (EfectosMundo) entre el mundo y la mano,
 * esconde la mano con la camara levantada y le da su zoom.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererEfectosMixin {
	@Shadow
	public abstract Camera getMainCamera();

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING",
		target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
	private void backrooms$efectos(DeltaTracker tiempo, CallbackInfo ci, @Local(ordinal = 0) Matrix4f proyeccion, @Local(ordinal = 1) Matrix4f vista) {
		EfectosMundo.mundo(proyeccion, vista, this.getMainCamera(), tiempo.getGameTimeDeltaPartialTick(true));
	}

	// al final: renderItemInHand solo encola la mano, que se dibuja despues con los
	// efectos de pantalla; si se oscureciera antes, en el apagon la mano saldria iluminada
	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void backrooms$mano(DeltaTracker tiempo, CallbackInfo ci) {
		EfectosMundo.mano();
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void backrooms$sinManoConCamara(float parcial, boolean dormido, Matrix4f vista, CallbackInfo ci) {
		if (HerramientasCliente.subida(parcial) > 0.5F) {
			ci.cancel();
		}
	}

	@ModifyReturnValue(method = "getFov", at = @At("RETURN"))
	private float backrooms$zoomCamara(float fov) {
		if (net.backrooms.evento.cliente.cinematica.CinematicaCliente.vistaVestibulo() != null) {
			return 70.0F; // el vestibulo por las puertas de la cinta: el mismo FOV que el shader
		}
		return fov * (1.0F - 0.18F * HerramientasCliente.subida(1.0F));
	}
}
