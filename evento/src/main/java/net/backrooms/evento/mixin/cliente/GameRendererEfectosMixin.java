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

	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
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
		return fov * (1.0F - 0.18F * HerramientasCliente.subida(1.0F));
	}
}
