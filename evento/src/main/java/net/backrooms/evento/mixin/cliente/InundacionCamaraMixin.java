package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.piscinas.Inundacion;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con la camara bajo el agua de la inundacion, la niebla y el velo de debajo del agua de Minecraft. */
@Mixin(Camera.class)
public abstract class InundacionCamaraMixin {
	@Shadow
	public abstract Vec3 position();

	@Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
	private void backrooms$bajoElAgua(CallbackInfoReturnable<FogType> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && this.position().y < Inundacion.nivelPara(mc.level)) {
			cir.setReturnValue(FogType.WATER);
		}
	}
}
