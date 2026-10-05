package net.backrooms.evento.mixin.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import net.backrooms.evento.cliente.Balanceo;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cambia el balanceo de la vista de Minecraft por el del evento (Balanceo).
 * bobView solo se llama con la opcion «Balanceo de la vista» puesta, asi que
 * quien la quita no tiene ninguno de los dos.
 */
@Mixin(GameRenderer.class)
public abstract class BalanceoMixin {
	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void backrooms$balanceo(PoseStack pose, float parcial, CallbackInfo ci) {
		Balanceo.aplicar(pose, parcial);
		ci.cancel();
	}
}
