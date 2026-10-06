package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.entidad.Bacteria;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A quien agarra la Bacteria se le lleva "montado" en ella (Bacteria#positionRider),
 * pero no debe verse sentado en el aire: cuelga de sus garras con las piernas
 * sueltas, como alguien de pie levantado del suelo.
 */
@Mixin(HumanoidMobRenderer.class)
public abstract class AgarradoPoseMixin {
	@Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
	private static void backrooms$colgando(LivingEntity entidad, HumanoidRenderState estado, float parcial, ItemModelResolver modelos, CallbackInfo ci) {
		if (entidad.getVehicle() instanceof Bacteria) {
			estado.isPassenger = false;
		}
	}
}
